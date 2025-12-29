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

public class DriverStatisticsManager extends StatisticsManager {
    private static final String TAG = "DriverStatisticsManager";
    private static final int CAPACIDAD_TOTAL = 14;

    public void calculateDailyStatistics(String conductorNombre, StatisticsCallback callback) {
        logInfo("📊 Calculando estadísticas diarias para conductor: " + conductorNombre);

        // ✅ Registrar evento analítico
        Map<String, Object> analyticsParams = new HashMap<>();
        analyticsParams.put("conductor_nombre", conductorNombre);
        analyticsParams.put("action", "calculate_daily_statistics");
        logAnalyticsEvent("driver_statistics_start", analyticsParams);

        if (conductorNombre == null || conductorNombre.isEmpty()) {
            logError("Nombre del conductor es nulo o vacío", null);
            callback.onError("Nombre del conductor no válido");
            return;
        }

        DatabaseReference reservasRef = getDatabaseReference("reservas");
        logInfo("Consultando reservas en Firebase Database...");

        reservasRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                logInfo("Datos de reservas recibidos - Total: " + snapshot.getChildrenCount());

                int confirmadasHoy = 0;
                int asientosOcupados = 0;
                double ingresosHoy = 0.0;

                long hoy = System.currentTimeMillis();
                long unDiaEnMillis = 24 * 60 * 60 * 1000;
                long inicioDelDia = hoy - (hoy % unDiaEnMillis);

                // Formatear fecha para logs
                String fechaHoy = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date(inicioDelDia));
                logInfo("Calculando estadísticas para el día: " + fechaHoy);
                logInfo("Capacidad total: " + CAPACIDAD_TOTAL);

                int totalReservasProcesadas = 0;
                int reservasDelConductor = 0;

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    totalReservasProcesadas++;
                    Reserva reserva = dataSnapshot.getValue(Reserva.class);
                    if (reserva != null) {
                        String conductorIdReserva = reserva.getConductor();
                        if (conductorIdReserva == null && dataSnapshot.hasChild("conductorId")) {
                            conductorIdReserva = dataSnapshot.child("conductorId").getValue(String.class);
                        }

                        boolean esDelConductor = conductorNombre.equals(conductorIdReserva);
                        boolean esDeHoy = reserva.getFechaReserva() >= inicioDelDia;

                        if (esDelConductor && esDeHoy) {
                            reservasDelConductor++;
                            Log.d(TAG, "🎯 Reserva del conductor encontrada:");
                            Log.d(TAG, "   - ID: " + reserva.getIdReserva());
                            Log.d(TAG, "   - Estado: " + reserva.getEstadoReserva());

                            if ("Confirmada".equals(reserva.getEstadoReserva())) {
                                confirmadasHoy++;
                                asientosOcupados++;
                                ingresosHoy += reserva.getPrecio();
                                logInfo("💰 Reserva CONFIRMADA - Sumando ingresos: $" + reserva.getPrecio());
                            } else if ("Por confirmar".equals(reserva.getEstadoReserva())) {
                                asientosOcupados++;
                                logInfo("⏳ Reserva POR CONFIRMAR - Asiento ocupado");
                            }
                        }
                    }
                }

                int asientosDisponibles = Math.max(0, CAPACIDAD_TOTAL - asientosOcupados);

                Log.d(TAG, "📈 ESTADÍSTICAS CALCULADAS:");
                Log.d(TAG, "   - Total reservas procesadas: " + totalReservasProcesadas);
                Log.d(TAG, "   - Reservas del conductor hoy: " + reservasDelConductor);
                Log.d(TAG, "   - Reservas confirmadas: " + confirmadasHoy);
                Log.d(TAG, "   - Asientos ocupados: " + asientosOcupados);
                Log.d(TAG, "   - Asientos disponibles: " + asientosDisponibles);
                Log.d(TAG, "   - Ingresos del día: $" + ingresosHoy);
                Log.d(TAG, "   - Capacidad utilizada: " + asientosOcupados + "/" + CAPACIDAD_TOTAL);

                // ✅ Registrar evento analítico con resultados
                Map<String, Object> resultParams = new HashMap<>();
                resultParams.put("conductor_nombre", conductorNombre);
                resultParams.put("reservas_confirmadas", confirmadasHoy);
                resultParams.put("asientos_disponibles", asientosDisponibles);
                resultParams.put("ingresos", ingresosHoy);
                resultParams.put("reservas_procesadas", totalReservasProcesadas);
                resultParams.put("fecha", fechaHoy);
                resultParams.put("capacidad_utilizada", asientosOcupados);
                resultParams.put("capacidad_total", CAPACIDAD_TOTAL);
                logAnalyticsEvent("driver_statistics_calculated", resultParams);

                callback.onStatisticsCalculated(confirmadasHoy, asientosDisponibles, ingresosHoy);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                logError("Error en Firebase Database al calcular estadísticas: " + error.getMessage(), null);

                // ✅ Registrar evento de error
                Map<String, Object> errorParams = new HashMap<>();
                errorParams.put("conductor_nombre", conductorNombre);
                errorParams.put("error_message", error.getMessage());
                errorParams.put("error_code", error.getCode());
                logAnalyticsEvent("driver_statistics_error", errorParams);

                callback.onError(error.getMessage());
            }
        });
    }

    public void updateIncomeInFirebase(String userId, double nuevosIngresos, IncomeUpdateCallback callback) {
        Log.d(TAG, "💰 Actualizando ingresos en Firebase:");
        Log.d(TAG, "   - UserId: " + userId);
        Log.d(TAG, "   - Nuevos ingresos: $" + nuevosIngresos);

        // ✅ Registrar evento de inicio de actualización
        Map<String, Object> startParams = new HashMap<>();
        startParams.put("user_id", userId);
        startParams.put("nuevos_ingresos", nuevosIngresos);
        startParams.put("timestamp", System.currentTimeMillis());
        logAnalyticsEvent("driver_income_update_start", startParams);

        if (userId == null || userId.isEmpty()) {
            logError("UserId es nulo o vacío - no se puede actualizar ingresos", null);
            callback.onError("UserId no válido");
            return;
        }

        // ✅ Usar MyApp para obtener la referencia
        DatabaseReference conductorRef = getDatabaseReference("conductores")
                .child(userId)
                .child("ingresosDiarios");

        logInfo("Actualizando en: conductores/" + userId + "/ingresosDiarios");

        conductorRef.setValue(nuevosIngresos)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Ingresos actualizados exitosamente en Firebase");

                    // ✅ Registrar evento de éxito
                    Map<String, Object> successParams = new HashMap<>();
                    successParams.put("user_id", userId);
                    successParams.put("ingresos_actualizados", nuevosIngresos);
                    successParams.put("timestamp", System.currentTimeMillis());
                    logAnalyticsEvent("driver_income_update_success", successParams);

                    callback.onSuccess(nuevosIngresos);
                })
                .addOnFailureListener(e -> {
                    logError("Error actualizando ingresos en Firebase: " + e.getMessage(), e);

                    // ✅ Registrar evento de error
                    Map<String, Object> errorParams = new HashMap<>();
                    errorParams.put("user_id", userId);
                    errorParams.put("error_message", e.getMessage());
                    errorParams.put("ingresos_intentados", nuevosIngresos);
                    errorParams.put("timestamp", System.currentTimeMillis());
                    logAnalyticsEvent("driver_income_update_error", errorParams);

                    callback.onError(e.getMessage());
                });
    }

    public void calculateStatisticsWithDateRange(String conductorNombre, long fechaInicio, long fechaFin, StatisticsCallback callback) {
        Log.d(TAG, "📅 Calculando estadísticas con rango personalizado:");
        Log.d(TAG, "   - Conductor: " + conductorNombre);
        Log.d(TAG, "   - Fecha inicio: " + new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date(fechaInicio)));
        Log.d(TAG, "   - Fecha fin: " + new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date(fechaFin)));

        // ✅ Registrar evento analítico
        Map<String, Object> analyticsParams = new HashMap<>();
        analyticsParams.put("conductor_nombre", conductorNombre);
        analyticsParams.put("fecha_inicio", fechaInicio);
        analyticsParams.put("fecha_fin", fechaFin);
        analyticsParams.put("rango_dias", (fechaFin - fechaInicio) / (24 * 60 * 60 * 1000));
        analyticsParams.put("timestamp", System.currentTimeMillis());
        logAnalyticsEvent("driver_statistics_range_start", analyticsParams);

        if (conductorNombre == null || conductorNombre.isEmpty()) {
            logError("Nombre del conductor es nulo", null);
            callback.onError("Nombre del conductor es nulo");
            return;
        }

        if (fechaInicio > fechaFin) {
            logError("Rango de fechas inválido - fechaInicio > fechaFin", null);
            callback.onError("Rango de fechas inválido");
            return;
        }

        // ✅ Usar MyApp para obtener la referencia
        DatabaseReference reservasRef = getDatabaseReference("reservas");

        reservasRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                logInfo("Datos de reservas recibidos para rango personalizado");

                int confirmadasEnRango = 0;
                int asientosOcupados = 0;
                double ingresosEnRango = 0.0;

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Reserva reserva = dataSnapshot.getValue(Reserva.class);
                    if (reserva != null) {
                        String conductorIdReserva = reserva.getConductor();
                        if (conductorIdReserva == null && dataSnapshot.hasChild("conductorId")) {
                            conductorIdReserva = dataSnapshot.child("conductorId").getValue(String.class);
                        }

                        boolean esDelConductor = conductorNombre.equals(conductorIdReserva);
                        boolean estaEnRango = reserva.getFechaReserva() >= fechaInicio && reserva.getFechaReserva() <= fechaFin;

                        if (esDelConductor && estaEnRango) {
                            if ("Confirmada".equals(reserva.getEstadoReserva())) {
                                confirmadasEnRango++;
                                asientosOcupados++;
                                ingresosEnRango += reserva.getPrecio();
                            } else if ("Por confirmar".equals(reserva.getEstadoReserva())) {
                                asientosOcupados++;
                            }
                        }
                    }
                }

                int asientosDisponibles = Math.max(0, CAPACIDAD_TOTAL - asientosOcupados);

                Log.d(TAG, "📊 ESTADÍSTICAS EN RANGO PERSONALIZADO:");
                Log.d(TAG, "   - Reservas confirmadas: " + confirmadasEnRango);
                Log.d(TAG, "   - Asientos disponibles: " + asientosDisponibles);
                Log.d(TAG, "   - Ingresos en rango: $" + ingresosEnRango);

                // ✅ Registrar evento con resultados
                Map<String, Object> resultParams = new HashMap<>();
                resultParams.put("conductor_nombre", conductorNombre);
                resultParams.put("reservas_confirmadas", confirmadasEnRango);
                resultParams.put("asientos_disponibles", asientosDisponibles);
                resultParams.put("ingresos_rango", ingresosEnRango);
                resultParams.put("fecha_inicio", new Date(fechaInicio).toString());
                resultParams.put("fecha_fin", new Date(fechaFin).toString());
                resultParams.put("timestamp", System.currentTimeMillis());
                logAnalyticsEvent("driver_statistics_range_calculated", resultParams);

                callback.onStatisticsCalculated(confirmadasEnRango, asientosDisponibles, ingresosEnRango);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                logError("Error calculando estadísticas con rango personalizado: " + error.getMessage(), null);

                // ✅ Registrar evento de error
                Map<String, Object> errorParams = new HashMap<>();
                errorParams.put("conductor_nombre", conductorNombre);
                errorParams.put("error_message", error.getMessage());
                errorParams.put("timestamp", System.currentTimeMillis());
                logAnalyticsEvent("driver_statistics_range_error", errorParams);

                callback.onError(error.getMessage());
            }
        });
    }

    // Métodos específicos del conductor
    public void calculateMonthlyIncome(String conductorNombre, int mes, int año, IncomeUpdateCallback callback) {
        Log.d(TAG, "📈 Calculando ingresos mensuales para conductor: " + conductorNombre + " - " + mes + "/" + año);

        // ✅ Registrar evento analítico
        Map<String, Object> analyticsParams = new HashMap<>();
        analyticsParams.put("conductor_nombre", conductorNombre);
        analyticsParams.put("mes", mes);
        analyticsParams.put("año", año);
        analyticsParams.put("timestamp", System.currentTimeMillis());
        logAnalyticsEvent("driver_monthly_income_calculation", analyticsParams);

        // Calcular fechas de inicio y fin del mes
        long fechaInicio = getStartOfMonth(mes, año);
        long fechaFin = getEndOfMonth(mes, año);

        calculateStatisticsWithDateRange(conductorNombre, fechaInicio, fechaFin,
                new StatisticsCallback() {
                    @Override
                    public void onStatisticsCalculated(int reservasConfirmadas, int asientosDisponibles, double ingresos) {
                        callback.onSuccess(ingresos);
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                }
        );
    }

    public void calculateWeeklyStatistics(String conductorNombre, StatisticsCallback callback) {
        Log.d(TAG, "📅 Calculando estadísticas semanales para conductor: " + conductorNombre);

        long hoy = System.currentTimeMillis();
        long unaSemanaEnMillis = 7 * 24 * 60 * 60 * 1000;
        long inicioDeSemana = hoy - unaSemanaEnMillis;

        calculateStatisticsWithDateRange(conductorNombre, inicioDeSemana, hoy, callback);
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