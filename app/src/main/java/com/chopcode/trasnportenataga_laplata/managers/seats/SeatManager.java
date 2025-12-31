package com.chopcode.trasnportenataga_laplata.managers.seats;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.managers.analytics.ReservationAnalyticsHelper;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manager dedicado a manejar la configuración y estado de los asientos
 */
public class SeatManager {
    private final Context context;
    private final Map<Integer, MaterialButton> mapaAsientos = new HashMap<>();
    private Integer asientoSeleccionado = null;
    private Set<Integer> asientosOcupados = new HashSet<>();
    private final ReservationAnalyticsHelper analyticsHelper;

    // Iconos de los asientos
    private final int VECTOR_ASIENTO_DISPONIBLE = R.drawable.asiento_disponible;
    private final int VECTOR_ASIENTO_SELECCIONADO = R.drawable.asiento_seleccionado;
    private final int VECTOR_ASIENTO_OCUPADO = R.drawable.asiento_ocupado;

    // ✅ AGREGADO: IDs de los botones de asientos
    private static final int[] BOTONES_ASIENTOS_IDS = {
            R.id.btnAsiento1, R.id.btnAsiento2, R.id.btnAsiento3, R.id.btnAsiento4,
            R.id.btnAsiento5, R.id.btnAsiento6, R.id.btnAsiento7, R.id.btnAsiento8,
            R.id.btnAsiento9, R.id.btnAsiento10, R.id.btnAsiento11, R.id.btnAsiento12,
            R.id.btnAsiento13
    };

    // Interface para comunicar eventos a la actividad
    public interface SeatSelectionListener {
        void onSeatSelected(int seatNumber);
        void onSeatDeselected(int seatNumber);
        void onExpandableSectionRequestedToCollapse();
    }

    private SeatSelectionListener listener;

    public SeatManager(Context context, ReservationAnalyticsHelper analyticsHelper) {
        this.context = context;
        this.analyticsHelper = analyticsHelper;
    }

    public void setSeatSelectionListener(SeatSelectionListener listener) {
        this.listener = listener;
    }

    /**
     * ✅ MODIFICADO: Configura todos los botones de asientos automáticamente
     * Usa los IDs predefinidos en la constante BOTONES_ASIENTOS_IDS
     */
    public void configurarAsientos() {
        for (int i = 0; i < BOTONES_ASIENTOS_IDS.length; i++) {
            MaterialButton btnAsiento = ((android.app.Activity) context).findViewById(BOTONES_ASIENTOS_IDS[i]);
            int numeroAsiento = i + 1;
            configurarBotonAsiento(btnAsiento, numeroAsiento);
        }

        // Registrar evento analítico
        Map<String, Object> params = new HashMap<>();
        params.put("total_asientos", BOTONES_ASIENTOS_IDS.length);
        analyticsHelper.logEvent("asientos_configurados", params);

        Log.d("SeatManager", "✅ Asientos configurados automáticamente: " + BOTONES_ASIENTOS_IDS.length + " asientos");
    }

    /**
     * ✅ MANTENIDO: Método flexible para configurar asientos con IDs personalizados
     * Útil si otra actividad tiene diferente número o IDs de asientos
     */
    public void configurarAsientos(int[] botonesIds) {
        for (int i = 0; i < botonesIds.length; i++) {
            MaterialButton btnAsiento = ((android.app.Activity) context).findViewById(botonesIds[i]);
            int numeroAsiento = i + 1;
            configurarBotonAsiento(btnAsiento, numeroAsiento);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("total_asientos", botonesIds.length);
        analyticsHelper.logEvent("asientos_configurados", params);

        Log.d("SeatManager", "✅ Asientos configurados con IDs personalizados: " + botonesIds.length + " asientos");
    }

    /**
     * Configura un botón de asiento individual
     */
    private void configurarBotonAsiento(MaterialButton btnAsiento, int numeroAsiento) {
        btnAsiento.setTag(numeroAsiento);
        btnAsiento.setVisibility(View.VISIBLE);

        // IMPORTANTE: Remover el tint del icono para que se muestren los colores correctos
        btnAsiento.setIconTint(null);

        mapaAsientos.put(numeroAsiento, btnAsiento);
    }

    /**
     * Actualiza el estado de los asientos basado en los ocupados
     */
    public void actualizarEstadoAsientos(Set<Integer> ocupados, int capacidadTotal) {
        this.asientosOcupados = ocupados != null ? ocupados : new HashSet<>();

        for (Map.Entry<Integer, MaterialButton> entry : mapaAsientos.entrySet()) {
            int numAsiento = entry.getKey();
            MaterialButton btn = entry.getValue();

            if (asientosOcupados.contains(numAsiento)) {
                marcarAsientoOcupado(btn);
            } else {
                configurarAsientoDisponible(btn, numAsiento);
            }
        }

        // Registrar evento analítico
        analyticsHelper.logAsientosCargados(ocupados.size(), capacidadTotal, null);

        Log.d("SeatManager", "✅ Estado de asientos actualizado. Ocupados: " + ocupados.size());
    }

    /**
     * Marca un asiento como ocupado
     */
    private void marcarAsientoOcupado(MaterialButton btn) {
        btn.setIcon(ContextCompat.getDrawable(context, VECTOR_ASIENTO_OCUPADO));
        btn.setEnabled(false);
        btn.setOnClickListener(null); // Remover cualquier listener previo
    }

    /**
     * Configura un asiento como disponible
     */
    private void configurarAsientoDisponible(MaterialButton btn, int numAsiento) {
        btn.setIcon(ContextCompat.getDrawable(context, VECTOR_ASIENTO_DISPONIBLE));
        btn.setEnabled(true);

        btn.setOnClickListener(v -> manejarSeleccionAsiento(numAsiento));
    }

    /**
     * Maneja la lógica cuando se selecciona un asiento
     */
    private void manejarSeleccionAsiento(int numAsiento) {
        // Deseleccionar asiento previo si existe
        if (asientoSeleccionado != null) {
            deseleccionarAsiento(asientoSeleccionado);
        }

        // Seleccionar nuevo asiento
        seleccionarAsiento(numAsiento);

        // Mostrar mensaje al usuario
        Toast.makeText(context, "Asiento seleccionado: " + asientoSeleccionado, Toast.LENGTH_SHORT).show();

        // Registrar evento analítico
        analyticsHelper.logAsientoSeleccionado(numAsiento);

        // Solicitar colapsar la sección expandible si está expandida
        if (listener != null) {
            listener.onExpandableSectionRequestedToCollapse();
        }

        Log.d("SeatManager", "✅ Asiento seleccionado: " + numAsiento);
    }

    /**
     * Selecciona un asiento
     */
    private void seleccionarAsiento(int numAsiento) {
        asientoSeleccionado = numAsiento;
        MaterialButton btn = mapaAsientos.get(numAsiento);
        if (btn != null) {
            btn.setIcon(ContextCompat.getDrawable(context, VECTOR_ASIENTO_SELECCIONADO));
        }

        if (listener != null) {
            listener.onSeatSelected(numAsiento);
        }
    }

    /**
     * Deselecciona un asiento
     */
    private void deseleccionarAsiento(int numAsiento) {
        MaterialButton btn = mapaAsientos.get(numAsiento);
        if (btn != null) {
            btn.setIcon(ContextCompat.getDrawable(context, VECTOR_ASIENTO_DISPONIBLE));
        }

        if (listener != null) {
            listener.onSeatDeselected(numAsiento);
        }
    }

    /**
     * Obtiene el asiento seleccionado actualmente
     */
    public Integer getAsientoSeleccionado() {
        return asientoSeleccionado;
    }

    /**
     * Establece un asiento seleccionado (útil para restaurar estado)
     */
    public void setAsientoSeleccionado(Integer asientoSeleccionado) {
        this.asientoSeleccionado = asientoSeleccionado;
        if (asientoSeleccionado != null && mapaAsientos.containsKey(asientoSeleccionado)) {
            seleccionarAsiento(asientoSeleccionado);
        }
    }

    /**
     * Limpia la selección actual de asiento
     */
    public void limpiarSeleccion() {
        if (asientoSeleccionado != null) {
            deseleccionarAsiento(asientoSeleccionado);
            asientoSeleccionado = null;
        }
    }

    /**
     * Obtiene la capacidad total de asientos
     */
    public int getCapacidadTotal() {
        return mapaAsientos.size();
    }

    /**
     * Obtiene la cantidad de asientos disponibles
     */
    public int getCapacidadDisponible() {
        return getCapacidadTotal() - asientosOcupados.size();
    }

    /**
     * Obtiene la cantidad de asientos ocupados
     */
    public int getAsientosOcupadosCount() {
        return asientosOcupados.size();
    }

    /**
     * Verifica si un asiento específico está ocupado
     */
    public boolean isAsientoOcupado(int numAsiento) {
        return asientosOcupados.contains(numAsiento);
    }

    /**
     * Verifica si hay un asiento seleccionado
     */
    public boolean hasAsientoSeleccionado() {
        return asientoSeleccionado != null;
    }

    /**
     * Obtiene todos los asientos ocupados
     */
    public Set<Integer> getAsientosOcupados() {
        return new HashSet<>(asientosOcupados);
    }

    /**
     * ✅ NUEVO: Obtiene los IDs de los botones de asientos
     */
    public static int[] getBotonesAsientosIds() {
        return BOTONES_ASIENTOS_IDS.clone(); // Devolver copia para evitar modificaciones
    }

    /**
     * ✅ NUEVO: Obtiene el número total de asientos configurados
     */
    public static int getNumeroTotalAsientos() {
        return BOTONES_ASIENTOS_IDS.length;
    }

    /**
     * Limpia todos los recursos
     */
    public void cleanup() {
        mapaAsientos.clear();
        asientosOcupados.clear();
        asientoSeleccionado = null;
        listener = null;
    }
}