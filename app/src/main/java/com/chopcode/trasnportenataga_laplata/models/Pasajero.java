package com.chopcode.trasnportenataga_laplata.models;

public class Pasajero extends Usuario {

    // Si en el futuro agregas atributos específicos para pasajeros, los defines aquí.

    public Pasajero() {
        super();
    }

    public Pasajero(String id, String nombre, String telefono, String email) {
        super(id, nombre, telefono, email);
    }

    // Métodos específicos para pasajeros, por ejemplo, para reservar asientos, pueden agregarse aquí.
}
