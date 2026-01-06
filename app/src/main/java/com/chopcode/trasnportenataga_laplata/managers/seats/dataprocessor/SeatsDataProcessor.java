package com.chopcode.trasnportenataga_laplata.managers.seats.dataprocessor;

import android.util.Log;
import androidx.annotation.NonNull;
import com.chopcode.trasnportenataga_laplata.config.MyApp;
import com.google.firebase.database.*;

import java.util.*;

    /**
     * Manager dedicado a manejar la lógica de base de datos de los asientos
     * Complementa al SeatManager que maneja la interfaz
     */
public class SeatsDataProcessor {
    private static final String TAG = "SeatsDataManager";
    private DatabaseReference databaseReference;

    // Interfaces para callbacks
    public interface SeatsDataCallback {
        void onSeatsDataLoaded(Set<Integer> occupiedSeats, int availableSeats);
        void onError(String error);
    }

    public interface SeatAvailabilityCallback {
        void onSeatAvailable(boolean available);
        void onError(String error);
    }

    public interface SeatReservationCallback {
        void onSuccess();
        void onError(String error);
    }

    public SeatsDataProcessor() {
        this.databaseReference = MyApp.getDatabaseReference("");
        Log.d(TAG, "✅ SeatsDataManager inicializado");
    }

    /**
     * 🔥 MÉTODO PRINCIPAL: Obtiene los asientos ocupados y disponibles de Firebase
     */
    public void loadSeatsDataForSchedule(String horarioId, SeatsDataCallback callback) {
        Log.d(TAG, "🔍 Cargando datos de asientos para horario: " + horarioId);

        // Referencia a disponibilidad
        DatabaseReference scheduleRef = databaseReference
                .child("disponibilidadAsientos")
                .child(horarioId);

        scheduleRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.e(TAG, "❌ Horario no encontrado: " + horarioId);
                    callback.onError("Horario no encontrado");
                    return;
                }

                try {
                    // Obtener asientos ocupados
                    Set<Integer> occupiedSeats = new HashSet<>();
                    DataSnapshot occupiedSnapshot = snapshot.child("asientosOcupados");

                    if (occupiedSnapshot.exists()) {
                        for (DataSnapshot seatSnapshot : occupiedSnapshot.getChildren()) {
                            try {
                                String seatKey = seatSnapshot.getKey();
                                Boolean isOccupied = seatSnapshot.getValue(Boolean.class);

                                if (seatKey != null && isOccupied != null && isOccupied) {
                                    int seatNumber = Integer.parseInt(seatKey);
                                    occupiedSeats.add(seatNumber);
                                    Log.d(TAG, "   - Asiento ocupado encontrado: " + seatNumber);
                                }
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "❌ Error parseando asiento: " + e.getMessage());
                                MyApp.logError(e);
                            }
                        }
                    } else {
                        Log.w(TAG, "⚠️ Nodo 'asientosOcupados' no existe, creándolo...");
                        // Crear nodo vacío si no existe
                        createOccupiedSeatsNode(horarioId);
                    }

                    // Obtener asientos disponibles
                    int availableSeats = 0;
                    DataSnapshot availableSnapshot = snapshot.child("asientosDisponibles");

                    if (availableSnapshot.exists()) {
                        Integer available = availableSnapshot.getValue(Integer.class);
                        availableSeats = available != null ? available : 0;
                    } else {
                        Log.w(TAG, "⚠️ Nodo 'asientosDisponibles' no encontrado, usando valor por defecto");
                        availableSeats = 13; // Valor por defecto basado en tu DB
                    }

                    Log.d(TAG, "✅ Datos cargados exitosamente:");
                    Log.d(TAG, "   - Asientos ocupados: " + occupiedSeats.size());
                    Log.d(TAG, "   - Asientos disponibles: " + availableSeats);

                    // Registrar evento en Analytics
                    Map<String, Object> params = new HashMap<>();
                    params.put("horario_id", horarioId);
                    params.put("ocupados", occupiedSeats.size());
                    params.put("disponibles", availableSeats);
                    MyApp.logEvent("datos_asientos_cargados", params);

                    callback.onSeatsDataLoaded(occupiedSeats, availableSeats);

                } catch (Exception e) {
                    Log.e(TAG, "❌ Error procesando datos: " + e.getMessage());
                    MyApp.logError(e);
                    callback.onError("Error procesando datos: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error cargando datos: " + error.getMessage());
                MyApp.logError(error.toException());
                callback.onError("Error cargando datos: " + error.getMessage());
            }
        });
    }

    /**
     * Verifica si un asiento específico está disponible
     */
    public void checkSeatAvailability(String horarioId, int seatNumber, SeatAvailabilityCallback callback) {
        Log.d(TAG, "🔍 Verificando disponibilidad - Horario: " + horarioId + ", Asiento: " + seatNumber);

        DatabaseReference seatRef = databaseReference
                .child("disponibilidadAsientos")
                .child(horarioId)
                .child("asientosOcupados")
                .child(String.valueOf(seatNumber));

        seatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean isOccupied = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                boolean isAvailable = !isOccupied;

                Log.d(TAG, "✅ Asiento " + seatNumber + " - " +
                        (isAvailable ? "✅ DISPONIBLE" : "❌ OCUPADO"));

                callback.onSeatAvailable(isAvailable);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error verificando asiento: " + error.getMessage());
                MyApp.logError(error.toException());
                callback.onError("Error verificando asiento: " + error.getMessage());
            }
        });
    }

    /**
     * 🔥 MÉTODO CRÍTICO: Reserva un asiento (marca como ocupado y actualiza contador)
     */
    public void reserveSeat(String horarioId, int seatNumber, SeatReservationCallback callback) {
        Log.d(TAG, "🔄 Iniciando reserva de asiento - Horario: " + horarioId + ", Asiento: " + seatNumber);

        // Primero verificar si el asiento está disponible
        checkSeatAvailability(horarioId, seatNumber, new SeatAvailabilityCallback() {
            @Override
            public void onSeatAvailable(boolean available) {
                if (!available) {
                    Log.e(TAG, "❌ Asiento " + seatNumber + " ya está ocupado");
                    callback.onError("El asiento ya está ocupado");
                    return;
                }

                // Si está disponible, proceder con la reserva
                performSeatReservation(horarioId, seatNumber, callback);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error verificando disponibilidad: " + error);
                callback.onError("Error verificando disponibilidad: " + error);
            }
        });
    }

    private void performSeatReservation(String horarioId, int seatNumber, SeatReservationCallback callback) {
        // Obtener referencia al asiento
        DatabaseReference seatRef = databaseReference
                .child("disponibilidadAsientos")
                .child(horarioId)
                .child("asientosOcupados")
                .child(String.valueOf(seatNumber));

        // Obtener referencia al contador
        DatabaseReference availableRef = databaseReference
                .child("disponibilidadAsientos")
                .child(horarioId)
                .child("asientosDisponibles");

        // Obtener valor actual de asientos disponibles
        availableRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer currentAvailable = snapshot.getValue(Integer.class);
                if (currentAvailable == null || currentAvailable <= 0) {
                    Log.e(TAG, "❌ No hay asientos disponibles");
                    callback.onError("No hay asientos disponibles");
                    return;
                }

                int newAvailable = currentAvailable - 1;

                // Crear mapa de actualizaciones
                Map<String, Object> updates = new HashMap<>();
                updates.put("asientosOcupados/" + seatNumber, true);
                updates.put("asientosDisponibles", newAvailable);

                // Aplicar actualizaciones
                DatabaseReference scheduleRef = databaseReference
                        .child("disponibilidadAsientos")
                        .child(horarioId);

                scheduleRef.updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "✅ Asiento reservado exitosamente:");
                            Log.d(TAG, "   - Asiento: " + seatNumber);
                            Log.d(TAG, "   - Disponibles antes: " + currentAvailable);
                            Log.d(TAG, "   - Disponibles ahora: " + newAvailable);

                            // Registrar evento
                            Map<String, Object> params = new HashMap<>();
                            params.put("horario_id", horarioId);
                            params.put("asiento", seatNumber);
                            params.put("disponibles_antes", currentAvailable);
                            params.put("disponibles_ahora", newAvailable);
                            MyApp.logEvent("asiento_reservado_exito", params);

                            callback.onSuccess();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "❌ Error reservando asiento: " + e.getMessage());
                            MyApp.logError(e);
                            callback.onError("Error reservando asiento: " + e.getMessage());
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error obteniendo disponibilidad: " + error.getMessage());
                callback.onError("Error obteniendo disponibilidad: " + error.getMessage());
            }
        });
    }

    /**
     * Libera un asiento (para cancelaciones)
     */
    public void freeSeat(String horarioId, int seatNumber, SeatReservationCallback callback) {
        Log.d(TAG, "🔄 Liberando asiento - Horario: " + horarioId + ", Asiento: " + seatNumber);

        DatabaseReference seatRef = databaseReference
                .child("disponibilidadAsientos")
                .child(horarioId)
                .child("asientosOcupados")
                .child(String.valueOf(seatNumber));

        DatabaseReference availableRef = databaseReference
                .child("disponibilidadAsientos")
                .child(horarioId)
                .child("asientosDisponibles");

        // Primero eliminar el asiento ocupado
        seatRef.removeValue((error, ref) -> {
            if (error != null) {
                Log.e(TAG, "❌ Error liberando asiento: " + error.getMessage());
                callback.onError("Error liberando asiento: " + error.getMessage());
                return;
            }

            Log.d(TAG, "✅ Asiento " + seatNumber + " liberado, actualizando contador...");

            // Luego incrementar el contador
            availableRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Integer currentAvailable = snapshot.getValue(Integer.class);
                    if (currentAvailable == null) {
                        currentAvailable = 0;
                    }

                    int newAvailable = Math.min(13, currentAvailable + 1);

                    Integer finalCurrentAvailable = currentAvailable;
                    availableRef.setValue(newAvailable)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "✅ Asiento liberado exitosamente:");
                                Log.d(TAG, "   - Asiento: " + seatNumber);
                                Log.d(TAG, "   - Disponibles antes: " + finalCurrentAvailable);
                                Log.d(TAG, "   - Disponibles ahora: " + newAvailable);

                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ Error actualizando contador: " + e.getMessage());
                                callback.onError("Error actualizando contador: " + e.getMessage());
                            });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "❌ Error obteniendo contador: " + error.getMessage());
                    callback.onError("Error obteniendo contador: " + error.getMessage());
                }
            });
        });
    }

    /**
     * Verifica y repara la estructura de datos si es necesario
     */
    public void repairSeatStructure(String horarioId) {
        Log.d(TAG, "🔧 Verificando estructura para horario: " + horarioId);

        DatabaseReference scheduleRef = databaseReference
                .child("disponibilidadAsientos")
                .child(horarioId);

        scheduleRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();
                boolean needsRepair = false;

                // Verificar nodos esenciales
                if (!snapshot.exists()) {
                    // Si no existe el horario, crear toda la estructura
                    updates.put("totalAsientos", 13);
                    updates.put("asientosDisponibles", 13);
                    updates.put("asientosOcupados", new HashMap<>());
                    needsRepair = true;
                    Log.d(TAG, "   - Creando estructura completa para horario nuevo");
                } else {
                    // Verificar cada nodo individualmente
                    if (!snapshot.hasChild("asientosOcupados")) {
                        updates.put("asientosOcupados", new HashMap<>());
                        needsRepair = true;
                        Log.d(TAG, "   - Agregando asientosOcupados");
                    }

                    if (!snapshot.hasChild("asientosDisponibles")) {
                        updates.put("asientosDisponibles", 13);
                        needsRepair = true;
                        Log.d(TAG, "   - Agregando asientosDisponibles");
                    }

                    if (!snapshot.hasChild("totalAsientos")) {
                        updates.put("totalAsientos", 13);
                        needsRepair = true;
                        Log.d(TAG, "   - Agregando totalAsientos");
                    }
                }

                if (needsRepair) {
                    scheduleRef.updateChildren(updates)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "✅ Estructura reparada para horario: " + horarioId);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ Error reparando estructura: " + e.getMessage());
                            });
                } else {
                    Log.d(TAG, "✅ Estructura ya está correcta para horario: " + horarioId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error verificando estructura: " + error.getMessage());
            }
        });
    }

    /**
     * Crea el nodo asientosOcupados si no existe
     */
    private void createOccupiedSeatsNode(String horarioId) {
        DatabaseReference occupiedRef = databaseReference
                .child("disponibilidadAsientos")
                .child(horarioId)
                .child("asientosOcupados");

        occupiedRef.setValue(new HashMap<>())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Nodo asientosOcupados creado para: " + horarioId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error creando nodo asientosOcupados: " + e.getMessage());
                });
    }

    /**
     * Inicializa todos los horarios en la base de datos
     */
    public void initializeAllSchedules() {
        Log.d(TAG, "🚀 Inicializando todos los horarios...");

        // Lista de horarios de tu base de datos
        String[] horarios = {"h001", "h002", "h003", "h004", "h005", "h006", "h007", "h008",
                "h009", "h010", "h011", "h012", "h013", "h014", "h015", "h016", "h017", "h018"};

        for (String horarioId : horarios) {
            repairSeatStructure(horarioId);
        }

        Log.d(TAG, "✅ Inicialización de horarios completada");
    }
}
