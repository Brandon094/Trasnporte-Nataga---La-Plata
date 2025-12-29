package com.chopcode.trasnportenataga_laplata.viewmodels.driver;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.chopcode.trasnportenataga_laplata.managers.reservations.ReservasManager;
import com.chopcode.trasnportenataga_laplata.managers.statistics.DriverStatisticsManager;
import com.chopcode.trasnportenataga_laplata.managers.routes.RutasManager;
import com.chopcode.trasnportenataga_laplata.managers.notificactions.NotificationManager;
import com.chopcode.trasnportenataga_laplata.models.Reserva;
import com.chopcode.trasnportenataga_laplata.models.Ruta;
import com.chopcode.trasnportenataga_laplata.config.MyApp;

import android.content.Context;
import android.util.Log;

import java.util.List;

public class DriverHomeViewModel extends ViewModel {
    private static final String TAG = "DriverHomeViewModel";

    private final ReservasManager reservasManager;
    private final DriverStatisticsManager statisticsManager;
    private final RutasManager rutasManager;
    private NotificationManager notificationManager;
    private Context context;

    // LiveData existentes
    private final MutableLiveData<List<Reserva>> reservasLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Ruta>> rutasLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> reservasConfirmadasLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> asientosDisponiblesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Double> ingresosLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> nombreConductorLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> placaVehiculoLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<String>> horariosLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>();

    // ✅ NUEVO: LiveData para estadísticas por ruta (primera ruta)
    private final MutableLiveData<String> nombreRuta1LiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> reservasRuta1LiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> asientosRuta1LiveData = new MutableLiveData<>();

    // ✅ NUEVO: LiveData para estadísticas por ruta (segunda ruta)
    private final MutableLiveData<String> nombreRuta2LiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> reservasRuta2LiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> asientosRuta2LiveData = new MutableLiveData<>();

    public DriverHomeViewModel() {
        this.reservasManager = new ReservasManager();
        this.statisticsManager = new DriverStatisticsManager();
        this.rutasManager = new RutasManager();
        this.loadingLiveData.setValue(false);
    }

    // ✅ Método para inicializar con contexto
    public void initialize(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = NotificationManager.getInstance(context);
        if (notificationManager != null) {
            reservasManager.setNotificationManager(notificationManager);
        }
    }

    // Getters para LiveData existentes
    public LiveData<List<Reserva>> getReservasLiveData() { return reservasLiveData; }
    public LiveData<List<Ruta>> getRutasLiveData() { return rutasLiveData; }
    public LiveData<Integer> getReservasConfirmadasLiveData() { return reservasConfirmadasLiveData; }
    public LiveData<Integer> getAsientosDisponiblesLiveData() { return asientosDisponiblesLiveData; }
    public LiveData<Double> getIngresosLiveData() { return ingresosLiveData; }
    public LiveData<String> getNombreConductorLiveData() { return nombreConductorLiveData; }
    public LiveData<String> getPlacaVehiculoLiveData() { return placaVehiculoLiveData; }
    public LiveData<List<String>> getHorariosLiveData() { return horariosLiveData; }
    public LiveData<String> getErrorLiveData() { return errorLiveData; }
    public LiveData<Boolean> getLoadingLiveData() { return loadingLiveData; }

    // ✅ NUEVO: Getters para estadísticas por ruta
    public LiveData<String> getNombreRuta1LiveData() { return nombreRuta1LiveData; }
    public LiveData<Integer> getReservasRuta1LiveData() { return reservasRuta1LiveData; }
    public LiveData<Integer> getAsientosRuta1LiveData() { return asientosRuta1LiveData; }
    public LiveData<String> getNombreRuta2LiveData() { return nombreRuta2LiveData; }
    public LiveData<Integer> getReservasRuta2LiveData() { return reservasRuta2LiveData; }
    public LiveData<Integer> getAsientosRuta2LiveData() { return asientosRuta2LiveData; }

    // Métodos principales
    public void loadDriverData(String userId) {
        Log.d(TAG, "🚀 Cargando datos del conductor: " + userId);
        loadingLiveData.postValue(true);

        if (userId == null || userId.isEmpty()) {
            errorLiveData.postValue("ID de usuario no válido");
            loadingLiveData.postValue(false);
            return;
        }

        reservasManager.loadDriverData(userId, new ReservasManager.DriverDataCallback() {
            @Override
            public void onDriverDataLoaded(String nombre, String telefono, String placa, List<String> horarios) {
                Log.d(TAG, "✅ Datos del conductor cargados: " + nombre);

                // Actualizar LiveData
                nombreConductorLiveData.postValue(nombre);
                placaVehiculoLiveData.postValue(placa);
                horariosLiveData.postValue(horarios);

                // Iniciar otros procesos
                setupRealTimeListener(nombre);
                calculateStatistics(nombre);
                loadReservations(nombre);
                if (horarios != null && !horarios.isEmpty()) {
                    loadRoutes(horarios);
                }

                loadingLiveData.postValue(false);

                // ✅ Registrar evento analítico usando MyApp
                registrarEventoAnalitico("conductor_data_loaded", nombre, null);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error cargando datos conductor: " + error);
                errorLiveData.postValue(error);
                loadingLiveData.postValue(false);

                // ✅ Registrar error usando MyApp
                registrarErrorAnalitico("conductor_data_error", error, userId);
            }
        });
    }

    public void setupRealTimeListener(String conductorNombre) {
        Log.d(TAG, "🔔 Configurando listener tiempo real para: " + conductorNombre);

        reservasManager.setupRealTimeListener(conductorNombre, new ReservasManager.RealTimeCallback() {
            @Override
            public void onDataChanged(List<Reserva> reservas, int nuevasConfirmadas) {
                Log.d(TAG, "🔄 Datos tiempo real actualizados: " + reservas.size() + " reservas");
                reservasLiveData.postValue(reservas);

                // Si hay nuevas confirmadas, actualizar estadísticas
                if (nuevasConfirmadas > 0) {
                    Integer current = reservasConfirmadasLiveData.getValue();
                    if (current == null || nuevasConfirmadas != current) {
                        reservasConfirmadasLiveData.postValue(nuevasConfirmadas);
                    }
                }

                // ✅ Calcular estadísticas por ruta cuando se actualizan las reservas
                List<Ruta> rutasActuales = rutasLiveData.getValue();
                if (rutasActuales != null && !rutasActuales.isEmpty()) {
                    calculateRouteStatistics(rutasActuales, reservas);
                }

                // ✅ Registrar evento analítico
                registrarEventoAnalitico("realtime_update", conductorNombre, reservas.size());
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error en listener tiempo real: " + error);
                errorLiveData.postValue(error);
            }
        });
    }

    public void calculateStatistics(String conductorNombre) {
        Log.d(TAG, "📊 Calculando estadísticas para: " + conductorNombre);

        statisticsManager.calculateDailyStatistics(conductorNombre, new DriverStatisticsManager.StatisticsCallback() {
            @Override
            public void onStatisticsCalculated(int reservasConfirmadas, int asientosDisp, double ingresos) {
                Log.d(TAG, "✅ Estadísticas calculadas: " +
                        "Confirmadas=" + reservasConfirmadas +
                        ", Asientos=" + asientosDisp +
                        ", Ingresos=$" + ingresos);

                reservasConfirmadasLiveData.postValue(reservasConfirmadas);
                asientosDisponiblesLiveData.postValue(asientosDisp);
                ingresosLiveData.postValue(ingresos);

                // ✅ Registrar evento analítico
                registrarEstadisticasAnaliticas(conductorNombre, reservasConfirmadas, asientosDisp, ingresos);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error calculando estadísticas: " + error);
                errorLiveData.postValue("Error calculando estadísticas: " + error);
            }
        });
    }

    public void loadReservations(String conductorNombre) {
        Log.d(TAG, "🔍 Cargando reservas para: " + conductorNombre);

        reservasManager.loadReservations(conductorNombre, new ReservasManager.ReservationsCallback() {
            @Override
            public void onReservationsLoaded(List<Reserva> reservas) {
                Log.d(TAG, "✅ Reservas cargadas: " + reservas.size());
                reservasLiveData.postValue(reservas);

                // ✅ Calcular estadísticas por ruta cuando se cargan las reservas
                List<Ruta> rutasActuales = rutasLiveData.getValue();
                if (rutasActuales != null && !rutasActuales.isEmpty()) {
                    calculateRouteStatistics(rutasActuales, reservas);
                }

                // ✅ Registrar evento analítico
                registrarEventoAnalitico("reservas_loaded", conductorNombre, reservas.size());
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error cargando reservas: " + error);
                errorLiveData.postValue("Error cargando reservas: " + error);
            }
        });
    }

    public void loadRoutes(List<String> horariosAsignados) {
        Log.d(TAG, "🗺️ Cargando rutas asignadas: " + horariosAsignados.size() + " horarios");

        rutasManager.loadAssignedRoutes(horariosAsignados, new RutasManager.RoutesCallback() {
            @Override
            public void onRoutesLoaded(List<Ruta> rutas) {
                Log.d(TAG, "✅ Rutas cargadas: " + rutas.size());
                rutasLiveData.postValue(rutas);

                // ✅ Calcular estadísticas por ruta cuando se cargan las rutas
                List<Reserva> reservasActuales = reservasLiveData.getValue();
                if (reservasActuales != null && !reservasActuales.isEmpty()) {
                    calculateRouteStatistics(rutas, reservasActuales);
                }

                // ✅ Registrar evento analítico
                registrarEventoAnalitico("rutas_loaded", null, rutas.size());
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error cargando rutas: " + error);
                errorLiveData.postValue("Error cargando rutas: " + error);
            }
        });
    }

    // ✅ NUEVO MÉTODO: Calcular estadísticas por ruta
    public void calculateRouteStatistics(List<Ruta> rutas, List<Reserva> reservas) {
        Log.d(TAG, "📊 Calculando estadísticas por ruta");

        if (rutas == null || rutas.isEmpty() || reservas == null) {
            Log.w(TAG, "⚠️ No hay rutas o reservas para calcular estadísticas por ruta");
            return;
        }

        // Procesar primera ruta si existe
        if (rutas.size() >= 1) {
            Ruta ruta1 = rutas.get(0);
            String nombreRuta1 = ruta1.getOrigen() + " → " + ruta1.getDestino();
            nombreRuta1LiveData.postValue(nombreRuta1);

            // Calcular reservas y asientos para ruta 1
            calculateRouteSpecificStats(ruta1, reservas, 1);
        }

        // Procesar segunda ruta si existe
        if (rutas.size() >= 2) {
            Ruta ruta2 = rutas.get(1);
            String nombreRuta2 = ruta2.getOrigen() + " → " + ruta2.getDestino();
            nombreRuta2LiveData.postValue(nombreRuta2);

            // Calcular reservas y asientos para ruta 2
            calculateRouteSpecificStats(ruta2, reservas, 2);
        }
    }

    // ✅ NUEVO MÉTODO: Calcular estadísticas específicas por ruta
    private void calculateRouteSpecificStats(Ruta ruta, List<Reserva> reservas, int routeNumber) {
        int reservasRuta = 0;
        int asientosOcupados = 0;

        // Asumimos que cada ruta tiene una capacidad total
        final int CAPACIDAD_RUTA = 14; // O usa ruta.getCapacidadTotal() si existe

        // Contar reservas "Por confirmar" para esta ruta
        for (Reserva reserva : reservas) {
            if (reserva != null &&
                    ruta.getOrigen().equals(reserva.getOrigen()) &&
                    ruta.getDestino().equals(reserva.getDestino())) {

                if ("Por confirmar".equals(reserva.getEstadoReserva())) {
                    reservasRuta++;
                    asientosOcupados++;
                }
            }
        }

        int asientosDisponibles = Math.max(0, CAPACIDAD_RUTA - asientosOcupados);

        if (routeNumber == 1) {
            reservasRuta1LiveData.postValue(reservasRuta);
            asientosRuta1LiveData.postValue(asientosDisponibles);
            Log.d(TAG, "📊 Ruta 1: " + ruta.getOrigen() + " → " + ruta.getDestino() +
                    " - Reservas: " + reservasRuta + ", Asientos disponibles: " + asientosDisponibles);
        } else if (routeNumber == 2) {
            reservasRuta2LiveData.postValue(reservasRuta);
            asientosRuta2LiveData.postValue(asientosDisponibles);
            Log.d(TAG, "📊 Ruta 2: " + ruta.getOrigen() + " → " + ruta.getDestino() +
                    " - Reservas: " + reservasRuta + ", Asientos disponibles: " + asientosDisponibles);
        }
    }

    public void confirmReservation(Reserva reserva) {
        Log.d(TAG, "✅ Confirmando reserva: " + reserva.getIdReserva() + " - " + reserva.getNombre());

        reservasManager.updateReservationStatus(reserva, "Confirmada", new ReservasManager.UpdateCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "✅ Reserva confirmada exitosamente");

                // ✅ Registrar evento analítico
                registrarAccionReserva(reserva, "confirmar");

                // Recargar estadísticas generales
                if (reserva.getConductor() != null) {
                    calculateStatistics(reserva.getConductor());
                }

                // Recargar estadísticas por ruta
                List<Ruta> rutasActuales = rutasLiveData.getValue();
                List<Reserva> reservasActuales = reservasLiveData.getValue();
                if (rutasActuales != null && reservasActuales != null) {
                    calculateRouteStatistics(rutasActuales, reservasActuales);
                }

                // Actualizar lista de reservas
                if (nombreConductorLiveData.getValue() != null) {
                    loadReservations(nombreConductorLiveData.getValue());
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error confirmando reserva: " + error);
                errorLiveData.postValue("Error confirmando reserva: " + error);

                // ✅ Registrar error analítico
                registrarErrorReserva(reserva, "confirmar", error);
            }
        });
    }

    public void cancelReservation(Reserva reserva) {
        Log.d(TAG, "❌ Cancelando reserva: " + reserva.getIdReserva() + " - " + reserva.getNombre());

        reservasManager.updateReservationStatus(reserva, "Cancelada", new ReservasManager.UpdateCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "✅ Reserva cancelada exitosamente");

                // ✅ Registrar evento analítico
                registrarAccionReserva(reserva, "cancelar");

                // Recargar estadísticas generales
                if (reserva.getConductor() != null) {
                    calculateStatistics(reserva.getConductor());
                }

                // Recargar estadísticas por ruta
                List<Ruta> rutasActuales = rutasLiveData.getValue();
                List<Reserva> reservasActuales = reservasLiveData.getValue();
                if (rutasActuales != null && reservasActuales != null) {
                    calculateRouteStatistics(rutasActuales, reservasActuales);
                }

                // Actualizar lista de reservas
                if (nombreConductorLiveData.getValue() != null) {
                    loadReservations(nombreConductorLiveData.getValue());
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error cancelando reserva: " + error);
                errorLiveData.postValue("Error cancelando reserva: " + error);

                // ✅ Registrar error analítico
                registrarErrorReserva(reserva, "cancelar", error);
            }
        });
    }

    public void updateIncome(double nuevosIngresos) {
        String userId = MyApp.getCurrentUserId();
        if (userId != null) {
            statisticsManager.updateIncomeInFirebase(userId, nuevosIngresos,
                    new DriverStatisticsManager.IncomeUpdateCallback() {
                        @Override
                        public void onSuccess(double ingresosActualizados) {
                            Log.d(TAG, "✅ Ingresos actualizados: $" + ingresosActualizados);
                            ingresosLiveData.postValue(ingresosActualizados);
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "❌ Error actualizando ingresos: " + error);
                            errorLiveData.postValue("Error actualizando ingresos: " + error);
                        }
                    });
        }
    }

    // ✅ Métodos auxiliares para analytics usando MyApp
    private void registrarEventoAnalitico(String evento, String conductorNombre, Integer cantidad) {
        try {
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("viewmodel", TAG);
            params.put("conductor_id", MyApp.getCurrentUserId());

            if (conductorNombre != null) {
                params.put("conductor_nombre", conductorNombre);
            }

            if (cantidad != null) {
                params.put("cantidad", cantidad);
            }

            params.put("timestamp", System.currentTimeMillis());

            MyApp.logEvent("vm_" + evento, params);
            Log.d(TAG, "📊 Evento analítico registrado: vm_" + evento);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando evento analítico: " + e.getMessage());
        }
    }

    private void registrarEstadisticasAnaliticas(String conductorNombre, int reservasConfirmadas,
                                                 int asientosDisp, double ingresos) {
        try {
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("viewmodel", TAG);
            params.put("conductor_id", MyApp.getCurrentUserId());
            params.put("conductor_nombre", conductorNombre);
            params.put("reservas_confirmadas", reservasConfirmadas);
            params.put("asientos_disponibles", asientosDisp);
            params.put("ingresos", ingresos);
            params.put("timestamp", System.currentTimeMillis());

            MyApp.logEvent("vm_estadisticas_calculadas", params);
            Log.d(TAG, "📊 Estadísticas registradas en análisis");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando estadísticas: " + e.getMessage());
        }
    }

    private void registrarAccionReserva(Reserva reserva, String accion) {
        try {
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("viewmodel", TAG);
            params.put("conductor_id", MyApp.getCurrentUserId());
            params.put("reserva_id", reserva.getIdReserva());
            params.put("pasajero_id", reserva.getUsuarioId());
            params.put("pasajero_nombre", reserva.getNombre());
            params.put("accion", accion);
            params.put("ruta", reserva.getOrigen() + " → " + reserva.getDestino());
            params.put("asiento", reserva.getPuestoReservado());
            params.put("precio", reserva.getPrecio());
            params.put("timestamp", System.currentTimeMillis());

            MyApp.logEvent("vm_accion_reserva", params);
            Log.d(TAG, "📊 Acción de reserva registrada: " + accion);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando acción de reserva: " + e.getMessage());
        }
    }

    private void registrarErrorAnalitico(String tipo, String error, String userId) {
        try {
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("viewmodel", TAG);
            params.put("error_tipo", tipo);
            params.put("error_mensaje", error);
            params.put("user_id", userId);
            params.put("timestamp", System.currentTimeMillis());

            MyApp.logEvent("vm_error", params);
            Log.d(TAG, "📊 Error registrado en análisis: " + tipo);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando error analítico: " + e.getMessage());
        }
    }

    private void registrarErrorReserva(Reserva reserva, String accion, String error) {
        try {
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("viewmodel", TAG);
            params.put("conductor_id", MyApp.getCurrentUserId());
            params.put("reserva_id", reserva.getIdReserva());
            params.put("accion_intentada", accion);
            params.put("error_mensaje", error);
            params.put("timestamp", System.currentTimeMillis());

            MyApp.logEvent("vm_error_reserva", params);
            Log.d(TAG, "📊 Error de reserva registrado: " + accion);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando error de reserva: " + e.getMessage());
        }
    }

    // ✅ Método para recargar todos los datos
    public void reloadAllData() {
        String nombreConductor = nombreConductorLiveData.getValue();
        List<String> horarios = horariosLiveData.getValue();

        if (nombreConductor != null && !nombreConductor.isEmpty()) {
            Log.d(TAG, "🔄 Recargando todos los datos para: " + nombreConductor);

            calculateStatistics(nombreConductor);
            loadReservations(nombreConductor);

            if (horarios != null && !horarios.isEmpty()) {
                loadRoutes(horarios);
            }

            // ✅ Registrar evento de recarga
            registrarEventoAnalitico("recarga_datos", nombreConductor, null);
        } else {
            Log.w(TAG, "⚠️ No se puede recargar datos - nombre de conductor no disponible");
        }
    }

    // ✅ Método para limpiar datos
    public void clearData() {
        reservasLiveData.postValue(null);
        rutasLiveData.postValue(null);
        reservasConfirmadasLiveData.postValue(0);
        asientosDisponiblesLiveData.postValue(0);
        ingresosLiveData.postValue(0.0);
        nombreConductorLiveData.postValue(null);
        placaVehiculoLiveData.postValue(null);
        horariosLiveData.postValue(null);
        errorLiveData.postValue(null);

        // Limpiar datos de rutas
        nombreRuta1LiveData.postValue(null);
        reservasRuta1LiveData.postValue(0);
        asientosRuta1LiveData.postValue(0);
        nombreRuta2LiveData.postValue(null);
        reservasRuta2LiveData.postValue(0);
        asientosRuta2LiveData.postValue(0);

        Log.d(TAG, "🧹 Datos limpiados del ViewModel");
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (reservasManager != null) {
            reservasManager.cleanup();
        }
        Log.d(TAG, "🔚 ViewModel destruido");
    }
}