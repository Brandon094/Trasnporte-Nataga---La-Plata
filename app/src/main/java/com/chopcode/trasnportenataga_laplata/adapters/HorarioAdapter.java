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
        public TextView tvHora, tvRuta;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHora = itemView.findViewById(R.id.tvHora);
            tvRuta = itemView.findViewById(R.id.tvRuta);
        }

        public void bind(Horario horario) {
            try {
                // Hora
                String hora = horario.getHora() != null ? horario.getHora() : "--:--";
                // Limpiar formato si es necesario
                if (hora.length() > 5 && hora.contains(":")) {
                    hora = hora.substring(0, 5);
                }
                tvHora.setText(hora);

                // Ruta
                String ruta = horario.getRuta() != null ? horario.getRuta() : "Ruta no disponible";
                tvRuta.setText(ruta);

            } catch (Exception e) {
                e.printStackTrace();
                // Valores por defecto
                tvHora.setText("--:--");
                tvRuta.setText("Ruta no disponible");
            }
        }
    }
}