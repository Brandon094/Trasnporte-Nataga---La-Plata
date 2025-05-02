package com.chopcode.trasnportenataga_laplata.models;

public class Usuario {
    protected String id;
    protected String nombre;
    protected String telefono;
    protected String email;
    protected String password;

    // Constructor vacío necesario para Firebase
    public Usuario() { }

    // Constructor para atributos comunes
    public Usuario(String id, String nombre, String telefono, String email, String password) {
        this.id = id;
        this.nombre = nombre;
        this.telefono = telefono;
        this.email = email;
        this.password = password;
    }

    // Getters y setters comunes
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() {return password;}
    public void setPassword(String password){this.password = password;}
}
