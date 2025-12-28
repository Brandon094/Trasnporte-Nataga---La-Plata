package com.chopcode.rutago_app.activities.common;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.chopcode.rutago_app.R;
import com.chopcode.rutago_app.config.MyApp; // ✅ NUEVO IMPORT
import com.chopcode.rutago_app.managers.NotificationManager;
import com.chopcode.rutago_app.services.auth.RegistroService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference; // ✅ NUEVO IMPORT

import java.util.HashMap; // ✅ NUEVO IMPORT
import java.util.Map; // ✅ NUEVO IMPORT

public class RegistroUsuarios extends AppCompatActivity {

    // ✅ TAG para logs
    private static final String TAG = "RegistroUsuarios";

    private TextInputEditText editTextNombre, editTextCorreo, editTextTelefono, editTextPassword, editTextConfirmPassword;
    private Button buttonRegistrar;
    private TextView buttonIniciarSesion;
    private MaterialToolbar topAppBar;
    private RegistroService registroService;

    // ✅ NUEVO: NotificationManager
    private NotificationManager notificationManager;

    // ✅ Constantes para SharedPreferences
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_ID = "user_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro_usuarios);

        Log.d(TAG, "✅ onCreate: Iniciando actividad de registro de usuarios");

        // ✅ VERIFICAR SI MyApp ESTÁ INICIALIZADO
        if (MyApp.getInstance() == null) {
            Log.e(TAG, "❌ MyApp no está inicializado. Verifica el AndroidManifest.xml");
        } else {
            Log.d(TAG, "✅ MyApp inicializado correctamente");
        }

        // Inicializar vistas del layout
        initViews();

        // Configurar la toolbar
        setupToolbar();

        // Inicializar servicio de registro
        registroService = new RegistroService();

        // ✅ NUEVO: Inicializar NotificationManager usando MyApp
        notificationManager = NotificationManager.getInstance(MyApp.getAppContext());

        Log.d(TAG, "✅ Servicio de registro y NotificationManager inicializados");

        // Redirigir al usuario a la pantalla de inicio de sesión
        buttonIniciarSesion.setOnClickListener(v -> {
            Log.d(TAG, "📱 Clic en 'Iniciar Sesión', redirigiendo a pantalla de login");

            // ✅ REGISTRAR EVENTO CON MyApp
            Map<String, Object> params = new HashMap<>();
            params.put("screen", "RegistroUsuarios");
            params.put("action", "click_iniciar_sesion");
            MyApp.logEvent("navigation_event", params);

            startActivity(new Intent(RegistroUsuarios.this, InicioDeSesion.class));
            finish(); // Cierra la pantalla de registro para que no vuelva atrás
        });

        // Manejar el clic del botón de registro
        buttonRegistrar.setOnClickListener(v -> {
            Log.d(TAG, "📱 Clic en botón Registrar");

            // ✅ REGISTRAR EVENTO CON MyApp
            Map<String, Object> params = new HashMap<>();
            params.put("screen", "RegistroUsuarios");
            params.put("action", "click_registrar");
            MyApp.logEvent("button_click", params);

            registrarUsuario();
        });

        Log.d(TAG, "✅ Actividad de registro configurada correctamente");
    }

    /**
     * Inicializa todas las vistas del layout
     */
    private void initViews() {
        Log.d(TAG, "🔧 Inicializando vistas del layout");

        editTextNombre = findViewById(R.id.editTextNombre);
        editTextCorreo = findViewById(R.id.editTextCorreo);
        editTextTelefono = findViewById(R.id.editTextTelefono);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonRegistrar = findViewById(R.id.buttonRegistrar);
        buttonIniciarSesion = findViewById(R.id.buttonIniciarSesion);
        topAppBar = findViewById(R.id.topAppBar);

        Log.d(TAG, "✅ Vistas inicializadas correctamente");
    }

    /**
     * Configura la toolbar con navegación
     */
    private void setupToolbar() {
        Log.d(TAG, "🔧 Configurando toolbar");
        topAppBar.setNavigationOnClickListener(v -> {
            Log.d(TAG, "📱 Clic en navegación de toolbar, regresando a actividad anterior");

            // ✅ REGISTRAR EVENTO CON MyApp
            Map<String, Object> params = new HashMap<>();
            params.put("screen", "RegistroUsuarios");
            params.put("action", "toolbar_back");
            MyApp.logEvent("navigation_event", params);

            onBackPressed();
        });
    }

    /**
     * Maneja la validación y registro del usuario.
     */
    private void registrarUsuario() {
        String nombre = editTextNombre.getText().toString().trim();
        String correo = editTextCorreo.getText().toString().trim();
        String telefono = editTextTelefono.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        Log.d(TAG, "👤 Iniciando proceso de registro para: " + correo);
        Log.d(TAG, "📝 Datos capturados - Nombre: " + nombre + ", Teléfono: " + telefono);

        // ✅ REGISTRAR EVENTO DE INICIO DE REGISTRO
        Map<String, Object> registroParams = new HashMap<>();
        registroParams.put("email", correo);
        registroParams.put("has_phone", !telefono.isEmpty());
        MyApp.logEvent("registro_iniciado", registroParams);

        // Validaciones mejoradas
        if (!validarCampos(nombre, correo, password, confirmPassword)) {
            Log.w(TAG, "❌ Validación de campos fallida");

            // ✅ REGISTRAR ERROR DE VALIDACIÓN
            Map<String, Object> errorParams = new HashMap<>();
            errorParams.put("error_type", "validacion_campos");
            MyApp.logEvent("registro_error", errorParams);

            return;
        }

        Log.d(TAG, "✅ Validación de campos exitosa");

        // Mostrar loading state en el botón
        buttonRegistrar.setEnabled(false);
        buttonRegistrar.setText("Registrando...");
        Log.d(TAG, "⏳ Deshabilitando botón de registro - proceso en curso");

        // Registro del usuario
        registroService.registrarUsuario(nombre, correo, telefono, password, new RegistroService.RegistroCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "🎉 Registro exitoso en Firebase Auth");

                // ✅ REGISTRAR ÉXITO DE REGISTRO
                Map<String, Object> successParams = new HashMap<>();
                successParams.put("email", correo);
                successParams.put("timestamp", System.currentTimeMillis());
                MyApp.logEvent("registro_exitoso", successParams);

                runOnUiThread(() -> {
                    buttonRegistrar.setEnabled(true);
                    buttonRegistrar.setText("Registrarse");
                    Log.d(TAG, "✅ Botón de registro reestablecido");

                    // ✅ Obtener y guardar el userId después del registro exitoso
                    FirebaseUser user = MyApp.getCurrentUser(); // ✅ USANDO MyApp
                    if (user != null) {
                        Log.d(TAG, "👤 Usuario de Firebase obtenido: " + user.getUid());
                        guardarUserIdEnPrefs(user.getUid());

                        // ✅ NUEVO: Guardar token FCM después del registro exitoso
                        guardarTokenFCM(user.getUid());

                        // ✅ GUARDAR DATOS ADICIONALES EN REALTIME DATABASE USANDO MyApp
                        guardarDatosUsuarioEnDatabase(user.getUid(), nombre, correo, telefono);
                    } else {
                        Log.e(TAG, "❌ Usuario de Firebase es null después del registro exitoso");

                        // ✅ REGISTRAR ERROR EN CRASHLYTICS
                        MyApp.logError(new Exception("Usuario null después de registro exitoso"));
                    }

                    Toast.makeText(RegistroUsuarios.this, "Usuario registrado exitosamente", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "🚀 Redirigiendo a pantalla de inicio de sesión");

                    // ✅ REGISTRAR NAVEGACIÓN
                    Map<String, Object> navParams = new HashMap<>();
                    navParams.put("from", "RegistroUsuarios");
                    navParams.put("to", "InicioDeSesion");
                    MyApp.logEvent("screen_transition", navParams);

                    startActivity(new Intent(RegistroUsuarios.this, InicioDeSesion.class));
                    finish();
                });
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "❌ Error en registro: " + error);

                // ✅ REGISTRAR ERROR EN CRASHLYTICS
                MyApp.logError(new Exception("Error en registro: " + error));

                // ✅ REGISTRAR EVENTO DE ERROR
                Map<String, Object> errorParams = new HashMap<>();
                errorParams.put("error_message", error);
                errorParams.put("email", correo);
                MyApp.logEvent("registro_fallido", errorParams);

                runOnUiThread(() -> {
                    buttonRegistrar.setEnabled(true);
                    buttonRegistrar.setText("Registrarse");
                    Log.d(TAG, "✅ Botón de registro reestablecido después del error");

                    Toast.makeText(RegistroUsuarios.this, "Error: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * ✅ NUEVO MÉTODO: Guardar datos del usuario en Realtime Database usando MyApp
     */
    private void guardarDatosUsuarioEnDatabase(String userId, String nombre, String correo, String telefono) {
        try {
            // ✅ USAR MyApp PARA OBTENER LA REFERENCIA A LA BASE DE DATOS
            DatabaseReference userRef = MyApp.getDatabaseReference("usuarios/" + userId);

            Map<String, Object> userData = new HashMap<>();
            userData.put("nombre", nombre);
            userData.put("correo", correo);
            userData.put("telefono", telefono);
            userData.put("fechaRegistro", System.currentTimeMillis());
            userData.put("rol", "usuario"); // Rol por defecto

            userRef.setValue(userData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅ Datos de usuario guardados en Realtime Database");

                        // ✅ REGISTRAR EVENTO
                        Map<String, Object> params = new HashMap<>();
                        params.put("user_id", userId);
                        MyApp.logEvent("user_data_saved", params);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Error guardando datos en Realtime Database: " + e.getMessage());

                        // ✅ REGISTRAR ERROR EN CRASHLYTICS
                        MyApp.logError(e);
                    });

        } catch (Exception e) {
            Log.e(TAG, "❌ Excepción en guardarDatosUsuarioEnDatabase: " + e.getMessage());
            MyApp.logError(e);
        }
    }

    /**
     * ✅ MÉTODO: Guardar userId en SharedPreferences para FCM
     */
    private void guardarUserIdEnPrefs(String userId) {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_USER_ID, userId);
            editor.apply();
            Log.d(TAG, "💾 UserId guardado en SharedPreferences: " + userId);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error guardando userId en SharedPreferences: " + e.getMessage(), e);
            MyApp.logError(e); // ✅ USANDO MyApp PARA LOG DE ERROR
        }
    }

    /**
     * ✅ NUEVO MÉTODO: Guardar token FCM después del registro exitoso
     */
    private void guardarTokenFCM(String userId) {
        Log.d(TAG, "🔑 Iniciando guardado de token FCM para usuario: " + userId);

        if (notificationManager != null) {
            try {
                notificationManager.saveFCMTokenToRealtimeDatabase(userId, "usuarios");
                Log.d(TAG, "✅ Llamada a saveFCMTokenToFirestore ejecutada para: " + userId);
            } catch (Exception e) {
                Log.e(TAG, "❌ Error llamando a saveFCMTokenToFirestore: " + e.getMessage());
                MyApp.logError(e); // ✅ USANDO MyApp PARA LOG DE ERROR
            }
        } else {
            Log.e(TAG, "❌ NotificationManager es null - no se puede guardar token FCM");
        }
    }

    /**
     * Valida todos los campos del formulario
     */
    private boolean validarCampos(String nombre, String correo, String password, String confirmPassword) {
        Log.d(TAG, "🔍 Validando campos del formulario");

        // Validar campos obligatorios
        if (nombre.isEmpty() || correo.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Log.w(TAG, "❌ Campos obligatorios vacíos");
            Toast.makeText(this, "Por favor, completa todos los campos obligatorios", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validar formato de email
        if (!isValidEmail(correo)) {
            Log.w(TAG, "❌ Formato de email inválido: " + correo);
            Toast.makeText(this, "Por favor, ingresa un correo electrónico válido", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validar contraseñas
        if (!validarContraseñas(password, confirmPassword)) {
            Log.w(TAG, "❌ Las contraseñas no coinciden");
            return false;
        }

        // Validar longitud mínima de contraseña
        if (password.length() < 6) {
            Log.w(TAG, "❌ Contraseña demasiado corta: " + password.length() + " caracteres");
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return false;
        }

        Log.d(TAG, "✅ Todos los campos validados correctamente");
        return true;
    }

    /**
     * Valida que las contraseñas coincidan
     */
    private boolean validarContraseñas(String password, String confirmPassword) {
        boolean coinciden = password.equals(confirmPassword);
        if (!coinciden) {
            Log.w(TAG, "❌ Contraseñas no coinciden - Password: " + password.length() + " chars, Confirm: " + confirmPassword.length() + " chars");
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "✅ Contraseñas coinciden correctamente");
        }
        return coinciden;
    }

    /**
     * Valida formato de email
     */
    private boolean isValidEmail(CharSequence target) {
        if (target == null) {
            Log.w(TAG, "❌ Email es null");
            return false;
        }
        boolean esValido = android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
        Log.d(TAG, "🔍 Validación email '" + target + "': " + (esValido ? "✅ VÁLIDO" : "❌ INVÁLIDO"));
        return esValido;
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "📱 onBackPressed: Regresando a actividad anterior");

        // ✅ REGISTRAR EVENTO
        Map<String, Object> params = new HashMap<>();
        params.put("screen", "RegistroUsuarios");
        params.put("action", "back_pressed");
        MyApp.logEvent("navigation_event", params);

        super.onBackPressed();
        // Opcional: agregar animación personalizada
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "🚀 onStart: Actividad visible");

        // ✅ REGISTRAR VISITA A PANTALLA
        Map<String, Object> params = new HashMap<>();
        params.put("screen_name", "RegistroUsuarios");
        MyApp.logEvent("screen_view", params);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "🔚 onDestroy: Actividad de registro siendo destruida");
    }
}