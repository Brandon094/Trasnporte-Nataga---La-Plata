package com.chopcode.trasnportenataga_laplata.activities;

import androidx.annotation.NonNull;
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
import com.chopcode.trasnportenataga_laplata.services.ReservaService;
import com.chopcode.trasnportenataga_laplata.services.UserService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio_usuarios);

        // Inicializar servicios
        authManager = AuthManager.getInstance();
        horarioService = new HorarioService();
        reservaService = new ReservaService();
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
            cargarContadoresUsuario(); // Recargar contadores al actualizar
            Toast.makeText(this, "Actualizando información...", Toast.LENGTH_SHORT).show();
        });
    }

    private void cargarDatosUsuario() {
        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser != null) {
            final String userId = currentUser.getUid();

            // Cargar datos completos del usuario desde Firebase
            userService.loadUserData(userId, new UserService.UserDataCallback() {
                @Override
                public void onUserDataLoaded(Usuario usuario) {
                    if (usuario != null && usuario.getNombre() != null) {
                        usuarioActual = usuario; // Guardar referencia del usuario
                        tvUserName.setText(usuario.getNombre());
                        tvWelcome.setText("¡Bienvenido, " + usuario.getNombre().split(" ")[0] + "!");
                    }
                    cargarContadoresAlternativo(userId);
                }

                @Override
                public void onError(String error) {
                    Log.e("UserData", "Error cargando datos: " + error);
                    cargarContadoresAlternativo(userId);
                }
            });
        }
    }

    // Método para obtener el usuario actual (público para el adapter)
    public Usuario getUsuarioActual() {
        return usuarioActual;
    }

    private void cargarContadoresUsuario() {
        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            cargarContadoresAlternativo(userId);
        }
    }

    // Método alternativo para cargar contadores de reservas y viajes - CORREGIDO
    private void cargarContadoresAlternativo(final String userId) {
        DatabaseReference reservasRef = FirebaseDatabase.getInstance().getReference("reservas");

        reservasRef.orderByChild("usuarioId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Usar variables locales en lugar de modificar las del método externo
                        final int reservasCount = contarReservasActivas(snapshot);
                        final int viajesCount = contarViajesCompletados(snapshot);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                actualizarContadores(reservasCount, viajesCount);
                                Log.d("Contadores", "Reservas activas: " + reservasCount + ", Viajes completados: " + viajesCount);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                actualizarContadores(0, 0);
                                Log.e("Contadores", "Error al cargar contadores: " + error.getMessage());
                            }
                        });
                    }
                });
    }

    // Método auxiliar para contar reservas activas
    private int contarReservasActivas(DataSnapshot snapshot) {
        int count = 0;
        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
            com.chopcode.trasnportenataga_laplata.models.Reserva reserva =
                    dataSnapshot.getValue(com.chopcode.trasnportenataga_laplata.models.Reserva.class);
            if (reserva != null) {
                String estado = reserva.getEstadoReserva();
                if (estado != null && (estado.equals("Confirmada") || estado.equals("Por confirmar"))) {
                    count++;
                }
            }
        }
        return count;
    }

    // Método auxiliar para contar viajes completados
    private int contarViajesCompletados(DataSnapshot snapshot) {
        int count = 0;
        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
            com.chopcode.trasnportenataga_laplata.models.Reserva reserva =
                    dataSnapshot.getValue(com.chopcode.trasnportenataga_laplata.models.Reserva.class);
            if (reserva != null) {
                String estado = reserva.getEstadoReserva();
                if (estado != null && estado.equals("Confirmada")) {
                    count++;
                }
            }
        }
        return count;
    }

    // Versión simplificada sin listener en tiempo real (para evitar complejidad)
    private void actualizarContadores(int reservasCount, int viajesCount) {
        tvReservasCount.setText(String.valueOf(reservasCount));
        tvViajesCount.setText(String.valueOf(viajesCount));

        // Opcional: Mostrar mensaje informativo en logs
        if (reservasCount == 0 && viajesCount == 0) {
            Log.i("Contadores", "El usuario no tiene reservas activas ni viajes completados");
        }
    }

    private void cargarHorarios() {
        horarioService.cargarHorarios(new HorarioService.HorarioCallback() {
            @Override
            public void onHorariosCargados(List<Horario> nataga, List<Horario> laPlata) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listaNataga.clear();
                        listaLaPlata.clear();

                        listaNataga.addAll(nataga);
                        listaLaPlata.addAll(laPlata);

                        // Actualizar el adaptador del ViewPager
                        if (pagerAdapter != null) {
                            pagerAdapter.actualizarDatos(listaNataga, listaLaPlata);
                        }

                        Toast.makeText(InicioUsuarios.this,
                                "Horarios actualizados: " + (listaNataga.size() + listaLaPlata.size()) + " total",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(InicioUsuarios.this, "Error al cargar horarios: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
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
            cargarHorarios();
        }
    }
}