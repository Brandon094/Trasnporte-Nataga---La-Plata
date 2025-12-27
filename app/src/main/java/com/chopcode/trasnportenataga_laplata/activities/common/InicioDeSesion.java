package com.chopcode.trasnportenataga_laplata.activities.common;

import static com.chopcode.trasnportenataga_laplata.managers.PermissionManager.requestNotificationPermission;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.activities.driver.InicioConductor;
import com.chopcode.trasnportenataga_laplata.activities.passenger.CrearReservas;
import com.chopcode.trasnportenataga_laplata.activities.passenger.InicioUsuarios;
import com.chopcode.trasnportenataga_laplata.config.MyApp;
import com.chopcode.trasnportenataga_laplata.managers.NotificationManager;
import com.chopcode.trasnportenataga_laplata.managers.PermissionManager;
import com.chopcode.trasnportenataga_laplata.services.auth.IniciarService;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class InicioDeSesion extends AppCompatActivity {

    private EditText editTextUsuario, editTextPassword;
    private Button buttonIngresar;
    private Button btnGoogleSignIn;
    private IniciarService iniciarService;
    private TextView buttonRegistro, olvidasteContraseña;

    // ✅ Constantes para SharedPreferences
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_TYPE = "user_type";
    private DatabaseReference rtdb;

    // ✅ NUEVO: Tag para logs
    private static final String TAG = "InicioDeSesion";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio_de_sesion);

        Log.d(TAG, "🚀 onCreate - Iniciando actividad de login");

        // Solicitar permiso de notificaciones
        requestNotificationPermission(this);

        // Inicializar Firebase - SOLO Realtime Database
        rtdb = MyApp.getDatabaseReference(""); // Referencia raiz a la base de datos

        // Inicializar IniciarService, pasando la actividad actual
        iniciarService = new IniciarService(this);
        Log.d(TAG, "✅ IniciarService inicializado");

        // Referenciar elementos de UI
        initViews();
        Log.d(TAG, "✅ Vistas inicializadas");

        // Manejar inicio de sesión con correo y contraseña
        setupEmailLogin();

        // Manejar inicio de sesión con Google
        setupGoogleLogin();

        // Manejar botón de registro
        setupRegistroButton();

        // Verificar si ya hay un usuario logueado
        verificarSesionExistente();

        Log.d(TAG, "✅ Configuración completa - Actividad lista");
    }

    /**
     * Inicializa todas las vistas del layout
     */
    private void initViews() {
        Log.d(TAG, "🔧 Inicializando vistas...");

        editTextUsuario = findViewById(R.id.editTextUsuario);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonIngresar = findViewById(R.id.buttonIngresar);
        buttonRegistro = findViewById(R.id.buttonRegistro);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        olvidasteContraseña = findViewById(R.id.olvidasteContraseña);

        TextInputLayout passwordInputLayout = findViewById(R.id.passwordInputLayout);
        TextInputEditText editTextPassword = findViewById(R.id.editTextPassword);

        // Establecer el icono inicial (contraseña oculta)
        passwordInputLayout.setEndIconDrawable(R.drawable.ic_visibility_off);

        // Manejar clic en el icono de visibilidad
        passwordInputLayout.setEndIconOnClickListener(v -> {
            if (editTextPassword.getTransformationMethod() instanceof PasswordTransformationMethod) {
                // Si está oculta, mostrarla
                editTextPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                passwordInputLayout.setEndIconDrawable(R.drawable.ic_visibility_on);
                Log.d(TAG, "👁️ Contraseña visible");
            } else {
                // Si está visible, ocultarla
                editTextPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                passwordInputLayout.setEndIconDrawable(R.drawable.ic_visibility_off);
                Log.d(TAG, "👁️ Contraseña oculta");
            }
            // Mover cursor al final
            editTextPassword.setSelection(editTextPassword.getText().length());
        });

        // ✅ NUEVO: Configurar "Olvidaste contraseña"
        if (olvidasteContraseña != null) {
            olvidasteContraseña.setOnClickListener(v -> {
                Log.d(TAG, "🔑 Usuario solicitó recuperar contraseña");
                Toast.makeText(InicioDeSesion.this, "Función en desarrollo", Toast.LENGTH_SHORT).show();
                // Aquí puedes implementar la recuperación de contraseña
            });
        }

        Log.d(TAG, "✅ Vistas referenciadas correctamente");
    }

    /**
     * Verificar si ya existe una sesión activa
     */
    private void verificarSesionExistente() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedUserId = prefs.getString(KEY_USER_ID, null);
        String savedUserType = prefs.getString(KEY_USER_TYPE, null);

        if (savedUserId != null && savedUserType != null) {
            Log.d(TAG, "📱 Sesión existente encontrada - UserId: " + savedUserId + ", Tipo: " + savedUserType);

            // Verificar con Firebase Auth también
            FirebaseUser currentUser = MyApp.getCurrentUser();
            if (currentUser != null && currentUser.getUid().equals(savedUserId)) {
                Log.d(TAG, "✅ Sesión Firebase válida, redirigiendo automáticamente...");

                // ✅ VERIFICAR PERMISOS ANTES DE REDIRIGIR
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    boolean tienePermiso = PermissionManager.isNotificationPermissionGranted(this);
                    if (!tienePermiso) {
                        Log.d(TAG, "🔔 Usuario no tiene permiso de notificaciones, solicitando...");
                        requestNotificationPermission(this);
                    }
                }

                redirigirSegunTipoUsuario(savedUserType);
            } else {
                Log.d(TAG, "⚠️ Sesión en SharedPreferences pero no en Firebase, limpiando...");
                limpiarSesionGuardada();
            }
        }
    }

    /**
     * ✅ NUEVO: Limpiar sesión guardada
     */
    private void limpiarSesionGuardada() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_USER_ID)
                .remove(KEY_USER_TYPE)
                .apply();
        Log.d(TAG, "🧹 Sesión guardada limpiada");
    }

    /**
     * Configura el login con email y contraseña
     */
    private void setupEmailLogin() {
        Log.d(TAG, "🔧 Configurando login con email...");

        buttonIngresar.setOnClickListener(v -> {
            String correo = editTextUsuario.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            Log.d(TAG, "📧 Intentando login con email: " + correo);
            Log.d(TAG, "🔐 Longitud de contraseña: " + password.length());

            if (correo.isEmpty() || password.isEmpty()) {
                Log.w(TAG, "⚠️ Campos vacíos - mostrando toast");
                Toast.makeText(InicioDeSesion.this, "Ingresa correo y contraseña", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ MEJORADO: Deshabilitar botón durante el login
            buttonIngresar.setEnabled(false);
            buttonIngresar.setText("Iniciando sesión...");

            Log.d(TAG, "🔄 Llamando a iniciarSesionCorreo...");
            iniciarService.iniciarSesionCorreo(correo, password, new IniciarService.LoginCallback() {
                @Override
                public void onLoginSuccess(String tipoUsuario) { // ✅ tipoUsuario YA VIENE DEL SERVICIO
                    Log.d(TAG, "✅ Login exitoso con email. Tipo recibido: " + tipoUsuario);

                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        Log.d(TAG, "👤 Usuario Firebase obtenido: " + user.getUid());

                        // ✅ CORREGIDO: Usar el tipoUsuario que YA VIENE del servicio
                        guardarUsuarioEnPrefs(user.getUid(), tipoUsuario);

                        if (tipoUsuario.equals("conductor")) {
                            Log.d(TAG, "🚗 Redirigiendo a InicioConductor");
                            irAInicioConductor();
                        } else {
                            Log.d(TAG, "👤 Redirigiendo a InicioUsuarios");
                            irAInicioUsuarios();
                        }
                    } else {
                        Log.e(TAG, "❌ Usuario Firebase es null después de login exitoso");
                        buttonIngresar.setEnabled(true);
                        buttonIngresar.setText("Ingresar");
                    }
                }

                @Override
                public void onLoginFailure(String error) {
                    Log.e(TAG, "❌ Error en login con email: " + error);
                    // ✅ REHABILITAR BOTÓN EN CASO DE ERROR
                    buttonIngresar.setEnabled(true);
                    buttonIngresar.setText("Ingresar");
                    Toast.makeText(InicioDeSesion.this, "Error: " + error, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    /**
     * Configura el login con Google
     */
    private void setupGoogleLogin() {
        Log.d(TAG, "🔧 Configurando login con Google...");

        btnGoogleSignIn.setOnClickListener(v -> {
            Log.d(TAG, "🔄 Iniciando login con Google...");
            // ✅ DESHABILITAR BOTÓN DURANTE LOGIN
            btnGoogleSignIn.setEnabled(false);

            iniciarService.iniciarSesionGoogle(new IniciarService.LoginCallback() {
                @Override
                public void onLoginSuccess(String tipoUsuario) { // ✅ tipoUsuario YA VIENE DEL SERVICIO
                    Log.d(TAG, "✅ Login con Google exitoso. Tipo recibido: " + tipoUsuario);

                    FirebaseUser user = MyApp.getCurrentUser();
                    if (user != null) {
                        // ✅ CORREGIDO: Usar el tipoUsuario que YA VIENE del servicio
                        guardarUsuarioEnPrefs(user.getUid(), tipoUsuario);

                        if (tipoUsuario.equals("conductor")) {
                            Log.d(TAG, "🚗 Redirigiendo a InicioConductor (Google)");
                            irAInicioConductor();
                        } else {
                            Log.d(TAG, "👤 Redirigiendo a InicioUsuarios (Google)");
                            irAInicioUsuarios();
                        }
                    }
                }

                @Override
                public void onLoginFailure(String error) {
                    Log.e(TAG, "❌ Error en login con Google: " + error);
                    btnGoogleSignIn.setEnabled(true);
                    Toast.makeText(InicioDeSesion.this, "Error: " + error, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    /**
     * Configura el botón de registro
     */
    private void setupRegistroButton() {
        Log.d(TAG, "🔧 Configurando botón de registro...");

        if (buttonRegistro != null) {
            buttonRegistro.setOnClickListener(v -> {
                Log.d(TAG, "📝 Navegando a RegistroUsuarios");
                Intent intent = new Intent(InicioDeSesion.this, RegistroUsuarios.class);
                startActivity(intent);
            });
        } else {
            Log.w(TAG, "⚠️ buttonRegistro es null");
        }
    }

    /**
     *  Recibir el resultado del One Tap Sign-In de Google
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "🔄 onActivityResult - requestCode: " + requestCode + ", resultCode: " + resultCode);

        if (requestCode == IniciarService.REQ_ONE_TAP) {
            Log.d(TAG, "🔍 Procesando resultado de Google Sign-In...");
            iniciarService.manejarResultadoGoogle(data, new IniciarService.LoginCallback() {
                @Override
                public void onLoginSuccess(String tipoUsuario) { // ✅ tipoUsuario YA VIENE DEL SERVICIO
                    Log.d(TAG, "✅ Google Sign-In exitoso desde onActivityResult. Tipo recibido: " + tipoUsuario);

                    FirebaseUser user = MyApp.getCurrentUser();
                    if (user != null) {
                        // ✅ CORREGIDO: Usar el tipoUsuario que YA VIENE del servicio
                        guardarUsuarioEnPrefs(user.getUid(), tipoUsuario);

                        if (tipoUsuario.equals("conductor")) {
                            Log.d(TAG, "🚗 Redirigiendo a InicioConductor (ActivityResult)");
                            irAInicioConductor();
                        } else {
                            Log.d(TAG, "👤 Redirigiendo a InicioUsuarios (ActivityResult)");
                            irAInicioUsuarios();
                        }
                    }
                }

                @Override
                public void onLoginFailure(String error) {
                    Log.e(TAG, "❌ Error en Google Sign-In (ActivityResult): " + error);
                    btnGoogleSignIn.setEnabled(true);
                    Toast.makeText(InicioDeSesion.this, "Error: " + error, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Log.d(TAG, "ℹ️ requestCode no manejado: " + requestCode);
        }
    }

    /**
     * ✅ MÉTODO MEJORADO: Guardar userId y tipo de usuario en SharedPreferences
     */
    private void guardarUsuarioEnPrefs(String userId, String tipoUsuario) {
        try {
            Log.d(TAG, "💾 Guardando usuario en SharedPreferences - ID: " + userId + ", Tipo: " + tipoUsuario);

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_USER_ID, userId);
            editor.putString(KEY_USER_TYPE, tipoUsuario); // Guardar el tipo de usuario
            boolean saved = editor.commit();

            if (saved) {
                Log.d(TAG, "✅ Usuario guardado exitosamente: " + userId + " (" + tipoUsuario + ")");

                // Guardar el token FCM en el nodo correcto usando NotificationManager
                guardarTokenFCMEnRealtimeDatabase(userId, tipoUsuario);
            } else {
                Log.e(TAG, "❌ Error: No se pudo guardar usuario en SharedPreferences");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error guardando usuario en SharedPreferences: " + e.getMessage());
        }
    }

    /**
     * ✅ CORREGIDO: Guardar token FCM en Realtime Database usando NotificationManager
     */
    private void guardarTokenFCMEnRealtimeDatabase(String userId, String tipoUsuario) {
        Log.d(TAG, "🔑 guardarTokenFCMEnRealtimeDatabase - Usuario: " + userId + ", Tipo: " + tipoUsuario);

        MyApp.getInstance().getFirebaseMessaging().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();
                        Log.d(TAG, "✅ Token FCM obtenido: " + (token != null ? token.substring(0, 20) + "..." : "null"));

                        // ✅ AGREGADO: Guardar token localmente como backup
                        guardarTokenLocalmente(token);

                        // ✅ USAR NOTIFICATION MANAGER PARA GUARDAR EN REALTIME DATABASE
                        NotificationManager notificationManager = NotificationManager.getInstance(this);
                        notificationManager.saveFCMTokenToRealtimeDatabase(userId, tipoUsuario);

                        // ✅ TAMBIÉN GUARDAR DIRECTAMENTE POR COMPATIBILIDAD
                        guardarTokenDirectamenteEnRTDB(userId, tipoUsuario, token);

                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Error desconocido";
                        Log.e(TAG, "❌ Error obteniendo token FCM: " + errorMsg);
                        if (task.getException() != null) {
                            MyApp.logError(task.getException());
                        }
                    }
                });
    }

    /**
     * ✅ CORREGIDO: Guardar token directamente en RTDB solo en el nodo correcto
     */
    private void guardarTokenDirectamenteEnRTDB(String userId, String tipoUsuario, String token) {
        try {
            if (userId == null || userId.isEmpty() || token == null || token.isEmpty()) {
                Log.e(TAG, "❌ Datos inválidos para guardar token - UserId: " + userId + ", Token: " + (token != null ? token.substring(0, 10) + "..." : "null"));
                return;
            }

            Log.d(TAG, "💾 Guardando token FCM para usuario: " + userId + ", Tipo: " + tipoUsuario);

            // ✅ Determinar el nodo CORRECTO según el tipo de usuario
            String nodoCorrecto;
            if (tipoUsuario.equals("conductor")) {
                nodoCorrecto = "conductores";
                Log.d(TAG, "👨‍✈️ Usuario es CONDUCTOR - Guardando en nodo 'conductores'");
            } else if (tipoUsuario.equals("pasajero") || tipoUsuario.equals("usuario")) {
                nodoCorrecto = "usuarios";
                Log.d(TAG, "👤 Usuario es PASAJERO - Guardando en nodo 'usuarios'");
            } else {
                Log.e(TAG, "❌ Tipo de usuario desconocido: " + tipoUsuario);
                Log.e(TAG, "⚠️ Por defecto, guardando en 'usuarios'");
                nodoCorrecto = "usuarios";
            }

            // ✅ PASO 1: Guardar SOLO en el nodo correcto
            rtdb.child(nodoCorrecto).child(userId).child("tokenFCM")
                    .setValue(token)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅ Token FCM guardado en '" + nodoCorrecto + "/" + userId + "/tokenFCM'");

                        // ✅ PASO 2: Verificar si el usuario existe en el otro nodo y eliminar token si es necesario
                        String otroNodo = nodoCorrecto.equals("conductores") ? "usuarios" : "conductores";

                        rtdb.child(otroNodo).child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    // ❌ El usuario existe en el nodo incorrecto - Eliminar su token de allí
                                    Log.w(TAG, "⚠️ Usuario " + userId + " también existe en '" + otroNodo + "' - Limpiando token incorrecto");

                                    // Verificar si tiene token en el nodo incorrecto
                                    if (dataSnapshot.child("tokenFCM").exists()) {
                                        rtdb.child(otroNodo).child(userId).child("tokenFCM").removeValue()
                                                .addOnSuccessListener(aVoid2 -> {
                                                    Log.d(TAG, "✅ Token eliminado del nodo incorrecto '" + otroNodo + "'");
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "❌ Error eliminando token del nodo incorrecto: " + e.getMessage());
                                                });
                                    }
                                } else {
                                    Log.d(TAG, "✅ Usuario NO existe en el nodo incorrecto '" + otroNodo + "' (esto es correcto)");
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Log.e(TAG, "❌ Error verificando nodo incorrecto: " + databaseError.getMessage());
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Error guardando token en RTDB: " + e.getMessage());
                        MyApp.logError(e);
                    });

        } catch (Exception e) {
            Log.e(TAG, "❌ Error crítico en guardarTokenDirectamenteEnRTDB: " + e.getMessage());
            MyApp.logError(e);
        }
    }

    /**
     * ✅ NUEVO: Guardar token localmente como backup
     */
    private void guardarTokenLocalmente(String token) {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString("fcm_token_local", token).apply();
            Log.d(TAG, "💾 Token guardado localmente como backup");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error guardando token localmente: " + e.getMessage());
        }
    }

    /**
     * ✅ MANEJAR RESULTADO DE SOLICITUD DE PERMISOS
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionManager.NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Log.d(TAG, "✅ Permiso de notificaciones CONCEDIDO por el usuario");
                Toast.makeText(this, "Notificaciones habilitadas", Toast.LENGTH_SHORT).show();

                // ✅ Opcional: Registrar token FCM inmediatamente
                registrarTokenFCMDespuesDePermiso();

            } else {
                Log.w(TAG, "❌ Permiso de notificaciones DENEGADO por el usuario");

                // Mostrar mensaje informativo
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Toast.makeText(this,
                            "Las notificaciones están desactivadas. Puedes activarlas en Configuración > Aplicaciones",
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * ✅ Registrar token FCM después de que se concede el permiso
     */
    private void registrarTokenFCMDespuesDePermiso() {
        FirebaseUser currentUser = MyApp.getCurrentUser();
        if (currentUser != null) {
            // Verificar el tipo de usuario actual
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String userType = prefs.getString(KEY_USER_TYPE, null);

            if (userType != null) {
                guardarTokenFCMEnRealtimeDatabase(currentUser.getUid(), userType);
            }
        }
    }

    /**
     * ✅ NUEVO: Redirigir según tipo de usuario (para sesión existente)
     */
    private void redirigirSegunTipoUsuario(String tipoUsuario) {
        if (tipoUsuario.equals("conductor")) {
            Log.d(TAG, "🚗 Redirigiendo a InicioConductor (sesión existente)");
            irAInicioConductor();
        } else {
            Log.d(TAG, "👤 Redirigiendo a InicioUsuarios (sesión existente)");
            irAInicioUsuarios();
        }
    }

    /**
     * Redirige a la actividad principal de usuarios o a reservas tras iniciar sesión.
     */
    private void irAInicioUsuarios() {
        Log.d(TAG, "🎯 Ejecutando irAInicioUsuarios");

        // Verificar si el usuario intentó hacer una reserva antes de iniciar sesión
        boolean volverAReserva = getIntent().getBooleanExtra("volverAReserva", false);
        Log.d(TAG, "📋 volverAReserva: " + volverAReserva);

        if (volverAReserva) {
            // Si vino de intentar reservar, llevarlo directamente a reservas
            Log.d(TAG, "🎫 Redirigiendo a CrearReservas (volver a reserva)");
            Intent intent = new Intent(InicioDeSesion.this, CrearReservas.class);
            startActivity(intent);
        } else {
            // Caso normal: ir a la pantalla principal
            Log.d(TAG, "🏠 Redirigiendo a InicioUsuarios (caso normal)");
            Intent intent = new Intent(InicioDeSesion.this, InicioUsuarios.class);
            startActivity(intent);
        }
        Log.d(TAG, "🔚 Finalizando actividad de login");
        finish();
    }

    private void irAInicioConductor() {
        Log.d(TAG, "🎯 Ejecutando irAInicioConductor");
        Log.d(TAG, "🚗 Redirigiendo a InicioConductor");

        Intent intent = new Intent(InicioDeSesion.this, InicioConductor.class);
        startActivity(intent);
        Log.d(TAG, "🔚 Finalizando actividad de login (conductor)");
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "📱 onStart - Actividad visible");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "📱 onResume - Actividad en primer plano");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "📱 onPause - Actividad en segundo plano");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "📱 onStop - Actividad no visible");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "📱 onDestroy - Actividad destruida");
    }
}