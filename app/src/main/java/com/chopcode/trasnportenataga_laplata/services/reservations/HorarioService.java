package com.chopcode.trasnportenataga_laplata.services.reservations;

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

    // ✅ NUEVO: Tag para logs
    private static final String TAG = "HorarioService";

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
        Log.d(TAG, "🚀 Constructor - Inicializando servicio de horarios");
        this.databaseReference = FirebaseDatabase.getInstance().getReference("horarios");
        Log.d(TAG, "✅ Referencia a Firebase Database configurada: horarios");
    }

    public void cargarHorarios(HorarioCallback callback) {
        Log.d(TAG, "🔄 Iniciando carga de horarios desde Firebase");

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "✅ Datos de horarios recibidos - Snapshots: " + dataSnapshot.getChildrenCount());

                List<Horario> listaNataga = new ArrayList<>();
                List<Horario> listaLaPlata = new ArrayList<>();

                int contadorNataga = 0;
                int contadorLaPlata = 0;
                int contadorSinRuta = 0;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    // Obtener valores manualmente para asegurar que funcionen
                    String hora = snapshot.child("hora").getValue(String.class);
                    String ruta = snapshot.child("ruta").getValue(String.class);
                    String id = snapshot.getKey(); // 🔥 OBTENER EL ID DEL HORARIO

                    Log.d(TAG, "📋 Procesando horario - ID: " + id + ", Hora: " + hora + ", Ruta: " + ruta);

                    // Crear horario solo con los datos esenciales
                    Horario horario = new Horario();
                    horario.setId(id); // 🔥 ASIGNAR EL ID
                    horario.setHora(hora != null ? hora : "--:--");
                    horario.setRuta(ruta != null ? ruta : "Ruta no disponible");

                    if (ruta != null) {
                        ruta = ruta.trim();
                        if (ruta.equals("Natagá -> La Plata")) {
                            listaNataga.add(horario);
                            contadorNataga++;
                            Log.d(TAG, "📍 Agregado a Natagá -> La Plata: " + hora);
                        } else if (ruta.equals("La Plata -> Natagá")) {
                            listaLaPlata.add(horario);
                            contadorLaPlata++;
                            Log.d(TAG, "📍 Agregado a La Plata -> Natagá: " + hora);
                        } else {
                            // Ruta no reconocida
                            listaNataga.add(horario);
                            contadorSinRuta++;
                            Log.w(TAG, "⚠️ Ruta no reconocida: '" + ruta + "' - Agregado a Natagá por defecto");
                        }
                    } else {
                        // Si no hay ruta, agregar a alguna lista por defecto
                        listaNataga.add(horario);
                        contadorSinRuta++;
                        Log.w(TAG, "⚠️ Ruta es null - Agregado a Natagá por defecto");
                    }
                }

                Log.d(TAG, "📊 Resumen de horarios cargados:");
                Log.d(TAG, "   - Natagá -> La Plata: " + contadorNataga + " horarios");
                Log.d(TAG, "   - La Plata -> Natagá: " + contadorLaPlata + " horarios");
                Log.d(TAG, "   - Sin ruta definida: " + contadorSinRuta + " horarios");
                Log.d(TAG, "   - TOTAL: " + (contadorNataga + contadorLaPlata + contadorSinRuta) + " horarios");

                callback.onHorariosCargados(listaNataga, listaLaPlata);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "❌ Error en Firebase Database al cargar horarios:");
                Log.e(TAG, "   - Mensaje: " + databaseError.getMessage());
                Log.e(TAG, "   - Código: " + databaseError.getCode());
                Log.e(TAG, "   - Detalles: " + databaseError.getDetails());
                callback.onError("Error al cargar horarios: " + databaseError.getMessage());
            }
        });
    }

    /** Metodo para obtener el horario mas proximo con respecto a la hora actual */
    public void obtenerHorarioMasProximo(String rutaSeleccionada,
                                         HorarioEncontradoCallback callback) {
        Log.d(TAG, "🔍 Buscando horario más próximo para ruta: " + rutaSeleccionada);
        Log.d(TAG, "   - Hora actual: " + new SimpleDateFormat("HH:mm", Locale.US).format(new Date()));

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "✅ Datos recibidos para búsqueda de horario próximo");

                long horaActual = System.currentTimeMillis();
                String horarioId = null;
                String horarioHora = null;
                long menorDiferencia = Long.MAX_VALUE;
                String primerHorarioDelDiaSiguienteId = null;
                String primerHorarioDelDiaSiguienteHora = null;

                int totalHorariosProcesados = 0;
                int horariosCoincidentes = 0;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    totalHorariosProcesados++;
                    String ruta = snapshot.child("ruta").getValue(String.class);
                    String hora = snapshot.child("hora").getValue(String.class);
                    String id = snapshot.getKey();

                    if (ruta != null && hora != null && ruta.equals(rutaSeleccionada)) {
                        horariosCoincidentes++;
                        long horaEnMillis = convertirHoraAMillis(hora.trim());

                        Log.d(TAG, "⏰ Procesando horario - ID: " + id + ", Hora: " + hora +
                                ", HoraEnMillis: " + horaEnMillis + ", Diferencia: " + (horaEnMillis - horaActual));

                        // Si la hora es posterior a la actual y es la más cercana, la guardamos
                        if (horaEnMillis > horaActual && (horaEnMillis - horaActual) < menorDiferencia) {
                            menorDiferencia = horaEnMillis - horaActual;
                            horarioId = id;
                            horarioHora = hora;
                            Log.d(TAG, "🎯 Nuevo horario más próximo encontrado: " + hora + " (ID: " + id + ")");
                        }

                        // Guardamos el primer horario del día siguiente para usarlo si no hay más opciones hoy
                        if (primerHorarioDelDiaSiguienteId == null ||
                                (primerHorarioDelDiaSiguienteHora != null && horaEnMillis < convertirHoraAMillis(primerHorarioDelDiaSiguienteHora))) {
                            primerHorarioDelDiaSiguienteId = id;
                            primerHorarioDelDiaSiguienteHora = hora;
                            Log.d(TAG, "📅 Primer horario del día siguiente: " + hora + " (ID: " + id + ")");
                        }
                    }
                }

                Log.d(TAG, "📊 Resumen búsqueda horario próximo:");
                Log.d(TAG, "   - Total horarios procesados: " + totalHorariosProcesados);
                Log.d(TAG, "   - Horarios coincidentes con ruta: " + horariosCoincidentes);
                Log.d(TAG, "   - Horario encontrado para hoy: " + (horarioId != null ? horarioHora : "Ninguno"));
                Log.d(TAG, "   - Primer horario día siguiente: " + (primerHorarioDelDiaSiguienteHora != null ? primerHorarioDelDiaSiguienteHora : "Ninguno"));

                // Si encontramos un horario para hoy, lo usamos. Si no, tomamos el primero del día siguiente.
                if (horarioId != null && horarioHora != null) {
                    Log.d(TAG, "✅ Horario más próximo seleccionado: " + horarioHora + " (ID: " + horarioId + ")");
                    callback.onHorarioEncontrado(horarioId, horarioHora);
                } else if (primerHorarioDelDiaSiguienteId != null && primerHorarioDelDiaSiguienteHora != null) {
                    Log.d(TAG, "📅 Usando primer horario del día siguiente: " + primerHorarioDelDiaSiguienteHora + " (ID: " + primerHorarioDelDiaSiguienteId + ")");
                    callback.onHorarioEncontrado(primerHorarioDelDiaSiguienteId, primerHorarioDelDiaSiguienteHora);
                } else {
                    Log.w(TAG, "⚠️ No se encontraron horarios disponibles para la ruta: " + rutaSeleccionada);
                    callback.onError("No hay horarios disponibles.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "❌ Error en Firebase Database al buscar horario próximo:");
                Log.e(TAG, "   - Mensaje: " + databaseError.getMessage());
                Log.e(TAG, "   - Código: " + databaseError.getCode());
                Log.e(TAG, "   - Detalles: " + databaseError.getDetails());
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
        Log.d(TAG, "🕒 Convirtiendo hora a milisegundos: " + hora);

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

                long resultado = calendar.getTimeInMillis();
                Log.d(TAG, "✅ Hora convertida: " + hora + " → " + resultado + " ms");
                return resultado;
            } else {
                Log.w(TAG, "⚠️ No se pudo parsear la hora: " + hora);
            }
        } catch (ParseException e) {
            Log.e(TAG, "❌ Error al convertir hora: " + hora, e);
            Log.e(TAG, "   - Formato esperado: hh:mm a (ej: 02:30 PM)");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error inesperado al convertir hora: " + hora, e);
        }

        Log.w(TAG, "⚠️ Retornando -1 por error en conversión de hora: " + hora);
        return -1; // Retorna -1 si hay un error
    }
}