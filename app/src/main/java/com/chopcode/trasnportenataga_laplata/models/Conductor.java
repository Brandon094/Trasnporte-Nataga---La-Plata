package com.chopcode.trasnportenataga_laplata.models;

import java.util.List;

public class Conductor extends Usuario {
    private String vehiculoId;
    private String placaVehiculo, modeloVehiculo;
    private int capacidadVehiculo;
    private List<String> horariosAsignados;

    public String getVehiculoId() {
        return vehiculoId;
    }

    public void setVehiculoId(String vehiculoId) {
        this.vehiculoId = vehiculoId;
    }

    public String getPlacaVehiculo() {
        return placaVehiculo;
    }

    public void setPlacaVehiculo(String placaVehiculo) {
        this.placaVehiculo = placaVehiculo;
    }

    public String getModeloVehiculo() {
        return modeloVehiculo;
    }

    public void setModeloVehiculo(String modeloVehiculo) {
        this.modeloVehiculo = modeloVehiculo;
    }

    public int getCapacidadVehiculo() {
        return capacidadVehiculo;
    }

    public void setCapacidadVehiculo(int capacidadVehiculo) {
        this.capacidadVehiculo = capacidadVehiculo;
    }

    public List<String> getHorariosAsignados() { return horariosAsignados; }
    public void setHorariosAsignados(List<String> horariosAsignados) { this.horariosAsignados = horariosAsignados; }
}