package com.chopcode.trasnportenataga_laplata.services.reservations.driver;

import android.util.Log;

import androidx.annotation.NonNull;

import com.chopcode.trasnportenataga_laplata.config.MyApp;
import com.chopcode.trasnportenataga_laplata.managers.statistics.DriverStatisticsManager;
import com.chopcode.trasnportenataga_laplata.models.Reserva;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverReservationService {

    private static final String TAG = "DriverReservationService";
    private final DriverStatisticsManager statisticsManager;

    // Callback interfaces
    public interface DriverReservationsCallback {
        void onDriverReservationsLoaded(List<Reserva> reservas);
        void onError(String error);
    }

    public interface DriverReservationsByUIDCallback {
        void onReservationsLoaded(List<Reserva> reservas);
        void onError(String error);
    }

    public interface ReservationUpdateCallback {
        void onSuccess();
        void onError(String error);
    }

    // 🔥 NUEVO: Clase simple para estadísticas
    public static class SimpleDriverStats {
        public int totalReservas = 0;
        public int reservasConfirmadas = 0;
        public int reservasCanceladas = 0;
        public int reservasPendientes = 0;
        public double ingresosTotales = 0.0;

        public SimpleDriverStats() {}

        @Override
        public String toString() {
            return "SimpleDriverStats{" +
                    "totalReservas=" + totalReservas +
                    ", confirmadas=" + reservasConfirmadas +
                    ", canceladas=" + reservasCanceladas +
                    ", pendientes=" + reservasPendientes +
                    ", ingresos=" + ingresosTotales +
                    '}';
        }
    }

    // 🔥 NUEVO: Callback para estadísticas simples
    public interface SimpleStatsCallback {
        void onStatsLoaded(SimpleDriverStats stats);
        void onError(String error);
    }

    public DriverReservationService() {
        this.statisticsManager = new DriverStatisticsManager();
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
     * Carga TODAS las reservas de un conductor por UID
     */
    public void cargarReservasConductorPorUID(String conductorUID, String estado, DriverReservationsByUIDCallback callback) {
        Log.d(TAG, "👤 Cargando reservas para conductor UID: " + conductorUID);
        Log.d(TAG, "   - Estado filtro: " + (estado != null ? estado : "TODAS"));

        DatabaseReference reservasRef = MyApp.getDatabaseReference("reservas");

        reservasRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "✅ Datos de reservas recibidos - Total: " + snapshot.getChildrenCount());
                List<Reserva> reservas = new ArrayList<>();
                int reservasDelConductor = 0;

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Reserva reserva = dataSnapshot.getValue(Reserva.class);
                    if (reserva != null) {
                        String conductorId = reserva.getConductor();

                        boolean esDelConductor = conductorId != null && conductorId.equals(conductorUID);
                        boolean estadoCoincide;

                        if ("TODAS".equalsIgnoreCase(estado) || estado == null || estado.isEmpty()) {
                            estadoCoincide = true;
                        } else {
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

                Collections.sort(reservas, (r1, r2) -> Long.compare(r2.getFechaReserva(), r1.getFechaReserva()));

                Log.d(TAG, "📊 Reservas del conductor cargadas: " + reservasDelConductor);

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
     * 🔥 NUEVO: Método para obtener estadísticas simples del conductor
     */
    /**
     * 🔥 NUEVO: Método para obtener estadísticas simples del conductor
     */
    public void obtenerEstadisticasSimples(String conductorUID, SimpleStatsCallback callback) {
        Log.d(TAG, "📊 Obteniendo estadísticas simples para conductor: " + conductorUID);

        DatabaseReference reservasRef = MyApp.getDatabaseReference("reservas");

        reservasRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                SimpleDriverStats stats = new SimpleDriverStats();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Reserva reserva = dataSnapshot.getValue(Reserva.class);
                    if (reserva != null && conductorUID.equals(reserva.getConductor())) {
                        stats.totalReservas++;

                        String estado = reserva.getEstadoReserva();
                        if (estado != null) {
                            Double precioReserva = reserva.getPrecio(); // 🔥 CORREGIDO
                            switch (estado) {
                                case "Confirmada":
                                    stats.reservasConfirmadas++;
                                    stats.ingresosTotales += (precioReserva != null ? precioReserva : 0.0); // 🔥 CORREGIDO
                                    break;
                                case "Cancelada":
                                    stats.reservasCanceladas++;
                                    break;
                                case "Por confirmar":
                                    stats.reservasPendientes++;
                                    break;
                            }
                        }
                    }
                }

                Log.d(TAG, "✅ Estadísticas simples cargadas: " + stats);
                callback.onStatsLoaded(stats);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error al cargar estadísticas: " + error.getMessage());
                callback.onError("Error al cargar estadísticas: " + error.getMessage());
            }
        });
    }

    /**
     * 🔥 NUEVO: Método para obtener estadísticas diarias usando DriverStatisticsManager
     */
    public void obtenerEstadisticasDiarias(String conductorNombre, DriverStatisticsManager.StatisticsCallback callback) {
        Log.d(TAG, "📅 Obteniendo estadísticas diarias para conductor: " + conductorNombre);
        statisticsManager.calculateDailyStatistics(conductorNombre, callback);
    }

    /**
     * Actualiza el estado de una reserva (Confirmar/Cancelar)
     */
    public void actualizarEstadoReserva(String reservaId, String nuevoEstado, ReservationUpdateCallback callback) {
        Log.d(TAG, "🔄 Actualizando estado de reserva:");
        Log.d(TAG, "   - Reserva ID: " + reservaId);
        Log.d(TAG, "   - Nuevo estado: " + nuevoEstado);

        DatabaseReference reservaRef = MyApp.getDatabaseReference(
                "reservas/" + reservaId + "/estadoReserva"
        );

        reservaRef.setValue(nuevoEstado)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Estado de reserva actualizado exitosamente");

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
     * 🔥 MEJORADO: Método para cancelar reserva con liberación de asiento
     */
    public void cancelarReservaConLiberacion(String reservaId, String horarioId, int numeroAsiento, ReservationUpdateCallback callback) {
        Log.d(TAG, "🔄 Cancelando reserva con liberación:");
        Log.d(TAG, "   - Reserva ID: " + reservaId);
        Log.d(TAG, "   - Horario: " + horarioId);
        Log.d(TAG, "   - Asiento: " + numeroAsiento);

        // Primero actualizar el estado de la reserva
        actualizarEstadoReserva(reservaId, "Cancelada", new ReservationUpdateCallback() {
            @Override
            public void onSuccess() {
                // Luego liberar el asiento
                liberarAsientoReservado(horarioId, numeroAsiento, new ReservationUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "✅ Reserva cancelada y asiento liberado exitosamente");
                        callback.onSuccess();
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "⚠️ Reserva cancelada pero error liberando asiento: " + error);
                        // Aún así consideramos éxito porque la reserva se canceló
                        callback.onSuccess();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error cancelando reserva: " + error);
                callback.onError(error);
            }
        });
    }

    /**
     * Libera un asiento cuando se cancela una reserva
     */
    public void liberarAsientoReservado(String horarioId, int numeroAsiento, ReservationUpdateCallback callback) {
        Log.d(TAG, "🔄 Liberando asiento para cancelación - Horario: " + horarioId + ", Asiento: " + numeroAsiento);

        DatabaseReference seatRef = MyApp.getDatabaseReference(
                "asientos/" + horarioId + "/asiento" + numeroAsiento
        );

        seatRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Asiento liberado exitosamente");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error liberando asiento: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }
}