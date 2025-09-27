package com.chopcode.trasnportenataga_laplata.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.models.Horario;
import java.util.ArrayList;
import java.util.List;

public class HorarioAdapter extends RecyclerView.Adapter<HorarioAdapter.ViewHolder> {

    private static final String TAG = "HorarioAdapter";
    private List<Horario> horarios;

    public HorarioAdapter(List<Horario> horarios) {
        this.horarios = (horarios != null) ? new ArrayList<>(horarios) : new ArrayList<>();
        Log.d(TAG, "Adapter creado con " + this.horarios.size() + " horarios");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_horario, parent, false);
        Log.d(TAG, "onCreateViewHolder llamado");
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < horarios.size()) {
            Horario horario = horarios.get(position);
            Log.d(TAG, "onBindViewHolder - Posición: " + position +
                    ", Hora: " + horario.getHora() + ", Ruta: " + horario.getRuta());
            holder.bind(horario);
        } else {
            Log.e(TAG, "Índice fuera de rango: " + position + ", tamaño: " + horarios.size());
        }
    }

    @Override
    public int getItemCount() {
        int count = horarios.size();
        Log.d(TAG, "getItemCount: " + count);
        return count;
    }

    public void actualizarHorarios(List<Horario> nuevosHorarios) {
        Log.d(TAG, "actualizarHorarios llamado - Nuevos datos: " +
                (nuevosHorarios != null ? nuevosHorarios.size() : "null"));

        this.horarios.clear();
        if (nuevosHorarios != null) {
            this.horarios.addAll(nuevosHorarios);
        }

        Log.d(TAG, "Datos actualizados, notificando cambios. Nuevo tamaño: " + horarios.size());
        notifyDataSetChanged();

        // Log detallado de los horarios
        for (int i = 0; i < horarios.size(); i++) {
            Horario h = horarios.get(i);
            Log.d(TAG, "Horario " + i + ": " + h.getHora() + " - " + h.getRuta());
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private static final String TAG = "HorarioViewHolder";
        public TextView tvHora, tvAmPm, tvRuta, tvAsientos, tvPrecio;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHora = itemView.findViewById(R.id.tvHora);
            tvAmPm = itemView.findViewById(R.id.tvAmPm);
            tvRuta = itemView.findViewById(R.id.tvRuta);
            tvAsientos = itemView.findViewById(R.id.tvAsientos);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);

            // Verificar que las vistas se encontraron
            if (tvHora == null) Log.e(TAG, "tvHora es null");
            if (tvAmPm == null) Log.e(TAG, "tvAmPm es null");
            if (tvRuta == null) Log.e(TAG, "tvRuta es null");
            if (tvAsientos == null) Log.e(TAG, "tvAsientos es null");
            if (tvPrecio == null) Log.e(TAG, "tvPrecio es null");
        }

        public void bind(Horario horario) {
            try {
                Log.d(TAG, "bind llamado para horario: " + horario.getHora() + " - " + horario.getRuta());

                // Formato esperado: "06:00 AM" o "06:00 PM"
                String[] horaParts = separarHoraYAmPm(horario.getHora());
                if (tvHora != null) tvHora.setText(horaParts[0]); // "06:00"
                if (tvAmPm != null) tvAmPm.setText(horaParts[1]); // "AM" o "PM"

                // Ruta
                String ruta = horario.getRuta() != null ? horario.getRuta() : "Ruta no disponible";
                if (tvRuta != null) tvRuta.setText(ruta);

                // Asientos disponibles
                int asientosDisponibles = horario.getAsientosDisponibles() > 0 ?
                        horario.getAsientosDisponibles() : 14;
                String textoAsientos = asientosDisponibles + " asientos disponibles";
                if (tvAsientos != null) tvAsientos.setText(textoAsientos);

                // Precio
                String precio = horario.getPrecio() != null ?
                        formatearPrecio(horario.getPrecio()) : "$12.000";
                if (tvPrecio != null) tvPrecio.setText(precio);

                // Cambiar colores según disponibilidad
                actualizarColoresSegunDisponibilidad(asientosDisponibles);

                Log.d(TAG, "bind completado exitosamente");

            } catch (Exception e) {
                Log.e(TAG, "Error en bind: " + e.getMessage(), e);
                // Valores por defecto en caso de error
                if (tvHora != null) tvHora.setText("--:--");
                if (tvAmPm != null) tvAmPm.setText("");
                if (tvRuta != null) tvRuta.setText("Error");
                if (tvAsientos != null) tvAsientos.setText("0 asientos");
                if (tvPrecio != null) tvPrecio.setText("$0.0");
            }
        }

        private String[] separarHoraYAmPm(String horaCompleta) {
            String[] resultado = {"--:--", ""};

            if (horaCompleta != null && !horaCompleta.trim().isEmpty()) {
                try {
                    horaCompleta = horaCompleta.trim().toUpperCase();
                    Log.d(TAG, "Procesando hora: " + horaCompleta);

                    int espacioIndex = horaCompleta.indexOf(" ");

                    if (espacioIndex > 0) {
                        resultado[0] = horaCompleta.substring(0, espacioIndex).trim();
                        resultado[1] = horaCompleta.substring(espacioIndex).trim();
                    } else {
                        resultado[0] = horaCompleta;
                    }

                    Log.d(TAG, "Hora separada: [" + resultado[0] + "] [" + resultado[1] + "]");

                } catch (Exception e) {
                    Log.e(TAG, "Error separando hora: " + e.getMessage());
                }
            } else {
                Log.w(TAG, "Hora completa es null o vacía");
            }

            return resultado;
        }

        private String formatearPrecio(String precio) {
            try {
                if (precio.contains("$")) {
                    return precio;
                } else if (precio.matches("\\d+")) {
                    return "$" + precio;
                } else if (precio.matches("\\d+\\.?\\d*")) {
                    return "$" + precio;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error formateando precio: " + e.getMessage());
            }
            return precio;
        }

        private void actualizarColoresSegunDisponibilidad(int asientosDisponibles) {
            try {
                int colorAsientos;

                if (asientosDisponibles == 0) {
                    colorAsientos = itemView.getContext().getColor(R.color.error);
                    if (tvHora != null) tvHora.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
                    if (tvAmPm != null) tvAmPm.setTextColor(itemView.getContext().getColor(R.color.text_tertiary));
                } else if (asientosDisponibles < 5) {
                    colorAsientos = itemView.getContext().getColor(R.color.status_pending);
                    if (tvHora != null) tvHora.setTextColor(itemView.getContext().getColor(R.color.primary_500));
                    if (tvAmPm != null) tvAmPm.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
                } else {
                    colorAsientos = itemView.getContext().getColor(R.color.success);
                    if (tvHora != null) tvHora.setTextColor(itemView.getContext().getColor(R.color.primary_500));
                    if (tvAmPm != null) tvAmPm.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
                }

                if (tvAsientos != null) tvAsientos.setTextColor(colorAsientos);

            } catch (Exception e) {
                Log.e(TAG, "Error actualizando colores: " + e.getMessage());
            }
        }
    }
}
