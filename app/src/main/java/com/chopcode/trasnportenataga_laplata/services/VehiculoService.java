package com.chopcode.trasnportenataga_laplata.services;

import android.util.Log;

import com.chopcode.trasnportenataga_laplata.models.Vehiculo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class VehiculoService {

    public interface VehiculoCallback {
        void onVehiculoCargado(Vehiculo vehiculo);
        void onError(String error);
    }

    public interface VehiculoMapCallback {
        void onVehiculoObtenido(Map<String, Object> vehiculo);
        void onError(String error);
    }

    // ✅ Método para obtener vehículo por ID (placa)
    public void obtenerVehiculoPorId(String vehiculoId, VehiculoMapCallback callback) {
        if (vehiculoId == null || vehiculoId.isEmpty()) {
            callback.onVehiculoObtenido(null);
            return;
        }

        DatabaseReference vehiculoRef = FirebaseDatabase.getInstance()
                .getReference("vehiculos")
                .child(vehiculoId);

        vehiculoRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d("VehiculoService", "Buscando vehículo con ID: " + vehiculoId);

                if (snapshot.exists()) {
                    try {
                        Map<String, Object> vehiculoMap = new HashMap<>();

                        // Obtener todos los valores del vehículo
                        for (DataSnapshot child : snapshot.getChildren()) {
                            vehiculoMap.put(child.getKey(), child.getValue());
                        }

                        // Agregar el ID del vehículo
                        vehiculoMap.put("id", snapshot.getKey());

                        Log.d("VehiculoService", "Vehículo encontrado: " + vehiculoMap.get("modelo"));
                        callback.onVehiculoObtenido(vehiculoMap);

                    } catch (Exception e) {
                        Log.e("VehiculoService", "Error al parsear vehículo: " + e.getMessage());
                        callback.onError("Error al procesar datos del vehículo");
                    }
                } else {
                    Log.e("VehiculoService", "No existe vehículo con ID: " + vehiculoId);
                    callback.onVehiculoObtenido(null);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("VehiculoService", "Error en BD: " + error.getMessage());
                callback.onError(error.getMessage());
            }
        });
    }

    // ✅ Método para obtener vehículo como objeto Vehiculo
    public void obtenerVehiculoPorId(String vehiculoId, VehiculoCallback callback) {
        if (vehiculoId == null || vehiculoId.isEmpty()) {
            callback.onVehiculoCargado(null);
            return;
        }

        DatabaseReference vehiculoRef = FirebaseDatabase.getInstance()
                .getReference("vehiculos")
                .child(vehiculoId);

        vehiculoRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d("VehiculoService", "Buscando vehículo con ID: " + vehiculoId);

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
                    Log.e("VehiculoService", "No existe vehículo con ID: " + vehiculoId);
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

    // ✅ CORREGIDO: Buscar directamente por la clave (placa) - Método existente
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

    // ✅ Método para obtener vehículo por conductor
    public void obtenerVehiculoPorConductor(String conductorId, VehiculoCallback callback) {
        if (conductorId == null || conductorId.isEmpty()) {
            callback.onVehiculoCargado(null);
            return;
        }

        DatabaseReference vehiculosRef = FirebaseDatabase.getInstance()
                .getReference("vehiculos");

        vehiculosRef.orderByChild("conductorId").equalTo(conductorId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Log.d("VehiculoService", "Buscando vehículo para conductor: " + conductorId);

                        if (snapshot.exists()) {
                            for (DataSnapshot vehiculoSnapshot : snapshot.getChildren()) {
                                Vehiculo vehiculo = vehiculoSnapshot.getValue(Vehiculo.class);
                                if (vehiculo != null) {
                                    vehiculo.setId(vehiculoSnapshot.getKey());
                                    Log.d("VehiculoService", "Vehículo encontrado: " + vehiculo.getModelo());
                                    callback.onVehiculoCargado(vehiculo);
                                    return;
                                }
                            }
                        }
                        Log.e("VehiculoService", "No existe vehículo para conductor: " + conductorId);
                        callback.onVehiculoCargado(null);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e("VehiculoService", "Error en BD: " + error.getMessage());
                        callback.onError(error.getMessage());
                    }
                });
    }
}