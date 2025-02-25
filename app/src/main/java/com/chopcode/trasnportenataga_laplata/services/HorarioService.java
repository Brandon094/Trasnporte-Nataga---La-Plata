package com.chopcode.trasnportenataga_laplata.services;

import android.util.Log;
import com.chopcode.trasnportenataga_laplata.models.Horario;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class HorarioService {

    private DatabaseReference databaseReference;

    public interface HorarioCallback {
        void onHorariosCargados(List<Horario> listaNataga, List<Horario> listaLaPlata);
        void onError(String error);
    }

    public HorarioService() {
        this.databaseReference = FirebaseDatabase.getInstance().getReference("horarios");
    }

    public void cargarHorarios(HorarioCallback callback) {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Horario> listaNataga = new ArrayList<>();
                List<Horario> listaLaPlata = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Horario horario = snapshot.getValue(Horario.class);
                    if (horario != null) {
                        String ruta = horario.getRuta().trim();
                        if (ruta.equals("Natagá -> La Plata")) {
                            listaNataga.add(horario);
                        } else if (ruta.equals("La Plata -> Natagá")) {
                            listaLaPlata.add(horario);
                        }
                    }
                }
                callback.onHorariosCargados(listaNataga, listaLaPlata);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError("Error al cargar horarios: " + databaseError.getMessage());
                Log.e("Firebase", "Error al cargar horarios: " + databaseError.getMessage());
            }
        });
    }
}
