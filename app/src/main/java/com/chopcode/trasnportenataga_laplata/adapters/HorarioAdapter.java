package com.chopcode.trasnportenataga_laplata.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.models.Horario;
import java.util.List;

public class HorarioAdapter extends RecyclerView.Adapter<HorarioAdapter.ViewHolder> {

    private List<Horario> horarios;

    public HorarioAdapter(List<Horario> horarios) {
        this.horarios = (horarios != null) ? horarios : new java.util.ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_horario, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Horario horario = horarios.get(position);
        if (horario != null) {
            holder.bind(horario);
        }
    }

    @Override
    public int getItemCount() {
        return horarios.size();
    }

    public void actualizarHorarios(List<Horario> nuevosHorarios) {
        this.horarios = (nuevosHorarios != null) ? nuevosHorarios : new java.util.ArrayList<>();
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvHora, tvAmPm, tvRuta, tvAsientos, tvPrecio;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHora = itemView.findViewById(R.id.tvHora);
            tvAmPm = itemView.findViewById(R.id.tvAmPm);
            tvRuta = itemView.findViewById(R.id.tvRuta);
            tvAsientos = itemView.findViewById(R.id.tvAsientos);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
        }

        public void bind(Horario horario) {
            try {
                // Formato esperado: "06:00 AM" o "06:00 PM"
                String[] horaParts = separarHoraYAmPm(horario.getHora());
                tvHora.setText(horaParts[0]); // "06:00"
                tvAmPm.setText(horaParts[1]); // "AM" o "PM"

                // Ruta
                String ruta = horario.getRuta() != null ? horario.getRuta() : "Ruta no disponible";
                tvRuta.setText(ruta);

                // Asientos disponibles (usar valor del modelo si existe)
                int asientosDisponibles = horario.getAsientosDisponibles() > 0 ?
                        horario.getAsientosDisponibles() : 14; // Valor por defecto
                String textoAsientos = asientosDisponibles + " asientos disponibles";
                tvAsientos.setText(textoAsientos);

                // Precio (usar valor del modelo si existe)
                String precio = horario.getPrecio() != null ?
                        formatearPrecio(horario.getPrecio()) : "$12.000";
                tvPrecio.setText(precio);

                // Cambiar colores según disponibilidad
                actualizarColoresSegunDisponibilidad(asientosDisponibles);

            } catch (Exception e) {
                e.printStackTrace();
                // Valores por defecto en caso de error
                tvHora.setText("--:--");
                tvAmPm.setText("");
                tvRuta.setText("Ruta no disponible");
                tvAsientos.setText("0 asientos disponibles");
                tvPrecio.setText("$0.0");
            }
        }

        private String[] separarHoraYAmPm(String horaCompleta) {
            String[] resultado = {"--:--", ""};

            if (horaCompleta != null && !horaCompleta.trim().isEmpty()) {
                try {
                    horaCompleta = horaCompleta.trim().toUpperCase();

                    // Buscar la posición donde termina la hora y empieza AM/PM
                    int espacioIndex = horaCompleta.indexOf(" ");

                    if (espacioIndex > 0) {
                        // Separar en dos partes: hora y AM/PM
                        resultado[0] = horaCompleta.substring(0, espacioIndex).trim(); // "06:00"
                        resultado[1] = horaCompleta.substring(espacioIndex).trim();    // "AM" o "PM"
                    } else {
                        // Si no tiene espacio, usar todo como hora
                        resultado[0] = horaCompleta;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    resultado[0] = horaCompleta;
                }
            }

            return resultado;
        }

        private String formatearPrecio(String precio) {
            try {
                // Si el precio ya tiene formato, dejarlo tal cual
                if (precio.contains("$")) {
                    return precio;
                }
                // Si es solo número, agregar símbolo de peso
                else if (precio.matches("\\d+")) {
                    return "$" + precio;
                }
                // Si tiene decimales o otros formatos
                else if (precio.matches("\\d+\\.?\\d*")) {
                    return "$" + precio;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return precio; // Devolver original si no se puede formatear
        }

        private void actualizarColoresSegunDisponibilidad(int asientosDisponibles) {
            int colorAsientos;

            if (asientosDisponibles == 0) {
                colorAsientos = itemView.getContext().getColor(R.color.error);
                tvHora.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
                tvAmPm.setTextColor(itemView.getContext().getColor(R.color.text_tertiary));
            } else if (asientosDisponibles < 5) {
                colorAsientos = itemView.getContext().getColor(R.color.status_pending);
                tvHora.setTextColor(itemView.getContext().getColor(R.color.primary_500));
                tvAmPm.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
            } else {
                colorAsientos = itemView.getContext().getColor(R.color.success);
                tvHora.setTextColor(itemView.getContext().getColor(R.color.primary_500));
                tvAmPm.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
            }

            tvAsientos.setTextColor(colorAsientos);
        }
    }
}