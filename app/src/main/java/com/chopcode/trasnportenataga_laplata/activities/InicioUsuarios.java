package com.chopcode.trasnportenataga_laplata.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.models.Horario;
import com.chopcode.trasnportenataga_laplata.adapters.HorarioAdapter;
import com.chopcode.trasnportenataga_laplata.services.HorarioService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class InicioUsuarios extends AppCompatActivity {

    private RecyclerView recyclerViewNataga, recyclerViewLaPlata;
    private HorarioAdapter adapterNataga, adapterLaPlata;
    private List<Horario> listaNataga = new ArrayList<>();
    private List<Horario> listaLaPlata = new ArrayList<>();
    private Button btnReservas, btnCerrarSesion;
    private HorarioService horarioService;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio_usuarios);
        // 🔹 Referencia a la barra superior
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);

        // 🔹 Inicializar Firebase
        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();

        // 🔹 Configurar RecyclerViews
        recyclerViewNataga = findViewById(R.id.recyclerViewNataga);
        recyclerViewLaPlata = findViewById(R.id.recyclerViewLaPlata);
        recyclerViewNataga.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewLaPlata.setLayoutManager(new LinearLayoutManager(this));

        // 🔹 Configurar Adapters
        adapterNataga = new HorarioAdapter(listaNataga);
        adapterLaPlata = new HorarioAdapter(listaLaPlata);
        recyclerViewNataga.setAdapter(adapterNataga);
        recyclerViewLaPlata.setAdapter(adapterLaPlata);

        // 🔹 Cargar horarios
        horarioService = new HorarioService();
        cargarHorarios();

        // 🔹 Manejo del botón Reservar
        btnReservas = findViewById(R.id.btnReservar);
        btnReservas.setOnClickListener(view -> {
            // Validar si el usuario está autenticado
            boolean usuario = validarLogIn();
            if (usuario != false) {
                // El usuario está autenticado, proceder a las reservas
                Intent reservas = new Intent(InicioUsuarios.this, Reservas.class);
                startActivity(reservas);
            } else {
                // Usuario no autenticado, redirigir al inicio de sesión
                Intent intent = new Intent(InicioUsuarios.this, InicioDeSesion.class);
                // Guardar la actividad actual para volver después del inicio de sesión
                intent.putExtra("volverAReserva", true);
                startActivity(intent);
                // No finalizar la actividad actual para que el usuario pueda volver a ver los
                // horarios si decide no iniciar sesión
            }
        });
        // Detectar clic en el ícono del perfil
        topAppBar.setOnMenuItemClickListener(item -> {
            // Validar si el usuario está autenticado
            boolean usuario = validarLogIn();
            if (usuario != false & item.getItemId() == R.id.action_perfil) {
                // El usuario está autenticado, proceder al perfil de usuario
                Intent intent = new Intent(InicioUsuarios.this, PerfilUsuario.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        // 🔹 Manejo del botón Cerrar Sesión
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        btnCerrarSesion.setOnClickListener(view -> cerrarSesion());
    }
    /**
     * 🔹 Carga los horarios desde Firebase.
     */
    private void cargarHorarios() {
        horarioService.cargarHorarios(new HorarioService.HorarioCallback() {
            @Override
            public void onHorariosCargados(List<Horario> nataga, List<Horario> laPlata) {
                listaNataga.clear();
                listaLaPlata.clear();

                listaNataga.addAll(nataga);
                adapterNataga.notifyItemRangeInserted(0, nataga.size());

                listaLaPlata.addAll(laPlata);
                adapterLaPlata.notifyItemRangeInserted(0, laPlata.size());
            }

            @Override
            public void onError(String error) {
                Log.e("Firebase", error);
            }
        });
    }
    /**
     * 🔹 Valida si el usuario ha iniciado sesión.
     * @return true si está autenticado, false si no.
     */
    private boolean validarLogIn() {
        FirebaseUser usuario = auth.getCurrentUser();
        if (usuario == null) {
            Intent intent = new Intent(this, InicioDeSesion.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return false;
        }
        return true;
    }

    /**
     * 🔹 Cierra la sesión y redirige a la pantalla de inicio.
     */
    private void cerrarSesion() {
        auth.signOut();
        Intent intent = new Intent(this, InicioDeSesion.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
