package com.chopcode.trasnportenataga_laplata.managers.dashboard.passenger;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.chopcode.trasnportenataga_laplata.config.MyApp;
import com.chopcode.trasnportenataga_laplata.managers.analytics.DashboardAnalyticsHelper;
import com.chopcode.trasnportenataga_laplata.models.Usuario;
import com.chopcode.trasnportenataga_laplata.services.user.UserService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class UserDashboardManager {

    private static final String TAG = "UserDashboardManager";

    private final Context context;
    private final DashboardAnalyticsHelper analyticsHelper;
    private final UserService userService;

    // Callbacks
    public interface DashboardListener {
        void onUserDataLoaded(Usuario usuario);
        void onUserDataError(String error);
        void onCountersLoaded(int reservasCount, int viajesCount);
        void onCountersError(String error);
    }

    private DashboardListener listener;
    private Usuario usuarioActual;

    public UserDashboardManager(Context context, DashboardAnalyticsHelper analyticsHelper) {
        this.context = context;
        this.analyticsHelper = analyticsHelper;
        this.userService = new UserService();
    }

    public void setDashboardListener(DashboardListener listener) {
        this.listener = listener;
    }

    public void loadUserData() {
        Log.d(TAG, "🔍 Cargando datos del usuario...");
        analyticsHelper.logScreenLoad();

        String userId = MyApp.getCurrentUserId();
        if (userId == null) {
            if (listener != null) {
                listener.onUserDataError("Usuario no autenticado");
            }
            return;
        }

        userService.loadUserData(userId, new UserService.UserDataCallback() {
            @Override
            public void onUserDataLoaded(Usuario usuario) {
                Log.d(TAG, "✅ Datos de usuario cargados exitosamente");
                usuarioActual = usuario;
                analyticsHelper.logUserLoaded(usuario);

                if (listener != null) {
                    listener.onUserDataLoaded(usuario);
                }

                // Cargar contadores después de tener los datos del usuario
                loadUserCounters(userId);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error cargando datos de usuario: " + error);
                analyticsHelper.logError("carga_usuario", error);

                if (listener != null) {
                    listener.onUserDataError(error);
                }

                // Intentar cargar contadores incluso si falla la carga del usuario
                loadUserCounters(userId);
            }
        });
    }

    private void loadUserCounters(String userId) {
        Log.d(TAG, "📊 Cargando contadores para: " + userId);

        DatabaseReference reservasRef = MyApp.getDatabaseReference("reservas");

        reservasRef.orderByChild("usuarioId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int reservasCount = contarReservasActivas(snapshot);
                        int viajesCount = contarViajesCompletados(snapshot);

                        Log.d(TAG, "📈 Contadores calculados: " + reservasCount + " reservas, " + viajesCount + " viajes");
                        analyticsHelper.logCountersLoaded(reservasCount, viajesCount);

                        if (listener != null) {
                            listener.onCountersLoaded(reservasCount, viajesCount);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "❌ Error cargando contadores: " + error.getMessage());
                        analyticsHelper.logError("carga_contadores", error.getMessage());

                        if (listener != null) {
                            listener.onCountersError(error.getMessage());
                        }
                    }
                });
    }

    private int contarReservasActivas(DataSnapshot snapshot) {
        int count = 0;
        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
            com.chopcode.trasnportenataga_laplata.models.Reserva reserva =
                    dataSnapshot.getValue(com.chopcode.trasnportenataga_laplata.models.Reserva.class);
            if (reserva != null) {
                String estado = reserva.getEstadoReserva();
                if (estado != null && (estado.equals("Confirmada") || estado.equals("Por confirmar"))) {
                    count++;
                }
            }
        }
        return count;
    }

    private int contarViajesCompletados(DataSnapshot snapshot) {
        int count = 0;
        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
            com.chopcode.trasnportenataga_laplata.models.Reserva reserva =
                    dataSnapshot.getValue(com.chopcode.trasnportenataga_laplata.models.Reserva.class);
            if (reserva != null) {
                String estado = reserva.getEstadoReserva();
                if (estado != null && estado.equals("Confirmada")) {
                    count++;
                }
            }
        }
        return count;
    }

    public Usuario getUsuarioActual() {
        return usuarioActual;
    }

    public void refreshData() {
        Log.d(TAG, "🔄 Refrescando datos del dashboard");
        analyticsHelper.logRefresh();
        loadUserData();
    }
}