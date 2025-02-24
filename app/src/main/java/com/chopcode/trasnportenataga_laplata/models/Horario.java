package com.chopcode.trasnportenataga_laplata.models;

public class Horario {
    private String id;          // Identificador único del horario
    private String rutaId;      // Relación con la ruta
    private String horaSalida;  // Hora de salida

    public Horario() { }

    public Horario(String id, String rutaId, String horaSalida) {
        this.id = id;
        this.rutaId = rutaId;
        this.horaSalida = horaSalida;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRutaId() { return rutaId; }
    public void setRutaId(String rutaId) { this.rutaId = rutaId; }

    public String getHoraSalida() { return horaSalida; }
    public void setHoraSalida(String horaSalida) { this.horaSalida = horaSalida; }
}
