package com.chopcode.trasnportenataga_laplata.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        return reservas != null ? reservas.size() : 0;
    }

    // 🔥 MÉTODO MEJORADO: Actualizar la lista completa
    public void actualizarReservas(List<Reserva> nuevasReservas) {
        this.reservas = nuevasReservas;
        notifyDataSetChanged();
    }

    static class ReservaViewHolder extends RecyclerView.ViewHolder {
        private TextView tvNombre, tvTelefono, tvOrigenDestino, tvFechaHora, tvAsiento, tvEstado;
        private MaterialButton btnConfirmar, btnCancelar;

        public ReservaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombrePasajero);
            tvTelefono = itemView.findViewById(R.id.tvTelefono);
            tvOrigenDestino = itemView.findViewById(R.id.tvOrigenDestino);
            tvFechaHora = itemView.findViewById(R.id.tvFechaHora);
            tvAsiento = itemView.findViewById(R.id.tvAsiento);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            btnConfirmar = itemView.findViewById(R.id.btnConfirmar);
            btnCancelar = itemView.findViewById(R.id.btnCancelar);
        }

        public void bind(Reserva reserva, OnReservaClickListener listener) {
            // 🔥 DATOS DEL PASAJERO
            tvNombre.setText(reserva.getNombre() != null ? reserva.getNombre() : "Nombre no disponible");
            tvTelefono.setText(reserva.getTelefono() != null ? "📞 " + reserva.getTelefono() : "📞 Teléfono no disponible");

            // 🔥 RUTA Y ORIGEN-DESTINO
            if (reserva.getOrigen() != null && reserva.getDestino() != null) {
                tvOrigenDestino.setText("📍 " + reserva.getOrigen() + " → " + reserva.getDestino());
            } else {
                tvOrigenDestino.setText("📍 Ruta no especificada");
            }

            // 🔥 ASIENTO
            if (reserva.getPuestoReservado() != null) {
                tvAsiento.setText("💺 Asiento " + reserva.getPuestoReservado());
            } else {
                tvAsiento.setText("💺 Asiento no asignado");
            }

            // 🔥 FECHA Y HORA
            if (reserva.getFechaReserva() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                String fechaFormateada = "🕒 " + sdf.format(new Date(reserva.getFechaReserva()));
                tvFechaHora.setText(fechaFormateada);
            } else {
                tvFechaHora.setText("🕒 Fecha no disponible");
            }

            // 🔥 ESTADO DE LA RESERVA CON COLORES DINÁMICOS
            if (reserva.getEstadoReserva() != null) {
                tvEstado.setText(reserva.getEstadoReserva());

                // Cambiar color según el estado
                switch (reserva.getEstadoReserva()) {
                    case "Por confirmar":
                        tvEstado.setBackgroundResource(R.drawable.bg_estado_pendiente);
                        btnConfirmar.setVisibility(View.VISIBLE);
                        btnCancelar.setVisibility(View.VISIBLE);
                        break;
                    case "Confirmada":
                        tvEstado.setBackgroundResource(R.drawable.bg_estado_confirmado);
                        btnConfirmar.setVisibility(View.GONE);
                        btnCancelar.setVisibility(View.GONE);
                        break;
                    case "Cancelada":
                        tvEstado.setBackgroundResource(R.drawable.bg_estado_cancelado);
                        btnConfirmar.setVisibility(View.GONE);
                        btnCancelar.setVisibility(View.GONE);
                        break;
                    default:
                        tvEstado.setBackgroundResource(R.drawable.bg_estado_pendiente);
                        break;
                }
            } else {
                tvEstado.setText("Desconocido");
                tvEstado.setBackgroundResource(R.drawable.bg_estado_pendiente);
            }

            // 🔥 LISTENERS DE BOTONES
            btnConfirmar.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConfirmarClick(reserva);
                }
            });

            btnCancelar.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCancelarClick(reserva);
                }
            });

            // 🔥 LOG PARA DEBUG
            Log.d("RESERVA_ADAPTER", "Reserva mostrada: " + reserva.getNombre() +
                    " - Asiento: " + reserva.getPuestoReservado() +
                    " - Estado: " + reserva.getEstadoReserva());
        }
    }
}