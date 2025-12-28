package com.chopcode.trasnportenataga_laplata.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class RegistroConductoresService {

    public void registrarConductoresExistentes() {
        DatabaseReference conductoresRef = FirebaseDatabase.getInstance().getReference("conductores");

        conductoresRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot conductorSnapshot : snapshot.getChildren()) {
                    try {
                        // Usar getValue() sin tipo específico y luego convertir
                        Object emailObj = conductorSnapshot.child("email").getValue();
                        Object passwordObj = conductorSnapshot.child("contraseña").getValue();
                        Object nombreObj = conductorSnapshot.child("nombre").getValue();

                        // Convertir a String de forma segura
                        String email = emailObj != null ? String.valueOf(emailObj) : null;
                        String password = passwordObj != null ? String.valueOf(passwordObj) : null;
                        String nombre = nombreObj != null ? String.valueOf(nombreObj) : null;

                        if (email != null && password != null && !email.isEmpty() && !password.isEmpty()) {
                            registrarConductorEnAuth(email, password, nombre);
                        } else {
                            Log.w("RegistroConductores", "Email o contraseña vacíos para conductor: " + conductorSnapshot.getKey());
                        }

                    } catch (Exception e) {
                        Log.e("RegistroConductores", "Error procesando conductor " + conductorSnapshot.getKey() + ": " + e.getMessage());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("RegistroConductores", "Error: " + error.getMessage());
            }
        });
    }

    private void registrarConductorEnAuth(String email, String password, String nombre) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        // Verificar si el usuario ya existe en Auth
        auth.fetchSignInMethodsForEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult().getSignInMethods().isEmpty()) {
                    // Usuario no existe, registrarlo
                    auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(createTask -> {
                                if (createTask.isSuccessful()) {
                                    Log.d("RegistroConductores", "✅ Conductor registrado en Auth: " + email);

                                    // Actualizar el display name
                                    FirebaseUser user = auth.getCurrentUser();
                                    if (user != null) {
                                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                                .setDisplayName(nombre)
                                                .build();
                                        user.updateProfile(profileUpdates);
                                    }
                                } else {
                                    Log.e("RegistroConductores", "❌ Error registrando: " + email + " - " +
                                            createTask.getException().getMessage());
                                }
                            });
                } else {
                    Log.d("RegistroConductores", "✅ Usuario ya existe en Auth: " + email);
                }
            }
        });
    }
}