package com.chopcode.trasnportenataga_laplata.managers.ui;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.config.MyApp;

import java.util.HashMap;
import java.util.Map;

/**
 * Manager para manejar secciones expandibles/collapsibles en la UI.
 * Proporciona funcionalidad para mostrar/ocultar contenido con animaciones.
 */
public class ExpandableSectionManager {
    private static final String TAG = "ExpandableSectionManager";

    // Views
    private final RelativeLayout headerView;
    private final LinearLayout contentView;
    private final LinearLayout summaryView;
    private final ImageView expandIcon;
    private final TextView routeSummary;
    private final TextView scheduleSummary;

    // Estado
    private boolean isExpanded = true;
    private final Context context;

    // Callback para eventos
    private ExpandableCallback callback;

    // Información para analytics
    private String screenName = "UnknownScreen";
    private String sectionName = "DefaultSection";

    /**
     * Interfaz para callback de eventos de la sección expandible
     */
    public interface ExpandableCallback {
        void onSectionExpanded();
        void onSectionCollapsed();
        void onToggleClicked(boolean isNowExpanded);
    }

    /**
     * Constructor para el manager
     * @param context Contexto de la actividad/fragmento
     * @param headerView Header clicable de la sección
     * @param contentView Contenido que se expande/colapsa
     * @param summaryView Vista de resumen (visible cuando colapsado)
     * @param expandIcon Icono de expandir/colapsar
     * @param routeSummary TextView para ruta en resumen
     * @param scheduleSummary TextView para horario en resumen
     */
    public ExpandableSectionManager(Context context,
                                    RelativeLayout headerView,
                                    LinearLayout contentView,
                                    LinearLayout summaryView,
                                    ImageView expandIcon,
                                    TextView routeSummary,
                                    TextView scheduleSummary) {
        this.context = context;
        this.headerView = headerView;
        this.contentView = contentView;
        this.summaryView = summaryView;
        this.expandIcon = expandIcon;
        this.routeSummary = routeSummary;
        this.scheduleSummary = scheduleSummary;

        setupHeaderClickListener();
        initializeViews();
    }

    /**
     * Configuración básica con valores por defecto
     */
    public ExpandableSectionManager(Context context,
                                    RelativeLayout headerView,
                                    LinearLayout contentView,
                                    ImageView expandIcon) {
        this(context, headerView, contentView, null, expandIcon, null, null);
    }

    private void setupHeaderClickListener() {
        if (headerView != null) {
            headerView.setOnClickListener(v -> toggleSection());
        }
    }

    private void initializeViews() {
        // Inicialmente expandido
        if (contentView != null && summaryView != null) {
            contentView.setVisibility(View.VISIBLE);
            summaryView.setVisibility(View.GONE);
            expandIcon.setImageResource(R.drawable.ic_expand_less);
        }
    }

    /**
     * Alterna el estado de la sección (expandir/colapsar)
     */
    public void toggleSection() {
        isExpanded = !isExpanded;

        if (isExpanded) {
            expandSection();
        } else {
            collapseSection();
        }

        // Ejecutar callback si está configurado
        if (callback != null) {
            callback.onToggleClicked(isExpanded);
        }

        // Registrar evento analítico
        logAnalyticsEvent();
    }

    /**
     * Expande la sección con animación
     */
    public void expandSection() {
        if (contentView == null || expandIcon == null) return;

        // Mostrar contenido
        contentView.setVisibility(View.VISIBLE);
        expandIcon.setImageResource(R.drawable.ic_expand_less);

        // Ocultar resumen si existe
        if (summaryView != null) {
            summaryView.setVisibility(View.GONE);
        }

        // Aplicar animación
        applyAnimation(contentView, R.anim.expand_animation);

        Log.d(TAG, "Sección expandida: " + sectionName);

        if (callback != null) {
            callback.onSectionExpanded();
        }
    }

    /**
     * Colapsa la sección con animación
     */
    public void collapseSection() {
        if (contentView == null || expandIcon == null) return;

        // Cambiar icono
        expandIcon.setImageResource(R.drawable.ic_expand_more);

        // Mostrar resumen si existe
        if (summaryView != null) {
            summaryView.setVisibility(View.VISIBLE);
        }

        // Aplicar animación y ocultar después
        applyAnimation(contentView, R.anim.collapse_animation, () -> {
            contentView.setVisibility(View.GONE);
        });

        Log.d(TAG, "Sección colapsada: " + sectionName);

        if (callback != null) {
            callback.onSectionCollapsed();
        }
    }

    /**
     * Aplica animación a una vista
     */
    private void applyAnimation(View view, int animationResId) {
        applyAnimation(view, animationResId, null);
    }

    /**
     * Aplica animación a una vista con callback al finalizar
     */
    private void applyAnimation(View view, int animationResId, Runnable onAnimationEnd) {
        Animation animation = AnimationUtils.loadAnimation(context, animationResId);

        if (onAnimationEnd != null) {
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    onAnimationEnd.run();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
        }

        view.startAnimation(animation);
    }

    /**
     * Actualiza la información del resumen
     * @param routeText Texto de la ruta
     * @param scheduleText Texto del horario
     */
    public void updateSummaryInfo(String routeText, String scheduleText) {
        if (routeSummary != null && routeText != null) {
            String shortenedRoute = routeText;

            // Si es una ruta con flecha, simplificarla
            if (routeText.contains(" -> ")) {
                String[] parts = routeText.split(" -> ");
                if (parts.length >= 2) {
                    String origen = parts[0].trim();
                    String destino = parts[1].trim();

                    // Tomar solo iniciales o abreviaturas
                    if (origen.contains("Natagá")) {
                        origen = "Nat.";
                    } else if (origen.contains("La Plata")) {
                        origen = "L.P.";
                    }

                    if (destino.contains("Natagá")) {
                        destino = "Nat.";
                    } else if (destino.contains("La Plata")) {
                        destino = "L.P.";
                    }

                    shortenedRoute = origen + " → " + destino;
                }
            }

            // Limitar más agresivamente para el header
            if (shortenedRoute.length() > 15) {
                shortenedRoute = shortenedRoute.substring(0, 12) + "...";
            }

            routeSummary.setText(shortenedRoute);
        }

        if (scheduleSummary != null && scheduleText != null) {
            String shortSchedule = scheduleText;

            // Separar AM/PM y la hora
            if (shortSchedule.contains(" ")) {
                String[] parts = shortSchedule.split(" ");
                if (parts.length >= 2) {
                    // Mostrar primero AM/PM, luego la hora
                    String amPm = parts[1]; // "AM" o "PM"
                    String hora = parts[0]; // "6:15"

                    // Formato: "AM 6:15" o "PM 8:30"
                    shortSchedule = hora + " " + amPm;
                }
            }

            scheduleSummary.setText(shortSchedule);
        }
    }

    /**
     * Actualiza solo la ruta del resumen
     */
    public void updateRouteSummary(String routeText) {
        updateSummaryInfo(routeText, null);
    }

    /**
     * Actualiza solo el horario del resumen
     */
    public void updateScheduleSummary(String scheduleText) {
        updateSummaryInfo(null, scheduleText);
    }

    /**
     * Configura el callback para eventos
     */
    public void setExpandableCallback(ExpandableCallback callback) {
        this.callback = callback;
    }

    /**
     * Establece nombres para analytics
     */
    public void setAnalyticsInfo(String screenName, String sectionName) {
        this.screenName = screenName != null ? screenName : "UnknownScreen";
        this.sectionName = sectionName != null ? sectionName : "DefaultSection";
    }

    /**
     * Obtiene el estado actual de la sección
     */
    public boolean isExpanded() {
        return isExpanded;
    }

    /**
     * Establece el estado de la sección
     */
    public void setExpanded(boolean expanded) {
        if (this.isExpanded != expanded) {
            this.isExpanded = expanded;

            if (expanded) {
                expandSection();
            } else {
                collapseSection();
            }
        }
    }

    /**
     * Restaura el estado desde un Bundle
     */
    public void restoreState(boolean wasExpanded) {
        this.isExpanded = wasExpanded;

        if (wasExpanded) {
            if (contentView != null) contentView.setVisibility(View.VISIBLE);
            if (summaryView != null) summaryView.setVisibility(View.GONE);
            if (expandIcon != null) expandIcon.setImageResource(R.drawable.ic_expand_less);
        } else {
            if (contentView != null) contentView.setVisibility(View.GONE);
            if (summaryView != null) summaryView.setVisibility(View.VISIBLE);
            if (expandIcon != null) expandIcon.setImageResource(R.drawable.ic_expand_more);
        }
    }

    /**
     * Método para colapsar automáticamente basado en ciertas condiciones
     */
    public void autoCollapseIfNeeded(boolean condition) {
        if (condition && isExpanded) {
            collapseSection();
        }
    }

    /**
     * Registra evento analítico
     */
    private void logAnalyticsEvent() {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("user_id", MyApp.getCurrentUserId());
            params.put("pantalla", screenName);
            params.put("seccion", sectionName);
            params.put("estado", isExpanded ? "expandido" : "colapsado");
            params.put("timestamp", System.currentTimeMillis());

            MyApp.logEvent("seccion_expandible_toggle", params);
            Log.d(TAG, "📊 Evento analítico registrado para sección: " + sectionName);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando evento analítico: " + e.getMessage());
        }
    }

    /**
     * Limpia recursos y listeners
     */
    public void cleanup() {
        if (headerView != null) {
            headerView.setOnClickListener(null);
        }
        callback = null;
    }
}