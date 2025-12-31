package com.chopcode.trasnportenataga_laplata.managers.reservations;

import android.os.Bundle;

import com.chopcode.trasnportenataga_laplata.managers.seats.SeatManager;
import com.chopcode.trasnportenataga_laplata.managers.ui.ExpandableSectionManager;

/**
 * Manager para manejar el estado de la reserva (save/restore instance state)
 */
public class ReservationStateManager {

    // Keys for savedInstanceState
    private static final String KEY_ASIENTO_SELECCIONADO = "asientoSeleccionado";
    private static final String KEY_RUTA_SELECCIONADA = "rutaSeleccionada";
    private static final String KEY_CONDUCTOR_NOMBRE = "conductorNombre";
    private static final String KEY_CONDUCTOR_TELEFONO = "conductorTelefono";
    private static final String KEY_INFO_EXPANDED = "isInfoExpanded";
    private static final String KEY_USUARIO_NOMBRE = "usuarioNombre";
    private static final String KEY_USUARIO_TELEFONO = "usuarioTelefono";
    private static final String KEY_USUARIO_ID = "usuarioId";

    /**
     * Guarda el estado de la reserva en el Bundle
     */
    public static void saveState(
            Bundle outState,
            SeatManager seatManager,
            String rutaSeleccionada,
            String conductorNombre,
            String conductorTelefono,
            ExpandableSectionManager expandableSectionManager,
            String usuarioNombre,
            String usuarioTelefono,
            String usuarioId) {

        if (seatManager.hasAsientoSeleccionado()) {
            outState.putInt(KEY_ASIENTO_SELECCIONADO, seatManager.getAsientoSeleccionado());
        }

        if (rutaSeleccionada != null) {
            outState.putString(KEY_RUTA_SELECCIONADA, rutaSeleccionada);
        }

        outState.putString(KEY_CONDUCTOR_NOMBRE, conductorNombre);
        outState.putString(KEY_CONDUCTOR_TELEFONO, conductorTelefono);

        if (expandableSectionManager != null) {
            outState.putBoolean(KEY_INFO_EXPANDED, expandableSectionManager.isExpanded());
        }

        if (usuarioNombre != null) outState.putString(KEY_USUARIO_NOMBRE, usuarioNombre);
        if (usuarioTelefono != null) outState.putString(KEY_USUARIO_TELEFONO, usuarioTelefono);
        if (usuarioId != null) outState.putString(KEY_USUARIO_ID, usuarioId);
    }

    /**
     * Restaura el estado desde el Bundle
     */
    public static RestoredState restoreState(
            Bundle savedInstanceState,
            SeatManager seatManager,
            ExpandableSectionManager expandableSectionManager) {

        RestoredState restoredState = new RestoredState();

        if (savedInstanceState != null) {
            int savedAsiento = savedInstanceState.getInt(KEY_ASIENTO_SELECCIONADO, -1);
            if (savedAsiento != -1) {
                seatManager.setAsientoSeleccionado(savedAsiento);
                restoredState.asientoSeleccionado = savedAsiento;
            }

            restoredState.rutaSeleccionada = savedInstanceState.getString(KEY_RUTA_SELECCIONADA);
            restoredState.conductorNombre = savedInstanceState.getString(KEY_CONDUCTOR_NOMBRE, "Cargando...");
            restoredState.conductorTelefono = savedInstanceState.getString(KEY_CONDUCTOR_TELEFONO);

            boolean isInfoExpanded = savedInstanceState.getBoolean(KEY_INFO_EXPANDED, true);
            if (expandableSectionManager != null) {
                expandableSectionManager.restoreState(isInfoExpanded);
                restoredState.isInfoExpanded = isInfoExpanded;
            }

            restoredState.usuarioNombre = savedInstanceState.getString(KEY_USUARIO_NOMBRE);
            restoredState.usuarioTelefono = savedInstanceState.getString(KEY_USUARIO_TELEFONO);
            restoredState.usuarioId = savedInstanceState.getString(KEY_USUARIO_ID);
        }

        return restoredState;
    }

    /**
     * Clase para contener el estado restaurado
     */
    public static class RestoredState {
        public Integer asientoSeleccionado;
        public String rutaSeleccionada;
        public String conductorNombre;
        public String conductorTelefono;
        public Boolean isInfoExpanded;
        public String usuarioNombre;
        public String usuarioTelefono;
        public String usuarioId;
    }
}