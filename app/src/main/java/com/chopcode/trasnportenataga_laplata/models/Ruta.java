package com.chopcode.trasnportenataga_laplata.models;

public class Ruta {
    private String id;      // Identificador único de la ruta
    private String origen;  // Lugar de salida
    private String destino; // Lugar de llegada
    private double tarifa;  // Precio del pasaje
    private Horario hora;

    public Ruta() { }

    public Ruta(String id, String origen, String destino, double tarifa) {
        this.id = id;
        this.origen = origen;
        this.destino = destino;
        this.tarifa = tarifa;
        this.hora = hora;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }

    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public double getTarifa() { return tarifa; }
    public void setTarifa(double tarifa) { this.tarifa = tarifa; }

    public Horario getHora() {
        return hora;
    }

    public void setHora(Horario hora) {
        this.hora = hora;
    }
}
