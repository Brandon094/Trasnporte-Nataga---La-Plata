package com.chopcode.trasnportenataga_laplata.managers.analytics;

import android.util.Log;

import com.chopcode.trasnportenataga_laplata.config.MyApp;
import com.chopcode.trasnportenataga_laplata.models.Vehiculo;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper dedicado a manejar eventos analíticos para reservas
 */
public class ReservationAnalyticsHelper {
    private static final String TAG = "ReservationAnalytics";
    private final String pantalla;

    public ReservationAnalyticsHelper(String pantalla) {
        this.pantalla = pantalla;
    }

    public void logEvent(String evento, Map<String, Object> params) {
        try {
            Map<String, Object> analyticsParams = new HashMap<>();
            analyticsParams.put("user_id", MyApp.getCurrentUserId());
            analyticsParams.put("pantalla", pantalla);
            analyticsParams.put("timestamp", System.currentTimeMillis());

            if (params != null) {
                analyticsParams.putAll(params);
            }

            MyApp.logEvent(evento, analyticsParams);
            Log.d(TAG, "📊 Evento registrado: " + evento);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando evento analítico: " + e.getMessage());
        }
    }

    // Métodos específicos para reservas
    public void logPantallaInicio() {
        Map<String, Object> params = new HashMap<>();
        params.put("accion", "inicio_pantalla");
        logEvent("pantalla_crear_reservas_inicio", params);
    }

    public void logDatosRecibidos(boolean tieneRuta, boolean tieneHorario) {
        Map<String, Object> params = new HashMap<>();
        params.put("tiene_ruta", tieneRuta ? 1 : 0);
        params.put("tiene_horario", tieneHorario ? 1 : 0);
        logEvent("datos_recibidos_intent", params);
    }

    public void logClickBoton(String boton) {
        Map<String, Object> params = new HashMap<>();
        params.put("boton", boton);
        logEvent("click_boton_" + boton, params);
    }

    public void logAsientoSeleccionado(int asiento) {
        Map<String, Object> params = new HashMap<>();
        params.put("asiento", asiento);
        logEvent("asiento_seleccionado", params);
    }

    public void logUsuarioCargado(String nombre, String telefono) {
        Map<String, Object> params = new HashMap<>();
        params.put("user_nombre", nombre != null ? nombre : "N/A");
        params.put("user_telefono", telefono != null ? telefono : "N/A");
        logEvent("usuario_cargado_crear_reserva", params);
    }

    public void logConductorCargado(String conductorId, String nombre, String telefono) {
        Map<String, Object> params = new HashMap<>();
        params.put("conductor_id", conductorId);
        params.put("conductor_nombre", nombre);
        params.put("conductor_telefono", telefono != null ? telefono : "N/A");
        logEvent("conductor_cargado_crear_reserva", params);
    }

    public void logVehiculoCargado(Vehiculo vehiculo, String conductorId) {
        Map<String, Object> params = new HashMap<>();
        params.put("conductor_id", conductorId);
        params.put("vehiculo_placa", vehiculo.getPlaca() != null ? vehiculo.getPlaca() : "N/A");
        params.put("vehiculo_modelo", vehiculo.getModelo() != null ? vehiculo.getModelo() : "N/A");
        params.put("vehiculo_capacidad", vehiculo.getCapacidad());
        logEvent("vehiculo_cargado_crear_reserva", params);
    }

    public void logAsientosCargados(int asientosOcupados, int capacidadTotal, String horario) {
        Map<String, Object> params = new HashMap<>();
        params.put("asientos_ocupados", asientosOcupados);
        params.put("capacidad_total", capacidadTotal);
        params.put("asientos_disponibles", capacidadTotal - asientosOcupados);
        params.put("horario", horario != null ? horario : "N/A");
        logEvent("asientos_cargados_crear_reserva", params);
    }

    public void logValidacionExitosa(int asiento, String ruta) {
        Map<String, Object> params = new HashMap<>();
        params.put("asiento", asiento);
        params.put("ruta", ruta != null ? ruta : "N/A");
        logEvent("validacion_exitosa_crear_reserva", params);
    }

    public void logValidacionFallida(String motivo) {
        Map<String, Object> params = new HashMap<>();
        params.put("motivo", motivo);
        logEvent("validacion_fallida", params);
    }

    public void logError(String tipoError, String mensaje) {
        Map<String, Object> params = new HashMap<>();
        params.put("tipo_error", tipoError);
        params.put("mensaje", mensaje);
        logEvent("error_" + tipoError, params);
    }
}