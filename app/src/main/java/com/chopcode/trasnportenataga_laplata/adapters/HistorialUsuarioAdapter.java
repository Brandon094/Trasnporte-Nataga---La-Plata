package com.chopcode.trasnportenataga_laplata.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.models.Reserva;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistorialUsuarioAdapter extends RecyclerView.Adapter<HistorialUsuarioAdapter.ViewHolder> {

    private List<Reserva> reservas;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy - HH:mm", new Locale("es", "ES"));

    public HistorialUsuarioAdapter(List<Reserva> reservas) {
        this.reservas = reservas;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_historial_viaje_usuario, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Reserva reserva = reservas.get(position);
        holder.bind(reserva);
    }

    @Override
    public int getItemCount() {
        return reservas.size();
    }

    public void actualizarDatos(List<Reserva> nuevasReservas) {
        this.reservas = nuevasReservas;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvFecha, tvRuta, tvConductor, tvAsientos, tvPrecioTotal, tvMetodoPago;
        private Chip chipEstado;
        private MaterialButton btnVerDetalles, btnCalificar, btnRepetir;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvRuta = itemView.findViewById(R.id.tvRuta);
            tvConductor = itemView.findViewById(R.id.tvConductor);
            tvAsientos = itemView.findViewById(R.id.tvAsientos);
            tvPrecioTotal = itemView.findViewById(R.id.tvPrecioTotal);
            tvMetodoPago = itemView.findViewById(R.id.tvMetodoPago);
            chipEstado = itemView.findViewById(R.id.chipEstado);
            btnVerDetalles = itemView.findViewById(R.id.btnVerDetalles);
            btnCalificar = itemView.findViewById(R.id.btnCalificar);
            btnRepetir = itemView.findViewById(R.id.btnRepetir);
        }

        public void bind(Reserva reserva) {
            // Formatear y mostrar datos
            tvFecha.setText(formatearFecha(reserva.getFechaReserva()));
            tvRuta.setText(reserva.getOrigen() + " → " + reserva.getDestino());
            tvConductor.setText(reserva.getConductor());
            tvAsientos.setText("Puesto " + reserva.getPuestoReservado() + " - " + reserva.getTiempoEstimado());
            tvPrecioTotal.setText(formatearPrecio(reserva.getPrecio()));
            tvMetodoPago.setText(reserva.getMetodoPago());

            // Estado de la reserva
            String estado = reserva.getEstadoReserva();
            if (estado != null) {
                chipEstado.setText(estado);
                configurarEstadoChip(estado);
            }

            // Mostrar/ocultar botón de calificar solo para viajes confirmados
            if ("confirmado".equalsIgnoreCase(estado) || "Confirmado".equalsIgnoreCase(estado)) {
                btnCalificar.setVisibility(View.VISIBLE);
            } else {
                btnCalificar.setVisibility(View.GONE);
            }

            // Configurar listeners
            btnVerDetalles.setOnClickListener(v -> verDetallesReserva(reserva));
            btnCalificar.setOnClickListener(v -> calificarViaje(reserva));
            btnRepetir.setOnClickListener(v -> repetirReserva(reserva));
        }

        private String formatearFecha(long timestamp) {
            try {
                return dateFormat.format(new Date(timestamp));
            } catch (Exception e) {
                return "Fecha no disponible";
            }
        }

        private String formatearPrecio(double precio) {
            return String.format("$%,.0f", precio);
        }

        private void configurarEstadoChip(String estado) {
            int colorFondo, colorTexto;

            switch (estado.toLowerCase()) {
                case "confirmado":
                    colorFondo = R.color.success_light;
                    colorTexto = R.color.success;
                    break;
                case "cancelado":
                    colorFondo = R.color.error;
                    colorTexto = R.color.error;
                    break;
                case "pendiente":
                    colorFondo = R.color.warning;
                    colorTexto = R.color.warning;
                    break;
                default:
                    colorFondo = R.color.surface;
                    colorTexto = R.color.text_secondary;
                    break;
            }

            // Aplicar colores (implementar según tu tema)
            chipEstado.setChipBackgroundColorResource(colorFondo);
            chipEstado.setTextColor(itemView.getContext().getColor(colorTexto));
        }

        private void verDetallesReserva(Reserva reserva) {
            // Navegar a actividad de detalles
        }

        private void calificarViaje(Reserva reserva) {
            // Abrir diálogo de calificación
        }

        private void repetirReserva(Reserva reserva) {
            // Repetir esta reserva
        }
    }
}