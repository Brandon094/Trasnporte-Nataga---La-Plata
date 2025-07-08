package com.chopcode.trasnportenataga_laplata.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.models.Pasajero;
import com.chopcode.trasnportenataga_laplata.models.Usuario;
import com.chopcode.trasnportenataga_laplata.services.UsuarioService;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Actividad para mostrar el perfil del usuario (pasajero).
 */
public class PerfilUsuario extends AppCompatActivity {
    private TextView tvNombre, tvCorreo, tvTelefono;
    private Button btnCerrarSesion, btnEditarPerfil, btnHistorialReservas;
    private FirebaseAuth auth;
    private UsuarioService usuarioService = new UsuarioService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_pasajero);

        // Inicializar FirebaseAuth
        auth = FirebaseAuth.getInstance();

        // Referencias a elementos de la UI
        tvNombre = findViewById(R.id.tvNombreUsuario);
        tvCorreo = findViewById(R.id.tvEmail);
        tvTelefono = findViewById(R.id.tvPhone);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        btnEditarPerfil = findViewById(R.id.btnEditarPerfil);
        btnHistorialReservas = findViewById(R.id.btnHistorialReservas);

        // Cargar los datos del usuario desde Firebase
        cargarInfoUsuario();

        // Botón para cerrar sesión
        btnCerrarSesion.setOnClickListener(view -> cerrarSesion());
        // Boton para editar el perfil
        btnEditarPerfil.setOnClickListener(view -> editPerfil());
        // Boton para ver el historial de reservas
        //btnHistorialReservas
    }
    /** llamar metodo para editar perfil*/
    private void editPerfil(){
        // Ir a la pantalla Editar Pasajero
        Intent intent = new Intent(this, EditarPerfil.class);
        startActivity(intent);
    }
    /** llamar metodo para ver historial de reservas*/
    private void historialReservas(){
        // Ir a la pantalla de Historial de reservas
        Intent intent = new Intent(this, HistorialReservas.class);
        startActivity(intent);
    }

    /**
     * Metodo para obtener la informacion del usuario desde un callBack*/
    private  void cargarInfoUsuario(){
        usuarioService.cargarInformacionPasajero(new UsuarioService.UsuarioCallback() {
            @Override
            public void onUsuarioCargado(Pasajero pasajero) {
                tvNombre.setText("👤 Nombre Pasajero: " + pasajero.getNombre());
                tvTelefono.setText("\uD83D\uDCDE Telefono Pasajero: " + pasajero.getTelefono());
                tvCorreo.setText("\uD83D\uDCE7 Email Pasajero: " + pasajero.getEmail());
            }

            @Override
            public void onError(String error) {
                Toast.makeText(PerfilUsuario.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Cierra la sesión y redirige a la pantalla de inicio de sesión.
     */
    private void cerrarSesion() {
        auth.signOut();
        Intent intent = new Intent(this, InicioDeSesion.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
