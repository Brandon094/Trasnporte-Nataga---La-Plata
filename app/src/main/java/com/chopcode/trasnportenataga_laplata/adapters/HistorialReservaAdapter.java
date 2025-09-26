package com.chopcode.trasnportenataga_laplata.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.models.Reserva;

import java.util.List;

public class HistorialReservaAdapter extends RecyclerView.Adapter<HistorialReservaAdapter.ReservaViewHolder> {

    private List<Reserva> listaReservas;
    private final OnReservaClickListener listener;

    public interface OnReservaClickListener {
        void onReservaClick(Reserva reserva);
        void onVerDetallesClick(Reserva reserva);
    }

    public HistorialReservaAdapter(List<Reserva> listaReservas, OnReservaClickListener listener) {
        this.listaReservas = listaReservas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReservaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_historial_reserva, parent, false);
        return new ReservaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReservaViewHolder holder, int position) {
        Reserva reserva = listaReservas.get(position);
        holder.bind(reserva, listener);
    }

    @Override
    public int getItemCount() {
        return listaReservas.size();
    }

    public void actualizarLista(List<Reserva> nuevaLista) {
        this.listaReservas = nuevaLista;
        notifyDataSetChanged();
    }

    static class ReservaViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvFecha, tvEstado, tvPasajero, tvTelefono, tvRuta, tvPuesto, tvPrecio;

        public ReservaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            tvPasajero = itemView.findViewById(R.id.tvPasajero);
            tvTelefono = itemView.findViewById(R.id.tvTelefono);
            tvRuta = itemView.findViewById(R.id.tvRuta);
            tvPuesto = itemView.findViewById(R.id.tvPuesto);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
        }

        public void bind(Reserva reserva, OnReservaClickListener listener) {
            tvFecha.setText(reserva.getFechaReserva() + " - " + reserva.getHorarioId());
            tvEstado.setText(reserva.getEstadoReserva());
            tvPasajero.setText(reserva.getNombre());
            tvTelefono.setText(reserva.getTelefono());
            tvRuta.setText(reserva.getOrigen() + " → " + reserva.getDestino());
            tvPuesto.setText("Puesto " + reserva.getPuestoReservado());
            tvPrecio.setText("$" + reserva.getPrecio());

            // Configurar color según estado
            int colorEstado = getColorEstado(reserva.getEstadoReserva());
            tvEstado.setTextColor(colorEstado);

            itemView.setOnClickListener(v -> listener.onReservaClick(reserva));

            itemView.findViewById(R.id.btnDetalles).setOnClickListener(v ->
                    listener.onVerDetallesClick(reserva));
        }

        private int getColorEstado(String estado) {
            switch (estado.toUpperCase()) {
                case "CONFIRMADA":
                    return itemView.getContext().getColor(R.color.status_confirmed);
                case "CANCELADA":
                    return itemView.getContext().getColor(R.color.status_cancelled);
                case "PENDIENTE":
                    return itemView.getContext().getColor(R.color.status_pending);
                default:
                    return itemView.getContext().getColor(R.color.text_secondary);
            }
        }
    }
}