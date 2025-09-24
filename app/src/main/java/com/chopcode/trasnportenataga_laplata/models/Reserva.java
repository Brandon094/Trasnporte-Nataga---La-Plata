package com.chopcode.trasnportenataga_laplata.models;

public class Reserva {
    private String idReserva, usuarioId, horarioId, conductor, vehiculoId, origen, destino,
            tiempoEstimado, metodoPago,
            estadoReserva, nombre, telefono, telefonoC, email;
    private long fechaReserva;
    private double precio;
    private int puestoReservado;

    // Constructor vacío (OBLIGATORIO para Firebase)
    public Reserva() { }

    public Reserva(String idReserva, String usuarioId, String horarioId, Integer puestoReservado,
                   String conductor, String telefonoC, String vehiculoId, double precio,
                   String origen, String destino, String tiempoEstimado, String metodoPago,
                   String estadoReserva, long fechaReserva, String nombre, String telefono,
                   String email) {
        this.idReserva = idReserva;
        this.usuarioId = usuarioId;
        this.horarioId = horarioId;
        this.puestoReservado = puestoReservado;
        this.conductor = conductor;
        this.vehiculoId = vehiculoId;
        this.precio = precio;
        this.origen = origen;
        this.destino = destino;
        this.tiempoEstimado = tiempoEstimado;
        this.metodoPago = metodoPago;
        this.estadoReserva = estadoReserva;
        this.fechaReserva = fechaReserva;
        this.nombre = nombre;
        this.telefono = telefono;
        this.telefonoC = telefonoC;
        this.email = email;
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

    // ✅ CORREGIDO: Firebase espera getConductor() para el campo "conductor"
    public String getConductor() { return conductor; }
    public void setConductor(String conductor) { this.conductor = conductor; }

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

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefonoC() { return telefonoC; }
    public void setTelefonoC(String telefonoC) { this.telefonoC = telefonoC; }
}