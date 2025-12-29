package com.chopcode.trasnportenataga_laplata.activities.passenger.profile;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.activities.passenger.editProfile.EditarPerfil;
import com.chopcode.trasnportenataga_laplata.activities.passenger.history.HistorialReservas;
import com.chopcode.trasnportenataga_laplata.activities.passenger.InicioUsuarios;
import com.chopcode.trasnportenataga_laplata.config.MyApp;
import com.chopcode.trasnportenataga_laplata.managers.auths.AuthManager;
import com.chopcode.trasnportenataga_laplata.models.Usuario;
import com.chopcode.trasnportenataga_laplata.services.user.UserService;
import com.google.android.material.card.MaterialCardView;

import java.util.HashMap;
import java.util.Map;

public class PerfilUsuario extends AppCompatActivity {
    private TextView tvNombre, tvCorreo, tvTelefono;
    private MaterialCardView cardEditarPerfil, cardHistorialReservas, cardVolverInicio, cardCerrarSesion;
    private AuthManager authManager;
    private UserService userService;

    // ✅ NUEVO: Tag para logs
    private static final String TAG = "PerfilUsuario";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "🚀 onCreate - Iniciando actividad de perfil de usuario");

        // ✅ Registrar evento analítico de inicio de pantalla
        registrarEventoAnalitico("pantalla_perfil_usuario_inicio", null, null);

        setContentView(R.layout.activity_perfil_pasajero);
        Log.d(TAG, "✅ Layout inflado correctamente");

        // Inicializar servicios
        authManager = AuthManager.getInstance();
        userService = new UserService();
        Log.d(TAG, "✅ Servicios inicializados");

        // Verificar si el usuario está logueado usando MyApp
        if (!authManager.isUserLoggedIn()) {
            Log.w(TAG, "⚠️ Usuario no autenticado - redirigiendo a login");

            // ✅ Registrar evento de redirección
            registrarEventoAnalitico("redireccion_login_no_autenticado", null, null);

            authManager.redirectToLogin(this);
            finish();
            return;
        }
        Log.d(TAG, "✅ Usuario autenticado validado");

        // Referencias a elementos de la UI
        inicializarVistas();

        // Cargar los datos del usuario desde Firebase
        cargarInfoUsuario();

        // Configurar listeners de botones
        configurarBotones();

        Log.d(TAG, "✅ Configuración completa - Actividad lista");
    }

    private void inicializarVistas() {
        Log.d(TAG, "🔧 Inicializando vistas...");

        // TextViews
        tvNombre = findViewById(R.id.tvNombreUsuario);
        tvCorreo = findViewById(R.id.tvEmail);
        tvTelefono = findViewById(R.id.tvPhone);
        Log.d(TAG, "✅ TextViews inicializados");

        // Cards del grid (nuevo diseño)
        cardEditarPerfil = findViewById(R.id.cardEditarPerfil);
        //cardHistorialReservas = findViewById(R.id.cardHistorialReservas);
        cardVolverInicio = findViewById(R.id.cardVolverInicio);
        cardCerrarSesion = findViewById(R.id.cardCerrarSesion);
        Log.d(TAG, "✅ Cards del grid inicializadas");

        Log.d(TAG, "✅ Todas las vistas inicializadas correctamente");
    }

    private void configurarBotones() {
        Log.d(TAG, "🔧 Configurando listeners de botones...");

        // Botón Editar Perfil
        cardEditarPerfil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "🎯 Click en Editar Perfil - iniciando animación");

                // ✅ Registrar evento analítico
                registrarEventoAnalitico("click_editar_perfil_card", null, null);

                cardEditarPerfil.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                cardEditarPerfil.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                                Log.d(TAG, "👤 Navegando a EditarPerfil");
                                editPerfil();
                            }
                        }).start();
            }
        });

        // Botón Historial Reservas
  /**      cardHistorialReservas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "🎯 Click en Historial Reservas - iniciando animación");

                // ✅ Registrar evento analítico
                registrarEventoAnalitico("click_historial_reservas", null, null);

                cardHistorialReservas.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                cardHistorialReservas.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                                Log.d(TAG, "📋 Navegando a HistorialReservas");
                                historialReservas();
                            }
                        }).start();
            }
        });
*/
        // Botón Volver al Inicio
        cardVolverInicio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "🎯 Click en Volver al Inicio - iniciando animación");

                // ✅ Registrar evento analítico
                registrarEventoAnalitico("click_volver_inicio", null, null);

                cardVolverInicio.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                cardVolverInicio.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                                Log.d(TAG, "🏠 Volviendo a InicioUsuarios");
                                volverAlInicio();
                            }
                        }).start();
            }
        });

        // Botón Cerrar Sesión
        cardCerrarSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "🎯 Click en Cerrar Sesión - iniciando animación");

                // ✅ Registrar evento analítico
                registrarEventoAnalitico("click_cerrar_sesion", null, null);

                cardCerrarSesion.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                cardCerrarSesion.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                                Log.d(TAG, "🚪 Mostrando diálogo de confirmación de cierre de sesión");
                                mostrarDialogoConfirmacion();
                            }
                        }).start();
            }
        });

        Log.d(TAG, "✅ Todos los listeners configurados correctamente");
    }

    /** Método para volver al inicio del pasajero */
    private void volverAlInicio() {
        Log.d(TAG, "🔄 Ejecutando volverAlInicio");

        // ✅ Registrar evento de navegación
        registrarEventoAnalitico("navegar_volver_inicio", null, null);

        Intent intent = new Intent(this, InicioUsuarios.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        Log.d(TAG, "✅ Navegación a inicio completada");
    }

    /** Método para editar perfil */
    private void editPerfil() {
        Log.d(TAG, "🔄 Ejecutando editPerfil");

        // ✅ Registrar evento de navegación
        registrarEventoAnalitico("navegar_editar_perfil", null, null);

        Intent intent = new Intent(this, EditarPerfil.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        Log.d(TAG, "✅ Navegación a EditarPerfil completada");
    }

    /** Método para ver historial de reservas */
    private void historialReservas() {
        Log.d(TAG, "🔄 Ejecutando historialReservas");

        // ✅ Registrar evento de navegación
        registrarEventoAnalitico("navegar_historial_reservas", null, null);

        Intent intent = new Intent(this, HistorialReservas.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        Log.d(TAG, "✅ Navegación a HistorialReservas completada");
    }

    /** Método para mostrar diálogo de confirmación de cierre de sesión */
    private void mostrarDialogoConfirmacion() {
        Log.d(TAG, "💬 Mostrando diálogo de confirmación de cierre de sesión");

        // ✅ Registrar evento de diálogo
        registrarEventoAnalitico("dialogo_cerrar_sesion_mostrado", null, null);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro de que quieres cerrar sesión?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    Log.d(TAG, "✅ Usuario confirmó cierre de sesión");

                    // ✅ Registrar evento de confirmación
                    registrarEventoAnalitico("cerrar_sesion_confirmado", null, null);

                    cerrarSesion();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    Log.d(TAG, "❌ Usuario canceló cierre de sesión");

                    // ✅ Registrar evento de cancelación
                    registrarEventoAnalitico("cerrar_sesion_cancelado", null, null);

                    dialog.dismiss();
                })
                .setIcon(R.drawable.ic_logout)
                .show();
    }

    /**
     * Método para obtener la información del usuario usando loadUserData
     */
    private void cargarInfoUsuario() {
        Log.d(TAG, "🔍 Cargando información del usuario...");

        // ✅ Usar MyApp para obtener el ID del usuario
        String userId = MyApp.getCurrentUserId();

        if (userId == null) {
            Log.e(TAG, "❌ UserId es null - no se pueden cargar datos");

            // ✅ Registrar evento de error
            registrarEventoAnalitico("error_userid_null", null, null);

            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "👤 Cargando datos para userId usando MyApp: " + userId);

        // ✅ Registrar evento de inicio de carga
        registrarEventoAnalitico("carga_datos_usuario_inicio", null, null);

        userService.loadUserData(userId, new UserService.UserDataCallback() {
            @Override
            public void onUserDataLoaded(Usuario usuario) {
                Log.d(TAG, "✅ Datos de usuario cargados exitosamente:");
                Log.d(TAG, "   - Nombre: " + usuario.getNombre());
                Log.d(TAG, "   - Email: " + usuario.getEmail());
                Log.d(TAG, "   - Teléfono: " + usuario.getTelefono());

                // ✅ Registrar evento de carga exitosa
                registrarUsuarioCargadoAnalitico(usuario);

                // Actualizar la UI con los datos del usuario
                runOnUiThread(() -> {
                    if (usuario.getNombre() != null) {
                        tvNombre.setText(usuario.getNombre());
                    } else {
                        tvNombre.setText("Nombre no disponible");
                        Log.w(TAG, "⚠️ Nombre del usuario no disponible");
                    }

                    if (usuario.getTelefono() != null) {
                        tvTelefono.setText(usuario.getTelefono());
                    } else {
                        tvTelefono.setText("Teléfono no disponible");
                        Log.w(TAG, "⚠️ Teléfono del usuario no disponible");
                    }

                    if (usuario.getEmail() != null) {
                        tvCorreo.setText(usuario.getEmail());
                    } else {
                        tvCorreo.setText("Email no disponible");
                        Log.w(TAG, "⚠️ Email del usuario no disponible");
                    }

                    Log.d(TAG, "✅ UI actualizada con datos del usuario");
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error cargando datos de usuario: " + error);

                // ✅ Usar MyApp para logging de errores
                MyApp.logError(new Exception("Error cargando datos usuario perfil: " + error));

                // ✅ Registrar evento de error
                registrarEventoAnalitico("error_carga_datos_usuario", null, null);

                runOnUiThread(() -> {
                    Toast.makeText(PerfilUsuario.this, "Error cargando datos: " + error, Toast.LENGTH_SHORT).show();

                    // Mostrar datos por defecto en caso de error
                    tvNombre.setText("Usuario");
                    tvTelefono.setText("Teléfono no disponible");

                    // ✅ Usar MyApp para obtener email del usuario actual
                    String userEmail = "Email no disponible";
                    if (MyApp.getCurrentUser() != null && MyApp.getCurrentUser().getEmail() != null) {
                        userEmail = MyApp.getCurrentUser().getEmail();
                    }
                    tvCorreo.setText(userEmail);

                    Log.w(TAG, "⚠️ Mostrando datos por defecto debido a error");
                });
            }
        });
    }

    /**
     * Cierra la sesión y redirige a la pantalla de inicio de sesión.
     */
    private void cerrarSesion() {
        Log.d(TAG, "🚪 Cerrando sesión del usuario...");

        // ✅ Registrar evento de cierre de sesión
        registrarEventoAnalitico("cerrar_sesion_ejecutado", null, null);

        authManager.signOut(this);
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "✅ Sesión cerrada exitosamente");
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "📱 onBackPressed - Volviendo atrás");

        // ✅ Registrar evento de navegación con back button
        registrarEventoAnalitico("navegar_back_button", null, null);

        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        Log.d(TAG, "✅ Animación de retroceso aplicada");
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

        // ✅ Registrar evento analítico de resumen
        registrarEventoAnalitico("pantalla_perfil_usuario_resume", null, null);
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

    /**
     * ✅ MÉTODO AUXILIAR: Registrar eventos analíticos usando MyApp
     */
    private void registrarEventoAnalitico(String evento, Integer count, Integer count2) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("user_id", MyApp.getCurrentUserId());
            params.put("pantalla", "PerfilUsuario");

            if (count != null) {
                params.put("count", count);
            }
            if (count2 != null) {
                params.put("count2", count2);
            }

            params.put("timestamp", System.currentTimeMillis());

            MyApp.logEvent(evento, params);
            Log.d(TAG, "📊 Evento analítico registrado: " + evento);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando evento analítico: " + e.getMessage());
        }
    }

    /**
     * ✅ MÉTODO AUXILIAR: Registrar usuario cargado usando MyApp
     */
    private void registrarUsuarioCargadoAnalitico(Usuario usuario) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("user_id", MyApp.getCurrentUserId());
            params.put("user_nombre", usuario.getNombre());
            params.put("user_email", usuario.getEmail());
            params.put("user_telefono", usuario.getTelefono() != null ? usuario.getTelefono() : "N/A");
            params.put("timestamp", System.currentTimeMillis());
            params.put("pantalla", "PerfilUsuario");

            MyApp.logEvent("usuario_cargado_perfil", params);
            Log.d(TAG, "📊 Usuario cargado en perfil registrado en analytics");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando usuario cargado: " + e.getMessage());
        }
    }
}