package com.chopcode.trasnportenataga_laplata.activities.driver;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InicioConductor extends AppCompatActivity {
    // Views
    private RecyclerView rvReservas, rvProximasRutas;
    private TextView tvConductor, tvPlacaVehiculo;
    private TextView tvEmptyReservas, tvEmptyRutas, tvReservasConfirmadas, tvAsientosDisponibles;
    private TextView tvTotalIngresos, tvInfoCapacidad, tvInfoIngresos;

    // ✅ NUEVAS VISTAS MEJORADAS
    private TextView tvSubtituloEstadisticas, tvTendenciaIngresos;
    private TextView tvInfoReservas, tvUltimaActualizacion;
    private TextView tvNombreRutaHeader1, tvNombreRutaHeader2;
    private TextView tvContadorReservas, tvContadorRutas;
    private TextView tvSubtituloReservas, tvSubtituloRutas;

    // ✅ TextViews para estadísticas por ruta
    private TextView tvNombreRutaReservas, tvReservasRuta, tvNombreRutaAsientos, tvAsientosRuta;
    private TextView tvNombreRutaReservas2, tvReservasRuta2, tvNombreRutaAsientos2, tvAsientosRuta2;

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
    private SimpleDateFormat timeFormat;

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

        // Inicializar formatos de fecha/hora
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        initializeViews();
        setupRecyclerView();
        setupButtons();
        setupObservers();

        loadDriverData();
    }

    private void initializeViews() {
        Log.d(TAG, "🔧 Inicializando vistas...");

        // Vistas básicas
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

        // ✅ NUEVAS VISTAS MEJORADAS
        tvSubtituloEstadisticas = findViewById(R.id.tvSubtituloEstadisticas);
        tvTendenciaIngresos = findViewById(R.id.tvTendenciaIngresos);
        tvInfoReservas = findViewById(R.id.tvInfoReservas);
        tvUltimaActualizacion = findViewById(R.id.tvUltimaActualizacion);
        tvNombreRutaHeader1 = findViewById(R.id.tvNombreRutaHeader1);
        tvNombreRutaHeader2 = findViewById(R.id.tvNombreRutaHeader2);
        tvContadorReservas = findViewById(R.id.tvContadorReservas);
        tvContadorRutas = findViewById(R.id.tvContadorRutas);
        tvSubtituloReservas = findViewById(R.id.tvSubtituloReservas);
        tvSubtituloRutas = findViewById(R.id.tvSubtituloRutas);

        // ✅ TextViews para estadísticas por ruta
        tvNombreRutaReservas = findViewById(R.id.tvNombreRutaReservas);
        tvReservasRuta = findViewById(R.id.tvReservasRuta);
        tvNombreRutaAsientos = findViewById(R.id.tvNombreRutaAsientos);
        tvAsientosRuta = findViewById(R.id.tvAsientosRuta);

        // ✅ Intentar inicializar TextViews para segunda ruta
        try {
            tvNombreRutaReservas2 = findViewById(R.id.tvNombreRutaReservas2);
            tvReservasRuta2 = findViewById(R.id.tvReservasRuta2);
            tvNombreRutaAsientos2 = findViewById(R.id.tvNombreRutaAsientos2);
            tvAsientosRuta2 = findViewById(R.id.tvAsientosRuta2);
        } catch (Exception e) {
            Log.d(TAG, "ℹ️ No se encontraron TextViews para segunda ruta: " + e.getMessage());
        }

        btnPerfilConductor = findViewById(R.id.btnPerfilConductor);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        rvReservas = findViewById(R.id.recyclerReservas);
        rvProximasRutas = findViewById(R.id.recyclerProximasRutas);

        // ✅ Configurar textos informativos mejorados
        if (tvInfoCapacidad != null) {
            tvInfoCapacidad.setText("De " + CAPACIDAD_TOTAL + " totales • Ocupación: 0%");
        }
        if (tvInfoIngresos != null) {
            tvInfoIngresos.setText("Acumulado desde el inicio del día");
        }
        if (tvInfoReservas != null) {
            tvInfoReservas.setText("Total del día • Actualizado ahora");
        }
        if (tvUltimaActualizacion != null) {
            String currentTime = timeFormat.format(new Date());
            tvUltimaActualizacion.setText("Última actualización: " + currentTime);
        }

        // ✅ Configurar valores por defecto para estadísticas por ruta
        if (tvNombreRutaReservas != null) {
            tvNombreRutaReservas.setText("Cargando...");
        }
        if (tvNombreRutaAsientos != null) {
            tvNombreRutaAsientos.setText("Cargando...");
        }
        if (tvReservasRuta != null) {
            tvReservasRuta.setText("0");
        }
        if (tvAsientosRuta != null) {
            tvAsientosRuta.setText("0");
        }

        // ✅ Configurar encabezados de rutas por defecto
        if (tvNombreRutaHeader1 != null) {
            tvNombreRutaHeader1.setText("Ruta de Ida");
        }
        if (tvNombreRutaHeader2 != null) {
            tvNombreRutaHeader2.setText("Ruta de Regreso");
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

                // ✅ Actualizar contador de reservas
                if (tvContadorReservas != null) {
                    tvContadorReservas.setText(String.valueOf(reservas.size()));
                }

                // ✅ Registrar evento analítico
                registrarEventoAnalitico("reservas_actualizadas", reservas.size(), null);

                // ✅ Actualizar tiempo de actualización
                actualizarTiempoActualizacion();
            } else {
                if (tvContadorReservas != null) {
                    tvContadorReservas.setText("0");
                }
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

                // ✅ Actualizar contador de rutas
                if (tvContadorRutas != null) {
                    tvContadorRutas.setText(String.valueOf(rutas.size()));
                }

                // ✅ Registrar evento analítico
                registrarEventoAnalitico("rutas_actualizadas", rutas.size(), null);

                // ✅ Actualizar información de rutas
                actualizarInformacionRutas(rutas);

                // ✅ Actualizar tiempo de actualización
                actualizarTiempoActualizacion();

                // Mostrar información de la próxima ruta
                if (!rutas.isEmpty()) {
                    Ruta proximaRuta = rutas.get(0);
                    String horario = proximaRuta.getHora() != null ?
                            proximaRuta.getHora().getHora() : "--:--";
                    Log.d(TAG, "✅ Ruta próxima: " + proximaRuta.getOrigen() +
                            " → " + proximaRuta.getDestino() + " (" + horario + ")");
                }
            } else {
                if (tvContadorRutas != null) {
                    tvContadorRutas.setText("0");
                }
            }
            updateRoutesUI();
        });

        // Observar estadísticas generales
        viewModel.getReservasConfirmadasLiveData().observe(this, count -> {
            if (count != null) {
                tvReservasConfirmadas.setText(String.valueOf(count));
                Log.d(TAG, "📊 Reservas confirmadas: " + count);

                // ✅ Actualizar información de capacidad
                actualizarInformacionCapacidad();
            }
        });

        viewModel.getAsientosDisponiblesLiveData().observe(this, asientos -> {
            if (asientos != null) {
                tvAsientosDisponibles.setText(String.valueOf(asientos));
                Log.d(TAG, "📊 Asientos disponibles: " + asientos);

                // ✅ Actualizar información de capacidad
                actualizarInformacionCapacidad();
            }
        });

        viewModel.getIngresosLiveData().observe(this, ingresos -> {
            if (ingresos != null) {
                tvTotalIngresos.setText(formatCurrency(ingresos));
                Log.d(TAG, "💰 Ingresos actualizados: $" + ingresos);

                // ✅ Actualizar tiempo de actualización
                actualizarTiempoActualizacion();
            }
        });

        // ✅ NUEVO: Observar estadísticas de la primera ruta
        viewModel.getNombreRuta1LiveData().observe(this, nombreRuta -> {
            if (nombreRuta != null && !nombreRuta.isEmpty()) {
                tvNombreRutaReservas.setText(nombreRuta);
                tvNombreRutaAsientos.setText(nombreRuta);
                if (tvNombreRutaHeader1 != null) {
                    tvNombreRutaHeader1.setText("Ruta: " + nombreRuta);
                }
                Log.d(TAG, "📊 Nombre Ruta 1 actualizado: " + nombreRuta);
            }
        });

        viewModel.getReservasRuta1LiveData().observe(this, reservas -> {
            if (reservas != null) {
                tvReservasRuta.setText(String.valueOf(reservas));
                Log.d(TAG, "📊 Reservas Ruta 1: " + reservas);

                // ✅ Actualizar tiempo de actualización
                actualizarTiempoActualizacion();
            }
        });

        viewModel.getAsientosRuta1LiveData().observe(this, asientos -> {
            if (asientos != null) {
                tvAsientosRuta.setText(String.valueOf(asientos));
                Log.d(TAG, "📊 Asientos Ruta 1: " + asientos);
            }
        });

        // ✅ NUEVO: Observar estadísticas de la segunda ruta (si existen las vistas)
        if (tvNombreRutaReservas2 != null && tvNombreRutaAsientos2 != null) {
            viewModel.getNombreRuta2LiveData().observe(this, nombreRuta -> {
                if (nombreRuta != null && !nombreRuta.isEmpty()) {
                    tvNombreRutaReservas2.setText(nombreRuta);
                    tvNombreRutaAsientos2.setText(nombreRuta);
                    if (tvNombreRutaHeader2 != null) {
                        tvNombreRutaHeader2.setText("Ruta: " + nombreRuta);
                    }
                    Log.d(TAG, "📊 Nombre Ruta 2 actualizado: " + nombreRuta);
                }
            });

            viewModel.getReservasRuta2LiveData().observe(this, reservas -> {
                if (reservas != null) {
                    tvReservasRuta2.setText(String.valueOf(reservas));
                    Log.d(TAG, "📊 Reservas Ruta 2: " + reservas);
                }
            });

            viewModel.getAsientosRuta2LiveData().observe(this, asientos -> {
                if (asientos != null) {
                    tvAsientosRuta2.setText(String.valueOf(asientos));
                    Log.d(TAG, "📊 Asientos Ruta 2: " + asientos);
                }
            });
        } else {
            Log.d(TAG, "ℹ️ No se configuraron observadores para segunda ruta - vistas no encontradas");
        }

        // Observar estado de carga
        viewModel.getLoadingLiveData().observe(this, isLoading -> {
            if (isLoading != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                Log.d(TAG, isLoading ? "⏳ Cargando datos..." : "✅ Carga completada");

                if (!isLoading) {
                    // ✅ Actualizar tiempo de actualización cuando termina la carga
                    actualizarTiempoActualizacion();
                }
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

        // ✅ NUEVO: Configurar click en encabezado de estadísticas para actualizar
        View headerEstadisticas = findViewById(R.id.tvTituloEstadisticas);
        if (headerEstadisticas != null) {
            headerEstadisticas.setOnClickListener(view -> {
                Log.d(TAG, "🔄 Actualizando estadísticas manualmente");

                // ✅ Registrar evento de actualización
                registrarEventoAnalitico("actualizar_estadisticas_manual", null, null);

                // Mostrar mensaje de actualización
                Toast.makeText(this, "Actualizando estadísticas...", Toast.LENGTH_SHORT).show();

                // Recargar datos
                viewModel.reloadAllData();
            });
        }

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

                    // ✅ Actualizar tiempo de actualización después de la acción
                    actualizarTiempoActualizacion();
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

        // ✅ Controlar visibilidad de la segunda ruta
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

    // ✅ NUEVO: Método para actualizar tiempo de actualización
    private void actualizarTiempoActualizacion() {
        if (tvUltimaActualizacion != null) {
            String currentTime = timeFormat.format(new Date());
            tvUltimaActualizacion.setText("Última actualización: " + currentTime);
            Log.d(TAG, "🕐 Tiempo de actualización actualizado: " + currentTime);
        }
    }

    // ✅ NUEVO: Método para actualizar información de capacidad
    private void actualizarInformacionCapacidad() {
        if (tvInfoCapacidad != null && tvAsientosDisponibles != null) {
            try {
                int disponibles = Integer.parseInt(tvAsientosDisponibles.getText().toString());
                int ocupados = CAPACIDAD_TOTAL - disponibles;
                int porcentajeOcupacion = (ocupados * 100) / CAPACIDAD_TOTAL;

                String info = String.format(Locale.getDefault(),
                        "De %d totales • Ocupación: %d%%",
                        CAPACIDAD_TOTAL, porcentajeOcupacion);
                tvInfoCapacidad.setText(info);

                Log.d(TAG, "📊 Información de capacidad actualizada: " + info);
            } catch (NumberFormatException e) {
                Log.e(TAG, "❌ Error al calcular porcentaje de ocupación: " + e.getMessage());
            }
        }
    }

    // ✅ NUEVO: Método para actualizar información de rutas
    private void actualizarInformacionRutas(List<Ruta> rutas) {
        if (rutas != null && !rutas.isEmpty()) {
            // Actualizar hora próxima para cada ruta si está disponible
            for (int i = 0; i < Math.min(rutas.size(), 2); i++) {
                Ruta ruta = rutas.get(i);
                String horario = ruta.getHora() != null ?
                        ruta.getHora().getHora() : "--:--";

                if (i == 0) {
                    // Ruta 1 - Buscar el TextView dentro del encabezado de la primera ruta
                    View headerRuta1 = findViewById(R.id.tvNombreRutaHeader1);
                    if (headerRuta1 != null && headerRuta1.getParent() instanceof ViewGroup) {
                        ViewGroup parent = (ViewGroup) headerRuta1.getParent();
                        // Buscar el TextView que muestra la hora próxima
                        for (int j = 0; j < parent.getChildCount(); j++) {
                            View child = parent.getChildAt(j);
                            if (child instanceof TextView) {
                                TextView textView = (TextView) child;
                                String currentText = textView.getText().toString();
                                // Verificar si es el TextView de hora por su contenido actual
                                if (currentText.contains("Próximo:")) {
                                    textView.setText("Próximo: " + horario);
                                    break;
                                }
                            }
                        }
                    }
                } else if (i == 1) {
                    // Ruta 2 - Buscar el TextView dentro del encabezado de la segunda ruta
                    View headerRuta2 = findViewById(R.id.tvNombreRutaHeader2);
                    if (headerRuta2 != null && headerRuta2.getParent() instanceof ViewGroup) {
                        ViewGroup parent = (ViewGroup) headerRuta2.getParent();
                        // Buscar el TextView que muestra la hora próxima
                        for (int j = 0; j < parent.getChildCount(); j++) {
                            View child = parent.getChildAt(j);
                            if (child instanceof TextView) {
                                TextView textView = (TextView) child;
                                String currentText = textView.getText().toString();
                                // Verificar si es el TextView de hora por su contenido actual
                                if (currentText.contains("Próximo:")) {
                                    textView.setText("Próximo: " + horario);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
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

        // ✅ Actualizar contadores
        if (tvContadorReservas != null) {
            tvContadorReservas.setText("0");
        }
        if (tvContadorRutas != null) {
            tvContadorRutas.setText("0");
        }

        // ✅ Actualizar tiempo de actualización
        actualizarTiempoActualizacion();

        // ✅ Actualizar información de capacidad
        actualizarInformacionCapacidad();

        // ✅ Datos por defecto para estadísticas por ruta
        if (tvNombreRutaReservas != null) {
            tvNombreRutaReservas.setText("Sin ruta asignada");
        }
        if (tvNombreRutaAsientos != null) {
            tvNombreRutaAsientos.setText("Sin ruta asignada");
        }
        if (tvReservasRuta != null) {
            tvReservasRuta.setText("0");
        }
        if (tvAsientosRuta != null) {
            tvAsientosRuta.setText("0");
        }

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