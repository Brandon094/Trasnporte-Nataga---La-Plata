package com.chopcode.trasnportenataga_laplata.activities.passenger;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.adapters.HistorialUsuarioAdapter;
import com.chopcode.trasnportenataga_laplata.managers.AuthManager;
import com.chopcode.trasnportenataga_laplata.models.Reserva;
import com.chopcode.trasnportenataga_laplata.services.reservations.ReservaService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistorialReservas extends AppCompatActivity {

    // ✅ NUEVO: Tag para logs
    private static final String TAG = "HistorialReservas";

    // Views
    private MaterialToolbar toolbar;
    private ChipGroup chipGroupFiltros;
    private RecyclerView recyclerHistorial;
    private View layoutEmptyState;
    private FloatingActionButton fabActualizar;
    private TextView tvTotalViajes, tvViajesConfirmados, tvViajesCancelados, tvTituloHistorial;

    // Servicios y managers
    private ReservaService reservaService;
    private AuthManager authManager;

    // Adapter y datos
    private HistorialUsuarioAdapter adapter;
    private List<Reserva> listaReservas = new ArrayList<>();
    private List<Reserva> listaFiltrada = new ArrayList<>();

    // Formateador de fechas
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy - HH:mm", new Locale("es", "ES"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "🚀 onCreate - Iniciando actividad de historial de reservas");

        setContentView(R.layout.activity_historial_reservas);
        Log.d(TAG, "✅ Layout inflado correctamente");

        // Inicializar servicios
        reservaService = new ReservaService();
        authManager = AuthManager.getInstance();
        Log.d(TAG, "✅ Servicios inicializados");

        // Inicializar vistas
        initViews();

        // Configurar toolbar
        setupToolbar();

        // Configurar RecyclerView
        setupRecyclerView();

        // Configurar listeners
        setupListeners();

        // Cargar datos del usuario
        cargarHistorialUsuario();

        Log.d(TAG, "✅ Configuración completa - Actividad lista");
    }

    private void initViews() {
        Log.d(TAG, "🔧 Inicializando vistas...");

        // Toolbar
        toolbar = findViewById(R.id.toolbar);

        // ChipGroup de filtros
        chipGroupFiltros = findViewById(R.id.chipGroupFiltros);

        // RecyclerView
        recyclerHistorial = findViewById(R.id.recyclerHistorial);

        // Empty state
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        // FAB
        fabActualizar = findViewById(R.id.fabActualizar);

        // TextViews de estadísticas
        tvTotalViajes = findViewById(R.id.tvTotalViajes);
        tvViajesConfirmados = findViewById(R.id.tvViajesConfirmados);
        tvViajesCancelados = findViewById(R.id.tvViajesCancelados);
        tvTituloHistorial = findViewById(R.id.tvTituloHistorial);

        Log.d(TAG, "✅ Todas las vistas inicializadas correctamente");
    }

    private void setupToolbar() {
        Log.d(TAG, "🔧 Configurando toolbar...");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> {
            Log.d(TAG, "📱 Click en navegación de toolbar");
            onBackPressed();
        });
        Log.d(TAG, "✅ Toolbar configurada correctamente");
    }

    private void setupRecyclerView() {
        Log.d(TAG, "🔧 Configurando RecyclerView...");
        adapter = new HistorialUsuarioAdapter(listaFiltrada);
        recyclerHistorial.setLayoutManager(new LinearLayoutManager(this));
        recyclerHistorial.setAdapter(adapter);
        Log.d(TAG, "✅ RecyclerView configurado correctamente");
    }

    private void setupListeners() {
        Log.d(TAG, "🔧 Configurando listeners...");

        // FAB - Actualizar
        fabActualizar.setOnClickListener(v -> {
            Log.d(TAG, "🔄 Click en FAB Actualizar");
            cargarHistorialUsuario();
            Snackbar.make(v, "Actualizando historial...", Snackbar.LENGTH_SHORT).show();
        });

        // ChipGroup - Filtros
        chipGroupFiltros.setOnCheckedStateChangeListener((group, checkedIds) -> {
            Log.d(TAG, "🎯 Cambio en filtros - IDs seleccionados: " + checkedIds);
            if (checkedIds.isEmpty()) {
                aplicarFiltro("TODOS");
            } else {
                int chipId = checkedIds.get(0);
                aplicarFiltroPorChip(chipId);
            }
        });

        Log.d(TAG, "✅ Listeners configurados correctamente");
    }

    private void aplicarFiltroPorChip(int chipId) {
        String tipoFiltro = "";
        if (chipId == R.id.chipTodos) {
            tipoFiltro = "TODOS";
            Log.d(TAG, "🔍 Aplicando filtro: TODOS");
        } else if (chipId == R.id.chipConfirmados) {
            tipoFiltro = "CONFIRMADOS";
            Log.d(TAG, "🔍 Aplicando filtro: CONFIRMADOS");
        } else if (chipId == R.id.chipCancelados) {
            tipoFiltro = "CANCELADOS";
            Log.d(TAG, "🔍 Aplicando filtro: CANCELADOS");
        } else if (chipId == R.id.chipEsteMes) {
            tipoFiltro = "ESTE_MES";
            Log.d(TAG, "🔍 Aplicando filtro: ESTE_MES");
        } else {
            Log.w(TAG, "⚠️ Chip ID no reconocido: " + chipId);
            tipoFiltro = "TODOS";
        }

        aplicarFiltro(tipoFiltro);
    }

    private void aplicarFiltro(String tipoFiltro) {
        Log.d(TAG, "🔄 Aplicando filtro: " + tipoFiltro);
        Log.d(TAG, "   - Reservas totales: " + listaReservas.size());

        listaFiltrada.clear();

        long tiempoActual = System.currentTimeMillis();
        long unMesAtras = tiempoActual - (30L * 24 * 60 * 60 * 1000); // 30 días atrás

        int contadorCoincidencias = 0;
        for (Reserva reserva : listaReservas) {
            boolean coincide = false;

            switch (tipoFiltro) {
                case "TODOS":
                    coincide = true;
                    break;
                case "CONFIRMADOS":
                    coincide = "confirmado".equalsIgnoreCase(reserva.getEstadoReserva()) ||
                            "Confirmado".equalsIgnoreCase(reserva.getEstadoReserva());
                    break;
                case "CANCELADOS":
                    coincide = "cancelado".equalsIgnoreCase(reserva.getEstadoReserva()) ||
                            "Cancelado".equalsIgnoreCase(reserva.getEstadoReserva());
                    break;
                case "ESTE_MES":
                    coincide = reserva.getFechaReserva() >= unMesAtras;
                    break;
            }

            if (coincide) {
                listaFiltrada.add(reserva);
                contadorCoincidencias++;
            }
        }

        Log.d(TAG, "✅ Filtro aplicado - Coincidencias: " + contadorCoincidencias);
        actualizarVista();
    }

    private void cargarHistorialUsuario() {
        Log.d(TAG, "🔍 Cargando historial del usuario...");

        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "⚠️ Usuario no autenticado - finalizando actividad");
            Snackbar.make(recyclerHistorial, "Debes iniciar sesión", Snackbar.LENGTH_LONG).show();
            finish();
            return;
        }

        String usuarioId = currentUser.getUid();
        Log.d(TAG, "👤 UserId del usuario: " + usuarioId);

        reservaService.obtenerHistorialUsuario(usuarioId, new ReservaService.HistorialCallback() {
            @Override
            public void onHistorialCargado(List<Reserva> reservas) {
                Log.d(TAG, "✅ Historial cargado exitosamente: " + (reservas != null ? reservas.size() : 0) + " reservas");

                runOnUiThread(() -> {
                    listaReservas.clear();
                    if (reservas != null) {
                        listaReservas.addAll(reservas);
                        Log.d(TAG, "📋 Reservas añadidas a lista: " + reservas.size());

                        // Log detallado de reservas cargadas
                        for (Reserva reserva : reservas) {
                            Log.d(TAG, "   - Reserva: " + reserva.getIdReserva() +
                                    " | Estado: " + reserva.getEstadoReserva() +
                                    " | Asiento: " + reserva.getPuestoReservado());
                        }
                    } else {
                        Log.w(TAG, "⚠️ Lista de reservas es null");
                    }

                    // Aplicar filtro por defecto
                    aplicarFiltro("TODOS");
                    actualizarEstadisticas();

                    Snackbar.make(recyclerHistorial, "Historial cargado", Snackbar.LENGTH_SHORT).show();
                    Log.d(TAG, "✅ UI actualizada con historial cargado");
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error cargando historial: " + error);
                runOnUiThread(() -> {
                    Snackbar.make(recyclerHistorial, "Error al cargar historial: " + error, Snackbar.LENGTH_LONG).show();

                    // Mostrar datos de ejemplo en caso de error
                    Log.w(TAG, "⚠️ Cargando datos de ejemplo debido a error");
                    cargarDatosDeEjemplo();
                });
            }
        });
    }

    private void cargarDatosDeEjemplo() {
        Log.d(TAG, "🔄 Cargando datos de ejemplo para testing");

        // Datos de ejemplo para testing
        listaReservas.clear();

        long fechaActual = System.currentTimeMillis();
        long unDia = 24 * 60 * 60 * 1000;

        listaReservas.add(new Reserva(
                "1", authManager.getCurrentUser().getUid(), "horario1", 5,
                "Carlos Rodríguez", "3001234567", "vehiculo1", 12000.0,
                "Natagá", "La Plata", "45 min", "Efectivo",
                "Confirmado", fechaActual - (unDia * 2),
                "Usuario Ejemplo", "3007654321", "usuario@ejemplo.com"
        ));

        listaReservas.add(new Reserva(
                "2", authManager.getCurrentUser().getUid(), "horario2", 3,
                "María García", "3001112233", "vehiculo2", 12000.0,
                "La Plata", "Natagá", "50 min", "Efectivo",
                "Confirmado", fechaActual - (unDia * 5),
                "Usuario Ejemplo", "3007654321", "usuario@ejemplo.com"
        ));

        listaReservas.add(new Reserva(
                "3", authManager.getCurrentUser().getUid(), "horario3", 8,
                "Carlos Rodríguez", "3001234567", "vehiculo1", 12000.0,
                "Natagá", "La Plata", "45 min", "Efectivo",
                "Cancelado", fechaActual - (unDia * 10),
                "Usuario Ejemplo", "3007654321", "usuario@ejemplo.com"
        ));

        Log.d(TAG, "✅ Datos de ejemplo cargados: " + listaReservas.size() + " reservas");
        aplicarFiltro("TODOS");
        actualizarEstadisticas();
    }

    private void actualizarEstadisticas() {
        Log.d(TAG, "📊 Actualizando estadísticas...");

        int total = listaReservas.size();
        int confirmados = 0;
        int cancelados = 0;

        for (Reserva reserva : listaReservas) {
            String estado = reserva.getEstadoReserva();
            if (estado != null) {
                if (estado.equalsIgnoreCase("confirmado")) {
                    confirmados++;
                } else if (estado.equalsIgnoreCase("cancelado")) {
                    cancelados++;
                }
            }
        }

        tvTotalViajes.setText(String.valueOf(total));
        tvViajesConfirmados.setText(String.valueOf(confirmados));
        tvViajesCancelados.setText(String.valueOf(cancelados));

        // Actualizar título con cantidad
        tvTituloHistorial.setText("Historial de Viajes (" + listaFiltrada.size() + ")");

        Log.d(TAG, "✅ Estadísticas actualizadas:");
        Log.d(TAG, "   - Total: " + total);
        Log.d(TAG, "   - Confirmados: " + confirmados);
        Log.d(TAG, "   - Cancelados: " + cancelados);
        Log.d(TAG, "   - Filtrados: " + listaFiltrada.size());
    }

    private void actualizarVista() {
        Log.d(TAG, "🔄 Actualizando vista UI...");

        adapter.actualizarDatos(listaFiltrada);
        actualizarEstadisticas();

        // Mostrar/ocultar empty state
        if (listaFiltrada.isEmpty()) {
            recyclerHistorial.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            Log.d(TAG, "ℹ️ Mostrando empty state - no hay reservas filtradas");
        } else {
            recyclerHistorial.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
            Log.d(TAG, "✅ Mostrando lista de reservas filtradas");
        }
    }

    // Método auxiliar para formatear fecha
    public String formatearFecha(long timestamp) {
        try {
            return dateFormat.format(new Date(timestamp));
        } catch (Exception e) {
            Log.w(TAG, "⚠️ Error formateando fecha: " + e.getMessage());
            return "Fecha no disponible";
        }
    }

    // Método auxiliar para formatear precio
    public String formatearPrecio(double precio) {
        return String.format("$%,.0f", precio);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "📱 onCreateOptionsMenu - Inflando menú");
        getMenuInflater().inflate(R.menu.menu_historial_usuario, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Log.d(TAG, "🎯 Selección de item de menú: " + id);

        if (id == R.id.action_filtrar) {
            Log.d(TAG, "🔍 Acción: Filtrar");
            mostrarDialogoFiltros();
            return true;
        } else if (id == R.id.action_ordenar) {
            Log.d(TAG, "🔍 Acción: Ordenar");
            mostrarDialogoOrdenamiento();
            return true;
        } else if (id == R.id.action_exportar) {
            Log.d(TAG, "🔍 Acción: Exportar");
            exportarHistorial();
            return true;
        } else if (id == R.id.action_compartir) {
            Log.d(TAG, "🔍 Acción: Compartir");
            compartirHistorial();
            return true;
        } else if (id == R.id.action_ayuda) {
            Log.d(TAG, "🔍 Acción: Ayuda");
            mostrarAyuda();
            return true;
        } else if (id == R.id.action_reportar) {
            Log.d(TAG, "🔍 Acción: Reportar");
            reportarProblema();
            return true;
        } else if (id == android.R.id.home) {
            Log.d(TAG, "🔍 Acción: Home/Back");
            onBackPressed();
            return true;
        }

        Log.w(TAG, "⚠️ Item de menú no manejado: " + id);
        return super.onOptionsItemSelected(item);
    }

    private void mostrarDialogoFiltros() {
        Log.d(TAG, "💬 Mostrando diálogo de filtros avanzados");
        Snackbar.make(recyclerHistorial, "Filtros avanzados", Snackbar.LENGTH_SHORT).show();
    }

    private void mostrarDialogoOrdenamiento() {
        Log.d(TAG, "💬 Mostrando diálogo de ordenamiento");
        Snackbar.make(recyclerHistorial, "Ordenar por...", Snackbar.LENGTH_SHORT).show();
    }

    private void exportarHistorial() {
        Log.d(TAG, "💾 Exportando historial");
        Snackbar.make(recyclerHistorial, "Exportando historial...", Snackbar.LENGTH_SHORT).show();
    }

    private void compartirHistorial() {
        Log.d(TAG, "📤 Compartiendo historial");
        Snackbar.make(recyclerHistorial, "Compartir historial", Snackbar.LENGTH_SHORT).show();
    }

    private void mostrarAyuda() {
        Log.d(TAG, "❓ Mostrando ayuda");
        Snackbar.make(recyclerHistorial, "Ayuda del historial", Snackbar.LENGTH_SHORT).show();
    }

    private void reportarProblema() {
        Log.d(TAG, "🐛 Reportando problema");
        Snackbar.make(recyclerHistorial, "Reportar problema", Snackbar.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "📱 onResume - Actividad en primer plano");

        // Recargar datos cuando la actividad se reanude
        if (authManager.isUserLoggedIn()) {
            Log.d(TAG, "🔄 Recargando historial en onResume");
            cargarHistorialUsuario();
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
}