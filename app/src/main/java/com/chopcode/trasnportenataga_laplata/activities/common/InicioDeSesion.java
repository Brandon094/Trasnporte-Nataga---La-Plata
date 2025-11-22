package com.chopcode.trasnportenataga_laplata.activities.common;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.widget.*;
import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.activities.driver.InicioConductor;
import com.chopcode.trasnportenataga_laplata.activities.passenger.CrearReservas;
import com.chopcode.trasnportenataga_laplata.activities.passenger.InicioUsuarios;
import com.chopcode.trasnportenataga_laplata.services.auth.IniciarService;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.*;

public class InicioDeSesion extends AppCompatActivity {

    private EditText editTextUsuario, editTextPassword;
    private Button buttonIngresar;
    private Button btnGoogleSignIn;
    private IniciarService iniciarService;
    private TextView buttonRegistro, olvidasteContraseña;

    // ✅ Constantes para SharedPreferences
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_ID = "user_id";

    // ✅ NUEVO: Tag para logs
    private static final String TAG = "InicioDeSesion";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio_de_sesion);

        Log.d(TAG, "🚀 onCreate - Iniciando actividad de login");

        // Inicializar IniciarService, pasando la actividad actual
        iniciarService = new IniciarService(this);
        Log.d(TAG, "✅ IniciarService inicializado");

        // Referenciar elementos de UI
        initViews();
        Log.d(TAG, "✅ Vistas inicializadas");

        // Configurar la toolbar
        setupToolbar();

        // Manejar inicio de sesión con correo y contraseña
        setupEmailLogin();

        // Manejar inicio de sesión con Google
        setupGoogleLogin();

        // Manejar botón de registro
        setupRegistroButton();

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

        Log.d(TAG, "✅ Vistas referenciadas correctamente");
    }

    /**
     * Configura la toolbar con navegación
     */
    private void setupToolbar() {
        Log.d(TAG, "🔧 Configurando toolbar...");
        // Tu código de toolbar aquí si lo tienes
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

            Log.d(TAG, "🔄 Llamando a iniciarSesionCorreo...");
            iniciarService.iniciarSesionCorreo(correo, password, new IniciarService.LoginCallback() {
                @Override
                public void onLoginSuccess() {
                    Log.d(TAG, "✅ Login exitoso con email");

                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        Log.d(TAG, "👤 Usuario Firebase obtenido: " + user.getUid());

                        // ✅ Guardar userId en SharedPreferences
                        guardarUserIdEnPrefs(user.getUid());

                        Log.d(TAG, "🔍 Detectando tipo de usuario...");
                        iniciarService.detectarTipoUsuario(user, new IniciarService.TipoUsuarioCallback() {
                            @Override
                            public void onTipoDetectado(String tipo) {
                                Log.d(TAG, "🎯 Tipo de usuario detectado: " + tipo);
                                if (tipo.equals("conductor")) {
                                    Log.d(TAG, "🚗 Redirigiendo a InicioConductor");
                                    irAInicioConductor();
                                } else {
                                    Log.d(TAG, "👤 Redirigiendo a InicioUsuarios");
                                    irAInicioUsuarios();
                                }
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "❌ Error detectando tipo de usuario: " + error);
                                Toast.makeText(InicioDeSesion.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Log.e(TAG, "❌ Usuario Firebase es null después de login exitoso");
                    }
                }

                @Override
                public void onLoginFailure(String error) {
                    Log.e(TAG, "❌ Error en login con email: " + error);
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
            iniciarService.iniciarSesionGoogle(new IniciarService.LoginCallback() {
                @Override
                public void onLoginSuccess() {
                    Log.d(TAG, "✅ Login con Google exitoso");

                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        Log.d(TAG, "👤 Usuario Google obtenido: " + user.getUid());

                        // ✅ Guardar userId en SharedPreferences
                        guardarUserIdEnPrefs(user.getUid());

                        Log.d(TAG, "🔍 Detectando tipo de usuario Google...");
                        iniciarService.detectarTipoUsuario(user, new IniciarService.TipoUsuarioCallback() {
                            @Override
                            public void onTipoDetectado(String tipo) {
                                Log.d(TAG, "🎯 Tipo de usuario Google: " + tipo);
                                if (tipo.equals("conductor")) {
                                    Log.d(TAG, "🚗 Redirigiendo a InicioConductor (Google)");
                                    irAInicioConductor();
                                } else {
                                    Log.d(TAG, "👤 Redirigiendo a InicioUsuarios (Google)");
                                    irAInicioUsuarios();
                                }
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "❌ Error detectando tipo de usuario Google: " + error);
                                Toast.makeText(InicioDeSesion.this, "Error al detectar tipo de usuario: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                @Override
                public void onLoginFailure(String error) {
                    Log.e(TAG, "❌ Error en login con Google: " + error);
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

    // Recibir el resultado del One Tap Sign-In de Google
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "🔄 onActivityResult - requestCode: " + requestCode + ", resultCode: " + resultCode);

        if (requestCode == IniciarService.REQ_ONE_TAP) {
            Log.d(TAG, "🔍 Procesando resultado de Google Sign-In...");
            iniciarService.manejarResultadoGoogle(data, new IniciarService.LoginCallback() {
                @Override
                public void onLoginSuccess() {
                    Log.d(TAG, "✅ Google Sign-In exitoso desde onActivityResult");

                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        Log.d(TAG, "👤 Usuario Google (ActivityResult): " + user.getUid());

                        // ✅ Guardar userId en SharedPreferences
                        guardarUserIdEnPrefs(user.getUid());

                        Log.d(TAG, "🔍 Detectando tipo de usuario (ActivityResult)...");
                        iniciarService.detectarTipoUsuario(user, new IniciarService.TipoUsuarioCallback() {
                            @Override
                            public void onTipoDetectado(String tipo) {
                                Log.d(TAG, "🎯 Tipo de usuario (ActivityResult): " + tipo);
                                if (tipo.equals("conductor")) {
                                    Log.d(TAG, "🚗 Redirigiendo a InicioConductor (ActivityResult)");
                                    irAInicioConductor();
                                } else {
                                    Log.d(TAG, "👤 Redirigiendo a InicioUsuarios (ActivityResult)");
                                    irAInicioUsuarios();
                                }
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "❌ Error detectando tipo de usuario (ActivityResult): " + error);
                                Toast.makeText(InicioDeSesion.this, "Error al detectar tipo de usuario: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                @Override
                public void onLoginFailure(String error) {
                    Log.e(TAG, "❌ Error en Google Sign-In (ActivityResult): " + error);
                    Toast.makeText(InicioDeSesion.this, "Error: " + error, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Log.d(TAG, "ℹ️ requestCode no manejado: " + requestCode);
        }
    }

    /**
     * ✅ MÉTODO: Guardar userId en SharedPreferences para FCM
     */
    private void guardarUserIdEnPrefs(String userId) {
        try {
            Log.d(TAG, "💾 Guardando userId en SharedPreferences: " + userId);

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_USER_ID, userId);
            boolean saved = editor.commit(); // Usar commit() para asegurar guardado inmediato

            if (saved) {
                Log.d(TAG, "✅ UserId guardado exitosamente en SharedPreferences: " + userId);

                // Verificar que se guardó correctamente
                String savedUserId = prefs.getString(KEY_USER_ID, null);
                if (savedUserId != null && savedUserId.equals(userId)) {
                    Log.d(TAG, "✅ Verificación: UserId correctamente guardado y recuperado");
                } else {
                    Log.e(TAG, "❌ Verificación: UserId NO se guardó correctamente");
                }
            } else {
                Log.e(TAG, "❌ Error: No se pudo guardar userId en SharedPreferences");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error guardando userId en SharedPreferences: " + e.getMessage());
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