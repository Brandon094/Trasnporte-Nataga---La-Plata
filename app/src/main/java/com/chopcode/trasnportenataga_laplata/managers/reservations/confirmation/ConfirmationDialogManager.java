package com.chopcode.trasnportenataga_laplata.managers.reservations.confirmation;

import android.app.AlertDialog;
import android.content.Context;

import com.chopcode.trasnportenataga_laplata.managers.analytics.ReservationAnalyticsHelper;

import java.util.HashMap;
import java.util.Map;

public class ConfirmationDialogManager {

    private final Context context;
    private final ConfirmationAnalyticsHelper analyticsHelper;

    // Callbacks
    public interface DialogCallback {
        void onPositiveAction();
        void onNegativeAction();
    }

    public ConfirmationDialogManager(Context context,
                                     ConfirmationAnalyticsHelper analyticsHelper) {
        this.context = context;
        this.analyticsHelper = analyticsHelper;
    }

    public void showCancellationDialog(DialogCallback callback) {
        analyticsHelper.logCancellationDialogShown();

        new AlertDialog.Builder(context)
                .setTitle("Cancelar reserva")
                .setMessage("¿Estás seguro de que quieres cancelar la reserva?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    analyticsHelper.logCancellationAction("confirmada");
                    if (callback != null) {
                        callback.onPositiveAction();
                    }
                })
                .setNegativeButton("No", (dialog, which) -> {
                    analyticsHelper.logCancellationAction("rechazada");
                    if (callback != null) {
                        callback.onNegativeAction();
                    }
                })
                .show();
    }
}