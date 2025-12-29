package com.chopcode.trasnportenataga_laplata.managers.routes;

import android.util.Log;

import com.chopcode.trasnportenataga_laplata.models.Ruta;
import com.chopcode.trasnportenataga_laplata.services.user.UserService;

import java.util.List;

public class RutasManager {
    private static final String TAG = "RutasManager";
    private final UserService userService;

    public interface RoutesCallback {
        void onRoutesLoaded(List<Ruta> rutas);
        void onError(String error);
    }

    public RutasManager() {
        this.userService = new UserService();
    }

    public void loadAssignedRoutes(List<String> horariosAsignados, RoutesCallback callback) {
        Log.d(TAG, "Cargando rutas asignadas");

        userService.loadAssignedRoutes(horariosAsignados, new UserService.RoutesCallback() {
            @Override
            public void onRoutesLoaded(List<Ruta> rutas) {
                Log.d(TAG, rutas.size() + " rutas cargadas");
                callback.onRoutesLoaded(rutas);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error cargando rutas: " + error);
                callback.onError(error);
            }
        });
    }
}