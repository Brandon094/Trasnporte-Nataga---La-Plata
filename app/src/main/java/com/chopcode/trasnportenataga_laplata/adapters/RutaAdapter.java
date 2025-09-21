package com.chopcode.trasnportenataga_laplata.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.models.Ruta;
import java.util.List;

public class RutaAdapter extends RecyclerView.Adapter<RutaAdapter.RutaViewHolder> {

    private List<Ruta> listaRutas;

    public RutaAdapter(List<Ruta> listaRutas) {
        this.listaRutas = listaRutas;
    }

    @NonNull
    @Override
    public RutaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ruta, parent, false);
        return new RutaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RutaViewHolder holder, int position) {
        Ruta ruta = listaRutas.get(position);
        holder.bind(ruta);
    }

    @Override
    public int getItemCount() {
        return listaRutas != null ? listaRutas.size() : 0;
    }

    public void actualizarLista(List<Ruta> nuevasRutas) {
        this.listaRutas = nuevasRutas;
        notifyDataSetChanged();
    }

    public static class RutaViewHolder extends RecyclerView.ViewHolder {
        private TextView tvOrigen, tvDestino, tvHorario, tvTarifa;

        public RutaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrigen = itemView.findViewById(R.id.tvOrigen);
            tvDestino = itemView.findViewById(R.id.tvDestino);
            tvHorario = itemView.findViewById(R.id.tvHorario);
        }

        public void bind(Ruta ruta) {
            tvOrigen.setText(ruta.getOrigen());
            tvDestino.setText(ruta.getDestino());

            // Mostrar la hora del horario (si existe)
            if (ruta.getHora() != null && ruta.getHora().getHora() != null) {
                tvHorario.setText(ruta.getHora().getHora()); // Esto mostrará "08:00 AM"
            } else {
                tvHorario.setText("--:--");
            }
        }
    }
}