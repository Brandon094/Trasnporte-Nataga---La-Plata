package com.chopcode.trasnportenataga_laplata.activities.passenger;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.managers.AuthManager;
import com.chopcode.trasnportenataga_laplata.managers.NotificationManager;
import com.chopcode.trasnportenataga_laplata.services.ReservaService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class ConfirmarReserva extends AppCompatActivity {

    // TextViews de la nueva interfaz
    private TextView tvRuta, tvFechaHora, tvTiempoEstimado, tvPrecio, tvAsiento;
    private TextView tvUsuario, tvTelefonoP, tvConductor, tvTelefonoC, tvPlaca;
    private RadioGroup radioGroupPago;
    private RadioButton radioEfectivo, radioTransferencia;
    private MaterialButton btnConfirmarReserva, btnCancelar;
    private MaterialToolbar topAppBar;

    // Variables de datos
    private int asientoSeleccionado;
    private String horarioId, horarioHora, rutaSeleccionada, fechaViaje;
    private String origen, destino, tiempoEstimado, metodoPago;
    private double precio;

    // Datos del conductor
    private String conductorNombre, conductorTelefono, conductorId;

    // Datos del vehículo
    private String vehiculoPlaca, vehiculoModelo, vehiculoCapacidad;

    // Datos del usuario
    private String usuarioNombre, usuarioTelefono, usuarioId;

    // Servicios
    private ReservaService reservaService;
    private AuthManager authManager;
    private NotificationManager notificationManager;

    private static final String TAG = "ConfirmarReserva";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmar_reserva);

        // ✅ Inicializar servicios
        reservaService = new ReservaService();
        authManager = AuthManager.getInstance();
        notificationManager = NotificationManager.getInstance();

        // Recibir TODOS los datos enviados desde CrearReservas
        recibirDatosIntent();

        // Inicializar vistas
        inicializarVistas();

        // Configurar toolbar
        configurarToolbar();

        // Configurar listeners
        configurarListeners();

        // Cargar información en la interfaz
        cargarInformacionBasica();
        cargarInformacionUsuarioYConductor();
    }

    /**
     * Recibir TODOS los datos del Intent
     */
    private void recibirDatosIntent() {
        Intent intent = getIntent();

        // Datos principales del viaje
        asientoSeleccionado = intent.getIntExtra("asientoSeleccionado", -1);
        horarioId = intent.getStringExtra("horarioId");
        horarioHora = intent.getStringExtra("horarioHora");
        rutaSeleccionada = intent.getStringExtra("rutaSelecionada");
        fechaViaje = intent.getStringExtra("fechaViaje");
        precio = intent.getDoubleExtra("precio", 12000.0);
        tiempoEstimado = intent.getStringExtra("tiempoEstimado");

        // Datos del conductor
        conductorNombre = intent.getStringExtra("conductorNombre");
        conductorTelefono = intent.getStringExtra("conductorTelefono");
        conductorId = intent.getStringExtra("conductorId");

        // Datos del vehículo
        vehiculoPlaca = intent.getStringExtra("vehiculoPlaca");
        vehiculoModelo = intent.getStringExtra("vehiculoModelo");
        vehiculoCapacidad = intent.getStringExtra("vehiculoCapacidad");

        // Datos del usuario
        usuarioNombre = intent.getStringExtra("usuarioNombre");
        usuarioTelefono = intent.getStringExtra("usuarioTelefono");
        usuarioId = intent.getStringExtra("usuarioId");

        // Origen y destino
        origen = intent.getStringExtra("origen");
        destino = intent.getStringExtra("destino");

        // Si no se recibieron origen/destino, extraer de la ruta
        if (origen == null && rutaSeleccionada != null) {
            String[] partes = rutaSeleccionada.split(" -> ");
            if (partes.length == 2) {
                origen = partes[0].trim();
                destino = partes[1].trim();
            } else {
                origen = "Natagá";
                destino = "La Plata";
            }
        }

        // Valores por defecto para datos críticos
        if (conductorNombre == null) conductorNombre = "Conductor Asignado";
        if (conductorTelefono == null) conductorTelefono = "300 123 4567";
        if (vehiculoPlaca == null) vehiculoPlaca = "ABC123";
        if (vehiculoModelo == null) vehiculoModelo = "----";
        if (usuarioNombre == null) usuarioNombre = "Usuario";
        if (usuarioTelefono == null) usuarioTelefono = "No disponible";
        if (tiempoEstimado == null) tiempoEstimado = "60 min";
        if (conductorId == null) conductorId = "conductor_default_id";

        metodoPago = "Efectivo"; // Por defecto

        Log.d(TAG, "✓ TODOS los datos recibidos via Intent:");
        Log.d(TAG, "  - Ruta: " + rutaSeleccionada + ", Asiento: " + asientoSeleccionado);
        Log.d(TAG, "  - Conductor: " + conductorNombre + ", ID: " + conductorId + ", Tel: " + conductorTelefono);
        Log.d(TAG, "  - Vehículo: " + vehiculoPlaca + " - " + vehiculoModelo);
        Log.d(TAG, "  - Usuario: " + usuarioNombre + ", Tel: " + usuarioTelefono);
    }

    /**
     * Inicializar todas las vistas
     */
    private void inicializarVistas() {
        // Toolbar
        topAppBar = findViewById(R.id.topAppBar);

        // Sección Detalles del Viaje
        tvRuta = findViewById(R.id.tvRuta);
        tvFechaHora = findViewById(R.id.tvFechaHora);
        tvAsiento = findViewById(R.id.tvAsiento);
        tvTiempoEstimado = findViewById(R.id.tvTiempoEstimado);
        tvPrecio = findViewById(R.id.tvPrecio);

        // Sección Información de Contacto
        tvUsuario = findViewById(R.id.tvUsuario);
        tvTelefonoP = findViewById(R.id.tvTelefonoP);
        tvConductor = findViewById(R.id.tvConductor);
        tvTelefonoC = findViewById(R.id.tvTelefonoC);
        tvPlaca = findViewById(R.id.tvPlaca);

        // Sección Método de Pago
        radioGroupPago = findViewById(R.id.radioGroupPago);
        radioEfectivo = findViewById(R.id.radioEfectivo);
        radioTransferencia = findViewById(R.id.radioTransferencia);

        // Botones
        btnConfirmarReserva = findViewById(R.id.btnConfirmarReserva);
        btnCancelar = findViewById(R.id.btnCancelar);
    }

    /**
     * Configurar la toolbar
     */
    private void configurarToolbar() {
        setSupportActionBar(topAppBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        topAppBar.setNavigationOnClickListener(v -> {
            onBackPressed();
        });
    }

    /**
     * Configurar listeners
     */
    private void configurarListeners() {
        // Listener para método de pago
        radioGroupPago.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioEfectivo) {
                metodoPago = "Efectivo";
            } else if (checkedId == R.id.radioTransferencia) {
                metodoPago = "Transferencia";
            }
            Log.d(TAG, "Método de pago seleccionado: " + metodoPago);
        });

        // Botón Confirmar Reserva
        btnConfirmarReserva.setOnClickListener(v -> {
            if (validarFormulario()) {
                registrarReserva();
            }
        });

        // Botón Cancelar
        btnCancelar.setOnClickListener(v -> {
            mostrarDialogoCancelacion();
        });
    }

    /**
     * Cargar información básica en la interfaz
     */
    private void cargarInformacionBasica() {
        // Ruta
        tvRuta.setText(rutaSeleccionada);

        // Fecha y Hora
        String fechaHoraCompleta = fechaViaje + " - " + horarioHora;
        tvFechaHora.setText(fechaHoraCompleta);

        // Asiento
        tvAsiento.setText("A" + asientoSeleccionado);

        // Tiempo estimado
        tvTiempoEstimado.setText(tiempoEstimado);

        // Precio
        String precioFormateado = String.format("$%,d", (int) precio);
        tvPrecio.setText(precioFormateado);

        // Seleccionar método de pago por defecto (Efectivo)
        radioEfectivo.setChecked(true);
    }

    /**
     * Cargar información del usuario y conductor
     */
    private void cargarInformacionUsuarioYConductor() {
        // Usuario (datos del Intent)
        tvUsuario.setText(usuarioNombre);
        tvTelefonoP.setText(usuarioTelefono);

        // Conductor (datos del Intent)
        tvConductor.setText(conductorNombre);
        tvTelefonoC.setText(conductorTelefono);

        // Vehículo (datos del Intent)
        String infoVehiculo = "Vehículo: " + vehiculoPlaca + " - " + vehiculoModelo;
        tvPlaca.setText(infoVehiculo);

        Log.d(TAG, "✓ Información cargada desde Intent");
    }

    /**
     * Validar formulario antes de registrar
     */
    private boolean validarFormulario() {
        if (metodoPago == null || metodoPago.isEmpty()) {
            Toast.makeText(this, "Por favor selecciona un método de pago", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /**
     * Mostrar diálogo de cancelación
     */
    private void mostrarDialogoCancelacion() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Cancelar reserva")
                .setMessage("¿Estás seguro de que quieres cancelar la reserva?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Registrar la reserva en Firebase y enviar notificación al conductor
     */
    private void registrarReserva() {
        String userId = authManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        String estadoReserva = "Por confirmar";

        Log.d(TAG, "Registrando reserva con datos del Intent:");
        Log.d(TAG, "  - Conductor: " + conductorNombre + ", ID: " + conductorId + ", Tel: " + conductorTelefono);
        Log.d(TAG, "  - Vehículo: " + vehiculoPlaca + " - " + vehiculoModelo);
        Log.d(TAG, "  - Usuario: " + usuarioNombre + ", Método Pago: " + metodoPago);

        reservaService.actualizarDisponibilidadAsientos(
                this, horarioId, asientoSeleccionado, origen, destino, tiempoEstimado,
                metodoPago, estadoReserva, vehiculoPlaca, precio, conductorNombre, conductorTelefono,
                new ReservaService.ReservaCallback() {
                    @Override
                    public void onReservaExitosa() {
                        runOnUiThread(() -> {
                            Toast.makeText(ConfirmarReserva.this, "✅ Reserva confirmada exitosamente", Toast.LENGTH_LONG).show();

                            // ✅ ENVIAR NOTIFICACIÓN AL CONDUCTOR
                            enviarNotificacionAlConductor();

                            Intent intent = new Intent(ConfirmarReserva.this, InicioUsuarios.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(ConfirmarReserva.this, "❌ Error al confirmar reserva: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    /**
     * Manejar el botón físico de back
     */
    @Override
    public void onBackPressed() {
        mostrarDialogoCancelacion();
    }

    /**
     * ✅ NOTIFICACIÓN 1: Enviar notificación al conductor sobre la nueva reserva
     */
    private void enviarNotificacionAlConductor() {
        if (conductorId == null || conductorId.equals("conductor_default_id")) {
            Log.w(TAG, "No se puede enviar notificación: ID del conductor no válido");
            return;
        }

        // Formatear fecha y hora
        String fechaHoraCompleta = fechaViaje + " - " + horarioHora;

        // 🔹 NOTIFICACIÓN AL CONDUCTOR
        notificationManager.notificarNuevaReservaAlConductor(
                conductorId,
                usuarioNombre,           // nombre del pasajero
                rutaSeleccionada,        // ruta
                fechaHoraCompleta,       // fecha y hora
                asientoSeleccionado,     // asiento
                precio,                  // precio
                metodoPago               // método de pago
        );

        Log.d(TAG, "📲 Notificación de NUEVA RESERVA enviada al conductor: " + conductorNombre);
    }
}