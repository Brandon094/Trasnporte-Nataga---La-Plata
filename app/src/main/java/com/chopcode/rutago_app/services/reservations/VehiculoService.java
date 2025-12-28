package com.chopcode.rutago_app.services.reservations;

import android.util.Log;

import com.chopcode.rutago_app.models.Vehiculo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class VehiculoService {

    // ✅ NUEVO: Tag para logs
    private static final String TAG = "VehiculoService";

    public interface VehiculoCallback {
        void onVehiculoCargado(Vehiculo vehiculo);
        void onError(String error);
    }

    public interface VehiculoMapCallback {
        void onVehiculoObtenido(Map<String, Object> vehiculo);
        void onError(String error);
    }

    // ✅ CORREGIDO: Buscar directamente por la clave (placa) - Método existente
    public void obtenerVehiculoPorPlaca(String placa, VehiculoCallback callback) {
        Log.d(TAG, "🔍 Buscando vehículo por placa: " + placa);

        if (placa == null || placa.isEmpty()) {
            Log.w(TAG, "⚠️ Placa es null o vacía - no se puede buscar vehículo");
            callback.onVehiculoCargado(null);
            return;
        }

        DatabaseReference vehiculoRef = FirebaseDatabase.getInstance()
                .getReference("vehiculos")
                .child(placa);

        Log.d(TAG, "📡 Consultando Firebase en: vehiculos/" + placa);

        vehiculoRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d(TAG, "✅ Datos de vehículo recibidos para placa: " + placa);
                Log.d(TAG, "   - Existe en BD: " + snapshot.exists());

                if (snapshot.exists()) {
                    Vehiculo vehiculo = snapshot.getValue(Vehiculo.class);
                    if (vehiculo != null) {
                        vehiculo.setId(snapshot.getKey());
                        Log.d(TAG, "🚗 Vehículo encontrado exitosamente:");
                        Log.d(TAG, "   - Placa: " + vehiculo.getPlaca());
                        Log.d(TAG, "   - Modelo: " + vehiculo.getModelo());
                        Log.d(TAG, "   - Marca: " + vehiculo.getMarca());
                        Log.d(TAG, "   - Capacidad: " + vehiculo.getCapacidad());
                        Log.d(TAG, "   - Conductor ID: " + vehiculo.getConductorId());
                        callback.onVehiculoCargado(vehiculo);
                    } else {
                        Log.e(TAG, "❌ Error al parsear vehículo - datos corruptos para placa: " + placa);
                        callback.onVehiculoCargado(null);
                    }
                } else {
                    Log.w(TAG, "⚠️ No existe vehículo con placa: " + placa);
                    Log.w(TAG, "   - Ruta consultada: vehiculos/" + placa);
                    callback.onVehiculoCargado(null);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "❌ Error en Firebase Database al buscar vehículo por placa:");
                Log.e(TAG, "   - Placa: " + placa);
                Log.e(TAG, "   - Mensaje: " + error.getMessage());
                Log.e(TAG, "   - Código: " + error.getCode());
                Log.e(TAG, "   - Detalles: " + error.getDetails());
                callback.onError(error.getMessage());
            }
        });
    }

    // ✅ Método para obtener vehículo por conductor
    public void obtenerVehiculoPorConductor(String conductorId, VehiculoCallback callback) {
        Log.d(TAG, "👤 Buscando vehículo por conductor ID: " + conductorId);

        if (conductorId == null || conductorId.isEmpty()) {
            Log.w(TAG, "⚠️ Conductor ID es null o vacío - no se puede buscar vehículo");
            callback.onVehiculoCargado(null);
            return;
        }

        DatabaseReference vehiculosRef = FirebaseDatabase.getInstance()
                .getReference("vehiculos");

        Log.d(TAG, "📡 Consultando Firebase: vehiculos ordenados por conductorId = " + conductorId);

        vehiculosRef.orderByChild("conductorId").equalTo(conductorId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Log.d(TAG, "✅ Datos de vehículos recibidos para conductor: " + conductorId);
                        Log.d(TAG, "   - Total vehículos encontrados: " + snapshot.getChildrenCount());

                        if (snapshot.exists()) {
                            int vehiculosProcesados = 0;
                            for (DataSnapshot vehiculoSnapshot : snapshot.getChildren()) {
                                vehiculosProcesados++;
                                Vehiculo vehiculo = vehiculoSnapshot.getValue(Vehiculo.class);
                                if (vehiculo != null) {
                                    vehiculo.setId(vehiculoSnapshot.getKey());
                                    Log.d(TAG, "🚗 Vehículo encontrado para conductor:");
                                    Log.d(TAG, "   - Placa: " + vehiculo.getPlaca());
                                    Log.d(TAG, "   - Modelo: " + vehiculo.getModelo());
                                    Log.d(TAG, "   - Marca: " + vehiculo.getMarca());
                                    Log.d(TAG, "   - Capacidad: " + vehiculo.getCapacidad());
                                    Log.d(TAG, "   - Conductor ID: " + vehiculo.getConductorId());
                                    callback.onVehiculoCargado(vehiculo);
                                    return;
                                } else {
                                    Log.e(TAG, "❌ Error al parsear vehículo en snapshot: " + vehiculoSnapshot.getKey());
                                }
                            }
                            Log.w(TAG, "⚠️ No se pudo parsear ningún vehículo de " + vehiculosProcesados + " encontrados");
                            callback.onVehiculoCargado(null);
                        } else {
                            Log.w(TAG, "⚠️ No existe vehículo para conductor: " + conductorId);
                            Log.w(TAG, "   - Conductor ID: " + conductorId);
                            Log.w(TAG, "   - Ruta consultada: vehiculos ordenados por conductorId");
                            callback.onVehiculoCargado(null);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "❌ Error en Firebase Database al buscar vehículo por conductor:");
                        Log.e(TAG, "   - Conductor ID: " + conductorId);
                        Log.e(TAG, "   - Mensaje: " + error.getMessage());
                        Log.e(TAG, "   - Código: " + error.getCode());
                        Log.e(TAG, "   - Detalles: " + error.getDetails());
                        callback.onError(error.getMessage());
                    }
                });
    }

    // ✅ NUEVO MÉTODO: Obtener información básica del vehículo como Map
    public void obtenerInfoVehiculoBasica(String placa, VehiculoMapCallback callback) {
        Log.d(TAG, "📋 Obteniendo información básica del vehículo por placa: " + placa);

        if (placa == null || placa.isEmpty()) {
            Log.w(TAG, "⚠️ Placa es null o vacía - no se puede obtener información");
            callback.onError("Placa no válida");
            return;
        }

        DatabaseReference vehiculoRef = FirebaseDatabase.getInstance()
                .getReference("vehiculos")
                .child(placa);

        Log.d(TAG, "📡 Consultando información básica en: vehiculos/" + placa);

        vehiculoRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Map<String, Object> vehiculoInfo = new HashMap<>();

                    // Extraer solo los campos necesarios
                    if (snapshot.hasChild("placa")) {
                        vehiculoInfo.put("placa", snapshot.child("placa").getValue());
                    }
                    if (snapshot.hasChild("modelo")) {
                        vehiculoInfo.put("modelo", snapshot.child("modelo").getValue());
                    }
                    if (snapshot.hasChild("marca")) {
                        vehiculoInfo.put("marca", snapshot.child("marca").getValue());
                    }
                    if (snapshot.hasChild("capacidad")) {
                        vehiculoInfo.put("capacidad", snapshot.child("capacidad").getValue());
                    }

                    Log.d(TAG, "✅ Información básica del vehículo obtenida:");
                    Log.d(TAG, "   - Placa: " + vehiculoInfo.get("placa"));
                    Log.d(TAG, "   - Modelo: " + vehiculoInfo.get("modelo"));
                    Log.d(TAG, "   - Marca: " + vehiculoInfo.get("marca"));
                    Log.d(TAG, "   - Capacidad: " + vehiculoInfo.get("capacidad"));

                    callback.onVehiculoObtenido(vehiculoInfo);
                } else {
                    Log.w(TAG, "⚠️ No se encontró información del vehículo con placa: " + placa);
                    callback.onError("Vehículo no encontrado");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "❌ Error al obtener información básica del vehículo:");
                Log.e(TAG, "   - Placa: " + placa);
                Log.e(TAG, "   - Mensaje: " + error.getMessage());
                callback.onError("Error al obtener información del vehículo: " + error.getMessage());
            }
        });
    }
}