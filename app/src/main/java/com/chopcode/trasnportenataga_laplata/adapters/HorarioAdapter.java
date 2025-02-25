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
            String ruta = (horario.getRuta() != null) ? horario.getRuta().trim() : "Ruta desconocida";
            String hora = (horario.getHora() != null) ? horario.getHora() : "Hora no disponible";

            holder.tvHorario.setText(ruta + "\n" + hora);
        }
    }

    @Override
    public int getItemCount() {
        return horarios.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvHorario;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHorario = itemView.findViewById(R.id.tvHorario);
        }
    }
}
