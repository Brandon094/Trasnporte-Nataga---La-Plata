package com.chopcode.trasnportenataga_laplata.managers.reservations;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import com.chopcode.trasnportenataga_laplata.config.MyApp;
import com.chopcode.trasnportenataga_laplata.managers.analytics.ReservationAnalyticsHelper;
import com.chopcode.trasnportenataga_laplata.managers.seats.SeatManager;
import com.chopcode.trasnportenataga_laplata.models.Vehiculo;
import com.chopcode.trasnportenataga_laplata.services.user.UserService;
import com.chopcode.trasnportenataga_laplata.services.reservations.VehiculoService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manager para cargar y manejar información del conductor y vehículo
 */
public class DriverVehicleManager {

    private static final String TAG = "DriverVehicleManager";

    private final Context context;
    private final ReservationAnalyticsHelper analyticsHelper;
    private final SeatManager seatManager;
    private final UserService userService;
    private final VehiculoService vehiculoService;

    // Callback interface
    public interface DriverVehicleCallback {
        void onDriverVehicleLoaded(
                String conductorId,
                String conductorNombre,
                String conductorTelefono,
                String placaVehiculo,
                String modeloVehiculo,
                Integer capacidadVehiculo
        );

        void onError(String error);
    }

    // UI references
    private TextView tvNombreConductor;
    private TextView tvVehiculoInfo;
    private TextView tvCapacidadInfo;

    // Data
    private String conductorId;
    private String conductorNombre;
    private String conductorTelefono;
    private String placaVehiculo;
    private String modeloVehiculo;
    private Integer capacidadVehiculo;

    public DriverVehicleManager(
            Context context,
            ReservationAnalyticsHelper analyticsHelper,
            SeatManager seatManager) {

        this.context = context;
        this.analyticsHelper = analyticsHelper;
        this.seatManager = seatManager;
        this.userService = new UserService();
        this.vehiculoService = new VehiculoService();
    }

    public void setUIReferences(
            TextView tvNombreConductor,
            TextView tvVehiculoInfo,
            TextView tvCapacidadInfo) {

        this.tvNombreConductor = tvNombreConductor;
        this.tvVehiculoInfo = tvVehiculoInfo;
        this.tvCapacidadInfo = tvCapacidadInfo;
    }

    /**
     * Carga la información del conductor y vehículo para un horario
     */
    public void loadDriverVehicleInfo(String horarioId, DriverVehicleCallback callback) {
        if (horarioId == null || horarioId.isEmpty()) {
            callback.onError("Horario ID es nulo o vacío");
            return;
        }

        Log.d(TAG, "Buscando conductor para el horario: " + horarioId);

        Map<String, Object> params = new HashMap<>();
        params.put("accion", "carga_info_vehiculo_conductor_inicio");
        analyticsHelper.logEvent("carga_info_vehiculo_conductor_inicio", params);

        buscarConductorPorHorario(horarioId, callback);
    }

    /**
     * Busca conductor por horario asignado
     */
    private void buscarConductorPorHorario(String horarioId, DriverVehicleCallback callback) {
        DatabaseReference conductoresRef = MyApp.getDatabaseReference("conductores");

        conductoresRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean conductorEncontrado = false;

                for (DataSnapshot conductorSnapshot : snapshot.getChildren()) {
                    if (conductorSnapshot.hasChild("horariosAsignados")) {
                        DataSnapshot horariosAsignadosSnapshot = conductorSnapshot.child("horariosAsignados");

                        for (DataSnapshot horarioAsignadoSnapshot : horariosAsignadosSnapshot.getChildren()) {
                            String horarioAsignado = horarioAsignadoSnapshot.getValue(String.class);
                            if (horarioId.equals(horarioAsignado)) {
                                conductorId = conductorSnapshot.getKey();
                                Log.d(TAG, "Conductor encontrado: " + conductorId);

                                Map<String, Object> params = new HashMap<>();
                                params.put("conductor_encontrado", 1);
                                analyticsHelper.logEvent("conductor_encontrado", params);

                                cargarInformacionConductor(conductorId, callback);
                                conductorEncontrado = true;
                                break;
                            }
                        }
                    }
                    if (conductorEncontrado) break;
                }

                if (!conductorEncontrado) {
                    Log.w(TAG, "No se encontró conductor para el horario " + horarioId);
                    establecerValoresPorDefecto();

                    if (callback != null) {
                        callback.onDriverVehicleLoaded(
                                null,
                                "------",
                                "------",
                                "------",
                                "------",
                                seatManager.getCapacidadTotal()
                        );
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error buscando conductor: " + error.getMessage());
                MyApp.logError(new Exception("Error buscando conductor: " + error.getMessage()));
                analyticsHelper.logError("busqueda_conductor", error.getMessage());

                establecerValoresPorDefecto();

                if (callback != null) {
                    callback.onError("Error buscando conductor: " + error.getMessage());
                }
            }
        });
    }

    /**
     * Carga información detallada del conductor
     */
    private void cargarInformacionConductor(String conductorId, DriverVehicleCallback callback) {
        Log.d(TAG, "Cargando información del conductor: " + conductorId);

        Map<String, Object> params = new HashMap<>();
        params.put("accion", "carga_conductor_inicio");
        analyticsHelper.logEvent("carga_conductor_inicio", params);

        userService.loadDriverData(conductorId, new UserService.DriverDataCallback() {
            @Override
            public void onDriverDataLoaded(String nombre, String telefono, String placa, List<String> horariosAsignados) {
                if (nombre != null && !nombre.isEmpty()) {
                    conductorNombre = nombre;
                    conductorTelefono = telefono != null ? telefono : "No disponible";
                    placaVehiculo = placa != null ? placa : "No disponible";

                    updateUI();
                    analyticsHelper.logConductorCargado(conductorId, nombre, telefono);

                    Log.d(TAG, "✓ Conductor cargado: " + conductorNombre);
                    cargarInformacionVehiculo(conductorId, callback);
                } else {
                    establecerValoresPorDefecto();
                    if (callback != null) {
                        callback.onError("Datos del conductor incompletos");
                    }
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error cargando datos del conductor: " + error);
                MyApp.logError(new Exception("Error cargando datos conductor: " + error));
                analyticsHelper.logError("carga_conductor", error);

                establecerValoresPorDefecto();

                if (callback != null) {
                    callback.onError("Error cargando conductor: " + error);
                }
            }
        });
    }

    /**
     * Carga información del vehículo del conductor
     */
    private void cargarInformacionVehiculo(String conductorId, DriverVehicleCallback callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("accion", "carga_vehiculo_inicio");
        analyticsHelper.logEvent("carga_vehiculo_inicio", params);

        vehiculoService.obtenerVehiculoPorConductor(conductorId, new VehiculoService.VehiculoCallback() {
            @Override
            public void onVehiculoCargado(Vehiculo vehiculo) {
                if (vehiculo != null) {
                    modeloVehiculo = vehiculo.getModelo() != null ? vehiculo.getModelo() : "No disponible";
                    placaVehiculo = vehiculo.getPlaca() != null ? vehiculo.getPlaca() : placaVehiculo;
                    capacidadVehiculo = vehiculo.getCapacidad() > 0 ?
                            vehiculo.getCapacidad() : seatManager.getCapacidadTotal();

                    analyticsHelper.logVehiculoCargado(vehiculo, conductorId);
                    updateUI();

                    Log.d(TAG, "✓ Vehículo cargado: " + placaVehiculo + " - " + modeloVehiculo);

                    if (callback != null) {
                        callback.onDriverVehicleLoaded(
                                conductorId,
                                conductorNombre,
                                conductorTelefono,
                                placaVehiculo,
                                modeloVehiculo,
                                capacidadVehiculo
                        );
                    }
                } else {
                    capacidadVehiculo = seatManager.getCapacidadTotal();
                    updateUI();

                    Map<String, Object> params = new HashMap<>();
                    params.put("accion", "vehiculo_no_encontrado");
                    analyticsHelper.logEvent("vehiculo_no_encontrado", params);

                    if (callback != null) {
                        callback.onDriverVehicleLoaded(
                                conductorId,
                                conductorNombre,
                                conductorTelefono,
                                placaVehiculo,
                                modeloVehiculo,
                                capacidadVehiculo
                        );
                    }
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error cargando vehículo: " + error);
                MyApp.logError(new Exception("Error cargando vehículo: " + error));
                analyticsHelper.logError("carga_vehiculo", error);

                capacidadVehiculo = seatManager.getCapacidadTotal();
                updateUI();

                if (callback != null) {
                    callback.onError("Error cargando vehículo: " + error);
                }
            }
        });
    }

    /**
     * Actualiza la UI con la información cargada
     */
    private void updateUI() {
        if (tvNombreConductor != null) {
            tvNombreConductor.setText(conductorNombre != null ? conductorNombre : "------");
        }

        if (tvVehiculoInfo != null) {
            String infoVehiculo = "Vehículo: " +
                    (placaVehiculo != null ? placaVehiculo : "------") + " - " +
                    (modeloVehiculo != null ? modeloVehiculo : "------");
            tvVehiculoInfo.setText(infoVehiculo);
        }

        if (tvCapacidadInfo != null) {
            int capacidad = capacidadVehiculo != null ? capacidadVehiculo : seatManager.getCapacidadTotal();
            tvCapacidadInfo.setText("Capacidad: " + capacidad + " asientos");
        }
    }

    /**
     * Establece valores por defecto cuando hay error
     */
    private void establecerValoresPorDefecto() {
        conductorNombre = "------";
        conductorTelefono = "------";
        placaVehiculo = "------";
        modeloVehiculo = "------";
        capacidadVehiculo = seatManager.getCapacidadTotal();

        updateUI();

        Map<String, Object> params = new HashMap<>();
        params.put("accion", "valores_por_defecto_conductor");
        analyticsHelper.logEvent("valores_por_defecto_conductor", params);
    }

    // Getters para obtener la información cargada
    public String getConductorId() { return conductorId; }
    public String getConductorNombre() { return conductorNombre; }
    public String getConductorTelefono() { return conductorTelefono; }
    public String getPlacaVehiculo() { return placaVehiculo; }
    public String getModeloVehiculo() { return modeloVehiculo; }
    public Integer getCapacidadVehiculo() { return capacidadVehiculo; }
}