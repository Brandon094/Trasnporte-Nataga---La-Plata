package com.chopcode.trasnportenataga_laplata.services.auth;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.util.Log;
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
import com.chopcode.trasnportenataga_laplata.config.MyApp;

public class IniciarService {
    private static final String TAG = "IniciarService";

    private FirebaseAuth auth;
    private Activity activity;
    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;
    private RegistroService registroService;
    public static final int REQ_ONE_TAP = 123;

    // Interfaz para callbacks de inicio de sesión
    public interface LoginCallback {
        void onLoginSuccess(String tipoUsuario);
        void onLoginFailure(String error);
    }
    public interface TipoUsuarioCallback {
        void onTipoDetectado(String tipo); // tipo = "pasajero" o "conductor"
        void onError(String error);
    }

    /** Constructor que recibe la actividad para poder usar startIntentSenderForResult, etc.*/
    public IniciarService(Activity activity) {
        Log.d(TAG, "🚀 Constructor - Inicializando servicio de autenticación");
        this.activity = activity;
        auth = MyApp.getInstance().getFirebaseAuth();
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
        Log.d(TAG, "✅ Servicio de autenticación inicializado correctamente");
    }

    /** Metodo que se encarga de manejar la logica
     * para identificar el tipo de usuario validando
     * en que nodo se encuentra registrado*/
    public void detectarTipoUsuario(FirebaseUser user, @NonNull TipoUsuarioCallback callback) {
        String uid = user.getUid();
        Log.d(TAG, "🔍 Detectando tipo de usuario para UID: " + uid);
        Log.d(TAG, "   - Email: " + user.getEmail());
        Log.d(TAG, "   - Nombre: " + user.getDisplayName());

        DatabaseReference dbRef = MyApp.getDatabaseReference("");

        // 🔍 Primero busca en el nodo "conductores"
        Log.d(TAG, "🔎 Buscando en nodo 'conductores'...");
        dbRef.child("conductores").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshotConductor) {
                        if (snapshotConductor.exists()) {
                            // ✅ Si el UID está en "conductores", es conductor
                            Log.d(TAG, "✅ Usuario encontrado en 'conductores' - Tipo: CONDUCTOR");
                            callback.onTipoDetectado("conductor");
                        } else {
                            Log.d(TAG, "🔍 Usuario no encontrado en 'conductores' - Buscando en 'usuarios'...");
                            // 🔍 Si no está en "conductores", buscar en "usuarios"
                            dbRef.child("usuarios").child(uid)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshotUsuario) {
                                            if (snapshotUsuario.exists()) {
                                                // ✅ Está en "usuarios", es pasajero
                                                Log.d(TAG, "✅ Usuario encontrado en 'usuarios' - Tipo: PASAJERO");
                                                callback.onTipoDetectado("pasajero");
                                            } else {
                                                // ❌ No se encontró en ninguno
                                                Log.w(TAG, "⚠️ Usuario no encontrado en 'usuarios' ni 'conductores'");
                                                Log.w(TAG, "   - UID: " + uid);
                                                Log.w(TAG, "   - Email: " + user.getEmail());
                                                callback.onError("No se encontró el usuario en usuarios ni conductores.");
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Log.e(TAG, "❌ Error en consulta a 'usuarios': " + error.getMessage());
                                            Log.e(TAG, "   - Código: " + error.getCode());
                                            Log.e(TAG, "   - Detalles: " + error.getDetails());
                                            callback.onError("Error al verificar en usuarios: " + error.getMessage());
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "❌ Error en consulta a 'conductores': " + error.getMessage());
                        Log.e(TAG, "   - Código: " + error.getCode());
                        Log.e(TAG, "   - Detalles: " + error.getDetails());
                        callback.onError("Error al verificar en conductores: " + error.getMessage());
                    }
                });
    }

    /**
     * Inicia sesión usando correo y contraseña.
     */
    public void iniciarSesionCorreo(String correo, String password, @NonNull LoginCallback callback) {
        Log.d(TAG, "🔐 Iniciando sesión con email: " + correo);
        Log.d(TAG, "   - Longitud contraseña: " + password.length());

        auth.signInWithEmailAndPassword(correo, password)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "✅ Autenticación con email exitosa");
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "👤 Usuario Firebase obtenido: " + user.getUid());
                            // 🔎 Detectar tipo de usuario después del login exitoso
                            detectarTipoUsuario(user, new TipoUsuarioCallback() {
                                @Override
                                public void onTipoDetectado(String tipo) {
                                    Log.d(TAG, "🎯 Tipo de usuario detectado: " + tipo);
                                    callback.onLoginSuccess(tipo); // Éxito, el callback manejará la
                                    // redirección
                                }

                                @Override
                                public void onError(String error) {
                                    Log.e(TAG, "❌ Error detectando tipo de usuario: " + error);
                                    callback.onLoginFailure("Usuario no encontrado en conductores ni usuarios: " + error);
                                }
                            });
                        } else {
                            Log.e(TAG, "❌ Usuario Firebase es null después de login exitoso");
                            callback.onLoginFailure("No se pudo obtener el usuario después del login");
                        }
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Error desconocido";
                        Log.e(TAG, "❌ Error en autenticación con email: " + errorMsg);
                        callback.onLoginFailure(errorMsg);
                    }
                });
    }

    /**
     * Inicia sesión con Google usando One Tap Sign-In.
     */
    public void iniciarSesionGoogle(@NonNull LoginCallback callback) {
        Log.d(TAG, "🔐 Iniciando flujo de Google Sign-In");

        oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(activity, result -> {
                    Log.d(TAG, "✅ Google Sign-In request exitoso - iniciando intent sender");
                    try {
                        activity.startIntentSenderForResult(
                                result.getPendingIntent().getIntentSender(),
                                REQ_ONE_TAP,
                                null, 0, 0, 0, null);
                        Log.d(TAG, "✅ Intent sender iniciado - REQ_ONE_TAP: " + REQ_ONE_TAP);
                    } catch (IntentSender.SendIntentException e) {
                        Log.e(TAG, "❌ Error en IntentSender: " + e.getMessage(), e);
                        callback.onLoginFailure(e.getMessage());
                    }
                })
                .addOnFailureListener(activity, e -> {
                    Log.e(TAG, "❌ Error en Google Sign-In request: " + e.getMessage(), e);
                    callback.onLoginFailure(e.getMessage());
                });
    }

    /** 🔥 Maneja el inicio de sesión con Google y guarda el usuario en Firebase si no existe.
     */
    public void manejarResultadoGoogle(Intent data, @NonNull LoginCallback callback) {
        Log.d(TAG, "🔄 Procesando resultado de Google Sign-In");

        try {
            SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(data);
            String idToken = credential.getGoogleIdToken();

            Log.d(TAG, "📋 Credencial Google obtenida:");
            Log.d(TAG, "   - ID: " + credential.getId());
            Log.d(TAG, "   - Email: " + credential.getId());
            Log.d(TAG, "   - Display Name: " + credential.getDisplayName());

            if (idToken != null) {
                Log.d(TAG, "✅ Token Google obtenido - autenticando con Firebase");
                AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
                auth.signInWithCredential(firebaseCredential)
                        .addOnCompleteListener(activity, task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "✅ Autenticación Firebase con Google exitosa");
                                FirebaseUser user = auth.getCurrentUser();
                                if (user != null) {
                                    Log.d(TAG, "👤 Usuario Google autenticado: " + user.getUid());
                                    Log.d(TAG, "   - Email: " + user.getEmail());
                                    Log.d(TAG, "   - Nombre: " + user.getDisplayName());

                                    // 🔎 Detectar si es conductor o pasajero
                                    detectarTipoUsuario(user, new TipoUsuarioCallback() {
                                        @Override
                                        public void onTipoDetectado(String tipo) {
                                            Log.d(TAG, "✅ Usuario Google ya registrado como: " + tipo);
                                            // Ya está registrado como pasajero o conductor, continuar
                                            callback.onLoginSuccess(tipo);
                                        }

                                        @Override
                                        public void onError(String error) {
                                            Log.w(TAG, "⚠️ Usuario Google no encontrado en BD - registrando como pasajero");
                                            Log.w(TAG, "   - Error: " + error);
                                            Log.w(TAG, "   - UID: " + user.getUid());

                                            // No existe en ningún nodo, lo registramos como pasajero por defecto
                                            registroService.guardarUsuarioSiNoExiste(user, new RegistroService.RegistroCallback() {
                                                @Override
                                                public void onSuccess() {
                                                    Log.d(TAG, "✅ Usuario Google registrado exitosamente como pasajero");
                                                    callback.onLoginSuccess("pasajero");
                                                }

                                                @Override
                                                public void onFailure(String error) {
                                                    Log.e(TAG, "❌ Error registrando usuario Google: " + error);
                                                    callback.onLoginFailure("Autenticado, pero fallo registro: " + error);
                                                }
                                            });
                                        }
                                    });
                                } else {
                                    Log.e(TAG, "❌ Usuario Firebase es null después de Google Sign-In");
                                    callback.onLoginFailure("No se pudo obtener el usuario de Firebase.");
                                }
                            } else {
                                String errorMsg = task.getException() != null ? task.getException().getMessage() : "Error desconocido";
                                Log.e(TAG, "❌ Error en autenticación Firebase con Google: " + errorMsg);
                                callback.onLoginFailure(errorMsg);
                            }
                        });
            } else {
                Log.e(TAG, "❌ Token Google es null");
                callback.onLoginFailure("No se obtuvo token de Google");
            }
        } catch (ApiException e) {
            Log.e(TAG, "❌ ApiException en Google Sign-In: " + e.getMessage(), e);
            Log.e(TAG, "   - Status Code: " + e.getStatusCode());
            Log.e(TAG, "   - Status Message: " + e.getStatusMessage());
            callback.onLoginFailure(e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "❌ Error inesperado en Google Sign-In: " + e.getMessage(), e);
            callback.onLoginFailure("Error inesperado: " + e.getMessage());
        }
    }
}