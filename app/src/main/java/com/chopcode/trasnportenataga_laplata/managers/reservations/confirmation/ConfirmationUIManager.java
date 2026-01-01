package com.chopcode.trasnportenataga_laplata.managers.reservations.confirmation;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.managers.analytics.ReservationAnalyticsHelper;
import java.util.HashMap;
import java.util.Map;

public class ConfirmationUIManager {

    private final ConfirmationDataProcessor dataProcessor;
    private final ReservationAnalyticsHelper analyticsHelper;

    // Referencias a vistas - NUEVO LAYOUT
    private TextView tvOrigen, tvDestino, tvFecha, tvHora, tvTiempoEstimado, tvPrecio, tvAsiento;
    private TextView tvUsuario, tvTelefonoP, tvConductor, tvTelefonoC, tvPlaca;
    private MaterialCardView cardEfectivo, cardTransferencia;
    private ExtendedFloatingActionButton fabAyuda;

    // Referencias a los ImageView de check
    private ImageView checkIconEfectivo, checkIconTransferencia;

    // Callbacks
    public interface ConfirmationListener {
        void onConfirmButtonClicked();
        void onCancelButtonClicked();
        void onPaymentMethodChanged(String metodoPago);
        void onHelpRequested(); // Nuevo callback para el FAB de ayuda
    }

    private ConfirmationListener listener;

    public ConfirmationUIManager(ConfirmationDataProcessor dataProcessor,
                                 ReservationAnalyticsHelper analyticsHelper) {
        this.dataProcessor = dataProcessor;
        this.analyticsHelper = analyticsHelper;
    }

    // NUEVO MÉTODO para el layout mejorado
    public void setNewViewReferences(
            TextView tvOrigen, TextView tvDestino, TextView tvFecha, TextView tvHora,
            TextView tvTiempoEstimado, TextView tvPrecio, TextView tvAsiento,
            TextView tvUsuario, TextView tvTelefonoP, TextView tvConductor,
            TextView tvTelefonoC, TextView tvPlaca,
            MaterialCardView cardEfectivo, MaterialCardView cardTransferencia) {

        this.tvOrigen = tvOrigen;
        this.tvDestino = tvDestino;
        this.tvFecha = tvFecha;
        this.tvHora = tvHora;
        this.tvTiempoEstimado = tvTiempoEstimado;
        this.tvPrecio = tvPrecio;
        this.tvAsiento = tvAsiento;
        this.tvUsuario = tvUsuario;
        this.tvTelefonoP = tvTelefonoP;
        this.tvConductor = tvConductor;
        this.tvTelefonoC = tvTelefonoC;
        this.tvPlaca = tvPlaca;
        this.cardEfectivo = cardEfectivo;
        this.cardTransferencia = cardTransferencia;
        this.fabAyuda = fabAyuda;

        // Obtener referencias a los ImageView de check
        if (cardEfectivo != null) {
            this.checkIconEfectivo = cardEfectivo.findViewById(R.id.checkIconEfectivo);
        }
        if (cardTransferencia != null) {
            this.checkIconTransferencia = cardTransferencia.findViewById(R.id.checkIconTransferencia);
        }
    }

    public void setConfirmationListener(ConfirmationListener listener) {
        this.listener = listener;
    }

    // NUEVO MÉTODO para el layout mejorado
    public void loadDataIntoNewUI(String usuarioNombre, String usuarioTelefono) {
        // Datos del viaje - Separados en origen y destino
        if (tvOrigen != null) {
            tvOrigen.setText(dataProcessor.getOrigen());
        }

        if (tvDestino != null) {
            tvDestino.setText(dataProcessor.getDestino());
        }

        // Fecha y hora separadas
        if (tvFecha != null) {
            // Usar el método formateado del dataProcessor
            tvFecha.setText(dataProcessor.getFechaViaje());
        }

        if (tvHora != null) {
            // Usar el método formateado del dataProcessor
            tvHora.setText(dataProcessor.getHoraFormateada());
        }

        // Resto de datos
        if (tvAsiento != null) {
            tvAsiento.setText(formatAsiento(dataProcessor.getAsientoSeleccionado()));
        }

        if (tvTiempoEstimado != null) {
            tvTiempoEstimado.setText(dataProcessor.getTiempoEstimado());
        }

        if (tvPrecio != null) {
            tvPrecio.setText(formatPrecio(dataProcessor.getPrecio()));
        }

        // Datos del usuario
        if (tvUsuario != null) {
            tvUsuario.setText(usuarioNombre);
        }

        if (tvTelefonoP != null) {
            tvTelefonoP.setText(usuarioTelefono);
        }

        // Datos del conductor
        if (tvConductor != null) {
            tvConductor.setText(dataProcessor.getConductorNombre());
        }

        if (tvTelefonoC != null) {
            tvTelefonoC.setText(dataProcessor.getConductorTelefono());
        }

        if (tvPlaca != null) {
            tvPlaca.setText(formatInfoVehiculo(
                    dataProcessor.getVehiculoPlaca(),
                    dataProcessor.getVehiculoModelo()
            ));
        }

        // Configurar método de pago por defecto (efectivo)
        setupDefaultPaymentMethod();
    }

    // NUEVO MÉTODO para el layout mejorado
    public void setupNewListeners() {
        setupPaymentMethodListeners();
        setupHelpButtonListener();
    }

    private void setupPaymentMethodListeners() {
        if (cardEfectivo != null) {
            cardEfectivo.setOnClickListener(v -> {
                selectPaymentMethod("efectivo");
                logPaymentMethodSelected("efectivo");
            });
        }

        if (cardTransferencia != null) {
            cardTransferencia.setOnClickListener(v -> {
                selectPaymentMethod("transferencia");
                logPaymentMethodSelected("transferencia");
            });
        }

        // Establecer método por defecto
        selectPaymentMethod("efectivo");
    }

    private void setupHelpButtonListener() {
        if (fabAyuda != null && listener != null) {
            fabAyuda.setOnClickListener(v -> {
                logButtonClick("ayuda_solicitada");
                listener.onHelpRequested();
            });
        }
    }

    private void setupDefaultPaymentMethod() {
        selectPaymentMethod("efectivo");
        dataProcessor.setMetodoPago("efectivo");
    }

    private void selectPaymentMethod(String metodo) {
        // Configurar UI visual para efectivo
        if (cardEfectivo != null) {
            if (metodo.equals("efectivo")) {
                cardEfectivo.setStrokeColor(ContextCompat.getColor(cardEfectivo.getContext(), R.color.primary_300));
                cardEfectivo.setCardBackgroundColor(ContextCompat.getColor(cardEfectivo.getContext(), R.color.primary_50));
                // Mostrar check
                if (checkIconEfectivo != null) {
                    checkIconEfectivo.setVisibility(View.VISIBLE);
                }
            } else {
                cardEfectivo.setStrokeColor(ContextCompat.getColor(cardEfectivo.getContext(), R.color.outline));
                cardEfectivo.setCardBackgroundColor(ContextCompat.getColor(cardEfectivo.getContext(), R.color.surface));
                // Ocultar check
                if (checkIconEfectivo != null) {
                    checkIconEfectivo.setVisibility(View.GONE);
                }
            }
        }

        // Configurar UI visual para transferencia
        if (cardTransferencia != null) {
            if (metodo.equals("transferencia")) {
                cardTransferencia.setStrokeColor(ContextCompat.getColor(cardTransferencia.getContext(), R.color.primary_300));
                cardTransferencia.setCardBackgroundColor(ContextCompat.getColor(cardTransferencia.getContext(), R.color.primary_50));
                // Mostrar check
                if (checkIconTransferencia != null) {
                    checkIconTransferencia.setVisibility(View.VISIBLE);
                }
            } else {
                cardTransferencia.setStrokeColor(ContextCompat.getColor(cardTransferencia.getContext(), R.color.outline));
                cardTransferencia.setCardBackgroundColor(ContextCompat.getColor(cardTransferencia.getContext(), R.color.surface));
                // Ocultar check
                if (checkIconTransferencia != null) {
                    checkIconTransferencia.setVisibility(View.GONE);
                }
            }
        }

        // Actualizar data processor
        dataProcessor.setMetodoPago(metodo);

        // Notificar al listener
        if (listener != null) {
            listener.onPaymentMethodChanged(metodo);
        }
    }

    // Métodos de formateo
    private String formatAsiento(int asiento) {
        return "A" + asiento;
    }

    private String formatPrecio(double precio) {
        return String.format("$%,.0f", precio);
    }

    private String formatInfoVehiculo(String placa, String modelo) {
        if (modelo != null && !modelo.isEmpty()) {
            return modelo + " • " + placa;
        }
        return placa != null ? placa : "Información no disponible";
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

    // Método para obtener el método de pago seleccionado
    public String getSelectedPaymentMethod() {
        return dataProcessor.getMetodoPago();
    }

    // Método para validar formulario
    public boolean validateForm() {
        String metodoPago = dataProcessor.getMetodoPago();
        return metodoPago != null && !metodoPago.isEmpty();
    }
}