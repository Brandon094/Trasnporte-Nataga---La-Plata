package com.chopcode.trasnportenataga_laplata.managers.reservations;

import android.app.AlertDialog;
import android.content.Context;

import com.chopcode.trasnportenataga_laplata.managers.analytics.ReservationAnalyticsHelper;
import com.chopcode.trasnportenataga_laplata.managers.seats.SeatManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Manager para manejar la navegación y diálogos de confirmación
 */
public class ReservationNavigationManager {

    private final Context context;
    private final ReservationAnalyticsHelper analyticsHelper;
    private final SeatManager seatManager;

    public interface NavigationCallback {
        void onConfirmNavigation();
        void onCancelNavigation();
    }

    public ReservationNavigationManager(
            Context context,
            ReservationAnalyticsHelper analyticsHelper,
            SeatManager seatManager) {

        this.context = context;
        this.analyticsHelper = analyticsHelper;
        this.seatManager = seatManager;
    }

    /**
     * Maneja la acción de volver atrás con confirmación si hay asiento seleccionado
     */
    public void handleBackAction(NavigationCallback callback) {
        if (seatManager.hasAsientoSeleccionado()) {
            showCancelSeatDialog(callback);
        } else {
            logSimpleNavigation();
            if (callback != null) {
                callback.onConfirmNavigation();
            }
        }
    }

    /**
     * Muestra diálogo de confirmación para cancelar selección de asiento
     */
    private void showCancelSeatDialog(NavigationCallback callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("asiento", seatManager.getAsientoSeleccionado());
        analyticsHelper.logEvent("dialogo_cancelar_asiento", params);

        new AlertDialog.Builder(context)
                .setTitle("Cancelar selección")
                .setMessage("¿Estás seguro de que quieres cancelar la selección de asiento?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    analyticsHelper.logEvent("cancelacion_asiento_confirmada", params);
                    if (callback != null) {
                        callback.onConfirmNavigation();
                    }
                })
                .setNegativeButton("No", (dialog, which) -> {
                    analyticsHelper.logEvent("cancelacion_asiento_rechazada", params);
                    if (callback != null) {
                        callback.onCancelNavigation();
                    }
                })
                .show();
    }

    /**
     * Log de navegación simple (sin asiento seleccionado)
     */
    private void logSimpleNavigation() {
        Map<String, Object> params = new HashMap<>();
        params.put("accion", "navegacion_atras_simple");
        analyticsHelper.logEvent("navegacion_atras_simple", params);
    }

    /**
     * Log de botón back físico
     */
    public void logPhysicalBackButton() {
        Map<String, Object> params = new HashMap<>();
        params.put("accion", "boton_back_fisico");
        analyticsHelper.logEvent("boton_back_fisico", params);
    }
}