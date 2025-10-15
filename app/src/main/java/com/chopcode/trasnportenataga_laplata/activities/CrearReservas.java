package com.chopcode.trasnportenataga_laplata.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.services.ReservaService;
import com.chopcode.trasnportenataga_laplata.services.UserService;
import com.chopcode.trasnportenataga_laplata.services.VehiculoService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Actividad para la gestión de reservas de asientos en un transporte.
 * Permite seleccionar una ruta, visualizar los asientos disponibles y confirmar la reserva.
 */
public class CrearReservas extends AppCompatActivity {
    /** Iconos de los asientos */
    private static final int VECTOR_ASIENTO_DISPONIBLE = R.drawable.asiento_disponible;
    private static final int VECTOR_ASIENTO_SELECCIONADO = R.drawable.asiento_seleccionado;
    private static final int VECTOR_ASIENTO_OCUPADO = R.drawable.asiento_ocupado;
    private Button btnConfirmar;
    private Button btnCancelar;
    private MaterialToolbar topAppBar;
    private Integer asientoSeleccionado = null;
    private String rutaSeleccionada, horarioId, horarioHora;
    private ReservaService reservaService;
    private VehiculoService vehiculoService;
    private UserService userService;
    private Map<Integer, MaterialButton> mapaAsientos = new HashMap<>();

    // Views de información del viaje
    private TextView tvRutaSeleccionada, tvDescripcionRuta;
    private TextView tvHorarioSeleccionado, tvFechaViaje;
    private TextView tvVehiculoInfo, tvCapacidadInfo, tvCapacidadDispo;
    private TextView tvNombreConductor;

    // Constantes
    private static final String TAG = "CrearReservas";
    private static final int CAPACIDAD_TOTAL = 14;

    /**
     * Método que se ejecuta al crear la actividad. Inicializa la UI y carga datos previos.
     * @param savedInstanceState Estado guardado de la actividad en caso de recreación.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_reservas);

        // Obtener los datos desde la actividad anterior
        Intent intent = getIntent();
        if (intent != null) {
            rutaSeleccionada = intent.getStringExtra("rutaSeleccionada");
            horarioId = intent.getStringExtra("horarioId");
            horarioHora = intent.getStringExtra("horarioHora");

            Log.d(TAG, "Datos recibidos - Ruta: " + rutaSeleccionada +
                    ", HorarioId: " + horarioId + ", Hora: " + horarioHora);
        }

        // Inicializar servicios
        reservaService = new ReservaService();
        vehiculoService = new VehiculoService();
        userService = new UserService();

        // Referencias a la UI
        inicializarViews();

        // Configurar la toolbar y botones de navegación
        configurarNavegacion();

        // Configurar información básica
        configurarInformacionBasica();

        if (savedInstanceState != null) {
            asientoSeleccionado = savedInstanceState.getInt("asientoSeleccionado", -1);
            if (asientoSeleccionado == -1) asientoSeleccionado = null;
            rutaSeleccionada = savedInstanceState.getString("rutaSeleccionada");
        }

        // Configurar asientos directamente con el horario recibido
        if (horarioId != null) {
            // Cargar información del vehículo y conductor
            cargarInformacionVehiculoYConductor();

            configurarSeleccionAsientos();
            cargarAsientosDesdeFirebase(horarioId);
        } else {
            Toast.makeText(this, "Error: No se recibió información del horario", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Accion del boton de confirmacion
        btnConfirmar.setOnClickListener(v -> validacionesReserva());
    }

    /**
     * Inicializar las views de la sección de información del viaje
     */
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
    }

    /**
     * Configurar la toolbar y botones de navegación
     */
    private void configurarNavegacion() {
        // Configurar la toolbar como action bar
        setSupportActionBar(topAppBar);

        // Habilitar flecha de navegación
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Configurar click listener para la flecha de navegación
        topAppBar.setNavigationOnClickListener(v -> {
            volverAtras();
        });

        // Configurar botón cancelar
        btnCancelar.setOnClickListener(v -> {
            volverAtras();
        });
    }

    /**
     * Método para manejar la acción de volver atrás
     */
    private void volverAtras() {
        if (asientoSeleccionado != null) {
            // Mostrar diálogo de confirmación si ya se seleccionó un asiento
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Cancelar selección")
                    .setMessage("¿Estás seguro de que quieres cancelar la selección de asiento?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        finish();
                    })
                    .setNegativeButton("No", null)
                    .show();
        } else {
            // Si no hay asiento seleccionado, simplemente volver
            finish();
        }
    }

    /**
     * Configurar información básica del viaje
     */
    private void configurarInformacionBasica() {
        // Configurar ruta
        if (rutaSeleccionada != null) {
            tvRutaSeleccionada.setText(rutaSeleccionada);

            // Establecer descripción de la ruta según la dirección
            String descripcionRuta = "Ruta directa - Tiempo estimado: ";
            if (rutaSeleccionada.contains("Natagá -> La Plata")) {
                descripcionRuta += "60 min";
            } else {
                descripcionRuta += "55 min";
            }
            tvDescripcionRuta.setText(descripcionRuta);
        }

        // Configurar horario
        if (horarioHora != null) {
            tvHorarioSeleccionado.setText(horarioHora);
        }

        // Configurar fecha actual
        String fechaActual = obtenerFechaActual();
        tvFechaViaje.setText(fechaActual);

        // Configurar información por defecto del vehículo
        tvVehiculoInfo.setText("Vehículo: Cargando...");
        tvCapacidadInfo.setText("Capacidad: " + CAPACIDAD_TOTAL + " asientos");
        tvCapacidadDispo.setText("Capacidad disponible: " + CAPACIDAD_TOTAL);
        tvNombreConductor.setText("Cargando...");
    }

    /**
     * Obtener la fecha actual formateada
     */
    private String obtenerFechaActual() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d 'de' MMMM 'del' yyyy", new Locale("es", "ES"));
        String fecha = sdf.format(new Date());
        // Capitalizar primera letra
        return fecha.substring(0, 1).toUpperCase() + fecha.substring(1);
    }

    /**
     * Cargar información del vehículo y conductor desde Firebase
     */
    private void cargarInformacionVehiculoYConductor() {
        Log.d(TAG, "Cargando información del vehículo y conductor...");

        // Primero, obtener información del horario para saber qué vehículo/conductor está asignado
        // Por ahora, usaremos un vehículo por defecto. Puedes ajustar esto según tu estructura de datos

        // Buscar vehículo por ID (puedes ajustar este ID según tu lógica)
        String vehiculoId = "TBO550"; // o la lógica para obtener el ID correcto

        vehiculoService.obtenerVehiculoPorId(vehiculoId, new VehiculoService.VehiculoMapCallback() {
            @Override
            public void onVehiculoObtenido(Map<String, Object> vehiculoMap) {
                runOnUiThread(() -> {
                    if (vehiculoMap != null) {
                        try {
                            // Extraer información del vehículo
                            String placa = (String) vehiculoMap.get("placa");
                            String modelo = (String) vehiculoMap.get("modelo");
                            String conductorId = (String) vehiculoMap.get("conductorId");
                            Integer capacidad = (Integer) vehiculoMap.get("capacidad");

                            if (placa == null) placa = "ABC123";
                            if (modelo == null) modelo = "Chevrolet D-Max";
                            if (capacidad == null) capacidad = CAPACIDAD_TOTAL;

                            // Actualizar información del vehículo
                            tvVehiculoInfo.setText("Vehículo: " + placa + " - " + modelo);
                            tvCapacidadInfo.setText("Capacidad: " + capacidad + " asientos");

                            // Si tenemos conductorId, cargar información del conductor
                            if (conductorId != null) {
                                cargarInformacionConductor(conductorId);
                            } else {
                                // Si no hay conductorId, usar valores por defecto
                                tvNombreConductor.setText("Oscar Mendoza");
                            }

                            Log.d(TAG, "Información del vehículo cargada: " + placa + " - " + modelo);

                        } catch (Exception e) {
                            Log.e(TAG, "Error al procesar datos del vehículo: " + e.getMessage());
                            configurarValoresPorDefecto();
                        }
                    } else {
                        Log.w(TAG, "No se encontró vehículo, usando valores por defecto");
                        configurarValoresPorDefecto();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error al cargar vehículo: " + error);
                    configurarValoresPorDefecto();
                    Toast.makeText(CrearReservas.this,
                            "Error al cargar información del vehículo", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Cargar información del conductor
     */
    private void cargarInformacionConductor(String conductorId) {
        userService.loadUserData(conductorId, new UserService.UserDataCallback() {
            @Override
            public void onUserDataLoaded(com.chopcode.trasnportenataga_laplata.models.Usuario usuario) {
                runOnUiThread(() -> {
                    if (usuario != null && usuario.getNombre() != null) {
                        tvNombreConductor.setText(usuario.getNombre());
                        Log.d(TAG, "Información del conductor cargada: " + usuario.getNombre());
                    } else {
                        tvNombreConductor.setText("Conductor no disponible");
                        Log.w(TAG, "No se pudo cargar información del conductor");
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error al cargar conductor: " + error);
                    tvNombreConductor.setText("Conductor no disponible");
                });
            }
        });
    }

    /**
     * Configurar valores por defecto en caso de error
     */
    private void configurarValoresPorDefecto() {
        tvVehiculoInfo.setText("Vehículo: ----- -----");
        tvCapacidadInfo.setText("Capacidad: " + CAPACIDAD_TOTAL + " asientos");
        tvNombreConductor.setText("----- -----");
    }

    /**
     * Carga la disponibilidad de los asientos desde Firebase y actualiza la UI.
     */
    private void cargarAsientosDesdeFirebase(String horarioId) {
        if (rutaSeleccionada == null) return;

        reservaService.obtenerAsientosOcupados(horarioId, new ReservaService.AsientosCallback() {
            @Override
            public void onAsientosObtenidos(int[] asientosOcupados) {
                Set<Integer> ocupados = new HashSet<>();
                for (int asiento : asientosOcupados) {
                    ocupados.add(asiento);
                }

                // Actualizar capacidad disponible
                int capacidadDisponible = CAPACIDAD_TOTAL - ocupados.size();
                tvCapacidadDispo.setText("Capacidad disponible: " + capacidadDisponible);

                for (Map.Entry<Integer, MaterialButton> entry : mapaAsientos.entrySet()) {
                    int numAsiento = entry.getKey();
                    MaterialButton btn = entry.getValue();

                    if (ocupados.contains(numAsiento)) {
                        btn.setIcon(ContextCompat.getDrawable(CrearReservas.this,
                                VECTOR_ASIENTO_OCUPADO));
                        btn.setEnabled(false);
                    } else {
                        btn.setIcon(ContextCompat.getDrawable(CrearReservas.this,
                                VECTOR_ASIENTO_DISPONIBLE));
                        btn.setEnabled(true);

                        btn.setOnClickListener(v -> {
                            if (asientoSeleccionado != null && mapaAsientos.containsKey(asientoSeleccionado)) {
                                mapaAsientos.get(asientoSeleccionado).setIcon(ContextCompat.getDrawable(CrearReservas.this, VECTOR_ASIENTO_DISPONIBLE));
                            }

                            asientoSeleccionado = numAsiento;
                            btn.setIcon(ContextCompat.getDrawable(CrearReservas.this,
                                    VECTOR_ASIENTO_SELECCIONADO));
                            Toast.makeText(CrearReservas.this,
                                    "Asiento seleccionado: " + asientoSeleccionado, Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(CrearReservas.this, "Error al obtener disponibilidad: " + error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Configura la selección de asientos y gestiona la lógica de clic en cada uno.
     */
    private void configurarSeleccionAsientos() {
        int[] botonesAsientos = {
                R.id.btnAsiento1, R.id.btnAsiento2, R.id.btnAsiento3, R.id.btnAsiento4,
                R.id.btnAsiento5, R.id.btnAsiento6, R.id.btnAsiento7, R.id.btnAsiento8,
                R.id.btnAsiento9, R.id.btnAsiento10, R.id.btnAsiento11, R.id.btnAsiento12,
                R.id.btnAsiento13, R.id.btnAsiento14
        };

        for (int i = 0; i < botonesAsientos.length; i++) {
            MaterialButton btnAsiento = findViewById(botonesAsientos[i]);
            int numeroAsiento = i + 1;
            btnAsiento.setTag(numeroAsiento);
            btnAsiento.setVisibility(View.VISIBLE);

            // IMPORTANTE: Remover el tint del icono
            btnAsiento.setIconTint(null);

            mapaAsientos.put(numeroAsiento, btnAsiento);
        }
    }

    /**
     * Valida que el usuario haya seleccionado una ruta y un asiento antes de continuar.
     */
    private void validacionesReserva() {
        if (rutaSeleccionada == null) {
            Toast.makeText(this, "Error: No hay ruta seleccionada", Toast.LENGTH_SHORT).show();
            return;
        }
        if (asientoSeleccionado == null) {
            Toast.makeText(this, "Selecciona un asiento", Toast.LENGTH_SHORT).show();
            return;
        }
        enviarConfirmarReserva();
    }

    /**
     * Enviar la informacion a la interfaz de confirmarReserva
     */
    private void enviarConfirmarReserva() {
        Intent confirmarReserva = new Intent(CrearReservas.this, ConfirmarReserva.class);
        confirmarReserva.putExtra("asientoSeleccionado", asientoSeleccionado);
        confirmarReserva.putExtra("rutaSelecionada", rutaSeleccionada);
        confirmarReserva.putExtra("horarioId", horarioId);
        confirmarReserva.putExtra("horarioHora", horarioHora);
        startActivity(confirmarReserva);
    }

    /**
     * Manejar el botón físico de back
     */
    @Override
    public void onBackPressed() {
        volverAtras();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (asientoSeleccionado != null) {
            outState.putInt("asientoSeleccionado", asientoSeleccionado);
        }
        if (rutaSeleccionada != null) {
            outState.putString("rutaSeleccionada", rutaSeleccionada);
        }
    }
}