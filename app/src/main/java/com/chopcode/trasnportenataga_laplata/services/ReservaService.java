package com.chopcode.trasnportenataga_laplata.services;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chopcode.trasnportenataga_laplata.adapters.ReservaAdapter;
import com.chopcode.trasnportenataga_laplata.models.DisponibilidadAsientos;
import com.chopcode.trasnportenataga_laplata.models.Horario;
import com.chopcode.trasnportenataga_laplata.models.Pasajero;
import com.chopcode.trasnportenataga_laplata.models.Reserva;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReservaService {
    private FirebaseFirestore db;
    private DatabaseReference databaseReference;
    private FirebaseAuth auth;

    /**
     * INTERFACES CONSOLIDADAS - Todas las funcionalidades de reservas
     */
    public interface ReservaCallback {
        void onReservaExitosa();

        void onError(String error);
    }

    public interface AsientosCallback {
        void onAsientosObtenidos(int[] asientosOcupados);

        void onError(String error);
    }

    public interface DisponibilidadCallback {
        void onDisponible(boolean disponible);

        void onError(String error);
    }

    public interface ReservaCargadaCallback {
        void onCargaExitosa(List<Reserva> reservas);

        void onError(String mensaje);
    }

    // 🔥 NUEVAS INTERFACES DEL RESERVATION MANAGER
    public interface ReservationsCallback {
        void onReservationsLoaded(List<Reserva> reservas);

        void onError(String error);
    }

    public interface ReservationUpdateCallback {
        void onSuccess();

        void onError(String error);
    }

    public interface DriverReservationsCallback {
        void onDriverReservationsLoaded(List<Reserva> reservas);

        void onError(String error);
    }

    public ReservaService() {
        this.databaseReference = FirebaseDatabase.getInstance().getReference();
        db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    /**
     * 🔥 MÉTODOS EXISTENTES (sin cambios)
     */
    public void actualizarDisponibilidadAsientos(Context context, String horarioId, int asientoSeleccionado,
                                                 String origen, String destino, String tiempoEstimado,
                                                 String metodoPago, String estadoReserva,
                                                 String placa, Double precio,
                                                 String conductor, String telefonoC,
                                                 ReservaCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Usuario no autenticado.");
            return;
        }

        String uid = currentUser.getUid();
        DatabaseReference userRef = databaseReference.child("usuarios").child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    callback.onError("No se encontró información del usuario.");
                    return;
                }

                String nombre = snapshot.child("nombre").getValue(String.class);
                String telefono = snapshot.child("telefono").getValue(String.class);
                String email = snapshot.child("email").getValue(String.class);

                if (nombre == null || email == null) {
                    callback.onError("Datos del usuario incompletos.");
                    return;
                }

                DatabaseReference refDisponibilidad = databaseReference.child("disponibilidadAsientos").child(horarioId);
                refDisponibilidad.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            callback.onError("No se encontró disponibilidad.");
                            return;
                        }

                        DisponibilidadAsientos disponibilidad = snapshot.getValue(DisponibilidadAsientos.class);
                        if (disponibilidad != null && disponibilidad.getAsientosDisponibles() > 0) {
                            int nuevosAsientosDisponibles = disponibilidad.getAsientosDisponibles() - 1;
                            refDisponibilidad.child("asientosDisponibles").setValue(nuevosAsientosDisponibles);
                            marcarAsientoComoOcupado(horarioId, asientoSeleccionado);

                            registrarReserva(context, uid, nombre, telefono, email, horarioId, asientoSeleccionado,
                                    origen, destino, tiempoEstimado, metodoPago, estadoReserva,
                                    placa, precio, conductor, telefonoC, callback);
                        } else {
                            callback.onError("No hay asientos disponibles.");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError("Error al consultar disponibilidad: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("Error al recuperar datos del usuario: " + error.getMessage());
            }
        });
    }

    private void marcarAsientoComoOcupado(String horarioId, int asiento) {
        DatabaseReference refAsientosOcupados = databaseReference
                .child("disponibilidadAsientos")
                .child(horarioId)
                .child("asientosOcupados");

        refAsientosOcupados.child(String.valueOf(asiento)).setValue(true);
    }

    private void registrarReserva(Context context, String uid, String nombre, String telefono, String email,
                                  String horarioId, int asientoSeleccionado, String origen, String destino,
                                  String tiempoEstimado, String metodoPago, String estadoReserva,
                                  String placa, double precio, String conductor, String telefonoC,
                                  ReservaCallback callback) {
        String idReserva = UUID.randomUUID().toString();
        long fechaReserva = System.currentTimeMillis();

        Reserva reserva = new Reserva(
                idReserva, uid, horarioId, asientoSeleccionado, conductor, telefonoC, placa, precio,
                origen, destino, tiempoEstimado, metodoPago, estadoReserva, fechaReserva,
                nombre, telefono, email
        );

        databaseReference.child("reservas").child(idReserva).setValue(reserva)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Reserva confirmada", Toast.LENGTH_SHORT).show();
                    callback.onReservaExitosa();
                })
                .addOnFailureListener(e -> {
                    callback.onError("Error al guardar reserva: " + e.getMessage());
                });
    }

    public void obtenerAsientosOcupados(String horarioId, AsientosCallback callback) {
        DatabaseReference refAsientosOcupados = databaseReference
                .child("disponibilidadAsientos")
                .child(horarioId)
                .child("asientosOcupados");

        refAsientosOcupados.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    callback.onAsientosObtenidos(new int[0]);
                    return;
                }

                List<Integer> asientosOcupadosList = new ArrayList<>();
                for (DataSnapshot asientoSnapshot : snapshot.getChildren()) {
                    try {
                        int numeroAsiento = Integer.parseInt(asientoSnapshot.getKey());
                        asientosOcupadosList.add(numeroAsiento);
                    } catch (NumberFormatException e) {
                        System.err.println("Error al convertir asiento: " + asientoSnapshot.getKey());
                    }
                }

                int[] asientosOcupados = new int[asientosOcupadosList.size()];
                for (int i = 0; i < asientosOcupadosList.size(); i++) {
                    asientosOcupados[i] = asientosOcupadosList.get(i);
                }

                callback.onAsientosObtenidos(asientosOcupados);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("Error al obtener asientos ocupados: " + error.getMessage());
            }
        });
    }
    /**
     * Carga las reservas específicas de un conductor con filtros por horarios asignados
     */
    public void cargarReservasConductor(String conductorNombre, List<String> horariosAsignados, DriverReservationsCallback callback) {
        if (conductorNombre == null) {
            callback.onError("Nombre del conductor es nulo");
            return;
        }

        DatabaseReference reservasRef = databaseReference.child("reservas");

        reservasRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Reserva> reservas = new ArrayList<>();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Reserva reserva = dataSnapshot.getValue(Reserva.class);
                    if (reserva != null) {
                        reserva.setIdReserva(dataSnapshot.getKey());

                        String conductorIdReserva = reserva.getConductor();
                        if (conductorIdReserva == null && dataSnapshot.hasChild("conductorId")) {
                            conductorIdReserva = dataSnapshot.child("conductorId").getValue(String.class);
                        }

                        String horarioIdReserva = reserva.getHorarioId();
                        if (horarioIdReserva == null && dataSnapshot.hasChild("horarioId")) {
                            horarioIdReserva = dataSnapshot.child("horarioId").getValue(String.class);
                            reserva.setHorarioId(horarioIdReserva);
                        }

                        boolean esDelConductor = conductorNombre.equals(conductorIdReserva);
                        boolean esEstadoValido = "Por confirmar".equals(reserva.getEstadoReserva());
                        boolean esDeHorarioAsignado = horariosAsignados.isEmpty() ||
                                horarioIdReserva == null ||
                                horariosAsignados.contains(horarioIdReserva);

                        if (esDelConductor && esEstadoValido && esDeHorarioAsignado) {
                            reservas.add(reserva);
                        }
                    }
                }

                callback.onDriverReservationsLoaded(reservas);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("Error al cargar reservas del conductor: " + error.getMessage());
            }
        });
    }

    /**
     * Actualiza el estado de una reserva (Confirmar/Cancelar)
     */
    public void actualizarEstadoReserva(String reservaId, String nuevoEstado, ReservationUpdateCallback callback) {
        DatabaseReference reservaRef = databaseReference
                .child("reservas")
                .child(reservaId)
                .child("estadoReserva");

        reservaRef.setValue(nuevoEstado)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Carga todas las reservas con un estado específico
     */
    public void cargarReservasPorEstado(String estado, ReservationsCallback callback) {
        DatabaseReference reservasRef = databaseReference.child("reservas");

        reservasRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Reserva> reservas = new ArrayList<>();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Reserva reserva = dataSnapshot.getValue(Reserva.class);
                    if (reserva != null && estado.equals(reserva.getEstadoReserva())) {
                        reserva.setIdReserva(dataSnapshot.getKey());
                        reservas.add(reserva);
                    }
                }

                callback.onReservationsLoaded(reservas);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("Error al cargar reservas: " + error.getMessage());
            }
        });
    }

    /**
     * Carga TODAS las reservas de un conductor
     */
    public void cargarReservasConductorPorUID(String conductorUID, String estado, ReservationsCallback callback) {
        DatabaseReference reservasRef = databaseReference.child("reservas");

        reservasRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Reserva> reservas = new ArrayList<>();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Reserva reserva = dataSnapshot.getValue(Reserva.class);
                    if (reserva != null && reserva.getConductorId() != null) {

                        // Comparar por UID del conductor
                        boolean esDelConductor = reserva.getConductorId().equals(conductorUID);

                        // ✅ CORREGIDO: Lógica correcta para filtrar por estado
                        boolean estadoCoincide;

                        if ("TODAS".equalsIgnoreCase(estado) || estado == null || estado.isEmpty()) {
                            // Cargar TODAS las reservas sin filtrar por estado
                            estadoCoincide = true;
                        } else {
                            // Filtrar por estado específico
                            estadoCoincide = reserva.getEstadoReserva() != null &&
                                    reserva.getEstadoReserva().equalsIgnoreCase(estado);
                        }

                        if (esDelConductor && estadoCoincide) {
                            reserva.setIdReserva(dataSnapshot.getKey());
                            reservas.add(reserva);
                        }
                    }
                }

                // Ordenar por fecha (más recientes primero)
                Collections.sort(reservas, (r1, r2) -> Long.compare(r2.getFechaReserva(), r1.getFechaReserva()));
                callback.onReservationsLoaded(reservas);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("Error al cargar reservas: " + error.getMessage());
            }
        });
    }
    // Método para cargar las reservas de en la interfaz de historial de reservas del usuario
    // pasajero
    public void obtenerHistorialUsuario(String usuarioId, HistorialCallback callback) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("reservas");

        ref.orderByChild("usuarioId").equalTo(usuarioId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        List<Reserva> reservas = new ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Reserva reserva = snapshot.getValue(Reserva.class);
                            if (reserva != null) {
                                reservas.add(reserva);
                            }
                        }
                        callback.onHistorialCargado(reservas);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        callback.onError(databaseError.getMessage());
                    }
                });
    }

    public interface HistorialCallback {
        void onHistorialCargado(List<Reserva> reservas);
        void onError(String error);
    }
}