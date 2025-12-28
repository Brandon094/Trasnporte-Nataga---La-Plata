package com.chopcode.rutago_app.models;

public class Pasajero extends Usuario {

    String rol = "pasajero";

    public Pasajero() {
        super();
    }
    // Constructor para Correo y contraseña
    public Pasajero(String id, String nombre, String telefono, String email, String password) {
        super(id, nombre, telefono, email, password);
    }
    // Constructor para iniciar con google
    public Pasajero(String id, String nombre, String telefono, String email){
        super(id, nombre, telefono, email);
    }

    // Metodo toString
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
