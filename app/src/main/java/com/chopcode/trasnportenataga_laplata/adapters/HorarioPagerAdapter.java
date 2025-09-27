package com.chopcode.trasnportenataga_laplata.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.chopcode.trasnportenataga_laplata.fragments.HorarioFragment;
import com.chopcode.trasnportenataga_laplata.models.Horario;

import java.util.ArrayList;
import java.util.List;

public class HorarioPagerAdapter extends FragmentStateAdapter {

    private List<Horario> listaNataga;
    private List<Horario> listaLaPlata;
    private List<HorarioFragment> fragments = new ArrayList<>();

    public HorarioPagerAdapter(@NonNull FragmentActivity fragmentActivity,
                               List<Horario> listaNataga,
                               List<Horario> listaLaPlata) {
        super(fragmentActivity);
        this.listaNataga = listaNataga != null ? new ArrayList<>(listaNataga) : new ArrayList<>();
        this.listaLaPlata = listaLaPlata != null ? new ArrayList<>(listaLaPlata) : new ArrayList<>();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        HorarioFragment fragment;
        if (position == 0) {
            fragment = HorarioFragment.newInstance(listaNataga, "Natagá -> La Plata");
        } else {
            fragment = HorarioFragment.newInstance(listaLaPlata, "La Plata -> Natagá");
        }

        // Guardar referencia al fragment
        if (fragments.size() > position) {
            fragments.set(position, fragment);
        } else {
            fragments.add(position, fragment);
        }

        return fragment;
    }

    @Override
    public int getItemCount() {
        return 2; // Dos pestañas
    }

    public void actualizarDatos(List<Horario> nataga, List<Horario> laPlata) {
        this.listaNataga.clear();
        this.listaLaPlata.clear();

        if (nataga != null) this.listaNataga.addAll(nataga);
        if (laPlata != null) this.listaLaPlata.addAll(laPlata);

        // Actualizar los fragments existentes
        for (int i = 0; i < fragments.size(); i++) {
            HorarioFragment fragment = fragments.get(i);
            if (fragment != null) {
                if (i == 0) {
                    fragment.actualizarHorarios(new ArrayList<>(listaNataga));
                } else if (i == 1) {
                    fragment.actualizarHorarios(new ArrayList<>(listaLaPlata));
                }
            }
        }

        notifyDataSetChanged();
    }

    // Método para obtener un fragment específico
    public HorarioFragment getFragment(int position) {
        if (position >= 0 && position < fragments.size()) {
            return fragments.get(position);
        }
        return null;
    }
}