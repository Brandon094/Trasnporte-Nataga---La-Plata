package com.chopcode.trasnportenataga_laplata.activities;

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
import com.chopcode.trasnportenataga_laplata.services.ReservaService;
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
        setContentView(R.layout.activity_historial_reservas);

        // Inicializar servicios
        reservaService = new ReservaService();
        authManager = AuthManager.getInstance();

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
    }

    private void initViews() {
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
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new HistorialUsuarioAdapter(listaFiltrada);
        recyclerHistorial.setLayoutManager(new LinearLayoutManager(this));
        recyclerHistorial.setAdapter(adapter);
    }

    private void setupListeners() {
        // FAB - Actualizar
        fabActualizar.setOnClickListener(v -> {
            cargarHistorialUsuario();
            Snackbar.make(v, "Actualizando historial...", Snackbar.LENGTH_SHORT).show();
        });

        // ChipGroup - Filtros
        chipGroupFiltros.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                aplicarFiltro("TODOS");
            } else {
                int chipId = checkedIds.get(0);
                aplicarFiltroPorChip(chipId);
            }
        });
    }

    private void aplicarFiltroPorChip(int chipId) {
        if (chipId == R.id.chipTodos) {
            aplicarFiltro("TODOS");
        } else if (chipId == R.id.chipConfirmados) {
            aplicarFiltro("CONFIRMADOS");
        } else if (chipId == R.id.chipCancelados) {
            aplicarFiltro("CANCELADOS");
        } else if (chipId == R.id.chipEsteMes) {
            aplicarFiltro("ESTE_MES");
        }
    }

    private void aplicarFiltro(String tipoFiltro) {
        listaFiltrada.clear();

        long tiempoActual = System.currentTimeMillis();
        long unMesAtras = tiempoActual - (30L * 24 * 60 * 60 * 1000); // 30 días atrás

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
            }
        }

        actualizarVista();
    }

    private void cargarHistorialUsuario() {
        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser == null) {
            Snackbar.make(recyclerHistorial, "Debes iniciar sesión", Snackbar.LENGTH_LONG).show();
            finish();
            return;
        }

        String usuarioId = currentUser.getUid();

        reservaService.obtenerHistorialUsuario(usuarioId, new ReservaService.HistorialCallback() {
            @Override
            public void onHistorialCargado(List<Reserva> reservas) {
                runOnUiThread(() -> {
                    listaReservas.clear();
                    if (reservas != null) {
                        listaReservas.addAll(reservas);
                        Log.d("HistorialReservas", "Reservas cargadas: " + reservas.size());
                    }

                    // Aplicar filtro por defecto
                    aplicarFiltro("TODOS");
                    actualizarEstadisticas();

                    Snackbar.make(recyclerHistorial, "Historial cargado", Snackbar.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e("HistorialReservas", "Error: " + error);
                    Snackbar.make(recyclerHistorial, "Error al cargar historial: " + error, Snackbar.LENGTH_LONG).show();

                    // Mostrar datos de ejemplo en caso de error
                    cargarDatosDeEjemplo();
                });
            }
        });
    }

    private void cargarDatosDeEjemplo() {
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

        aplicarFiltro("TODOS");
        actualizarEstadisticas();
    }

    private void actualizarEstadisticas() {
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
    }

    private void actualizarVista() {
        adapter.actualizarDatos(listaFiltrada);
        actualizarEstadisticas();

        // Mostrar/ocultar empty state
        if (listaFiltrada.isEmpty()) {
            recyclerHistorial.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerHistorial.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    // Método auxiliar para formatear fecha
    public String formatearFecha(long timestamp) {
        try {
            return dateFormat.format(new Date(timestamp));
        } catch (Exception e) {
            return "Fecha no disponible";
        }
    }

    // Método auxiliar para formatear precio
    public String formatearPrecio(double precio) {
        return String.format("$%,.0f", precio);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_historial_usuario, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_filtrar) {
            mostrarDialogoFiltros();
            return true;
        } else if (id == R.id.action_ordenar) {
            mostrarDialogoOrdenamiento();
            return true;
        } else if (id == R.id.action_exportar) {
            exportarHistorial();
            return true;
        } else if (id == R.id.action_compartir) {
            compartirHistorial();
            return true;
        } else if (id == R.id.action_ayuda) {
            mostrarAyuda();
            return true;
        } else if (id == R.id.action_reportar) {
            reportarProblema();
            return true;
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void mostrarDialogoFiltros() {
        Snackbar.make(recyclerHistorial, "Filtros avanzados", Snackbar.LENGTH_SHORT).show();
    }

    private void mostrarDialogoOrdenamiento() {
        Snackbar.make(recyclerHistorial, "Ordenar por...", Snackbar.LENGTH_SHORT).show();
    }

    private void exportarHistorial() {
        Snackbar.make(recyclerHistorial, "Exportando historial...", Snackbar.LENGTH_SHORT).show();
    }

    private void compartirHistorial() {
        Snackbar.make(recyclerHistorial, "Compartir historial", Snackbar.LENGTH_SHORT).show();
    }

    private void mostrarAyuda() {
        Snackbar.make(recyclerHistorial, "Ayuda del historial", Snackbar.LENGTH_SHORT).show();
    }

    private void reportarProblema() {
        Snackbar.make(recyclerHistorial, "Reportar problema", Snackbar.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar datos cuando la actividad se reanude
        if (authManager.isUserLoggedIn()) {
            cargarHistorialUsuario();
        }
    }
}