package com.chopcode.trasnportenataga_laplata.managers;

import android.util.Log;
import com.chopcode.trasnportenataga_laplata.models.Reserva;
import com.chopcode.trasnportenataga_laplata.services.user.UserService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StatisticsManager {
    // ✅ NUEVO: Tag para logs
    private static final String TAG = "StatisticsManager";

    private static final int CAPACIDAD_TOTAL = 14;

    public interface StatisticsCallback {
        void onStatisticsCalculated(int reservasConfirmadas, int asientosDisponibles, double ingresos);
        void onError(String error);
    }

    public void calculateDailyStatistics(String conductorNombre, UserService.StatisticsCallback callback) {
        Log.d(TAG, "📊 Calculando estadísticas diarias para conductor: " + conductorNombre);

        if (conductorNombre == null) {
            Log.e(TAG, "❌ Nombre del conductor es nulo - no se pueden calcular estadísticas");
            callback.onError("Nombre del conductor es nulo");
            return;
        }

        DatabaseReference reservasRef = FirebaseDatabase.getInstance().getReference("reservas");
        Log.d(TAG, "🔍 Consultando reservas en Firebase Database...");

        reservasRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d(TAG, "✅ Datos de reservas recibidos - Total: " + snapshot.getChildrenCount());

                int confirmadasHoy = 0;
                int asientosOcupados = 0;
                double ingresosHoy = 0.0;

                long hoy = System.currentTimeMillis();
                long unDiaEnMillis = 24 * 60 * 60 * 1000;
                long inicioDelDia = hoy - (hoy % unDiaEnMillis);

                // Formatear fecha para logs
                String fechaHoy = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date(inicioDelDia));
                Log.d(TAG, "📅 Calculando estadísticas para el día: " + fechaHoy);
                Log.d(TAG, "   - Inicio del día: " + inicioDelDia);
                Log.d(TAG, "   - Capacidad total: " + CAPACIDAD_TOTAL);

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
                            Log.d(TAG, "   - Precio: $" + reserva.getPrecio());
                            Log.d(TAG, "   - Fecha: " + new Date(reserva.getFechaReserva()));

                            if ("Confirmada".equals(reserva.getEstadoReserva())) {
                                confirmadasHoy++;
                                asientosOcupados++;
                                ingresosHoy += reserva.getPrecio();
                                Log.d(TAG, "💰 Reserva CONFIRMADA - Sumando ingresos: $" + reserva.getPrecio());
                            } else if ("Por confirmar".equals(reserva.getEstadoReserva())) {
                                asientosOcupados++;
                                Log.d(TAG, "⏳ Reserva POR CONFIRMAR - Asiento ocupado");
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

                callback.onStatisticsCalculated(confirmadasHoy, asientosDisponibles, ingresosHoy);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "❌ Error en Firebase Database al calcular estadísticas:");
                Log.e(TAG, "   - Mensaje: " + error.getMessage());
                Log.e(TAG, "   - Código: " + error.getCode());
                Log.e(TAG, "   - Detalles: " + error.getDetails());
                callback.onError(error.getMessage());
            }
        });
    }

    public void updateIncomeInFirebase(String userId, double nuevosIngresos, UserService.IncomeUpdateCallback callback) {
        Log.d(TAG, "💰 Actualizando ingresos en Firebase:");
        Log.d(TAG, "   - UserId: " + userId);
        Log.d(TAG, "   - Nuevos ingresos: $" + nuevosIngresos);

        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "❌ UserId es nulo o vacío - no se puede actualizar ingresos");
            callback.onError("UserId no válido");
            return;
        }

        DatabaseReference conductorRef = FirebaseDatabase.getInstance()
                .getReference("conductores")
                .child(userId)
                .child("ingresosDiarios");

        Log.d(TAG, "📡 Actualizando en: conductores/" + userId + "/ingresosDiarios");

        conductorRef.setValue(nuevosIngresos)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Ingresos actualizados exitosamente en Firebase");
                    Log.d(TAG, "   - UserId: " + userId);
                    Log.d(TAG, "   - Ingresos: $" + nuevosIngresos);
                    callback.onSuccess(nuevosIngresos);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error actualizando ingresos en Firebase: " + e.getMessage());
                    Log.e(TAG, "   - UserId: " + userId);
                    Log.e(TAG, "   - Ingresos intentados: $" + nuevosIngresos);
                    callback.onError(e.getMessage());
                });
    }

    // ✅ NUEVO MÉTODO: Calcular estadísticas con rango de fechas personalizado
    public void calculateStatisticsWithDateRange(String conductorNombre, long fechaInicio, long fechaFin, StatisticsCallback callback) {
        Log.d(TAG, "📅 Calculando estadísticas con rango personalizado:");
        Log.d(TAG, "   - Conductor: " + conductorNombre);
        Log.d(TAG, "   - Fecha inicio: " + new Date(fechaInicio));
        Log.d(TAG, "   - Fecha fin: " + new Date(fechaFin));
        Log.d(TAG, "   - Rango: " + (fechaFin - fechaInicio) / (24 * 60 * 60 * 1000) + " días");

        if (conductorNombre == null) {
            Log.e(TAG, "❌ Nombre del conductor es nulo");
            callback.onError("Nombre del conductor es nulo");
            return;
        }

        if (fechaInicio > fechaFin) {
            Log.e(TAG, "❌ Rango de fechas inválido - fechaInicio > fechaFin");
            callback.onError("Rango de fechas inválido");
            return;
        }

        DatabaseReference reservasRef = FirebaseDatabase.getInstance().getReference("reservas");

        reservasRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d(TAG, "✅ Datos de reservas recibidos para rango personalizado");

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

                callback.onStatisticsCalculated(confirmadasEnRango, asientosDisponibles, ingresosEnRango);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "❌ Error calculando estadísticas con rango personalizado: " + error.getMessage());
                callback.onError(error.getMessage());
            }
        });
    }

    public interface IncomeUpdateCallback {
        void onSuccess(double nuevosIngresos);
        void onError(String error);
    }
}