package com.chopcode.trasnportenataga_laplata.adapters.horarios;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.models.Horario;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class HorarioAdapter extends RecyclerView.Adapter<HorarioAdapter.ViewHolder> {

    private static final String TAG = "HorarioAdapter";
    private List<Horario> horarios;
    private OnReservarClickListener listener;

    // Interface para el click listener
    public interface OnReservarClickListener {
        void onReservarClick(Horario horario);
    }

    // Nuevo constructor con listener
    public HorarioAdapter(List<Horario> horarios, OnReservarClickListener listener) {
        this.horarios = (horarios != null) ? new ArrayList<>(horarios) : new ArrayList<>();
        this.listener = listener;
        Log.d(TAG, "Adapter creado con " + this.horarios.size() + " horarios y listener: " +
                (listener != null ? "presente" : "NULO"));

        // Log detallado de los primeros horarios
        if (!this.horarios.isEmpty()) {
            Log.i(TAG, "Primeros 3 horarios cargados:");
            for (int i = 0; i < Math.min(3, this.horarios.size()); i++) {
                Horario h = this.horarios.get(i);
                Log.i(TAG, "  [" + i + "] " + h.getHora() + " - " + h.getRuta() +
                        " - Asientos: " + h.getAsientosDisponibles());
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder - viewType: " + viewType);
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_horario, parent, false);
        Log.d(TAG, "View inflado exitosamente");
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder - Posición: " + position + "/" + (getItemCount() - 1));

        if (position < horarios.size()) {
            Horario horario = horarios.get(position);
            Log.d(TAG, "onBindViewHolder - Posición: " + position +
                    ", Hora: " + horario.getHora() +
                    ", Ruta: " + horario.getRuta() +
                    ", Asientos: " + horario.getAsientosDisponibles());
            holder.bind(horario, listener);
        } else {
            Log.e(TAG, "Índice fuera de rango: " + position + ", tamaño: " + horarios.size());
        }
    }

    @Override
    public int getItemCount() {
        int count = horarios.size();
        Log.v(TAG, "getItemCount: " + count);
        return count;
    }

    public void actualizarHorarios(List<Horario> nuevosHorarios) {
        Log.i(TAG, "=== INICIANDO ACTUALIZACIÓN DE HORARIOS ===");
        Log.d(TAG, "actualizarHorarios llamado - Nuevos datos: " +
                (nuevosHorarios != null ? nuevosHorarios.size() : "null") +
                ", Datos actuales: " + horarios.size());

        int tamañoAnterior = horarios.size();
        this.horarios.clear();
        if (nuevosHorarios != null) {
            this.horarios.addAll(nuevosHorarios);
        }

        Log.d(TAG, "Datos actualizados - Anterior: " + tamañoAnterior +
                ", Nuevo: " + horarios.size() + ", notificando cambios");

        // Log detallado de los horarios actualizados
        if (!horarios.isEmpty()) {
            Log.i(TAG, "Resumen de horarios actualizados:");
            for (int i = 0; i < Math.min(5, horarios.size()); i++) {
                Horario h = horarios.get(i);
                Log.i(TAG, "  [" + i + "] " + h.getHora() + " - " + h.getRuta() +
                        " - Asientos: " + h.getAsientosDisponibles());
            }
            if (horarios.size() > 5) {
                Log.i(TAG, "  ... y " + (horarios.size() - 5) + " más");
            }
        } else {
            Log.w(TAG, "Lista de horarios actualizada está VACÍA");
        }

        notifyDataSetChanged();
        Log.i(TAG, "=== ACTUALIZACIÓN COMPLETADA ===");
    }

    // Método para diagnóstico del estado del adapter
    public void logEstadoCompleto() {
        Log.i(TAG, "=== DIAGNÓSTICO HORARIO ADAPTER ===");
        Log.d(TAG, "Total horarios: " + horarios.size());
        Log.d(TAG, "Listener: " + (listener != null ? "PRESENTE" : "AUSENTE"));

        for (int i = 0; i < horarios.size(); i++) {
            Horario h = horarios.get(i);
            Log.d(TAG, String.format("Horario [%d]: %s - %s - Asientos: %d - Precio: %s",
                    i, h.getHora(), h.getRuta(), h.getAsientosDisponibles(), h.getPrecio()));
        }
        Log.i(TAG, "==================================");
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private static final String TAG = "HorarioViewHolder";
        public TextView tvHora, tvAmPm, tvRuta, tvAsientos, tvPrecio;
        public FloatingActionButton btnReservar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            Log.d(TAG, "ViewHolder creado para posición: " + getAdapterPosition());

            tvHora = itemView.findViewById(R.id.tvHora);
            tvAmPm = itemView.findViewById(R.id.tvAmPm);
            tvRuta = itemView.findViewById(R.id.tvRuta);
            tvAsientos = itemView.findViewById(R.id.tvAsientos);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
            btnReservar = itemView.findViewById(R.id.btnReservar);

            // Verificar que las vistas se encontraron
            int vistasEncontradas = 0;
            int vistasFaltantes = 0;

            if (tvHora != null) vistasEncontradas++; else { Log.e(TAG, "❌ tvHora es null"); vistasFaltantes++; }
            if (tvAmPm != null) vistasEncontradas++; else { Log.e(TAG, "❌ tvAmPm es null"); vistasFaltantes++; }
            if (tvRuta != null) vistasEncontradas++; else { Log.e(TAG, "❌ tvRuta es null"); vistasFaltantes++; }
            if (tvAsientos != null) vistasEncontradas++; else { Log.e(TAG, "❌ tvAsientos es null"); vistasFaltantes++; }
            if (tvPrecio != null) vistasEncontradas++; else { Log.e(TAG, "❌ tvPrecio es null"); vistasFaltantes++; }
            if (btnReservar != null) vistasEncontradas++; else { Log.e(TAG, "❌ btnReservar es null"); vistasFaltantes++; }

            Log.i(TAG, "Vistas inicializadas: " + vistasEncontradas + " OK, " + vistasFaltantes + " FALTANTES");
        }

        public void bind(Horario horario, OnReservarClickListener listener) {
            int position = getAdapterPosition();
            Log.d(TAG, "bind iniciado - Posición: " + position +
                    ", Hora: " + horario.getHora() + ", Ruta: " + horario.getRuta());

            try {
                // Formato esperado: "06:00 AM" o "06:00 PM"
                String[] horaParts = separarHoraYAmPm(horario.getHora());
                if (tvHora != null) {
                    tvHora.setText(horaParts[0]); // "06:00"
                    Log.v(TAG, "Hora establecida: " + horaParts[0]);
                }
                if (tvAmPm != null) {
                    tvAmPm.setText(horaParts[1]); // "AM" o "PM"
                    Log.v(TAG, "AM/PM establecido: " + horaParts[1]);
                }

                // Ruta
                String ruta = horario.getRuta() != null ? horario.getRuta() : "Ruta no disponible";
                if (tvRuta != null) {
                    tvRuta.setText(ruta);
                    Log.v(TAG, "Ruta establecida: " + ruta);
                }

                // Asientos disponibles
                int asientosDisponibles = horario.getAsientosDisponibles() > 0 ?
                        horario.getAsientosDisponibles() : 14;
                String textoAsientos = asientosDisponibles + " asientos disponibles";
                if (tvAsientos != null) {
                    tvAsientos.setText(textoAsientos);
                    Log.v(TAG, "Asientos establecidos: " + textoAsientos);
                }

                // Precio
                String precio = horario.getPrecio() != null ?
                        formatearPrecio(horario.getPrecio()) : "$12.000";
                if (tvPrecio != null) {
                    tvPrecio.setText(precio);
                    Log.v(TAG, "Precio establecido: " + precio);
                }

                // Cambiar colores según disponibilidad
                actualizarColoresSegunDisponibilidad(asientosDisponibles);

                // Configurar el click listener SOLO en el botón
                if (btnReservar != null) {
                    btnReservar.setOnClickListener(v -> {
                        Log.i(TAG, "Botón Reservar clickeado - Posición: " + position +
                                ", Hora: " + horario.getHora() + ", Ruta: " + horario.getRuta());
                        if (listener != null) {
                            listener.onReservarClick(horario);
                            Log.d(TAG, "Listener ejecutado exitosamente");
                        } else {
                            Log.e(TAG, "❌ Listener es NULO - No se puede procesar click");
                        }
                    });
                    Log.v(TAG, "Listener del botón configurado - Habilitado: " + btnReservar.isEnabled());
                } else {
                    Log.e(TAG, "❌ btnReservar es null - No se puede configurar listener");
                }

                Log.i(TAG, "bind completado exitosamente para posición: " + position);

            } catch (Exception e) {
                Log.e(TAG, "❌ Error crítico en bind - Posición: " + position +
                        ", Error: " + e.getMessage(), e);
                // Valores por defecto en caso de error
                establecerValoresPorDefecto();
            }
        }

        private String[] separarHoraYAmPm(String horaCompleta) {
            String[] resultado = {"--:--", ""};

            if (horaCompleta != null && !horaCompleta.trim().isEmpty()) {
                try {
                    horaCompleta = horaCompleta.trim().toUpperCase();
                    Log.v(TAG, "Procesando hora completa: '" + horaCompleta + "'");

                    int espacioIndex = horaCompleta.indexOf(" ");

                    if (espacioIndex > 0) {
                        resultado[0] = horaCompleta.substring(0, espacioIndex).trim();
                        resultado[1] = horaCompleta.substring(espacioIndex).trim();
                        Log.v(TAG, "Hora separada: parte='" + resultado[0] + "', ampm='" + resultado[1] + "'");
                    } else {
                        resultado[0] = horaCompleta;
                        Log.v(TAG, "Sin separador AM/PM, usando: '" + resultado[0] + "'");
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error separando hora: " + e.getMessage());
                }
            } else {
                Log.w(TAG, "Hora completa es null o vacía, usando valores por defecto");
            }

            return resultado;
        }

        private String formatearPrecio(String precio) {
            try {
                if (precio == null || precio.trim().isEmpty()) {
                    Log.w(TAG, "Precio es null o vacío, usando valor por defecto");
                    return "$12.000";
                }

                String precioLimpio = precio.trim();
                Log.v(TAG, "Formateando precio: '" + precioLimpio + "'");

                if (precioLimpio.contains("$")) {
                    Log.v(TAG, "Precio ya tiene formato correcto");
                    return precioLimpio;
                } else if (precioLimpio.matches("\\d+")) {
                    String resultado = "$" + precioLimpio;
                    Log.v(TAG, "Precio formateado a: '" + resultado + "'");
                    return resultado;
                } else if (precioLimpio.matches("\\d+\\.?\\d*")) {
                    String resultado = "$" + precioLimpio;
                    Log.v(TAG, "Precio decimal formateado a: '" + resultado + "'");
                    return resultado;
                } else {
                    Log.w(TAG, "Formato de precio no reconocido: '" + precioLimpio + "'");
                    return "$" + precioLimpio; // Intentar formatear de todos modos
                }
            } catch (Exception e) {
                Log.e(TAG, "Error formateando precio: " + e.getMessage());
                return "$12.000"; // Valor por defecto seguro
            }
        }

        private void actualizarColoresSegunDisponibilidad(int asientosDisponibles) {
            try {
                Log.v(TAG, "Actualizando colores - Asientos disponibles: " + asientosDisponibles);
                int colorAsientos;

                if (asientosDisponibles == 0) {
                    colorAsientos = itemView.getContext().getColor(R.color.error);
                    if (tvHora != null) tvHora.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
                    if (tvAmPm != null) tvAmPm.setTextColor(itemView.getContext().getColor(R.color.text_tertiary));
                    // Deshabilitar botón si no hay asientos
                    if (btnReservar != null) {
                        btnReservar.setEnabled(false);
                        btnReservar.setAlpha(0.5f);
                    }
                    Log.v(TAG, "Estado: SIN ASIENTOS - Botón deshabilitado");
                } else if (asientosDisponibles < 5) {
                    colorAsientos = itemView.getContext().getColor(R.color.status_pending);
                    if (tvHora != null) tvHora.setTextColor(itemView.getContext().getColor(R.color.primary_500));
                    if (tvAmPm != null) tvAmPm.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
                    // Habilitar botón
                    if (btnReservar != null) {
                        btnReservar.setEnabled(true);
                        btnReservar.setAlpha(1.0f);
                    }
                    Log.v(TAG, "Estado: POCOS ASIENTOS (" + asientosDisponibles + ") - Botón habilitado");
                } else {
                    colorAsientos = itemView.getContext().getColor(R.color.success);
                    if (tvHora != null) tvHora.setTextColor(itemView.getContext().getColor(R.color.primary_500));
                    if (tvAmPm != null) tvAmPm.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
                    // Habilitar botón
                    if (btnReservar != null) {
                        btnReservar.setEnabled(true);
                        btnReservar.setAlpha(1.0f);
                    }
                    Log.v(TAG, "Estado: SUFICIENTES ASIENTOS (" + asientosDisponibles + ") - Botón habilitado");
                }

                if (tvAsientos != null) tvAsientos.setTextColor(colorAsientos);
                Log.v(TAG, "Colores actualizados exitosamente");

            } catch (Exception e) {
                Log.e(TAG, "Error actualizando colores: " + e.getMessage());
            }
        }

        private void establecerValoresPorDefecto() {
            Log.w(TAG, "Estableciendo valores por defecto debido a error");
            if (tvHora != null) tvHora.setText("--:--");
            if (tvAmPm != null) tvAmPm.setText("");
            if (tvRuta != null) tvRuta.setText("Error");
            if (tvAsientos != null) tvAsientos.setText("0 asientos");
            if (tvPrecio != null) tvPrecio.setText("$0.0");
            if (btnReservar != null) {
                btnReservar.setEnabled(false);
                btnReservar.setAlpha(0.5f);
            }
        }
    }
}