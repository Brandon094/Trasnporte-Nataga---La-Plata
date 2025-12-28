package com.chopcode.trasnportenataga_laplata.activities.passenger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.activities.passenger.editProfile.EditarPerfil;
import com.chopcode.trasnportenataga_laplata.activities.passenger.profile.PerfilUsuario;
import com.chopcode.trasnportenataga_laplata.adapters.horarios.HorarioPagerAdapter;
import com.chopcode.trasnportenataga_laplata.config.MyApp;
import com.chopcode.trasnportenataga_laplata.managers.AuthManager;
import com.chopcode.trasnportenataga_laplata.models.Horario;
import com.chopcode.trasnportenataga_laplata.models.Usuario;
import com.chopcode.trasnportenataga_laplata.services.reservations.HorarioService;
import com.chopcode.trasnportenataga_laplata.services.reservations.ReservaService;
import com.chopcode.trasnportenataga_laplata.services.user.UserService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InicioUsuarios extends AppCompatActivity {

    // Services y Managers
    private HorarioService horarioService;
    private UserService userService;
    private ReservaService reservaService;
    private AuthManager authManager;

    // Views del nuevo layout
    private TextView tvUserName, tvWelcome, tvReservasCount, tvViajesCount;
    private MaterialButton btnEditarPerfil, btnRefresh;
    private TabLayout tabLayout;
    private ViewPager2 viewPagerHorarios;
    private HorarioPagerAdapter pagerAdapter;

    // Datos
    private List<Horario> listaNataga = new ArrayList<>();
    private List<Horario> listaLaPlata = new ArrayList<>();
    // Almacenar datos usuario
    private Usuario usuarioActual;

    // ✅ NUEVO: Tag para logs
    private static final String TAG = "InicioUsuarios";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "🚀 onCreate - Iniciando actividad principal de usuario");

        // ✅ Registrar evento analítico de inicio de pantalla
        registrarEventoAnalitico("pantalla_inicio_usuario_inicio", null, null);

        setContentView(R.layout.activity_inicio_usuarios);
        Log.d(TAG, "✅ Layout inflado correctamente");

        // Inicializar servicios
        authManager = AuthManager.getInstance();
        horarioService = new HorarioService();
        reservaService = new ReservaService();
        userService = new UserService();
        Log.d(TAG, "✅ Servicios inicializados");

        // Inicializar vistas
        initViews();

        // Configurar listeners
        configurarListeners();

        // Cargar datos del usuario
        cargarDatosUsuario();

        // Configurar ViewPager y TabLayout
        configurarViewPager();

        // Cargar horarios
        cargarHorarios();

        Log.d(TAG, "✅ Configuración completa - Actividad lista");
    }

    private void initViews() {
        Log.d(TAG, "🔧 Inicializando vistas...");

        // Toolbar
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        Log.d(TAG, "✅ Toolbar inicializada");

        // TextViews de información del usuario
        tvUserName = findViewById(R.id.tvUserName);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvReservasCount = findViewById(R.id.tvReservasCount);
        tvViajesCount = findViewById(R.id.tvViajesCount);
        Log.d(TAG, "✅ TextViews inicializados");

        // Botones
        btnEditarPerfil = findViewById(R.id.btnEditarPerfil);
        btnRefresh = findViewById(R.id.btnRefresh);
        Log.d(TAG, "✅ Botones inicializados");

        // TabLayout y ViewPager
        tabLayout = findViewById(R.id.tabLayout);
        viewPagerHorarios = findViewById(R.id.viewPagerHorarios);
        Log.d(TAG, "✅ TabLayout y ViewPager inicializados");

        // Configurar menú de la toolbar
        topAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_perfil && validarLogIn()) {
                Log.d(TAG, "👤 Navegando a PerfilUsuario desde toolbar");

                // ✅ Registrar evento analítico de navegación
                registrarEventoAnalitico("navegar_perfil_usuario_toolbar", null, null);

                Intent intent = new Intent(InicioUsuarios.this, PerfilUsuario.class);
                startActivity(intent);
                return true;
            }
            Log.d(TAG, "ℹ️ Item de menú no manejado: " + item.getItemId());
            return false;
        });

        Log.d(TAG, "✅ Todas las vistas inicializadas correctamente");
    }

    private void configurarViewPager() {
        Log.d(TAG, "🔧 Configurando ViewPager y TabLayout...");

        pagerAdapter = new HorarioPagerAdapter(this, listaNataga, listaLaPlata);
        viewPagerHorarios.setAdapter(pagerAdapter);
        Log.d(TAG, "✅ Adapter del ViewPager configurado");

        // Conectar TabLayout con ViewPager2
        new TabLayoutMediator(tabLayout, viewPagerHorarios,
                (tab, position) -> {
                    if (position == 0) {
                        tab.setText("Natagá → La Plata");
                        Log.d(TAG, "📍 Tab 0 configurado: Natagá → La Plata");
                    } else {
                        tab.setText("La Plata → Natagá");
                        Log.d(TAG, "📍 Tab 1 configurado: La Plata → Natagá");
                    }
                }
        ).attach();

        Log.d(TAG, "✅ ViewPager y TabLayout completamente configurados");
    }

    private void configurarListeners() {
        Log.d(TAG, "🔧 Configurando listeners...");

        // Botón Editar Perfil
        btnEditarPerfil.setOnClickListener(view -> {
            Log.d(TAG, "🎯 Click en botón Editar Perfil");

            // ✅ Registrar evento analítico de botón
            registrarEventoAnalitico("click_editar_perfil", null, null);

            if (validarLogIn()) {
                Log.d(TAG, "👤 Navegando a EditarPerfil");
                Intent intent = new Intent(InicioUsuarios.this, EditarPerfil.class);
                startActivity(intent);
            } else {
                Log.w(TAG, "⚠️ Usuario no logeado - no se puede editar perfil");
            }
        });

        // Botón Actualizar
        btnRefresh.setOnClickListener(view -> {
            Log.d(TAG, "🔄 Click en botón Actualizar");

            // ✅ Registrar evento analítico de actualización
            registrarEventoAnalitico("click_actualizar", null, null);

            cargarHorarios();
            cargarContadoresUsuario(); // Recargar contadores al actualizar
            Toast.makeText(this, "Actualizando información...", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "✅ Solicitud de actualización enviada");
        });

        Log.d(TAG, "✅ Listeners configurados correctamente");
    }

    private void cargarDatosUsuario() {
        Log.d(TAG, "🔍 Cargando datos del usuario...");

        // ✅ Usar MyApp para obtener el usuario actual
        FirebaseUser currentUser = MyApp.getCurrentUser();
        if (currentUser != null) {
            final String userId = currentUser.getUid();
            Log.d(TAG, "👤 UserId encontrado usando MyApp: " + userId);

            // ✅ Registrar evento de carga de datos
            registrarEventoAnalitico("carga_datos_usuario", null, null);

            // Cargar datos completos del usuario desde Firebase
            userService.loadUserData(userId, new UserService.UserDataCallback() {
                @Override
                public void onUserDataLoaded(Usuario usuario) {
                    Log.d(TAG, "✅ Datos de usuario cargados exitosamente");

                    if (usuario != null && usuario.getNombre() != null) {
                        usuarioActual = usuario; // Guardar referencia del usuario
                        tvUserName.setText(usuario.getNombre());
                        tvWelcome.setText("¡Bienvenido, " + usuario.getNombre().split(" ")[0] + "!");

                        Log.d(TAG, "👋 Usuario cargado: " + usuario.getNombre());
                        Log.d(TAG, "   - Email: " + usuario.getEmail());
                        Log.d(TAG, "   - Teléfono: " + usuario.getTelefono());

                        // ✅ Registrar evento de usuario cargado
                        registrarUsuarioCargadoAnalitico(usuario);
                    } else {
                        Log.w(TAG, "⚠️ Datos de usuario incompletos o nulos");
                        MyApp.logError(new Exception("Datos de usuario incompletos para userId: " + userId));
                    }

                    Log.d(TAG, "📊 Cargando contadores de reservas...");
                    cargarContadoresAlternativo(userId);
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "❌ Error cargando datos de usuario: " + error);

                    // ✅ Usar MyApp para logging de errores
                    MyApp.logError(new Exception("Error cargando datos usuario: " + error));

                    cargarContadoresAlternativo(userId);
                }
            });
        } else {
            Log.w(TAG, "⚠️ Usuario no autenticado - no se pueden cargar datos");
        }
    }

    // Método para obtener el usuario actual (público para el adapter)
    public Usuario getUsuarioActual() {
        Log.d(TAG, "🔍 Solicitando usuario actual");
        if (usuarioActual != null) {
            Log.d(TAG, "✅ Usuario actual devuelto: " + usuarioActual.getNombre());
        } else {
            Log.w(TAG, "⚠️ Usuario actual es null");
        }
        return usuarioActual;
    }

    private void cargarContadoresUsuario() {
        Log.d(TAG, "🔄 Recargando contadores de usuario...");

        // ✅ Usar MyApp para obtener el ID del usuario
        String userId = MyApp.getCurrentUserId();
        if (userId != null) {
            Log.d(TAG, "👤 Recargando contadores para userId usando MyApp: " + userId);
            cargarContadoresAlternativo(userId);
        } else {
            Log.w(TAG, "⚠️ No se pueden recargar contadores - usuario no autenticado");
        }
    }

    // Método alternativo para cargar contadores de reservas y viajes
    private void cargarContadoresAlternativo(final String userId) {
        Log.d(TAG, "📊 Cargando contadores alternativos para: " + userId);

        // ✅ Usar MyApp para obtener referencia a la base de datos
        DatabaseReference reservasRef = MyApp.getDatabaseReference("reservas");
        Log.d(TAG, "🔗 Conectando a Firebase Database usando MyApp...");

        reservasRef.orderByChild("usuarioId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d(TAG, "✅ Datos de reservas recibidos - Snapshots: " + snapshot.getChildrenCount());

                        // ✅ Registrar evento de datos cargados
                        registrarEventoAnalitico("reservas_cargadas", (int) snapshot.getChildrenCount(), null);

                        // Usar variables locales en lugar de modificar las del método externo
                        final int reservasCount = contarReservasActivas(snapshot);
                        final int viajesCount = contarViajesCompletados(snapshot);

                        Log.d(TAG, "📈 Contadores calculados:");
                        Log.d(TAG, "   - Reservas activas: " + reservasCount);
                        Log.d(TAG, "   - Viajes completados: " + viajesCount);

                        // ✅ Registrar estadísticas de usuario
                        registrarEstadisticasUsuario(reservasCount, viajesCount);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                actualizarContadores(reservasCount, viajesCount);
                                Log.d(TAG, "✅ Contadores actualizados en UI");
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "❌ Error en Firebase Database: " + error.getMessage());
                        Log.e(TAG, "   - Código: " + error.getCode());
                        Log.e(TAG, "   - Detalles: " + error.getDetails());

                        // ✅ Usar MyApp para logging de errores
                        MyApp.logError(new Exception("DatabaseError contadores: " + error.getMessage()));

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                actualizarContadores(0, 0);
                                Log.w(TAG, "⚠️ Contadores establecidos a 0 por error");
                            }
                        });
                    }
                });
    }

    // Método auxiliar para contar reservas activas
    private int contarReservasActivas(DataSnapshot snapshot) {
        Log.d(TAG, "🔢 Contando reservas activas...");
        int count = 0;
        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
            com.chopcode.trasnportenataga_laplata.models.Reserva reserva =
                    dataSnapshot.getValue(com.chopcode.trasnportenataga_laplata.models.Reserva.class);
            if (reserva != null) {
                String estado = reserva.getEstadoReserva();
                if (estado != null && (estado.equals("Confirmada") || estado.equals("Por confirmar"))) {
                    count++;
                    Log.d(TAG, "   ✅ Reserva activa encontrada: " + reserva.getIdReserva() + " - Estado: " + estado);
                }
            }
        }
        Log.d(TAG, "📋 Total reservas activas: " + count);
        return count;
    }

    // Método auxiliar para contar viajes completados
    private int contarViajesCompletados(DataSnapshot snapshot) {
        Log.d(TAG, "🔢 Contando viajes completados...");
        int count = 0;
        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
            com.chopcode.trasnportenataga_laplata.models.Reserva reserva =
                    dataSnapshot.getValue(com.chopcode.trasnportenataga_laplata.models.Reserva.class);
            if (reserva != null) {
                String estado = reserva.getEstadoReserva();
                if (estado != null && estado.equals("Confirmada")) {
                    count++;
                    Log.d(TAG, "   ✅ Viaje completado encontrado: " + reserva.getIdReserva());
                }
            }
        }
        Log.d(TAG, "📋 Total viajes completados: " + count);
        return count;
    }

    // Versión simplificada sin listener en tiempo real (para evitar complejidad)
    private void actualizarContadores(int reservasCount, int viajesCount) {
        Log.d(TAG, "🔄 Actualizando contadores en UI:");
        Log.d(TAG, "   - Reservas: " + reservasCount);
        Log.d(TAG, "   - Viajes: " + viajesCount);

        tvReservasCount.setText(String.valueOf(reservasCount));
        tvViajesCount.setText(String.valueOf(viajesCount));

        // Opcional: Mostrar mensaje informativo en logs
        if (reservasCount == 0 && viajesCount == 0) {
            Log.i(TAG, "ℹ️ El usuario no tiene reservas activas ni viajes completados");
        } else if (reservasCount > 0) {
            Log.i(TAG, "🎫 Usuario tiene " + reservasCount + " reserva(s) activa(s)");
        } else if (viajesCount > 0) {
            Log.i(TAG, "✈️ Usuario tiene " + viajesCount + " viaje(s) completado(s)");
        }

        Log.d(TAG, "✅ Contadores actualizados correctamente en UI");
    }

    private void cargarHorarios() {
        Log.d(TAG, "🕒 Cargando horarios...");

        horarioService.cargarHorarios(new HorarioService.HorarioCallback() {
            @Override
            public void onHorariosCargados(List<Horario> nataga, List<Horario> laPlata) {
                Log.d(TAG, "✅ Horarios cargados exitosamente:");
                Log.d(TAG, "   - Natagá → La Plata: " + nataga.size() + " horarios");
                Log.d(TAG, "   - La Plata → Natagá: " + laPlata.size() + " horarios");

                // ✅ Registrar evento de horarios cargados
                registrarEventoHorariosCargados(nataga.size(), laPlata.size());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listaNataga.clear();
                        listaLaPlata.clear();

                        listaNataga.addAll(nataga);
                        listaLaPlata.addAll(laPlata);

                        Log.d(TAG, "🔄 Actualizando adaptador del ViewPager...");

                        // Actualizar el adaptador del ViewPager
                        if (pagerAdapter != null) {
                            pagerAdapter.actualizarDatos(listaNataga, listaLaPlata);
                            Log.d(TAG, "✅ Adaptador del ViewPager actualizado");
                        } else {
                            Log.e(TAG, "❌ pagerAdapter es null - no se puede actualizar");
                            MyApp.logError(new Exception("pagerAdapter es null en InicioUsuarios"));
                        }

                        Toast.makeText(InicioUsuarios.this,
                                "Horarios actualizados: " + (listaNataga.size() + listaLaPlata.size()) + " total",
                                Toast.LENGTH_SHORT).show();

                        Log.d(TAG, "✅ Horarios completamente cargados y mostrados");
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error cargando horarios: " + error);

                // ✅ Usar MyApp para logging de errores
                MyApp.logError(new Exception("Error cargando horarios: " + error));

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(InicioUsuarios.this, "Error al cargar horarios: " + error, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "❌ Error mostrado al usuario");
                    }
                });
            }
        });
    }

    private boolean validarLogIn() {
        Log.d(TAG, "🔐 Validando login...");
        if (!authManager.isUserLoggedIn()) {
            Log.w(TAG, "⚠️ Usuario no logeado - redirigiendo a login");

            // ✅ Registrar evento de validación fallida
            registrarEventoAnalitico("validacion_login_fallida", null, null);

            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show();
            authManager.redirectToLogin(this);
            return false;
        }
        Log.d(TAG, "✅ Usuario validado correctamente");
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "📱 onResume - Actividad en primer plano");

        // ✅ Registrar evento analítico de resumen
        registrarEventoAnalitico("pantalla_inicio_usuario_resume", null, null);

        // Actualizar datos cuando la actividad se reanude
        if (authManager.isUserLoggedIn()) {
            Log.d(TAG, "🔄 Recargando datos en onResume...");
            cargarDatosUsuario();
            cargarHorarios();
        } else {
            Log.w(TAG, "⚠️ Usuario no logeado en onResume");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "📱 onStart - Actividad visible");
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

            if (count != null) {
                params.put("count", count);
            }
            if (count2 != null) {
                params.put("count2", count2);
            }

            params.put("timestamp", System.currentTimeMillis());
            params.put("pantalla", "InicioUsuarios");

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

            MyApp.logEvent("usuario_cargado_inicio", params);
            Log.d(TAG, "📊 Usuario cargado registrado en analytics");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando usuario cargado: " + e.getMessage());
        }
    }

    /**
     * ✅ MÉTODO AUXILIAR: Registrar estadísticas de usuario usando MyApp
     */
    private void registrarEstadisticasUsuario(int reservasActivas, int viajesCompletados) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("user_id", MyApp.getCurrentUserId());
            params.put("reservas_activas", reservasActivas);
            params.put("viajes_completados", viajesCompletados);
            params.put("timestamp", System.currentTimeMillis());

            MyApp.logEvent("estadisticas_usuario", params);
            Log.d(TAG, "📊 Estadísticas de usuario registradas");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando estadísticas usuario: " + e.getMessage());
        }
    }

    /**
     * ✅ MÉTODO AUXILIAR: Registrar horarios cargados usando MyApp
     */
    private void registrarEventoHorariosCargados(int horariosNataga, int horariosLaPlata) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("user_id", MyApp.getCurrentUserId());
            params.put("horarios_nataga", horariosNataga);
            params.put("horarios_laplata", horariosLaPlata);
            params.put("total_horarios", horariosNataga + horariosLaPlata);
            params.put("timestamp", System.currentTimeMillis());

            MyApp.logEvent("horarios_cargados", params);
            Log.d(TAG, "📊 Horarios cargados registrados en analytics");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando horarios cargados: " + e.getMessage());
        }
    }
}