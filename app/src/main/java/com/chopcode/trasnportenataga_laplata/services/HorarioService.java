package com.chopcode.trasnportenataga_laplata.services;

import android.util.Log;
import com.chopcode.trasnportenataga_laplata.models.Horario;
import com.google.firebase.database.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HorarioService {

    private DatabaseReference databaseReference;
    /** Interfaz para cargar los horarios de forma asincronica */
    public interface HorarioCallback {
        void onHorariosCargados(List<Horario> listaNataga, List<Horario> listaLaPlata);
        void onError(String error);
    }
    /**
     * Interfaz para manejar la busqueda de Horarios de manera asíncrona.
     */
    public interface HorarioEncontradoCallback {
        void onHorarioEncontrado(String horarioId, String horarioHora);
        void onError(String error);
    }

    public HorarioService() {
        this.databaseReference = FirebaseDatabase.getInstance().getReference("horarios");
    }

    public void cargarHorarios(HorarioCallback callback) {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Horario> listaNataga = new ArrayList<>();
                List<Horario> listaLaPlata = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    // Obtener valores manualmente para asegurar que funcionen
                    String hora = snapshot.child("hora").getValue(String.class);
                    String ruta = snapshot.child("ruta").getValue(String.class);

                    // Crear horario solo con los datos esenciales
                    Horario horario = new Horario();
                    horario.setHora(hora != null ? hora : "--:--");
                    horario.setRuta(ruta != null ? ruta : "Ruta no disponible");

                    if (ruta != null) {
                        ruta = ruta.trim();
                        if (ruta.equals("Natagá -> La Plata")) {
                            listaNataga.add(horario);
                        } else if (ruta.equals("La Plata -> Natagá")) {
                            listaLaPlata.add(horario);
                        }
                    } else {
                        // Si no hay ruta, agregar a alguna lista por defecto
                        listaNataga.add(horario);
                    }
                }
                callback.onHorariosCargados(listaNataga, listaLaPlata);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError("Error al cargar horarios: " + databaseError.getMessage());
            }
        });
    }

    /** Metodo para obtener el horario mas proximo con respecto a la hora actual */
    public void obtenerHorarioMasProximo(String rutaSeleccionada,
                                         HorarioEncontradoCallback callback) {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long horaActual = System.currentTimeMillis();
                String horarioId = null;
                String horarioHora = null;
                long menorDiferencia = Long.MAX_VALUE;
                String primerHorarioDelDiaSiguienteId = null;
                String primerHorarioDelDiaSiguienteHora = null;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String ruta = snapshot.child("ruta").getValue(String.class);
                    String hora = snapshot.child("hora").getValue(String.class);
                    String id = snapshot.getKey();

                    if (ruta != null && hora != null && ruta.equals(rutaSeleccionada)) {
                        long horaEnMillis = convertirHoraAMillis(hora.trim());

                        // Si la hora es posterior a la actual y es la más cercana, la guardamos
                        if (horaEnMillis > horaActual && (horaEnMillis - horaActual) < menorDiferencia) {
                            menorDiferencia = horaEnMillis - horaActual;
                            horarioId = id;
                            horarioHora = hora;
                        }

                        // Guardamos el primer horario del día siguiente para usarlo si no hay más opciones hoy
                        if (primerHorarioDelDiaSiguienteId == null ||
                                (primerHorarioDelDiaSiguienteHora != null && horaEnMillis < convertirHoraAMillis(primerHorarioDelDiaSiguienteHora))) {
                            primerHorarioDelDiaSiguienteId = id;
                            primerHorarioDelDiaSiguienteHora = hora;
                        }
                    }
                }

                // Si encontramos un horario para hoy, lo usamos. Si no, tomamos el primero del día siguiente.
                if (horarioId != null && horarioHora != null) {
                    callback.onHorarioEncontrado(horarioId, horarioHora);
                } else if (primerHorarioDelDiaSiguienteId != null && primerHorarioDelDiaSiguienteHora != null) {
                    callback.onHorarioEncontrado(primerHorarioDelDiaSiguienteId, primerHorarioDelDiaSiguienteHora);
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
     * 🔥 Convierte una hora en formato "HH:mm" a milisegundos del día actual.
     *
     * @param hora Hora en formato "HH:mm"
     * @return Milisegundos desde la época Unix (1970)
     */
    private long convertirHoraAMillis(String hora) {
        try {
            // Usa formato de 24 horas (HH:mm)
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
            sdf.setLenient(false); // Para evitar conversiones erróneas

            Date date = sdf.parse(hora);
            if (date != null) {
                // Obtener la fecha actual
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);

                // Ajustar la fecha al día actual
                Calendar now = Calendar.getInstance();
                calendar.set(Calendar.YEAR, now.get(Calendar.YEAR));
                calendar.set(Calendar.MONTH, now.get(Calendar.MONTH));
                calendar.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

                return calendar.getTimeInMillis();
            }
        } catch (ParseException e) {
            Log.e("Conversión", "Error al convertir hora: " + hora, e);
        }
        return -1; // Retorna -1 si hay un error
    }
}
