package com.chopcode.trasnportenataga_laplata.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.services.RegistroService;

public class RegistroUsuarios extends AppCompatActivity {

    private EditText editTextNombre, editTextCorreo, editTextTelefono, editTextPassword, editTextConfirmPassword;
    private Button buttonRegistrar;
    private TextView buttonIniciarSesion;
    private RegistroService registroService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro_usuarios);

        // Inicializar vistas del layout
        editTextNombre = findViewById(R.id.editTextNombre);
        editTextCorreo = findViewById(R.id.editTextCorreo);
        editTextTelefono = findViewById(R.id.editTextTelefono);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonRegistrar = findViewById(R.id.buttonRegistrar);
        buttonIniciarSesion = findViewById(R.id.buttonIniciarSesion);

        // Inicializar servicio de registro
        registroService = new RegistroService();

        // Redirigir al usuario a la pantalla de inicio de sesión
        buttonIniciarSesion.setOnClickListener(v -> {
            startActivity(new Intent(RegistroUsuarios.this, InicioDeSesion.class));
            finish(); // Cierra la pantalla de registro para que no vuelva atrás
        });

        // Manejar el clic del botón de registro
        buttonRegistrar.setOnClickListener(v -> registrarUsuario());
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

        // Validaciones
        if (nombre.isEmpty() || correo.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        // Registro del usuario
        registroService.registrarUsuario(nombre, correo, telefono, password, new RegistroService.RegistroCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(RegistroUsuarios.this, "Usuario registrado exitosamente", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(RegistroUsuarios.this, InicioDeSesion.class));
                finish();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(RegistroUsuarios.this, "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
