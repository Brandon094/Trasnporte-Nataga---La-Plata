package com.chopcode.trasnportenataga_laplata.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.services.RegistroService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

public class RegistroUsuarios extends AppCompatActivity {

    private TextInputEditText editTextNombre, editTextCorreo, editTextTelefono, editTextPassword, editTextConfirmPassword;
    private Button buttonRegistrar;
    private TextView buttonIniciarSesion;
    private MaterialToolbar topAppBar;
    private RegistroService registroService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro_usuarios);

        // Inicializar vistas del layout
        initViews();

        // Configurar la toolbar
        setupToolbar();

        // Inicializar servicio de registro
        registroService = new RegistroService();

        // Redirigir al usuario a la pantalla de inicio de sesión
        buttonIniciarSesion.setOnClickListener(v -> {
            startActivity(new Intent(RegistroUsuarios.this, InicioDeSesion.class));
            finish(); // Cierra la pantalla de registro para que no vuelva atrás
        });

        // Manejar el clic del botón de registro
        buttonRegistrar.setOnClickListener(v -> registrarUsuario());

        // Configurar limpieza de campos al hacer clic en los iconos de clear
        setupClearTextFunctionality();
    }

    /**
     * Inicializa todas las vistas del layout
     */
    private void initViews() {
        editTextNombre = findViewById(R.id.editTextNombre);
        editTextCorreo = findViewById(R.id.editTextCorreo);
        editTextTelefono = findViewById(R.id.editTextTelefono);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonRegistrar = findViewById(R.id.buttonRegistrar);
        buttonIniciarSesion = findViewById(R.id.buttonIniciarSesion);
        topAppBar = findViewById(R.id.topAppBar);
    }

    /**
     * Configura la toolbar con navegación
     */
    private void setupToolbar() {
        topAppBar.setNavigationOnClickListener(v -> {
            // Regresar a la actividad anterior
            onBackPressed();
        });
    }

    /**
     * Configura la funcionalidad de limpiar campos con los iconos
     */
    private void setupClearTextFunctionality() {
        // Los iconos de clear text funcionan automáticamente con TextInputLayout
        // pero podemos agregar validaciones adicionales si es necesario

        // Limpiar mensajes de error cuando el usuario comience a escribir
        setupTextWatchers();
    }

    /**
     * Configura listeners para limpiar errores cuando el usuario escribe
     */
    private void setupTextWatchers() {
        // Puedes agregar TextWatchers aquí para validación en tiempo real
        // Por ejemplo, limpiar errores cuando el usuario comience a corregir
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

        // Validaciones mejoradas
        if (!validarCampos(nombre, correo, password, confirmPassword)) {
            return;
        }

        // Mostrar loading state en el botón
        buttonRegistrar.setEnabled(false);
        buttonRegistrar.setText("Registrando...");

        // Registro del usuario
        registroService.registrarUsuario(nombre, correo, telefono, password, new RegistroService.RegistroCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    buttonRegistrar.setEnabled(true);
                    buttonRegistrar.setText("Registrarse");
                    Toast.makeText(RegistroUsuarios.this, "Usuario registrado exitosamente", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegistroUsuarios.this, InicioDeSesion.class));
                    finish();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    buttonRegistrar.setEnabled(true);
                    buttonRegistrar.setText("Registrarse");
                    Toast.makeText(RegistroUsuarios.this, "Error: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Valida todos los campos del formulario
     */
    private boolean validarCampos(String nombre, String correo, String password, String confirmPassword) {
        // Validar campos obligatorios
        if (nombre.isEmpty() || correo.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos obligatorios", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validar formato de email
        if (!isValidEmail(correo)) {
            Toast.makeText(this, "Por favor, ingresa un correo electrónico válido", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validar contraseñas
        if (!validarContraseñas(password, confirmPassword)) {
            return false;
        }

        // Validar longitud mínima de contraseña
        if (password.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    /**
     * Valida que las contraseñas coincidan
     */
    private boolean validarContraseñas(String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /**
     * Valida formato de email
     */
    private boolean isValidEmail(CharSequence target) {
        if (target == null) {
            return false;
        }
        return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Opcional: agregar animación personalizada
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}