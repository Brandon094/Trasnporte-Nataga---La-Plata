package com.chopcode.trasnportenataga_laplata.managers.dashboard.passenger;

import android.util.Log;

import com.chopcode.trasnportenataga_laplata.config.MyApp;
import com.chopcode.trasnportenataga_laplata.managers.analytics.DashboardAnalyticsHelper;
import com.chopcode.trasnportenataga_laplata.models.Horario;
import com.chopcode.trasnportenataga_laplata.services.reservations.HorarioService;

import java.util.ArrayList;
import java.util.List;

public class ScheduleManager {

    private static final String TAG = "ScheduleManager";

    private final DashboardAnalyticsHelper analyticsHelper;
    private final HorarioService horarioService;

    private List<Horario> listaNataga = new ArrayList<>();
    private List<Horario> listaLaPlata = new ArrayList<>();

    // Callbacks
    public interface ScheduleListener {
        void onSchedulesLoaded(List<Horario> nataga, List<Horario> laPlata);
        void onSchedulesError(String error);
    }

    private ScheduleListener listener;

    public ScheduleManager(DashboardAnalyticsHelper analyticsHelper) {
        this.analyticsHelper = analyticsHelper;
        this.horarioService = new HorarioService();
    }

    public void setScheduleListener(ScheduleListener listener) {
        this.listener = listener;
    }

    public void loadSchedules() {
        Log.d(TAG, "🕒 Cargando horarios...");
        analyticsHelper.logScheduleLoadStart();

        horarioService.cargarHorarios(new HorarioService.HorarioCallback() {
            @Override
            public void onHorariosCargados(List<Horario> nataga, List<Horario> laPlata) {
                Log.d(TAG, "✅ Horarios cargados exitosamente");
                listaNataga.clear();
                listaLaPlata.clear();
                listaNataga.addAll(nataga);
                listaLaPlata.addAll(laPlata);

                analyticsHelper.logSchedulesLoaded(nataga.size(), laPlata.size());

                if (listener != null) {
                    listener.onSchedulesLoaded(nataga, laPlata);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error cargando horarios: " + error);
                analyticsHelper.logError("carga_horarios", error);

                if (listener != null) {
                    listener.onSchedulesError(error);
                }
            }
        });
    }

    public List<Horario> getNatagaSchedules() {
        return listaNataga;
    }

    public List<Horario> getLaPlataSchedules() {
        return listaLaPlata;
    }

    public int getTotalSchedules() {
        return listaNataga.size() + listaLaPlata.size();
    }
}