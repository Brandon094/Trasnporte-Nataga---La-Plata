package com.chopcode.trasnportenataga_laplata.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.managers.AuthManager;
import com.chopcode.trasnportenataga_laplata.models.Vehiculo;
import com.chopcode.trasnportenataga_laplata.services.UserService;
import com.chopcode.trasnportenataga_laplata.services.VehiculoService;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class PerfilConductor extends AppCompatActivity {
    private TextView tvConductor, tvEmail, tvTelefono, tvPlaca, tvModVehiculo, tvCapacidad, tvAnioVehiculo;
    private MaterialCardView cardEditarPerfil, cardHistorialViajes, cardDisponibilidad, cardCerrarSesion;
    private UserService userService;
    private VehiculoService vehiculoService;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_conductor);

        // Inicializar servicios
        userService = new UserService();
        vehiculoService = new VehiculoService();
        authManager = AuthManager.getInstance();

        // Verificar autenticación
        if (!authManager.isUserLoggedIn()) {
            authManager.redirectToLogin(this);
            finish();
            return;
        }

        inicializarVistas();
        cargarInfoConductorYVehiculo();
        configurarBotones();
    }

    private void inicializarVistas() {
        // TextViews de información personal
        tvConductor = findViewById(R.id.tvNombreUsuario);
        tvEmail = findViewById(R.id.tvEmail);
        tvTelefono = findViewById(R.id.tvPhone);

        // TextViews de información del vehículo
        tvPlaca = findViewById(R.id.tvPlacaVehiculo);
        tvModVehiculo = findViewById(R.id.tvModeloVehiculo);
        tvCapacidad = findViewById(R.id.tvCapacidadVehiculo);
        tvAnioVehiculo = findViewById(R.id.tvAnioVehiculo);

        // Cards de botones
        cardEditarPerfil = findViewById(R.id.cardEditarPerfil);
        cardHistorialViajes = findViewById(R.id.cardHistorialViajes);
        cardDisponibilidad = findViewById(R.id.cardInicio);
        cardCerrarSesion = findViewById(R.id.cardCerrarSesion);
    }

    private void configurarBotones() {
        cardEditarPerfil.setOnClickListener(view -> irEditarPerfil());
        cardHistorialViajes.setOnClickListener(view -> irHistorialViajes());
        cardDisponibilidad.setOnClickListener(view -> irInicioConductor());

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

    /**
     * Método para cargar la información del conductor y su vehículo
     */
    private void cargarInfoConductorYVehiculo() {
        String userId = authManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        userService.loadDriverData(userId, new UserService.DriverDataCallback() {
            @Override
            public void onDriverDataLoaded(String nombre, String placaVehiculo, List<String> horariosAsignados) {
                runOnUiThread(() -> {
                    // Información del conductor
                    tvConductor.setText(nombre != null ? nombre : "Conductor");
                    tvPlaca.setText(placaVehiculo != null ? placaVehiculo : "No asignado");

                    // Cargar información adicional del usuario
                    cargarInformacionUsuarioCompleta(userId);

                    // Si hay placa de vehículo, cargar información detallada
                    if (placaVehiculo != null && !placaVehiculo.isEmpty()) {
                        cargarInformacionVehiculo(placaVehiculo);
                    } else {
                        mostrarVehiculoNoDisponible();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(PerfilConductor.this, "Error al cargar conductor: " + error, Toast.LENGTH_SHORT).show();
                    Log.e("PerfilConductor", error);
                    cargarInformacionUsuarioCompleta(userId);
                    mostrarVehiculoNoDisponible();
                });
            }
        });
    }

    /**
     * Cargar información completa del usuario (email, teléfono)
     */
    private void cargarInformacionUsuarioCompleta(String userId) {
        userService.loadUserData(userId, new UserService.UserDataCallback() {
            @Override
            public void onUserDataLoaded(com.chopcode.trasnportenataga_laplata.models.Usuario usuario) {
                runOnUiThread(() -> {
                    if (usuario.getEmail() != null) {
                        tvEmail.setText(usuario.getEmail());
                    } else {
                        tvEmail.setText(authManager.getCurrentUser() != null ?
                                authManager.getCurrentUser().getEmail() : "No disponible");
                    }

                    if (usuario.getTelefono() != null) {
                        tvTelefono.setText(usuario.getTelefono());
                    } else {
                        tvTelefono.setText("No disponible");
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    if (authManager.getCurrentUser() != null) {
                        tvEmail.setText(authManager.getCurrentUser().getEmail());
                    }
                    tvTelefono.setText("No disponible");
                });
            }
        });
    }

    /**
     * Cargar información detallada del vehículo por placa
     */
    private void cargarInformacionVehiculo(String placa) {
        vehiculoService.obtenerVehiculoPorPlaca(placa, new VehiculoService.VehiculoCallback() {
            @Override
            public void onVehiculoCargado(Vehiculo vehiculo) {
                runOnUiThread(() -> {
                    if (vehiculo != null) {
                        // Mostrar información detallada del vehículo
                        tvPlaca.setText(vehiculo.getPlaca() != null ? vehiculo.getPlaca() : "No disponible");
                        tvModVehiculo.setText(vehiculo.getModelo() != null ?
                                vehiculo.getMarca() + " " + vehiculo.getModelo() : "No disponible");
                        tvCapacidad.setText(String.valueOf(vehiculo.getCapacidad()));

                        if (vehiculo.getAnio() > 0) {
                            tvAnioVehiculo.setText(String.valueOf(vehiculo.getAnio()));
                        } else {
                            tvAnioVehiculo.setText("N/A");
                        }
                    } else {
                        mostrarVehiculoBasico(placa);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e("PerfilConductor", "Error cargando vehículo: " + error);
                    mostrarVehiculoBasico(placa);
                });
            }
        });
    }

    /**
     * Mostrar información básica del vehículo cuando no se pueden cargar los detalles
     */
    private void mostrarVehiculoBasico(String placa) {
        tvPlaca.setText(placa);
        tvModVehiculo.setText("Información no disponible");
        tvCapacidad.setText("N/A");
        tvAnioVehiculo.setText("N/A");
    }

    /**
     * Método para mostrar estado cuando no hay vehículo
     */
    private void mostrarVehiculoNoDisponible() {
        tvPlaca.setText("No asignado");
        tvCapacidad.setText("N/A");
        tvModVehiculo.setText("No asignado");
        tvAnioVehiculo.setText("N/A");
    }

    /**
     * Método para mostrar diálogo de confirmación de cierre de sesión
     */
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
     * Método para ir a la edición del perfil
     */
    public void irEditarPerfil(){
        Intent intent = new Intent(PerfilConductor.this, EditarPerfilConductor.class);
        startActivity(intent);
    }

    /**
     * Método para ir al historial de viajes
     */
    public void irHistorialViajes(){
        Intent intent = new Intent(PerfilConductor.this, HistorialViajesConductor.class);
        startActivity(intent);
    }

    /**
     * Método para ir al inicio del conductor
     */
    public void irInicioConductor(){
        Intent intent = new Intent(PerfilConductor.this, InicioConductor.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    /**
     * Método para cerrar sesión
     */
    private void cerrarSesion() {
        authManager.signOut(this);
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarInfoConductorYVehiculo();
    }
}