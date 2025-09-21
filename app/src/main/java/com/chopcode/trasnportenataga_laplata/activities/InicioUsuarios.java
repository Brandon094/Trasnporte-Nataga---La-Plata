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

        // 🔹 Configurar Adapters (SIN listener)
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
            if (validarLogIn()) {
                Intent reservas = new Intent(InicioUsuarios.this, Reservas.class);
                startActivity(reservas);
            }
        });

        // Detectar clic en el ícono del perfil
        topAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_perfil && validarLogIn()) {
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
                listaLaPlata.addAll(laPlata);

                adapterNataga.actualizarHorarios(listaNataga);
                adapterLaPlata.actualizarHorarios(listaLaPlata);
            }

            @Override
            public void onError(String error) {
                Log.e("Firebase", error);
                Toast.makeText(InicioUsuarios.this, "Error al cargar horarios", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 🔹 Valida si el usuario ha iniciado sesión.
     */
    private boolean validarLogIn() {
        FirebaseUser usuario = auth.getCurrentUser();
        if (usuario == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, InicioDeSesion.class);
            startActivity(intent);
            return false;
        }
        return true;
    }

    /**
     * 🔹 Cierra la sesión y redirige a la pantalla de inicio.
     */
    private void cerrarSesion() {
        auth.signOut();
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, InicioDeSesion.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}