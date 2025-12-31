package com.chopcode.trasnportenataga_laplata.managers.dashboard.passenger;

import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.managers.analytics.DashboardAnalyticsHelper;
import com.chopcode.trasnportenataga_laplata.models.Usuario;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

public class DashboardUIManager {

    private final DashboardAnalyticsHelper analyticsHelper;

    // UI References
    private TextView tvUserName, tvWelcome, tvReservasCount, tvViajesCount;
    private MaterialButton btnEditarPerfil, btnRefresh;

    // Callbacks
    public interface UIActionsListener {
        void onEditProfileClicked();
        void onRefreshClicked();
        void onProfileMenuItemClicked();
    }

    private UIActionsListener listener;

    public DashboardUIManager(DashboardAnalyticsHelper analyticsHelper) {
        this.analyticsHelper = analyticsHelper;
    }

    public void setUIActionsListener(UIActionsListener listener) {
        this.listener = listener;
    }

    public void setViewReferences(
            TextView tvUserName, TextView tvWelcome,
            TextView tvReservasCount, TextView tvViajesCount,
            MaterialButton btnEditarPerfil, MaterialButton btnRefresh) {

        this.tvUserName = tvUserName;
        this.tvWelcome = tvWelcome;
        this.tvReservasCount = tvReservasCount;
        this.tvViajesCount = tvViajesCount;
        this.btnEditarPerfil = btnEditarPerfil;
        this.btnRefresh = btnRefresh;
    }

    public void setupToolbar(Toolbar toolbar) {
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_perfil) {
                analyticsHelper.logMenuItemClick("perfil");
                if (listener != null) {
                    listener.onProfileMenuItemClicked();
                }
                return true;
            }
            return false;
        });
    }

    public void setupButtonListeners() {
        btnEditarPerfil.setOnClickListener(view -> {
            analyticsHelper.logButtonClick("editar_perfil");
            if (listener != null) {
                listener.onEditProfileClicked();
            }
        });

        btnRefresh.setOnClickListener(view -> {
            analyticsHelper.logButtonClick("actualizar");
            if (listener != null) {
                listener.onRefreshClicked();
            }
        });
    }

    public void updateUserInfo(Usuario usuario) {
        if (usuario != null && usuario.getNombre() != null) {
            tvUserName.setText(usuario.getNombre());

            String firstName = usuario.getNombre().split(" ")[0];
            tvWelcome.setText("¡Bienvenido, " + firstName + "!");
        }
    }

    public void updateCounters(int reservasCount, int viajesCount) {
        tvReservasCount.setText(String.valueOf(reservasCount));
        tvViajesCount.setText(String.valueOf(viajesCount));
    }

    public void showRefreshMessage() {
        // Este método se llamaría desde la Activity
        // Toast.makeText(context, "Actualizando información...", Toast.LENGTH_SHORT).show();
    }

    public void showErrorMessage(String message) {
        // Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public void setupTabLayout(TabLayout tabLayout, androidx.viewpager2.widget.ViewPager2 viewPager) {
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    if (position == 0) {
                        tab.setText("Natagá → La Plata");
                    } else {
                        tab.setText("La Plata → Natagá");
                    }
                }
        ).attach();
    }
}