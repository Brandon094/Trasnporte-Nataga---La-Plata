package com.chopcode.trasnportenataga_laplata.managers.reservations.confirmation;

import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.managers.analytics.ReservationAnalyticsHelper;

import java.util.HashMap;
import java.util.Map;

public class ConfirmationUIManager {

    private final ConfirmationDataProcessor dataProcessor;
    private final ReservationAnalyticsHelper analyticsHelper;

    // Referencias a vistas
    private TextView tvRuta, tvFechaHora, tvTiempoEstimado, tvPrecio, tvAsiento;
    private TextView tvUsuario, tvTelefonoP, tvConductor, tvTelefonoC, tvPlaca;
    private RadioGroup radioGroupPago;
    private RadioButton radioEfectivo, radioTransferencia;

    // Callbacks
    public interface ConfirmationListener {
        void onConfirmButtonClicked();
        void onCancelButtonClicked();
        void onPaymentMethodChanged(String metodoPago);
    }

    private ConfirmationListener listener;

    public ConfirmationUIManager(ConfirmationDataProcessor dataProcessor,
                                 ReservationAnalyticsHelper analyticsHelper) {
        this.dataProcessor = dataProcessor;
        this.analyticsHelper = analyticsHelper;
    }

    public void setViewReferences(
            TextView tvRuta, TextView tvFechaHora, TextView tvTiempoEstimado,
            TextView tvPrecio, TextView tvAsiento, TextView tvUsuario,
            TextView tvTelefonoP, TextView tvConductor, TextView tvTelefonoC,
            TextView tvPlaca, RadioGroup radioGroupPago,
            RadioButton radioEfectivo, RadioButton radioTransferencia) {

        this.tvRuta = tvRuta;
        this.tvFechaHora = tvFechaHora;
        this.tvTiempoEstimado = tvTiempoEstimado;
        this.tvPrecio = tvPrecio;
        this.tvAsiento = tvAsiento;
        this.tvUsuario = tvUsuario;
        this.tvTelefonoP = tvTelefonoP;
        this.tvConductor = tvConductor;
        this.tvTelefonoC = tvTelefonoC;
        this.tvPlaca = tvPlaca;
        this.radioGroupPago = radioGroupPago;
        this.radioEfectivo = radioEfectivo;
        this.radioTransferencia = radioTransferencia;
    }

    public void setConfirmationListener(ConfirmationListener listener) {
        this.listener = listener;
    }

    public void loadDataIntoUI(String usuarioNombre, String usuarioTelefono) {
        // Datos del viaje
        tvRuta.setText(dataProcessor.getRutaSeleccionada());

        String fechaHoraCompleta = formatFechaHora(
                dataProcessor.getFechaViaje(),
                dataProcessor.getHorarioHora()
        );
        tvFechaHora.setText(fechaHoraCompleta);

        tvAsiento.setText(formatAsiento(dataProcessor.getAsientoSeleccionado()));
        tvTiempoEstimado.setText(dataProcessor.getTiempoEstimado());
        tvPrecio.setText(formatPrecio(dataProcessor.getPrecio()));

        // Datos del usuario
        tvUsuario.setText(usuarioNombre);
        tvTelefonoP.setText(usuarioTelefono);

        // Datos del conductor
        tvConductor.setText(dataProcessor.getConductorNombre());
        tvTelefonoC.setText(dataProcessor.getConductorTelefono());
        tvPlaca.setText(formatInfoVehiculo(
                dataProcessor.getVehiculoPlaca(),
                dataProcessor.getVehiculoModelo()
        ));

        // Método de pago por defecto
        radioEfectivo.setChecked(true);
    }

    public void setupListeners() {
        setupPaymentMethodListener();
    }

    private void setupPaymentMethodListener() {
        radioGroupPago.setOnCheckedChangeListener((group, checkedId) -> {
            String metodoPago = getSelectedPaymentMethod(checkedId);
            dataProcessor.setMetodoPago(metodoPago);

            logPaymentMethodSelected(metodoPago);

            if (listener != null) {
                listener.onPaymentMethodChanged(metodoPago);
            }
        });
    }

    private String getSelectedPaymentMethod(int checkedId) {
        if (checkedId == R.id.radioEfectivo) {
            return "Efectivo";
        } else if (checkedId == R.id.radioTransferencia) {
            return "Transferencia";
        }
        return "Efectivo";
    }

    // Métodos de formateo
    private String formatFechaHora(String fecha, String hora) {
        return fecha + " - " + hora;
    }

    private String formatAsiento(int asiento) {
        return "A" + asiento;
    }

    private String formatPrecio(double precio) {
        return String.format("$%,d", (int) precio);
    }

    private String formatInfoVehiculo(String placa, String modelo) {
        return "Vehículo: " + placa + " - " + modelo;
    }

    // Métodos de analytics
    private void logPaymentMethodSelected(String metodoPago) {
        Map<String, Object> params = new HashMap<>();
        params.put("tipo", metodoPago.toLowerCase());
        params.put("asiento", dataProcessor.getAsientoSeleccionado());
        analyticsHelper.logEvent("metodo_pago_seleccionado", params);
    }

    public void logButtonClick(String accion) {
        Map<String, Object> params = new HashMap<>();
        params.put("accion", accion);
        params.put("asiento", dataProcessor.getAsientoSeleccionado());
        analyticsHelper.logEvent("click_boton", params);
    }
}