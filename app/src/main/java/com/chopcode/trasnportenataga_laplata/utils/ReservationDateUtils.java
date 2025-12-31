package com.chopcode.trasnportenataga_laplata.utils;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Utilidades para manejo de fechas y horas en reservas
 */
public class ReservationDateUtils {

    private static final String TAG = "ReservationDateUtils";

    /**
     * Obtiene la fecha del viaje formateada
     */
    public static String obtenerFechaDelViaje(String horarioHora) {
        Calendar calendar = Calendar.getInstance();
        Calendar ahora = Calendar.getInstance();

        if (horarioHora != null && esHorarioEnElPasado(horarioHora, ahora)) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            Log.d(TAG, "Horario en el pasado detectado: " + horarioHora);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d 'de' MMMM 'del' yyyy", new Locale("es", "ES"));
        String fecha = sdf.format(calendar.getTime());
        return fecha.substring(0, 1).toUpperCase() + fecha.substring(1);
    }

    /**
     * Verifica si un horario ya pasó
     */
    public static boolean esHorarioEnElPasado(String horarioSeleccionado, Calendar ahora) {
        try {
            SimpleDateFormat formato12h = new SimpleDateFormat("h:mm a", Locale.US);
            Date horaSeleccionadaDate = formato12h.parse(horarioSeleccionado);

            if (horaSeleccionadaDate != null) {
                Calendar calSeleccionado = Calendar.getInstance();
                calSeleccionado.setTime(horaSeleccionadaDate);

                int horaSeleccionada = calSeleccionado.get(Calendar.HOUR);
                int minutosSeleccionados = calSeleccionado.get(Calendar.MINUTE);
                int amPmSeleccionado = calSeleccionado.get(Calendar.AM_PM);

                int horaActual = ahora.get(Calendar.HOUR);
                int minutosActuales = ahora.get(Calendar.MINUTE);
                int amPmActual = ahora.get(Calendar.AM_PM);

                int horaSeleccionada24 = convertirA24Horas(horaSeleccionada, amPmSeleccionado);
                int horaActual24 = convertirA24Horas(horaActual, amPmActual);

                if (horaSeleccionada24 < horaActual24) {
                    return true;
                } else if (horaSeleccionada24 == horaActual24) {
                    return minutosSeleccionados <= minutosActuales;
                }
                return false;
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error al parsear horario: " + horarioSeleccionado, e);
            return esHorarioEnElPasadoSimple(horarioSeleccionado);
        }
        return false;
    }

    /**
     * Convierte hora 12h a 24h
     */
    private static int convertirA24Horas(int hora12, int amPm) {
        if (amPm == Calendar.PM && hora12 != 12) {
            return hora12 + 12;
        } else if (amPm == Calendar.AM && hora12 == 12) {
            return 0;
        }
        return hora12;
    }

    /**
     * Método simple para verificar si un horario ya pasó
     */
    private static boolean esHorarioEnElPasadoSimple(String horario) {
        if (horario == null) return false;

        Calendar ahora = Calendar.getInstance();
        int horaActual24 = ahora.get(Calendar.HOUR_OF_DAY);
        int minutoActual = ahora.get(Calendar.MINUTE);

        String horarioUpper = horario.toUpperCase();

        try {
            String[] partes = horario.split(":");
            if (partes.length >= 2) {
                int horaSeleccionada = Integer.parseInt(partes[0].trim());
                String[] minutosYAmPm = partes[1].split(" ");
                int minutosSeleccionados = Integer.parseInt(minutosYAmPm[0].trim());

                if (horarioUpper.contains("PM") && horaSeleccionada != 12) {
                    horaSeleccionada += 12;
                } else if (horarioUpper.contains("AM") && horaSeleccionada == 12) {
                    horaSeleccionada = 0;
                }

                if (horaSeleccionada < horaActual24) {
                    return true;
                } else if (horaSeleccionada == horaActual24) {
                    return minutosSeleccionados <= minutoActual;
                }
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error en fallback parser para: " + horario);
        }
        return false;
    }

    /**
     * Obtiene la hora actual formateada
     */
    public static String obtenerHoraActualFormateada() {
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.US);
        return sdf.format(new Date());
    }

    /**
     * Formatea una fecha en formato español
     */
    public static String formatearFechaEspanol(Date fecha) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d 'de' MMMM 'del' yyyy", new Locale("es", "ES"));
        String fechaStr = sdf.format(fecha);
        return fechaStr.substring(0, 1).toUpperCase() + fechaStr.substring(1);
    }

    /**
     * Calcula el tiempo estimado basado en la ruta
     */
    public static String calcularTiempoEstimado(String rutaSeleccionada) {
        if (rutaSeleccionada == null) {
            return "55 min";
        }

        return rutaSeleccionada.contains("Natagá -> La Plata") ? "60 min" : "55 min";
    }
}