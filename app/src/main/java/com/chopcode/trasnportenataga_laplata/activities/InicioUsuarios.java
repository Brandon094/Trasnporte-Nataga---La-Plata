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
import com.chopcode.trasnportenataga_laplata.managers.AuthManager;
import com.chopcode.trasnportenataga_laplata.models.Horario;
import com.chopcode.trasnportenataga_laplata.adapters.HorarioAdapter;
import com.chopcode.trasnportenataga_laplata.services.HorarioService;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class InicioUsuarios extends AppCompatActivity {

    private RecyclerView recyclerViewNataga, recyclerViewLaPlata;
    private HorarioAdapter adapterNataga, adapterLaPlata;
    private List<Horario> listaNataga = new ArrayList<>();
    private List<Horario> listaLaPlata = new ArrayList<>();
    private Button btnReservas, btnCerrarSesion;
    private HorarioService horarioService;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio_usuarios);

        // 🔹 Inicializar AuthManager
        authManager = AuthManager.getInstance();

        // 🔹 Referencia a la barra superior
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);

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
     * 🔹 Valida si el usuario ha iniciado sesión usando AuthManager.
     */
    private boolean validarLogIn() {
        if (!authManager.isUserLoggedIn()) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show();
            authManager.redirectToLogin(this);
            return false;
        }
        return true;
    }

    /**
     * 🔹 Cierra la sesión usando AuthManager.
     */
    private void cerrarSesion() {
        authManager.signOut(this);
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
    }
}