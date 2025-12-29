package com.chopcode.trasnportenataga_laplata.activities.driver;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.activities.driver.profile.PerfilConductor;
import com.chopcode.trasnportenataga_laplata.adapters.reservas.ReservaAdapter;
import com.chopcode.trasnportenataga_laplata.adapters.rutas.RutaAdapter;
import com.chopcode.trasnportenataga_laplata.config.MyApp;
import com.chopcode.trasnportenataga_laplata.managers.AuthManager;
import com.chopcode.trasnportenataga_laplata.models.Reserva;
import com.chopcode.trasnportenataga_laplata.models.Ruta;
import com.chopcode.trasnportenataga_laplata.viewmodels.driver.DriverHomeViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class InicioConductor extends AppCompatActivity {
    // Views
    private RecyclerView rvReservas, rvProximasRutas;
    private TextView tvConductor, tvPlacaVehiculo;
    private TextView tvEmptyReservas, tvEmptyRutas, tvReservasConfirmadas, tvAsientosDisponibles;
    private TextView tvTotalIngresos, tvInfoCapacidad, tvInfoIngresos;
    private MaterialButton btnPerfilConductor, btnCerrarSesion;
    private ProgressBar progressBar;

    // Adapters
    private ReservaAdapter reservaAdapter;
    private RutaAdapter rutaAdapter;

    // Data
    private List<Reserva> listaReservas = new ArrayList<>();
    private List<Ruta> listaRutas = new ArrayList<>();

    // ViewModel
    private DriverHomeViewModel viewModel;
    private AuthManager authManager;

    private static final String TAG = "InicioConductor";
    private static final int CAPACIDAD_TOTAL = 14;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio_conductor);
        Log.d(TAG, "🚀 onCreate - Iniciando actividad de conductor");

        // ✅ Registrar evento de inicio usando MyApp
        registrarEventoAnalitico("pantalla_inicio_conductor_inicio", null, null);

        // Inicializar ViewModel
        viewModel = new ViewModelProvider(this).get(DriverHomeViewModel.class);
        // ✅ Inicializar ViewModel con contexto
        viewModel.initialize(this.getApplicationContext());

        authManager = AuthManager.getInstance();

        initializeViews();
        setupRecyclerView();
        setupButtons();
        setupObservers();

        loadDriverData();
    }

    private void initializeViews() {
        Log.d(TAG, "🔧 Inicializando vistas...");

        tvConductor = findViewById(R.id.tvConductor);
        tvPlacaVehiculo = findViewById(R.id.tvPlacaVehiculo);
        tvTotalIngresos = findViewById(R.id.tvTotalIngresos);
        tvEmptyReservas = findViewById(R.id.tvEmptyReservas);
        tvEmptyRutas = findViewById(R.id.tvEmptyRutas);
        tvReservasConfirmadas = findViewById(R.id.tvReservasConfirmadas);
        tvAsientosDisponibles = findViewById(R.id.tvAsientosDisponibles);
        tvInfoCapacidad = findViewById(R.id.tvInfoCapacidad);
        tvInfoIngresos = findViewById(R.id.tvInfoIngresos);
        progressBar = findViewById(R.id.progressBar);

        btnPerfilConductor = findViewById(R.id.btnPerfilConductor);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        rvReservas = findViewById(R.id.recyclerReservas);
        rvProximasRutas = findViewById(R.id.recyclerProximasRutas);

        // Configurar textos informativos
        if (tvInfoCapacidad != null) {
            tvInfoCapacidad.setText("De " + CAPACIDAD_TOTAL + " totales");
        }
        if (tvInfoIngresos != null) {
            tvInfoIngresos.setText("Acumulado del día");
        }

        Log.d(TAG, "✅ Todas las vistas inicializadas");
    }

    private void setupObservers() {
        Log.d(TAG, "👀 Configurando observadores...");

        // Observar nombre del conductor
        viewModel.getNombreConductorLiveData().observe(this, nombre -> {
            if (nombre != null && !nombre.isEmpty()) {
                tvConductor.setText(nombre);
                Log.d(TAG, "✅ Nombre del conductor actualizado: " + nombre);
            } else {
                tvConductor.setText("N/A");
            }
        });

        // Observar placa del vehículo
        viewModel.getPlacaVehiculoLiveData().observe(this, placa -> {
            if (placa != null && !placa.isEmpty()) {
                tvPlacaVehiculo.setText(getString(R.string.placaVehiculo, placa));
                Log.d(TAG, "✅ Placa del vehículo actualizada: " + placa);
            } else {
                tvPlacaVehiculo.setText(getString(R.string.placaVehiculo, "N/A"));
            }
        });

        // Observar reservas
        viewModel.getReservasLiveData().observe(this, reservas -> {
            Log.d(TAG, "🔄 Reservas actualizadas: " + (reservas != null ? reservas.size() : 0));

            listaReservas.clear();
            if (reservas != null && !reservas.isEmpty()) {
                listaReservas.addAll(reservas);
                reservaAdapter.actualizarReservas(new ArrayList<>(reservas));

                // ✅ Registrar evento analítico
                registrarEventoAnalitico("reservas_actualizadas", reservas.size(), null);
            }
            updateReservationsUI();
        });

        // Observar rutas
        viewModel.getRutasLiveData().observe(this, rutas -> {
            Log.d(TAG, "🔄 Rutas actualizadas: " + (rutas != null ? rutas.size() : 0));

            listaRutas.clear();
            if (rutas != null && !rutas.isEmpty()) {
                listaRutas.addAll(rutas);
                rutaAdapter.notifyDataSetChanged();

                // ✅ Registrar evento analítico
                registrarEventoAnalitico("rutas_actualizadas", rutas.size(), null);

                // Mostrar información de la próxima ruta
                if (!rutas.isEmpty()) {
                    Ruta proximaRuta = rutas.get(0);
                    String horario = proximaRuta.getHora() != null ?
                            proximaRuta.getHora().getHora() : "--:--";
                    Log.d(TAG, "✅ Ruta próxima: " + proximaRuta.getOrigen() +
                            " → " + proximaRuta.getDestino() + " (" + horario + ")");
                }
            }
            updateRoutesUI();
        });

        // Observar estadísticas
        viewModel.getReservasConfirmadasLiveData().observe(this, count -> {
            if (count != null) {
                tvReservasConfirmadas.setText(String.valueOf(count));
                Log.d(TAG, "📊 Reservas confirmadas: " + count);
            }
        });

        viewModel.getAsientosDisponiblesLiveData().observe(this, asientos -> {
            if (asientos != null) {
                tvAsientosDisponibles.setText(String.valueOf(asientos));
                Log.d(TAG, "📊 Asientos disponibles: " + asientos);
            }
        });

        viewModel.getIngresosLiveData().observe(this, ingresos -> {
            if (ingresos != null) {
                tvTotalIngresos.setText(formatCurrency(ingresos));
                Log.d(TAG, "💰 Ingresos actualizados: $" + ingresos);
            }
        });

        // Observar estado de carga
        viewModel.getLoadingLiveData().observe(this, isLoading -> {
            if (isLoading != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                Log.d(TAG, isLoading ? "⏳ Cargando datos..." : "✅ Carga completada");
            }
        });

        // Observar errores
        viewModel.getErrorLiveData().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Log.e(TAG, "❌ Error observado: " + error);
                Toast.makeText(InicioConductor.this,
                        "Error: " + error, Toast.LENGTH_SHORT).show();

                // ✅ Registrar error en analytics
                registrarErrorAnalitico("error_ui", error);
            }
        });

        Log.d(TAG, "✅ Observadores configurados");
    }

    private void setupRecyclerView() {
        Log.d(TAG, "🔧 Configurando RecyclerView...");

        reservaAdapter = new ReservaAdapter(listaReservas, new ReservaAdapter.OnReservaClickListener() {
            @Override
            public void onConfirmarClick(Reserva reserva) {
                Log.d(TAG, "🎯 Click en CONFIRMAR reserva: " + reserva.getIdReserva());
                showConfirmationDialog(reserva, true);
            }

            @Override
            public void onCancelarClick(Reserva reserva) {
                Log.d(TAG, "🎯 Click en CANCELAR reserva: " + reserva.getIdReserva());
                showConfirmationDialog(reserva, false);
            }
        });

        rvReservas.setLayoutManager(new LinearLayoutManager(this));
        rvReservas.setAdapter(reservaAdapter);
        Log.d(TAG, "✅ RecyclerView de reservas configurado");

        rutaAdapter = new RutaAdapter(listaRutas);
        rvProximasRutas.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false));
        rvProximasRutas.setAdapter(rutaAdapter);
        Log.d(TAG, "✅ RecyclerView de rutas configurado");
    }

    private void setupButtons() {
        Log.d(TAG, "🔧 Configurando botones...");

        btnCerrarSesion.setOnClickListener(view -> {
            Log.d(TAG, "🚪 Cerrando sesión de conductor...");

            // ✅ Registrar evento de cierre de sesión usando MyApp
            registrarEventoAnalitico("conductor_cerro_sesion", null, null);

            authManager.signOut(this);
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
        });

        btnPerfilConductor.setOnClickListener(view -> {
            Log.d(TAG, "👤 Navegando a perfil de conductor");

            // ✅ Registrar evento de navegación usando MyApp
            registrarEventoAnalitico("navegar_perfil_conductor", null, null);

            goToDriverProfile();
        });

        Log.d(TAG, "✅ Botones configurados");
    }

    private void loadDriverData() {
        Log.d(TAG, "🔧 Cargando datos del conductor...");

        if (!authManager.validateLogin(this)) {
            Log.w(TAG, "⚠️ Login no válido - finalizando actividad");
            finish();
            return;
        }

        String userId = MyApp.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "⚠️ UserId es null - mostrando datos por defecto");
            showDefaultData();
            return;
        }

        Log.d(TAG, "👤 UserId del conductor: " + userId);
        viewModel.loadDriverData(userId);
    }

    private void showConfirmationDialog(Reserva reserva, boolean isConfirmation) {
        Log.d(TAG, "💬 Mostrando diálogo de " + (isConfirmation ? "confirmación" : "cancelación") +
                " para reserva: " + reserva.getIdReserva());

        new MaterialAlertDialogBuilder(this)
                .setTitle(isConfirmation ? "Confirmar Reserva" : "Cancelar Reserva")
                .setMessage(isConfirmation ?
                        "¿Confirmar reserva de " + reserva.getNombre() + "?" :
                        "¿Cancelar reserva de " + reserva.getNombre() + "?")
                .setPositiveButton(isConfirmation ? "Confirmar" : "Cancelar", (dialog, which) -> {
                    Log.d(TAG, "✅ Usuario confirmó " + (isConfirmation ? "confirmación" : "cancelación"));

                    // ✅ Registrar evento de acción
                    registrarAccionReserva(reserva, isConfirmation ? "confirmar" : "cancelar");

                    if (isConfirmation) {
                        viewModel.confirmReservation(reserva);
                    } else {
                        viewModel.cancelReservation(reserva);
                    }
                })
                .setNegativeButton("Volver", (dialog, which) -> {
                    Log.d(TAG, "❌ Usuario canceló la acción");
                    dialog.dismiss();
                })
                .show();
    }

    // Métodos de UI
    private void updateReservationsUI() {
        Log.d(TAG, "🔄 Actualizando UI de reservas - Total: " + listaReservas.size());

        boolean hayReservas = !listaReservas.isEmpty();
        tvEmptyReservas.setVisibility(hayReservas ? View.GONE : View.VISIBLE);
        rvReservas.setVisibility(hayReservas ? View.VISIBLE : View.GONE);

        Log.d(TAG, "✅ UI de reservas actualizada - " +
                (hayReservas ? "Mostrando " + listaReservas.size() + " reservas" : "Sin reservas"));
    }

    private void updateRoutesUI() {
        Log.d(TAG, "🔄 Actualizando UI de rutas - Total: " + listaRutas.size());

        boolean hayRutas = !listaRutas.isEmpty();
        tvEmptyRutas.setVisibility(hayRutas ? View.GONE : View.VISIBLE);
        rvProximasRutas.setVisibility(hayRutas ? View.VISIBLE : View.GONE);

        Log.d(TAG, "✅ UI de rutas actualizada - " +
                (hayRutas ? "Mostrando " + listaRutas.size() + " rutas" : "Sin rutas"));
    }

    private String formatCurrency(double amount) {
        if (amount == 0) {
            return "$0";
        } else if (amount < 1000) {
            return String.format("$%.0f", amount);
        } else if (amount < 1000000) {
            return String.format("$%.1fK", amount / 1000);
        } else {
            return String.format("$%.1fM", amount / 1000000);
        }
    }

    private void goToDriverProfile() {
        Log.d(TAG, "👤 Navegando a perfil de conductor");

        if (authManager.isUserLoggedIn()) {
            startActivity(new Intent(this, PerfilConductor.class));
            Log.d(TAG, "✅ Intent iniciado para PerfilConductor");
        } else {
            Log.w(TAG, "⚠️ Usuario no logeado - no se puede navegar al perfil");
            Toast.makeText(this, "Debe iniciar sesión primero", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDefaultData() {
        Log.d(TAG, "ℹ️ Mostrando datos por defecto");

        showEmptyReservations();
        showEmptyRoutes();
        tvConductor.setText("N/A");
        tvPlacaVehiculo.setText(getString(R.string.placaVehiculo, "N/A"));
        tvReservasConfirmadas.setText("0");
        tvAsientosDisponibles.setText(String.valueOf(CAPACIDAD_TOTAL));
        tvTotalIngresos.setText("$0");

        Log.d(TAG, "✅ Datos por defecto mostrados");
    }

    private void showEmptyReservations() {
        Log.d(TAG, "ℹ️ Mostrando estado vacío para reservas");

        tvEmptyReservas.setVisibility(View.VISIBLE);
        rvReservas.setVisibility(View.GONE);
    }

    private void showEmptyRoutes() {
        Log.d(TAG, "ℹ️ Mostrando estado vacío para rutas");

        tvEmptyRutas.setVisibility(View.VISIBLE);
        rvProximasRutas.setVisibility(View.GONE);
    }

    // ✅ MÉTODO AUXILIAR: Registrar eventos analíticos usando MyApp
    private void registrarEventoAnalitico(String evento, Integer cantidad, Integer confirmadas) {
        try {
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("conductor_id", MyApp.getCurrentUserId());
            params.put("pantalla", "InicioConductor");
            params.put("timestamp", System.currentTimeMillis());

            if (cantidad != null) {
                params.put("cantidad", cantidad);
            }
            if (confirmadas != null) {
                params.put("confirmadas", confirmadas);
            }

            MyApp.logEvent(evento, params);
            Log.d(TAG, "📊 Evento analítico registrado: " + evento);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando evento analítico: " + e.getMessage());
        }
    }

    // ✅ MÉTODO AUXILIAR: Registrar errores usando MyApp
    private void registrarErrorAnalitico(String tipoError, String mensaje) {
        try {
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("conductor_id", MyApp.getCurrentUserId());
            params.put("pantalla", "InicioConductor");
            params.put("tipo_error", tipoError);
            params.put("mensaje_error", mensaje);
            params.put("timestamp", System.currentTimeMillis());

            MyApp.logEvent("error_pantalla", params);
            Log.d(TAG, "📊 Error registrado en análisis: " + tipoError);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando error analítico: " + e.getMessage());
        }
    }

    // ✅ MÉTODO AUXILIAR: Registrar acción sobre reserva usando MyApp
    private void registrarAccionReserva(Reserva reserva, String accion) {
        try {
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("conductor_id", MyApp.getCurrentUserId());
            params.put("reserva_id", reserva.getIdReserva());
            params.put("pasajero_id", reserva.getUsuarioId());
            params.put("pasajero_nombre", reserva.getNombre());
            params.put("accion", accion);
            params.put("ruta", reserva.getOrigen() + " → " + reserva.getDestino());
            params.put("asiento", reserva.getPuestoReservado());
            params.put("precio", reserva.getPrecio());
            params.put("timestamp", System.currentTimeMillis());

            MyApp.logEvent("accion_reserva_conductor", params);
            Log.d(TAG, "📊 Acción de reserva registrada: " + accion);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando acción de reserva: " + e.getMessage());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "📱 onStart - Actividad visible");

        // ✅ Registrar evento de inicio usando MyApp
        registrarEventoAnalitico("pantalla_inicio_conductor_start", null, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "📱 onResume - Actividad en primer plano");

        // ✅ Registrar evento de resumen usando MyApp
        registrarEventoAnalitico("pantalla_inicio_conductor_resume", null, null);

        // Recargar datos si es necesario
        recargarSiEsNecesario();
    }

    private void recargarSiEsNecesario() {
        // Verificar si los datos están cargados
        if (viewModel.getNombreConductorLiveData().getValue() != null) {
            Log.d(TAG, "🔄 Recargando datos en onResume");
            viewModel.reloadAllData();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "📱 onPause - Actividad en segundo plano");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "📱 onStop - Actividad no visible");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "📱 onDestroy - Actividad destruida");

        // Limpiar ViewModel si es necesario
        viewModel.clearData();
    }
}