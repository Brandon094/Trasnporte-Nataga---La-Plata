package com.chopcode.trasnportenataga_laplata.activities.passenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.managers.AuthManager;
import com.chopcode.trasnportenataga_laplata.models.Usuario;
import com.chopcode.trasnportenataga_laplata.models.Vehiculo;
import com.chopcode.trasnportenataga_laplata.services.ReservaService;
import com.chopcode.trasnportenataga_laplata.services.UserService;
import com.chopcode.trasnportenataga_laplata.services.VehiculoService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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
    private AuthManager authManager; // ✅ AGREGADO: AuthManager
    private Map<Integer, MaterialButton> mapaAsientos = new HashMap<>();

    // Views de información del viaje
    private TextView tvRutaSeleccionada, tvDescripcionRuta, tvHorarioSeleccionado, tvFechaViaje;
    private TextView tvVehiculoInfo, tvCapacidadInfo, tvCapacidadDispo, tvNombreConductor;

    // Informacion del vehiculo - VARIABLES CORREGIDAS
    private String placaVehiculo = "Cargando...";
    private String modeloVehiculo = "Cargando...";
    private Integer capacidadVehiculo = CAPACIDAD_TOTAL;

    // Constantes
    private static final String TAG = "CrearReservas";
    private static final int CAPACIDAD_TOTAL = 14;

    // Agregar estas variables para almacenar información del conductor
    private String conductorNombre = "Cargando...";
    private String conductorTelefono = "Cargando...";
    private String conductorId;

    // Datos del usuario autenticado
    private String usuarioNombre;
    private String usuarioTelefono;
    private String usuarioId;

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

            // ✅ AGREGAR: Recibir datos del usuario desde el Intent
            usuarioId = intent.getStringExtra("usuarioId");
            usuarioNombre = intent.getStringExtra("usuarioNombre");
            usuarioTelefono = intent.getStringExtra("usuarioTelefono");

            // DEBUG: Verificar qué datos llegan
            Log.d(TAG, "📥 DATOS RECIBIDOS DESDE HORARIO FRAGMENT:");
            Log.d(TAG, "  - Ruta: " + rutaSeleccionada);
            Log.d(TAG, "  - Horario ID: " + horarioId);
            Log.d(TAG, "  - Horario Hora: " + horarioHora);
            Log.d(TAG, "  - Usuario ID: " + usuarioId);
            Log.d(TAG, "  - Usuario Nombre: " + usuarioNombre);
            Log.d(TAG, "  - Usuario Teléfono: " + usuarioTelefono);
        }

        // Inicializar servicios
        reservaService = new ReservaService();
        vehiculoService = new VehiculoService();
        userService = new UserService();
        authManager = AuthManager.getInstance(); // ✅ INICIALIZADO: AuthManager

        // Referencias a la UI
        inicializarViews();

        // Configurar la toolbar y botones de navegación
        configurarNavegacion();

        // Configurar información básica
        configurarInformacionBasica();

        // ✅ AGREGAR: Cargar usuario si no llegó del Intent
        if (usuarioNombre == null || usuarioId == null) {
            Log.w(TAG, "⚠️ DATOS DE USUARIO NO RECIBIDOS, CARGANDO DESDE FIREBASE...");
            cargarUsuarioAutenticado();
        } else {
            Log.d(TAG, "✅ DATOS DE USUARIO RECIBIDOS CORRECTAMENTE VIA INTENT");
        }

        if (savedInstanceState != null) {
            asientoSeleccionado = savedInstanceState.getInt("asientoSeleccionado", -1);
            if (asientoSeleccionado == -1) asientoSeleccionado = null;
            rutaSeleccionada = savedInstanceState.getString("rutaSeleccionada");
            conductorNombre = savedInstanceState.getString("conductorNombre", "Cargando...");

            // Restaurar datos del usuario
            if (usuarioNombre == null) {
                usuarioNombre = savedInstanceState.getString("usuarioNombre");
                usuarioTelefono = savedInstanceState.getString("usuarioTelefono");
                usuarioId = savedInstanceState.getString("usuarioId");
            }
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

    // ✅ CORREGIDO: Método para cargar usuario desde Firebase (fallback)
    private void cargarUsuarioAutenticado() {
        String userId = authManager.getUserId();
        if (userId == null) {
            Log.e(TAG, "No se pudo obtener el ID del usuario autenticado");
            establecerUsuarioPorDefecto();
            return;
        }

        userService.loadUserData(userId, new UserService.UserDataCallback() {
            @Override
            public void onUserDataLoaded(Usuario usuario) {
                if (usuario != null) {
                    usuarioNombre = usuario.getNombre();
                    usuarioTelefono = usuario.getTelefono();
                    usuarioId = usuario.getId();

                    Log.d(TAG, "Usuario cargado desde Firebase: " + usuarioNombre + ", Tel: " + usuarioTelefono);
                } else {
                    Log.e(TAG, "Usuario es null");
                    establecerUsuarioPorDefecto();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error cargando usuario: " + errorMessage);
                establecerUsuarioPorDefecto();
            }
        });
    }

    // ✅ AGREGADO: Método para establecer valores por defecto del usuario
    private void establecerUsuarioPorDefecto() {
        usuarioNombre = "Usuario";
        usuarioTelefono = "No disponible";
        Log.w(TAG, "Usando valores por defecto para el usuario");
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

        // Configurar fecha del viaje (considerando si el horario ya pasó hoy)
        String fechaViaje = obtenerFechaDelViaje();
        tvFechaViaje.setText(fechaViaje);

        // Configurar información por defecto del vehículo
        tvVehiculoInfo.setText("Vehículo: Cargando...");
        tvCapacidadInfo.setText("Capacidad: " + CAPACIDAD_TOTAL + " asientos");
        tvCapacidadDispo.setText("Capacidad disponible: " + CAPACIDAD_TOTAL);
        tvNombreConductor.setText(conductorNombre);
    }

    /**
     * Obtener la fecha del viaje basándose en el horario seleccionado y la hora actual
     * Si el horario seleccionado es en la mañana pero la hora actual es más tarde,
     * entonces el viaje es para el día siguiente
     */
    private String obtenerFechaDelViaje() {
        Calendar calendar = Calendar.getInstance();
        Calendar ahora = Calendar.getInstance();

        if (horarioHora != null && esHorarioEnElPasado(horarioHora, ahora)) {
            // Si el horario seleccionado ya pasó hoy, usar el día siguiente
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            Log.d(TAG, "Horario en el pasado detectado: " + horarioHora +
                    " - Hora actual: " + obtenerHoraActualFormateada() +
                    " - Usando fecha del día siguiente");
        } else {
            Log.d(TAG, "Horario futuro detectado: " + horarioHora +
                    " - Hora actual: " + obtenerHoraActualFormateada() +
                    " - Usando fecha actual");
        }

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d 'de' MMMM 'del' yyyy", new Locale("es", "ES"));
        String fecha = sdf.format(calendar.getTime());

        // Capitalizar primera letra
        return fecha.substring(0, 1).toUpperCase() + fecha.substring(1);
    }

    /**
     * Determina si un horario seleccionado ya pasó en el día de hoy
     * @param horarioSeleccionado Hora en formato String (ej: "6:15 AM", "10:30 PM")
     * @param ahora Calendar con la hora actual
     * @return true si el horario seleccionado ya pasó hoy
     */
    private boolean esHorarioEnElPasado(String horarioSeleccionado, Calendar ahora) {
        try {
            // Parsear el horario seleccionado
            SimpleDateFormat formato12h = new SimpleDateFormat("h:mm a", Locale.US);
            Date horaSeleccionadaDate = formato12h.parse(horarioSeleccionado);

            if (horaSeleccionadaDate != null) {
                Calendar calSeleccionado = Calendar.getInstance();
                calSeleccionado.setTime(horaSeleccionadaDate);

                // Obtener hora y minutos del horario seleccionado
                int horaSeleccionada = calSeleccionado.get(Calendar.HOUR);
                int minutosSeleccionados = calSeleccionado.get(Calendar.MINUTE);
                int amPmSeleccionado = calSeleccionado.get(Calendar.AM_PM);

                // Obtener hora y minutos actuales
                int horaActual = ahora.get(Calendar.HOUR);
                int minutosActuales = ahora.get(Calendar.MINUTE);
                int amPmActual = ahora.get(Calendar.AM_PM);

                // Convertir a formato 24 horas para comparación más fácil
                int horaSeleccionada24 = convertirA24Horas(horaSeleccionada, amPmSeleccionado);
                int horaActual24 = convertirA24Horas(horaActual, amPmActual);

                Log.d(TAG, "Comparando horarios - Seleccionado: " + horaSeleccionada24 + ":" + minutosSeleccionados +
                        " - Actual: " + horaActual24 + ":" + minutosActuales);

                // Comparar horas y minutos
                if (horaSeleccionada24 < horaActual24) {
                    return true; // La hora seleccionada ya pasó hoy
                } else if (horaSeleccionada24 == horaActual24) {
                    return minutosSeleccionados <= minutosActuales; // Misma hora, comparar minutos
                }

                return false; // La hora seleccionada es futura hoy
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error al parsear horario: " + horarioSeleccionado, e);

            // Fallback: lógica simple basada en texto
            return esHorarioEnElPasadoSimple(horarioSeleccionado);
        }

        return false;
    }

    /**
     * Convierte hora en formato 12h a 24h
     */
    private int convertirA24Horas(int hora12, int amPm) {
        if (amPm == Calendar.PM && hora12 != 12) {
            return hora12 + 12;
        } else if (amPm == Calendar.AM && hora12 == 12) {
            return 0; // 12 AM = 0 horas
        }
        return hora12;
    }

    /**
     * Lógica simple de fallback para determinar si un horario ya pasó
     */
    private boolean esHorarioEnElPasadoSimple(String horario) {
        if (horario == null) return false;

        Calendar ahora = Calendar.getInstance();
        int horaActual24 = ahora.get(Calendar.HOUR_OF_DAY);
        int minutoActual = ahora.get(Calendar.MINUTE);

        String horarioUpper = horario.toUpperCase();

        try {
            // Extraer hora y minutos del string
            String[] partes = horario.split(":");
            if (partes.length >= 2) {
                int horaSeleccionada = Integer.parseInt(partes[0].trim());
                String[] minutosYAmPm = partes[1].split(" ");
                int minutosSeleccionados = Integer.parseInt(minutosYAmPm[0].trim());

                // Convertir a 24 horas
                if (horarioUpper.contains("PM") && horaSeleccionada != 12) {
                    horaSeleccionada += 12;
                } else if (horarioUpper.contains("AM") && horaSeleccionada == 12) {
                    horaSeleccionada = 0;
                }

                // Comparar
                if (horaSeleccionada < horaActual24) {
                    return true;
                } else if (horaSeleccionada == horaActual24) {
                    return minutosSeleccionados <= minutoActual;
                }
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error en fallback parser para: " + horario);
        }

        return false;
    }

    /**
     * Obtener la hora actual formateada para logging
     */
    private String obtenerHoraActualFormateada() {
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.US);
        return sdf.format(new Date());
    }

    /**
     * Cargar información del vehículo y conductor desde Firebase - MÉTODO MEJORADO
     */
    private void cargarInformacionVehiculoYConductor() {
        Log.d(TAG, "Cargando información del vehículo y conductor...");

        // Buscar conductor por horario (esto también cargará la info del vehículo)
        buscarConductorPorHorario();
    }

    /**
     * Buscar conductor asignado a este horario específico
     */
    private void buscarConductorPorHorario() {
        Log.d(TAG, "Buscando conductor para el horario: " + horarioId);

        DatabaseReference conductoresRef = FirebaseDatabase.getInstance().getReference("conductores");

        conductoresRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean conductorEncontrado = false;

                for (DataSnapshot conductorSnapshot : snapshot.getChildren()) {
                    // Verificar si este conductor tiene el horario asignado
                    if (conductorSnapshot.hasChild("horariosAsignados")) {
                        DataSnapshot horariosAsignadosSnapshot = conductorSnapshot.child("horariosAsignados");

                        // Iterar sobre los horarios asignados
                        for (DataSnapshot horarioAsignadoSnapshot : horariosAsignadosSnapshot.getChildren()) {
                            String horarioAsignado = horarioAsignadoSnapshot.getValue(String.class);
                            if (horarioId != null && horarioId.equals(horarioAsignado)) {
                                // Este conductor está asignado a este horario
                                conductorId = conductorSnapshot.getKey();
                                Log.d(TAG, "Conductor encontrado: " + conductorId + " para horario: " + horarioId);

                                // Cargar información completa del conductor y vehículo
                                cargarInformacionConductor(conductorId);
                                conductorEncontrado = true;
                                break;
                            }
                        }
                    }
                    if (conductorEncontrado) break;
                }

                // Si no se encontró conductor específico
                if (!conductorEncontrado) {
                    Log.w(TAG, "No se encontró conductor para el horario " + horarioId);
                    runOnUiThread(() -> {
                        conductorNombre = "------";
                        conductorTelefono = "------";
                        tvNombreConductor.setText(conductorNombre);
                        tvVehiculoInfo.setText("Vehículo: ------");
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error buscando conductor por horario: " + error.getMessage());
                    conductorNombre = "------";
                    conductorTelefono = "------";
                    tvNombreConductor.setText(conductorNombre);
                    tvVehiculoInfo.setText("Vehículo: ------");
                });
            }
        });
    }

    /**
     * Cargar información del conductor desde el nodo "conductores" - MÉTODO MEJORADO
     */
    private void cargarInformacionConductor(String conductorId) {
        Log.d(TAG, "Cargando información del conductor: " + conductorId);

        userService.loadDriverData(conductorId, new UserService.DriverDataCallback() {
            @Override
            public void onDriverDataLoaded(String nombre, String telefono, String placa, List<String> horariosAsignados) {
                runOnUiThread(() -> {
                    if (nombre != null && !nombre.isEmpty()) {
                        conductorNombre = nombre;
                        conductorTelefono = telefono != null ? telefono : "No disponible";
                        placaVehiculo = placa != null ? placa : "No disponible";

                        tvNombreConductor.setText(conductorNombre);
                        Log.d(TAG, "✓ Información del conductor cargada: " + conductorNombre + ", Tel: " + conductorTelefono);

                        // Ahora cargar información detallada del vehículo
                        cargarInformacionVehiculo(conductorId);
                    } else {
                        establecerValoresPorDefecto();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error cargando datos del conductor: " + error);
                    establecerValoresPorDefecto();
                });
            }
        });
    }

    /**
     * Cargar información detallada del vehículo - MÉTODO NUEVO
     */
    private void cargarInformacionVehiculo(String conductorId) {
        vehiculoService.obtenerVehiculoPorConductor(conductorId, new VehiculoService.VehiculoCallback() {
            @Override
            public void onVehiculoCargado(Vehiculo vehiculo) {
                runOnUiThread(() -> {
                    if (vehiculo != null) {
                        modeloVehiculo = vehiculo.getModelo() != null ? vehiculo.getModelo() : "No disponible";
                        placaVehiculo = vehiculo.getPlaca() != null ? vehiculo.getPlaca() : placaVehiculo;
                        capacidadVehiculo = vehiculo.getCapacidad() > 0 ?
                                vehiculo.getCapacidad() : CAPACIDAD_TOTAL;

                        // Actualizar UI con información del vehículo
                        String infoVehiculo = "Vehículo: " + placaVehiculo + " - " + modeloVehiculo;
                        tvVehiculoInfo.setText(infoVehiculo);
                        tvCapacidadInfo.setText("Capacidad: " + capacidadVehiculo + " asientos");

                        Log.d(TAG, "✓ Información del vehículo cargada: " + infoVehiculo + ", Capacidad: " + capacidadVehiculo);
                    } else {
                        // Usar información básica si no se encuentra vehículo específico
                        String infoVehiculo = "Vehículo: " + placaVehiculo + " - " + modeloVehiculo;
                        tvVehiculoInfo.setText(infoVehiculo);
                        tvCapacidadInfo.setText("Capacidad: " + CAPACIDAD_TOTAL + " asientos");
                        Log.w(TAG, "No se encontró información detallada del vehículo, usando datos básicos");
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error cargando vehículo: " + error);
                    // Usar información básica en caso de error
                    String infoVehiculo = "Vehículo: " + placaVehiculo + " - " + modeloVehiculo;
                    tvVehiculoInfo.setText(infoVehiculo);
                    tvCapacidadInfo.setText("Capacidad: " + CAPACIDAD_TOTAL + " asientos");
                });
            }
        });
    }

    // Método auxiliar para establecer valores por defecto
    private void establecerValoresPorDefecto() {
        conductorNombre = "------";
        conductorTelefono = "------";
        placaVehiculo = "------";
        modeloVehiculo = "------";

        tvNombreConductor.setText(conductorNombre);
        tvVehiculoInfo.setText("Vehículo: ------");
        tvCapacidadInfo.setText("Capacidad: " + CAPACIDAD_TOTAL + " asientos");
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
     * Enviar la informacion a la interfaz de confirmarReserva - MÉTODO MEJORADO
     */
    private void enviarConfirmarReserva() {
        Intent confirmarReserva = new Intent(CrearReservas.this, ConfirmarReserva.class);

        // DEBUG: Verificar qué datos vamos a enviar
        Log.d(TAG, "📤 ENVIANDO DATOS A CONFIRMAR RESERVA:");
        Log.d(TAG, "  - Usuario Nombre: " + usuarioNombre);
        Log.d(TAG, "  - Usuario Teléfono: " + usuarioTelefono);
        Log.d(TAG, "  - Usuario ID: " + usuarioId);

        // Información básica del viaje
        confirmarReserva.putExtra("asientoSeleccionado", asientoSeleccionado);
        confirmarReserva.putExtra("rutaSelecionada", rutaSeleccionada);
        confirmarReserva.putExtra("horarioId", horarioId);
        confirmarReserva.putExtra("horarioHora", horarioHora);
        confirmarReserva.putExtra("fechaViaje", obtenerFechaDelViaje());

        // Información del conductor
        confirmarReserva.putExtra("conductorNombre", conductorNombre);
        confirmarReserva.putExtra("conductorTelefono", conductorTelefono);
        confirmarReserva.putExtra("conductorId", conductorId);

        // Información del vehículo
        confirmarReserva.putExtra("vehiculoPlaca", placaVehiculo);
        confirmarReserva.putExtra("vehiculoModelo", modeloVehiculo);
        confirmarReserva.putExtra("vehiculoCapacidad", capacidadVehiculo);

        // Información del pasajero
        confirmarReserva.putExtra("usuarioNombre", usuarioNombre);
        confirmarReserva.putExtra("usuarioTelefono", usuarioTelefono);
        confirmarReserva.putExtra("usuarioId", usuarioId);

        // Información adicional del viaje
        String[] partesRuta = rutaSeleccionada.split(" -> ");
        if (partesRuta.length == 2) {
            confirmarReserva.putExtra("origen", partesRuta[0].trim());
            confirmarReserva.putExtra("destino", partesRuta[1].trim());
        }

        confirmarReserva.putExtra("precio", 12000.0); // Precio fijo por ahora
        confirmarReserva.putExtra("tiempoEstimado",
                rutaSeleccionada.contains("Natagá -> La Plata") ? "60 min" : "55 min");

        Log.d(TAG, "Enviando datos a ConfirmarReserva - Conductor: " + conductorNombre +
                ", Vehículo: " + placaVehiculo + " - " + modeloVehiculo +
                ", Usuario: " + usuarioNombre);

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
        outState.putString("conductorNombre", conductorNombre);
        outState.putString("conductorTelefono", conductorTelefono);

        // ✅ AGREGAR: Guardar datos del usuario
        if (usuarioNombre != null) outState.putString("usuarioNombre", usuarioNombre);
        if (usuarioTelefono != null) outState.putString("usuarioTelefono", usuarioTelefono);
        if (usuarioId != null) outState.putString("usuarioId", usuarioId);
    }
}