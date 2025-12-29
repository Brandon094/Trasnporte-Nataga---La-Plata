package com.chopcode.trasnportenataga_laplata.activities.driver;

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
import com.chopcode.trasnportenataga_laplata.activities.driver.profile.PerfilConductor;
import com.chopcode.trasnportenataga_laplata.adapters.reservas.ReservaAdapter;
import com.chopcode.trasnportenataga_laplata.adapters.rutas.RutaAdapter;
import com.chopcode.trasnportenataga_laplata.config.MyApp;
import com.chopcode.trasnportenataga_laplata.managers.AuthManager;
import com.chopcode.trasnportenataga_laplata.managers.NotificationManager;
import com.chopcode.trasnportenataga_laplata.managers.StatisticsManager;
import com.chopcode.trasnportenataga_laplata.models.Reserva;
import com.chopcode.trasnportenataga_laplata.models.Ruta;
import com.chopcode.trasnportenataga_laplata.services.reservations.ReservaService;
import com.chopcode.trasnportenataga_laplata.services.user.UserService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class InicioConductor extends AppCompatActivity {
    // Views
    private RecyclerView rvReservas, rvProximasRutas;
    private TextView tvConductor, tvPlacaVehiculo;
    private TextView tvEmptyReservas, tvEmptyRutas, tvReservasConfirmadas, tvAsientosDisponibles, tvInfoCapacidad;
    private TextView tvTotalIngresos, tvInfoIngresos;
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

    // Listeners para tiempo real
    private DatabaseReference reservasRef;
    private ValueEventListener reservasListener;

    // Tag para logs
    private static final String TAG = "InicioConductor";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "🚀 onCreate - Iniciando actividad de conductor");

        try {
            setContentView(R.layout.activity_inicio_conductor);
            Log.d(TAG, "✅ Layout inflado correctamente");

            // Inicializar servicios usando MyApp para Firebase
            authManager = AuthManager.getInstance();
            userService = new UserService();
            reservaService = new ReservaService();
            statisticsManager = new StatisticsManager();
            notificationManager = NotificationManager.getInstance(this);
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
            MyApp.logError(e); // ✅ Usar MyApp para logging de errores
            Toast.makeText(this, "Error al iniciar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
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

        btnPerfilConductor = findViewById(R.id.btnPerfilConductor);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        rvReservas = findViewById(R.id.recyclerReservas);
        rvProximasRutas = findViewById(R.id.recyclerProximasRutas);

        Log.d(TAG, "✅ Todas las vistas inicializadas");
        updateStatisticsUI();
    }

    // Configurar listener en tiempo real usando MyApp
    private void setupRealTimeListener(String conductorNombre) {
        Log.d(TAG, "🔔 Configurando listener en tiempo real para: " + conductorNombre);

        try {
            // Remover listener anterior si existe
            if (reservasRef != null && reservasListener != null) {
                reservasRef.removeEventListener(reservasListener);
                Log.d(TAG, "🗑️ Listener anterior removido");
            }

            // ✅ Usar MyApp para obtener referencia a la base de datos
            reservasRef = MyApp.getDatabaseReference("reservas");

            reservasListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Log.d(TAG, "🔄 Datos cambiados en Firebase - Actualizando en tiempo real");

                    int nuevasConfirmadas = 0;
                    final List<Reserva> reservasTiempoReal = new ArrayList<>();

                    // Procesar todas las reservas
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        try {
                            Reserva reserva = snapshot.getValue(Reserva.class);
                            if (reserva != null) {
                                // Filtrar por conductor y estado "Por confirmar"
                                if (conductorNombre.equals(reserva.getConductor()) &&
                                        "Por confirmar".equals(reserva.getEstadoReserva())) {

                                    reservasTiempoReal.add(reserva);

                                    // Contar reservas confirmadas
                                    if ("Confirmada".equals(reserva.getEstadoReserva())) {
                                        nuevasConfirmadas++;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Error procesando reserva: " + e.getMessage());
                            MyApp.logError(e);
                        }
                    }

                    Log.d(TAG, "📊 Estadísticas en tiempo real:");
                    Log.d(TAG, "   - Reservas por confirmar: " + reservasTiempoReal.size());
                    Log.d(TAG, "   - Reservas confirmadas: " + nuevasConfirmadas);

                    // Crear copia final de las variables para usar en el Runnable
                    final int finalNuevasConfirmadas = nuevasConfirmadas;
                    final int finalReservasConfirmadasHoy = reservasConfirmadasHoy;

                    // ✅ Registrar evento analítico usando MyApp
                    registrarEventoAnalitico("reservas_tiempo_real",
                            reservasTiempoReal.size(), nuevasConfirmadas);

                    // Actualizar UI en el hilo principal
                    runOnUiThread(() -> {
                        // Actualizar contador de reservas confirmadas
                        if (finalReservasConfirmadasHoy != finalNuevasConfirmadas) {
                            Log.d(TAG, "🔄 Actualizando contador de confirmadas: " +
                                    finalReservasConfirmadasHoy + " → " + finalNuevasConfirmadas);
                            reservasConfirmadasHoy = finalNuevasConfirmadas;
                            tvReservasConfirmadas.setText(String.valueOf(reservasConfirmadasHoy));

                            // Animación sutil para indicar cambio
                            tvReservasConfirmadas.animate()
                                    .scaleX(1.2f)
                                    .scaleY(1.2f)
                                    .setDuration(200)
                                    .withEndAction(() -> tvReservasConfirmadas.animate()
                                            .scaleX(1.0f)
                                            .scaleY(1.0f)
                                            .setDuration(200)
                                            .start())
                                    .start();
                        }

                        // Actualizar lista de reservas si hay cambios
                        if (!reservasTiempoReal.equals(listaReservas)) {
                            Log.d(TAG, "🔄 Actualizando lista de reservas en tiempo real");
                            listaReservas.clear();
                            listaReservas.addAll(reservasTiempoReal);
                            reservaAdapter.actualizarReservas(new ArrayList<>(listaReservas));
                            updateReservationsUI();
                        }
                    });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, "❌ Error en listener tiempo real: " + databaseError.getMessage());
                    MyApp.logError(new Exception("DatabaseError: " + databaseError.getMessage()));
                }
            };

            // Agregar listener
            reservasRef.addValueEventListener(reservasListener);
            Log.d(TAG, "✅ Listener en tiempo real configurado exitosamente");

        } catch (Exception e) {
            Log.e(TAG, "💥 Error crítico configurando listener tiempo real: " + e.getMessage());
            MyApp.logError(e);
        }
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

        // ✅ Usar MyApp para obtener el ID del usuario actual
        String userId = MyApp.getCurrentUserId();
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

                    // Configurar listener en tiempo real después de cargar datos
                    setupRealTimeListener(nombre);

                    calculateStatistics(nombre);
                    loadReservations(nombre);
                    loadRoutes();
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error cargando datos conductor: " + error);
                MyApp.logError(new Exception("Error cargando datos conductor: " + error)); // ✅ Logging
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

                // ✅ Registrar evento estadístico usando MyApp
                registrarEstadisticasAnaliticas(reservasConfirmadas, asientosDisp, ingresos);

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
                MyApp.logError(new Exception("Error calculando estadísticas: " + error)); // ✅ Logging
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

                        // Filtrar solo las reservas "Por confirmar"
                        List<Reserva> reservasPorConfirmar = new ArrayList<>();
                        for (Reserva reserva : reservas) {
                            if ("Por confirmar".equals(reserva.getEstadoReserva())) {
                                reservasPorConfirmar.add(reserva);
                            }
                        }

                        // ✅ Registrar evento analítico
                        registrarEventoAnalitico("reservas_cargadas", reservasPorConfirmar.size(), null);

                        runOnUiThread(() -> {
                            listaReservas.clear();
                            listaReservas.addAll(reservasPorConfirmar); // Solo agregar las por confirmar
                            reservaAdapter.actualizarReservas(new ArrayList<>(listaReservas));
                            updateReservationsUI();

                            // Log detallado de reservas
                            for (Reserva reserva : reservasPorConfirmar) {
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
                        MyApp.logError(new Exception("Error cargando reservas: " + error));
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

                // ✅ Registrar evento analítico
                registrarEventoAnalitico("rutas_cargadas", rutas.size(), null);

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
                MyApp.logError(new Exception("Error cargando rutas: " + error)); // ✅ Logging
                showEmptyRoutes();
            }
        });
    }

    private void updateStatisticsUI() {
        Log.d(TAG, "🔄 Actualizando UI de estadísticas");

        tvReservasConfirmadas.setText(String.valueOf(reservasConfirmadasHoy));
        tvAsientosDisponibles.setText(String.valueOf(asientosDisponibles));
        tvTotalIngresos.setText(formatCurrency(totalIngresos));

        if (tvInfoCapacidad != null) {
            tvInfoCapacidad.setText("De " + CAPACIDAD_TOTAL + " totales");
        }
        if (tvInfoIngresos != null) {
            tvInfoIngresos.setText("Acumulado del día");
        }

        Log.d(TAG, "✅ UI de estadísticas actualizada");
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

    private void updateReservationsUI() {
        Log.d(TAG, "🔄 Actualizando UI de reservas - Total: " + listaReservas.size());

        tvEmptyReservas.setVisibility(listaReservas.isEmpty() ? View.VISIBLE : View.GONE);
        rvReservas.setVisibility(listaReservas.isEmpty() ? View.GONE : View.VISIBLE);

        Log.d(TAG, "✅ UI de reservas actualizada");
    }

    private void updateRoutesUI() {
        Log.d(TAG, "🔄 Actualizando UI de rutas");

        if (!listaRutas.isEmpty()) {
            Ruta proximaRuta = listaRutas.get(0);
            String horario = proximaRuta.getHora() != null ? proximaRuta.getHora().getHora() : "--:--";
            Log.d(TAG, "✅ Ruta próxima: " + proximaRuta.getOrigen() + " → " + proximaRuta.getDestino());
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

                    // ✅ Registrar evento de acción
                    registrarAccionReserva(reserva, isConfirmation ? "confirmar" : "cancelar");

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
                        MyApp.logError(new Exception("Error actualizando reserva: " + error)); // ✅ Logging
                        Toast.makeText(InicioConductor.this,
                                "Error al actualizar: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void enviarNotificacionConfirmacionAlPasajero(Reserva reserva) {
        Log.d(TAG, "🎯 INICIANDO ENVÍO DE NOTIFICACIÓN DE CONFIRMACIÓN");

        try {
            String nombreConductor = tvConductor.getText().toString();
            if (nombreConductor.equals("N/A") || nombreConductor.isEmpty()) {
                nombreConductor = reserva.getConductor() != null ? reserva.getConductor() : "Tu conductor";
            }

            String placaVehiculo = obtenerPlacaVehiculo();
            String modeloVehiculo = obtenerModeloVehiculo();

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

                notificationManager.notificarReservaConfirmadaAlPasajero(
                        pasajeroId,
                        nombreConductor,
                        ruta,
                        fechaHora,
                        asiento,
                        placaVehiculo,
                        modeloVehiculo,
                        new NotificationManager.NotificationCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "✅ Notificación de CONFIRMACIÓN enviada exitosamente al pasajero");
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "❌ Error enviando notificación de confirmación: " + error);
                                MyApp.logError(new Exception("Error notificación confirmación: " + error)); // ✅ Logging
                            }
                        }
                );

                Log.d(TAG, "✅ Notificación de CONFIRMACIÓN enviada a NotificationManager");

            } else {
                Log.w(TAG, "❌ No se pudo enviar notificación: ID del pasajero no disponible");
                Log.w(TAG, "   - Reserva ID: " + reserva.getIdReserva());
            }

        } catch (Exception e) {
            Log.e(TAG, "💥 ERROR CRÍTICO enviando notificación al pasajero: " + e.getMessage());
            MyApp.logError(e); // ✅ Logging con MyApp
        }
    }

    private void enviarNotificacionCancelacionAlPasajero(Reserva reserva) {
        Log.d(TAG, "🎯 INICIANDO ENVÍO DE NOTIFICACIÓN DE CANCELACIÓN");

        try {
            String nombreConductor = tvConductor.getText().toString();
            if (nombreConductor.equals("N/A") || nombreConductor.isEmpty()) {
                nombreConductor = "El conductor";
            }

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
                        motivo,
                        new NotificationManager.NotificationCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "✅ Notificación de CANCELACIÓN enviada exitosamente al pasajero");
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "❌ Error enviando notificación de cancelación: " + error);
                                MyApp.logError(new Exception("Error notificación cancelación: " + error)); // ✅ Logging
                            }
                        }
                );

                Log.d(TAG, "✅ Notificación de CANCELACIÓN enviada al pasajero");
            } else {
                Log.w(TAG, "❌ No se pudo enviar notificación de cancelación: ID del pasajero no disponible");
            }

        } catch (Exception e) {
            Log.e(TAG, "💥 ERROR enviando notificación de cancelación: " + e.getMessage());
            MyApp.logError(e); // ✅ Logging con MyApp
        }
    }

    private String obtenerPlacaVehiculo() {
        String placa = tvPlacaVehiculo.getText().toString();
        if (placa.contains(":")) {
            return placa.split(":")[1].trim();
        }
        return placa.replace("Placa: ", "").trim();
    }

    private String obtenerModeloVehiculo() {
        return "Vehículo de transporte";
    }

    private String obtenerFechaHoraReserva(Reserva reserva) {
        try {
            if (reserva.getFechaReserva() > 0) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy - HH:mm", java.util.Locale.getDefault());
                return sdf.format(new java.util.Date(reserva.getFechaReserva()));
            }
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

        // ✅ Usar MyApp para obtener el ID del usuario
        String userId = MyApp.getCurrentUserId();
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
                            MyApp.logError(new Exception("Error actualizando ingresos: " + error)); // ✅ Logging
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

        totalIngresos = 0.0;
        reservasConfirmadasHoy = 0;
        asientosDisponibles = CAPACIDAD_TOTAL;
        updateStatisticsUI();

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

    /**
     * ✅ MÉTODO AUXILIAR: Registrar eventos analíticos usando MyApp
     */
    private void registrarEventoAnalitico(String evento, Integer reservas, Integer confirmadas) {
        try {
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("conductor_id", MyApp.getCurrentUserId());
            params.put("conductor_nombre", tvConductor.getText().toString());

            if (reservas != null) {
                params.put("total_reservas", reservas);
            }
            if (confirmadas != null) {
                params.put("reservas_confirmadas", confirmadas);
            }

            params.put("timestamp", System.currentTimeMillis());
            params.put("pantalla", "InicioConductor");

            MyApp.logEvent(evento, params);
            Log.d(TAG, "📊 Evento analítico registrado: " + evento);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando evento analítico: " + e.getMessage());
        }
    }

    /**
     * ✅ MÉTODO AUXILIAR: Registrar estadísticas usando MyApp
     */
    private void registrarEstadisticasAnaliticas(int reservasConfirmadas, int asientosDisp, double ingresos) {
        try {
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("conductor_id", MyApp.getCurrentUserId());
            params.put("reservas_confirmadas", reservasConfirmadas);
            params.put("asientos_disponibles", asientosDisp);
            params.put("ingresos", ingresos);
            params.put("capacidad_total", CAPACIDAD_TOTAL);
            params.put("timestamp", System.currentTimeMillis());

            MyApp.logEvent("estadisticas_conductor", params);
            Log.d(TAG, "📊 Estadísticas registradas en análisis");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando estadísticas: " + e.getMessage());
        }
    }

    /**
     * ✅ MÉTODO AUXILIAR: Registrar acción sobre reserva usando MyApp
     */
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
        registrarEventoAnalitico("pantalla_inicio_conductor_inicio", null, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "📱 onResume - Actividad en primer plano");

        // ✅ Registrar evento de resumen usando MyApp
        registrarEventoAnalitico("pantalla_inicio_conductor_resume", null, null);
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

        // Limpiar listeners cuando la actividad se destruya
        if (reservasRef != null && reservasListener != null) {
            reservasRef.removeEventListener(reservasListener);
            Log.d(TAG, "🗑️ Listener de Firebase removido");
        }
    }
}