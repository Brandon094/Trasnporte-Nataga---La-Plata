package com.chopcode.trasnportenataga_laplata.adapters.rutas;

import android.util.Log;
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

    private static final String TAG = "RutaAdapter";
    private static final String SUB_TAG = "RUTAS_ADAPTER";

    private List<Ruta> listaRutas;

    public RutaAdapter(List<Ruta> listaRutas) {
        Log.d(TAG, SUB_TAG + " - Constructor llamado con " +
                (listaRutas != null ? listaRutas.size() : "null") + " rutas");
        this.listaRutas = listaRutas;
    }

    @NonNull
    @Override
    public RutaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.v(TAG, SUB_TAG + " - onCreateViewHolder para viewType: " + viewType);

        try {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ruta, parent, false);
            Log.d(TAG, SUB_TAG + " - Vista item_ruta inflada exitosamente");
            return new RutaViewHolder(view);
        } catch (Exception e) {
            Log.e(TAG, SUB_TAG + " - Error al inflar layout item_ruta: " + e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RutaViewHolder holder, int position) {
        Log.v(TAG, SUB_TAG + " - onBindViewHolder para posición: " + position);

        if (listaRutas == null || listaRutas.isEmpty()) {
            Log.w(TAG, SUB_TAG + " - Lista de rutas vacía o nula en onBindViewHolder");
            return;
        }

        if (position < 0 || position >= listaRutas.size()) {
            Log.e(TAG, SUB_TAG + " - Posición inválida: " + position + ", tamaño lista: " + listaRutas.size());
            return;
        }

        try {
            Ruta ruta = listaRutas.get(position);
            Log.d(TAG, SUB_TAG + " - Enlazando ruta en posición " + position +
                    ": " + ruta.getOrigen() + " → " + ruta.getDestino());

            holder.bind(ruta);
            Log.d(TAG, SUB_TAG + " - Ruta enlazada exitosamente en posición: " + position);

        } catch (Exception e) {
            Log.e(TAG, SUB_TAG + " - Error en onBindViewHolder posición " + position +
                    ": " + e.getMessage(), e);
        }
    }

    @Override
    public int getItemCount() {
        int count = listaRutas != null ? listaRutas.size() : 0;
        Log.v(TAG, SUB_TAG + " - getItemCount: " + count);
        return count;
    }

    public void actualizarRutas(List<Ruta> nuevasRutas) {
        Log.i(TAG, SUB_TAG + " - actualizarLista llamado. " +
                "Lista anterior: " + (listaRutas != null ? listaRutas.size() : "null") +
                ", Nueva lista: " + (nuevasRutas != null ? nuevasRutas.size() : "null"));

        int cambios = 0;
        if (listaRutas != null && nuevasRutas != null) {
            cambios = Math.abs(listaRutas.size() - nuevasRutas.size());
        }

        this.listaRutas = nuevasRutas;
        notifyDataSetChanged();

        Log.d(TAG, SUB_TAG + " - Lista actualizada. Notificando cambios. " +
                "Items afectados: " + cambios);
    }

    public static class RutaViewHolder extends RecyclerView.ViewHolder {
        private static final String VIEW_HOLDER_TAG = "RutaViewHolder";

        private TextView tvOrigen, tvDestino, tvHorario;

        public RutaViewHolder(@NonNull View itemView) {
            super(itemView);
            Log.d(TAG, VIEW_HOLDER_TAG + " - Constructor llamado");

            try {
                tvOrigen = itemView.findViewById(R.id.tvOrigen);
                tvDestino = itemView.findViewById(R.id.tvDestino);
                tvHorario = itemView.findViewById(R.id.tvHorario);

                Log.d(TAG, VIEW_HOLDER_TAG + " - Views inicializadas: " +
                        (tvOrigen != null) + ", " +
                        (tvDestino != null) + ", " +
                        (tvHorario != null));

            } catch (Exception e) {
                Log.e(TAG, VIEW_HOLDER_TAG + " - Error al inicializar views: " + e.getMessage(), e);
                throw e;
            }
        }

        public void bind(Ruta ruta) {
            Log.v(TAG, VIEW_HOLDER_TAG + " - bind llamado para ruta: " +
                    ruta.getOrigen() + " → " + ruta.getDestino());

            try {
                // Validar datos antes de mostrarlos
                if (ruta.getOrigen() != null && !ruta.getOrigen().isEmpty()) {
                    tvOrigen.setText(ruta.getOrigen());
                    Log.d(TAG, VIEW_HOLDER_TAG + " - Origen establecido: " + ruta.getOrigen());
                } else {
                    tvOrigen.setText("Origen no disponible");
                    Log.w(TAG, VIEW_HOLDER_TAG + " - Origen vacío o nulo");
                }

                if (ruta.getDestino() != null && !ruta.getDestino().isEmpty()) {
                    tvDestino.setText(ruta.getDestino());
                    Log.d(TAG, VIEW_HOLDER_TAG + " - Destino establecido: " + ruta.getDestino());
                } else {
                    tvDestino.setText("Destino no disponible");
                    Log.w(TAG, VIEW_HOLDER_TAG + " - Destino vacío o nulo");
                }

                // Mostrar la hora del horario (si existe)
                if (ruta.getHora() != null && ruta.getHora().getHora() != null) {
                    String hora = ruta.getHora().getHora();
                    tvHorario.setText(hora);
                    Log.d(TAG, VIEW_HOLDER_TAG + " - Horario establecido: " + hora);
                } else {
                    tvHorario.setText("--:--");
                    Log.w(TAG, VIEW_HOLDER_TAG + " - Horario no disponible");
                }

                Log.i(TAG, VIEW_HOLDER_TAG + " - Ruta enlazada exitosamente: " +
                        ruta.getOrigen() + " → " + ruta.getDestino());

            } catch (Exception e) {
                Log.e(TAG, VIEW_HOLDER_TAG + " - Error en bind: " + e.getMessage(), e);
                // Fallback seguro
                tvOrigen.setText("Error");
                tvDestino.setText("Error");
                tvHorario.setText("--:--");
            }
        }
    }
}