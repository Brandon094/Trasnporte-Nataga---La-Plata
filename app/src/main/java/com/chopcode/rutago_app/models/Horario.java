package com.chopcode.rutago_app.models;

import java.io.Serializable;

public class Horario implements Serializable {
    private String id;      // ID del horario (h001, h002, etc.)
    private String ruta;    // Ruta de la hora (ej: "Natagá → La Plata")
    private String hora;    // Hora de salida (ej: "08:00 AM")
    private String duracion; // Duración del viaje (ej: "60 min")
    private String precio;  // Precio del pasaje (ej: "12.000")
    private int asientosDisponibles; // Manejar asientos disponibles
    private int capacidadTotal;      // Capacidad total del vehículo

    public Horario() { }

    public Horario(String id, String ruta, String hora, String duracion, String precio,
                   int asientosDisponibles, int capacidadTotal) {
        this.id = id;
        this.ruta = ruta;
        this.hora = hora;
        this.duracion = duracion;
        this.precio = precio;
        this.asientosDisponibles = asientosDisponibles;
        this.capacidadTotal = capacidadTotal;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRuta() { return ruta; }
    public void setRuta(String ruta) { this.ruta = ruta; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }

    public String getDuracion() { return duracion; }
    public void setDuracion(String duracion) { this.duracion = duracion; }

    public String getPrecio() { return precio; }
    public void setPrecio(String precio) { this.precio = precio; }

    public int getAsientosDisponibles() { return asientosDisponibles; }
    public void setAsientosDisponibles(int asientosDisponibles) {
        this.asientosDisponibles = asientosDisponibles;
    }

    public int getCapacidadTotal() { return capacidadTotal; }
    public void setCapacidadTotal(int capacidadTotal) {
        this.capacidadTotal = capacidadTotal;
    }
}