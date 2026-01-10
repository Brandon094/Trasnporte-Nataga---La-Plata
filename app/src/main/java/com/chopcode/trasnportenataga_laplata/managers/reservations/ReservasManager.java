package com.chopcode.trasnportenataga_laplata.managers.reservations;

import android.util.Log;

import com.chopcode.trasnportenataga_laplata.config.MyApp;
import com.chopcode.trasnportenataga_laplata.managers.notificactions.NotificationManager;
import com.chopcode.trasnportenataga_laplata.models.Reserva;
import com.chopcode.trasnportenataga_laplata.services.reservations.driver.DriverReservationService;
import com.chopcode.trasnportenataga_laplata.services.user.UserService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ReservasManager {
    private static final String TAG = "ReservasManager";

    private final DriverReservationService driverReservationService;
    private final UserService userService;
    private NotificationManager notificationManager;

    private DatabaseReference reservasRef;
    private ValueEventListener reservasListener;

    public interface DriverDataCallback {
        void onDriverDataLoaded(String nombre, String telefono, String placa, List<String> horarios);
        void onError(String error);
    }

    public interface RealTimeCallback {
        void onDataChanged(List<Reserva> reservas, int nuevasConfirmadas);
        void onError(String error);
    }

    public interface ReservationsCallback {
        void onReservationsLoaded(List<Reserva> reservas);
        void onError(String error);
    }

    public interface UpdateCallback {
        void onSuccess();
        void onError(String error);
    }

    // 🔥 NUEVO: Callback para estadísticas
    public interface StatsCallback {
        void onStatsLoaded(DriverReservationService.SimpleDriverStats stats);
        void onError(String error);
    }

    public ReservasManager() {
        this.driverReservationService = new DriverReservationService();
        this.userService = new UserService();
        Log.d(TAG, "✅ ReservasManager inicializado con DriverReservationService");
    }

    public void setNotificationManager(NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
    }

    public void loadDriverData(String userId, DriverDataCallback callback) {
        Log.d(TAG, "Cargando datos del conductor: " + userId);

        userService.loadDriverData(userId, new UserService.DriverDataCallback() {
            @Override
            public void onDriverDataLoaded(String nombre, String telefono, String placa, List<String> horarios) {
                callback.onDriverDataLoaded(nombre, telefono, placa, horarios);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void setupRealTimeListener(String conductorNombre, RealTimeCallback callback) {
        Log.d(TAG, "Configurando listener tiempo real para: " + conductorNombre);

        try {
            if (reservasRef != null && reservasListener != null) {
                reservasRef.removeEventListener(reservasListener);
            }

            reservasRef = MyApp.getDatabaseReference("reservas");

            reservasListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Log.d(TAG, "Datos cambiados en Firebase");

                    int nuevasConfirmadas = 0;
                    List<Reserva> reservasTiempoReal = new ArrayList<>();

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        try {
                            Reserva reserva = snapshot.getValue(Reserva.class);
                            if (reserva != null &&
                                    conductorNombre.equals(reserva.getConductor()) &&
                                    "Por confirmar".equals(reserva.getEstadoReserva())) {

                                reservasTiempoReal.add(reserva);

                                if ("Confirmada".equals(reserva.getEstadoReserva())) {
                                    nuevasConfirmadas++;
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error procesando reserva: " + e.getMessage());
                            MyApp.logError(e);
                        }
                    }

                    callback.onDataChanged(reservasTiempoReal, nuevasConfirmadas);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, "Error en listener tiempo real: " + databaseError.getMessage());
                    callback.onError(databaseError.getMessage());
                }
            };

            reservasRef.addValueEventListener(reservasListener);
            Log.d(TAG, "Listener en tiempo real configurado");

        } catch (Exception e) {
            Log.e(TAG, "Error configurando listener: " + e.getMessage());
            callback.onError(e.getMessage());
        }
    }

    public void loadReservations(String conductorNombre, ReservationsCallback callback) {
        Log.d(TAG, "Cargando reservas para: " + conductorNombre);

        driverReservationService.cargarReservasConductor(conductorNombre, new ArrayList<>(),
                new DriverReservationService.DriverReservationsCallback() {
                    @Override
                    public void onDriverReservationsLoaded(List<Reserva> reservas) {
                        Log.d(TAG, "✅ " + reservas.size() + " reservas cargadas desde DriverReservationService");

                        // Filtrar solo las reservas "Por confirmar"
                        List<Reserva> reservasPorConfirmar = new ArrayList<>();
                        for (Reserva reserva : reservas) {
                            if ("Por confirmar".equals(reserva.getEstadoReserva())) {
                                reservasPorConfirmar.add(reserva);
                            }
                        }

                        Log.d(TAG, "📊 Reservas por confirmar: " + reservasPorConfirmar.size());
                        callback.onReservationsLoaded(reservasPorConfirmar);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ Error cargando reservas: " + error);
                        callback.onError(error);
                    }
                });
    }

    // 🔥 NUEVO: Método para cargar todas las reservas del conductor
    public void loadAllDriverReservations(String conductorUID, String estadoFiltro, ReservationsCallback callback) {
        Log.d(TAG, "Cargando TODAS las reservas del conductor UID: " + conductorUID);

        driverReservationService.cargarReservasConductorPorUID(conductorUID, estadoFiltro,
                new DriverReservationService.DriverReservationsByUIDCallback() {
                    @Override
                    public void onReservationsLoaded(List<Reserva> reservas) {
                        Log.d(TAG, "✅ " + reservas.size() + " reservas cargadas (todas)");
                        callback.onReservationsLoaded(reservas);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ Error cargando todas las reservas: " + error);
                        callback.onError(error);
                    }
                });
    }

    // 🔥 NUEVO: Método para obtener estadísticas del conductor
    public void loadDriverStatistics(String conductorUID, StatsCallback callback) {
        Log.d(TAG, "📊 Cargando estadísticas del conductor: " + conductorUID);

        driverReservationService.obtenerEstadisticasSimples(conductorUID,
                new DriverReservationService.SimpleStatsCallback() {
                    @Override
                    public void onStatsLoaded(DriverReservationService.SimpleDriverStats stats) {
                        Log.d(TAG, "✅ Estadísticas cargadas: " + stats);
                        callback.onStatsLoaded(stats);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ Error cargando estadísticas: " + error);
                        callback.onError(error);
                    }
                });
    }

    public void updateReservationStatus(Reserva reserva, String nuevoEstado, UpdateCallback callback) {
        Log.d(TAG, "Actualizando estado de reserva a: " + nuevoEstado);

        driverReservationService.actualizarEstadoReserva(reserva.getIdReserva(), nuevoEstado,
                new DriverReservationService.ReservationUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        // Enviar notificación al pasajero si es necesario
                        if (notificationManager != null) {
                            sendNotificationToPassenger(reserva, nuevoEstado);
                        }
                        callback.onSuccess();
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                });
    }

    // 🔥 NUEVO: Método mejorado para cancelar reserva con liberación de asiento
    public void cancelReservationWithSeatRelease(Reserva reserva, UpdateCallback callback) {
        Log.d(TAG, "Cancelando reserva con liberación de asiento:");
        Log.d(TAG, "   - Reserva ID: " + reserva.getIdReserva());
        Log.d(TAG, "   - Horario: " + reserva.getHorarioId());
        Log.d(TAG, "   - Asiento: " + reserva.getPuestoReservado());

        if (reserva.getHorarioId() == null || reserva.getPuestoReservado() <= 0) {
            Log.w(TAG, "⚠️ No hay suficiente información para liberar el asiento");
            // Cancelar solo la reserva
            updateReservationStatus(reserva, "Cancelada", callback);
            return;
        }

        driverReservationService.cancelarReservaConLiberacion(
                reserva.getIdReserva(),
                reserva.getHorarioId(),
                reserva.getPuestoReservado(),
                new DriverReservationService.ReservationUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        // Enviar notificación de cancelación
                        if (notificationManager != null) {
                            sendNotificationToPassenger(reserva, "Cancelada");
                        }
                        callback.onSuccess();
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                });
    }

    private void sendNotificationToPassenger(Reserva reserva, String estado) {
        if (notificationManager == null) return;

        if ("Confirmada".equals(estado)) {
            notificationManager.notificarReservaConfirmadaAlPasajero(
                    reserva.getUsuarioId(),
                    reserva.getConductor(),
                    reserva.getOrigen() + " → " + reserva.getDestino(),
                    "Próximo viaje",
                    reserva.getPuestoReservado(),
                    "N/A", // Placa (deberías obtenerla de otra manera)
                    "Vehículo de transporte",
                    new NotificationManager.NotificationCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "✅ Notificación enviada al pasajero");
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "❌ Error enviando notificación: " + error);
                        }
                    }
            );
        } else if ("Cancelada".equals(estado)) {
            notificationManager.notificarReservaCanceladaAlPasajero(
                    reserva.getUsuarioId(),
                    reserva.getConductor(),
                    reserva.getOrigen() + " → " + reserva.getDestino(),
                    "Por decisión del conductor",
                    new NotificationManager.NotificationCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "✅ Notificación de cancelación enviada");
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "❌ Error enviando notificación de cancelación: " + error);
                        }
                    }
            );
        }
    }

    public void cleanup() {
        if (reservasRef != null && reservasListener != null) {
            reservasRef.removeEventListener(reservasListener);
            Log.d(TAG, "✅ Listener de Firebase removido");
        }
    }

    // 🔥 NUEVO: Método para obtener el DriverReservationService
    public DriverReservationService getDriverReservationService() {
        return driverReservationService;
    }
}