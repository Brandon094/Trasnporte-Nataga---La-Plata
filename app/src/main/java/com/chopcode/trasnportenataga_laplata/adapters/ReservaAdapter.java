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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReservaAdapter extends RecyclerView.Adapter<ReservaAdapter.ViewHolder> {

    public interface OnReservaClickListener {
        void onConfirmarClick(Reserva reserva);
        void onCancelarClick(Reserva reserva);
    }

    private List<Reserva> reservas;
    private final OnReservaClickListener listener;
    private final SimpleDateFormat dateFormat;

    public ReservaAdapter(List<Reserva> reservas, OnReservaClickListener listener) {
        this.reservas = reservas;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    }

    public void actualizarReservas(List<Reserva> nuevasReservas) {
        this.reservas = nuevasReservas;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reserva, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Reserva reserva = reservas.get(position);
        holder.bind(reserva, listener);
    }

    @Override
    public int getItemCount() {
        return reservas != null ? reservas.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvNombrePasajero;
        private final TextView tvTelefono;
        private final TextView tvOrigenDestino;
        private final TextView tvFechaHora;
        private final TextView tvAsiento;
        private final TextView tvEstado;
        private final Button btnConfirmar;
        private final Button btnCancelar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombrePasajero = itemView.findViewById(R.id.tvNombrePasajero);
            tvTelefono = itemView.findViewById(R.id.tvTelefono);
            tvOrigenDestino = itemView.findViewById(R.id.tvOrigenDestino);
            tvFechaHora = itemView.findViewById(R.id.tvFechaHora);
            tvAsiento = itemView.findViewById(R.id.tvAsiento);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            btnConfirmar = itemView.findViewById(R.id.btnConfirmar);
            btnCancelar = itemView.findViewById(R.id.btnCancelar);
        }

        public void bind(Reserva reserva, OnReservaClickListener listener) {
            // Información básica del pasajero
            tvNombrePasajero.setText(reserva.getNombre());
            tvTelefono.setText(reserva.getTelefono());

            // Detalles del viaje
            tvOrigenDestino.setText(String.format("%s → %s", reserva.getOrigen(), reserva.getDestino()));
            tvAsiento.setText(String.format("Asiento: %d", reserva.getPuestoReservado()));

            // Fecha y hora formateadas
            String fechaHora = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(new Date(reserva.getFechaReserva()));
            tvFechaHora.setText(fechaHora);

            // Estado de la reserva
            tvEstado.setText(reserva.getEstadoReserva());
            tvEstado.setTextColor(itemView.getContext().getResources().getColor(
                    "Por confirmar".equals(reserva.getEstadoReserva()) ?
                            R.color.colorEstadoPendiente :
                            R.color.colorEstadoConfirmado
            ));

            // Configurar botones
            btnConfirmar.setOnClickListener(v -> listener.onConfirmarClick(reserva));
            btnCancelar.setOnClickListener(v -> listener.onCancelarClick(reserva));

            // Ocultar botones si ya no está pendiente
            if (!"Por confirmar".equals(reserva.getEstadoReserva())) {
                btnConfirmar.setVisibility(View.GONE);
                btnCancelar.setVisibility(View.GONE);
            } else {
                btnConfirmar.setVisibility(View.VISIBLE);
                btnCancelar.setVisibility(View.VISIBLE);
            }
        }
    }
}