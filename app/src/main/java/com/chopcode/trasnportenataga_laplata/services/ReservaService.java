package com.chopcode.trasnportenataga_laplata.services;

import android.util.Log;
import com.chopcode.trasnportenataga_laplata.models.DisponibilidadAsientos;
import com.chopcode.trasnportenataga_laplata.models.Reserva;
import com.google.firebase.database.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReservaService {

    private DatabaseReference databaseReference;

    public interface HorarioCallback {
        void onHorarioEncontrado(String horarioId, String horarioHora);
        void onError(String error);
    }

    public interface ReservaCallback {
        void onReservaExitosa();
        void onError(String error);
    }

    public ReservaService() {
        this.databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    /**
     * 🔥 Busca el horario más próximo basado en la ruta y la hora actual
     */
    public void obtenerHorarioMasProximo(String rutaSeleccionada, HorarioCallback callback) {
        databaseReference.child("horarios").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long horaActual = System.currentTimeMillis();
                String horarioId = null;
                String horarioHora = null;
                long menorDiferencia = Long.MAX_VALUE;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String ruta = snapshot.child("ruta").getValue(String.class);
                    String hora = snapshot.child("hora").getValue(String.class);
                    String id = snapshot.getKey();

                    if (ruta != null && hora != null && ruta.equals(rutaSeleccionada)) {
                        long horaEnMillis = convertirHoraAMillis(hora.trim());

                        if (horaEnMillis > horaActual && (horaEnMillis - horaActual) < menorDiferencia) {
                            menorDiferencia = horaEnMillis - horaActual;
                            horarioId = id;
                            horarioHora = hora;
                        }
                    }
                }

                if (horarioId != null && horarioHora != null) {
                    callback.onHorarioEncontrado(horarioId, horarioHora);
                } else {
                    callback.onError("No hay horarios disponibles.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError("Error al obtener horarios: " + databaseError.getMessage());
            }
        });
    }

    /**
     * 🔥 Convierte una hora en formato "HH:mm a" a milisegundos
     */
    private long convertirHoraAMillis(String hora) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
            Date date = sdf.parse(hora);
            if (date != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                Calendar now = Calendar.getInstance();
                calendar.set(Calendar.YEAR, now.get(Calendar.YEAR));
                calendar.set(Calendar.MONTH, now.get(Calendar.MONTH));
                calendar.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));
                return calendar.getTimeInMillis();
            }
        } catch (ParseException e) {
            Log.e("Conversión", "Error al convertir hora: " + hora);
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 🔥 Guarda la reserva en Firebase y actualiza la disponibilidad de asientos
     */
    public void guardarReserva(String horarioId, int asientoSeleccionado, ReservaCallback callback) {
        DatabaseReference refDisponibilidad = databaseReference.child("disponibilidadAsientos").child(horarioId);

        refDisponibilidad.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    DisponibilidadAsientos disponibilidad = snapshot.getValue(DisponibilidadAsientos.class);

                    if (disponibilidad != null && disponibilidad.getAsientosDisponibles() > 0) {
                        // 🔹 Restar un asiento y actualizar Firebase
                        int nuevosAsientosDisponibles = disponibilidad.getAsientosDisponibles() - 1;
                        refDisponibilidad.child("asientosDisponibles").setValue(nuevosAsientosDisponibles);

                        // 🔹 Guardar la reserva en Firebase
                        registrarReserva(horarioId, asientoSeleccionado, callback);
                    } else {
                        callback.onError("No hay asientos disponibles.");
                    }
                } else {
                    callback.onError("Error: No se encontró disponibilidad.");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError("Error al consultar disponibilidad: " + error.getMessage());
            }
        });
    }

    /**
     * 🔥 Registra la reserva en Firebase
     */
    private void registrarReserva(String horarioId, int asientoSeleccionado, ReservaCallback callback) {
        String idReserva = UUID.randomUUID().toString();
        long fechaReserva = System.currentTimeMillis();

        Reserva reserva = new Reserva(
                idReserva, "usuario_demo1", horarioId, asientoSeleccionado,
                "uid_conductor_1", "vehiculo_1", 10000, "Natagá", "La Plata",
                "1h 30m", "Efectivo", "Pendiente", fechaReserva
        );

        databaseReference.child("reservas").child(idReserva).setValue(reserva)
                .addOnSuccessListener(aVoid -> callback.onReservaExitosa())
                .addOnFailureListener(e -> callback.onError("Error al guardar reserva: " + e.getMessage()));
    }
}
