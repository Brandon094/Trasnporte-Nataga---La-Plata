package com.chopcode.trasnportenataga_laplata.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.models.Pasajero;
import com.chopcode.trasnportenataga_laplata.services.UsuarioService;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Actividad para mostrar el perfil del usuario (pasajero) - Diseño Moderno
 */
public class PerfilUsuario extends AppCompatActivity {
    private TextView tvNombre, tvCorreo, tvTelefono;
    private MaterialCardView cardEditarPerfil, cardHistorialReservas, cardVolverInicio, cardCerrarSesion;
    private FirebaseAuth auth;
    private UsuarioService usuarioService = new UsuarioService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_pasajero);

        // Inicializar FirebaseAuth
        auth = FirebaseAuth.getInstance();

        // Referencias a elementos de la UI - Nuevo diseño
        inicializarVistas();

        // Cargar los datos del usuario desde Firebase
        cargarInfoUsuario();

        // Configurar listeners de botones
        configurarBotones();
    }

    private void inicializarVistas() {
        // TextViews
        tvNombre = findViewById(R.id.tvNombreUsuario);
        tvCorreo = findViewById(R.id.tvEmail);
        tvTelefono = findViewById(R.id.tvPhone);

        // Cards del grid (nuevo diseño)
        cardEditarPerfil = findViewById(R.id.cardEditarPerfil);
        cardHistorialReservas = findViewById(R.id.cardHistorialReservas);
        cardVolverInicio = findViewById(R.id.cardVolverInicio);
        cardCerrarSesion = findViewById(R.id.cardCerrarSesion);
    }

    private void configurarBotones() {
        // Botón Editar Perfil
        cardEditarPerfil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Efecto de pulsación
                cardEditarPerfil.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                cardEditarPerfil.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                                editPerfil();
                            }
                        }).start();
            }
        });

        // Botón Historial Reservas
        cardHistorialReservas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardHistorialReservas.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                cardHistorialReservas.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                                historialReservas();
                            }
                        }).start();
            }
        });

        // Botón Volver al Inicio
        cardVolverInicio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardVolverInicio.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                cardVolverInicio.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                                volverAlInicio();
                            }
                        }).start();
            }
        });

        // Botón Cerrar Sesión
        cardCerrarSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardCerrarSesion.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                cardCerrarSesion.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                                mostrarDialogoConfirmacion();
                            }
                        }).start();
            }
        });
    }

    /** Método para volver al inicio del pasajero */
    private void volverAlInicio() {
        Intent intent = new Intent(this, InicioUsuarios.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    /** Método para editar perfil */
    private void editPerfil() {
        Intent intent = new Intent(this, EditarPerfil.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    /** Método para ver historial de reservas */
    private void historialReservas() {
        Intent intent = new Intent(this, HistorialReservas.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    /** Método para mostrar diálogo de confirmación de cierre de sesión */
    private void mostrarDialogoConfirmacion() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro de que quieres cerrar sesión?")
                .setPositiveButton("Sí", (dialog, which) -> cerrarSesion())
                .setNegativeButton("Cancelar", null)
                .setIcon(R.drawable.ic_logout)
                .show();
    }

    /**
     * Método para obtener la información del usuario desde un callback
     */
    private void cargarInfoUsuario() {
        usuarioService.cargarInformacionPasajero(new UsuarioService.UsuarioCallback() {
            @Override
            public void onUsuarioCargado(Pasajero pasajero) {
                // Actualizar la UI con los datos del usuario
                tvNombre.setText(pasajero.getNombre());
                tvTelefono.setText(pasajero.getTelefono());
                tvCorreo.setText(pasajero.getEmail());

                // Opcional: Cargar foto de perfil si está disponible
                // cargarFotoPerfil(pasajero.getFotoUrl());
            }

            @Override
            public void onError(String error) {
                Toast.makeText(PerfilUsuario.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Método opcional para cargar foto de perfil
     */
    private void cargarFotoPerfil(String fotoUrl) {
        if (fotoUrl != null && !fotoUrl.isEmpty()) {
            // Usar Picasso o Glide para cargar la imagen
            // Picasso.get().load(fotoUrl).into(ivProfilePicture);
        }
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
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}