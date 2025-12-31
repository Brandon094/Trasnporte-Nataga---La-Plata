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
import com.chopcode.trasnportenataga_laplata.managers.auths.AuthManager;
import com.chopcode.trasnportenataga_laplata.managers.notificactions.NotificationManager;
import com.chopcode.trasnportenataga_laplata.services.reservations.ReservaService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.Map;

public class ConfirmarReservaActivity extends AppCompatActivity {

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

    // Handler para timeouts
    private Handler timeoutHandler;

    private static final String TAG = "ConfirmarReserva";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Registrar evento analítico de inicio de pantalla
        registrarEventoAnalitico("pantalla_confirmar_reserva_inicio", null, null);

        setContentView(R.layout.activity_confirmar_reserva);

        // ✅ Inicializar servicios CON CONTEXTO usando MyApp para el ID de usuario
        reservaService = new ReservaService();
        authManager = AuthManager.getInstance();
        //notificationManager = NotificationManager.getInstance(this); // ✅ Pasar contexto
        notificationManager = null; // Temporalmente deshabilitado
        timeoutHandler = new Handler();

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ✅ Limpiar todos los callbacks del handler para evitar memory leaks
        if (timeoutHandler != null) {
            timeoutHandler.removeCallbacksAndMessages(null);
        }

        // ✅ Registrar evento de destrucción
        registrarEventoAnalitico("pantalla_confirmar_reserva_destroy", null, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "📱 onResume - Actividad en primer plano");

        // ✅ Registrar evento analítico de resumen
        registrarEventoAnalitico("pantalla_confirmar_reserva_resume", null, null);
    }

    /**
     * Recibir TODOS los datos del Intent
     */
    private void recibirDatosIntent() {
        Intent intent = getIntent();

        // ✅ Registrar evento de recepción de datos
        registrarEventoAnalitico("datos_recibidos_confirmar_reserva", null, null);

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
        if (conductorNombre == null) conductorNombre = "N/A";
        if (conductorTelefono == null) conductorTelefono = "N/A";
        if (vehiculoPlaca == null) vehiculoPlaca = "N/A";
        if (vehiculoModelo == null) vehiculoModelo = "N/A";
        if (usuarioNombre == null) usuarioNombre = "N/A";
        if (usuarioTelefono == null) usuarioTelefono = "N/A";
        if (tiempoEstimado == null) tiempoEstimado = "N/A";
        if (conductorId == null) conductorId = "N/A";

        metodoPago = "Efectivo"; // Por defecto

        // ✅ Registrar detalles de los datos recibidos
        registrarDatosRecibidosAnalitico();

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
            // ✅ Registrar evento de navegación en toolbar
            registrarEventoAnalitico("click_toolbar_navigation", null, null);
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

            // ✅ Registrar evento de cambio de método de pago
            registrarEventoAnalitico("metodo_pago_seleccionado", null, metodoPago.equals("Efectivo") ? 1 : 2);

            Log.d(TAG, "Método de pago seleccionado: " + metodoPago);
        });

        // Botón Confirmar Reserva
        btnConfirmarReserva.setOnClickListener(v -> {
            // ✅ Registrar evento de click en confirmar
            registrarEventoAnalitico("click_confirmar_reserva", asientoSeleccionado, null);

            if (validarFormulario()) {
                registrarReserva();
            }
        });

        // Botón Cancelar
        btnCancelar.setOnClickListener(v -> {
            // ✅ Registrar evento de click en cancelar
            registrarEventoAnalitico("click_cancelar_reserva", asientoSeleccionado, null);

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

        // ✅ Registrar evento de información cargada
        registrarEventoAnalitico("informacion_basica_cargada", asientoSeleccionado, null);
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

        // ✅ Registrar evento de información de usuario y conductor cargada
        registrarInfoUsuarioConductorAnalitico();

        Log.d(TAG, "✓ Información cargada desde Intent");
    }

    /**
     * Validar formulario antes de registrar
     */
    private boolean validarFormulario() {
        if (metodoPago == null || metodoPago.isEmpty()) {
            Toast.makeText(this, "Por favor selecciona un método de pago", Toast.LENGTH_SHORT).show();

            // ✅ Registrar evento de validación fallida
            registrarEventoAnalitico("validacion_fallida_sin_metodo_pago", null, null);

            return false;
        }

        // ✅ Registrar evento de validación exitosa
        registrarEventoAnalitico("validacion_exitosa_confirmar", asientoSeleccionado, null);

        return true;
    }

    /**
     * Mostrar diálogo de cancelación
     */
    private void mostrarDialogoCancelacion() {
        // ✅ Registrar evento de diálogo mostrado
        registrarEventoAnalitico("dialogo_cancelacion_mostrado", asientoSeleccionado, null);

        new android.app.AlertDialog.Builder(this)
                .setTitle("Cancelar reserva")
                .setMessage("¿Estás seguro de que quieres cancelar la reserva?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    // ✅ Registrar evento de confirmación de cancelación
                    registrarEventoAnalitico("cancelacion_reserva_confirmada", asientoSeleccionado, null);

                    finish();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // ✅ Registrar evento de rechazo de cancelación
                    registrarEventoAnalitico("cancelacion_reserva_rechazada", asientoSeleccionado, null);

                    dialog.dismiss();
                })
                .show();
    }

    /**
     * ✅ MÉTODO MEJORADO: Registrar la reserva con mejor manejo de errores usando MyApp
     */
    private void registrarReserva() {
        // ✅ Usar MyApp para obtener el ID del usuario
        String userId = MyApp.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();

            // ✅ Registrar evento de error
            registrarEventoAnalitico("error_usuario_no_autenticado", null, null);

            return;
        }

        String estadoReserva = "Por confirmar";

        Log.d(TAG, "Registrando reserva con datos del Intent:");
        Log.d(TAG, "  - Conductor: " + conductorNombre + ", ID: " + conductorId + ", Tel: " + conductorTelefono);

        // ✅ Registrar evento de inicio de registro
        registrarEventoAnalitico("registro_reserva_inicio", asientoSeleccionado, null);

        // Deshabilitar botón para evitar múltiples clics
        btnConfirmarReserva.setEnabled(false);
        btnConfirmarReserva.setText("Procesando...");

        // ✅ TIMEOUT para evitar que se quede bloqueado
        Runnable timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    Log.w(TAG, "⏰ TIMEOUT - La operación está tomando demasiado tiempo");

                    // ✅ Registrar evento de timeout
                    registrarEventoAnalitico("timeout_registro_reserva", asientoSeleccionado, null);

                    runOnUiThread(() -> {
                        if (!isFinishing()) {
                            btnConfirmarReserva.setEnabled(true);
                            btnConfirmarReserva.setText("Confirmar Reserva");
                            Toast.makeText(ConfirmarReservaActivity.this,
                                    "La operación está tardando más de lo esperado. Verifica tu conexión.",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        };

        // Establecer timeout de 15 segundos
        timeoutHandler.postDelayed(timeoutRunnable, 15000);

        reservaService.actualizarDisponibilidadAsientos(
                this, horarioId, asientoSeleccionado, origen, destino, tiempoEstimado,
                metodoPago, estadoReserva, vehiculoPlaca, precio, conductorNombre, conductorTelefono,
                new ReservaService.ReservaCallback() {
                    @Override
                    public void onReservaExitosa() {
                        // ✅ Cancelar el timeout
                        timeoutHandler.removeCallbacks(timeoutRunnable);

                        runOnUiThread(() -> {
                            if (!isFinishing()) {
                                Toast.makeText(ConfirmarReservaActivity.this, "✅ Reserva creada exitosamente", Toast.LENGTH_LONG).show();

                                // ✅ Registrar evento de reserva exitosa
                                registrarEventoAnalitico("reserva_registrada_exitosa", asientoSeleccionado, null);
                                registrarReservaExitosaAnalitico();

                                // ✅ NOTIFICACIONES DESHABILITADAS TEMPORALMENTE
                                Log.d(TAG, "📢 NOTIFICACIONES DESHABILITADAS - Navegando directamente a inicio");

                                // ✅ Navegar inmediatamente sin esperar notificaciones
                                navegarAInicioUsuarios();

                                // ✅ ENVIAR NOTIFICACIÓN AL CONDUCTOR CON MANEJO DE ERRORES MEJORADO
                                //enviarNotificacionAlConductor();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        // ✅ Cancelar el timeout
                        timeoutHandler.removeCallbacks(timeoutRunnable);

                        runOnUiThread(() -> {
                            if (!isFinishing()) {
                                btnConfirmarReserva.setEnabled(true);
                                btnConfirmarReserva.setText("Confirmar Reserva");

                                // ✅ Usar MyApp para logging de errores
                                MyApp.logError(new Exception("Error registrando reserva: " + error));

                                // ✅ Registrar evento de error
                                registrarEventoAnalitico("error_registro_reserva", asientoSeleccionado, null);

                                Toast.makeText(ConfirmarReservaActivity.this, "❌ Error al confirmar reserva: " + error, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
    }

    /**
     * ✅ NOTIFICACIÓN MEJORADA: Con timeout y mejor manejo de errores usando MyApp
     */
    private void enviarNotificacionAlConductor() {
        if (conductorId == null || conductorId.isEmpty()) {
            Log.w(TAG, "No se puede enviar notificación: ID del conductor no válido");

            // ✅ Registrar evento de error
            registrarEventoAnalitico("error_conductor_id_invalido", asientoSeleccionado, null);

            // ✅ NAVEGAR DE TODAS FORMAS AUNQUE FALLE LA NOTIFICACIÓN
            navegarAInicioUsuarios();
            return;
        }

        String fechaHoraCompleta = fechaViaje + " - " + horarioHora;

        // Mostrar progreso
        btnConfirmarReserva.setText("Enviando notificación...");

        // ✅ Registrar evento de inicio de notificación
        registrarEventoAnalitico("notificacion_conductor_inicio", asientoSeleccionado, null);

        // ✅ TIMEOUT para notificación
        Runnable notificationTimeout = new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    Log.w(TAG, "⏰ TIMEOUT NOTIFICACIÓN - Envío de notificación tardando demasiado");

                    // ✅ Registrar evento de timeout
                    registrarEventoAnalitico("timeout_notificacion_conductor", asientoSeleccionado, null);

                    runOnUiThread(() -> {
                        if (!isFinishing()) {
                            mostrarErrorNotificacion("El envío de notificación está tardando demasiado. La reserva fue creada exitosamente.");
                        }
                    });
                }
            }
        };
        timeoutHandler.postDelayed(notificationTimeout, 10000);

        // ✅ NOTIFICACIÓN CON MANEJO DE ÉXITO/ERROR MEJORADO
        notificationManager.notificarNuevaReservaAlConductor(
                conductorId,
                usuarioNombre,
                rutaSeleccionada,
                fechaHoraCompleta,
                asientoSeleccionado,
                precio,
                metodoPago,
                new NotificationManager.NotificationCallback() {
                    @Override
                    public void onSuccess() {
                        // ✅ Cancelar timeout de notificación
                        timeoutHandler.removeCallbacks(notificationTimeout);

                        runOnUiThread(() -> {
                            if (!isFinishing()) {
                                Log.d(TAG, "✅ Notificación enviada exitosamente al conductor");

                                // ✅ Registrar evento de notificación exitosa
                                registrarEventoAnalitico("notificacion_conductor_exitosa", asientoSeleccionado, null);

                                Toast.makeText(ConfirmarReservaActivity.this, "✅ Reserva confirmada y notificación enviada", Toast.LENGTH_LONG).show();
                                navegarAInicioUsuarios();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        // ✅ Cancelar timeout de notificación
                        timeoutHandler.removeCallbacks(notificationTimeout);

                        runOnUiThread(() -> {
                            if (!isFinishing()) {
                                Log.e(TAG, "❌ Error enviando notificación: " + error);

                                // ✅ Usar MyApp para logging de errores
                                MyApp.logError(new Exception("Error enviando notificación conductor: " + error));

                                // ✅ Registrar evento de error en notificación
                                registrarEventoAnalitico("error_notificacion_conductor", asientoSeleccionado, null);

                                mostrarErrorNotificacion("Error enviando notificación al conductor: " + error);
                            }
                        });
                    }
                });
    }

    /**
     * ✅ MÉTODO NUEVO: Navegar a inicio de usuarios
     */
    private void navegarAInicioUsuarios() {
        Log.d(TAG, "🏠 Navegando a InicioUsuarios");

        // ✅ Registrar evento de navegación
        registrarEventoAnalitico("navegacion_inicio_usuarios", asientoSeleccionado, null);

        try {
            Intent intent = new Intent(ConfirmarReservaActivity.this, InicioUsuariosActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "❌ Error navegando a InicioUsuarios: " + e.getMessage());

            // ✅ Usar MyApp para logging de errores
            MyApp.logError(e);

            // ✅ Registrar evento de error en navegación
            registrarEventoAnalitico("error_navegacion_inicio", asientoSeleccionado, null);

            // Si hay error, al menos finalizar esta actividad
            finish();
        }
    }

    /**
     * ✅ MOSTRAR ERROR DE NOTIFICACIÓN MEJORADO
     */
    private void mostrarErrorNotificacion(String mensaje) {
        try {
            // ✅ Registrar evento de diálogo de error
            registrarEventoAnalitico("dialogo_error_notificacion", asientoSeleccionado, null);

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Información de Notificación")
                    .setMessage(mensaje + "\n\nLa reserva se creó exitosamente, pero hubo un problema con la notificación al conductor.")
                    .setPositiveButton("Continuar", (dialog, which) -> {
                        // ✅ Registrar evento de confirmación de diálogo
                        registrarEventoAnalitico("confirmacion_dialogo_error", asientoSeleccionado, null);

                        navegarAInicioUsuarios();
                    })
                    .setCancelable(false) // ✅ Evitar que el usuario cierre el diálogo sin acción
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "❌ Error mostrando diálogo: " + e.getMessage());

            // ✅ Usar MyApp para logging de errores
            MyApp.logError(e);

            // ✅ Registrar evento de error en diálogo
            registrarEventoAnalitico("error_mostrando_dialogo", asientoSeleccionado, null);

            // Si falla el diálogo, navegar directamente
            navegarAInicioUsuarios();
        }
    }

    /**
     * Manejar el botón físico de back
     */
    @Override
    public void onBackPressed() {
        // ✅ Registrar evento de botón físico back
        registrarEventoAnalitico("boton_back_fisico", asientoSeleccionado, null);

        mostrarDialogoCancelacion();
    }

    /**
     * ✅ MÉTODO AUXILIAR: Registrar eventos analíticos usando MyApp
     */
    private void registrarEventoAnalitico(String evento, Integer asiento, Integer tipo) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("user_id", MyApp.getCurrentUserId());
            params.put("pantalla", "ConfirmarReserva");

            if (asiento != null) {
                params.put("asiento", asiento);
            }
            if (tipo != null) {
                params.put("tipo", tipo);
            }

            params.put("ruta", rutaSeleccionada != null ? rutaSeleccionada : "N/A");
            params.put("conductor", conductorNombre != null ? conductorNombre : "N/A");
            params.put("metodo_pago", metodoPago != null ? metodoPago : "N/A");
            params.put("precio", precio);
            params.put("timestamp", System.currentTimeMillis());

            MyApp.logEvent(evento, params);
            Log.d(TAG, "📊 Evento analítico registrado: " + evento);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando evento analítico: " + e.getMessage());
        }
    }

    /**
     * ✅ MÉTODO AUXILIAR: Registrar detalles de datos recibidos usando MyApp
     */
    private void registrarDatosRecibidosAnalitico() {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("user_id", MyApp.getCurrentUserId());
            params.put("asiento", asientoSeleccionado);
            params.put("ruta", rutaSeleccionada != null ? rutaSeleccionada : "N/A");
            params.put("conductor", conductorNombre != null ? conductorNombre : "N/A");
            params.put("conductor_id", conductorId != null ? conductorId : "N/A");
            params.put("vehiculo_placa", vehiculoPlaca != null ? vehiculoPlaca : "N/A");
            params.put("precio", precio);
            params.put("metodo_pago", metodoPago);
            params.put("timestamp", System.currentTimeMillis());
            params.put("pantalla", "ConfirmarReserva");

            MyApp.logEvent("datos_recibidos_detalle_confirmar", params);
            Log.d(TAG, "📊 Detalles de datos recibidos registrados en analytics");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando detalles de datos: " + e.getMessage());
        }
    }

    /**
     * ✅ MÉTODO AUXILIAR: Registrar información de usuario y conductor usando MyApp
     */
    private void registrarInfoUsuarioConductorAnalitico() {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("user_id", MyApp.getCurrentUserId());
            params.put("usuario_nombre", usuarioNombre != null ? usuarioNombre : "N/A");
            params.put("conductor_nombre", conductorNombre != null ? conductorNombre : "N/A");
            params.put("conductor_telefono", conductorTelefono != null ? conductorTelefono : "N/A");
            params.put("vehiculo_placa", vehiculoPlaca != null ? vehiculoPlaca : "N/A");
            params.put("timestamp", System.currentTimeMillis());
            params.put("pantalla", "ConfirmarReserva");

            MyApp.logEvent("info_usuario_conductor_cargada", params);
            Log.d(TAG, "📊 Información de usuario y conductor registrada en analytics");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando info usuario/conductor: " + e.getMessage());
        }
    }

    /**
     * ✅ MÉTODO AUXILIAR: Registrar reserva exitosa usando MyApp
     */
    private void registrarReservaExitosaAnalitico() {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("user_id", MyApp.getCurrentUserId());
            params.put("asiento", asientoSeleccionado);
            params.put("ruta", rutaSeleccionada != null ? rutaSeleccionada : "N/A");
            params.put("conductor", conductorNombre != null ? conductorNombre : "N/A");
            params.put("metodo_pago", metodoPago);
            params.put("precio", precio);
            params.put("timestamp", System.currentTimeMillis());
            params.put("pantalla", "ConfirmarReserva");

            MyApp.logEvent("reserva_completa_detalle", params);
            Log.d(TAG, "📊 Detalles de reserva completa registrados en analytics");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando detalles de reserva: " + e.getMessage());
        }
    }
}