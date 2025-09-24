package com.chopcode.trasnportenataga_laplata.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.adapters.ReservaAdapter;
import com.chopcode.trasnportenataga_laplata.adapters.RutaAdapter;
import com.chopcode.trasnportenataga_laplata.managers.AuthManager;
import com.chopcode.trasnportenataga_laplata.managers.StatisticsManager;
import com.chopcode.trasnportenataga_laplata.models.Reserva;
import com.chopcode.trasnportenataga_laplata.models.Ruta;
import com.chopcode.trasnportenataga_laplata.services.ReservaService;
import com.chopcode.trasnportenataga_laplata.services.UserService;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class InicioConductor extends AppCompatActivity {
    // Views
    private RecyclerView rvReservas, rvProximasRutas;
    private TextView tvTotalIngresos, tvReservasActivas, tvProximaRuta, tvConductor, tvPlacaVehiculo;
    private TextView tvEmptyReservas, tvEmptyRutas, tvReservasConfirmadas, tvAsientosDisponibles, tvInfoCapacidad;
    private MaterialButton btnPerfilConductor, btnCerrarSesion;

    // Adapters
    private ReservaAdapter reservaAdapter;
    private RutaAdapter rutaAdapter;

    // Data
    private List<Reserva> listaReservas = new ArrayList<>();
    private List<Ruta> listaRutas = new ArrayList<>();
    private List<String> horariosAsignados = new ArrayList<>();

    // Statistics
    private double totalIngresos = 0.0;
    private int reservasConfirmadasHoy = 0;
    private int asientosDisponibles = 14;
    private final int CAPACIDAD_TOTAL = 14;

    // Services
    private AuthManager authManager;
    private UserService userService;
    private ReservaService reservaService;
    private StatisticsManager statisticsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_inicio_conductor);

            // Inicializar services
            authManager = AuthManager.getInstance();
            userService = new UserService();
            reservaService = new ReservaService();

            if (!authManager.validateLogin(this)) {
                finish();
                return;
            }

            initializeViews();
            setupRecyclerView();
            setupButtons();
            loadDriverData();

        } catch (Exception e) {
            Log.e("InicioConductor", "Error en onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error al iniciar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeViews() {
        tvConductor = findViewById(R.id.tvConductor);
        tvPlacaVehiculo = findViewById(R.id.tvPlacaVehiculo);
        tvTotalIngresos = findViewById(R.id.tvTotalIngresos);
        tvReservasActivas = findViewById(R.id.tvReservasActivas);
        tvProximaRuta = findViewById(R.id.tvProximaRuta);
        tvEmptyReservas = findViewById(R.id.tvEmptyReservas);
        tvEmptyRutas = findViewById(R.id.tvEmptyRutas);
        tvReservasConfirmadas = findViewById(R.id.tvReservasConfirmadas);
        tvAsientosDisponibles = findViewById(R.id.tvAsientosDisponibles);
        tvInfoCapacidad = findViewById(R.id.tvInfoCapacidad);
        btnPerfilConductor = findViewById(R.id.btnPerfilConductor);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        rvReservas = findViewById(R.id.recyclerReservas);
        rvProximasRutas = findViewById(R.id.recyclerProximasRutas);

        updateStatisticsUI();
    }

    private void setupRecyclerView() {
        reservaAdapter = new ReservaAdapter(listaReservas, new ReservaAdapter.OnReservaClickListener() {
            @Override
            public void onConfirmarClick(Reserva reserva) {
                showConfirmationDialog(reserva, true);
            }

            @Override
            public void onCancelarClick(Reserva reserva) {
                showConfirmationDialog(reserva, false);
            }
        });

        rvReservas.setLayoutManager(new LinearLayoutManager(this));
        rvReservas.setAdapter(reservaAdapter);

        rutaAdapter = new RutaAdapter(listaRutas);
        rvProximasRutas.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvProximasRutas.setAdapter(rutaAdapter);
    }

    private void setupButtons() {
        btnCerrarSesion.setOnClickListener(view -> {
            authManager.signOut(this);
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
        });
        btnPerfilConductor.setOnClickListener(view -> goToDriverProfile());
    }

    private void loadDriverData() {
        String userId = authManager.getUserId();
        if (userId == null) {
            showDefaultData();
            return;
        }

        userService.loadDriverData(userId, new UserService.DriverDataCallback() {
            @Override
            public void onDriverDataLoaded(String nombre, String placa, List<String> horarios) {
                runOnUiThread(() -> {
                    tvConductor.setText(nombre != null ? nombre : "N/A");
                    tvPlacaVehiculo.setText(getString(R.string.placaVehiculo, placa != null ? placa : "N/A"));
                    horariosAsignados = horarios;

                    calculateStatistics(nombre);
                    loadReservations(nombre);
                    loadRoutes();
                });
            }

            @Override
            public void onError(String error) {
                Log.e("InicioConductor", "Error cargando datos conductor: " + error);
                showDefaultData();
            }
        });
    }

    private void calculateStatistics(String conductorNombre) {
        statisticsManager.calculateDailyStatistics(conductorNombre, new UserService.StatisticsCallback() {
            @Override
            public void onStatisticsCalculated(int reservasConfirmadas, int asientosDisp, double ingresos) {
                runOnUiThread(() -> {
                    reservasConfirmadasHoy = reservasConfirmadas;
                    asientosDisponibles = asientosDisp;
                    totalIngresos = ingresos;
                    updateStatisticsUI();
                });
            }

            @Override
            public void onError(String error) {
                Log.e("InicioConductor", "Error calculando estadísticas: " + error);
            }
        });
    }

    private void loadReservations(String conductorNombre) {
        reservaService.cargarReservasConductor(conductorNombre, horariosAsignados,
                new ReservaService.DriverReservationsCallback() {
                    @Override
                    public void onDriverReservationsLoaded(List<Reserva> reservas) {
                        runOnUiThread(() -> {
                            listaReservas.clear();
                            listaReservas.addAll(reservas);
                            reservaAdapter.actualizarReservas(new ArrayList<>(listaReservas));
                            updateReservationsUI();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("InicioConductor", "Error cargando reservas: " + error);
                        showEmptyReservations();
                    }
                });
    }

    private void loadRoutes() {
        userService.loadAssignedRoutes(horariosAsignados, new UserService.RoutesCallback() {
            @Override
            public void onRoutesLoaded(List<Ruta> rutas) {
                runOnUiThread(() -> {
                    listaRutas.clear();
                    listaRutas.addAll(rutas);
                    rutaAdapter.notifyDataSetChanged();
                    updateRoutesUI();
                });
            }

            @Override
            public void onError(String error) {
                Log.e("InicioConductor", "Error cargando rutas: " + error);
                showEmptyRoutes();
            }
        });
    }

    private void updateStatisticsUI() {
        tvReservasConfirmadas.setText(String.valueOf(reservasConfirmadasHoy));
        tvAsientosDisponibles.setText(String.valueOf(asientosDisponibles));
        tvInfoCapacidad.setText("Capacidad total: " + CAPACIDAD_TOTAL + " asientos");
        tvTotalIngresos.setText(getString(R.string.totalIngresos, totalIngresos));
    }

    private void updateReservationsUI() {
        tvReservasActivas.setText(getString(R.string.reservasPendientes, listaReservas.size()));
        tvEmptyReservas.setVisibility(listaReservas.isEmpty() ? View.VISIBLE : View.GONE);
        rvReservas.setVisibility(listaReservas.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void updateRoutesUI() {
        if (!listaRutas.isEmpty()) {
            Ruta proximaRuta = listaRutas.get(0);
            String horario = proximaRuta.getHora() != null ? proximaRuta.getHora().getHora() : "--:--";
            tvProximaRuta.setText("Próxima ruta: " + proximaRuta.getOrigen() + " → " +
                    proximaRuta.getDestino() + " a las " + horario);
        } else {
            tvProximaRuta.setText("Próxima: No hay rutas programadas");
        }

        tvEmptyRutas.setVisibility(listaRutas.isEmpty() ? View.VISIBLE : View.GONE);
        rvProximasRutas.setVisibility(listaRutas.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showConfirmationDialog(Reserva reserva, boolean isConfirmation) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(isConfirmation ? "Confirmar Reserva" : "Cancelar Reserva")
                .setMessage(isConfirmation ?
                        "¿Confirmar reserva de " + reserva.getNombre() + "?" :
                        "¿Cancelar reserva de " + reserva.getNombre() + "?")
                .setPositiveButton(isConfirmation ? "Confirmar" : "Cancelar", (dialog, which) -> {
                    if (isConfirmation) {
                        confirmReservation(reserva);
                    } else {
                        cancelReservation(reserva);
                    }
                })
                .setNegativeButton("Volver", null)
                .show();
    }

    private void confirmReservation(Reserva reserva) {
        updateReservationStatus(reserva, "Confirmada");
    }

    private void cancelReservation(Reserva reserva) {
        updateReservationStatus(reserva, "Cancelada");
    }

    private void updateReservationStatus(Reserva reserva, String nuevoEstado) {
        reservaService.actualizarEstadoReserva(reserva.getIdReserva(), nuevoEstado,
                new ReservaService.ReservationUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(InicioConductor.this,
                                "Reserva " + nuevoEstado.toLowerCase(), Toast.LENGTH_SHORT).show();

                        if ("Confirmada".equals(nuevoEstado)) {
                            updateIncomeAfterConfirmation(reserva);
                        }

                        reloadData();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(InicioConductor.this,
                                "Error al actualizar: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateIncomeAfterConfirmation(Reserva reserva) {
        String userId = authManager.getUserId();
        if (userId != null) {
            statisticsManager.updateIncomeInFirebase(userId, totalIngresos + reserva.getPrecio(),
                    new UserService.IncomeUpdateCallback() {
                        @Override
                        public void onSuccess(double nuevosIngresos) {
                            totalIngresos = nuevosIngresos;
                            runOnUiThread(() -> updateStatisticsUI());
                        }

                        @Override
                        public void onError(String error) {
                            Log.e("InicioConductor", "Error actualizando ingresos: " + error);
                        }
                    });
        }
    }

    private void reloadData() {
        String nombreConductor = tvConductor.getText().toString().replace("Conductor: ", "");
        if (!nombreConductor.isEmpty()) {
            calculateStatistics(nombreConductor);
            loadReservations(nombreConductor);
        }
    }

    private void goToDriverProfile() {
        if (authManager.isUserLoggedIn()) {
            startActivity(new Intent(this, PerfilConductor.class));
        }
    }

    private void showDefaultData() {
        showEmptyReservations();
        showEmptyRoutes();
        tvConductor.setText("N/A");
        tvPlacaVehiculo.setText("Placa: N/A");
        tvTotalIngresos.setText("Ingresos: $0");
        reservasConfirmadasHoy = 0;
        asientosDisponibles = CAPACIDAD_TOTAL;
        updateStatisticsUI();
    }

    private void showEmptyReservations() {
        tvEmptyReservas.setVisibility(View.VISIBLE);
        rvReservas.setVisibility(View.GONE);
        tvReservasActivas.setText(getString(R.string.reservasPendientes, 0));
    }

    private void showEmptyRoutes() {
        tvEmptyRutas.setVisibility(View.VISIBLE);
        rvProximasRutas.setVisibility(View.GONE);
        tvProximaRuta.setText("Próxima: No hay rutas asignadas");
    }
}