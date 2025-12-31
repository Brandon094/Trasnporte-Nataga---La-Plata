package com.chopcode.trasnportenataga_laplata.activities.passenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.activities.passenger.editProfile.EditarPerfilActivity;
import com.chopcode.trasnportenataga_laplata.activities.passenger.profile.PerfilUsuarioActivity;
import com.chopcode.trasnportenataga_laplata.adapters.horarios.HorarioPagerAdapter;
import com.chopcode.trasnportenataga_laplata.config.MyApp;
import com.chopcode.trasnportenataga_laplata.managers.analytics.DashboardAnalyticsHelper;
import com.chopcode.trasnportenataga_laplata.managers.dashboard.passenger.DashboardUIManager;
import com.chopcode.trasnportenataga_laplata.managers.dashboard.passenger.ScheduleManager;
import com.chopcode.trasnportenataga_laplata.managers.dashboard.passenger.UserDashboardManager;
import com.chopcode.trasnportenataga_laplata.models.Horario;
import com.chopcode.trasnportenataga_laplata.models.Usuario;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

public class InicioUsuariosActivity extends AppCompatActivity implements
        UserDashboardManager.DashboardListener,
        ScheduleManager.ScheduleListener,
        DashboardUIManager.UIActionsListener {

    private static final String TAG = "InicioUsuarios";

    // Managers
    private DashboardAnalyticsHelper analyticsHelper;
    private UserDashboardManager dashboardManager;
    private ScheduleManager scheduleManager;
    private DashboardUIManager uiManager;

    // UI Elements
    private TabLayout tabLayout;
    private ViewPager2 viewPagerHorarios;
    private HorarioPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "🚀 onCreate - Iniciando actividad principal de usuario");

        // Inicializar managers
        initializeManagers();

        setContentView(R.layout.activity_inicio_usuarios);

        // Inicializar vistas
        initializeViews();

        // Configurar UI
        configureUI();

        // Cargar datos iniciales
        loadInitialData();
    }

    private void initializeManagers() {
        analyticsHelper = new DashboardAnalyticsHelper();

        dashboardManager = new UserDashboardManager(this, analyticsHelper);
        dashboardManager.setDashboardListener(this);

        scheduleManager = new ScheduleManager(analyticsHelper);
        scheduleManager.setScheduleListener(this);

        uiManager = new DashboardUIManager(analyticsHelper);
        uiManager.setUIActionsListener(this);
    }

    private void initializeViews() {
        // Toolbar
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);

        // TextViews
        TextView tvUserName = findViewById(R.id.tvUserName);
        TextView tvWelcome = findViewById(R.id.tvWelcome);
        TextView tvReservasCount = findViewById(R.id.tvReservasCount);
        TextView tvViajesCount = findViewById(R.id.tvViajesCount);

        // Botones
        MaterialButton btnEditarPerfil = findViewById(R.id.btnEditarPerfil);
        MaterialButton btnRefresh = findViewById(R.id.btnRefresh);

        // TabLayout y ViewPager
        tabLayout = findViewById(R.id.tabLayout);
        viewPagerHorarios = findViewById(R.id.viewPagerHorarios);

        // Configurar UI Manager
        uiManager.setViewReferences(
                tvUserName, tvWelcome, tvReservasCount, tvViajesCount,
                btnEditarPerfil, btnRefresh
        );
        uiManager.setupToolbar(topAppBar);

        // Inicializar ViewPager
        pagerAdapter = new HorarioPagerAdapter(
                this,
                scheduleManager.getNatagaSchedules(),
                scheduleManager.getLaPlataSchedules()
        );
        viewPagerHorarios.setAdapter(pagerAdapter);
        uiManager.setupTabLayout(tabLayout, viewPagerHorarios);
    }

    private void configureUI() {
        uiManager.setupButtonListeners();
    }

    private void loadInitialData() {
        dashboardManager.loadUserData();
        scheduleManager.loadSchedules();
    }

    // Implementación de UserDashboardManager.DashboardListener
    @Override
    public void onUserDataLoaded(Usuario usuario) {
        runOnUiThread(() -> {
            uiManager.updateUserInfo(usuario);
        });
    }

    @Override
    public void onUserDataError(String error) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Error cargando datos: " + error, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onCountersLoaded(int reservasCount, int viajesCount) {
        runOnUiThread(() -> {
            uiManager.updateCounters(reservasCount, viajesCount);

            // Mostrar notificación solo si hay cambios importantes
            if (reservasCount > 0) {
                Log.d(TAG, "📊 Contadores actualizados: " + reservasCount + " reservas activas");
            }
        });
    }

    @Override
    public void onCountersError(String error) {
        runOnUiThread(() -> {
            uiManager.updateCounters(0, 0);
            Log.e(TAG, "❌ Error en contadores: " + error);
        });
    }

    // Implementación de ScheduleManager.ScheduleListener
    @Override
    public void onSchedulesLoaded(List<Horario> nataga, List<Horario> laPlata) {
        runOnUiThread(() -> {
            pagerAdapter.actualizarDatos(nataga, laPlata);
            Toast.makeText(this,
                    "Horarios actualizados: " + scheduleManager.getTotalSchedules() + " total",
                    Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onSchedulesError(String error) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Error al cargar horarios: " + error, Toast.LENGTH_SHORT).show();
        });
    }

    // Implementación de DashboardUIManager.UIActionsListener
    @Override
    public void onEditProfileClicked() {
        if (validateLogin()) {
            Intent intent = new Intent(this, EditarPerfilActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onRefreshClicked() {
        uiManager.showRefreshMessage();
        dashboardManager.refreshData();
        scheduleManager.loadSchedules();
    }

    @Override
    public void onProfileMenuItemClicked() {
        if (validateLogin()) {
            Intent intent = new Intent(this, PerfilUsuarioActivity.class);
            startActivity(intent);
        }
    }

    private boolean validateLogin() {
        if (!MyApp.isUserLoggedIn()) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show();
            // Redirigir a login si es necesario
            return false;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        analyticsHelper.logScreenResume();

        if (MyApp.isUserLoggedIn()) {
            // Los contadores ya están en tiempo real, solo recargar si es necesario
            scheduleManager.loadSchedules();

            // Opcional: forzar una actualización de datos del usuario
            dashboardManager.refreshData();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "⏸️ onPause - Pausando actividad");
        // Los listeners en tiempo real siguen activos en background
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "⏹️ onStop - Deteniendo actividad");
        // Considerar si quieres mantener los listeners o no
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "🗑️ onDestroy - Destruyendo actividad");

        // Limpiar recursos para evitar memory leaks
        if (dashboardManager != null) {
            dashboardManager.cleanup();
        }
    }

    public Usuario getUsuarioActual() {
        return dashboardManager.getUsuarioActual();
    }
}