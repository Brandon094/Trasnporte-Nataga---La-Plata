package com.chopcode.trasnportenataga_laplata.managers.reservations.confirmation;

import com.chopcode.trasnportenataga_laplata.managers.analytics.ReservationAnalyticsHelper;

import java.util.HashMap;
import java.util.Map;

public class ConfirmationAnalyticsHelper {

    private final ReservationAnalyticsHelper analyticsHelper;
    private final ConfirmationDataProcessor dataProcessor;

    public ConfirmationAnalyticsHelper(ReservationAnalyticsHelper analyticsHelper,
                                       ConfirmationDataProcessor dataProcessor) {
        this.analyticsHelper = analyticsHelper;
        this.dataProcessor = dataProcessor;
    }

    public void logButtonClick(String accion) {
        Map<String, Object> params = new HashMap<>();
        params.put("accion", accion);
        params.put("asiento", dataProcessor.getAsientoSeleccionado());
        analyticsHelper.logEvent("click_boton", params);
    }

    public void logValidationSuccess() {
        Map<String, Object> params = new HashMap<>();
        params.put("asiento", dataProcessor.getAsientoSeleccionado());
        params.put("ruta", dataProcessor.getRutaSeleccionada());
        params.put("metodo_pago", dataProcessor.getMetodoPago());
        analyticsHelper.logEvent("validacion_exitosa", params);
    }

    public void logValidationFailed(String razon) {
        Map<String, Object> params = new HashMap<>();
        params.put("razon", razon);
        params.put("asiento", dataProcessor.getAsientoSeleccionado());
        analyticsHelper.logEvent("validacion_fallida", params);
    }

    public void logCancellationDialogShown() {
        Map<String, Object> params = new HashMap<>();
        params.put("asiento", dataProcessor.getAsientoSeleccionado());
        analyticsHelper.logEvent("dialogo_cancelacion_mostrado", params);
    }

    public void logCancellationAction(String accion) {
        Map<String, Object> params = new HashMap<>();
        params.put("asiento", dataProcessor.getAsientoSeleccionado());
        params.put("accion", accion);
        analyticsHelper.logEvent("cancelacion_reserva", params);
    }

    public void logNavigation(String destino) {
        Map<String, Object> params = new HashMap<>();
        params.put("destino", destino);
        params.put("asiento", dataProcessor.getAsientoSeleccionado());
        analyticsHelper.logEvent("navegacion", params);
    }

    public void logError(String tipoError, String mensaje) {
        Map<String, Object> params = new HashMap<>();
        params.put("error", tipoError);
        params.put("mensaje", mensaje);
        params.put("asiento", dataProcessor.getAsientoSeleccionado());
        analyticsHelper.logEvent("error", params);
    }

    public void logScreenEvent(String evento) {
        Map<String, Object> params = new HashMap<>();
        params.put("pantalla", "ConfirmarReserva");
        params.put("evento", evento);
        analyticsHelper.logEvent("pantalla_evento", params);
    }
}