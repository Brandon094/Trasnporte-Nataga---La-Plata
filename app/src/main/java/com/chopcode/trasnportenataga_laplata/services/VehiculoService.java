package com.chopcode.trasnportenataga_laplata.services;

import android.util.Log;

import com.chopcode.trasnportenataga_laplata.models.Vehiculo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class VehiculoService {

    public interface VehiculoCallback {
        void onVehiculoCargado(Vehiculo vehiculo);
        void onError(String error);
    }

    // ✅ CORREGIDO: Buscar directamente por la clave (placa)
    public void obtenerVehiculoPorPlaca(String placa, VehiculoCallback callback) {
        if (placa == null || placa.isEmpty()) {
            callback.onVehiculoCargado(null);
            return;
        }

        DatabaseReference vehiculoRef = FirebaseDatabase.getInstance()
                .getReference("vehiculos")
                .child(placa);

        vehiculoRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d("VehiculoService", "Buscando vehículo con placa: " + placa);

                if (snapshot.exists()) {
                    Vehiculo vehiculo = snapshot.getValue(Vehiculo.class);
                    if (vehiculo != null) {
                        vehiculo.setId(snapshot.getKey());
                        Log.d("VehiculoService", "Vehículo encontrado: " + vehiculo.getModelo());
                        callback.onVehiculoCargado(vehiculo);
                    } else {
                        Log.e("VehiculoService", "Error al parsear vehículo");
                        callback.onVehiculoCargado(null);
                    }
                } else {
                    Log.e("VehiculoService", "No existe vehículo con placa: " + placa);
                    callback.onVehiculoCargado(null);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("VehiculoService", "Error en BD: " + error.getMessage());
                callback.onError(error.getMessage());
            }
        });
    }
}