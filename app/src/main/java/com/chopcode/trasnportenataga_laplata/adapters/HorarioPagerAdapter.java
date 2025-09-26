package com.chopcode.trasnportenataga_laplata.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.chopcode.trasnportenataga_laplata.fragments.HorarioFragment;
import com.chopcode.trasnportenataga_laplata.models.Horario;

import java.util.List;

public class HorarioPagerAdapter extends FragmentStateAdapter {

    private List<Horario> listaNataga;
    private List<Horario> listaLaPlata;

    public HorarioPagerAdapter(@NonNull FragmentActivity fragmentActivity,
                               List<Horario> listaNataga,
                               List<Horario> listaLaPlata) {
        super(fragmentActivity);
        this.listaNataga = listaNataga;
        this.listaLaPlata = listaLaPlata;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return HorarioFragment.newInstance(listaNataga, "Natagá → La Plata");
        } else {
            return HorarioFragment.newInstance(listaLaPlata, "La Plata → Natagá");
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Dos pestañas
    }

    public void actualizarDatos(List<Horario> nataga, List<Horario> laPlata) {
        this.listaNataga = nataga;
        this.listaLaPlata = laPlata;
        notifyDataSetChanged();
    }
}