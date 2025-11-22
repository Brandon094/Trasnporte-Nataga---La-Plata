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
import com.chopcode.trasnportenataga_laplata.managers.NotificationManager;
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
    private NotificationManager notificationManager;

    // ✅ NUEVO: Tag para logs
    private static final String TAG = "InicioConductor";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "🚀 onCreate - Iniciando actividad de conductor");

        try {
            setContentView(R.layout.activity_inicio_conductor);
            Log.d(TAG, "✅ Layout inflado correctamente");

            // Inicializar services
            authManager = AuthManager.getInstance();
            userService = new UserService();
            reservaService = new ReservaService();
            statisticsManager = new StatisticsManager();
            notificationManager = NotificationManager.getInstance();
            Log.d(TAG, "✅ Servicios inicializados");

            if (!authManager.validateLogin(this)) {
                Log.w(TAG, "⚠️ Login no válido - finalizando actividad");
                finish();
                return;
            }
            Log.d(TAG, "✅ Login validado");

            initializeViews();
            setupRecyclerView();
            setupButtons();
            loadDriverData();

            Log.d(TAG, "✅ Configuración completa - Actividad lista");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error crítico en onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error al iniciar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeViews() {
        Log.d(TAG, "🔧 Inicializando vistas...");

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

        Log.d(TAG, "✅ Todas las vistas inicializadas");
        updateStatisticsUI();
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
        rvProximasRutas.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvProximasRutas.setAdapter(rutaAdapter);
        Log.d(TAG, "✅ RecyclerView de rutas configurado");
    }

    private void setupButtons() {
        Log.d(TAG, "🔧 Configurando botones...");

        btnCerrarSesion.setOnClickListener(view -> {
            Log.d(TAG, "🚪 Cerrando sesión de conductor...");
            authManager.signOut(this);
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
        });

        btnPerfilConductor.setOnClickListener(view -> {
            Log.d(TAG, "👤 Navegando a perfil de conductor");
            goToDriverProfile();
        });

        Log.d(TAG, "✅ Botones configurados");
    }

    private void loadDriverData() {
        Log.d(TAG, "🔧 Cargando datos del conductor...");

        String userId = authManager.getUserId();
        if (userId == null) {
            Log.w(TAG, "⚠️ UserId es null - mostrando datos por defecto");
            showDefaultData();
            return;
        }

        Log.d(TAG, "👤 UserId del conductor: " + userId);
        userService.loadDriverData(userId, new UserService.DriverDataCallback() {
            @Override
            public void onDriverDataLoaded(String nombre, String telefono, String placa, List<String> horarios) {
                Log.d(TAG, "✅ Datos del conductor cargados:");
                Log.d(TAG, "   - Nombre: " + nombre);
                Log.d(TAG, "   - Teléfono: " + telefono);
                Log.d(TAG, "   - Placa: " + placa);
                Log.d(TAG, "   - Horarios asignados: " + (horarios != null ? horarios.size() : 0));

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
                Log.e(TAG, "❌ Error cargando datos conductor: " + error);
                showDefaultData();
            }
        });
    }

    private void calculateStatistics(String conductorNombre) {
        Log.d(TAG, "📊 Calculando estadísticas para: " + conductorNombre);

        statisticsManager.calculateDailyStatistics(conductorNombre, new UserService.StatisticsCallback() {
            @Override
            public void onStatisticsCalculated(int reservasConfirmadas, int asientosDisp, double ingresos) {
                Log.d(TAG, "✅ Estadísticas calculadas:");
                Log.d(TAG, "   - Reservas confirmadas: " + reservasConfirmadas);
                Log.d(TAG, "   - Asientos disponibles: " + asientosDisp);
                Log.d(TAG, "   - Ingresos: $" + ingresos);

                runOnUiThread(() -> {
                    reservasConfirmadasHoy = reservasConfirmadas;
                    asientosDisponibles = asientosDisp;
                    totalIngresos = ingresos;
                    updateStatisticsUI();
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error calculando estadísticas: " + error);
            }
        });
    }

    private void loadReservations(String conductorNombre) {
        Log.d(TAG, "🔍 Cargando reservas para conductor: " + conductorNombre);

        reservaService.cargarReservasConductor(conductorNombre, horariosAsignados,
                new ReservaService.DriverReservationsCallback() {
                    @Override
                    public void onDriverReservationsLoaded(List<Reserva> reservas) {
                        Log.d(TAG, "✅ Reservas cargadas: " + reservas.size() + " reservas encontradas");

                        runOnUiThread(() -> {
                            listaReservas.clear();
                            listaReservas.addAll(reservas);
                            reservaAdapter.actualizarReservas(new ArrayList<>(listaReservas));
                            updateReservationsUI();

                            // Log detallado de reservas
                            for (Reserva reserva : reservas) {
                                Log.d(TAG, "   - Reserva: " + reserva.getIdReserva() +
                                        " | " + reserva.getNombre() +
                                        " | Asiento: " + reserva.getPuestoReservado() +
                                        " | Estado: " + reserva.getEstadoReserva());
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ Error cargando reservas: " + error);
                        showEmptyReservations();
                    }
                });
    }

    private void loadRoutes() {
        Log.d(TAG, "🗺️ Cargando rutas asignadas...");

        userService.loadAssignedRoutes(horariosAsignados, new UserService.RoutesCallback() {
            @Override
            public void onRoutesLoaded(List<Ruta> rutas) {
                Log.d(TAG, "✅ Rutas cargadas: " + rutas.size() + " rutas encontradas");

                runOnUiThread(() -> {
                    listaRutas.clear();
                    listaRutas.addAll(rutas);
                    rutaAdapter.notifyDataSetChanged();
                    updateRoutesUI();
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error cargando rutas: " + error);
                showEmptyRoutes();
            }
        });
    }

    private void updateStatisticsUI() {
        Log.d(TAG, "🔄 Actualizando UI de estadísticas");

        tvReservasConfirmadas.setText(String.valueOf(reservasConfirmadasHoy));
        tvAsientosDisponibles.setText(String.valueOf(asientosDisponibles));
        tvInfoCapacidad.setText("Capacidad total: " + CAPACIDAD_TOTAL + " asientos");
        tvTotalIngresos.setText(getString(R.string.totalIngresos, totalIngresos));

        Log.d(TAG, "✅ UI de estadísticas actualizada");
    }

    private void updateReservationsUI() {
        Log.d(TAG, "🔄 Actualizando UI de reservas - Total: " + listaReservas.size());

        tvReservasActivas.setText(getString(R.string.reservasPendientes, listaReservas.size()));
        tvEmptyReservas.setVisibility(listaReservas.isEmpty() ? View.VISIBLE : View.GONE);
        rvReservas.setVisibility(listaReservas.isEmpty() ? View.GONE : View.VISIBLE);

        Log.d(TAG, "✅ UI de reservas actualizada");
    }

    private void updateRoutesUI() {
        Log.d(TAG, "🔄 Actualizando UI de rutas");

        if (!listaRutas.isEmpty()) {
            Ruta proximaRuta = listaRutas.get(0);
            String horario = proximaRuta.getHora() != null ? proximaRuta.getHora().getHora() : "--:--";
            tvProximaRuta.setText("Próxima ruta: " + proximaRuta.getOrigen() + " → " +
                    proximaRuta.getDestino() + " a las " + horario);
            Log.d(TAG, "✅ Ruta próxima: " + proximaRuta.getOrigen() + " → " + proximaRuta.getDestino());
        } else {
            tvProximaRuta.setText("Próxima: No hay rutas programadas");
            Log.d(TAG, "ℹ️ No hay rutas programadas");
        }

        tvEmptyRutas.setVisibility(listaRutas.isEmpty() ? View.VISIBLE : View.GONE);
        rvProximasRutas.setVisibility(listaRutas.isEmpty() ? View.GONE : View.VISIBLE);

        Log.d(TAG, "✅ UI de rutas actualizada");
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
                    if (isConfirmation) {
                        confirmReservation(reserva);
                    } else {
                        cancelReservation(reserva);
                    }
                })
                .setNegativeButton("Volver", (dialog, which) -> {
                    Log.d(TAG, "❌ Usuario canceló la acción");
                    dialog.dismiss();
                })
                .show();
    }

    private void confirmReservation(Reserva reserva) {
        Log.d(TAG, "🔄 Confirmando reserva: " + reserva.getIdReserva());
        updateReservationStatus(reserva, "Confirmada");
    }

    private void cancelReservation(Reserva reserva) {
        Log.d(TAG, "🔄 Cancelando reserva: " + reserva.getIdReserva());
        updateReservationStatus(reserva, "Cancelada");
    }

    private void updateReservationStatus(Reserva reserva, String nuevoEstado) {
        Log.d(TAG, "🔄 Actualizando estado de reserva a: " + nuevoEstado);
        Log.d(TAG, "   - Reserva ID: " + reserva.getIdReserva());
        Log.d(TAG, "   - Pasajero: " + reserva.getNombre());
        Log.d(TAG, "   - Pasajero ID: " + reserva.getUsuarioId());

        reservaService.actualizarEstadoReserva(reserva.getIdReserva(), nuevoEstado,
                new ReservaService.ReservationUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "✅ Estado de reserva actualizado exitosamente a: " + nuevoEstado);
                        Toast.makeText(InicioConductor.this,
                                "Reserva " + nuevoEstado.toLowerCase(), Toast.LENGTH_SHORT).show();

                        if ("Confirmada".equals(nuevoEstado)) {
                            Log.d(TAG, "💰 Actualizando ingresos después de confirmación");
                            updateIncomeAfterConfirmation(reserva);

                            Log.d(TAG, "📲 Enviando notificación de confirmación al pasajero");
                            enviarNotificacionConfirmacionAlPasajero(reserva);
                        } else if ("Cancelada".equals(nuevoEstado)) {
                            Log.d(TAG, "📲 Enviando notificación de cancelación al pasajero");
                            enviarNotificacionCancelacionAlPasajero(reserva);
                        }

                        Log.d(TAG, "🔄 Recargando datos después de actualización");
                        reloadData();
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ Error actualizando estado de reserva: " + error);
                        Toast.makeText(InicioConductor.this,
                                "Error al actualizar: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * ✅ VERSIÓN MEJORADA: Enviar notificación al pasajero cuando el conductor confirma la reserva
     */
    private void enviarNotificacionConfirmacionAlPasajero(Reserva reserva) {
        Log.d(TAG, "🎯 INICIANDO ENVÍO DE NOTIFICACIÓN DE CONFIRMACIÓN");

        try {
            // Obtener datos del conductor actual
            String nombreConductor = tvConductor.getText().toString();
            if (nombreConductor.equals("N/A") || nombreConductor.isEmpty()) {
                nombreConductor = reserva.getConductor() != null ? reserva.getConductor() : "Tu conductor";
            }

            // Obtener datos del vehículo
            String placaVehiculo = obtenerPlacaVehiculo();
            String modeloVehiculo = obtenerModeloVehiculo();

            // ✅ USAR LOS CAMPOS CORRECTOS DE TU MODELO RESERVA
            String pasajeroId = reserva.getUsuarioId();
            String pasajeroNombre = reserva.getNombre();
            String ruta = reserva.getOrigen() + " → " + reserva.getDestino();
            String fechaHora = obtenerFechaHoraReserva(reserva);
            int asiento = reserva.getPuestoReservado();

            Log.d(TAG, "📋 Datos para notificación:");
            Log.d(TAG, "   - Pasajero ID: " + pasajeroId);
            Log.d(TAG, "   - Pasajero Nombre: " + pasajeroNombre);
            Log.d(TAG, "   - Ruta: " + ruta);
            Log.d(TAG, "   - Asiento: " + asiento);
            Log.d(TAG, "   - Conductor: " + nombreConductor);

            if (pasajeroId != null && !pasajeroId.isEmpty()) {
                Log.d(TAG, "📤 Llamando a NotificationManager...");

                // 🔹 ENVIAR NOTIFICACIÓN DE CONFIRMACIÓN AL PASAJERO
                notificationManager.notificarReservaConfirmadaAlPasajero(
                        pasajeroId,
                        nombreConductor,
                        ruta,
                        fechaHora,
                        asiento,
                        placaVehiculo,
                        modeloVehiculo
                );

                Log.d(TAG, "✅ Notificación de CONFIRMACIÓN enviada a NotificationManager");
                Log.d(TAG, "   - Destino: " + pasajeroNombre + " (ID: " + pasajeroId + ")");
                Log.d(TAG, "   - Ruta: " + ruta);
                Log.d(TAG, "   - Asiento: A" + asiento);

            } else {
                Log.w(TAG, "❌ No se pudo enviar notificación: ID del pasajero no disponible");
                Log.w(TAG, "   - Reserva ID: " + reserva.getIdReserva());
            }

        } catch (Exception e) {
            Log.e(TAG, "💥 ERROR CRÍTICO enviando notificación al pasajero: " + e.getMessage());
            Log.e(TAG, "   - Reserva ID: " + reserva.getIdReserva());
            e.printStackTrace();
        }
    }

    /**
     * ✅ OPCIONAL: Enviar notificación al pasajero cuando el conductor cancela la reserva
     */
    private void enviarNotificacionCancelacionAlPasajero(Reserva reserva) {
        Log.d(TAG, "🎯 INICIANDO ENVÍO DE NOTIFICACIÓN DE CANCELACIÓN");

        try {
            String nombreConductor = tvConductor.getText().toString();
            if (nombreConductor.equals("N/A") || nombreConductor.isEmpty()) {
                nombreConductor = "El conductor";
            }

            // ✅ USAR getUsuarioId() en lugar de getUserId()
            String pasajeroId = reserva.getUsuarioId();
            String ruta = reserva.getOrigen() + " → " + reserva.getDestino();
            String motivo = "Por decisión del conductor";

            Log.d(TAG, "📋 Datos para cancelación:");
            Log.d(TAG, "   - Pasajero ID: " + pasajeroId);
            Log.d(TAG, "   - Ruta: " + ruta);

            if (pasajeroId != null && !pasajeroId.isEmpty()) {
                notificationManager.notificarReservaCanceladaAlPasajero(
                        pasajeroId,
                        nombreConductor,
                        ruta,
                        motivo
                );

                Log.d(TAG, "✅ Notificación de CANCELACIÓN enviada al pasajero");
            } else {
                Log.w(TAG, "❌ No se pudo enviar notificación de cancelación: ID del pasajero no disponible");
            }

        } catch (Exception e) {
            Log.e(TAG, "💥 ERROR enviando notificación de cancelación: " + e.getMessage());
        }
    }

    /**
     * ✅ Métodos auxiliares para obtener datos del vehículo
     */
    private String obtenerPlacaVehiculo() {
        String placa = tvPlacaVehiculo.getText().toString();
        if (placa.contains(":")) {
            return placa.split(":")[1].trim();
        }
        return placa.replace("Placa: ", "").trim();
    }

    private String obtenerModeloVehiculo() {
        // Si no tienes modelo, puedes usar un valor por defecto o obtenerlo de tu base de datos
        return "Vehículo de transporte";
    }

    /**
     * ✅ NUEVO MÉTODO AUXILIAR: Formatear fecha y hora de la reserva
     */
    private String obtenerFechaHoraReserva(Reserva reserva) {
        try {
            // Si tienes fechaReserva como timestamp
            if (reserva.getFechaReserva() > 0) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy - HH:mm", java.util.Locale.getDefault());
                return sdf.format(new java.util.Date(reserva.getFechaReserva()));
            }

            // Si no hay fecha específica, usar información básica
            return "Próximo viaje";

        } catch (Exception e) {
            return "Próximo viaje";
        }
    }

    private void updateIncomeAfterConfirmation(Reserva reserva) {
        Log.d(TAG, "💰 Actualizando ingresos después de confirmar reserva");
        Log.d(TAG, "   - Precio reserva: $" + reserva.getPrecio());
        Log.d(TAG, "   - Ingresos actuales: $" + totalIngresos);
        Log.d(TAG, "   - Nuevos ingresos: $" + (totalIngresos + reserva.getPrecio()));

        String userId = authManager.getUserId();
        if (userId != null) {
            statisticsManager.updateIncomeInFirebase(userId, totalIngresos + reserva.getPrecio(),
                    new UserService.IncomeUpdateCallback() {
                        @Override
                        public void onSuccess(double nuevosIngresos) {
                            Log.d(TAG, "✅ Ingresos actualizados en Firebase: $" + nuevosIngresos);
                            totalIngresos = nuevosIngresos;
                            runOnUiThread(() -> updateStatisticsUI());
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "❌ Error actualizando ingresos: " + error);
                        }
                    });
        }
    }

    private void reloadData() {
        Log.d(TAG, "🔄 Recargando datos del conductor...");

        String nombreConductor = tvConductor.getText().toString().replace("Conductor: ", "");
        if (!nombreConductor.isEmpty()) {
            Log.d(TAG, "👤 Recargando datos para: " + nombreConductor);
            calculateStatistics(nombreConductor);
            loadReservations(nombreConductor);
        } else {
            Log.w(TAG, "⚠️ No se puede recargar datos - nombre de conductor vacío");
        }
    }

    private void goToDriverProfile() {
        Log.d(TAG, "👤 Navegando a perfil de conductor");

        if (authManager.isUserLoggedIn()) {
            startActivity(new Intent(this, PerfilConductor.class));
            Log.d(TAG, "✅ Intent iniciado para PerfilConductor");
        } else {
            Log.w(TAG, "⚠️ Usuario no logeado - no se puede navegar al perfil");
        }
    }

    private void showDefaultData() {
        Log.d(TAG, "ℹ️ Mostrando datos por defecto");

        showEmptyReservations();
        showEmptyRoutes();
        tvConductor.setText("N/A");
        tvPlacaVehiculo.setText("Placa: N/A");
        tvTotalIngresos.setText("Ingresos: $0");
        reservasConfirmadasHoy = 0;
        asientosDisponibles = CAPACIDAD_TOTAL;
        updateStatisticsUI();

        Log.d(TAG, "✅ Datos por defecto mostrados");
    }

    private void showEmptyReservations() {
        Log.d(TAG, "ℹ️ Mostrando estado vacío para reservas");

        tvEmptyReservas.setVisibility(View.VISIBLE);
        rvReservas.setVisibility(View.GONE);
        tvReservasActivas.setText(getString(R.string.reservasPendientes, 0));
    }

    private void showEmptyRoutes() {
        Log.d(TAG, "ℹ️ Mostrando estado vacío para rutas");

        tvEmptyRutas.setVisibility(View.VISIBLE);
        rvProximasRutas.setVisibility(View.GONE);
        tvProximaRuta.setText("Próxima: No hay rutas asignadas");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "📱 onStart - Actividad visible");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "📱 onResume - Actividad en primer plano");
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
    }
}