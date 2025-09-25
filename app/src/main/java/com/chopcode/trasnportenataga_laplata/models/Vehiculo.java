package com.chopcode.trasnportenataga_laplata.models;

public class Vehiculo {
    private String id;
    private String placa;
    private String modelo;
    private String color;
    private String ano;
    private int capacidad;
    private String conductorId;
    private String estado;

    // ✅ Constructor vacío requerido para Firebase
    public Vehiculo() {}

    public Vehiculo(String id, String placa, String modelo, String color, String ano,
                    int capacidad, String conductorId, String estado) {
        this.id = id;
        this.placa = placa;
        this.modelo = modelo;
        this.color = color;
        this.ano = ano;
        this.capacidad = capacidad;
        this.conductorId = conductorId;
        this.estado = estado;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getAno() { return ano; }
    public void setAno(String ano) { this.ano = ano; }  // ✅ CORREGIDO

    public int getCapacidad() { return capacidad; }
    public void setCapacidad(int capacidad) { this.capacidad = capacidad; }

    public String getConductorId() { return conductorId; }
    public void setConductorId(String conductorId) { this.conductorId = conductorId; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}