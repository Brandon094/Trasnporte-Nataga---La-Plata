package com.chopcode.trasnportenataga_laplata.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
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

        // Referencias a la UI
        tvConductor = findViewById(R.id.tvNombreUsuario);
        tvEmail = findViewById(R.id.tvEmail);
        tvTelefono = findViewById(R.id.tvPhone);

        //Datos del vehiculo
        tvPlaca = findViewById(R.id.tvPlacaVehiculo);
        tvModVehiculo = findViewById(R.id.tvModeloVehiculo);
        tvCapacidad = findViewById(R.id.tvCapacidadVehiculo);
        // Cargar info conductor
        cargarInfoConductor();

        // 🔹 Manejo del botón Cerrar Sesión
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        btnCerrarSesion.setOnClickListener(view -> cerrarSesion());

        // 🔹 Manejo del botón Historial de reservas
        btnHistorialReservas = findViewById(R.id.btnHistorialReservas);
        btnHistorialReservas.setOnClickListener(view -> irHistorialReservas());

        // 🔹 Manejo del botón Historial de reservas
        btnEditarPerfil = findViewById(R.id.btnEditarPerfil);
        btnEditarPerfil.setOnClickListener(view -> irEditarPerfil());
    }
    /**Metodo para abrir el editor del perfil
     */
    public void irEditarPerfil(){
        Intent intent = new Intent(PerfilConductor.this, EditarPerfilConductor.class);
    }
    /**
     * Metodo para abrir el hsitorial de reservas
     */
    public void irHistorialReservas(){
        Intent intent = new Intent(PerfilConductor.this, HistorialReservas.class);
        startActivity(intent);
    }

    /**
     *  Metodo para obtener la informacion del conductor desde un callBack
     */
    private void cargarInfoConductor(){
        usuarioService.cargarInformacionConductor(new UsuarioService.ConductorCallback() {
            @Override
            public void onConductorCargado(Conductor conductor) {
                // Informacion del conductor
                tvConductor.setText("\uD83D\uDC68\u200D✈️ Nombre Conductor: " + conductor.getNombre());
                tvTelefono.setText("\uD83D\uDCDE Teléfono Conductor: " + conductor.getTelefono());
                tvEmail.setText("\uD83D\uDCE7 Email: " + conductor.getEmail());
                // Informacion del vehiculo
                tvPlaca.setText("\uD83D\uDE98 Placa del Vehículo: " + conductor.getPlacaVehiculo());
                tvCapacidad.setText("\uD83D\uDC65 Capacidad: " +conductor.getCapacidadVehiculo() );
                tvModVehiculo.setText("#️⃣ Modelo: " +conductor.getModeloVehiculo());
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getApplicationContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    /**
     * 🔹 Cierra la sesión y redirige a la pantalla de inicio.
     */
    private void cerrarSesion() {
        auth.signOut();
        Intent intent = new Intent(this, InicioDeSesion.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}