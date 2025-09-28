package com.chopcode.trasnportenataga_laplata.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.managers.AuthManager;
import com.chopcode.trasnportenataga_laplata.models.Usuario;
import com.chopcode.trasnportenataga_laplata.services.UserService;
import com.google.android.material.card.MaterialCardView;

public class PerfilUsuario extends AppCompatActivity {
    private TextView tvNombre, tvCorreo, tvTelefono;
    private MaterialCardView cardEditarPerfil, cardHistorialReservas, cardVolverInicio, cardCerrarSesion;
    private AuthManager authManager;
    private UserService userService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_pasajero);

        // Inicializar servicios
        authManager = AuthManager.getInstance();
        userService = new UserService();

        // Verificar si el usuario está logueado
        if (!authManager.isUserLoggedIn()) {
            authManager.redirectToLogin(this);
            finish();
            return;
        }

        // Referencias a elementos de la UI
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
     * Método para obtener la información del usuario usando loadUserData
     */
    private void cargarInfoUsuario() {
        String userId = authManager.getUserId();

        if (userId == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        userService.loadUserData(userId, new UserService.UserDataCallback() {
            @Override
            public void onUserDataLoaded(Usuario usuario) {
                // Actualizar la UI con los datos del usuario
                runOnUiThread(() -> {
                    if (usuario.getNombre() != null) {
                        tvNombre.setText(usuario.getNombre());
                    } else {
                        tvNombre.setText("Nombre no disponible");
                    }

                    if (usuario.getTelefono() != null) {
                        tvTelefono.setText(usuario.getTelefono());
                    } else {
                        tvTelefono.setText("Teléfono no disponible");
                    }

                    if (usuario.getEmail() != null) {
                        tvCorreo.setText(usuario.getEmail());
                    } else {
                        tvCorreo.setText("Email no disponible");
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(PerfilUsuario.this, "Error cargando datos: " + error, Toast.LENGTH_SHORT).show();
                    // Mostrar datos por defecto en caso de error
                    tvNombre.setText("Usuario");
                    tvTelefono.setText("Teléfono no disponible");
                    tvCorreo.setText(authManager.getCurrentUser().getEmail());
                });
            }
        });
    }

    /**
     * Cierra la sesión y redirige a la pantalla de inicio de sesión.
     */
    private void cerrarSesion() {
        authManager.signOut(this);
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}