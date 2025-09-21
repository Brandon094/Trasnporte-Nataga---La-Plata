package com.chopcode.trasnportenataga_laplata.models;

import java.io.Serializable;

public class Horario implements Serializable {
    private String ruta;  // Ruta de la hora (ej: "Natagá → La Plata")
    private String hora;  // Hora de salida (ej: "08:00 AM")
    private String duracion; // Duración del viaje (ej: "60 min")
    private String precio; // Precio del pasaje (ej: "12.000")

    // Constructor vacío requerido por Firebase
    public Horario() { }

    public Horario(String ruta, String hora, String duracion, String precio) {
        this.ruta = ruta;
        this.hora = hora;
    }

    // Constructor simplificado
    public Horario(String ruta, String hora) {
        this.ruta = ruta;
        this.hora = hora;
    }

    public String getRuta() { return ruta; }
    public void setRuta(String ruta) { this.ruta = ruta; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }

    public String getDuracion() { return duracion; }
    public void setDuracion(String duracion) { this.duracion = duracion; }
}