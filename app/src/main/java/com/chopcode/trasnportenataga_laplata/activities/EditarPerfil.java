package com.chopcode.trasnportenataga_laplata.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.managers.AuthManager;
import com.chopcode.trasnportenataga_laplata.models.Usuario;
import com.chopcode.trasnportenataga_laplata.services.UserService;

public class EditarPerfil extends AppCompatActivity {

    private Button btnGuardar, btnCancelar;
    private EditText etNombre, etTelefono, etCorreo;
    private TextView tvNombreActual, tvTelefonoActual, tvCorreoActual;
    private UserService userService;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_perfil_pasajero);

        // Inicializar servicios
        userService = new UserService();
        authManager = AuthManager.getInstance();

        // Verificar autenticación
        if (!authManager.isUserLoggedIn()) {
            authManager.redirectToLogin(this);
            finish();
            return;
        }

        // Inicializar vistas
        inicializarVistas();

        // Cargar datos actuales
        cargarInfoUsuario();

        // Configurar listeners
        configurarListeners();
    }

    private void inicializarVistas() {
        etNombre = findViewById(R.id.etNombre);
        etTelefono = findViewById(R.id.etTelefono);
        etCorreo = findViewById(R.id.etCorreo);

        tvNombreActual = findViewById(R.id.tvNombreActual);
        tvTelefonoActual = findViewById(R.id.tvTelefonoActual);
        tvCorreoActual = findViewById(R.id.tvCorreoActual);

        btnGuardar = findViewById(R.id.btnGuardarCambios);
        btnCancelar = findViewById(R.id.btnCancelar);
    }

    private void configurarListeners() {
        btnGuardar.setOnClickListener(view -> guardarCambios());
        btnCancelar.setOnClickListener(view -> finish());
    }

    private void guardarCambios() {
        String nuevoNombre = etNombre.getText().toString().trim();
        String nuevoTelefono = etTelefono.getText().toString().trim();

        if (nuevoNombre.isEmpty()) {
            etNombre.setError("Ingresa tu nombre");
            return;
        }

        if (nuevoTelefono.isEmpty()) {
            etTelefono.setError("Ingresa tu número de teléfono");
            return;
        }

        String userId = authManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Usar el método correcto del UserService
        userService.updateUserProfile(userId, nuevoNombre, nuevoTelefono,
                new UserService.UserUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(EditarPerfil.this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(EditarPerfil.this, "Error al actualizar: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void cargarInfoUsuario() {
        String userId = authManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        userService.loadUserData(userId, new UserService.UserDataCallback() {
            @Override
            public void onUserDataLoaded(Usuario usuario) {
                // Mostrar valores actuales en los TextViews
                tvNombreActual.setText("Nombre actual: " +
                        (usuario.getNombre() != null ? usuario.getNombre() : "No disponible"));
                tvTelefonoActual.setText("Teléfono actual: " +
                        (usuario.getTelefono() != null ? usuario.getTelefono() : "No disponible"));
                tvCorreoActual.setText("Correo actual: " +
                        (usuario.getEmail() != null ? usuario.getEmail() : "No disponible"));

                // Poblar los campos editables con los valores actuales
                etNombre.setText("");
                etTelefono.setText("");
                etCorreo.setText(usuario.getEmail());
            }

            @Override
            public void onError(String error) {
                Toast.makeText(EditarPerfil.this, "Error al cargar datos: " + error, Toast.LENGTH_SHORT).show();

                // Mostrar datos por defecto en caso de error
                tvNombreActual.setText("Nombre actual: No disponible");
                tvTelefonoActual.setText("Teléfono actual: No disponible");
                tvCorreoActual.setText("Correo actual: " +
                        (authManager.getCurrentUser() != null ? authManager.getCurrentUser().getEmail() : "No disponible"));
            }
        });
    }
}