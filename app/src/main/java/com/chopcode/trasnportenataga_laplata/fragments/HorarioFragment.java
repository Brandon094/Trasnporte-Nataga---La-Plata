package com.chopcode.trasnportenataga_laplata.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.activities.CrearReservas;
import com.chopcode.trasnportenataga_laplata.adapters.HorarioAdapter;
import com.chopcode.trasnportenataga_laplata.managers.AuthManager;
import com.chopcode.trasnportenataga_laplata.models.Horario;

import java.util.ArrayList;
import java.util.List;

public class HorarioFragment extends Fragment implements HorarioAdapter.OnReservarClickListener {

    private static final String ARG_HORARIOS = "horarios";
    private static final String ARG_TITULO = "titulo";

    private RecyclerView recyclerView;
    private HorarioAdapter adapter;
    private List<Horario> horarios = new ArrayList<>();
    private String titulo;
    private AuthManager authManager;

    public static HorarioFragment newInstance(List<Horario> horarios, String titulo) {
        HorarioFragment fragment = new HorarioFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_HORARIOS, new ArrayList<>(horarios));
        args.putString(ARG_TITULO, titulo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authManager = AuthManager.getInstance();

        // Cargar datos iniciales desde los argumentos
        if (getArguments() != null) {
            List<Horario> horariosArgs = (List<Horario>) getArguments().getSerializable(ARG_HORARIOS);
            if (horariosArgs != null) {
                horarios.clear();
                horarios.addAll(horariosArgs);
            }
            titulo = getArguments().getString(ARG_TITULO);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_horarios, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewHorarios);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Pasar this como listener para los clics de reserva
        adapter = new HorarioAdapter(horarios, this);
        recyclerView.setAdapter(adapter);

        Log.d("HorarioFragment", "Fragment creado para: " + titulo + " con " + horarios.size() + " horarios");

        return view;
    }

    public void actualizarHorarios(List<Horario> nuevosHorarios) {
        if (adapter != null) {
            horarios.clear();
            if (nuevosHorarios != null) {
                horarios.addAll(nuevosHorarios);
            }
            adapter.actualizarHorarios(horarios);
            Log.d("HorarioFragment", "Horarios actualizados para: " + titulo + " - " + horarios.size() + " elementos");
        } else {
            // Si el adapter aún no está creado, guardar los datos para cuando se cree
            horarios.clear();
            if (nuevosHorarios != null) {
                horarios.addAll(nuevosHorarios);
            }
        }
    }

    @Override
    public void onReservarClick(Horario horario) {
        if (!authManager.isUserLoggedIn()) {
            Toast.makeText(getContext(), "Debes iniciar sesión para reservar", Toast.LENGTH_SHORT).show();
            authManager.redirectToLogin(getActivity());
            return;
        }

        navegarACrearReservas(horario);
    }

    private void navegarACrearReservas(Horario horario) {
        try {
            Intent intent = new Intent(getActivity(), CrearReservas.class);
            intent.putExtra("horarioId", horario.getId());
            intent.putExtra("horarioHora", horario.getHora());
            intent.putExtra("rutaSeleccionada", titulo); // Usamos el título como ruta
            startActivity(intent);
            Log.d("HorarioFragment", "Navegando a CrearReservas con horario: " + horario.getHora());
        } catch (Exception e) {
            Log.e("HorarioFragment", "Error al navegar a CrearReservas: " + e.getMessage());
            Toast.makeText(getContext(), "Error al abrir la reserva", Toast.LENGTH_SHORT).show();
        }
    }
}