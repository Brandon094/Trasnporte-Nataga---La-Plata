package com.chopcode.trasnportenataga_laplata.services;

import com.chopcode.trasnportenataga_laplata.models.Horario;
import com.chopcode.trasnportenataga_laplata.models.Ruta;
import com.chopcode.trasnportenataga_laplata.models.Usuario;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserService {
    private static final int CAPACIDAD_TOTAL = 14;

    // 🔥 INTERFACES CONSOLIDADAS
    public interface UserDataCallback {
        void onUserDataLoaded(Usuario usuario);
        void onError(String error);
    }

    public interface DriverCheckCallback {
        void onDriverCheckComplete(boolean isDriver);
        void onError(String error);
    }

    public interface DriverDataCallback {
        void onDriverDataLoaded(String nombre, String telefono, String placa,
                                List<String> horariosAsignados);
        void onError(String error);
    }

    public interface RoutesCallback {
        void onRoutesLoaded(List<Ruta> rutas);
        void onError(String error);
    }

    public interface StatisticsCallback {
        void onStatisticsCalculated(int reservasConfirmadas, int asientosDisponibles, double ingresos);
        void onError(String error);
    }

    public interface IncomeUpdateCallback {
        void onSuccess(double nuevosIngresos);
        void onError(String error);
    }

    public interface UserUpdateCallback {
        void onSuccess();
        void onError(String error);
    }

    // 🔥 MÉTODOS GENERALES DE USUARIO
    public void loadUserData(String userId, UserDataCallback callback) {
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Usuario usuario = snapshot.getValue(Usuario.class);
                    if (usuario != null) {
                        usuario.setId(userId);
                        callback.onUserDataLoaded(usuario);
                    } else {
                        callback.onError("Error al parsear datos del usuario");
                    }
                } else {
                    callback.onError("No se encontró el usuario en la BD");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public void updateUserProfile(String userId, String nombre, String telefono, UserUpdateCallback callback) {
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .child(userId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("nombre", nombre);
        updates.put("telefono", telefono);

        userRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // 🔥 MÉTODOS ESPECÍFICOS DE CONDUCTOR
    public void loadDriverData(String userId, DriverDataCallback callback) {
        DatabaseReference conductorRef = FirebaseDatabase.getInstance()
                .getReference("conductores")
                .child(userId);

        conductorRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String nombre = snapshot.child("nombre").getValue(String.class);
                    String telefono = snapshot.child("telefono").getValue(String.class); // ✅ TELÉFONO DESDE CONDUCTORES
                    String placa = snapshot.child("placaVehiculo").getValue(String.class);
                    List<String> horariosAsignados = new ArrayList<>();

                    if (snapshot.hasChild("horariosAsignados")) {
                        for (DataSnapshot horarioSnapshot : snapshot.child("horariosAsignados").getChildren()) {
                            String horarioId = horarioSnapshot.getValue(String.class);
                            if (horarioId != null) {
                                horariosAsignados.add(horarioId);
                            }
                        }
                    }

                    // ✅ PASA EL TELÉFONO AL CALLBACK
                    callback.onDriverDataLoaded(nombre, telefono, placa, horariosAsignados);
                } else {
                    callback.onError("No se encontró el conductor en la BD");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public void updateDriverProfile(String userId, String nombre, String telefono, String placa, List<String> horariosAsignados, UserUpdateCallback callback) {
        DatabaseReference driverRef = FirebaseDatabase.getInstance()
                .getReference("conductores")
                .child(userId);

        Map<String, Object> driverUpdates = new HashMap<>();
        driverUpdates.put("nombre", nombre);
        driverUpdates.put("telefono", telefono);
        driverUpdates.put("placaVehiculo", placa);

        // Actualizar horarios asignados si se proporcionan
        if (horariosAsignados != null) {
            driverUpdates.put("horariosAsignados", horariosAsignados);
        }

        // Actualizar datos del conductor
        driverRef.updateChildren(driverUpdates)
                .addOnSuccessListener(aVoid -> {
                    // También actualizar en usuarios para consistencia
                    updateUserProfile(userId, nombre, telefono, new UserUpdateCallback() {
                        @Override
                        public void onSuccess() {
                            callback.onSuccess();
                        }

                        @Override
                        public void onError(String error) {
                            callback.onError("Conductor actualizado pero error en usuario: " + error);
                        }
                    });
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // 🔥 MÉTODOS DE RUTAS
    public void loadAssignedRoutes(List<String> horariosAsignados, RoutesCallback callback) {
        if (horariosAsignados.isEmpty()) {
            callback.onRoutesLoaded(new ArrayList<>());
            return;
        }

        DatabaseReference horariosRef = FirebaseDatabase.getInstance().getReference("horarios");

        horariosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Ruta> rutas = new ArrayList<>();

                for (String horarioId : horariosAsignados) {
                    DataSnapshot horarioSnapshot = snapshot.child(horarioId);
                    if (horarioSnapshot.exists()) {
                        String hora = horarioSnapshot.child("hora").getValue(String.class);
                        String rutaNombre = horarioSnapshot.child("ruta").getValue(String.class);

                        if (hora != null && rutaNombre != null) {
                            Horario horario = new Horario();
                            horario.setId(horarioId);
                            horario.setHora(hora);
                            horario.setRuta(rutaNombre);

                            String origen, destino;
                            if (rutaNombre.contains("Natagá -> La Plata")) {
                                origen = "Natagá";
                                destino = "La Plata";
                            } else {
                                origen = "La Plata";
                                destino = "Natagá";
                            }

                            Ruta nuevaRuta = new Ruta(horarioId, origen, destino, 12000);
                            nuevaRuta.setHora(horario);
                            nuevaRuta.setHorarioId(horarioId);
                            rutas.add(nuevaRuta);
                        }
                    }
                }

                Collections.sort(rutas, (r1, r2) -> {
                    if (r1.getHora() != null && r2.getHora() != null) {
                        return r1.getHora().getHora().compareTo(r2.getHora().getHora());
                    }
                    return 0;
                });

                callback.onRoutesLoaded(rutas);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }
    // 🔥 MÉTODOS DE VERIFICACIÓN
    public void checkIfUserIsDriver(String userId, DriverCheckCallback callback) {
        DatabaseReference driverRef = FirebaseDatabase.getInstance()
                .getReference("conductores")
                .child(userId);

        driverRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                callback.onDriverCheckComplete(snapshot.exists());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }
}