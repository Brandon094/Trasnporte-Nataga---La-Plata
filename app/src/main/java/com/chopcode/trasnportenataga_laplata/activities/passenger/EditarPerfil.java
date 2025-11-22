package com.chopcode.trasnportenataga_laplata.activities.passenger;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.managers.AuthManager;
import com.chopcode.trasnportenataga_laplata.models.Usuario;
import com.chopcode.trasnportenataga_laplata.services.user.UserService;

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

        setContentView(R.layout.edit_perfil_pasajero);
        Log.d(TAG, "✅ Layout inflado correctamente");

        // Inicializar servicios
        userService = new UserService();
        authManager = AuthManager.getInstance();
        Log.d(TAG, "✅ Servicios inicializados");

        // Verificar autenticación
        if (!authManager.isUserLoggedIn()) {
            Log.w(TAG, "⚠️ Usuario no autenticado - redirigiendo a login");
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
            guardarCambios();
        });

        btnCancelar.setOnClickListener(view -> {
            Log.d(TAG, "🎯 Click en botón Cancelar - finalizando actividad");
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
            etNombre.setError("Ingresa tu nombre");
            return;
        }

        if (nuevoTelefono.isEmpty()) {
            Log.w(TAG, "⚠️ Validación fallida - teléfono vacío");
            etTelefono.setError("Ingresa tu número de teléfono");
            return;
        }

        Log.d(TAG, "✅ Validaciones de campos exitosas");

        String userId = authManager.getUserId();
        if (userId == null) {
            Log.e(TAG, "❌ UserId es null - no se puede actualizar perfil");
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "👤 UserId obtenido: " + userId);
        Log.d(TAG, "🔄 Llamando a updateUserProfile...");

        // Usar el método correcto del UserService
        userService.updateUserProfile(userId, nuevoNombre, nuevoTelefono,
                new UserService.UserUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "✅ Perfil actualizado exitosamente en Firebase");
                        runOnUiThread(() -> {
                            Toast.makeText(EditarPerfil.this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "✅ Mostrando toast de éxito");
                            finish();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ Error actualizando perfil: " + error);
                        runOnUiThread(() -> {
                            Toast.makeText(EditarPerfil.this, "Error al actualizar: " + error, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "❌ Mostrando toast de error al usuario");
                        });
                    }
                });
    }

    private void cargarInfoUsuario() {
        Log.d(TAG, "🔍 Cargando información actual del usuario...");

        String userId = authManager.getUserId();
        if (userId == null) {
            Log.e(TAG, "❌ UserId es null - no se pueden cargar datos");
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "👤 Cargando datos para userId: " + userId);
        userService.loadUserData(userId, new UserService.UserDataCallback() {
            @Override
            public void onUserDataLoaded(Usuario usuario) {
                Log.d(TAG, "✅ Datos de usuario cargados exitosamente:");
                Log.d(TAG, "   - Nombre: " + usuario.getNombre());
                Log.d(TAG, "   - Teléfono: " + usuario.getTelefono());
                Log.d(TAG, "   - Email: " + usuario.getEmail());

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
                runOnUiThread(() -> {
                    Toast.makeText(EditarPerfil.this, "Error al cargar datos: " + error, Toast.LENGTH_SHORT).show();

                    // Mostrar datos por defecto en caso de error
                    tvNombreActual.setText("Nombre actual: No disponible");
                    tvTelefonoActual.setText("Teléfono actual: No disponible");

                    String emailDefault = authManager.getCurrentUser() != null ?
                            authManager.getCurrentUser().getEmail() : "No disponible";
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