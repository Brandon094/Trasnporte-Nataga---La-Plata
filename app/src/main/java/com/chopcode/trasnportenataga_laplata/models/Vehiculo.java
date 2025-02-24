package com.chopcode.trasnportenataga_laplata.models;

public class Vehiculo {
    private String id;         // Identificador único del vehículo
    private String conductorId; // Relación con el usuario (conductor)
    private String placa;      // Placa del vehículo
    private String modelo;     // Modelo y marca del vehículo
    private int capacidad;     // Capacidad total de asientos

    public Vehiculo() { }

    public Vehiculo(String id, String conductorId, String placa, String modelo, int capacidad) {
        this.id = id;
        this.conductorId = conductorId;
        this.placa = placa;
        this.modelo = modelo;
        this.capacidad = capacidad;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getConductorId() { return conductorId; }
    public void setConductorId(String conductorId) { this.conductorId = conductorId; }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public int getCapacidad() { return capacidad; }
    public void setCapacidad(int capacidad) { this.capacidad = capacidad; }
}
