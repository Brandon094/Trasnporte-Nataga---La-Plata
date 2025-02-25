package com.chopcode.trasnportenataga_laplata.models;

public class Horario {
    private String ruta;  // Ruta de la hora
    private String hora;  // Hora de salida

    // Constructor vacío requerido por Firebase
    public Horario() { }

    public Horario(String ruta, String hora) {
        this.ruta = ruta;
        this.hora = hora;
    }

    public String getRuta() { return ruta; }
    public void setRuta(String ruta) { this.ruta = ruta; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }
}
