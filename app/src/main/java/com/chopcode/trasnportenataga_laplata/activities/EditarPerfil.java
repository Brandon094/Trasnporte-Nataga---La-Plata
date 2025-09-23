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

    private Button btnGuardar, btnCancelar;
    private EditText etNombre, etTelefono;
    private TextView tvNombreActual, tvTelefonoActual, tvCorreoActual;
    private RegistroService registroService;
    private UsuarioService usuarioService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_perfil_pasajero);

        // Inicializar servicios
        registroService = new RegistroService();
        usuarioService = new UsuarioService();

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

        // ✅ CORRECTO: Solo pasar nombre y teléfono
        registroService.editarPerfilPasajero(nuevoNombre, nuevoTelefono, new RegistroService.RegistroCallback() {
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
    }

    private void cargarInfoUsuario() {
        usuarioService.cargarInformacionPasajero(new UsuarioService.UsuarioCallback() {
            @Override
            public void onUsuarioCargado(Pasajero pasajero) {
                // Mostrar valores actuales en los TextViews
                tvNombreActual.setText("Nombre actual: " + pasajero.getNombre());
                tvTelefonoActual.setText("Teléfono actual: " + pasajero.getTelefono());
                tvCorreoActual.setText("Correo actual: " + pasajero.getEmail());

                // Poblar los campos editables con los valores actuales
                etNombre.setText(pasajero.getNombre());
                etTelefono.setText(pasajero.getTelefono());
            }

            @Override
            public void onError(String error) {
                Toast.makeText(EditarPerfil.this, "Error al cargar datos: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}