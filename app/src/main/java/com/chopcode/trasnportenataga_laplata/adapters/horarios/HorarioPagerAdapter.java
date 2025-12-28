package com.chopcode.trasnportenataga_laplata.adapters.horarios;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.chopcode.trasnportenataga_laplata.activities.passenger.InicioUsuarios;
import com.chopcode.trasnportenataga_laplata.fragments.HorarioFragment;
import com.chopcode.trasnportenataga_laplata.models.Horario;
import com.chopcode.trasnportenataga_laplata.models.Usuario;

import java.util.ArrayList;
import java.util.List;

public class HorarioPagerAdapter extends FragmentStateAdapter {

    private static final String TAG = "HorarioPagerAdapter";
    private List<Horario> listaNataga;
    private List<Horario> listaLaPlata;
    private List<HorarioFragment> fragments = new ArrayList<>();
    private InicioUsuarios actividadPadre;

    public HorarioPagerAdapter(@NonNull InicioUsuarios fragmentActivity,
                               List<Horario> listaNataga,
                               List<Horario> listaLaPlata) {
        super(fragmentActivity);
        Log.d(TAG, "Constructor - Inicializando adapter");
        this.actividadPadre = fragmentActivity; // CORRECCIÓN: Asignar la actividad padre

        this.listaNataga = listaNataga != null ? new ArrayList<>(listaNataga) : new ArrayList<>();
        this.listaLaPlata = listaLaPlata != null ? new ArrayList<>(listaLaPlata) : new ArrayList<>();

        Log.i(TAG, "Datos inicializados - Natagá: " + this.listaNataga.size() +
                " horarios, La Plata: " + this.listaLaPlata.size() + " horarios");
        Log.d(TAG, "Actividad padre: " + (actividadPadre != null ? "asignada" : "NULA"));
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Log.d(TAG, "createFragment - Creando fragment para posición: " + position);

        HorarioFragment fragment;
        if (position == 0) {
            Log.v(TAG, "Creando fragment Natagá -> La Plata");
            fragment = HorarioFragment.newInstance(listaNataga, "Natagá -> La Plata");
        } else {
            Log.v(TAG, "Creando fragment La Plata -> Natagá");
            fragment = HorarioFragment.newInstance(listaLaPlata, "La Plata -> Natagá");
        }

        // CORRECCIÓN: Pasar la referencia de la actividad al fragment
        if (actividadPadre != null) {
            Log.d(TAG, "Configurando listener de datos de usuario para posición: " + position);
            // Usar el método setUsuarioDataListener en lugar de setActividadPadre
            fragment.setUsuarioDataListener(new HorarioFragment.OnUsuarioDataListener() {
                @Override
                public Usuario getUsuarioActual() {
                    Log.v(TAG, "Obteniendo usuario actual desde actividad padre");
                    return actividadPadre.getUsuarioActual();
                }
            });
        } else {
            Log.w(TAG, "Actividad padre es nula - No se puede configurar listener de usuario");
        }

        // Guardar referencia al fragment
        if (fragments.size() > position) {
            fragments.set(position, fragment);
            Log.d(TAG, "Fragment actualizado en posición: " + position);
        } else {
            fragments.add(position, fragment);
            Log.d(TAG, "Fragment agregado en posición: " + position + ", total fragments: " + fragments.size());
        }

        Log.i(TAG, "Fragment creado exitosamente para posición: " + position);
        return fragment;
    }

    @Override
    public int getItemCount() {
        Log.v(TAG, "getItemCount - Siempre retorna 2 pestañas");
        return 2; // Dos pestañas
    }

    public void actualizarDatos(List<Horario> nataga, List<Horario> laPlata) {
        Log.i(TAG, "actualizarDatos - Iniciando actualización");
        Log.d(TAG, "Datos anteriores - Natagá: " + listaNataga.size() +
                ", La Plata: " + listaLaPlata.size());
        Log.d(TAG, "Nuevos datos - Natagá: " + (nataga != null ? nataga.size() : 0) +
                ", La Plata: " + (laPlata != null ? laPlata.size() : 0));

        this.listaNataga.clear();
        this.listaLaPlata.clear();

        if (nataga != null) this.listaNataga.addAll(nataga);
        if (laPlata != null) this.listaLaPlata.addAll(laPlata);

        Log.d(TAG, "Listas actualizadas - Natagá: " + listaNataga.size() +
                ", La Plata: " + listaLaPlata.size());

        // Actualizar los fragments existentes
        Log.d(TAG, "Actualizando " + fragments.size() + " fragments existentes");
        for (int i = 0; i < fragments.size(); i++) {
            HorarioFragment fragment = fragments.get(i);
            if (fragment != null) {
                Log.d(TAG, "Actualizando fragment en posición: " + i);
                if (i == 0) {
                    fragment.actualizarHorarios(new ArrayList<>(listaNataga));
                    Log.v(TAG, "Horarios de Natagá actualizados en fragment posición 0");
                } else if (i == 1) {
                    fragment.actualizarHorarios(new ArrayList<>(listaLaPlata));
                    Log.v(TAG, "Horarios de La Plata actualizados en fragment posición 1");
                }

                // CORRECCIÓN: Re-configurar el listener al actualizar datos
                if (actividadPadre != null) {
                    Log.d(TAG, "Re-configurando listener para fragment posición: " + i);
                    fragment.setUsuarioDataListener(new HorarioFragment.OnUsuarioDataListener() {
                        @Override
                        public Usuario getUsuarioActual() {
                            Log.v(TAG, "Obteniendo usuario actual (re-configurado)");
                            return actividadPadre.getUsuarioActual();
                        }
                    });
                } else {
                    Log.w(TAG, "No se puede re-configurar listener - Actividad padre nula");
                }
            } else {
                Log.w(TAG, "Fragment nulo en posición: " + i + " - No se puede actualizar");
            }
        }

        Log.d(TAG, "Notificando cambio de dataset");
        notifyDataSetChanged();
        Log.i(TAG, "Actualización de datos completada exitosamente");
    }

    // Método para obtener un fragment específico
    public HorarioFragment getFragment(int position) {
        Log.d(TAG, "getFragment - Solicitando fragment en posición: " + position);

        if (position >= 0 && position < fragments.size()) {
            HorarioFragment fragment = fragments.get(position);
            Log.d(TAG, "Fragment encontrado en posición: " + position +
                    " - " + (fragment != null ? "válido" : "NULO"));
            return fragment;
        } else {
            Log.w(TAG, "Posición fuera de rango: " + position +
                    ", tamaño de fragments: " + fragments.size());
            return null;
        }
    }

    // Método para diagnóstico
    public void logEstadoActual() {
        Log.i(TAG, "=== ESTADO ACTUAL DEL ADAPTER ===");
        Log.d(TAG, "Lista Natagá: " + listaNataga.size() + " horarios");
        Log.d(TAG, "Lista La Plata: " + listaLaPlata.size() + " horarios");
        Log.d(TAG, "Fragments registrados: " + fragments.size());
        for (int i = 0; i < fragments.size(); i++) {
            Log.d(TAG, "Fragment " + i + ": " + (fragments.get(i) != null ? "presente" : "nulo"));
        }
        Log.d(TAG, "Actividad padre: " + (actividadPadre != null ? "asignada" : "NULA"));
        Log.i(TAG, "=================================");
    }
}