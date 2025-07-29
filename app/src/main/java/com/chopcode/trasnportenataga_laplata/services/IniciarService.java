package com.chopcode.trasnportenataga_laplata.services;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import com.google.firebase.database.*;

import com.chopcode.trasnportenataga_laplata.R;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import androidx.annotation.NonNull;

public class IniciarService {
    private FirebaseAuth auth;
    private Activity activity;
    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;
    private RegistroService registroService;
    public static final int REQ_ONE_TAP = 123;

    // Interfaz para callbacks de inicio de sesión
    public interface LoginCallback {
        void onLoginSuccess();
        void onLoginFailure(String error);
    }
    public interface TipoUsuarioCallback {
        void onTipoDetectado(String tipo); // tipo = "pasajero" o "conductor"
        void onError(String error);
    }

    /** Constructor que recibe la actividad para poder usar startIntentSenderForResult, etc.*/
    public IniciarService(Activity activity) {
        this.activity = activity;
        auth = FirebaseAuth.getInstance();
        registroService = new RegistroService();
        oneTapClient = Identity.getSignInClient(activity);
        signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                                .setSupported(true)
                                .setServerClientId(activity.getString(R.string.default_web_client_id))
                                .setFilterByAuthorizedAccounts(false)
                                .build())
                .build();
    }
    /** Metodo que se encarga de manejar la logica
     * para identificar el tipo de usuario validando
     * en que nodo se encuentra registrado*/
    public void detectarTipoUsuario(FirebaseUser user, @NonNull TipoUsuarioCallback callback) {
        String uid = user.getUid();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

        // 🔍 Primero busca en el nodo "conductores"
        dbRef.child("conductores").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshotConductor) {
                        if (snapshotConductor.exists()) {
                            // ✅ Si el UID está en "conductores", es conductor
                            callback.onTipoDetectado("conductor");
                        } else {
                            // 🔍 Si no está en "conductores", buscar en "usuarios"
                            dbRef.child("usuarios").child(uid)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshotUsuario) {
                                            if (snapshotUsuario.exists()) {
                                                // ✅ Está en "usuarios", es pasajero
                                                callback.onTipoDetectado("pasajero");
                                            } else {
                                                // ❌ No se encontró en ninguno
                                                callback.onError("No se encontró el usuario en usuarios ni conductores.");
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            callback.onError("Error al verificar en usuarios: " + error.getMessage());
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError("Error al verificar en conductores: " + error.getMessage());
                    }
                });
    }

    /**
     * Inicia sesión usando correo y contraseña.
     */
    public void iniciarSesionCorreo(String correo, String password, @NonNull LoginCallback callback) {
        auth.signInWithEmailAndPassword(correo, password)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        callback.onLoginSuccess();
                    } else {
                        callback.onLoginFailure(task.getException().getMessage());
                    }
                });
    }

    /**
     * Inicia sesión con Google usando One Tap Sign-In.
     */
    public void iniciarSesionGoogle(@NonNull LoginCallback callback) {
        oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(activity, result -> {
                    try {
                        activity.startIntentSenderForResult(
                                result.getPendingIntent().getIntentSender(),
                                REQ_ONE_TAP,
                                null, 0, 0, 0, null);
                    } catch (IntentSender.SendIntentException e) {
                        callback.onLoginFailure(e.getMessage());
                    }
                })
                .addOnFailureListener(activity, e -> {
                    callback.onLoginFailure(e.getMessage());
                });
    }

     /** 🔥 Maneja el inicio de sesión con Google y guarda el usuario en Firebase si no existe.
     */
    public void manejarResultadoGoogle(Intent data, @NonNull LoginCallback callback) {
        try {
            SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(data);
            String idToken = credential.getGoogleIdToken();
            if (idToken != null) {
                AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
                auth.signInWithCredential(firebaseCredential)
                        .addOnCompleteListener(activity, task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = auth.getCurrentUser();
                                if (user != null) {
                                    // 🔎 Detectar si es conductor o pasajero
                                    detectarTipoUsuario(user, new TipoUsuarioCallback() {
                                        @Override
                                        public void onTipoDetectado(String tipo) {
                                            // Ya está registrado como pasajero o conductor, continuar
                                            callback.onLoginSuccess();
                                        }

                                        @Override
                                        public void onError(String error) {
                                            // No existe en ningún nodo, lo registramos como pasajero por defecto
                                            registroService.guardarUsuarioSiNoExiste(user, new RegistroService.RegistroCallback() {
                                                @Override
                                                public void onSuccess() {
                                                    callback.onLoginSuccess();
                                                }

                                                @Override
                                                public void onFailure(String error) {
                                                    callback.onLoginFailure("Autenticado, pero fallo registro: " + error);
                                                }
                                            });
                                        }
                                    });
                                } else {
                                    callback.onLoginFailure("No se pudo obtener el usuario de Firebase.");
                                }
                            } else {
                                callback.onLoginFailure(task.getException().getMessage());
                            }
                        });
            } else {
                callback.onLoginFailure("No se obtuvo token de Google");
            }
        } catch (ApiException e) {
            callback.onLoginFailure(e.getMessage());
        }
    }
}