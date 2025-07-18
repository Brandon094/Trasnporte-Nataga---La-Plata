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
     * Interfaz para manejar la carga de la reserva de manera asíncrona.
     */
    public interface ReservaCallback {
        void onReservaExitosa();
        void onError(String error);
    }
    /**
     * Interfaz para la carga de asientos de forma asincronica
     */
    public interface AsientosCallback {
        void onAsientosObtenidos(int[] asientosOcupados);
        void onError(String error);
    }
    /**
     * Interfaz para la carga de la disponibilidad de asientos
     * */
    public interface DisponibilidadCallback {
        void onDisponible(boolean disponible);
        void onError(String error);
    }
    /**
     * Interfaz para la carga de las reservas
     * */
    public interface ReservaCargadaCallback {
        void onCargaExitosa(List<Reserva> reservas);
        void onError(String mensaje);
    }

    public ReservaService() {
        this.databaseReference = FirebaseDatabase.getInstance().getReference();
        db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }
    /**
     * 🔥 Actualiza la disponibilidad de asientos y guarda la reserva en Firebase
     */
    public void actualizarDisponibilidadAsientos(Context context, String horarioId, int asientoSeleccionado,
                                                 String origen, String destino, String tiempoEstimado,
                                                 String metodoPago, String estadoReserva,
                                                 String placa,Double precio,
                                                 String conductor, String telefonoC,
                                                 ReservaCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Usuario no autenticado.");
            return;
        }

        String uid = currentUser.getUid();
        DatabaseReference userRef = databaseReference.child("usuarios").child(uid);

        // Recuperar datos del usuario antes de registrar la reserva
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
                            // 🔹 Restar un asiento y actualizar Firebase
                            int nuevosAsientosDisponibles = disponibilidad.getAsientosDisponibles() - 1;
                            refDisponibilidad.child("asientosDisponibles").setValue(nuevosAsientosDisponibles);

                            // 🔹 Guardar el asiento reservado en `asientosOcupados`
                            marcarAsientoComoOcupado(horarioId, asientoSeleccionado);

                            // 🔹 Registrar la reserva en Firebase con los datos del usuario
                            registrarReserva(context, uid, nombre, telefono, email, horarioId, asientoSeleccionado,
                                    origen, destino, tiempoEstimado, metodoPago, estadoReserva,
                                    placa, precio,
                                    conductor, telefonoC, callback);
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

    /**
     * 🔥 Marca un asiento como ocupado en Firebase.
     */
    private void marcarAsientoComoOcupado(String horarioId, int asiento) {
        DatabaseReference refAsientosOcupados = databaseReference
                .child("disponibilidadAsientos")
                .child(horarioId)
                .child("asientosOcupados");

        // Guardar el asiento como ocupado
        refAsientosOcupados.child(String.valueOf(asiento)).setValue(true);
    }

    // 🔥 Registra la reserva en Firebase con los datos del usuario.
    private void registrarReserva(Context context, String uid, String nombre, String telefono, String email,
                                  String horarioId, int asientoSeleccionado, String origen, String destino,
                                  String tiempoEstimado, String metodoPago, String estadoReserva,
                                  String placa, double precio,String conductor, String telefonoC,
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
    /**
     * 🔥 Obtiene los asientos ocupados de Firebase y los envía al callback.
     */
    public void obtenerAsientosOcupados(String horarioId, AsientosCallback callback) {
        DatabaseReference refAsientosOcupados = databaseReference
                .child("disponibilidadAsientos")
                .child(horarioId)
                .child("asientosOcupados");

        refAsientosOcupados.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    callback.onAsientosObtenidos(new int[0]); // 🔹 No hay asientos ocupados
                    return;
                }

                List<Integer> asientosOcupadosList = new ArrayList<>();

                for (DataSnapshot asientoSnapshot : snapshot.getChildren()) {
                    try {
                        int numeroAsiento = Integer.parseInt(asientoSnapshot.getKey());
                        asientosOcupadosList.add(numeroAsiento);
                    } catch (NumberFormatException e) {
                        // 🔴 Registro del error si el valor no es un número válido
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
     * Método para cargar todas las reservas"
     */
    public void cargarReservas(@NonNull final ReservaCargadaCallback callback) {
        databaseReference.child("reservas")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Reserva> listaReservas = new ArrayList<>();
                        for (DataSnapshot reservaSnap : snapshot.getChildren()) {
                            Reserva reserva = reservaSnap.getValue(Reserva.class);
                            if (reserva != null && "Por confirmar".equals(reserva.getEstadoReserva())) {
                                listaReservas.add(reserva);
                            }
                        }
                        callback.onCargaExitosa(listaReservas);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError("Error al cargar reservas: " + error.getMessage());
                    }
                });
    }
    /**
     * Metodo para cargar las rutas siguientes
     */
}
