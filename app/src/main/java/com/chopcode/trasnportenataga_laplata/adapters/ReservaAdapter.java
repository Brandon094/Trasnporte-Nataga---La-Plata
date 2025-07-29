package com.chopcode.trasnportenataga_laplata.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.models.Reserva;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReservaAdapter extends RecyclerView.Adapter<ReservaAdapter.ReservaViewHolder> {

    private List<Reserva> reservas;
    private OnReservaClickListener listener;

    public interface OnReservaClickListener {
        void onConfirmarClick(Reserva reserva);
        void onCancelarClick(Reserva reserva);
    }

    public ReservaAdapter(List<Reserva> reservas, OnReservaClickListener listener) {
        this.reservas = reservas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReservaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reserva, parent, false);
        return new ReservaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReservaViewHolder holder, int position) {
        Reserva reserva = reservas.get(position);
        holder.bind(reserva, listener);
    }

    @Override
    public int getItemCount() {
        return reservas.size();
    }

    public void actualizarReservas(List<Reserva> nuevasReservas) {
        reservas.clear();
        reservas.addAll(nuevasReservas);
        notifyDataSetChanged();
    }

    static class ReservaViewHolder extends RecyclerView.ViewHolder {
        private TextView tvNombre, tvOrigen, tvDestino, tvHora, tvPrecio;
        private MaterialButton btnConfirmar, btnCancelar;

        public ReservaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreUsuario);
            tvOrigen = itemView.findViewById(R.id.tvOrigen);
            tvDestino = itemView.findViewById(R.id.tvDestino);
            tvHora = itemView.findViewById(R.id.tvFechaHora);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
            btnConfirmar = itemView.findViewById(R.id.btnConfirmar);
            btnCancelar = itemView.findViewById(R.id.btnCancelar);
        }

        public void bind(Reserva reserva, OnReservaClickListener listener) {
            tvNombre.setText(reserva.getNombre());
            tvOrigen.setText(reserva.getOrigen());
            tvDestino.setText(reserva.getDestino());
            tvHora.setText(reserva.getHorarioId());
            tvPrecio.setText(String.format(Locale.getDefault(), "$%.2f", reserva.getPrecio()));

            btnConfirmar.setOnClickListener(v -> listener.onConfirmarClick(reserva));
            btnCancelar.setOnClickListener(v -> listener.onCancelarClick(reserva));
        }
    }
}