package com.chopcode.trasnportenataga_laplata.managers.statistics;

import android.util.Log;

import com.chopcode.trasnportenataga_laplata.models.Reserva;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PassengerStatisticsManager extends StatisticsManager {
    private static final String TAG = "PassengerStatisticsManager";

    public interface PassengerStatisticsCallback {
        void onStatisticsCalculated(int viajesCompletados, double totalGastado,
                                    int reservasPendientes, int reservasCanceladas);
        void onError(String error);
    }

    public interface TravelHistoryCallback {
        void onTravelHistoryCalculated(int viajesRealizados, int destinosVisitados, String ultimoViaje);
        void onError(String error);
    }

    public void calculatePassengerStatistics(String pasajeroId, PassengerStatisticsCallback callback) {
        Log.d(TAG, "👤 Calculando estadísticas para pasajero: " + pasajeroId);

        // ✅ Registrar evento analítico
        Map<String, Object> analyticsParams = new HashMap<>();
        analyticsParams.put("pasajero_id", pasajeroId);
        analyticsParams.put("user_type", "passenger");
        analyticsParams.put("timestamp", System.currentTimeMillis());
        logAnalyticsEvent("passenger_statistics_start", analyticsParams);

        if (pasajeroId == null || pasajeroId.isEmpty()) {
            logError("ID del pasajero es nulo o vacío", null);
            callback.onError("ID del pasajero no válido");
            return;
        }

        DatabaseReference reservasRef = getDatabaseReference("reservas");
        logInfo("Consultando reservas del pasajero...");

        reservasRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                logInfo("Datos de reservas recibidos para pasajero");

                int viajesCompletados = 0;
                double totalGastado = 0.0;
                int reservasPendientes = 0;
                int reservasCanceladas = 0;

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Reserva reserva = dataSnapshot.getValue(Reserva.class);
                    if (reserva != null && pasajeroId.equals(reserva.getUsuarioId())) {
                        String estado = reserva.getEstadoReserva();

                        Log.d(TAG, "📋 Reserva encontrada:");
                        Log.d(TAG, "   - ID: " + reserva.getIdReserva());
                        Log.d(TAG, "   - Estado: " + estado);
                        Log.d(TAG, "   - Precio: $" + reserva.getPrecio());

                        switch (estado) {
                            case "Confirmada":
                                viajesCompletados++;
                                totalGastado += reserva.getPrecio();
                                Log.d(TAG, "✅ Viaje COMPLETADO");
                                break;
                            case "Por confirmar":
                                reservasPendientes++;
                                Log.d(TAG, "⏳ Reserva PENDIENTE");
                                break;
                            case "Cancelada":
                                reservasCanceladas++;
                                Log.d(TAG, "❌ Reserva CANCELADA");
                                break;
                        }
                    }
                }

                Log.d(TAG, "📊 ESTADÍSTICAS DEL PASAJERO:");
                Log.d(TAG, "   - Viajes completados: " + viajesCompletados);
                Log.d(TAG, "   - Total gastado: $" + totalGastado);
                Log.d(TAG, "   - Reservas pendientes: " + reservasPendientes);
                Log.d(TAG, "   - Reservas canceladas: " + reservasCanceladas);

                // ✅ Registrar evento analítico con resultados
                Map<String, Object> resultParams = new HashMap<>();
                resultParams.put("pasajero_id", pasajeroId);
                resultParams.put("viajes_completados", viajesCompletados);
                resultParams.put("total_gastado", totalGastado);
                resultParams.put("reservas_pendientes", reservasPendientes);
                resultParams.put("reservas_canceladas", reservasCanceladas);
                resultParams.put("timestamp", System.currentTimeMillis());
                logAnalyticsEvent("passenger_statistics_calculated", resultParams);

                callback.onStatisticsCalculated(viajesCompletados, totalGastado, reservasPendientes, reservasCanceladas);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                logError("Error calculando estadísticas del pasajero: " + error.getMessage(), null);

                // ✅ Registrar evento de error
                Map<String, Object> errorParams = new HashMap<>();
                errorParams.put("pasajero_id", pasajeroId);
                errorParams.put("error_message", error.getMessage());
                errorParams.put("timestamp", System.currentTimeMillis());
                logAnalyticsEvent("passenger_statistics_error", errorParams);

                callback.onError(error.getMessage());
            }
        });
    }

    public void calculateMonthlyExpenses(String pasajeroId, int mes, int año,
                                         final IncomeUpdateCallback callback) {
        Log.d(TAG, "💰 Calculando gastos mensuales para pasajero: " + pasajeroId + " - " + mes + "/" + año);

        // ✅ Registrar evento analítico
        Map<String, Object> analyticsParams = new HashMap<>();
        analyticsParams.put("pasajero_id", pasajeroId);
        analyticsParams.put("mes", mes);
        analyticsParams.put("año", año);
        analyticsParams.put("timestamp", System.currentTimeMillis());
        logAnalyticsEvent("passenger_monthly_expenses_start", analyticsParams);

        // Calcular fechas de inicio y fin del mes
        long fechaInicio = getStartOfMonth(mes, año);
        long fechaFin = getEndOfMonth(mes, año);

        DatabaseReference reservasRef = getDatabaseReference("reservas");

        reservasRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                double gastosMensuales = 0.0;

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Reserva reserva = dataSnapshot.getValue(Reserva.class);
                    if (reserva != null &&
                            pasajeroId.equals(reserva.getUsuarioId()) &&
                            "Confirmada".equals(reserva.getEstadoReserva()) &&
                            reserva.getFechaReserva() >= fechaInicio &&
                            reserva.getFechaReserva() <= fechaFin) {

                        gastosMensuales += reserva.getPrecio();
                    }
                }

                Log.d(TAG, "📅 Gastos mensuales calculados: $" + gastosMensuales);

                // ✅ Registrar evento con resultados
                Map<String, Object> resultParams = new HashMap<>();
                resultParams.put("pasajero_id", pasajeroId);
                resultParams.put("mes", mes);
                resultParams.put("año", año);
                resultParams.put("gastos_mensuales", gastosMensuales);
                resultParams.put("timestamp", System.currentTimeMillis());
                logAnalyticsEvent("passenger_monthly_expenses_calculated", resultParams);

                callback.onSuccess(gastosMensuales);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                logError("Error calculando gastos mensuales: " + error.getMessage(), null);

                // ✅ Registrar evento de error
                Map<String, Object> errorParams = new HashMap<>();
                errorParams.put("pasajero_id", pasajeroId);
                errorParams.put("mes", mes);
                errorParams.put("año", año);
                errorParams.put("error_message", error.getMessage());
                errorParams.put("timestamp", System.currentTimeMillis());
                logAnalyticsEvent("passenger_monthly_expenses_error", errorParams);

                callback.onError(error.getMessage());
            }
        });
    }

    public void getTravelHistory(String pasajeroId, TravelHistoryCallback callback) {
        Log.d(TAG, "🕒 Obteniendo historial de viajes para pasajero: " + pasajeroId);

        // ✅ Registrar evento analítico
        Map<String, Object> analyticsParams = new HashMap<>();
        analyticsParams.put("pasajero_id", pasajeroId);
        analyticsParams.put("timestamp", System.currentTimeMillis());
        logAnalyticsEvent("passenger_travel_history_start", analyticsParams);

        DatabaseReference reservasRef = getDatabaseReference("reservas");

        reservasRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                int viajesRealizados = 0;
                java.util.Set<String> destinosUnicos = new java.util.HashSet<>();
                String ultimoViaje = "Ninguno";
                long ultimaFecha = 0;

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Reserva reserva = dataSnapshot.getValue(Reserva.class);
                    if (reserva != null &&
                            pasajeroId.equals(reserva.getUsuarioId()) &&
                            "Confirmada".equals(reserva.getEstadoReserva())) {

                        viajesRealizados++;

                        // Agregar destino a conjunto de destinos únicos
                        String destino = reserva.getDestino();
                        if (destino != null && !destino.isEmpty()) {
                            destinosUnicos.add(destino);
                        }

                        // Encontrar el viaje más reciente
                        if (reserva.getFechaReserva() > ultimaFecha) {
                            ultimaFecha = reserva.getFechaReserva();
                            ultimoViaje = reserva.getOrigen() + " → " + reserva.getDestino() +
                                    " (" + new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date(reserva.getFechaReserva())) + ")";
                        }
                    }
                }

                int destinosVisitados = destinosUnicos.size();

                Log.d(TAG, "📋 HISTORIAL DE VIAJES:");
                Log.d(TAG, "   - Viajes realizados: " + viajesRealizados);
                Log.d(TAG, "   - Destinos visitados: " + destinosVisitados);
                Log.d(TAG, "   - Último viaje: " + ultimoViaje);

                // ✅ Registrar evento con resultados
                Map<String, Object> resultParams = new HashMap<>();
                resultParams.put("pasajero_id", pasajeroId);
                resultParams.put("viajes_realizados", viajesRealizados);
                resultParams.put("destinos_visitados", destinosVisitados);
                resultParams.put("ultimo_viaje", ultimoViaje);
                resultParams.put("timestamp", System.currentTimeMillis());
                logAnalyticsEvent("passenger_travel_history_calculated", resultParams);

                callback.onTravelHistoryCalculated(viajesRealizados, destinosVisitados, ultimoViaje);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                logError("Error obteniendo historial de viajes: " + error.getMessage(), null);

                // ✅ Registrar evento de error
                Map<String, Object> errorParams = new HashMap<>();
                errorParams.put("pasajero_id", pasajeroId);
                errorParams.put("error_message", error.getMessage());
                errorParams.put("timestamp", System.currentTimeMillis());
                logAnalyticsEvent("passenger_travel_history_error", errorParams);

                callback.onError(error.getMessage());
            }
        });
    }

    public void calculateWeeklyExpenses(String pasajeroId, IncomeUpdateCallback callback) {
        Log.d(TAG, "💰 Calculando gastos semanales para pasajero: " + pasajeroId);

        long hoy = System.currentTimeMillis();
        long unaSemanaEnMillis = 7 * 24 * 60 * 60 * 1000;
        long inicioDeSemana = hoy - unaSemanaEnMillis;

        // ✅ Registrar evento analítico
        Map<String, Object> analyticsParams = new HashMap<>();
        analyticsParams.put("pasajero_id", pasajeroId);
        analyticsParams.put("fecha_inicio", inicioDeSemana);
        analyticsParams.put("fecha_fin", hoy);
        analyticsParams.put("timestamp", System.currentTimeMillis());
        logAnalyticsEvent("passenger_weekly_expenses_start", analyticsParams);

        DatabaseReference reservasRef = getDatabaseReference("reservas");

        reservasRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                double gastosSemanales = 0.0;

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Reserva reserva = dataSnapshot.getValue(Reserva.class);
                    if (reserva != null &&
                            pasajeroId.equals(reserva.getUsuarioId()) &&
                            "Confirmada".equals(reserva.getEstadoReserva()) &&
                            reserva.getFechaReserva() >= inicioDeSemana &&
                            reserva.getFechaReserva() <= hoy) {

                        gastosSemanales += reserva.getPrecio();
                    }
                }

                Log.d(TAG, "📅 Gastos semanales calculados: $" + gastosSemanales);
                callback.onSuccess(gastosSemanales);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                logError("Error calculando gastos semanales: " + error.getMessage(), null);
                callback.onError(error.getMessage());
            }
        });
    }

    private long getStartOfMonth(int mes, int año) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(año, mes - 1);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getEndOfMonth(int mes, int año) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(año, mes - 1, 1, 23, 59, 59);
        calendar.set(java.util.Calendar.MILLISECOND, 999);
        calendar.add(java.util.Calendar.MONTH, 1);
        calendar.add(java.util.Calendar.DATE, -1);
        return calendar.getTimeInMillis();
    }
}