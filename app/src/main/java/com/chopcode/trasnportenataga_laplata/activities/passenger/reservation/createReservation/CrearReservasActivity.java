package com.chopcode.trasnportenataga_laplata.activities.passenger.reservation.createReservation;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.utils.ReservationDateUtils;
import com.chopcode.trasnportenataga_laplata.activities.passenger.reservation.confirmReservation.ConfirmarReservaActivity;
import com.chopcode.trasnportenataga_laplata.config.MyApp;
import com.chopcode.trasnportenataga_laplata.managers.analytics.ReservationAnalyticsHelper;
import com.chopcode.trasnportenataga_laplata.managers.seats.SeatManager;
import com.chopcode.trasnportenataga_laplata.managers.ui.ExpandableSectionManager;
import com.chopcode.trasnportenataga_laplata.managers.reservations.dataprocessor.ReservationDataProcessor;
import com.chopcode.trasnportenataga_laplata.managers.reservations.DriverVehicleManager;
import com.chopcode.trasnportenataga_laplata.managers.reservations.ReservationUserManager;
import com.chopcode.trasnportenataga_laplata.services.reservations.ReservaService;
import com.chopcode.trasnportenataga_laplata.services.user.UserService;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Actividad para la gestión de reservas de asientos en un transporte.
 * Permite seleccionar una ruta, visualizar los asientos disponibles y confirmar la reserva.
 */
public class CrearReservasActivity extends AppCompatActivity implements SeatManager.SeatSelectionListener {

    // Constantes
    private static final String TAG = "CrearReservas";

    // UI Elements
    private Button btnConfirmar;
    private Button btnCancelar;
    private MaterialToolbar topAppBar;

    // Data from intent
    private String rutaSeleccionada, horarioId, horarioHora;

    // Services
    private ReservaService reservaService;

    // Views de información del viaje
    private TextView tvRutaSeleccionada, tvDescripcionRuta, tvHorarioSeleccionado, tvFechaViaje;
    private TextView tvVehiculoInfo, tvCapacidadInfo, tvCapacidadDispo, tvNombreConductor;

    // Views para sección plegable
    private ExpandableSectionManager expandableSectionManager;
    private RelativeLayout headerInfo;
    private LinearLayout contenidoExpandible;
    private LinearLayout resumenInfo;
    private ImageView iconExpandCollapse;
    private TextView tvRutaResumen;
    private TextView tvHorarioResumen;

    // Información del conductor y vehículo
    private String conductorId;
    private String conductorNombre;
    private String conductorTelefono;
    private String placaVehiculo;
    private String modeloVehiculo;
    private Integer capacidadVehiculo;

    // Managers
    private ReservationAnalyticsHelper analyticsHelper;
    private SeatManager seatManager;
    private ReservationDataProcessor reservationDataProcessor;
    private DriverVehicleManager driverVehicleManager;
    private ReservationUserManager reservationUserManager; // ✅ NUEVO MANAGER

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar analytics helper
        analyticsHelper = new ReservationAnalyticsHelper("CrearReservas");
        analyticsHelper.logPantallaInicio();

        // ✅ Inicializar managers
        reservationDataProcessor = new ReservationDataProcessor(analyticsHelper);
        reservationUserManager = new ReservationUserManager(analyticsHelper); // ✅ NUEVO

        setContentView(R.layout.activity_crear_reservas);

        // ✅ Inicializar SeatManager
        seatManager = new SeatManager(this, analyticsHelper);
        seatManager.setSeatSelectionListener(this);

        // ✅ Inicializar DriverVehicleManager
        driverVehicleManager = new DriverVehicleManager(this, analyticsHelper, seatManager);

        // Obtener datos del intent
        obtenerDatosDelIntent();

        // Inicializar servicios
        reservaService = new ReservaService();

        // Referencias a la UI
        inicializarViews();

        // ✅ Configurar DriverVehicleManager con referencias UI
        driverVehicleManager.setUIReferences(tvNombreConductor, tvVehiculoInfo, tvCapacidadInfo);

        // Configurar navegación
        configurarNavegacion();

        // Configurar información básica
        configurarInformacionBasica();

        // ✅ REFACTORIZADO: Cargar usuario usando ReservationUserManager
        cargarUsuario();

        // Restaurar estado si existe
        if (savedInstanceState != null) {
            restaurarEstado(savedInstanceState);
        }

        // Configurar asientos
        configurarSeleccionAsientos();

        // Cargar información del vehículo y conductor si tenemos horario
        if (horarioId != null) {
            cargarInformacionVehiculoYConductor();
            cargarAsientosDesdeFirebase(horarioId);
        } else {
            mostrarErrorSinHorario();
        }

        // Configurar botón confirmar
        btnConfirmar.setOnClickListener(v -> {
            analyticsHelper.logClickBoton("confirmar");
            validacionesReserva();
        });
    }

    private void obtenerDatosDelIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            rutaSeleccionada = intent.getStringExtra("rutaSeleccionada");
            horarioId = intent.getStringExtra("horarioId");
            horarioHora = intent.getStringExtra("horarioHora");

            // ✅ REFACTORIZADO: Usar ReservationUserManager para actualizar desde intent
            String usuarioId = intent.getStringExtra("usuarioId");
            String usuarioNombre = intent.getStringExtra("usuarioNombre");
            String usuarioTelefono = intent.getStringExtra("usuarioTelefono");

            if (usuarioId != null || usuarioNombre != null) {
                reservationUserManager.updateFromIntent(usuarioId, usuarioNombre, usuarioTelefono);
                Log.d(TAG, "✅ Datos del usuario actualizados desde Intent");
            }

            analyticsHelper.logDatosRecibidos(rutaSeleccionada != null, horarioId != null);

            Log.d(TAG, "📥 DATOS RECIBIDOS:");
            Log.d(TAG, "  - Ruta: " + rutaSeleccionada);
            Log.d(TAG, "  - Horario ID: " + horarioId);
            Log.d(TAG, "  - Horario Hora: " + horarioHora);
            Log.d(TAG, "  - Usuario: " + reservationUserManager.getUserSummary());
        }
    }

    private void inicializarViews() {
        tvRutaSeleccionada = findViewById(R.id.tvRutaSeleccionada);
        tvDescripcionRuta = findViewById(R.id.tvDescripcionRuta);
        tvHorarioSeleccionado = findViewById(R.id.tvHorarioSeleccionado);
        tvFechaViaje = findViewById(R.id.tvFechaViaje);
        tvVehiculoInfo = findViewById(R.id.tvVehiculoInfo);
        tvCapacidadInfo = findViewById(R.id.tvCapacidadInfo);
        tvCapacidadDispo = findViewById(R.id.tvCapacidadDispo);
        tvNombreConductor = findViewById(R.id.tvNombreConductor);
        btnConfirmar = findViewById(R.id.buttonConfirmar);
        btnCancelar = findViewById(R.id.buttonCancelar);
        topAppBar = findViewById(R.id.topAppBar);

        // Inicializar vistas para sección plegable
        headerInfo = findViewById(R.id.headerInfo);
        contenidoExpandible = findViewById(R.id.contenidoExpandible);
        resumenInfo = findViewById(R.id.resumenInfo);
        iconExpandCollapse = findViewById(R.id.iconExpandCollapse);
        tvRutaResumen = findViewById(R.id.tvRutaResumen);
        tvHorarioResumen = findViewById(R.id.tvHorarioResumen);

        initializeExpandableSection();
    }

    private void initializeExpandableSection() {
        expandableSectionManager = new ExpandableSectionManager(
                this,
                headerInfo,
                contenidoExpandible,
                resumenInfo,
                iconExpandCollapse,
                tvRutaResumen,
                tvHorarioResumen
        );
        expandableSectionManager.setAnalyticsInfo("CrearReservas", "info_viaje");
    }

    private void configurarNavegacion() {
        setSupportActionBar(topAppBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        topAppBar.setNavigationOnClickListener(v -> {
            analyticsHelper.logClickBoton("navegacion_atras");
            volverAtras();
        });

        btnCancelar.setOnClickListener(v -> {
            analyticsHelper.logClickBoton("cancelar");
            volverAtras();
        });
    }

    private void volverAtras() {
        if (seatManager.hasAsientoSeleccionado()) {
            Map<String, Object> params = new HashMap<>();
            params.put("asiento", seatManager.getAsientoSeleccionado());
            analyticsHelper.logEvent("dialogo_cancelar_asiento", params);

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Cancelar selección")
                    .setMessage("¿Estás seguro de que quieres cancelar la selección de asiento?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        analyticsHelper.logEvent("cancelacion_asiento_confirmada", params);
                        finish();
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        analyticsHelper.logEvent("cancelacion_asiento_rechazada", params);
                        dialog.dismiss();
                    })
                    .show();
        } else {
            Map<String, Object> params = new HashMap<>();
            params.put("accion", "navegacion_atras_simple");
            analyticsHelper.logEvent("navegacion_atras_simple", params);
            finish();
        }
    }

    private void configurarInformacionBasica() {
        if (rutaSeleccionada != null) {
            tvRutaSeleccionada.setText(rutaSeleccionada);
            if (expandableSectionManager != null) {
                expandableSectionManager.updateSummaryInfo(rutaSeleccionada, null);
            }

            // ✅ REFACTORIZADO: Usar ReservationDateUtils para calcular tiempo estimado
            String tiempoEstimado = ReservationDateUtils.calcularTiempoEstimado(rutaSeleccionada);
            String descripcionRuta = "Ruta directa - Tiempo estimado: " + tiempoEstimado;
            tvDescripcionRuta.setText(descripcionRuta);
        }

        if (horarioHora != null) {
            tvHorarioSeleccionado.setText(horarioHora);
            if (expandableSectionManager != null) {
                expandableSectionManager.updateSummaryInfo(null, horarioHora);
            }
        }

        // ✅ REFACTORIZADO: Usar ReservationDateUtils para obtener fecha
        String fechaViaje = ReservationDateUtils.obtenerFechaDelViaje(horarioHora);
        tvFechaViaje.setText(fechaViaje);

        // ✅ MEJORADO: Usar la capacidad del SeatManager
        int capacidadTotal = seatManager.getNumeroTotalAsientos();
        tvVehiculoInfo.setText("Vehículo: Cargando...");
        tvCapacidadInfo.setText("Capacidad: " + capacidadTotal + " asientos");
        tvCapacidadDispo.setText("Capacidad disponible: " + capacidadTotal);
        tvNombreConductor.setText("Cargando...");
    }

    /**
     * ✅ REFACTORIZADO: Usar ReservationUserManager para cargar usuario
     */
    private void cargarUsuario() {
        if (!reservationUserManager.hasUserData()) {
            Log.d(TAG, "No hay datos de usuario, cargando desde Firebase...");
            reservationUserManager.loadAuthenticatedUser(new ReservationUserManager.UserDataCallback() {
                @Override
                public void onUserDataLoaded(String usuarioId, String usuarioNombre, String usuarioTelefono) {
                    Log.d(TAG, "✅ Usuario cargado exitosamente: " + usuarioNombre);
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error cargando usuario: " + error);
                }
            });
        } else {
            Log.d(TAG, "✅ Usuario ya cargado desde Intent: " + reservationUserManager.getUsuarioNombre());
        }
    }

    private void restaurarEstado(Bundle savedInstanceState) {
        int savedAsiento = savedInstanceState.getInt("asientoSeleccionado", -1);
        if (savedAsiento != -1) {
            seatManager.setAsientoSeleccionado(savedAsiento);
        }

        rutaSeleccionada = savedInstanceState.getString("rutaSeleccionada");
        conductorNombre = savedInstanceState.getString("conductorNombre", "Cargando...");

        // ✅ Obtener datos del DriverVehicleManager si ya estaban cargados
        if (driverVehicleManager != null) {
            conductorNombre = driverVehicleManager.getConductorNombre();
            conductorTelefono = driverVehicleManager.getConductorTelefono();
            placaVehiculo = driverVehicleManager.getPlacaVehiculo();
            modeloVehiculo = driverVehicleManager.getModeloVehiculo();
            capacidadVehiculo = driverVehicleManager.getCapacidadVehiculo();
            conductorId = driverVehicleManager.getConductorId();
        }

        boolean isInfoExpanded = savedInstanceState.getBoolean("isInfoExpanded", true);
        if (expandableSectionManager != null) {
            expandableSectionManager.restoreState(isInfoExpanded);
        }

        // ✅ Restaurar datos del usuario desde savedInstanceState
        String savedUsuarioId = savedInstanceState.getString("usuarioId");
        String savedUsuarioNombre = savedInstanceState.getString("usuarioNombre");
        String savedUsuarioTelefono = savedInstanceState.getString("usuarioTelefono");

        if (savedUsuarioNombre != null) {
            reservationUserManager.updateFromIntent(savedUsuarioId, savedUsuarioNombre, savedUsuarioTelefono);
        }
    }

    private void configurarSeleccionAsientos() {
        // ✅ MEJORADO: Usar el método automático del SeatManager
        seatManager.configurarAsientos();

        int capacidadTotal = seatManager.getNumeroTotalAsientos();
        Log.d(TAG, "✅ Asientos configurados automáticamente. Capacidad total: " + capacidadTotal);
    }

    private void cargarAsientosDesdeFirebase(String horarioId) {
        if (rutaSeleccionada == null) return;

        Map<String, Object> params = new HashMap<>();
        params.put("accion", "carga_asientos_inicio");
        analyticsHelper.logEvent("carga_asientos_inicio", params);

        reservaService.obtenerAsientosOcupados(horarioId, new ReservaService.AsientosCallback() {
            @Override
            public void onAsientosObtenidos(int[] asientosOcupados) {
                Set<Integer> ocupados = new HashSet<>();
                for (int asiento : asientosOcupados) {
                    ocupados.add(asiento);
                }

                // ✅ MEJORADO: Obtener capacidad total del SeatManager
                int capacidadTotal = seatManager.getCapacidadTotal();
                seatManager.actualizarEstadoAsientos(ocupados, capacidadTotal);

                // ✅ MEJORADO: Usar métodos del SeatManager para obtener información
                int capacidadDisponible = seatManager.getCapacidadDisponible();
                tvCapacidadDispo.setText("Capacidad disponible: " + capacidadDisponible);

                Log.d(TAG, "✅ Estado actualizado - Ocupados: " + ocupados.size() +
                        ", Disponibles: " + capacidadDisponible);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(CrearReservasActivity.this,
                        "Error al obtener disponibilidad: " + error, Toast.LENGTH_SHORT).show();
                MyApp.logError(new Exception("Error obteniendo asientos: " + error));
                analyticsHelper.logError("carga_asientos", error);
            }
        });
    }

    /**
     * ✅ REFACTORIZADO: Usando ReservationDataProcessor para validaciones y envío
     */
    private void validacionesReserva() {
        // ✅ Obtener datos actualizados del DriverVehicleManager
        obtenerDatosActualizadosDelManager();

        // ✅ REFACTORIZADO: Usar ReservationDateUtils para obtener fecha
        String fechaViaje = ReservationDateUtils.obtenerFechaDelViaje(horarioHora);

        // ✅ REFACTORIZADO: Usar ReservationUserManager para obtener datos del usuario
        String usuarioId = reservationUserManager.getUsuarioId();
        String usuarioNombre = reservationUserManager.getUsuarioNombre();
        String usuarioTelefono = reservationUserManager.getUsuarioTelefono();

        Intent intent = reservationDataProcessor.prepareReservationConfirmation(
                this,
                seatManager,
                rutaSeleccionada,
                horarioId,
                horarioHora,
                conductorNombre,
                conductorTelefono,
                conductorId,
                placaVehiculo,
                modeloVehiculo,
                capacidadVehiculo,
                usuarioNombre,
                usuarioTelefono,
                usuarioId,
                fechaViaje
        );

        if (intent != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "Error: Datos de reserva incompletos", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * ✅ NUEVO: Obtener datos actualizados del DriverVehicleManager
     */
    private void obtenerDatosActualizadosDelManager() {
        if (driverVehicleManager != null) {
            conductorId = driverVehicleManager.getConductorId();
            conductorNombre = driverVehicleManager.getConductorNombre();
            conductorTelefono = driverVehicleManager.getConductorTelefono();
            placaVehiculo = driverVehicleManager.getPlacaVehiculo();
            modeloVehiculo = driverVehicleManager.getModeloVehiculo();
            capacidadVehiculo = driverVehicleManager.getCapacidadVehiculo();
        }
    }

    private void mostrarErrorSinHorario() {
        analyticsHelper.logError("sin_horario_id", "No se recibió información del horario");
        Toast.makeText(this, "Error: No se recibió información del horario", Toast.LENGTH_SHORT).show();
        finish();
    }

    // ============================================================
    // ✅ REFACTORIZADO: Métodos de conductor/vehículo usando DriverVehicleManager
    // ============================================================

    private void cargarInformacionVehiculoYConductor() {
        Log.d(TAG, "Cargando información del vehículo y conductor usando DriverVehicleManager...");

        driverVehicleManager.loadDriverVehicleInfo(horarioId, new DriverVehicleManager.DriverVehicleCallback() {
            @Override
            public void onDriverVehicleLoaded(String conductorId, String conductorNombre,
                                              String conductorTelefono, String placaVehiculo,
                                              String modeloVehiculo, Integer capacidadVehiculo) {

                // ✅ Actualizar variables locales con los datos cargados
                CrearReservasActivity.this.conductorId = conductorId;
                CrearReservasActivity.this.conductorNombre = conductorNombre;
                CrearReservasActivity.this.conductorTelefono = conductorTelefono;
                CrearReservasActivity.this.placaVehiculo = placaVehiculo;
                CrearReservasActivity.this.modeloVehiculo = modeloVehiculo;
                CrearReservasActivity.this.capacidadVehiculo = capacidadVehiculo;

                Log.d(TAG, "✅ Información de conductor y vehículo cargada exitosamente");
                Log.d(TAG, "  - Conductor: " + conductorNombre);
                Log.d(TAG, "  - Vehículo: " + placaVehiculo + " - " + modeloVehiculo);
                Log.d(TAG, "  - Capacidad: " + capacidadVehiculo);

                // ✅ Actualizar capacidad disponible en UI
                int capacidadDisponible = seatManager.getCapacidadDisponible();
                tvCapacidadDispo.setText("Capacidad disponible: " + capacidadDisponible);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error cargando información del conductor/vehículo: " + error);
                Toast.makeText(CrearReservasActivity.this,
                        "Error cargando información del vehículo: " + error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ============================================================
    // Métodos de SeatSelectionListener
    // ============================================================

    @Override
    public void onSeatSelected(int seatNumber) {
        Log.d(TAG, "✅ Asiento " + seatNumber + " seleccionado");

        int capacidadTotal = seatManager.getCapacidadTotal();
        int disponibles = seatManager.getCapacidadDisponible();
        Log.d(TAG, "  - Capacidad total: " + capacidadTotal);
        Log.d(TAG, "  - Disponibles: " + disponibles);
    }

    @Override
    public void onSeatDeselected(int seatNumber) {
        Log.d(TAG, "✅ Asiento " + seatNumber + " deseleccionado");
    }

    @Override
    public void onExpandableSectionRequestedToCollapse() {
        if (expandableSectionManager != null && expandableSectionManager.isExpanded()) {
            expandableSectionManager.collapseSection();
        }
    }

    // ============================================================
    // Métodos del ciclo de vida
    // ============================================================

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (seatManager.hasAsientoSeleccionado()) {
            outState.putInt("asientoSeleccionado", seatManager.getAsientoSeleccionado());
        }

        if (rutaSeleccionada != null) {
            outState.putString("rutaSeleccionada", rutaSeleccionada);
        }

        // ✅ Guardar datos del DriverVehicleManager
        if (driverVehicleManager != null) {
            outState.putString("conductorNombre", driverVehicleManager.getConductorNombre());
            outState.putString("conductorTelefono", driverVehicleManager.getConductorTelefono());
        } else {
            outState.putString("conductorNombre", conductorNombre);
            outState.putString("conductorTelefono", conductorTelefono);
        }

        // ✅ Guardar datos del ReservationUserManager
        outState.putString("usuarioId", reservationUserManager.getUsuarioId());
        outState.putString("usuarioNombre", reservationUserManager.getUsuarioNombre());
        outState.putString("usuarioTelefono", reservationUserManager.getUsuarioTelefono());

        if (expandableSectionManager != null) {
            outState.putBoolean("isInfoExpanded", expandableSectionManager.isExpanded());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "📱 onResume");
        Map<String, Object> params = new HashMap<>();
        params.put("accion", "pantalla_crear_reservas_resume");
        analyticsHelper.logEvent("pantalla_crear_reservas_resume", params);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "📱 onDestroy");

        Map<String, Object> params = new HashMap<>();
        params.put("accion", "pantalla_crear_reservas_destroy");
        analyticsHelper.logEvent("pantalla_crear_reservas_destroy", params);

        if (seatManager != null) {
            seatManager.cleanup();
        }

        if (expandableSectionManager != null) {
            expandableSectionManager.cleanup();
        }
    }

    @Override
    public void onBackPressed() {
        Map<String, Object> params = new HashMap<>();
        params.put("accion", "boton_back_fisico");
        analyticsHelper.logEvent("boton_back_fisico", params);
        volverAtras();
    }
}