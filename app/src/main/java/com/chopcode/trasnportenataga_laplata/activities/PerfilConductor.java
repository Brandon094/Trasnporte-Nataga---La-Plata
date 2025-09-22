package com.chopcode.trasnportenataga_laplata.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.models.Conductor;
import com.chopcode.trasnportenataga_laplata.services.UsuarioService;
import com.google.firebase.auth.FirebaseAuth;

public class PerfilConductor extends AppCompatActivity {
    private TextView tvConductor, tvEmail, tvTelefono, tvPlaca, tvModVehiculo, tvCapacidad;
    private Button btnEditarPerfil, btnHistorialReservas, btnCerrarSesion;
    private UsuarioService usuarioService = new UsuarioService();
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_conductor);

        auth = FirebaseAuth.getInstance();
        inicializarVistas();
        cargarInfoConductor();
        configurarBotones();
    }

    private void inicializarVistas() {
        tvConductor = findViewById(R.id.tvNombreUsuario);
        tvEmail = findViewById(R.id.tvEmail);
        tvTelefono = findViewById(R.id.tvPhone);
        tvPlaca = findViewById(R.id.tvPlacaVehiculo);
        tvModVehiculo = findViewById(R.id.tvModeloVehiculo);
        tvCapacidad = findViewById(R.id.tvCapacidadVehiculo);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        btnHistorialReservas = findViewById(R.id.btnHistorialReservas);
        btnEditarPerfil = findViewById(R.id.btnEditarPerfil);
    }

    private void configurarBotones() {
        btnCerrarSesion.setOnClickListener(view -> cerrarSesion());
        btnHistorialReservas.setOnClickListener(view -> irHistorialReservas());
        btnEditarPerfil.setOnClickListener(view -> irEditarPerfil());
    }

    /**
     * Método para cargar la información del conductor autenticado
     */
    private void cargarInfoConductor(){
        usuarioService.cargarInformacionConductor(new UsuarioService.ConductorCallback() {
            @Override
            public void onConductorCargado(Conductor conductor) {
                // Información del conductor - Solo los valores, sin etiquetas
                tvConductor.setText(conductor.getNombre());
                tvTelefono.setText(conductor.getTelefono());
                tvEmail.setText(conductor.getEmail());
                // Información del vehículo - Solo los valores, sin etiquetas
                tvPlaca.setText(conductor.getPlacaVehiculo());
                tvCapacidad.setText(String.valueOf(conductor.getCapacidadVehiculo()));
                tvModVehiculo.setText(conductor.getModeloVehiculo());
            }

            @Override
            public void onError(String error) {
                Toast.makeText(PerfilConductor.this, error, Toast.LENGTH_SHORT).show();
                Log.e("PerfilConductor", error);
            }
        });
    }

    public void irEditarPerfil(){
        Intent intent = new Intent(PerfilConductor.this, EditarPerfilConductor.class);
        startActivity(intent);
    }

    public void irHistorialReservas(){
        Intent intent = new Intent(PerfilConductor.this, HistorialReservas.class);
        startActivity(intent);
    }

    private void cerrarSesion() {
        if (auth != null) {
            auth.signOut();
        }
        Intent intent = new Intent(this, InicioDeSesion.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}