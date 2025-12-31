package com.chopcode.trasnportenataga_laplata.activities.passenger.reservation.confirmReservation;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.activities.passenger.InicioUsuariosActivity;
import com.chopcode.trasnportenataga_laplata.config.MyApp;
import com.chopcode.trasnportenataga_laplata.managers.analytics.ReservationAnalyticsHelper;
import com.chopcode.trasnportenataga_laplata.managers.reservations.ReservationUserManager;
import com.chopcode.trasnportenataga_laplata.managers.reservations.confirmation.ConfirmationDataProcessor;
import com.chopcode.trasnportenataga_laplata.services.reservations.ReservaService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.Map;

public class ConfirmarReservaActivity extends AppCompatActivity {

    private static final String TAG = "ConfirmarReserva";

    // UI Elements
    private TextView tvRuta, tvFechaHora, tvTiempoEstimado, tvPrecio, tvAsiento;
    private TextView tvUsuario, tvTelefonoP, tvConductor, tvTelefonoC, tvPlaca;
    private RadioGroup radioGroupPago;
    private RadioButton radioEfectivo, radioTransferencia;
    private MaterialButton btnConfirmarReserva, btnCancelar;
    private MaterialToolbar topAppBar;

    // Managers (usando los que ya tienes)
    private ReservationAnalyticsHelper analyticsHelper;
    private ReservationUserManager userManager;
    private ConfirmationDataProcessor dataProcessor;

    // Services
    private ReservaService reservaService;
    private Handler timeoutHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Inicializar managers REUTILIZADOS
        analyticsHelper = new ReservationAnalyticsHelper("ConfirmarReserva");
        analyticsHelper.logPantallaInicio();

        userManager = new ReservationUserManager(analyticsHelper);
        dataProcessor = new ConfirmationDataProcessor(analyticsHelper);

        setContentView(R.layout.activity_confirmar_reserva);

        // ✅ Inicializar servicios
        reservaService = new ReservaService();
        timeoutHandler = new Handler();

        // ✅ Procesar datos del Intent
        processIntentData();

        // ✅ Inicializar vistas
        initializeViews();

        // ✅ Configurar UI
        configureToolbar();
        configureListeners();
        loadDataIntoUI();
    }

    private void processIntentData() {
        Intent intent = getIntent();

        // ✅ Procesar datos del viaje
        dataProcessor.processIntentData(intent);

        // ✅ Actualizar userManager con datos del usuario (si vienen en el Intent)
        String usuarioId = intent.getStringExtra("usuarioId");
        String usuarioNombre = intent.getStringExtra("usuarioNombre");
        String usuarioTelefono = intent.getStringExtra("usuarioTelefono");

        if (usuarioNombre != null) {
            userManager.updateFromIntent(usuarioId, usuarioNombre, usuarioTelefono);
            Log.d(TAG, "✅ Datos de usuario actualizados desde Intent");
        } else {
            // Si no vienen en el Intent, cargar del usuario autenticado
            loadAuthenticatedUser();
        }
    }

    private void loadAuthenticatedUser() {
        userManager.loadAuthenticatedUser(new ReservationUserManager.UserDataCallback() {
            @Override
            public void onUserDataLoaded(String usuarioId, String usuarioNombre, String usuarioTelefono) {
                Log.d(TAG, "✅ Usuario cargado: " + usuarioNombre);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error cargando usuario: " + error);
            }
        });
    }

    private void initializeViews() {
        // Toolbar
        topAppBar = findViewById(R.id.topAppBar);

        // Detalles del Viaje
        tvRuta = findViewById(R.id.tvRuta);
        tvFechaHora = findViewById(R.id.tvFechaHora);
        tvAsiento = findViewById(R.id.tvAsiento);
        tvTiempoEstimado = findViewById(R.id.tvTiempoEstimado);
        tvPrecio = findViewById(R.id.tvPrecio);

        // Información de Contacto
        tvUsuario = findViewById(R.id.tvUsuario);
        tvTelefonoP = findViewById(R.id.tvTelefonoP);
        tvConductor = findViewById(R.id.tvConductor);
        tvTelefonoC = findViewById(R.id.tvTelefonoC);
        tvPlaca = findViewById(R.id.tvPlaca);

        // Método de Pago
        radioGroupPago = findViewById(R.id.radioGroupPago);
        radioEfectivo = findViewById(R.id.radioEfectivo);
        radioTransferencia = findViewById(R.id.radioTransferencia);

        // Botones
        btnConfirmarReserva = findViewById(R.id.btnConfirmarReserva);
        btnCancelar = findViewById(R.id.btnCancelar);
    }

    private void configureToolbar() {
        setSupportActionBar(topAppBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        topAppBar.setNavigationOnClickListener(v -> {
            Map<String, Object> params = new HashMap<>();
            params.put("accion", "navegacion_atras");
            analyticsHelper.logEvent("click_boton", params);
            onBackPressed();
        });
    }

    private void configureListeners() {
        // Método de pago
        radioGroupPago.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioEfectivo) {
                dataProcessor.setMetodoPago("Efectivo");

                // ✅ CORREGIDO: Usar Map en lugar de String
                Map<String, Object> params = new HashMap<>();
                params.put("tipo", "efectivo");
                params.put("asiento", dataProcessor.getAsientoSeleccionado());
                analyticsHelper.logEvent("metodo_pago_seleccionado", params);

            } else if (checkedId == R.id.radioTransferencia) {
                dataProcessor.setMetodoPago("Transferencia");

                // ✅ CORREGIDO: Usar Map en lugar de String
                Map<String, Object> params = new HashMap<>();
                params.put("tipo", "transferencia");
                params.put("asiento", dataProcessor.getAsientoSeleccionado());
                analyticsHelper.logEvent("metodo_pago_seleccionado", params);
            }
        });

        // Botón Confirmar
        btnConfirmarReserva.setOnClickListener(v -> {
            Map<String, Object> params = new HashMap<>();
            params.put("accion", "confirmar_reserva");
            params.put("asiento", dataProcessor.getAsientoSeleccionado());
            analyticsHelper.logEvent("click_boton", params);

            if (validateForm()) {
                confirmReservation();
            } else {
                Toast.makeText(this, "Por favor selecciona un método de pago", Toast.LENGTH_SHORT).show();
            }
        });

        // Botón Cancelar
        btnCancelar.setOnClickListener(v -> {
            Map<String, Object> params = new HashMap<>();
            params.put("accion", "cancelar_reserva");
            params.put("asiento", dataProcessor.getAsientoSeleccionado());
            analyticsHelper.logEvent("click_boton", params);

            showCancellationDialog();
        });
    }

    private void loadDataIntoUI() {
        // Datos del viaje
        tvRuta.setText(dataProcessor.getRutaSeleccionada());

        String fechaHoraCompleta = dataProcessor.getFechaViaje() + " - " + dataProcessor.getHorarioHora();
        tvFechaHora.setText(fechaHoraCompleta);

        tvAsiento.setText("A" + dataProcessor.getAsientoSeleccionado());
        tvTiempoEstimado.setText(dataProcessor.getTiempoEstimado());

        String precioFormateado = String.format("$%,d", (int) dataProcessor.getPrecio());
        tvPrecio.setText(precioFormateado);

        // Datos del usuario
        tvUsuario.setText(userManager.getUsuarioNombre());
        tvTelefonoP.setText(userManager.getUsuarioTelefono());

        // Datos del conductor
        tvConductor.setText(dataProcessor.getConductorNombre());
        tvTelefonoC.setText(dataProcessor.getConductorTelefono());

        String infoVehiculo = "Vehículo: " + dataProcessor.getVehiculoPlaca() +
                " - " + dataProcessor.getVehiculoModelo();
        tvPlaca.setText(infoVehiculo);

        // Método de pago por defecto
        radioEfectivo.setChecked(true);
    }

    private boolean validateForm() {
        String metodoPago = dataProcessor.getMetodoPago();
        boolean isValid = metodoPago != null && !metodoPago.isEmpty();

        if (isValid) {
            // ✅ Usar el método correcto de analyticsHelper
            Map<String, Object> params = new HashMap<>();
            params.put("asiento", dataProcessor.getAsientoSeleccionado());
            params.put("ruta", dataProcessor.getRutaSeleccionada());
            params.put("metodo_pago", metodoPago);
            analyticsHelper.logEvent("validacion_exitosa", params);
        } else {
            // ✅ Usar logError en lugar de logValidacionFallida si no existe
            Map<String, Object> params = new HashMap<>();
            params.put("razon", "sin_metodo_pago");
            analyticsHelper.logEvent("validacion_fallida", params);
        }

        return isValid;
    }

    private void showCancellationDialog() {
        Map<String, Object> params = new HashMap<>();
        params.put("asiento", dataProcessor.getAsientoSeleccionado());
        analyticsHelper.logEvent("dialogo_cancelacion_mostrado", params);

        new android.app.AlertDialog.Builder(this)
                .setTitle("Cancelar reserva")
                .setMessage("¿Estás seguro de que quieres cancelar la reserva?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    Map<String, Object> confirmParams = new HashMap<>();
                    confirmParams.put("asiento", dataProcessor.getAsientoSeleccionado());
                    confirmParams.put("accion", "confirmada");
                    analyticsHelper.logEvent("cancelacion_reserva", confirmParams);
                    finish();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    Map<String, Object> cancelParams = new HashMap<>();
                    cancelParams.put("asiento", dataProcessor.getAsientoSeleccionado());
                    cancelParams.put("accion", "rechazada");
                    analyticsHelper.logEvent("cancelacion_reserva", cancelParams);
                    dialog.dismiss();
                })
                .show();
    }

    private void confirmReservation() {
        String userId = MyApp.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();

            Map<String, Object> params = new HashMap<>();
            params.put("error", "usuario_no_autenticado");
            analyticsHelper.logEvent("error", params);

            return;
        }

        btnConfirmarReserva.setEnabled(false);
        btnConfirmarReserva.setText("Procesando...");

        String estadoReserva = "Por confirmar";

        // Timeout
        Runnable timeoutRunnable = () -> {
            runOnUiThread(() -> {
                btnConfirmarReserva.setEnabled(true);
                btnConfirmarReserva.setText("Confirmar Reserva");
                Toast.makeText(this,
                        "La operación está tardando más de lo esperado. Verifica tu conexión.",
                        Toast.LENGTH_LONG).show();

                Map<String, Object> params = new HashMap<>();
                params.put("tipo", "timeout_registro_reserva");
                analyticsHelper.logEvent("timeout", params);
            });
        };
        timeoutHandler.postDelayed(timeoutRunnable, 15000);

        reservaService.actualizarDisponibilidadAsientos(
                this,
                dataProcessor.getHorarioId(),
                dataProcessor.getAsientoSeleccionado(),
                dataProcessor.getOrigen(),
                dataProcessor.getDestino(),
                dataProcessor.getTiempoEstimado(),
                dataProcessor.getMetodoPago(),
                estadoReserva,
                dataProcessor.getVehiculoPlaca(),
                dataProcessor.getPrecio(),
                dataProcessor.getConductorNombre(),
                dataProcessor.getConductorTelefono(),
                new ReservaService.ReservaCallback() {
                    @Override
                    public void onReservaExitosa() {
                        timeoutHandler.removeCallbacks(timeoutRunnable);
                        runOnUiThread(() -> {
                            Toast.makeText(ConfirmarReservaActivity.this,
                                    "✅ Reserva creada exitosamente", Toast.LENGTH_LONG).show();

                            Map<String, Object> params = new HashMap<>();
                            params.put("asiento", dataProcessor.getAsientoSeleccionado());
                            params.put("ruta", dataProcessor.getRutaSeleccionada());
                            params.put("conductor", dataProcessor.getConductorNombre());
                            analyticsHelper.logEvent("reserva_exitosa", params);

                            navigateToHome();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        timeoutHandler.removeCallbacks(timeoutRunnable);
                        runOnUiThread(() -> {
                            btnConfirmarReserva.setEnabled(true);
                            btnConfirmarReserva.setText("Confirmar Reserva");

                            MyApp.logError(new Exception("Error registrando reserva: " + error));

                            Map<String, Object> params = new HashMap<>();
                            params.put("error", error);
                            params.put("asiento", dataProcessor.getAsientoSeleccionado());
                            analyticsHelper.logEvent("error_registro_reserva", params);

                            Toast.makeText(ConfirmarReservaActivity.this,
                                    "❌ Error al confirmar reserva: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void navigateToHome() {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("destino", "InicioUsuariosActivity");
            analyticsHelper.logEvent("navegacion", params);

            Intent intent = new Intent(this, InicioUsuariosActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error navegando a inicio: " + e.getMessage());

            Map<String, Object> params = new HashMap<>();
            params.put("error", e.getMessage());
            analyticsHelper.logEvent("error_navegacion", params);

            finish();
        }
    }

    @Override
    public void onBackPressed() {
        Map<String, Object> params = new HashMap<>();
        params.put("tipo", "boton_back_fisico");
        analyticsHelper.logEvent("navegacion", params);

        showCancellationDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timeoutHandler != null) {
            timeoutHandler.removeCallbacksAndMessages(null);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("pantalla", "ConfirmarReserva");
        analyticsHelper.logEvent("pantalla_destroy", params);
    }
}