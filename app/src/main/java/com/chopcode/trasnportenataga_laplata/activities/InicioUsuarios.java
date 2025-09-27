package com.chopcode.trasnportenataga_laplata.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.adapters.HorarioPagerAdapter;
import com.chopcode.trasnportenataga_laplata.managers.AuthManager;
import com.chopcode.trasnportenataga_laplata.models.Horario;
import com.chopcode.trasnportenataga_laplata.models.Usuario;
import com.chopcode.trasnportenataga_laplata.services.HorarioService;
import com.chopcode.trasnportenataga_laplata.services.UserService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class InicioUsuarios extends AppCompatActivity {

    // Services y Managers
    private HorarioService horarioService;
    private UserService userService;
    private AuthManager authManager;

    // Views del nuevo layout
    private TextView tvUserName, tvWelcome, tvReservasCount, tvViajesCount;
    private MaterialButton btnReservar, btnMiPerfil, btnEditarPerfil, btnRefresh;
    private FloatingActionButton fabReservar;
    private TabLayout tabLayout;
    private ViewPager2 viewPagerHorarios;
    private HorarioPagerAdapter pagerAdapter;

    // Datos
    private List<Horario> listaNataga = new ArrayList<>();
    private List<Horario> listaLaPlata = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio_usuarios);

        // Inicializar servicios
        authManager = AuthManager.getInstance();
        horarioService = new HorarioService();
        userService = new UserService();

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
    }

    private void initViews() {
        // Toolbar
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);

        // TextViews de información del usuario
        tvUserName = findViewById(R.id.tvUserName);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvReservasCount = findViewById(R.id.tvReservasCount);
        tvViajesCount = findViewById(R.id.tvViajesCount);

        // Botones
        btnReservar = findViewById(R.id.btnReservar);
        btnEditarPerfil = findViewById(R.id.btnEditarPerfil);
        btnRefresh = findViewById(R.id.btnRefresh);

        // TabLayout y ViewPager
        tabLayout = findViewById(R.id.tabLayout);
        viewPagerHorarios = findViewById(R.id.viewPagerHorarios);

        // Configurar menú de la toolbar
        topAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_perfil && validarLogIn()) {
                Intent intent = new Intent(InicioUsuarios.this, PerfilUsuario.class);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    private void configurarViewPager() {
        pagerAdapter = new HorarioPagerAdapter(this, listaNataga, listaLaPlata);
        viewPagerHorarios.setAdapter(pagerAdapter);

        // Conectar TabLayout con ViewPager2
        new TabLayoutMediator(tabLayout, viewPagerHorarios,
                (tab, position) -> {
                    if (position == 0) {
                        tab.setText("Natagá → La Plata");
                    } else {
                        tab.setText("La Plata → Natagá");
                    }
                }
        ).attach();
    }

    private void configurarListeners() {
        // Botón Reservar principal
        btnReservar.setOnClickListener(view -> {
            if (validarLogIn()) {
                navegarAReservas();
            }
        });

        // Botón Editar Perfil
        btnEditarPerfil.setOnClickListener(view -> {
            if (validarLogIn()) {
                Intent intent = new Intent(InicioUsuarios.this, EditarPerfil.class);
                startActivity(intent);
            }
        });

        // Botón Actualizar
        btnRefresh.setOnClickListener(view -> {
            cargarHorarios();
            Toast.makeText(this, "Actualizando horarios...", Toast.LENGTH_SHORT).show();
        });
    }

    private void cargarDatosUsuario() {
        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser != null) {
            // Cargar datos completos del usuario desde Firebase
            userService.loadUserData(currentUser.getUid(), new UserService.UserDataCallback() {
                @Override
                public void onUserDataLoaded(Usuario usuario) {
                    if (usuario != null && usuario.getNombre() != null) {
                        tvUserName.setText(usuario.getNombre());
                        tvWelcome.setText("¡Bienvenido, " + usuario.getNombre().split(" ")[0] + "!");
                    }
                    // Aquí podrías cargar también los contadores de reservas y viajes
                    actualizarContadores(10, 10); // Valores temporales
                }

                @Override
                public void onError(String error) {
                    Log.e("UserData", "Error cargando datos: " + error);
                }
            });
        }
    }

    private void actualizarContadores(int reservasCount, int viajesCount) {
        tvReservasCount.setText(String.valueOf(reservasCount));
        tvViajesCount.setText(String.valueOf(viajesCount));
    }

    private void cargarHorarios() {
        horarioService.cargarHorarios(new HorarioService.HorarioCallback() {
            @Override
            public void onHorariosCargados(List<Horario> nataga, List<Horario> laPlata) {
                runOnUiThread(() -> {
                    listaNataga.clear();
                    listaLaPlata.clear();

                    listaNataga.addAll(nataga);
                    listaLaPlata.addAll(laPlata);

                    // Actualizar el adaptador del ViewPager
                    if (pagerAdapter != null) {
                        pagerAdapter.actualizarDatos(listaNataga, listaLaPlata);

                        // Forzar actualización de los fragments visibles
                        int currentItem = viewPagerHorarios.getCurrentItem();
                    }

                    Toast.makeText(InicioUsuarios.this,
                            "Horarios actualizados: " + (listaNataga.size() + listaLaPlata.size()) + " total",
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(InicioUsuarios.this, "Error al cargar horarios: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    private void navegarAReservas() {
        Intent reservas = new Intent(InicioUsuarios.this, Reservas.class);
        startActivity(reservas);
    }

    private boolean validarLogIn() {
        if (!authManager.isUserLoggedIn()) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show();
            authManager.redirectToLogin(this);
            return false;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Actualizar datos cuando la actividad se reanude
        if (authManager.isUserLoggedIn()) {
            cargarDatosUsuario();
        }
    }
}