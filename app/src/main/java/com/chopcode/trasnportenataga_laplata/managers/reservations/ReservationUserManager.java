package com.chopcode.trasnportenataga_laplata.managers.reservations;

import android.util.Log;

import com.chopcode.trasnportenataga_laplata.config.MyApp;
import com.chopcode.trasnportenataga_laplata.managers.analytics.ReservationAnalyticsHelper;
import com.chopcode.trasnportenataga_laplata.models.Usuario;
import com.chopcode.trasnportenataga_laplata.services.user.UserService;

import java.util.HashMap;
import java.util.Map;

/**
 * Manager para cargar y manejar información del usuario en reservas
 */
public class ReservationUserManager {

    private static final String TAG = "ReservationUserManager";

    private final ReservationAnalyticsHelper analyticsHelper;
    private final UserService userService;

    // Callback interface
    public interface UserDataCallback {
        void onUserDataLoaded(String usuarioId, String usuarioNombre, String usuarioTelefono);
        void onError(String error);
    }

    // Data
    private String usuarioId;
    private String usuarioNombre;
    private String usuarioTelefono;

    public ReservationUserManager(ReservationAnalyticsHelper analyticsHelper) {
        this.analyticsHelper = analyticsHelper;
        this.userService = new UserService();
    }

    /**
     * Carga información del usuario autenticado
     */
    public void loadAuthenticatedUser(UserDataCallback callback) {
        String userId = MyApp.getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "No se pudo obtener el ID del usuario");
            analyticsHelper.logError("userid_null", "ID de usuario es null");

            establecerUsuarioPorDefecto();
            if (callback != null) {
                callback.onUserDataLoaded(null, usuarioNombre, usuarioTelefono);
            }
            return;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("accion", "carga_usuario_inicio");
        analyticsHelper.logEvent("carga_usuario_inicio", params);

        userService.loadUserData(userId, new UserService.UserDataCallback() {
            @Override
            public void onUserDataLoaded(Usuario usuario) {
                if (usuario != null) {
                    usuarioNombre = usuario.getNombre();
                    usuarioTelefono = usuario.getTelefono();
                    usuarioId = usuario.getId();

                    analyticsHelper.logUsuarioCargado(usuarioNombre, usuarioTelefono);
                    Log.d(TAG, "Usuario cargado: " + usuarioNombre);

                    if (callback != null) {
                        callback.onUserDataLoaded(usuarioId, usuarioNombre, usuarioTelefono);
                    }
                } else {
                    Log.e(TAG, "Usuario es null");
                    analyticsHelper.logError("usuario_null", "Usuario es null");

                    establecerUsuarioPorDefecto();
                    if (callback != null) {
                        callback.onUserDataLoaded(null, usuarioNombre, usuarioTelefono);
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error cargando usuario: " + errorMessage);
                MyApp.logError(new Exception("Error cargando usuario: " + errorMessage));
                analyticsHelper.logError("carga_usuario", errorMessage);

                establecerUsuarioPorDefecto();
                if (callback != null) {
                    callback.onError(errorMessage);
                }
            }
        });
    }

    /**
     * Establece valores por defecto para el usuario
     */
    private void establecerUsuarioPorDefecto() {
        usuarioNombre = "Usuario";
        usuarioTelefono = "No disponible";

        Map<String, Object> params = new HashMap<>();
        params.put("accion", "usuario_por_defecto");
        analyticsHelper.logEvent("usuario_por_defecto", params);
        Log.w(TAG, "Usando valores por defecto para el usuario");
    }

    /**
     * Verifica si los datos del usuario están disponibles
     */
    public boolean hasUserData() {
        return usuarioId != null && usuarioNombre != null;
    }

    /**
     * Actualiza datos del usuario desde intent
     */
    public void updateFromIntent(String usuarioId, String usuarioNombre, String usuarioTelefono) {
        if (usuarioId != null) this.usuarioId = usuarioId;
        if (usuarioNombre != null) this.usuarioNombre = usuarioNombre;
        if (usuarioTelefono != null) this.usuarioTelefono = usuarioTelefono;

        if (usuarioNombre != null && usuarioId != null) {
            analyticsHelper.logUsuarioCargado(usuarioNombre, usuarioTelefono);
        }
    }

    // Getters
    public String getUsuarioId() { return usuarioId; }
    public String getUsuarioNombre() { return usuarioNombre; }
    public String getUsuarioTelefono() { return usuarioTelefono; }

    /**
     * Obtiene un resumen del usuario para logging
     */
    public String getUserSummary() {
        return String.format(
                "Usuario: %s (ID: %s, Tel: %s)",
                usuarioNombre != null ? usuarioNombre : "N/A",
                usuarioId != null ? usuarioId : "N/A",
                usuarioTelefono != null ? usuarioTelefono : "N/A"
        );
    }
}