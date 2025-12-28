package com.chopcode.rutago_app.activities.driver.history;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chopcode.rutago_app.R;
import com.chopcode.rutago_app.adapters.historial.HistorialConductorAdapter;
import com.chopcode.rutago_app.managers.AuthManager;
import com.chopcode.rutago_app.models.Reserva;
import com.chopcode.rutago_app.services.reservations.ReservaService;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HistorialConductor extends AppCompatActivity {

    // Views
    private Toolbar toolbar;
    private ChipGroup chipGroupFiltros;
    private Chip chipTodas, chipConfirmadas, chipCanceladas, chipHoy;
    private RecyclerView recyclerHistorial;
    private TextView tvTituloLista, tvTotalReservas, tvConfirmadas, tvCanceladas;
    private View layoutEmptyState;
    private FloatingActionButton fabExportar;

    // Adapters y Data
    private HistorialConductorAdapter reservaAdapter;
    private List<Reserva> listaReservas = new ArrayList<>();
    private List<Reserva> listaFiltrada = new ArrayList<>();

    // Services
    private AuthManager authManager;
    private ReservaService reservaService;

    // Filtros
    private String filtroEstado = "TODAS";
    private String filtroFecha = "TODAS";
    private String textoBusqueda = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_historial_conductor);

        // Inicializar servicios
        authManager = AuthManager.getInstance();
        reservaService = new ReservaService();

        inicializarVistas();
        configurarToolbar();
        configurarChips();
        configurarRecyclerView();
        configurarFAB();
        cargarDatos();
    }

    private void inicializarVistas() {
        toolbar = findViewById(R.id.toolbar);
        chipGroupFiltros = findViewById(R.id.chipGroupFiltros);
        chipTodas = findViewById(R.id.chipTodas);
        chipConfirmadas = findViewById(R.id.chipConfirmadas);
        chipCanceladas = findViewById(R.id.chipCanceladas);
        chipHoy = findViewById(R.id.chipHoy);
        recyclerHistorial = findViewById(R.id.recyclerHistorial);
        tvTituloLista = findViewById(R.id.tvTituloLista);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        fabExportar = findViewById(R.id.fabExportar);

        View cardEstadisticas = findViewById(R.id.cardEstadisticas);
        if (cardEstadisticas != null) {
            tvTotalReservas = cardEstadisticas.findViewById(R.id.tvTotal);
            tvConfirmadas = cardEstadisticas.findViewById(R.id.tvConfirmadas);
            tvCanceladas = cardEstadisticas.findViewById(R.id.tvCanceladas);
        }
    }

    private void configurarToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void configurarChips() {
        // ✅ CORREGIDO: Configurar correctamente los chips
        chipGroupFiltros.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                // Ningún chip seleccionado
                filtroEstado = "TODAS";
                filtroFecha = "TODAS";
            } else {
                int chipId = checkedIds.get(0);

                // Determinar si es un chip de estado o de fecha
                if (chipId == R.id.chipTodas || chipId == R.id.chipConfirmadas || chipId == R.id.chipCanceladas) {
                    // Es un chip de estado
                    if (chipId == R.id.chipConfirmadas) {
                        filtroEstado = "CONFIRMADA";
                    } else if (chipId == R.id.chipCanceladas) {
                        filtroEstado = "CANCELADA";
                    } else {
                        filtroEstado = "TODAS";
                    }
                } else if (chipId == R.id.chipHoy) {
                    // Es un chip de fecha
                    filtroFecha = "HOY";
                }
            }
            aplicarFiltros();
        });
    }

    private void configurarRecyclerView() {
        reservaAdapter = new HistorialConductorAdapter(listaFiltrada, new HistorialConductorAdapter.OnReservaClickListener() {
            @Override
            public void onReservaClick(Reserva reserva) {
                mostrarDetallesReserva(reserva);
            }

            @Override
            public void onVerDetallesClick(Reserva reserva) {
                // Navegar a pantalla de detalles
                Toast.makeText(HistorialConductor.this,
                        "Ver detalles de: " + reserva.getNombre(), Toast.LENGTH_SHORT).show();
            }
        });

        recyclerHistorial.setLayoutManager(new LinearLayoutManager(this));
        recyclerHistorial.setAdapter(reservaAdapter);
    }

    private void configurarFAB() {
        fabExportar.setOnClickListener(v -> exportarHistorial());
    }

    private void cargarDatos() {
        String conductorUID = authManager.getUserId();
        if (conductorUID == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mostrarLoading(true);

        // ✅ USAR UID EN LUGAR DE NOMBRE para mayor precisión
        reservaService.cargarReservasConductorPorUID(conductorUID, "TODAS", new ReservaService.ReservationsCallback() {
            @Override
            public void onReservationsLoaded(List<Reserva> reservas) {
                runOnUiThread(() -> {
                    mostrarLoading(false);
                    listaReservas.clear();
                    listaReservas.addAll(reservas);
                    aplicarFiltros();
                    actualizarEstadisticas();
                    actualizarUI();

                    if (reservas.isEmpty()) {
                        Toast.makeText(HistorialConductor.this,
                                "No hay reservas en tu historial", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    mostrarLoading(false);
                    Toast.makeText(HistorialConductor.this,
                            "Error al cargar historial: " + error, Toast.LENGTH_SHORT).show();
                    mostrarEmptyState();
                });
            }
        });
    }
    private void aplicarFiltros() {
        listaFiltrada.clear();

        for (Reserva reserva : listaReservas) {
            boolean coincideEstado = filtroEstado.equals("TODAS") ||
                    reserva.getEstadoReserva().equalsIgnoreCase(filtroEstado);

            boolean coincideFecha = filtroFecha.equals("TODAS") ||
                    esReservaDeHoy(reserva);

            boolean coincideBusqueda = textoBusqueda.isEmpty() ||
                    contieneTexto(reserva, textoBusqueda);

            if (coincideEstado && coincideFecha && coincideBusqueda) {
                listaFiltrada.add(reserva);
            }
        }

        if (reservaAdapter != null) {
            reservaAdapter.actualizarLista(listaFiltrada);
        }
        actualizarUI();
    }

    private boolean esReservaDeHoy(Reserva reserva) {
        try {
            Calendar hoy = Calendar.getInstance();
            Calendar fechaReserva = Calendar.getInstance();
            fechaReserva.setTimeInMillis(reserva.getFechaReserva());

            return hoy.get(Calendar.YEAR) == fechaReserva.get(Calendar.YEAR) &&
                    hoy.get(Calendar.MONTH) == fechaReserva.get(Calendar.MONTH) &&
                    hoy.get(Calendar.DAY_OF_MONTH) == fechaReserva.get(Calendar.DAY_OF_MONTH);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean contieneTexto(Reserva reserva, String texto) {
        if (texto == null || texto.isEmpty()) return true;

        String textoLower = texto.toLowerCase();
        return (reserva.getNombre() != null && reserva.getNombre().toLowerCase().contains(textoLower)) ||
                (reserva.getTelefono() != null && reserva.getTelefono().toLowerCase().contains(textoLower)) ||
                (reserva.getOrigen() != null && reserva.getOrigen().toLowerCase().contains(textoLower)) ||
                (reserva.getDestino() != null && reserva.getDestino().toLowerCase().contains(textoLower));
    }

    private void actualizarEstadisticas() {
        int total = listaReservas.size();
        int confirmadas = 0;
        int canceladas = 0;
        int pendientes = 0;

        for (Reserva reserva : listaReservas) {
            if (reserva.getEstadoReserva() != null) {
                String estado = reserva.getEstadoReserva().toUpperCase();
                switch (estado) {
                    case "CONFIRMADA":
                    case "CONFIRMADO":
                        confirmadas++;
                        break;
                    case "CANCELADA":
                    case "CANCELADO":
                        canceladas++;
                        break;
                    case "PENDIENTE":
                    case "POR CONFIRMAR":
                    case "PENDIENTE DE CONFIRMACIÓN":
                        pendientes++;
                        break;
                }
            }
        }

        if (tvTotalReservas != null) tvTotalReservas.setText(String.valueOf(total));
        if (tvConfirmadas != null) tvConfirmadas.setText(String.valueOf(confirmadas));
        if (tvCanceladas != null) tvCanceladas.setText(String.valueOf(canceladas));
    }

    private void actualizarUI() {
        if (listaFiltrada.isEmpty()) {
            mostrarEmptyState();
        } else {
            ocultarEmptyState();
        }

        if (tvTituloLista != null) {
            tvTituloLista.setText("Últimas reservas (" + listaFiltrada.size() + ")");
        }
    }

    private void mostrarEmptyState() {
        if (recyclerHistorial != null) recyclerHistorial.setVisibility(View.GONE);
        if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
    }

    private void ocultarEmptyState() {
        if (recyclerHistorial != null) recyclerHistorial.setVisibility(View.VISIBLE);
        if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
    }

    private void mostrarLoading(boolean mostrar) {
        // ✅ IMPLEMENTACIÓN BÁSICA DE LOADING
        if (mostrar) {
            // Mostrar progreso (puedes agregar un ProgressBar)
            if (recyclerHistorial != null) recyclerHistorial.setVisibility(View.GONE);
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
        } else {
            // Ocultar progreso
        }
    }

    private void mostrarDetallesReserva(Reserva reserva) {
        // ✅ MOSTRAR DETALLES EN UN Toast
        String detalles = String.format(
                "Pasajero: %s\nTeléfono: %s\nRuta: %s → %s\nFecha: %s %s\nPuesto: %d\nPrecio: $%d\nEstado: %s",
                reserva.getNombre(),
                reserva.getTelefono(),
                reserva.getOrigen(),
                reserva.getDestino(),
                reserva.getFechaReserva(),
                reserva.getHorarioId(),
                reserva.getPuestoReservado(),
                reserva.getPrecio(),
                reserva.getEstadoReserva()
        );

        Toast.makeText(this, detalles, Toast.LENGTH_LONG).show();
    }

    private void exportarHistorial() {
        Toast.makeText(this, "Exportando historial...", Toast.LENGTH_SHORT).show();
        // ✅ IMPLEMENTAR LÓGICA DE EXPORTACIÓN
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_historial_filtros, menu);

        // Configurar SearchView
        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                searchView.setQueryHint("Buscar por pasajero...");

                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        textoBusqueda = query;
                        aplicarFiltros();
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        textoBusqueda = newText;
                        aplicarFiltros();
                        return true;
                    }
                });
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_filter_date) {
            mostrarDialogoFiltroFecha();
            return true;
        } else if (id == R.id.action_export) {
            exportarHistorial();
            return true;
        } else if (id == R.id.action_clear_filters) {
            limpiarFiltros();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void mostrarDialogoFiltroFecha() {
        Toast.makeText(this, "Filtrar por fecha - Próximamente", Toast.LENGTH_SHORT).show();
    }

    private void limpiarFiltros() {
        if (chipGroupFiltros != null) {
            chipGroupFiltros.clearCheck();
        }
        filtroEstado = "TODAS";
        filtroFecha = "TODAS";
        textoBusqueda = "";
        aplicarFiltros();
        Toast.makeText(this, "Filtros limpiados", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar datos si es necesario
        if (listaReservas.isEmpty()) {
            cargarDatos();
        }
    }
}