package com.chopcode.rutago_app.services.reservations;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.chopcode.rutago_app.models.DisponibilidadAsientos;
import com.chopcode.rutago_app.models.Reserva;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class ReservaService {

    // ✅ NUEVO: Tag para logs
    private static final String TAG = "ReservaService";

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

    public interface HistorialCallback {
        void onHistorialCargado(List<Reserva> reservas);
        void onError(String error);
    }

    public ReservaService() {
        Log.d(TAG, "🚀 Constructor - Inicializando servicio de reservas");
        this.databaseReference = FirebaseDatabase.getInstance().getReference();
        db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        Log.d(TAG, "✅ Servicio de reservas inicializado correctamente");
    }

    /**
     * 🔥 MÉTODOS EXISTENTES (con logs agregados)
     */
    public void actualizarDisponibilidadAsientos(Context context, String horarioId, int asientoSeleccionado,
                                                 String origen, String destino, String tiempoEstimado,
                                                 String metodoPago, String estadoReserva,
                                                 String placa, Double precio,
                                                 String conductor, String telefonoC,
                                                 ReservaCallback callback) {
        Log.d(TAG, "🔄 Iniciando actualización de disponibilidad de asientos:");
        Log.d(TAG, "   - Horario ID: " + horarioId);
        Log.d(TAG, "   - Asiento seleccionado: " + asientoSeleccionado);
        Log.d(TAG, "   - Origen: " + origen + " → Destino: " + destino);
        Log.d(TAG, "   - Conductor: " + conductor);
        Log.d(TAG, "   - Precio: $" + precio);

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "❌ Usuario no autenticado");
            callback.onError("Usuario no autenticado.");
            return;
        }

        String uid = currentUser.getUid();
        Log.d(TAG, "👤 Usuario autenticado - UID: " + uid);

        DatabaseReference userRef = databaseReference.child("usuarios").child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.e(TAG, "❌ No se encontró información del usuario en la base de datos");
                    callback.onError("No se encontró información del usuario.");
                    return;
                }

                String nombre = snapshot.child("nombre").getValue(String.class);
                String telefono = snapshot.child("telefono").getValue(String.class);
                String email = snapshot.child("email").getValue(String.class);

                Log.d(TAG, "📋 Datos del usuario obtenidos:");
                Log.d(TAG, "   - Nombre: " + nombre);
                Log.d(TAG, "   - Teléfono: " + telefono);
                Log.d(TAG, "   - Email: " + email);

                if (nombre == null || email == null) {
                    Log.e(TAG, "❌ Datos del usuario incompletos");
                    callback.onError("Datos del usuario incompletos.");
                    return;
                }

                DatabaseReference refDisponibilidad = databaseReference.child("disponibilidadAsientos").child(horarioId);
                Log.d(TAG, "🔍 Consultando disponibilidad en: disponibilidadAsientos/" + horarioId);

                refDisponibilidad.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Log.e(TAG, "❌ No se encontró disponibilidad para el horario: " + horarioId);
                            callback.onError("No se encontró disponibilidad.");
                            return;
                        }

                        DisponibilidadAsientos disponibilidad = snapshot.getValue(DisponibilidadAsientos.class);
                        Log.d(TAG, "📊 Disponibilidad obtenida - Asientos disponibles: " +
                                (disponibilidad != null ? disponibilidad.getAsientosDisponibles() : "null"));

                        if (disponibilidad != null && disponibilidad.getAsientosDisponibles() > 0) {
                            int nuevosAsientosDisponibles = disponibilidad.getAsientosDisponibles() - 1;
                            Log.d(TAG, "🔄 Actualizando asientos disponibles: " +
                                    disponibilidad.getAsientosDisponibles() + " → " + nuevosAsientosDisponibles);

                            refDisponibilidad.child("asientosDisponibles").setValue(nuevosAsientosDisponibles);
                            marcarAsientoComoOcupado(horarioId, asientoSeleccionado);

                            registrarReserva(context, uid, nombre, telefono, email, horarioId, asientoSeleccionado,
                                    origen, destino, tiempoEstimado, metodoPago, estadoReserva,
                                    placa, precio, conductor, telefonoC, callback);
                        } else {
                            Log.w(TAG, "⚠️ No hay asientos disponibles para el horario: " + horarioId);
                            callback.onError("No hay asientos disponibles.");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "❌ Error al consultar disponibilidad: " + error.getMessage());
                        Log.e(TAG, "   - Código: " + error.getCode());
                        Log.e(TAG, "   - Detalles: " + error.getDetails());
                        callback.onError("Error al consultar disponibilidad: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error al recuperar datos del usuario: " + error.getMessage());
                callback.onError("Error al recuperar datos del usuario: " + error.getMessage());
            }
        });
    }

    private void marcarAsientoComoOcupado(String horarioId, int asiento) {
        Log.d(TAG, "📍 Marcando asiento como ocupado - Horario: " + horarioId + ", Asiento: " + asiento);

        DatabaseReference refAsientosOcupados = databaseReference
                .child("disponibilidadAsientos")
                .child(horarioId)
                .child("asientosOcupados");

        refAsientosOcupados.child(String.valueOf(asiento)).setValue(true)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Asiento " + asiento + " marcado como ocupado exitosamente");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error marcando asiento como ocupado: " + e.getMessage());
                });
    }

    private void registrarReserva(Context context, String uid, String nombre, String telefono, String email,
                                  String horarioId, int asientoSeleccionado, String origen, String destino,
                                  String tiempoEstimado, String metodoPago, String estadoReserva,
                                  String placa, double precio, String conductor, String telefonoC,
                                  ReservaCallback callback) {
        String idReserva = UUID.randomUUID().toString();
        long fechaReserva = System.currentTimeMillis();

        Log.d(TAG, "📝 Registrando nueva reserva:");
        Log.d(TAG, "   - ID Reserva: " + idReserva);
        Log.d(TAG, "   - Usuario: " + nombre + " (" + uid + ")");
        Log.d(TAG, "   - Horario: " + horarioId);
        Log.d(TAG, "   - Asiento: " + asientoSeleccionado);
        Log.d(TAG, "   - Ruta: " + origen + " → " + destino);
        Log.d(TAG, "   - Método Pago: " + metodoPago);
        Log.d(TAG, "   - Precio: $" + precio);

        Reserva reserva = new Reserva(
                idReserva, uid, horarioId, asientoSeleccionado, conductor, telefonoC, placa, precio,
                origen, destino, tiempoEstimado, metodoPago, estadoReserva, fechaReserva,
                nombre, telefono, email
        );

        databaseReference.child("reservas").child(idReserva).setValue(reserva)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Reserva registrada exitosamente en Firebase:");
                    Log.d(TAG, "   - ID: " + idReserva);
                    Log.d(TAG, "   - Estado: " + estadoReserva);
                    Log.d(TAG, "   - Fecha: " + new Date(fechaReserva));

                    Toast.makeText(context, "Reserva confirmada", Toast.LENGTH_SHORT).show();
                    callback.onReservaExitosa();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error al guardar reserva en Firebase: " + e.getMessage());
                    callback.onError("Error al guardar reserva: " + e.getMessage());
                });
    }

    public void obtenerAsientosOcupados(String horarioId, AsientosCallback callback) {
        Log.d(TAG, "🔍 Obteniendo asientos ocupados para horario: " + horarioId);

        DatabaseReference refAsientosOcupados = databaseReference
                .child("disponibilidadAsientos")
                .child(horarioId)
                .child("asientosOcupados");

        refAsientosOcupados.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.d(TAG, "ℹ️ No hay asientos ocupados para el horario: " + horarioId);
                    callback.onAsientosObtenidos(new int[0]);
                    return;
                }

                List<Integer> asientosOcupadosList = new ArrayList<>();
                Log.d(TAG, "📋 Procesando " + snapshot.getChildrenCount() + " asientos ocupados");

                for (DataSnapshot asientoSnapshot : snapshot.getChildren()) {
                    try {
                        int numeroAsiento = Integer.parseInt(asientoSnapshot.getKey());
                        asientosOcupadosList.add(numeroAsiento);
                        Log.d(TAG, "   - Asiento ocupado: " + numeroAsiento);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "❌ Error al convertir asiento: " + asientoSnapshot.getKey());
                    }
                }

                int[] asientosOcupados = new int[asientosOcupadosList.size()];
                for (int i = 0; i < asientosOcupadosList.size(); i++) {
                    asientosOcupados[i] = asientosOcupadosList.get(i);
                }

                Log.d(TAG, "✅ Asientos ocupados obtenidos: " + asientosOcupados.length + " asientos");
                callback.onAsientosObtenidos(asientosOcupados);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error al obtener asientos ocupados: " + error.getMessage());
                callback.onError("Error al obtener asientos ocupados: " + error.getMessage());
            }
        });
    }

    /**
     * Carga las reservas específicas de un conductor con filtros por horarios asignados
     */
    public void cargarReservasConductor(String conductorNombre, List<String> horariosAsignados, DriverReservationsCallback callback) {
        Log.d(TAG, "👤 Cargando reservas para conductor: " + conductorNombre);
        Log.d(TAG, "   - Horarios asignados: " + (horariosAsignados != null ? horariosAsignados.size() : 0));

        if (conductorNombre == null) {
            Log.e(TAG, "❌ Nombre del conductor es nulo");
            callback.onError("Nombre del conductor es nulo");
            return;
        }

        DatabaseReference reservasRef = databaseReference.child("reservas");
        Log.d(TAG, "🔍 Consultando reservas en Firebase...");

        reservasRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "✅ Datos de reservas recibidos - Total: " + snapshot.getChildrenCount());
                List<Reserva> reservas = new ArrayList<>();
                int reservasFiltradas = 0;

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
                            reservasFiltradas++;
                            Log.d(TAG, "🎯 Reserva filtrada - ID: " + reserva.getIdReserva() +
                                    ", Pasajero: " + reserva.getNombre() +
                                    ", Asiento: " + reserva.getPuestoReservado());
                        }
                    }
                }

                Log.d(TAG, "📊 Reservas del conductor cargadas: " + reservasFiltradas + " de " + snapshot.getChildrenCount());
                callback.onDriverReservationsLoaded(reservas);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error al cargar reservas del conductor: " + error.getMessage());
                callback.onError("Error al cargar reservas del conductor: " + error.getMessage());
            }
        });
    }

    /**
     * Actualiza el estado de una reserva (Confirmar/Cancelar)
     */
    public void actualizarEstadoReserva(String reservaId, String nuevoEstado, ReservationUpdateCallback callback) {
        Log.d(TAG, "🔄 Actualizando estado de reserva:");
        Log.d(TAG, "   - Reserva ID: " + reservaId);
        Log.d(TAG, "   - Nuevo estado: " + nuevoEstado);

        DatabaseReference reservaRef = databaseReference
                .child("reservas")
                .child(reservaId)
                .child("estadoReserva");

        reservaRef.setValue(nuevoEstado)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Estado de reserva actualizado exitosamente");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error actualizando estado de reserva: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Carga todas las reservas con un estado específico
     */
    public void cargarReservasPorEstado(String estado, ReservationsCallback callback) {
        Log.d(TAG, "🔍 Cargando reservas por estado: " + estado);

        DatabaseReference reservasRef = databaseReference.child("reservas");

        reservasRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "✅ Datos de reservas recibidos - Total: " + snapshot.getChildrenCount());
                List<Reserva> reservas = new ArrayList<>();
                int reservasCoincidentes = 0;

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Reserva reserva = dataSnapshot.getValue(Reserva.class);
                    if (reserva != null && estado.equals(reserva.getEstadoReserva())) {
                        reserva.setIdReserva(dataSnapshot.getKey());
                        reservas.add(reserva);
                        reservasCoincidentes++;
                    }
                }

                Log.d(TAG, "📊 Reservas encontradas con estado '" + estado + "': " + reservasCoincidentes);
                callback.onReservationsLoaded(reservas);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error al cargar reservas por estado: " + error.getMessage());
                callback.onError("Error al cargar reservas: " + error.getMessage());
            }
        });
    }

    /**
     * Carga TODAS las reservas de un conductor
     */
    public void cargarReservasConductorPorUID(String conductorUID, String estado, ReservationsCallback callback) {
        Log.d(TAG, "👤 Cargando reservas para conductor UID: " + conductorUID);
        Log.d(TAG, "   - Estado filtro: " + (estado != null ? estado : "TODAS"));

        DatabaseReference reservasRef = databaseReference.child("reservas");

        reservasRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "✅ Datos de reservas recibidos - Total: " + snapshot.getChildrenCount());
                List<Reserva> reservas = new ArrayList<>();
                int reservasDelConductor = 0;

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
                            reservasDelConductor++;
                            Log.d(TAG, "🎯 Reserva encontrada - ID: " + reserva.getIdReserva() +
                                    ", Estado: " + reserva.getEstadoReserva());
                        }
                    }
                }

                // Ordenar por fecha (más recientes primero)
                Collections.sort(reservas, (r1, r2) -> Long.compare(r2.getFechaReserva(), r1.getFechaReserva()));

                Log.d(TAG, "📊 Reservas del conductor cargadas: " + reservasDelConductor);
                callback.onReservationsLoaded(reservas);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error al cargar reservas del conductor: " + error.getMessage());
                callback.onError("Error al cargar reservas: " + error.getMessage());
            }
        });
    }

    /**
     * Método para cargar las reservas en la interfaz de historial de reservas del usuario pasajero
     */
    public void obtenerHistorialUsuario(String usuarioId, HistorialCallback callback) {
        Log.d(TAG, "📋 Obteniendo historial de reservas para usuario: " + usuarioId);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("reservas");

        ref.orderByChild("usuarioId").equalTo(usuarioId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Log.d(TAG, "✅ Historial recibido - Total reservas: " + dataSnapshot.getChildrenCount());
                        List<Reserva> reservas = new ArrayList<>();
                        int reservasProcesadas = 0;

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Reserva reserva = snapshot.getValue(Reserva.class);
                            if (reserva != null) {
                                reservas.add(reserva);
                                reservasProcesadas++;
                                Log.d(TAG, "   - Reserva: " + reserva.getIdReserva() +
                                        " | " + reserva.getOrigen() + " → " + reserva.getDestino() +
                                        " | Estado: " + reserva.getEstadoReserva());
                            }
                        }

                        Log.d(TAG, "📊 Historial procesado: " + reservasProcesadas + " reservas");
                        callback.onHistorialCargado(reservas);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "❌ Error al obtener historial: " + databaseError.getMessage());
                        callback.onError(databaseError.getMessage());
                    }
                });
    }
}