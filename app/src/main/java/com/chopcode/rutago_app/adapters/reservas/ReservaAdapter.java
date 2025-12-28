package com.chopcode.rutago_app.adapters.reservas;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chopcode.rutago_app.R;
import com.chopcode.rutago_app.models.Reserva;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReservaAdapter extends RecyclerView.Adapter<ReservaAdapter.ReservaViewHolder> {

    private static final String TAG = "ReservaAdapter";
    private List<Reserva> reservas;
    private OnReservaClickListener listener;

    public interface OnReservaClickListener {
        void onConfirmarClick(Reserva reserva);
        void onCancelarClick(Reserva reserva);
    }

    public ReservaAdapter(List<Reserva> reservas, OnReservaClickListener listener) {
        Log.d(TAG, "Constructor llamado - Número de reservas: " + (reservas != null ? reservas.size() : 0));
        this.reservas = reservas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReservaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder - Creando nuevo ViewHolder");
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reserva, parent, false);
        return new ReservaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReservaViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder - Posición: " + position + ", Total reservas: " + getItemCount());
        if (reservas != null && position < reservas.size()) {
            Reserva reserva = reservas.get(position);
            Log.d(TAG, "Enlazando reserva: " + reserva.getNombre() + " en posición " + position);
            holder.bind(reserva, listener);
        } else {
            Log.e(TAG, "Error: Índice fuera de rango en onBindViewHolder - posición: " + position);
        }
    }

    @Override
    public int getItemCount() {
        int count = reservas != null ? reservas.size() : 0;
        Log.v(TAG, "getItemCount: " + count);
        return count;
    }

    // 🔥 MÉTODO MEJORADO: Actualizar la lista completa
    public void actualizarReservas(List<Reserva> nuevasReservas) {
        Log.d(TAG, "actualizarReservas - Nuevo tamaño: " + (nuevasReservas != null ? nuevasReservas.size() : 0) +
                ", Viejo tamaño: " + (reservas != null ? reservas.size() : 0));

        this.reservas = nuevasReservas;
        notifyDataSetChanged();

        Log.i(TAG, "Lista de reservas actualizada y notificada al adapter");
    }

    static class ReservaViewHolder extends RecyclerView.ViewHolder {
        private static final String TAG = "ReservaViewHolder";
        private TextView tvNombre, tvTelefono, tvOrigenDestino, tvFechaHora, tvAsiento, tvEstado;
        private MaterialButton btnConfirmar, btnCancelar;

        public ReservaViewHolder(@NonNull View itemView) {
            super(itemView);
            Log.d(TAG, "ReservaViewHolder constructor - Inicializando vistas");
            tvNombre = itemView.findViewById(R.id.tvNombrePasajero);
            tvTelefono = itemView.findViewById(R.id.tvTelefono);
            tvOrigenDestino = itemView.findViewById(R.id.tvOrigenDestino);
            tvFechaHora = itemView.findViewById(R.id.tvFechaHora);
            tvAsiento = itemView.findViewById(R.id.tvAsiento);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            btnConfirmar = itemView.findViewById(R.id.btnConfirmar);
            btnCancelar = itemView.findViewById(R.id.btnCancelar);

            // Verificar que todas las vistas se encontraron correctamente
            if (tvNombre == null) Log.w(TAG, "tvNombre no encontrado");
            if (tvTelefono == null) Log.w(TAG, "tvTelefono no encontrado");
            if (tvOrigenDestino == null) Log.w(TAG, "tvOrigenDestino no encontrado");
            if (tvFechaHora == null) Log.w(TAG, "tvFechaHora no encontrado");
            if (tvAsiento == null) Log.w(TAG, "tvAsiento no encontrado");
            if (tvEstado == null) Log.w(TAG, "tvEstado no encontrado");
            if (btnConfirmar == null) Log.w(TAG, "btnConfirmar no encontrado");
            if (btnCancelar == null) Log.w(TAG, "btnCancelar no encontrado");
        }

        public void bind(Reserva reserva, OnReservaClickListener listener) {
            Log.d(TAG, "bind - Vinculando reserva: " + reserva.getNombre() +
                    ", Asiento: " + reserva.getPuestoReservado() +
                    ", Estado: " + reserva.getEstadoReserva());

            // 🔥 DATOS DEL PASAJERO
            tvNombre.setText(reserva.getNombre() != null ? reserva.getNombre() : "Nombre no disponible");
            tvTelefono.setText(reserva.getTelefono() != null ? "📞 " + reserva.getTelefono() : "📞 Teléfono no disponible");

            // 🔥 RUTA Y ORIGEN-DESTINO
            if (reserva.getOrigen() != null && reserva.getDestino() != null) {
                tvOrigenDestino.setText("📍 " + reserva.getOrigen() + " → " + reserva.getDestino());
            } else {
                tvOrigenDestino.setText("📍 Ruta no especificada");
                Log.w(TAG, "Ruta no especificada para reserva: " + reserva.getNombre());
            }

            // 🔥 ASIENTO
            if (reserva.getPuestoReservado() != null) {
                tvAsiento.setText("💺 Asiento " + reserva.getPuestoReservado());
            } else {
                tvAsiento.setText("💺 Asiento no asignado");
                Log.w(TAG, "Asiento no asignado para reserva: " + reserva.getNombre());
            }

            // 🔥 FECHA Y HORA
            if (reserva.getFechaReserva() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                String fechaFormateada = "🕒 " + sdf.format(new Date(reserva.getFechaReserva()));
                tvFechaHora.setText(fechaFormateada);
            } else {
                tvFechaHora.setText("🕒 Fecha no disponible");
                Log.w(TAG, "Fecha no disponible para reserva: " + reserva.getNombre());
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
                        Log.d(TAG, "Reserva en estado 'Por confirmar' - Botones visibles");
                        break;
                    case "Confirmada":
                        tvEstado.setBackgroundResource(R.drawable.bg_estado_confirmado);
                        btnConfirmar.setVisibility(View.GONE);
                        btnCancelar.setVisibility(View.GONE);
                        Log.d(TAG, "Reserva en estado 'Confirmada' - Botones ocultos");
                        break;
                    case "Cancelada":
                        tvEstado.setBackgroundResource(R.drawable.bg_estado_cancelado);
                        btnConfirmar.setVisibility(View.GONE);
                        btnCancelar.setVisibility(View.GONE);
                        Log.d(TAG, "Reserva en estado 'Cancelada' - Botones ocultos");
                        break;
                    default:
                        tvEstado.setBackgroundResource(R.drawable.bg_estado_pendiente);
                        Log.w(TAG, "Estado desconocido: " + reserva.getEstadoReserva());
                        break;
                }
            } else {
                tvEstado.setText("Desconocido");
                tvEstado.setBackgroundResource(R.drawable.bg_estado_pendiente);
                Log.e(TAG, "Estado nulo para reserva: " + reserva.getNombre());
            }

            // 🔥 LISTENERS DE BOTONES
            btnConfirmar.setOnClickListener(v -> {
                Log.i(TAG, "Botón Confirmar clickeado - Reserva: " + reserva.getNombre());
                if (listener != null) {
                    listener.onConfirmarClick(reserva);
                } else {
                    Log.e(TAG, "Listener nulo al hacer click en Confirmar");
                }
            });

            btnCancelar.setOnClickListener(v -> {
                Log.i(TAG, "Botón Cancelar clickeado - Reserva: " + reserva.getNombre());
                if (listener != null) {
                    listener.onCancelarClick(reserva);
                } else {
                    Log.e(TAG, "Listener nulo al hacer click en Cancelar");
                }
            });

            // 🔥 LOG PARA DEBUG
            Log.d(TAG, "Reserva mostrada exitosamente: " + reserva.getNombre() +
                    " - Asiento: " + reserva.getPuestoReservado() +
                    " - Estado: " + reserva.getEstadoReserva());
        }
    }
}