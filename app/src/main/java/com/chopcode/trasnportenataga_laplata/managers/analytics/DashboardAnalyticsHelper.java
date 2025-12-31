package com.chopcode.trasnportenataga_laplata.managers.analytics;

import com.chopcode.trasnportenataga_laplata.config.MyApp;
import com.chopcode.trasnportenataga_laplata.models.Usuario;

import java.util.HashMap;
import java.util.Map;

public class DashboardAnalyticsHelper {

    private static final String SCREEN_NAME = "InicioUsuarios";

    public void logScreenLoad() {
        Map<String, Object> params = new HashMap<>();
        params.put("pantalla", SCREEN_NAME);
        params.put("user_id", MyApp.getCurrentUserId());
        MyApp.logEvent("pantalla_inicio_usuario_inicio", params);
    }

    public void logScreenResume() {
        Map<String, Object> params = new HashMap<>();
        params.put("pantalla", SCREEN_NAME);
        params.put("user_id", MyApp.getCurrentUserId());
        MyApp.logEvent("pantalla_inicio_usuario_resume", params);
    }

    public void logUserLoaded(Usuario usuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("user_id", MyApp.getCurrentUserId());
        params.put("user_nombre", usuario.getNombre());
        params.put("user_email", usuario.getEmail());
        params.put("user_telefono", usuario.getTelefono() != null ? usuario.getTelefono() : "N/A");
        MyApp.logEvent("usuario_cargado_inicio", params);
    }

    public void logCountersLoaded(int reservasCount, int viajesCount) {
        Map<String, Object> params = new HashMap<>();
        params.put("user_id", MyApp.getCurrentUserId());
        params.put("reservas_activas", reservasCount);
        params.put("viajes_completados", viajesCount);
        MyApp.logEvent("estadisticas_usuario", params);
    }

    public void logSchedulesLoaded(int natagaCount, int laPlataCount) {
        Map<String, Object> params = new HashMap<>();
        params.put("user_id", MyApp.getCurrentUserId());
        params.put("horarios_nataga", natagaCount);
        params.put("horarios_laplata", laPlataCount);
        params.put("total_horarios", natagaCount + laPlataCount);
        MyApp.logEvent("horarios_cargados", params);
    }

    public void logScheduleLoadStart() {
        Map<String, Object> params = new HashMap<>();
        params.put("user_id", MyApp.getCurrentUserId());
        MyApp.logEvent("carga_horarios_inicio", params);
    }

    public void logButtonClick(String buttonName) {
        Map<String, Object> params = new HashMap<>();
        params.put("boton", buttonName);
        params.put("user_id", MyApp.getCurrentUserId());
        MyApp.logEvent("click_boton_inicio", params);
    }

    public void logMenuItemClick(String itemName) {
        Map<String, Object> params = new HashMap<>();
        params.put("menu_item", itemName);
        params.put("user_id", MyApp.getCurrentUserId());
        MyApp.logEvent("click_menu_item", params);
    }

    public void logRefresh() {
        Map<String, Object> params = new HashMap<>();
        params.put("user_id", MyApp.getCurrentUserId());
        MyApp.logEvent("actualizacion_manual", params);
    }

    public void logError(String errorType, String message) {
        Map<String, Object> params = new HashMap<>();
        params.put("tipo_error", errorType);
        params.put("mensaje", message);
        params.put("user_id", MyApp.getCurrentUserId());
        MyApp.logEvent("error_dashboard", params);
    }
}