package com.chopcode.trasnportenataga_laplata.managers.reservations.confirmation;

import android.content.Intent;
import android.util.Log;
import com.chopcode.trasnportenataga_laplata.managers.analytics.ReservationAnalyticsHelper;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class ConfirmationDataProcessor {

    private static final String TAG = "ConfirmationDataProcessor";

    private final ReservationAnalyticsHelper analyticsHelper;

    // Datos de la reserva
    private String rutaSeleccionada;
    private String horarioId;
    private String horarioHora;
    private String fechaViaje;
    private int asientoSeleccionado;
    private String conductorNombre;
    private String conductorTelefono;
    private String conductorId;
    private String vehiculoPlaca;
    private String vehiculoModelo;
    private int vehiculoCapacidad;
    private double precio;
    private String tiempoEstimado;
    private String origen;
    private String destino;

    // Datos del usuario (pueden venir del Intent o cargarse después)
    private String usuarioNombre;
    private String usuarioTelefono;
    private String usuarioId;

    // Método de pago seleccionado
    private String metodoPago;

    public ConfirmationDataProcessor(ReservationAnalyticsHelper analyticsHelper) {
        this.analyticsHelper = analyticsHelper;
        this.metodoPago = "efectivo"; // Valor por defecto
    }

    /**
     * Procesa los datos del Intent recibido
     */
    public void processIntentData(Intent intent) {
        if (intent == null) {
            Log.e(TAG, "Intent nulo recibido");
            analyticsHelper.logError("intent_nulo", "Intent vacío recibido");
            return;
        }

        try {
            // 1. Procesar datos básicos del viaje
            processBasicTripData(intent);

            // 2. Procesar información del conductor
            processDriverData(intent);

            // 3. Procesar información del vehículo
            processVehicleData(intent);

            // 4. Procesar información del pasajero (si viene en el Intent)
            processPassengerData(intent);

            // 5. Procesar datos adicionales
            processAdditionalData(intent);

            // 6. Log analytics
            logProcessedData();

        } catch (Exception e) {
            Log.e(TAG, "Error procesando datos del Intent: " + e.getMessage(), e);
            analyticsHelper.logError("procesamiento_datos", e.getMessage());
        }
    }

    private void processBasicTripData(Intent intent) {
        asientoSeleccionado = intent.getIntExtra("asientoSeleccionado", 0);
        rutaSeleccionada = intent.getStringExtra("rutaSelecionada");
        horarioId = intent.getStringExtra("horarioId");
        horarioHora = intent.getStringExtra("horarioHora");
        fechaViaje = intent.getStringExtra("fechaViaje");

        Log.d(TAG, "✅ Datos básicos procesados - Asiento: " + asientoSeleccionado);
    }

    private void processDriverData(Intent intent) {
        conductorNombre = intent.getStringExtra("conductorNombre");
        conductorTelefono = intent.getStringExtra("conductorTelefono");
        conductorId = intent.getStringExtra("conductorId");

        Log.d(TAG, "✅ Datos conductor procesados - Nombre: " + conductorNombre);
    }

    private void processVehicleData(Intent intent) {
        vehiculoPlaca = intent.getStringExtra("vehiculoPlaca");
        vehiculoModelo = intent.getStringExtra("vehiculoModelo");
        vehiculoCapacidad = intent.getIntExtra("vehiculoCapacidad", 0);

        Log.d(TAG, "✅ Datos vehículo procesados - Placa: " + vehiculoPlaca);
    }

    private void processPassengerData(Intent intent) {
        usuarioNombre = intent.getStringExtra("usuarioNombre");
        usuarioTelefono = intent.getStringExtra("usuarioTelefono");
        usuarioId = intent.getStringExtra("usuarioId");

        if (usuarioNombre != null) {
            Log.d(TAG, "✅ Datos pasajero del Intent - Nombre: " + usuarioNombre);
        }
    }

    private void processAdditionalData(Intent intent) {
        precio = intent.getDoubleExtra("precio", 12000.0);
        tiempoEstimado = intent.getStringExtra("tiempoEstimado");

        // PRIMERO intentar obtener del Intent (si ReservationDataProcessor los agregó)
        origen = intent.getStringExtra("origen");
        destino = intent.getStringExtra("destino");

        // SI no vienen en el Intent, extraer de la ruta
        if ((origen == null || origen.isEmpty()) && rutaSeleccionada != null && rutaSeleccionada.contains("->")) {
            String[] partes = rutaSeleccionada.split("->");
            if (partes.length >= 2) {
                origen = partes[0].trim();
                destino = partes[1].trim();
            }
        }

        // Si no se pudo extraer, usar valores por defecto
        if (origen == null || origen.isEmpty()) {
            origen = "Natagá";
        }
        if (destino == null || destino.isEmpty()) {
            destino = "La Plata";
        }

        Log.d(TAG, "✅ Datos adicionales procesados - Origen: " + origen + ", Destino: " + destino);
    }

    private void logProcessedData() {
        Map<String, Object> params = new HashMap<>();
        params.put("asiento", asientoSeleccionado);
        params.put("origen", origen);
        params.put("destino", destino);
        params.put("conductor", conductorNombre != null ? conductorNombre : "N/A");
        analyticsHelper.logEvent("datos_procesados_exitoso", params);
    }

    // GETTERS para el UIManager

    public String getRutaSeleccionada() {
        return rutaSeleccionada != null ? rutaSeleccionada : "Ruta no disponible";
    }

    public String getFechaViaje() {
        if (fechaViaje != null) {
            try {
                // Formatear fecha a un formato más legible
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                Date date = inputFormat.parse(fechaViaje);
                return outputFormat.format(date);
            } catch (Exception e) {
                // Si hay error en el parseo, devolver la fecha original
                return fechaViaje;
            }
        }
        return "Fecha no disponible";
    }
    public String getHorarioId() {
        return horarioId != null ? horarioId : "";
    }

    public String getHorarioHora() {
        return horarioHora != null ? horarioHora : "Hora no disponible";
    }

    public int getAsientoSeleccionado() {
        return asientoSeleccionado;
    }

    public String getTiempoEstimado() {
        return tiempoEstimado != null ? tiempoEstimado : "60 min";
    }

    public double getPrecio() {
        return precio;
    }

    public String getConductorNombre() {
        return conductorNombre != null ? conductorNombre : "Conductor no disponible";
    }

    public String getConductorTelefono() {
        return conductorTelefono != null ? conductorTelefono : "Teléfono no disponible";
    }

    public String getVehiculoPlaca() {
        return vehiculoPlaca != null ? vehiculoPlaca : "Placa no disponible";
    }

    public String getVehiculoModelo() {
        return vehiculoModelo != null ? vehiculoModelo : "Modelo no disponible";
    }

    // NUEVOS GETTERS para el layout mejorado

    public String getOrigen() {
        return origen != null ? origen : "Natagá";
    }

    public String getDestino() {
        return destino != null ? destino : "La Plata";
    }

    // Método para formatear la hora (12h con AM/PM)
    public String getHoraFormateada() {
        if (horarioHora == null) return "Hora no disponible";

        try {
            String[] partes = horarioHora.split(":");
            if (partes.length >= 2) {
                int hora = Integer.parseInt(partes[0]);
                int minuto = Integer.parseInt(partes[1]);

                String periodo = hora >= 12 ? "PM" : "AM";
                if (hora > 12) hora -= 12;
                if (hora == 0) hora = 12;

                return String.format(Locale.getDefault(), "%d:%02d %s", hora, minuto, periodo);
            }
            return horarioHora;
        } catch (NumberFormatException e) {
            return horarioHora;
        }
    }

    // Métodos para actualizar datos del usuario (cuando se cargan del manager)

    public void setUsuarioNombre(String nombre) {
        this.usuarioNombre = nombre;
    }

    public void setUsuarioTelefono(String telefono) {
        this.usuarioTelefono = telefono;
    }

    public void setUsuarioId(String id) {
        this.usuarioId = id;
    }

    public String getUsuarioNombre() {
        return usuarioNombre != null ? usuarioNombre : "Usuario no disponible";
    }

    public String getUsuarioTelefono() {
        return usuarioTelefono != null ? usuarioTelefono : "Teléfono no disponible";
    }

    // Métodos para el método de pago

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
        Log.d(TAG, "Método de pago actualizado: " + metodoPago);
    }

    public String getMetodoPago() {
        return metodoPago != null ? metodoPago : "efectivo";
    }

    /**
     * Valida si todos los datos requeridos están presentes
     */
    public boolean isDataComplete() {
        boolean isComplete =
                asientoSeleccionado > 0 &&
                        rutaSeleccionada != null && !rutaSeleccionada.isEmpty() &&
                        horarioHora != null && !horarioHora.isEmpty() &&
                        fechaViaje != null && !fechaViaje.isEmpty() &&
                        conductorNombre != null && !conductorNombre.isEmpty();

        if (!isComplete) {
            Log.w(TAG, "⚠️ Datos incompletos detectados");
            Map<String, Object> params = new HashMap<>();
            params.put("asiento_seleccionado", asientoSeleccionado);
            params.put("tiene_ruta", rutaSeleccionada != null);
            params.put("tiene_horario", horarioHora != null);
            params.put("tiene_fecha", fechaViaje != null);
            params.put("tiene_conductor", conductorNombre != null);
            analyticsHelper.logEvent("validacion_datos_incompletos", params);
        }

        return isComplete;
    }

    /**
     * Obtiene un resumen de los datos procesados para logging
     */
    public String getSummary() {
        return String.format(
                "Resumen Confirmación:\n" +
                        "- Origen: %s\n" +
                        "- Destino: %s\n" +
                        "- Asiento: %d\n" +
                        "- Fecha: %s\n" +
                        "- Hora: %s\n" +
                        "- Conductor: %s\n" +
                        "- Vehículo: %s %s\n" +
                        "- Precio: $%,.0f\n" +
                        "- Tiempo estimado: %s\n" +
                        "- Método pago: %s",
                getOrigen(),
                getDestino(),
                asientoSeleccionado,
                getFechaViaje(),
                getHoraFormateada(),
                conductorNombre,
                vehiculoModelo,
                vehiculoPlaca,
                precio,
                tiempoEstimado,
                metodoPago
        );
    }
}