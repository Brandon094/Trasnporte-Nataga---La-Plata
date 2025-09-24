package com.chopcode.trasnportenataga_laplata.models;

public class Vehiculo {
    private String id;
    private String placa;
    private String modelo;
    private String marca;
    private int anio;
    private int capacidad;
    private String color;
    private String tipo; // Sedan, SUV, Camioneta, etc.
    private boolean activo;

    public Vehiculo() {
        // Constructor vacío requerido para Firebase
    }

    public Vehiculo(String id, String placa, String modelo, String marca, int anio,
                    int capacidad, String color, String tipo) {
        this.id = id;
        this.placa = placa;
        this.modelo = modelo;
        this.marca = marca;
        this.anio = anio;
        this.capacidad = capacidad;
        this.color = color;
        this.tipo = tipo;
        this.activo = true;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public int getAnio() { return anio; }
    public void setAnio(int anio) { this.anio = anio; }

    public int getCapacidad() { return capacidad; }
    public void setCapacidad(int capacidad) { this.capacidad = capacidad; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}