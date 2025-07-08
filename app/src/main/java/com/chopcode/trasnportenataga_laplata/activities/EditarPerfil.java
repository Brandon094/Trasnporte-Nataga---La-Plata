package com.chopcode.trasnportenataga_laplata.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.models.Pasajero;
import com.chopcode.trasnportenataga_laplata.services.RegistroService;
import com.chopcode.trasnportenataga_laplata.services.UsuarioService;

public class EditarPerfil extends AppCompatActivity {

    private Button btnGuardar;
    private EditText nombre, correo, telefono;
    private TextView nombreActual, telefonoActual, correoActual;
    private RegistroService registroService;
    private UsuarioService usuarioService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_perfil_pasajero);

        // Inicializar servicios y UI
        registroService = new RegistroService();
        usuarioService = new UsuarioService();

        nombre = findViewById(R.id.etNombre);
        correo = findViewById(R.id.etCorreo);
        telefono = findViewById(R.id.etTelefono);

        nombreActual = findViewById(R.id.tvNombreActual);
        correoActual = findViewById(R.id.tvCorreoActual);
        telefonoActual = findViewById(R.id.tvTelefonoActual);
        btnGuardar = findViewById(R.id.btnGuardarCambios);

        // Cargar datos actuales
        cargarInfoUsuario();

        btnGuardar.setOnClickListener(view -> {
            String nuevoNombre = nombre.getText().toString().trim();
            String nuevoCorreo = correo.getText().toString().trim();
            String nuevoTelefono = telefono.getText().toString().trim();

            if (nuevoNombre.isEmpty()) {
                nombre.setError("Ingresa tu nombre");
                return;
            }

            if (nuevoCorreo.isEmpty()) {
                correo.setError("Ingresa tu correo");
                return;
            }

            if (nuevoTelefono.isEmpty()) {
                telefono.setError("Ingresa tu número de teléfono");
                return;
            }

            registroService.editarPerfilPasajero(nuevoNombre, nuevoTelefono, nuevoCorreo, new RegistroService.RegistroCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(EditarPerfil.this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(EditarPerfil.this, "Error al actualizar: " + error, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void cargarInfoUsuario() {
        usuarioService.cargarInformacionPasajero(new UsuarioService.UsuarioCallback() {
            @Override
            public void onUsuarioCargado(Pasajero pasajero) {
                nombreActual.setText("👤 Nombre actual: " + pasajero.getNombre());
                telefonoActual.setText("📞 Teléfono actual: " + pasajero.getTelefono());
                correoActual.setText("📧 Email actual: " + pasajero.getEmail());
            }

            @Override
            public void onError(String error) {
                Toast.makeText(EditarPerfil.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
