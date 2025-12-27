package com.chopcode.trasnportenataga_laplata.services.auth;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.util.Log;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.config.MyApp;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;

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

    /** Constructor */
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

    /**
     * ✅ CORREGIDO: Método mejorado para detectar tipo de usuario
     * Verifica si es conductor REAL (con datos completos) o solo tiene token
     */
    public void detectarTipoUsuario(FirebaseUser user, @NonNull TipoUsuarioCallback callback) {
        String uid = user.getUid();
        Log.d(TAG, "🔍 Detectando tipo de usuario para UID: " + uid);

        DatabaseReference dbRef = MyApp.getDatabaseReference("");

        // 🔍 Buscar en AMBOS nodos simultáneamente
        Log.d(TAG, "🔍 Buscando usuario en toda la base de datos...");

        // Buscar en conductores
        dbRef.child("conductores").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshotConductor) {
                        // Buscar en usuarios también
                        dbRef.child("usuarios").child(uid)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshotUsuario) {
                                        // ✅ ANALIZAR AMBOS RESULTADOS
                                        analizarResultados(uid, snapshotConductor, snapshotUsuario, callback);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e(TAG, "❌ Error consultando usuarios: " + error.getMessage());
                                        callback.onError("Error verificando usuarios: " + error.getMessage());
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "❌ Error consultando conductores: " + error.getMessage());
                        callback.onError("Error verificando conductores: " + error.getMessage());
                    }
                });
    }

    /**
     * ✅ NUEVO MÉTODO: Analizar resultados de ambas consultas
     */
    private void analizarResultados(String uid, DataSnapshot snapshotConductor,
                                    DataSnapshot snapshotUsuario, TipoUsuarioCallback callback) {

        Log.d(TAG, "📊 ANALIZANDO RESULTADOS para: " + uid);
        Log.d(TAG, "   - En conductores: " + (snapshotConductor.exists() ? "SÍ" : "NO"));
        Log.d(TAG, "   - En usuarios: " + (snapshotUsuario.exists() ? "SÍ" : "NO"));

        // ✅ CASO 1: Está en USUARIOS y tiene datos completos
        if (snapshotUsuario.exists() && esUsuarioCompleto(snapshotUsuario)) {
            Log.d(TAG, "✅ Usuario encontrado en 'usuarios' con datos COMPLETOS - Tipo: PASAJERO");
            callback.onTipoDetectado("pasajero");
            return;
        }

        // ✅ CASO 2: Está en CONDUCTORES y tiene datos completos
        if (snapshotConductor.exists() && esConductorCompleto(snapshotConductor)) {
            Log.d(TAG, "✅ Usuario encontrado en 'conductores' con datos COMPLETOS - Tipo: CONDUCTOR");
            callback.onTipoDetectado("conductor");
            return;
        }

        // ✅ CASO 3: Está en AMBOS nodos (posible duplicidad)
        if (snapshotConductor.exists() && snapshotUsuario.exists()) {
            Log.w(TAG, "⚠️ Usuario encontrado en AMBOS nodos - Analizando...");

            boolean conductorCompleto = esConductorCompleto(snapshotConductor);
            boolean usuarioCompleto = esUsuarioCompleto(snapshotUsuario);

            if (conductorCompleto && !usuarioCompleto) {
                Log.d(TAG, "   → Conductor con datos completos - Tipo: CONDUCTOR");
                callback.onTipoDetectado("conductor");
            } else if (usuarioCompleto && !conductorCompleto) {
                Log.d(TAG, "   → Usuario con datos completos - Tipo: PASAJERO");
                callback.onTipoDetectado("pasajero");
            } else if (conductorCompleto && usuarioCompleto) {
                // ❌ ERROR: Usuario completo en ambos nodos
                Log.e(TAG, "❌ ERROR: Usuario completo en AMBOS nodos - Priorizando conductor");
                callback.onTipoDetectado("conductor");
            } else {
                // Ninguno tiene datos completos - Usar usuarios como default
                Log.w(TAG, "   → Ninguno tiene datos completos - Default: PASAJERO");
                callback.onTipoDetectado("pasajero");
            }
            return;
        }

        // ✅ CASO 4: Solo en conductores pero datos incompletos (solo token)
        if (snapshotConductor.exists() && !esConductorCompleto(snapshotConductor)) {
            Log.w(TAG, "⚠️ Entrada en 'conductores' pero datos INCOMPLETOS - Tipo: PASAJERO");
            Log.w(TAG, "   - Probablemente solo tokenFCM - Tratando como pasajero");
            callback.onTipoDetectado("pasajero");
            return;
        }

        // ✅ CASO 5: Solo en usuarios pero datos incompletos
        if (snapshotUsuario.exists() && !esUsuarioCompleto(snapshotUsuario)) {
            Log.w(TAG, "⚠️ Entrada en 'usuarios' pero datos INCOMPLETOS - Tipo: PASAJERO");
            callback.onTipoDetectado("pasajero");
            return;
        }

        // ✅ CASO 6: No está en ningún lado
        Log.w(TAG, "⚠️ Usuario no encontrado en ninguna colección con datos válidos");
        callback.onError("Usuario no encontrado en usuarios ni conductores.");
    }

    /**
     * ✅ NUEVO MÉTODO: Verificar si es conductor COMPLETO
     * Un conductor debe tener al menos: nombre, placaVehiculo, modeloVehiculo
     */
    private boolean esConductorCompleto(DataSnapshot snapshot) {
        try {
            // Campos MÍNIMOS requeridos para ser conductor REAL
            boolean tieneNombre = snapshot.hasChild("nombre");
            boolean tienePlaca = snapshot.hasChild("placaVehiculo") || snapshot.hasChild("vehiculoId");
            boolean tieneModelo = snapshot.hasChild("modeloVehiculo");
            boolean tieneCapacidad = snapshot.hasChild("capacidadVehiculo");

            // Verificar que el nombre no sea "No disponible" o similar
            String nombre = snapshot.child("nombre").getValue(String.class);
            boolean nombreValido = nombre != null &&
                    !nombre.isEmpty() &&
                    !nombre.contains("Conductor") && // No "Conductor ABC123"
                    !nombre.equals("No disponible");

            Log.d(TAG, "🔍 Verificando conductor completo:");
            Log.d(TAG, "   - Tiene nombre: " + tieneNombre + " (" + nombre + ")");
            Log.d(TAG, "   - Tiene placa: " + tienePlaca);
            Log.d(TAG, "   - Tiene modelo: " + tieneModelo);
            Log.d(TAG, "   - Tiene capacidad: " + tieneCapacidad);
            Log.d(TAG, "   - Nombre válido: " + nombreValido);

            // Debe tener al menos nombre válido Y placa para ser conductor real
            boolean esConductorReal = tieneNombre && nombreValido && tienePlaca;

            if (esConductorReal) {
                Log.d(TAG, "   ✅ ES CONDUCTOR REAL");
            } else {
                Log.d(TAG, "   ❌ NO ES CONDUCTOR REAL (falta información)");
            }

            return esConductorReal;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error verificando conductor completo: " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ NUEVO MÉTODO: Verificar si es usuario COMPLETO
     */
    private boolean esUsuarioCompleto(DataSnapshot snapshot) {
        try {
            // Campos mínimos para usuario
            boolean tieneNombre = snapshot.hasChild("nombre");
            boolean tieneEmail = snapshot.hasChild("email");
            boolean tieneTelefono = snapshot.hasChild("telefono");

            Log.d(TAG, "🔍 Verificando usuario completo:");
            Log.d(TAG, "   - Tiene nombre: " + tieneNombre);
            Log.d(TAG, "   - Tiene email: " + tieneEmail);
            Log.d(TAG, "   - Tiene teléfono: " + tieneTelefono);

            // Para ser usuario válido necesita al menos nombre
            boolean esUsuarioValido = tieneNombre;

            if (esUsuarioValido) {
                Log.d(TAG, "   ✅ ES USUARIO COMPLETO");
            } else {
                Log.d(TAG, "   ❌ NO ES USUARIO COMPLETO");
            }

            return esUsuarioValido;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error verificando usuario completo: " + e.getMessage());
            return false;
        }
    }

    /**
     * Inicia sesión usando correo y contraseña.
     */
    public void iniciarSesionCorreo(String correo, String password, @NonNull LoginCallback callback) {
        Log.d(TAG, "🔐 Iniciando sesión con email: " + correo);

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
                                    callback.onLoginSuccess(tipo);
                                }

                                @Override
                                public void onError(String error) {
                                    Log.e(TAG, "❌ Error detectando tipo de usuario: " + error);
                                    callback.onLoginFailure("Usuario no encontrado: " + error);
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

                                    // 🔎 Detectar si es conductor o pasajero
                                    detectarTipoUsuario(user, new TipoUsuarioCallback() {
                                        @Override
                                        public void onTipoDetectado(String tipo) {
                                            Log.d(TAG, "✅ Usuario Google ya registrado como: " + tipo);
                                            callback.onLoginSuccess(tipo);
                                        }

                                        @Override
                                        public void onError(String error) {
                                            Log.w(TAG, "⚠️ Usuario Google no encontrado en BD - registrando como pasajero");
                                            Log.w(TAG, "   - Error: " + error);

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
            callback.onLoginFailure(e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "❌ Error inesperado en Google Sign-In: " + e.getMessage(), e);
            callback.onLoginFailure("Error inesperado: " + e.getMessage());
        }
    }
}