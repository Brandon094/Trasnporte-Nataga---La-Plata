package com.chopcode.trasnportenataga_laplata.managers.statistics;

import android.util.Log;

import com.chopcode.trasnportenataga_laplata.config.MyApp;
import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;
import java.util.Map;

public abstract class StatisticsManager {
    protected static final String TAG = "StatisticsManager";

    // Interfaces comunes
    public interface StatisticsCallback {
        void onStatisticsCalculated(int reservasConfirmadas, int asientosDisponibles, double ingresos);
        void onError(String error);
    }

    public interface IncomeUpdateCallback {
        void onSuccess(double nuevosIngresos);
        void onError(String error);
    }

    // ✅ Usar MyApp para obtener DatabaseReference
    protected DatabaseReference getDatabaseReference(String path) {
        return MyApp.getDatabaseReference(path);
    }

    // ✅ Métodos de logging usando MyApp
    protected void logInfo(String message) {
        Log.i(TAG, "ℹ️ " + message);
    }

    protected void logError(String message, Exception e) {
        Log.e(TAG, "❌ " + message);
        if (e != null) {
            MyApp.logError(e);
        }
    }

    protected void logWarning(String message) {
        Log.w(TAG, "⚠️ " + message);
    }

    // ✅ Método para registrar eventos analíticos usando MyApp
    protected void logAnalyticsEvent(String eventName, Map<String, Object> params) {
        try {
            if (params == null) {
                params = new HashMap<>();
            }

            // Agregar metadatos automáticos
            params.put("statistics_manager", this.getClass().getSimpleName());
            params.put("timestamp", System.currentTimeMillis());

            MyApp.logEvent(eventName, params);
            Log.d(TAG, "📊 Evento analítico registrado: " + eventName);
        } catch (Exception e) {
            logError("Error registrando evento analítico: " + e.getMessage(), e);
        }
    }

    // ✅ Método para obtener el ID del usuario actual
    protected String getCurrentUserId() {
        return MyApp.getCurrentUserId();
    }

    // ✅ Método para verificar si hay usuario logeado
    protected boolean isUserLoggedIn() {
        return MyApp.isUserLoggedIn();
    }
}