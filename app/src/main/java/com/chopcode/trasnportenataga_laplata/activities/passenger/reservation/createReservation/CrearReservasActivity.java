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
import com.chopcode.trasnportenataga_laplata.activities.passenger.reservation.confirmReservation.ConfirmarReservaActivity;
import com.chopcode.trasnportenataga_laplata.config.MyApp;
import com.chopcode.trasnportenataga_laplata.managers.analytics.ReservationAnalyticsHelper;
import com.chopcode.trasnportenataga_laplata.managers.seats.SeatManager;
import com.chopcode.trasnportenataga_laplata.managers.auths.AuthManager;
import com.chopcode.trasnportenataga_laplata.managers.ui.ExpandableSectionManager;
import com.chopcode.trasnportenataga_laplata.managers.reservations.dataprocessor.ReservationDataProcessor;
import com.chopcode.trasnportenataga_laplata.managers.reservations.DriverVehicleManager; // ✅ NUEVO IMPORT
import com.chopcode.trasnportenataga_laplata.models.Usuario;
import com.chopcode.trasnportenataga_laplata.models.Vehiculo;
import com.chopcode.trasnportenataga_laplata.services.reservations.ReservaService;
import com.chopcode.trasnportenataga_laplata.services.user.UserService;
import com.chopcode.trasnportenataga_laplata.services.reservations.VehiculoService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
    private UserService userService;

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

    // Información del conductor y vehículo (ahora manejada por DriverVehicleManager)
    private String conductorId;
    private String conductorNombre;
    private String conductorTelefono;
    private String placaVehiculo;
    private String modeloVehiculo;
    private Integer capacidadVehiculo;

    // Datos del usuario autenticado
    private String usuarioNombre;
    private String usuarioTelefono;
    private String usuarioId;

    // Managers
    private ReservationAnalyticsHelper analyticsHelper;
    private SeatManager seatManager;
    private ReservationDataProcessor reservationDataProcessor;
    private DriverVehicleManager driverVehicleManager; // ✅ NUEVO MANAGER

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar analytics helper
        analyticsHelper = new ReservationAnalyticsHelper("CrearReservas");
        analyticsHelper.logPantallaInicio();

        // ✅ Inicializar managers
        reservationDataProcessor = new ReservationDataProcessor(analyticsHelper);

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
        userService = new UserService();

        // Referencias a la UI
        inicializarViews();

        // ✅ Configurar DriverVehicleManager con referencias UI
        driverVehicleManager.setUIReferences(tvNombreConductor, tvVehiculoInfo, tvCapacidadInfo);

        // Configurar navegación
        configurarNavegacion();

        // Configurar información básica
        configurarInformacionBasica();

        // Cargar usuario si no llegó del Intent
        if (usuarioNombre == null || usuarioId == null) {
            cargarUsuarioAutenticado();
        } else {
            analyticsHelper.logUsuarioCargado(usuarioNombre, usuarioTelefono);
        }

        // Restaurar estado si existe
        if (savedInstanceState != null) {
            restaurarEstado(savedInstanceState);
        }

        // Configurar asientos
        configurarSeleccionAsientos();

        // Cargar información del vehículo y conductor si tenemos horario
        if (horarioId != null) {
            cargarInformacionVehiculoYConductor(); // ✅ Ahora usa DriverVehicleManager
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
            usuarioId = intent.getStringExtra("usuarioId");
            usuarioNombre = intent.getStringExtra("usuarioNombre");
            usuarioTelefono = intent.getStringExtra("usuarioTelefono");

            analyticsHelper.logDatosRecibidos(rutaSeleccionada != null, horarioId != null);

            Log.d(TAG, "📥 DATOS RECIBIDOS:");
            Log.d(TAG, "  - Ruta: " + rutaSeleccionada);
            Log.d(TAG, "  - Horario ID: " + horarioId);
            Log.d(TAG, "  - Horario Hora: " + horarioHora);
            Log.d(TAG, "  - Usuario ID: " + usuarioId);
            Log.d(TAG, "  - Usuario Nombre: " + usuarioNombre);
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

            String descripcionRuta = "Ruta directa - Tiempo estimado: ";
            if (rutaSeleccionada.contains("Natagá -> La Plata")) {
                descripcionRuta += "60 min";
            } else {
                descripcionRuta += "55 min";
            }
            tvDescripcionRuta.setText(descripcionRuta);
        }

        if (horarioHora != null) {
            tvHorarioSeleccionado.setText(horarioHora);
            if (expandableSectionManager != null) {
                expandableSectionManager.updateSummaryInfo(null, horarioHora);
            }
        }

        String fechaViaje = obtenerFechaDelViaje();
        tvFechaViaje.setText(fechaViaje);

        // ✅ MEJORADO: Usar la capacidad del SeatManager
        int capacidadTotal = seatManager.getNumeroTotalAsientos();
        tvVehiculoInfo.setText("Vehículo: Cargando...");
        tvCapacidadInfo.setText("Capacidad: " + capacidadTotal + " asientos");
        tvCapacidadDispo.setText("Capacidad disponible: " + capacidadTotal);
        tvNombreConductor.setText("Cargando...");
    }

    private void cargarUsuarioAutenticado() {
        String userId = MyApp.getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "No se pudo obtener el ID del usuario");
            analyticsHelper.logError("userid_null", "ID de usuario es null");
            establecerUsuarioPorDefecto();
            return;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("accion", "carga_usuario_inicio");
        analyticsHelper.logEvent("carga_usuario_inicio", params);

        userService.loadUserData(userId, new UserService.UserDataCallback() {
            @Override
            public void onUserDataLoaded(Usuario usuario) {
                if (usuario != null) {
                    usuarioNombre = usuario.getNombre();
                    usuarioTelefono = usuario.getTelefono();
                    usuarioId = usuario.getId();
                    analyticsHelper.logUsuarioCargado(usuarioNombre, usuarioTelefono);
                } else {
                    Log.e(TAG, "Usuario es null");
                    analyticsHelper.logError("usuario_null", "Usuario es null");
                    establecerUsuarioPorDefecto();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error cargando usuario: " + errorMessage);
                MyApp.logError(new Exception("Error cargando usuario: " + errorMessage));
                analyticsHelper.logError("carga_usuario", errorMessage);
                establecerUsuarioPorDefecto();
            }
        });
    }

    private void establecerUsuarioPorDefecto() {
        usuarioNombre = "Usuario";
        usuarioTelefono = "No disponible";
        Map<String, Object> params = new HashMap<>();
        params.put("accion", "usuario_por_defecto");
        analyticsHelper.logEvent("usuario_por_defecto", params);
        Log.w(TAG, "Usando valores por defecto para el usuario");
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

        if (usuarioNombre == null) {
            usuarioNombre = savedInstanceState.getString("usuarioNombre");
            usuarioTelefono = savedInstanceState.getString("usuarioTelefono");
            usuarioId = savedInstanceState.getString("usuarioId");
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
     * ✅ MEJORADO: Usando ReservationDataProcessor para validaciones y envío
     */
    private void validacionesReserva() {
        // ✅ Obtener datos actualizados del DriverVehicleManager
        obtenerDatosActualizadosDelManager();

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
                obtenerFechaDelViaje()
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
    // Métodos de fecha/hora (sin cambios)
    // ============================================================

    private String obtenerFechaDelViaje() {
        Calendar calendar = Calendar.getInstance();
        Calendar ahora = Calendar.getInstance();

        if (horarioHora != null && esHorarioEnElPasado(horarioHora, ahora)) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            Log.d(TAG, "Horario en el pasado detectado: " + horarioHora);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d 'de' MMMM 'del' yyyy", new Locale("es", "ES"));
        String fecha = sdf.format(calendar.getTime());
        return fecha.substring(0, 1).toUpperCase() + fecha.substring(1);
    }

    private boolean esHorarioEnElPasado(String horarioSeleccionado, Calendar ahora) {
        try {
            SimpleDateFormat formato12h = new SimpleDateFormat("h:mm a", Locale.US);
            Date horaSeleccionadaDate = formato12h.parse(horarioSeleccionado);

            if (horaSeleccionadaDate != null) {
                Calendar calSeleccionado = Calendar.getInstance();
                calSeleccionado.setTime(horaSeleccionadaDate);

                int horaSeleccionada = calSeleccionado.get(Calendar.HOUR);
                int minutosSeleccionados = calSeleccionado.get(Calendar.MINUTE);
                int amPmSeleccionado = calSeleccionado.get(Calendar.AM_PM);

                int horaActual = ahora.get(Calendar.HOUR);
                int minutosActuales = ahora.get(Calendar.MINUTE);
                int amPmActual = ahora.get(Calendar.AM_PM);

                int horaSeleccionada24 = convertirA24Horas(horaSeleccionada, amPmSeleccionado);
                int horaActual24 = convertirA24Horas(horaActual, amPmActual);

                if (horaSeleccionada24 < horaActual24) {
                    return true;
                } else if (horaSeleccionada24 == horaActual24) {
                    return minutosSeleccionados <= minutosActuales;
                }
                return false;
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error al parsear horario: " + horarioSeleccionado, e);
            MyApp.logError(e);
            return esHorarioEnElPasadoSimple(horarioSeleccionado);
        }
        return false;
    }

    private int convertirA24Horas(int hora12, int amPm) {
        if (amPm == Calendar.PM && hora12 != 12) {
            return hora12 + 12;
        } else if (amPm == Calendar.AM && hora12 == 12) {
            return 0;
        }
        return hora12;
    }

    private boolean esHorarioEnElPasadoSimple(String horario) {
        if (horario == null) return false;

        Calendar ahora = Calendar.getInstance();
        int horaActual24 = ahora.get(Calendar.HOUR_OF_DAY);
        int minutoActual = ahora.get(Calendar.MINUTE);

        String horarioUpper = horario.toUpperCase();

        try {
            String[] partes = horario.split(":");
            if (partes.length >= 2) {
                int horaSeleccionada = Integer.parseInt(partes[0].trim());
                String[] minutosYAmPm = partes[1].split(" ");
                int minutosSeleccionados = Integer.parseInt(minutosYAmPm[0].trim());

                if (horarioUpper.contains("PM") && horaSeleccionada != 12) {
                    horaSeleccionada += 12;
                } else if (horarioUpper.contains("AM") && horaSeleccionada == 12) {
                    horaSeleccionada = 0;
                }

                if (horaSeleccionada < horaActual24) {
                    return true;
                } else if (horaSeleccionada == horaActual24) {
                    return minutosSeleccionados <= minutoActual;
                }
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error en fallback parser para: " + horario);
            MyApp.logError(e);
        }
        return false;
    }

    private String obtenerHoraActualFormateada() {
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.US);
        return sdf.format(new Date());
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

        if (expandableSectionManager != null) {
            outState.putBoolean("isInfoExpanded", expandableSectionManager.isExpanded());
        }

        if (usuarioNombre != null) outState.putString("usuarioNombre", usuarioNombre);
        if (usuarioTelefono != null) outState.putString("usuarioTelefono", usuarioTelefono);
        if (usuarioId != null) outState.putString("usuarioId", usuarioId);
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