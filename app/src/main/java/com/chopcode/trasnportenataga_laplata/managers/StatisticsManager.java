package com.chopcode.trasnportenataga_laplata.managers;

import com.chopcode.trasnportenataga_laplata.models.Reserva;
import com.chopcode.trasnportenataga_laplata.services.UserService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class StatisticsManager {
    private static final int CAPACIDAD_TOTAL = 14;

    public interface StatisticsCallback {
        void onStatisticsCalculated(int reservasConfirmadas, int asientosDisponibles, double ingresos);
        void onError(String error);
    }

    public void calculateDailyStatistics(String conductorNombre, UserService.StatisticsCallback callback) {
        if (conductorNombre == null) {
            callback.onError("Nombre del conductor es nulo");
            return;
        }

        DatabaseReference reservasRef = FirebaseDatabase.getInstance().getReference("reservas");

        reservasRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                int confirmadasHoy = 0;
                int asientosOcupados = 0;
                double ingresosHoy = 0.0;

                long hoy = System.currentTimeMillis();
                long unDiaEnMillis = 24 * 60 * 60 * 1000;
                long inicioDelDia = hoy - (hoy % unDiaEnMillis);

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Reserva reserva = dataSnapshot.getValue(Reserva.class);
                    if (reserva != null) {
                        String conductorIdReserva = reserva.getConductor();
                        if (conductorIdReserva == null && dataSnapshot.hasChild("conductorId")) {
                            conductorIdReserva = dataSnapshot.child("conductorId").getValue(String.class);
                        }

                        boolean esDelConductor = conductorNombre.equals(conductorIdReserva);
                        boolean esDeHoy = reserva.getFechaReserva() >= inicioDelDia;

                        if (esDelConductor && esDeHoy) {
                            if ("Confirmada".equals(reserva.getEstadoReserva())) {
                                confirmadasHoy++;
                                asientosOcupados++;
                                ingresosHoy += reserva.getPrecio();
                            } else if ("Por confirmar".equals(reserva.getEstadoReserva())) {
                                asientosOcupados++;
                            }
                        }
                    }
                }

                int asientosDisponibles = Math.max(0, CAPACIDAD_TOTAL - asientosOcupados);
                callback.onStatisticsCalculated(confirmadasHoy, asientosDisponibles, ingresosHoy);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public void updateIncomeInFirebase(String userId, double nuevosIngresos, UserService.IncomeUpdateCallback callback) {
        DatabaseReference conductorRef = FirebaseDatabase.getInstance()
                .getReference("conductores")
                .child(userId)
                .child("ingresosDiarios");

        conductorRef.setValue(nuevosIngresos)
                .addOnSuccessListener(aVoid -> callback.onSuccess(nuevosIngresos))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public interface IncomeUpdateCallback {
        void onSuccess(double nuevosIngresos);
        void onError(String error);
    }
}