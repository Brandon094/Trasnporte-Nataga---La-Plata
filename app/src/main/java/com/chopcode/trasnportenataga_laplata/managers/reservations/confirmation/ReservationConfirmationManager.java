package com.chopcode.trasnportenataga_laplata.managers.reservations.confirmation;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import com.chopcode.trasnportenataga_laplata.config.MyApp;
import com.chopcode.trasnportenataga_laplata.managers.analytics.ReservationAnalyticsHelper;
import com.chopcode.trasnportenataga_laplata.services.reservations.ReservaService;

import java.util.HashMap;
import java.util.Map;

public class ReservationConfirmationManager {

    private static final String TAG = "ReservationConfirmationManager";
    private static final int RESERVATION_TIMEOUT_MS = 15000;

    private final Context context;
    private final ReservationAnalyticsHelper analyticsHelper;
    private final ReservaService reservaService;
    private final Handler timeoutHandler;
    private ConfirmationDataProcessor dataProcessor;

    // Callbacks
    public interface ConfirmationCallback {
        void onConfirmationStarted();
        void onConfirmationSuccess();
        void onConfirmationError(String error);
        void onButtonStateChanged(boolean enabled, String text);
    }

    private ConfirmationCallback callback;

    public ReservationConfirmationManager(Context context,
                                          ReservationAnalyticsHelper analyticsHelper) {
        this.context = context;
        this.analyticsHelper = analyticsHelper;
        this.reservaService = new ReservaService();
        this.timeoutHandler = new Handler();
    }

    public void setDataProcessor(ConfirmationDataProcessor dataProcessor) {
        this.dataProcessor = dataProcessor;
    }

    public void setConfirmationCallback(ConfirmationCallback callback) {
        this.callback = callback;
    }

    public void confirmReservation() {
        if (!validateUserAuthentication()) {
            return;
        }

        if (callback != null) {
            callback.onConfirmationStarted();
        }

        logConfirmationStarted();

        // Configurar timeout
        Runnable timeoutRunnable = createTimeoutRunnable();
        timeoutHandler.postDelayed(timeoutRunnable, RESERVATION_TIMEOUT_MS);

        // Realizar reserva
        String estadoReserva = "Por confirmar";

        reservaService.actualizarDisponibilidadAsientos(
                context,
                dataProcessor.getHorarioId(),
                dataProcessor.getAsientoSeleccionado(),
                dataProcessor.getOrigen(),
                dataProcessor.getDestino(),
                dataProcessor.getTiempoEstimado(),
                dataProcessor.getMetodoPago(),
                estadoReserva,
                dataProcessor.getVehiculoPlaca(),
                dataProcessor.getPrecio(),
                dataProcessor.getConductorNombre(),
                dataProcessor.getConductorTelefono(),
                new ReservaService.ReservaCallback() {
                    @Override
                    public void onReservaExitosa() {
                        timeoutHandler.removeCallbacks(timeoutRunnable);
                        handleConfirmationSuccess();
                    }

                    @Override
                    public void onError(String error) {
                        timeoutHandler.removeCallbacks(timeoutRunnable);
                        handleConfirmationError("Error al confirmar reserva: " + error);
                    }
                });
    }

    private boolean validateUserAuthentication() {
        String userId = MyApp.getCurrentUserId();
        if (userId == null) {
            showToast("Error: Usuario no autenticado");

            Map<String, Object> params = new HashMap<>();
            params.put("error", "usuario_no_autenticado");
            analyticsHelper.logEvent("error", params);

            return false;
        }
        return true;
    }

    private void handleConfirmationSuccess() {
        logConfirmationSuccess();

        if (callback != null) {
            callback.onConfirmationSuccess();
        }

        showToast("✅ Reserva creada exitosamente");
    }

    private void handleConfirmationError(String error) {
        if (callback != null) {
            callback.onConfirmationError(error);
            callback.onButtonStateChanged(true, "Confirmar Reserva");
        }

        MyApp.logError(new Exception(error));
        logConfirmationError(error);
        showToast("❌ " + error);
    }

    private Runnable createTimeoutRunnable() {
        return () -> {
            if (callback != null) {
                callback.onButtonStateChanged(true, "Confirmar Reserva");
            }

            showToast("La operación está tardando más de lo esperado. Verifica tu conexión.");
            logTimeout();
        };
    }

    private void showToast(String message) {
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(() -> {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            });
        }
    }

    public void cleanup() {
        if (timeoutHandler != null) {
            timeoutHandler.removeCallbacksAndMessages(null);
        }
    }

    // Métodos de logging
    private void logConfirmationStarted() {
        Map<String, Object> params = new HashMap<>();
        params.put("asiento", dataProcessor.getAsientoSeleccionado());
        params.put("ruta", dataProcessor.getRutaSeleccionada());
        params.put("metodo_pago", dataProcessor.getMetodoPago());
        params.put("precio", dataProcessor.getPrecio());

        analyticsHelper.logEvent("confirmacion_reserva_inicio", params);
    }

    private void logConfirmationSuccess() {
        Map<String, Object> params = new HashMap<>();
        params.put("asiento", dataProcessor.getAsientoSeleccionado());
        params.put("ruta", dataProcessor.getRutaSeleccionada());
        params.put("conductor", dataProcessor.getConductorNombre());
        params.put("metodo_pago", dataProcessor.getMetodoPago());
        params.put("precio", dataProcessor.getPrecio());

        analyticsHelper.logEvent("reserva_confirmada_exitosa", params);
    }

    private void logConfirmationError(String error) {
        Map<String, Object> params = new HashMap<>();
        params.put("error", error);
        params.put("asiento", dataProcessor.getAsientoSeleccionado());

        analyticsHelper.logEvent("error_registro_reserva", params);
    }

    private void logTimeout() {
        Map<String, Object> params = new HashMap<>();
        params.put("tipo", "timeout_registro_reserva");
        params.put("asiento", dataProcessor.getAsientoSeleccionado());

        analyticsHelper.logEvent("timeout", params);
    }
}