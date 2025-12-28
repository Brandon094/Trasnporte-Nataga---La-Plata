package com.chopcode.rutago_app.models;

public class DisponibilidadAsientos {
    private String horarioId;  // Relacionado con un horario específico
    private int totalAsientos; // Número total de asientos en la ruta
    private int asientosDisponibles; // Cuántos quedan

    public DisponibilidadAsientos() { }

    public DisponibilidadAsientos(String horarioId, int totalAsientos, int asientosDisponibles) {
        this.horarioId = horarioId;
        this.totalAsientos = totalAsientos;
        this.asientosDisponibles = asientosDisponibles;
    }

    public String getHorarioId() { return horarioId; }
    public void setHorarioId(String horarioId) { this.horarioId = horarioId; }

    public int getTotalAsientos() { return totalAsientos; }
    public void setTotalAsientos(int totalAsientos) { this.totalAsientos = totalAsientos; }

    public int getAsientosDisponibles() { return asientosDisponibles; }
    public void setAsientosDisponibles(int asientosDisponibles) { this.asientosDisponibles = asientosDisponibles; }
}
