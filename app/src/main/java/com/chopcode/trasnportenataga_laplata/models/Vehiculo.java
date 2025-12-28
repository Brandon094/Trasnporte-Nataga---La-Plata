package com.chopcode.trasnportenataga_laplata.models;

public class Vehiculo {
    private String id;
    private String placa;
    private String modelo;
    private String marca;  // ✅ NUEVO: Campo marca agregado
    private String color;
    private String ano;
    private int capacidad;
    private String conductorId;
    private String estado;

    // ✅ Constructor vacío requerido para Firebase
    public Vehiculo() {}

    // ✅ Constructor actualizado con marca
    public Vehiculo(String id, String placa, String marca, String modelo, String color, String ano,
                    int capacidad, String conductorId, String estado) {
        this.id = id;
        this.placa = placa;
        this.marca = marca;
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

    public String getMarca() { return marca; }  // ✅ NUEVO: Getter para marca
    public void setMarca(String marca) { this.marca = marca; }  // ✅ NUEVO: Setter para marca

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getAno() { return ano; }
    public void setAno(String ano) { this.ano = ano; }

    public int getCapacidad() { return capacidad; }
    public void setCapacidad(int capacidad) { this.capacidad = capacidad; }

    public String getConductorId() { return conductorId; }
    public void setConductorId(String conductorId) { this.conductorId = conductorId; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    // ✅ NUEVO: Método toString para debugging
    @Override
    public String toString() {
        return "Vehiculo{" +
                "id='" + id + '\'' +
                ", placa='" + placa + '\'' +
                ", marca='" + marca + '\'' +
                ", modelo='" + modelo + '\'' +
                ", color='" + color + '\'' +
                ", ano='" + ano + '\'' +
                ", capacidad=" + capacidad +
                ", conductorId='" + conductorId + '\'' +
                ", estado='" + estado + '\'' +
                '}';
    }
}