package com.chopcode.trasnportenataga_laplata.managers.reservations.confirmation;

import android.content.Intent;
import android.util.Log;

import com.chopcode.trasnportenataga_laplata.config.MyApp;
import com.chopcode.trasnportenataga_laplata.managers.analytics.ReservationAnalyticsHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Procesador de datos para confirmación (SIMPLIFICADO usando tus managers existentes)
 */
public class ConfirmationDataProcessor {

    private static final String TAG = "ConfirmationDataProcessor";
    private final ReservationAnalyticsHelper analyticsHelper;

    // Datos
    private int asientoSeleccionado;
    private String horarioId, horarioHora, rutaSeleccionada, fechaViaje;
    private String origen, destino, tiempoEstimado, metodoPago;
    private double precio;
    private String conductorNombre, conductorTelefono, conductorId;
    private String vehiculoPlaca, vehiculoModelo;

    public ConfirmationDataProcessor(ReservationAnalyticsHelper analyticsHelper) {
        this.analyticsHelper = analyticsHelper;
        this.metodoPago = "Efectivo";
    }

    /**
     * Procesa datos del Intent (versión simplificada)
     */
    public void processIntentData(Intent intent) {
        if (intent == null) return;

        // Extraer datos básicos
        asientoSeleccionado = intent.getIntExtra("asientoSeleccionado", -1);
        horarioId = intent.getStringExtra("horarioId");
        horarioHora = intent.getStringExtra("horarioHora");
        rutaSeleccionada = intent.getStringExtra("rutaSelecionada");
        fechaViaje = intent.getStringExtra("fechaViaje");
        precio = intent.getDoubleExtra("precio", 12000.0);
        tiempoEstimado = intent.getStringExtra("tiempoEstimado");

        // Datos del conductor
        conductorNombre = intent.getStringExtra("conductorNombre");
        conductorTelefono = intent.getStringExtra("conductorTelefono");
        conductorId = intent.getStringExtra("conductorId");

        // Datos del vehículo
        vehiculoPlaca = intent.getStringExtra("vehiculoPlaca");
        vehiculoModelo = intent.getStringExtra("vehiculoModelo");

        // Origen y destino
        origen = intent.getStringExtra("origen");
        destino = intent.getStringExtra("destino");

        // Valores por defecto
        setDefaultValues();
        processRouteIfNeeded();

        logDataReceived();
    }

    private void setDefaultValues() {
        if (conductorNombre == null) conductorNombre = "N/A";
        if (conductorTelefono == null) conductorTelefono = "N/A";
        if (conductorId == null) conductorId = "N/A";
        if (vehiculoPlaca == null) vehiculoPlaca = "N/A";
        if (vehiculoModelo == null) vehiculoModelo = "N/A";
        if (tiempoEstimado == null) tiempoEstimado = "N/A";
    }

    private void processRouteIfNeeded() {
        if (origen == null && rutaSeleccionada != null) {
            String[] partes = rutaSeleccionada.split(" -> ");
            if (partes.length == 2) {
                origen = partes[0].trim();
                destino = partes[1].trim();
            } else {
                origen = "Natagá";
                destino = "La Plata";
            }
        }
    }

    private void logDataReceived() {
        Map<String, Object> params = new HashMap<>();
        params.put("asiento", asientoSeleccionado);
        params.put("ruta", rutaSeleccionada != null ? rutaSeleccionada : "N/A");
        params.put("conductor", conductorNombre != null ? conductorNombre : "N/A");
        params.put("precio", precio);

        // ✅ Esto debería funcionar si analyticsHelper.logEvent acepta Map
        analyticsHelper.logEvent("datos_confirmacion_procesados", params);

        Log.d(TAG, "✓ Datos procesados:");
        Log.d(TAG, "  - Ruta: " + rutaSeleccionada);
        Log.d(TAG, "  - Conductor: " + conductorNombre);
        Log.d(TAG, "  - Precio: $" + precio);
    }

    // Getters
    public int getAsientoSeleccionado() { return asientoSeleccionado; }
    public String getHorarioId() { return horarioId; }
    public String getHorarioHora() { return horarioHora; }
    public String getRutaSeleccionada() { return rutaSeleccionada; }
    public String getFechaViaje() { return fechaViaje; }
    public String getOrigen() { return origen; }
    public String getDestino() { return destino; }
    public String getTiempoEstimado() { return tiempoEstimado; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    public double getPrecio() { return precio; }
    public String getConductorNombre() { return conductorNombre; }
    public String getConductorTelefono() { return conductorTelefono; }
    public String getConductorId() { return conductorId; }
    public String getVehiculoPlaca() { return vehiculoPlaca; }
    public String getVehiculoModelo() { return vehiculoModelo; }
}