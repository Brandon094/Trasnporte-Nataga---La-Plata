package com.chopcode.trasnportenataga_laplata.models;

import java.util.Date;

public class Reserva {
    private String idReserva;      // Identificador único de la reserva
    private String usuarioId;      // 🔥 Relaciona la reserva con el usuario que la hizo
    private String horarioId;      // 🔥 Relaciona la reserva con un horario específico
    private Integer puestoReservado;
    private String conductorId;    // 🔥 Relaciona con el conductor
    private String vehiculoId;     // 🔥 Relaciona con el vehículo
    private double precio;         // 🔥 Cambié BigDecimal a double (Firebase no lo soporta)
    private String origen;
    private String destino;
    private String tiempoEstimado;
    private String metodoPago;
    private String estadoReserva;
    private long fechaReserva;     // 🔥 Usar timestamp (para Firebase)

    public Reserva() { }

    public Reserva(String idReserva, String usuarioId, String horarioId, Integer puestoReservado,
                   String conductorId, String vehiculoId, double precio, String origen, String destino,
                   String tiempoEstimado, String metodoPago, String estadoReserva, long fechaReserva) {
        this.idReserva = idReserva;
        this.usuarioId = usuarioId;
        this.horarioId = horarioId;
        this.puestoReservado = puestoReservado;
        this.conductorId = conductorId;
        this.vehiculoId = vehiculoId;
        this.precio = precio;
        this.origen = origen;
        this.destino = destino;
        this.tiempoEstimado = tiempoEstimado;
        this.metodoPago = metodoPago;
        this.estadoReserva = estadoReserva;
        this.fechaReserva = fechaReserva;
    }

    // Getters y Setters
    public String getIdReserva() { return idReserva; }
    public void setIdReserva(String idReserva) { this.idReserva = idReserva; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public String getHorarioId() { return horarioId; }
    public void setHorarioId(String horarioId) { this.horarioId = horarioId; }

    public Integer getPuestoReservado() { return puestoReservado; }
    public void setPuestoReservado(Integer puestoReservado) { this.puestoReservado = puestoReservado; }

    public String getConductorId() { return conductorId; }
    public void setConductorId(String conductorId) { this.conductorId = conductorId; }

    public String getVehiculoId() { return vehiculoId; }
    public void setVehiculoId(String vehiculoId) { this.vehiculoId = vehiculoId; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }

    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }

    public String getTiempoEstimado() { return tiempoEstimado; }
    public void setTiempoEstimado(String tiempoEstimado) { this.tiempoEstimado = tiempoEstimado; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getEstadoReserva() { return estadoReserva; }
    public void setEstadoReserva(String estadoReserva) { this.estadoReserva = estadoReserva; }

    public long getFechaReserva() { return fechaReserva; }
    public void setFechaReserva(long fechaReserva) { this.fechaReserva = fechaReserva; }
}
