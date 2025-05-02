package com.chopcode.trasnportenataga_laplata.models;

public class Pasajero extends Usuario {

    String rol = "pasajero";

    public Pasajero() {
        super();
    }

    public Pasajero(String id, String nombre, String telefono, String email, String rol, String password) {
        super(id, nombre, telefono, email, password);
    }

    @Override
    public String toString() {
        return "Pasajero: "+
                "id=" + id +
                "nombre=" + nombre +
                "telefono=" + telefono +
                "email=" + email +
                "rol=" + rol;
    }

    // Métodos específicos para pasajeros, por ejemplo, para reservar asientos, pueden agregarse aquí.

}
