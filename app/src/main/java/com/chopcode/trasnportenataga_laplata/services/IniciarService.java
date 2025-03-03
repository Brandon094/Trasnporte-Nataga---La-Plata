package com.chopcode.trasnportenataga_laplata.services;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.util.Log;

import com.chopcode.trasnportenataga_laplata.R;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import androidx.annotation.NonNull;

public class IniciarService {

    private FirebaseAuth auth;
    private Activity activity;
    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;
    public static final int REQ_ONE_TAP = 123;

    // Interfaz para callbacks de inicio de sesión
    public interface LoginCallback {
        void onLoginSuccess();
        void onLoginFailure(String error);
    }

    // Constructor que recibe la actividad para poder usar startIntentSenderForResult, etc.
    public IniciarService(Activity activity) {
        this.activity = activity;
        auth = FirebaseAuth.getInstance();
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

    /**
     * Maneja el resultado del inicio de sesión con Google.
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
                                callback.onLoginSuccess();
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
