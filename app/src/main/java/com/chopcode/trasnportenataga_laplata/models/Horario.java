package com.chopcode.trasnportenataga_laplata.models;

import java.io.Serializable;

public class Horario implements Serializable {
    private String id;      // 🔥 NUEVO: ID del horario (h001, h002, etc.)
    private String ruta;    // Ruta de la hora (ej: "Natagá → La Plata")
    private String hora;    // Hora de salida (ej: "08:00 AM")
    private String duracion; // Duración del viaje (ej: "60 min")
    private String precio;  // Precio del pasaje (ej: "12.000")

    // Constructor vacío requerido por Firebase
    public Horario() { }

    public Horario(String id, String ruta, String hora, String duracion, String precio) {
        this.id = id;
        this.ruta = ruta;
        this.hora = hora;
        this.duracion = duracion;
        this.precio = precio;
    }

    // Constructor simplificado
    public Horario(String id, String ruta, String hora) {
        this.id = id;
        this.ruta = ruta;
        this.hora = hora;
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
}