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

    // 🔥 NUEVAS INTERFACES DEL RESERVATION MANAGER
    public interface ReservationsCallback {
        void onReservationsLoaded(List<Reserva> reservas);
        void onError(String error);
    }

    public interface ReservationUpdateCallback {
        void onSuccess();
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

        // Asegúrate de que asientoSeleccionado sea mayor a 0
        if (asientoSeleccionado <= 0) {
            Log.e(TAG, "❌ Número de asiento inválido: " + asientoSeleccionado);
            callback.onError("Número de asiento inválido");
            return;
        }

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
}