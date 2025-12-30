// InicioConductor.java (Versión actualizada con strings)
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
import com.chopcode.trasnportenataga_laplata.managers.auths.AuthManager;
import com.chopcode.trasnportenataga_laplata.models.Reserva;
import com.chopcode.trasnportenataga_laplata.models.Ruta;
import com.chopcode.trasnportenataga_laplata.viewmodels.driver.DriverHomeViewModel;
import com.chopcode.trasnportenataga_laplata.viewmodels.driver.EstadisticasViewModel;
import com.chopcode.trasnportenataga_laplata.viewmodels.driver.ReservasViewModel;
import com.chopcode.trasnportenataga_laplata.viewmodels.driver.RutasViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InicioConductor extends AppCompatActivity {
    private static final String TAG = "InicioConductor";

    // Views principales
    private RecyclerView rvReservas, rvProximasRutas;
    private TextView tvConductor, tvPlacaVehiculo;
    private TextView tvEmptyReservas, tvEmptyRutas;
    private ProgressBar progressBar;
    private MaterialButton btnPerfilConductor, btnCerrarSesion;

    // Views de estadísticas
    private TextView tvReservasConfirmadas, tvAsientosDisponibles, tvTotalIngresos;
    private TextView tvInfoCapacidad, tvInfoIngresos, tvInfoReservas;
    private TextView tvUltimaActualizacion;

    // Views por ruta
    private TextView tvNombreRutaReservas, tvReservasRuta, tvNombreRutaAsientos, tvAsientosRuta;
    private TextView tvNombreRutaReservas2, tvReservasRuta2, tvNombreRutaAsientos2, tvAsientosRuta2;
    private TextView tvContadorReservas, tvContadorRutas;

    // ViewModels
    private DriverHomeViewModel viewModel;
    private ReservasViewModel reservasViewModel;
    private EstadisticasViewModel estadisticasViewModel;
    private RutasViewModel rutasViewModel;

    private AuthManager authManager;
    private ReservaAdapter reservaAdapter;
    private RutaAdapter rutaAdapter;
    private List<Reserva> listaReservas = new ArrayList<>();
    private List<Ruta> listaRutas = new ArrayList<>();
    private SimpleDateFormat timeFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio_conductor);
        Log.d(TAG, "🚀 Iniciando actividad con nueva arquitectura");

        authManager = AuthManager.getInstance();
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        // Inicializar ViewModels
        viewModel = new ViewModelProvider(this).get(DriverHomeViewModel.class);
        reservasViewModel = viewModel.getReservasViewModel();
        estadisticasViewModel = viewModel.getEstadisticasViewModel();
        rutasViewModel = viewModel.getRutasViewModel();

        // Inicializar contexto en ViewModels que lo necesiten
        viewModel.initialize(getApplicationContext());

        initializeViews();
        setupRecyclerView();
        setupButtons();
        setupObservers();

        loadDriverData();
    }

    private void initializeViews() {
        // Inicializar todas las vistas según tu XML
        tvConductor = findViewById(R.id.tvConductor);
        tvPlacaVehiculo = findViewById(R.id.tvPlacaVehiculo);
        tvReservasConfirmadas = findViewById(R.id.tvReservasConfirmadas);
        tvAsientosDisponibles = findViewById(R.id.tvAsientosDisponibles);
        tvTotalIngresos = findViewById(R.id.tvTotalIngresos);
        tvInfoCapacidad = findViewById(R.id.tvInfoCapacidad);
        tvInfoIngresos = findViewById(R.id.tvInfoIngresos);
        tvInfoReservas = findViewById(R.id.tvInfoReservas);
        tvUltimaActualizacion = findViewById(R.id.tvUltimaActualizacion);

        tvNombreRutaReservas = findViewById(R.id.tvNombreRutaReservas);
        tvReservasRuta = findViewById(R.id.tvReservasRuta);
        tvNombreRutaAsientos = findViewById(R.id.tvNombreRutaAsientos);
        tvAsientosRuta = findViewById(R.id.tvAsientosRuta);

        tvNombreRutaReservas2 = findViewById(R.id.tvNombreRutaReservas2);
        tvReservasRuta2 = findViewById(R.id.tvReservasRuta2);
        tvNombreRutaAsientos2 = findViewById(R.id.tvNombreRutaAsientos2);
        tvAsientosRuta2 = findViewById(R.id.tvAsientosRuta2);

        tvContadorReservas = findViewById(R.id.tvContadorReservas);
        tvContadorRutas = findViewById(R.id.tvContadorRutas);

        progressBar = findViewById(R.id.progressBar);
        rvReservas = findViewById(R.id.recyclerReservas);
        rvProximasRutas = findViewById(R.id.recyclerProximasRutas);
        tvEmptyReservas = findViewById(R.id.tvEmptyReservas);
        tvEmptyRutas = findViewById(R.id.tvEmptyRutas);

        // Botones
        btnPerfilConductor = findViewById(R.id.btnPerfilConductor);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

        // Configurar valores iniciales usando strings
        tvReservasConfirmadas.setText(getString(R.string.contador_reservas, 0));
        tvAsientosDisponibles.setText("28"); // Valor por defecto
        tvTotalIngresos.setText(getString(R.string.formato_moneda, "0"));
        tvContadorReservas.setText(getString(R.string.contador_reservas, 0));
        tvContadorRutas.setText(getString(R.string.contador_rutas, 0));

        // Configurar información por defecto usando strings
        if (tvInfoCapacidad != null) {
            tvInfoCapacidad.setText(getString(R.string.ocupacion_porcentaje, 28, 0));
        }
        if (tvInfoIngresos != null) {
            tvInfoIngresos.setText(getString(R.string.acumulado_desde_inicio));
        }
        if (tvInfoReservas != null) {
            tvInfoReservas.setText(getString(R.string.total_del_dia));
        }

        // Actualizar tiempo de actualización
        actualizarTiempoActualizacion();
    }

    private void setupButtons() {
        Log.d(TAG, "🔧 Configurando botones...");

        // Botón de cerrar sesión
        btnCerrarSesion.setOnClickListener(view -> {
            Log.d(TAG, "🚪 Cerrando sesión de conductor...");

            // Mostrar diálogo de confirmación usando strings
            new MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.cerrar_sesion))
                    .setMessage(getString(R.string.confirmar_cerrar_sesion))
                    .setPositiveButton(getString(R.string.confirmar), (dialog, which) -> {
                        authManager.signOut(this);
                        Toast.makeText(this, getString(R.string.sesion_cerrada_exito), Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton(getString(R.string.cancelar), null)
                    .show();
        });

        // Botón de perfil del conductor
        btnPerfilConductor.setOnClickListener(view -> {
            Log.d(TAG, "👤 Navegando a perfil de conductor");
            goToDriverProfile();
        });

        // Configurar click en encabezado de estadísticas para actualizar
        View headerEstadisticas = findViewById(R.id.tvTituloEstadisticas);
        if (headerEstadisticas != null) {
            headerEstadisticas.setOnClickListener(view -> {
                Log.d(TAG, "🔄 Actualizando estadísticas manualmente");
                Toast.makeText(this, getString(R.string.actualizando_datos), Toast.LENGTH_SHORT).show();
                viewModel.reloadAllData();
            });
        }

        Log.d(TAG, "✅ Botones configurados");
    }

    private void setupObservers() {
        Log.d(TAG, "👀 Configurando observadores...");

        // Observar datos del conductor (ViewModel principal)
        viewModel.getNombreConductorLiveData().observe(this, nombre -> {
            if (nombre != null) {
                tvConductor.setText(nombre);
                Log.d(TAG, "✅ Nombre del conductor actualizado: " + nombre);
            } else {
                tvConductor.setText(getString(R.string.no_disponible));
            }
        });

        viewModel.getPlacaVehiculoLiveData().observe(this, placa -> {
            if (placa != null) {
                tvPlacaVehiculo.setText(getString(R.string.placaVehiculo, placa));
                Log.d(TAG, "✅ Placa del vehículo actualizada: " + placa);
            } else {
                tvPlacaVehiculo.setText(getString(R.string.placaVehiculo, getString(R.string.no_disponible)));
            }
        });

        // Observar reservas
        reservasViewModel.getReservasLiveData().observe(this, reservas -> {
            Log.d(TAG, "🔄 Reservas actualizadas: " + (reservas != null ? reservas.size() : 0));

            if (reservas != null) {
                listaReservas.clear();
                listaReservas.addAll(reservas);
                reservaAdapter.actualizarReservas(new ArrayList<>(reservas));
                updateReservationsUI();

                // Actualizar contador
                tvContadorReservas.setText(getString(R.string.contador_reservas, reservas.size()));
                Log.d(TAG, "✅ Contador de reservas: " + reservas.size());
            } else {
                tvContadorReservas.setText(getString(R.string.contador_reservas, 0));
            }
        });

        reservasViewModel.getContadorReservasLiveData().observe(this, contador -> {
            if (contador != null) {
                tvContadorReservas.setText(getString(R.string.contador_reservas, contador));
            }
        });

        // Observar estadísticas generales
        estadisticasViewModel.getReservasConfirmadasLiveData().observe(this, count -> {
            if (count != null) {
                tvReservasConfirmadas.setText(String.valueOf(count));
                Log.d(TAG, "📊 Reservas confirmadas: " + count);

                // Actualizar información de capacidad
                actualizarInformacionCapacidad(count);
            }
        });

        estadisticasViewModel.getAsientosDisponiblesLiveData().observe(this, asientos -> {
            if (asientos != null) {
                tvAsientosDisponibles.setText(String.valueOf(asientos));
                Log.d(TAG, "📊 Asientos disponibles: " + asientos);

                // Actualizar información de capacidad
                actualizarInformacionCapacidad(null);
            }
        });

        estadisticasViewModel.getIngresosLiveData().observe(this, ingresos -> {
            if (ingresos != null) {
                tvTotalIngresos.setText(formatCurrency(ingresos));
                Log.d(TAG, "💰 Ingresos: " + formatCurrency(ingresos));

                // Actualizar tiempo de actualización
                actualizarTiempoActualizacion();
            }
        });

        // Observar estadísticas por ruta 1
        estadisticasViewModel.getReservasRuta1LiveData().observe(this, count -> {
            if (count != null) {
                tvReservasRuta.setText(String.valueOf(count));
                Log.d(TAG, "📊 Reservas Ruta 1: " + count);
            }
        });

        estadisticasViewModel.getAsientosRuta1LiveData().observe(this, asientos -> {
            if (asientos != null) {
                tvAsientosRuta.setText(String.valueOf(asientos));
                Log.d(TAG, "📊 Asientos Ruta 1: " + asientos);
            }
        });

        estadisticasViewModel.getNombreRuta1LiveData().observe(this, nombre -> {
            if (nombre != null) {
                tvNombreRutaReservas.setText(nombre);
                tvNombreRutaAsientos.setText(nombre);
                Log.d(TAG, "📊 Nombre Ruta 1: " + nombre);
            }
        });

        // Observar estadísticas por ruta 2 (si existen las vistas)
        try {
            estadisticasViewModel.getReservasRuta2LiveData().observe(this, count -> {
                if (count != null && tvReservasRuta2 != null) {
                    tvReservasRuta2.setText(String.valueOf(count));
                    Log.d(TAG, "📊 Reservas Ruta 2: " + count);
                }
            });

            estadisticasViewModel.getAsientosRuta2LiveData().observe(this, asientos -> {
                if (asientos != null && tvAsientosRuta2 != null) {
                    tvAsientosRuta2.setText(String.valueOf(asientos));
                    Log.d(TAG, "📊 Asientos Ruta 2: " + asientos);
                }
            });

            estadisticasViewModel.getNombreRuta2LiveData().observe(this, nombre -> {
                if (nombre != null && tvNombreRutaReservas2 != null && tvNombreRutaAsientos2 != null) {
                    tvNombreRutaReservas2.setText(nombre);
                    tvNombreRutaAsientos2.setText(nombre);
                    Log.d(TAG, "📊 Nombre Ruta 2: " + nombre);
                }
            });
        } catch (Exception e) {
            Log.d(TAG, "ℹ️ No se configuraron observadores para segunda ruta: " + e.getMessage());
        }

        // Observar rutas
        rutasViewModel.getRutasLiveData().observe(this, rutas -> {
            Log.d(TAG, "🔄 Rutas actualizadas: " + (rutas != null ? rutas.size() : 0));

            if (rutas != null) {
                listaRutas.clear();
                listaRutas.addAll(rutas);
                rutaAdapter.notifyDataSetChanged();
                updateRoutesUI();

                // Actualizar contador
                tvContadorRutas.setText(getString(R.string.contador_rutas, rutas.size()));
                Log.d(TAG, "✅ Contador de rutas: " + rutas.size());

                // Actualizar estadísticas por ruta cuando se cargan nuevas rutas
                if (!rutas.isEmpty() && !listaReservas.isEmpty()) {
                    estadisticasViewModel.calculateRouteStatistics(rutas, listaReservas);
                }

                // Actualizar tiempo de actualización
                actualizarTiempoActualizacion();
            } else {
                tvContadorRutas.setText(getString(R.string.contador_rutas, 0));
            }
        });

        rutasViewModel.getContadorRutasLiveData().observe(this, contador -> {
            if (contador != null) {
                tvContadorRutas.setText(getString(R.string.contador_rutas, contador));
            }
        });

        // Observar estado de carga del ViewModel principal
        viewModel.getLoadingLiveData().observe(this, isLoading -> {
            if (isLoading != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                Log.d(TAG, isLoading ? "⏳ Cargando datos..." : "✅ Carga completada");
            }
        });

        // Observar errores del ViewModel principal
        viewModel.getErrorLiveData().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Log.e(TAG, "❌ Error observado: " + error);
                Toast.makeText(InicioConductor.this,
                        getString(R.string.error_carga_estadisticas), Toast.LENGTH_SHORT).show();
            }
        });

        // Observar cuando una reserva es procesada
        reservasViewModel.getReservaProcesadaLiveData().observe(this, procesada -> {
            if (procesada != null && procesada) {
                Log.d(TAG, "✅ Reserva procesada exitosamente");
                Toast.makeText(this, getString(R.string.reserva_procesada_exito), Toast.LENGTH_SHORT).show();
                // Actualizar estadísticas después de procesar una reserva
                if (viewModel.getNombreConductorLiveData().getValue() != null) {
                    estadisticasViewModel.calculateStatistics(viewModel.getNombreConductorLiveData().getValue());
                }
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

    private void showDefaultData() {
        Log.d(TAG, "ℹ️ Mostrando datos por defecto");

        showEmptyReservations();
        showEmptyRoutes();
        tvConductor.setText(getString(R.string.no_disponible));
        tvPlacaVehiculo.setText(getString(R.string.placaVehiculo, getString(R.string.no_disponible)));

        // Valores por defecto usando strings
        tvReservasConfirmadas.setText(getString(R.string.contador_reservas, 0));
        tvAsientosDisponibles.setText("28"); // Valor por defecto
        tvTotalIngresos.setText(getString(R.string.formato_moneda, "0"));
        tvContadorReservas.setText(getString(R.string.contador_reservas, 0));
        tvContadorRutas.setText(getString(R.string.contador_rutas, 0));

        // Datos por defecto para cada ruta individual
        if (tvReservasRuta != null) tvReservasRuta.setText(getString(R.string.contador_reservas, 0));
        if (tvAsientosRuta != null) tvAsientosRuta.setText("14"); // Valor por defecto
        if (tvReservasRuta2 != null) tvReservasRuta2.setText(getString(R.string.contador_reservas, 0));
        if (tvAsientosRuta2 != null) tvAsientosRuta2.setText("14"); // Valor por defecto

        // Actualizar tiempo de actualización
        actualizarTiempoActualizacion();

        // Actualizar información de capacidad
        actualizarInformacionCapacidad(0);

        // Datos por defecto para estadísticas por ruta
        if (tvNombreRutaReservas != null) tvNombreRutaReservas.setText(getString(R.string.nataga_laplata));
        if (tvNombreRutaAsientos != null) tvNombreRutaAsientos.setText(getString(R.string.nataga_laplata));
        if (tvNombreRutaReservas2 != null) tvNombreRutaReservas2.setText(getString(R.string.laplata_nataga));
        if (tvNombreRutaAsientos2 != null) tvNombreRutaAsientos2.setText(getString(R.string.laplata_nataga));

        Log.d(TAG, "✅ Datos por defecto mostrados");
    }

    private void showConfirmationDialog(Reserva reserva, boolean isConfirmation) {
        Log.d(TAG, "💬 Mostrando diálogo de " + (isConfirmation ? "confirmación" : "cancelación"));

        new MaterialAlertDialogBuilder(this)
                .setTitle(isConfirmation ?
                        getString(R.string.confirmar_reserva) :
                        getString(R.string.cancelar_reserva))
                .setMessage(isConfirmation ?
                        String.format(getString(R.string.confirmar_reserva_mensaje), reserva.getNombre()) :
                        String.format(getString(R.string.cancelar_reserva_mensaje), reserva.getNombre()))
                .setPositiveButton(isConfirmation ?
                        getString(R.string.confirmar) :
                        getString(R.string.cancelar), (dialog, which) -> {
                    Log.d(TAG, "✅ Usuario confirmó " + (isConfirmation ? "confirmación" : "cancelación"));

                    if (isConfirmation) {
                        reservasViewModel.confirmReservation(reserva);
                    } else {
                        reservasViewModel.cancelReservation(reserva);
                    }

                    // Actualizar tiempo de actualización
                    actualizarTiempoActualizacion();
                })
                .setNegativeButton(getString(R.string.volver), (dialog, which) -> {
                    Log.d(TAG, "❌ Usuario canceló la acción");
                    dialog.dismiss();
                })
                .show();
    }

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

        // Controlar visibilidad de la segunda ruta
        View layoutRuta2 = findViewById(R.id.layoutRuta2);
        boolean haySegundaRuta = hayRutas && listaRutas.size() >= 2;

        if (layoutRuta2 != null) {
            layoutRuta2.setVisibility(haySegundaRuta ? View.VISIBLE : View.GONE);
            Log.d(TAG, "✅ Visibilidad de segunda ruta: " +
                    (haySegundaRuta ? "VISIBLE" : "GONE"));
        }

        Log.d(TAG, "✅ UI de rutas actualizada - " +
                (hayRutas ? "Mostrando " + listaRutas.size() + " rutas" : "Sin rutas"));
    }

    private void actualizarTiempoActualizacion() {
        if (tvUltimaActualizacion != null) {
            String currentTime = timeFormat.format(new Date());
            tvUltimaActualizacion.setText(String.format(getString(R.string.ultima_actualizacion), currentTime));
            Log.d(TAG, "🕐 Tiempo de actualización: " + currentTime);
        }
    }

    private void actualizarInformacionCapacidad(Integer reservasConfirmadas) {
        if (tvInfoCapacidad != null && tvAsientosDisponibles != null) {
            try {
                // Leer el valor actual de asientos disponibles
                int disponibles = Integer.parseInt(tvAsientosDisponibles.getText().toString());

                // Si se pasa reservasConfirmadas, actualizar ocupados
                int ocupados = reservasConfirmadas != null ? reservasConfirmadas :
                        (28 - disponibles); // Total fijo de 28 asientos

                final int CAPACIDAD_TOTAL = 28;
                int porcentajeOcupacion = ocupados > 0 ? (ocupados * 100) / CAPACIDAD_TOTAL : 0;

                String info = getString(R.string.ocupacion_porcentaje_detallada,
                        CAPACIDAD_TOTAL, disponibles, ocupados, porcentajeOcupacion);
                tvInfoCapacidad.setText(info);

                Log.d(TAG, "📊 Información de capacidad: " + info);
            } catch (NumberFormatException e) {
                Log.e(TAG, "❌ Error al calcular información de capacidad: " + e.getMessage());
                tvInfoCapacidad.setText(getString(R.string.ocupacion_porcentaje, 28, 0));
            }
        }
    }

    private void goToDriverProfile() {
        Log.d(TAG, "👤 Navegando a perfil de conductor");

        if (authManager.isUserLoggedIn()) {
            startActivity(new Intent(this, PerfilConductor.class));
            Log.d(TAG, "✅ Intent iniciado para PerfilConductor");
        } else {
            Log.w(TAG, "⚠️ Usuario no logeado - no se puede navegar al perfil");
            Toast.makeText(this, getString(R.string.debe_iniciar_sesion), Toast.LENGTH_SHORT).show();
        }
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

    private String formatCurrency(double amount) {
        // Usar el string de formato directamente
        return getString(R.string.formato_moneda, String.format(Locale.getDefault(), "%.0f", amount));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "📱 onResume - Actividad en primer plano");

        // Recargar datos si es necesario
        if (viewModel.getNombreConductorLiveData().getValue() != null) {
            Log.d(TAG, "🔄 Recargando datos en onResume");
            viewModel.reloadAllData();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "📱 onDestroy - Actividad destruida");

        // Limpiar ViewModel
        viewModel.clearAllData();
    }
}