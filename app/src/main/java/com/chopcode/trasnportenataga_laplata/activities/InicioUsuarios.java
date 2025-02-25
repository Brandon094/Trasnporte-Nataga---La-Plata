package com.chopcode.trasnportenataga_laplata.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.models.Horario;
import com.chopcode.trasnportenataga_laplata.adapters.HorarioAdapter;
import com.chopcode.trasnportenataga_laplata.services.HorarioService;
import com.google.firebase.FirebaseApp;

import java.util.ArrayList;
import java.util.List;

public class InicioUsuarios extends AppCompatActivity {

    private RecyclerView recyclerViewNataga, recyclerViewLaPlata;
    private HorarioAdapter adapterNataga, adapterLaPlata;
    private List<Horario> listaNataga = new ArrayList<>();
    private List<Horario> listaLaPlata = new ArrayList<>();
    private Button btnReservas;
    private HorarioService horarioService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio_usuarios);

        // Inicializar Firebase
        FirebaseApp.initializeApp(this);

        // Obtener referencia del botón
        btnReservas = findViewById(R.id.btnReservar);
        btnReservas.setOnClickListener(view -> {
            Intent reservas = new Intent(InicioUsuarios.this, Reservas.class);
            startActivity(reservas);
        });

        // Configurar RecyclerViews
        recyclerViewNataga = findViewById(R.id.recyclerViewNataga);
        recyclerViewLaPlata = findViewById(R.id.recyclerViewLaPlata);
        recyclerViewNataga.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewLaPlata.setLayoutManager(new LinearLayoutManager(this));

        // Configurar Adapters
        adapterNataga = new HorarioAdapter(listaNataga);
        adapterLaPlata = new HorarioAdapter(listaLaPlata);
        recyclerViewNataga.setAdapter(adapterNataga);
        recyclerViewLaPlata.setAdapter(adapterLaPlata);

        // 🔥 Usar el servicio para cargar los horarios
        horarioService = new HorarioService();
        cargarHorarios();
    }

    private void cargarHorarios() {
        horarioService.cargarHorarios(new HorarioService.HorarioCallback() {
            @Override
            public void onHorariosCargados(List<Horario> nataga, List<Horario> laPlata) {
                listaNataga.clear();
                listaLaPlata.clear();
                listaNataga.addAll(nataga);
                listaLaPlata.addAll(laPlata);

                adapterNataga.notifyDataSetChanged();
                adapterLaPlata.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                Log.e("Firebase", error);
            }
        });
    }
}
