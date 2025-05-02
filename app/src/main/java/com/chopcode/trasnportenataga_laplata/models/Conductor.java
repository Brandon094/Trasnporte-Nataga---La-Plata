package com.chopcode.trasnportenataga_laplata.models;

public class Conductor extends Usuario {
    private String placaVehiculo;
    private String modeloVehiculo;
    private int capacidadVehiculo;  // Número de asientos disponibles

    public Conductor() {
        super();
    }

    public Conductor(String id, String nombre, String telefono, String email, String password,
                     String placaVehiculo, String modeloVehiculo, int capacidadVehiculo) {
        super(id, nombre, telefono, email, password);
        this.placaVehiculo = placaVehiculo;
        this.modeloVehiculo = modeloVehiculo;
        this.capacidadVehiculo = capacidadVehiculo;
    }

    // Getters y Setters específicos para conductores
    public String getPlacaVehiculo() { return placaVehiculo; }
    public void setPlacaVehiculo(String placaVehiculo) { this.placaVehiculo = placaVehiculo; }

    public String getModeloVehiculo() { return modeloVehiculo; }
    public void setModeloVehiculo(String modeloVehiculo) { this.modeloVehiculo = modeloVehiculo; }

    public int getCapacidadVehiculo() { return capacidadVehiculo; }
    public void setCapacidadVehiculo(int capacidadVehiculo) { this.capacidadVehiculo = capacidadVehiculo; }
}
