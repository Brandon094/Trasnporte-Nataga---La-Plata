package com.chopcode.rutago_app.activities.passenger.editProfile;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.chopcode.rutago_app.R;
import com.chopcode.rutago_app.config.MyApp;
import com.chopcode.rutago_app.managers.AuthManager;
import com.chopcode.rutago_app.models.Usuario;
import com.chopcode.rutago_app.services.user.UserService;

import java.util.HashMap;
import java.util.Map;

public class EditarPerfil extends AppCompatActivity {

    private Button btnGuardar, btnCancelar;
    private EditText etNombre, etTelefono, etCorreo;
    private TextView tvNombreActual, tvTelefonoActual, tvCorreoActual;
    private UserService userService;
    private AuthManager authManager;

    // ✅ NUEVO: Tag para logs
    private static final String TAG = "EditarPerfil";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "🚀 onCreate - Iniciando actividad de edición de perfil");

        // ✅ Registrar evento analítico de inicio de pantalla
        registrarEventoAnalitico("pantalla_editar_perfil_inicio", null, null);

        setContentView(R.layout.edit_perfil_pasajero);
        Log.d(TAG, "✅ Layout inflado correctamente");

        // Inicializar servicios
        userService = new UserService();
        authManager = AuthManager.getInstance();
        Log.d(TAG, "✅ Servicios inicializados");

        // Verificar autenticación usando MyApp
        if (!authManager.isUserLoggedIn()) {
            Log.w(TAG, "⚠️ Usuario no autenticado - redirigiendo a login");

            // ✅ Registrar evento de redirección
            registrarEventoAnalitico("redireccion_login_editar_perfil", null, null);

            authManager.redirectToLogin(this);
            finish();
            return;
        }
        Log.d(TAG, "✅ Usuario autenticado validado");

        // Inicializar vistas
        inicializarVistas();

        // Cargar datos actuales
        cargarInfoUsuario();

        // Configurar listeners
        configurarListeners();

        Log.d(TAG, "✅ Configuración completa - Actividad lista");
    }

    private void inicializarVistas() {
        Log.d(TAG, "🔧 Inicializando vistas...");

        etNombre = findViewById(R.id.etNombre);
        etTelefono = findViewById(R.id.etTelefono);
        etCorreo = findViewById(R.id.etCorreo);

        tvNombreActual = findViewById(R.id.tvNombreActual);
        tvTelefonoActual = findViewById(R.id.tvTelefonoActual);
        tvCorreoActual = findViewById(R.id.tvCorreoActual);

        btnGuardar = findViewById(R.id.btnGuardarCambios);
        btnCancelar = findViewById(R.id.btnCancelar);

        Log.d(TAG, "✅ Todas las vistas inicializadas correctamente");
    }

    private void configurarListeners() {
        Log.d(TAG, "🔧 Configurando listeners...");

        btnGuardar.setOnClickListener(view -> {
            Log.d(TAG, "🎯 Click en botón Guardar");

            // ✅ Registrar evento de interacción
            registrarEventoAnalitico("click_boton_guardar", null, null);

            guardarCambios();
        });

        btnCancelar.setOnClickListener(view -> {
            Log.d(TAG, "🎯 Click en botón Cancelar - finalizando actividad");

            // ✅ Registrar evento de interacción
            registrarEventoAnalitico("click_boton_cancelar", null, null);

            finish();
        });

        Log.d(TAG, "✅ Listeners configurados correctamente");
    }

    private void guardarCambios() {
        Log.d(TAG, "🔄 Iniciando proceso de guardar cambios...");

        String nuevoNombre = etNombre.getText().toString().trim();
        String nuevoTelefono = etTelefono.getText().toString().trim();

        Log.d(TAG, "📝 Datos capturados:");
        Log.d(TAG, "   - Nuevo nombre: " + nuevoNombre);
        Log.d(TAG, "   - Nuevo teléfono: " + nuevoTelefono);

        if (nuevoNombre.isEmpty()) {
            Log.w(TAG, "⚠️ Validación fallida - nombre vacío");

            // ✅ Registrar evento de validación fallida
            registrarEventoAnalitico("validacion_fallida_nombre_vacio", null, null);

            etNombre.setError("Ingresa tu nombre");
            return;
        }

        if (nuevoTelefono.isEmpty()) {
            Log.w(TAG, "⚠️ Validación fallida - teléfono vacío");

            // ✅ Registrar evento de validación fallida
            registrarEventoAnalitico("validacion_fallida_telefono_vacio", null, null);

            etTelefono.setError("Ingresa tu número de teléfono");
            return;
        }

        Log.d(TAG, "✅ Validaciones de campos exitosas");

        // ✅ Registrar evento de validación exitosa
        registrarEventoAnalitico("validacion_exitosa", null, null);

        // ✅ Usar MyApp para obtener el ID del usuario
        String userId = MyApp.getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "❌ UserId es null - no se puede actualizar perfil");

            // ✅ Registrar evento de error
            registrarEventoAnalitico("error_userid_null_actualizacion", null, null);

            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "👤 UserId obtenido usando MyApp: " + userId);
        Log.d(TAG, "🔄 Llamando a updateUserProfile...");

        // ✅ Registrar evento de inicio de actualización
        registrarEventoAnalitico("actualizacion_perfil_inicio", null, null);
        registrarCambiosAnaliticos(nuevoNombre, nuevoTelefono);

        // Usar el método correcto del UserService
        userService.updateUserProfile(userId, nuevoNombre, nuevoTelefono,
                new UserService.UserUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "✅ Perfil actualizado exitosamente en Firebase");

                        // ✅ Registrar evento de éxito
                        registrarEventoAnalitico("actualizacion_perfil_exitosa", null, null);

                        runOnUiThread(() -> {
                            Toast.makeText(EditarPerfil.this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "✅ Mostrando toast de éxito");

                            // ✅ Registrar evento de finalización
                            registrarEventoAnalitico("pantalla_editar_perfil_finalizada", null, null);

                            finish();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ Error actualizando perfil: " + error);

                        // ✅ Usar MyApp para logging de errores
                        MyApp.logError(new Exception("Error actualizando perfil usuario: " + error));

                        // ✅ Registrar evento de error
                        registrarEventoAnalitico("actualizacion_perfil_error", null, null);

                        runOnUiThread(() -> {
                            Toast.makeText(EditarPerfil.this, "Error al actualizar: " + error, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "❌ Mostrando toast de error al usuario");
                        });
                    }
                });
    }

    private void cargarInfoUsuario() {
        Log.d(TAG, "🔍 Cargando información actual del usuario...");

        // ✅ Usar MyApp para obtener el ID del usuario
        String userId = MyApp.getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "❌ UserId es null - no se pueden cargar datos");

            // ✅ Registrar evento de error
            registrarEventoAnalitico("error_userid_null_carga", null, null);

            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "👤 Cargando datos para userId usando MyApp: " + userId);

        // ✅ Registrar evento de inicio de carga
        registrarEventoAnalitico("carga_datos_usuario_editar_inicio", null, null);

        userService.loadUserData(userId, new UserService.UserDataCallback() {
            @Override
            public void onUserDataLoaded(Usuario usuario) {
                Log.d(TAG, "✅ Datos de usuario cargados exitosamente:");
                Log.d(TAG, "   - Nombre: " + usuario.getNombre());
                Log.d(TAG, "   - Teléfono: " + usuario.getTelefono());
                Log.d(TAG, "   - Email: " + usuario.getEmail());

                // ✅ Registrar evento de carga exitosa
                registrarDatosUsuarioCargadosAnalitico(usuario);

                runOnUiThread(() -> {
                    // Mostrar valores actuales en los TextViews
                    String nombreActual = usuario.getNombre() != null ? usuario.getNombre() : "No disponible";
                    String telefonoActual = usuario.getTelefono() != null ? usuario.getTelefono() : "No disponible";
                    String correoActual = usuario.getEmail() != null ? usuario.getEmail() : "No disponible";

                    tvNombreActual.setText("Nombre actual: " + nombreActual);
                    tvTelefonoActual.setText("Teléfono actual: " + telefonoActual);
                    tvCorreoActual.setText("Correo actual: " + correoActual);

                    Log.d(TAG, "✅ TextViews de datos actuales actualizados");

                    // Poblar los campos editables con los valores actuales
                    etNombre.setText(nombreActual.equals("No disponible") ? "" : nombreActual);
                    etTelefono.setText(telefonoActual.equals("No disponible") ? "" : telefonoActual);
                    etCorreo.setText(correoActual);

                    Log.d(TAG, "✅ Campos editables poblados con datos actuales");
                    Log.d(TAG, "   - Campo nombre: " + (nombreActual.equals("No disponible") ? "vacío" : "con datos"));
                    Log.d(TAG, "   - Campo teléfono: " + (telefonoActual.equals("No disponible") ? "vacío" : "con datos"));
                    Log.d(TAG, "   - Campo correo: " + correoActual);
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error cargando datos de usuario: " + error);

                // ✅ Usar MyApp para logging de errores
                MyApp.logError(new Exception("Error cargando datos usuario editar: " + error));

                // ✅ Registrar evento de error
                registrarEventoAnalitico("carga_datos_usuario_editar_error", null, null);

                runOnUiThread(() -> {
                    Toast.makeText(EditarPerfil.this, "Error al cargar datos: " + error, Toast.LENGTH_SHORT).show();

                    // Mostrar datos por defecto en caso de error
                    tvNombreActual.setText("Nombre actual: No disponible");
                    tvTelefonoActual.setText("Teléfono actual: No disponible");

                    // ✅ Usar MyApp para obtener email del usuario
                    String emailDefault = "No disponible";
                    if (MyApp.getCurrentUser() != null && MyApp.getCurrentUser().getEmail() != null) {
                        emailDefault = MyApp.getCurrentUser().getEmail();
                    }
                    tvCorreoActual.setText("Correo actual: " + emailDefault);

                    // Poblar campos editables con valores vacíos
                    etNombre.setText("");
                    etTelefono.setText("");
                    etCorreo.setText(emailDefault);

                    Log.w(TAG, "⚠️ Mostrando datos por defecto debido a error en carga");
                    Log.d(TAG, "   - Email por defecto: " + emailDefault);
                });
            }
        });
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

        // ✅ Registrar evento analítico de resumen
        registrarEventoAnalitico("pantalla_editar_perfil_resume", null, null);
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

        // ✅ Registrar evento de destrucción
        registrarEventoAnalitico("pantalla_editar_perfil_destroy", null, null);
    }

    /**
     * ✅ MÉTODO AUXILIAR: Registrar eventos analíticos usando MyApp
     */
    private void registrarEventoAnalitico(String evento, Integer count, Integer count2) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("user_id", MyApp.getCurrentUserId());
            params.put("pantalla", "EditarPerfil");

            if (count != null) {
                params.put("count", count);
            }
            if (count2 != null) {
                params.put("count2", count2);
            }

            params.put("timestamp", System.currentTimeMillis());

            MyApp.logEvent(evento, params);
            Log.d(TAG, "📊 Evento analítico registrado: " + evento);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando evento analítico: " + e.getMessage());
        }
    }

    /**
     * ✅ MÉTODO AUXILIAR: Registrar datos de usuario cargados usando MyApp
     */
    private void registrarDatosUsuarioCargadosAnalitico(Usuario usuario) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("user_id", MyApp.getCurrentUserId());
            params.put("user_nombre", usuario.getNombre() != null ? usuario.getNombre() : "N/A");
            params.put("user_telefono", usuario.getTelefono() != null ? usuario.getTelefono() : "N/A");
            params.put("user_email", usuario.getEmail() != null ? usuario.getEmail() : "N/A");
            params.put("timestamp", System.currentTimeMillis());
            params.put("pantalla", "EditarPerfil");

            MyApp.logEvent("datos_usuario_cargados_editar", params);
            Log.d(TAG, "📊 Datos de usuario cargados registrados en analytics");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando datos usuario cargados: " + e.getMessage());
        }
    }

    /**
     * ✅ MÉTODO AUXILIAR: Registrar cambios realizados usando MyApp
     */
    private void registrarCambiosAnaliticos(String nuevoNombre, String nuevoTelefono) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("user_id", MyApp.getCurrentUserId());
            params.put("nuevo_nombre", nuevoNombre);
            params.put("nuevo_telefono", nuevoTelefono);
            params.put("timestamp", System.currentTimeMillis());
            params.put("pantalla", "EditarPerfil");

            MyApp.logEvent("cambios_perfil_solicitados", params);
            Log.d(TAG, "📊 Cambios de perfil solicitados registrados en analytics");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando cambios analíticos: " + e.getMessage());
        }
    }
}