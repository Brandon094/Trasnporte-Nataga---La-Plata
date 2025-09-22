package com.chopcode.trasnportenataga_laplata.services;

import androidx.annotation.NonNull;

import com.chopcode.trasnportenataga_laplata.models.Conductor;
import com.chopcode.trasnportenataga_laplata.models.Pasajero;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

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
        void onConductorCargado(Conductor conductor);
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
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("No hay un usuario autenticado.");
            return;
        }

        String conductorUid = currentUser.getUid();
        DatabaseReference refConductores = FirebaseDatabase.getInstance().getReference("conductores");

        // 🔥 Cambio importante: usar child(conductorUid) en lugar de iterar
        refConductores.child(conductorUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    callback.onError("No se encontró información para el conductor autenticado.");
                    return;
                }

                Conductor conductor = snapshot.getValue(Conductor.class);
                if (conductor != null) {
                    conductor.setId(snapshot.getKey());
                    callback.onConductorCargado(conductor);
                } else {
                    callback.onError("Error al obtener los datos del conductor.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("Error al cargar la información del conductor: " + error.getMessage());
            }
        });
    }

    /**
     * 🔥 NUEVO MÉTODO: Carga información de conductor por UID específico
     * (útil si necesitas cargar desde otra actividad)
     */
    public void cargarInformacionConductorPorUid(String conductorUid, @NonNull final ConductorCallback callback) {
        DatabaseReference refConductores = FirebaseDatabase.getInstance().getReference("conductores");

        refConductores.child(conductorUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    callback.onError("No se encontró información para el conductor con UID: " + conductorUid);
                    return;
                }

                Conductor conductor = snapshot.getValue(Conductor.class);
                if (conductor != null) {
                    conductor.setId(snapshot.getKey());
                    callback.onConductorCargado(conductor);
                } else {
                    callback.onError("Error al obtener los datos del conductor.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("Error al cargar la información del conductor: " + error.getMessage());
            }
        });
    }
}
