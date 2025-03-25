package com.chopcode.trasnportenataga_laplata.services;

import androidx.annotation.NonNull;
import com.chopcode.trasnportenataga_laplata.models.Pasajero;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class UsuarioService {

    private final FirebaseAuth auth;
    private final DatabaseReference databaseReference;

    // Constructor
    public UsuarioService() {
        this.auth = FirebaseAuth.getInstance();
        this.databaseReference = FirebaseDatabase.getInstance().getReference("usuarios");
    }

    /**
     * Callback para manejar la carga de la información del usuario de forma asíncrona.
     */
    public interface UsuarioCallback {
        void onUsuarioCargado(Pasajero pasajero);
        void onError(String error);
    }

    /**
     * Callback para manejar la carga del conductor de forma asíncrona.
     */
    public interface ConductorCallback {
        void onConductorCargado(String nombre, String placa, String telefono);
        void onError(String error);
    }

    /**
     * 🔥 Carga la información del usuario autenticado (solo pasajeros).
     */
    public void cargarInformacionPasajero(@NonNull final UsuarioCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("No hay un usuario autenticado.");
            return;
        }

        String uid = currentUser.getUid();
        databaseReference.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    callback.onError("No se encontraron datos del usuario en la base de datos.");
                    return;
                }

                // Verifica si el usuario es un conductor
                String tipoUsuario = snapshot.child("tipoUsuario").getValue(String.class);
                if ("conductor".equals(tipoUsuario)) {
                    callback.onError("Este usuario es un conductor y no puede realizar reservas.");
                    return;
                }

                // Obtener datos del pasajero
                Pasajero pasajero = snapshot.getValue(Pasajero.class);
                if (pasajero != null) {
                    callback.onUsuarioCargado(pasajero);
                } else {
                    callback.onError("Error al obtener los datos del pasajero.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("Error al cargar la información del usuario: " + error.getMessage());
            }
        });
    }

    /**
     * 🔥 Carga la información del primer conductor disponible en la base de datos.
     */
    public void cargarInformacionConductor(@NonNull final ConductorCallback callback) {
        databaseReference.orderByChild("tipoUsuario").equalTo("conductor")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            callback.onError("No se encontraron conductores registrados.");
                            return;
                        }

                        for (DataSnapshot conductorSnapshot : snapshot.getChildren()) {
                            String nombre = conductorSnapshot.child("nombre").getValue(String.class);
                            String placa = conductorSnapshot.child("placaVehiculo").getValue(String.class);
                            String telefono = conductorSnapshot.child("telefono").getValue(String.class);

                            if (nombre != null && placa != null && telefono != null) {
                                callback.onConductorCargado(nombre, placa, telefono);
                                return; // Solo necesitamos el primer conductor encontrado
                            }
                        }

                        callback.onError("No se pudo obtener la información del conductor.");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError("Error al cargar el conductor: " + error.getMessage());
                    }
                });
    }
}
