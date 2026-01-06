package com.chopcode.trasnportenataga_laplata.services.reservations;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.chopcode.trasnportenataga_laplata.config.MyApp;
import com.chopcode.trasnportenataga_laplata.managers.seats.dataprocessor.SeatsDataProcessor;
import com.chopcode.trasnportenataga_laplata.models.Reserva;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.*;

public class ReservaService {

    // ✅ NUEVO: Tag para logs
    private static final String TAG = "ReservaService";

    private DatabaseReference databaseReference;
    private SeatsDataProcessor seatsDataManager;

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
        this.databaseReference = MyApp.getDatabaseReference("");
        this.seatsDataManager = new SeatsDataProcessor(); // ✅ INICIALIZAR EL MANAGER
        Log.d(TAG, "✅ Servicio de reservas inicializado correctamente usando MyApp");
    }

    /**
     * 🔥 MÉTODOS EXISTENTES (refactorizados para usar MyApp)
     */
    public void actualizarDisponibilidadAsientos(Context context, String horarioId, int asientoSeleccionado,
                                                 String origen, String destino, String tiempoEstimado,
                                                 String metodoPago, String estadoReserva,
                                                 String placa, Double precio,
                                                 String conductor, String telefonoC,
                                                 ReservaCallback callback) {

        Log.d(TAG, "🔄 Iniciando proceso de reserva para asiento: " + asientoSeleccionado);

        // ✅ Registrar evento
        Map<String, Object> eventParams = new HashMap<>();
        eventParams.put("horario_id", horarioId);
        eventParams.put("asiento", asientoSeleccionado);
        MyApp.logEvent("reserva_iniciada", eventParams);

        FirebaseUser currentUser = MyApp.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "❌ Usuario no autenticado");
            callback.onError("Usuario no autenticado.");
            return;
        }

        String uid = currentUser.getUid();
        Log.d(TAG, "👤 Usuario autenticado - UID: " + uid);

        // ✅ PASO 1: Verificar disponibilidad del asiento ANTES de obtener datos del usuario
        seatsDataManager.checkSeatAvailability(horarioId, asientoSeleccionado,
                new SeatsDataProcessor.SeatAvailabilityCallback(){
                    @Override
                    public void onSeatAvailable(boolean available) {
                        if (!available) {
                            Log.e(TAG, "❌ Asiento " + asientoSeleccionado + " ya está ocupado");
                            callback.onError("El asiento seleccionado ya está ocupado. Por favor selecciona otro.");
                            return;
                        }

                        // ✅ PASO 2: Si el asiento está disponible, obtener datos del usuario
                        obtenerDatosUsuarioYContinuar(context, uid, horarioId, asientoSeleccionado,
                                origen, destino, tiempoEstimado, metodoPago, estadoReserva,
                                placa, precio, conductor, telefonoC, callback);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ Error verificando disponibilidad: " + error);
                        callback.onError("Error verificando disponibilidad: " + error);
                    }
                });
    }

    private void obtenerDatosUsuarioYContinuar(Context context, String uid, String horarioId, int asientoSeleccionado,
                                               String origen, String destino, String tiempoEstimado,
                                               String metodoPago, String estadoReserva,
                                               String placa, Double precio, String conductor, String telefonoC,
                                               ReservaCallback callback) {

        DatabaseReference userRef = MyApp.getDatabaseReference("usuarios/" + uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.e(TAG, "❌ No se encontró información del usuario");
                    callback.onError("No se encontró información del usuario.");
                    return;
                }

                String nombre = snapshot.child("nombre").getValue(String.class);
                String telefono = snapshot.child("telefono").getValue(String.class);
                String email = snapshot.child("email").getValue(String.class);

                if (nombre == null || email == null) {
                    Log.e(TAG, "❌ Datos del usuario incompletos");
                    callback.onError("Datos del usuario incompletos.");
                    return;
                }

                // ✅ PASO 3: Reservar el asiento en Firebase
                seatsDataManager.reserveSeat(horarioId, asientoSeleccionado,
                        new SeatsDataProcessor.SeatReservationCallback() {
                            @Override
                            public void onSuccess() {
                                // ✅ PASO 4: Crear la reserva
                                registrarReserva(context, uid, nombre, telefono, email, horarioId, asientoSeleccionado,
                                        origen, destino, tiempoEstimado, metodoPago, estadoReserva,
                                        placa, precio, conductor, telefonoC, callback);
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "❌ Error reservando asiento: " + error);
                                callback.onError("Error reservando asiento: " + error);
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error obteniendo datos del usuario: " + error.getMessage());
                callback.onError("Error obteniendo datos del usuario: " + error.getMessage());
            }
        });
    }

    /**
     * 🔥 NUEVO: Método para liberar un asiento cuando se cancela una reserva
     */
    public void liberarAsientoReservado(String horarioId, int numeroAsiento, ReservationUpdateCallback callback) {
        Log.d(TAG, "🔄 Liberando asiento para cancelación - Horario: " + horarioId + ", Asiento: " + numeroAsiento);

        seatsDataManager.freeSeat(horarioId, numeroAsiento,
                new SeatsDataProcessor.SeatReservationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "✅ Asiento liberado exitosamente");
                callback.onSuccess();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error liberando asiento: " + error);
                callback.onError(error);
            }
        });
    }

    /**
     * 🔥 NUEVO: Método para reparar la estructura de asientos de un horario
     */
    public void repararEstructuraAsientos(String horarioId) {
        Log.d(TAG, "🔧 Reparando estructura de asientos para: " + horarioId);
        seatsDataManager.repairSeatStructure(horarioId);
    }

    /**
     * 🔥 NUEVO: Método para inicializar todos los horarios
     */
    public void inicializarTodosHorarios() {
        Log.d(TAG, "🚀 Inicializando todos los horarios en la base de datos");
        seatsDataManager.initializeAllSchedules();
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

        // ✅ USANDO MyApp para obtener referencia
        DatabaseReference reservaRef = MyApp.getDatabaseReference("reservas/" + idReserva);

        reservaRef.setValue(reserva)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Reserva registrada exitosamente en Firebase:");
                    Log.d(TAG, "   - ID: " + idReserva);
                    Log.d(TAG, "   - Estado: " + estadoReserva);
                    Log.d(TAG, "   - Fecha: " + new Date(fechaReserva));

                    // ✅ Registrar evento exitoso en Analytics
                    Map<String, Object> eventParams = new HashMap<>();
                    eventParams.put("reserva_id", idReserva);
                    eventParams.put("origen", origen);
                    eventParams.put("destino", destino);
                    eventParams.put("precio", precio);
                    eventParams.put("metodo_pago", metodoPago);
                    eventParams.put("asiento", asientoSeleccionado);
                    MyApp.logEvent("reserva_exitosa", eventParams);

                    Toast.makeText(context, "Reserva confirmada", Toast.LENGTH_SHORT).show();
                    callback.onReservaExitosa();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error al guardar reserva en Firebase: " + e.getMessage());
                    MyApp.logError(e);

                    // ✅ IMPORTANTE: Si falla crear la reserva, LIBERAR EL ASIENTO
                    liberarAsientoReservado(horarioId, asientoSeleccionado,
                            new ReservationUpdateCallback() {
                                @Override
                                public void onSuccess() {
                                    Log.w(TAG, "⚠️ Asiento liberado después de error en reserva");
                                }

                                @Override
                                public void onError(String error) {
                                    Log.e(TAG, "❌ Error liberando asiento después de fallo: " + error);
                                }
                            });

                    callback.onError("Error al guardar reserva: " + e.getMessage());
                });
    }

    public void obtenerAsientosOcupados(String horarioId, AsientosCallback callback) {
        Log.d(TAG, "🔍 Obteniendo asientos ocupados para horario: " + horarioId);

        // ✅ USAR SeatsDataManager en lugar de consultar directamente
        seatsDataManager.loadSeatsDataForSchedule(horarioId,
                new SeatsDataProcessor.SeatsDataCallback() {
            @Override
            public void onSeatsDataLoaded(Set<Integer> occupiedSeats, int availableSeats) {
                // Convertir Set<Integer> a int[]
                int[] asientosOcupadosArray = new int[occupiedSeats.size()];
                int index = 0;
                for (Integer seat : occupiedSeats) {
                    asientosOcupadosArray[index++] = seat;
                }

                Log.d(TAG, "✅ Asientos obtenidos - Ocupados: " + asientosOcupadosArray.length +
                        ", Disponibles: " + availableSeats);
                callback.onAsientosObtenidos(asientosOcupadosArray);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error obteniendo asientos: " + error);
                callback.onError(error);
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
            MyApp.logError(new Exception("Nombre del conductor es nulo en cargarReservasConductor"));
            callback.onError("Nombre del conductor es nulo");
            return;
        }

        // ✅ USANDO MyApp para obtener referencia
        DatabaseReference reservasRef = MyApp.getDatabaseReference("reservas");
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

                // ✅ Registrar evento de carga exitosa
                Map<String, Object> eventParams = new HashMap<>();
                eventParams.put("conductor_nombre", conductorNombre);
                eventParams.put("reservas_encontradas", reservasFiltradas);
                eventParams.put("horarios_asignados", horariosAsignados != null ? horariosAsignados.size() : 0);
                MyApp.logEvent("reservas_conductor_cargadas", eventParams);

                callback.onDriverReservationsLoaded(reservas);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error al cargar reservas del conductor: " + error.getMessage());
                MyApp.logError(error.toException());
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

        // ✅ USANDO MyApp para obtener referencia
        DatabaseReference reservaRef = MyApp.getDatabaseReference(
                "reservas/" + reservaId + "/estadoReserva"
        );

        reservaRef.setValue(nuevoEstado)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Estado de reserva actualizado exitosamente");

                    // ✅ Registrar evento en Analytics
                    Map<String, Object> eventParams = new HashMap<>();
                    eventParams.put("reserva_id", reservaId);
                    eventParams.put("nuevo_estado", nuevoEstado);
                    MyApp.logEvent("reserva_estado_actualizado", eventParams);

                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error actualizando estado de reserva: " + e.getMessage());
                    MyApp.logError(e);
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Carga todas las reservas con un estado específico
     */
    public void cargarReservasPorEstado(String estado, ReservationsCallback callback) {
        Log.d(TAG, "🔍 Cargando reservas por estado: " + estado);

        // ✅ USANDO MyApp para obtener referencia
        DatabaseReference reservasRef = MyApp.getDatabaseReference("reservas");

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

                // ✅ Registrar evento de carga
                Map<String, Object> eventParams = new HashMap<>();
                eventParams.put("estado", estado);
                eventParams.put("total_reservas", reservasCoincidentes);
                MyApp.logEvent("reservas_cargadas_por_estado", eventParams);

                callback.onReservationsLoaded(reservas);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error al cargar reservas por estado: " + error.getMessage());
                MyApp.logError(error.toException());
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

        // ✅ USANDO MyApp para obtener referencia
        DatabaseReference reservasRef = MyApp.getDatabaseReference("reservas");

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

                // ✅ Registrar evento
                Map<String, Object> eventParams = new HashMap<>();
                eventParams.put("conductor_uid", conductorUID);
                eventParams.put("filtro_estado", estado != null ? estado : "TODAS");
                eventParams.put("reservas_encontradas", reservasDelConductor);
                MyApp.logEvent("reservas_conductor_uid_cargadas", eventParams);

                callback.onReservationsLoaded(reservas);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error al cargar reservas del conductor: " + error.getMessage());
                MyApp.logError(error.toException());
                callback.onError("Error al cargar reservas: " + error.getMessage());
            }
        });
    }

    /**
     * Método para cargar las reservas en la interfaz de historial de reservas del usuario pasajero
     */
    public void obtenerHistorialUsuario(String usuarioId, HistorialCallback callback) {
        Log.d(TAG, "📋 Obteniendo historial de reservas para usuario: " + usuarioId);

        // ✅ USANDO MyApp para obtener referencia
        DatabaseReference ref = MyApp.getDatabaseReference("reservas");

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

                        // ✅ Registrar evento
                        Map<String, Object> eventParams = new HashMap<>();
                        eventParams.put("usuario_id", usuarioId);
                        eventParams.put("reservas_encontradas", reservasProcesadas);
                        MyApp.logEvent("historial_usuario_cargado", eventParams);

                        callback.onHistorialCargado(reservas);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "❌ Error al obtener historial: " + databaseError.getMessage());
                        MyApp.logError(databaseError.toException());
                        callback.onError(databaseError.getMessage());
                    }
                });
    }

    /**
     * 🔥 NUEVO: Método simplificado para obtener la referencia usando MyApp
     */
    public DatabaseReference getDatabaseReference(String path) {
        return MyApp.getDatabaseReference(path);
    }

    /**
     * 🔥 NUEVO: Método para verificar disponibilidad rápida
     */
    public void verificarDisponibilidadRapida(String horarioId, DisponibilidadCallback callback) {
        Log.d(TAG, "⚡ Verificando disponibilidad rápida para horario: " + horarioId);

        DatabaseReference ref = MyApp.getDatabaseReference("disponibilidadAsientos/" + horarioId + "/asientosDisponibles");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer asientosDisponibles = snapshot.getValue(Integer.class);
                    boolean disponible = asientosDisponibles != null && asientosDisponibles > 0;
                    Log.d(TAG, "⚡ Disponibilidad: " + (disponible ? "✅ Sí (" + asientosDisponibles + " asientos)" : "❌ No"));
                    callback.onDisponible(disponible);
                } else {
                    Log.w(TAG, "⚠️ No se encontró información de disponibilidad");
                    callback.onDisponible(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error en verificación rápida: " + error.getMessage());
                MyApp.logError(error.toException());
                callback.onError("Error verificando disponibilidad: " + error.getMessage());
            }
        });
    }
}