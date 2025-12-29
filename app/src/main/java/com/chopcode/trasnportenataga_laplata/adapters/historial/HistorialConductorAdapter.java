package com.chopcode.trasnportenataga_laplata.adapters.historial;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.config.MyApp;
import com.chopcode.trasnportenataga_laplata.models.Reserva;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistorialConductorAdapter extends RecyclerView.Adapter<HistorialConductorAdapter.ReservaViewHolder> {

    private static final String TAG = "HistorialConductorAdapter";
    private List<Reserva> listaReservas;
    private final OnReservaClickListener listener;

    public interface OnReservaClickListener {
        void onReservaClick(Reserva reserva);
        void onVerDetallesClick(Reserva reserva);
    }

    public HistorialConductorAdapter(List<Reserva> listaReservas, OnReservaClickListener listener) {
        Log.d(TAG, "Constructor - Inicializando adapter con " +
                (listaReservas != null ? listaReservas.size() : "null") + " reservas");

        // ✅ Registrar evento analítico de creación de adapter
        registrarEventoAnalitico("adapter_historial_conductor_creado",
                listaReservas != null ? listaReservas.size() : 0, null);

        this.listaReservas = listaReservas;
        this.listener = listener;

        // Log de diagnóstico inicial
        if (listaReservas != null && !listaReservas.isEmpty()) {
            Log.i(TAG, "Primeras 3 reservas cargadas:");
            for (int i = 0; i < Math.min(3, listaReservas.size()); i++) {
                Reserva r = listaReservas.get(i);
                Log.i(TAG, "  [" + i + "] " + r.getNombre() + " - " +
                        r.getOrigen() + " → " + r.getDestino() + " - " + r.getEstadoReserva());
            }
        } else {
            Log.w(TAG, "Lista de reservas inicial está " +
                    (listaReservas == null ? "NULL" : "VACÍA"));
        }

        Log.d(TAG, "Listener: " + (listener != null ? "PRESENTE" : "AUSENTE"));
    }

    @NonNull
    @Override
    public ReservaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder - viewType: " + viewType);

        // ✅ Registrar evento analítico
        registrarEventoAnalitico("viewholder_historial_creado", viewType, null);

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_historial_reserva, parent, false);
        Log.d(TAG, "View inflado exitosamente para item_historial_reserva");
        return new ReservaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReservaViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder - Posición: " + position + "/" + (getItemCount() - 1));

        if (position < listaReservas.size()) {
            Reserva reserva = listaReservas.get(position);
            Log.d(TAG, "Enlazando reserva - Posición: " + position +
                    ", Pasajero: " + reserva.getNombre() +
                    ", Ruta: " + reserva.getOrigen() + " → " + reserva.getDestino() +
                    ", Estado: " + reserva.getEstadoReserva());

            // ✅ Registrar evento analítico de binding
            registrarReservaBindAnalitico(reserva, position);

            holder.bind(reserva, listener);
        } else {
            Log.e(TAG, "❌ Índice fuera de rango en onBindViewHolder - posición: " + position +
                    ", tamaño: " + listaReservas.size());

            // ✅ Usar MyApp para logging de errores
            MyApp.logError(new IndexOutOfBoundsException(
                    "Índice fuera de rango en HistorialConductorAdapter: " + position +
                            ", tamaño: " + listaReservas.size()
            ));
        }
    }

    @Override
    public int getItemCount() {
        int count = listaReservas != null ? listaReservas.size() : 0;
        Log.v(TAG, "getItemCount: " + count);
        return count;
    }

    public void actualizarLista(List<Reserva> nuevaLista) {
        Log.i(TAG, "=== ACTUALIZANDO LISTA DE RESERVAS CONDUCTOR ===");
        Log.d(TAG, "actualizarLista - Nueva lista: " +
                (nuevaLista != null ? nuevaLista.size() : "null") +
                " reservas, Actual: " + (listaReservas != null ? listaReservas.size() : "null"));

        int tamañoAnterior = listaReservas != null ? listaReservas.size() : 0;
        this.listaReservas = nuevaLista;

        Log.d(TAG, "Lista actualizada - Anterior: " + tamañoAnterior +
                ", Nuevo: " + (listaReservas != null ? listaReservas.size() : "null"));

        // ✅ Registrar evento analítico de actualización
        registrarActualizacionListaAnalitico(tamañoAnterior,
                listaReservas != null ? listaReservas.size() : 0);

        // Log resumen de las nuevas reservas
        if (listaReservas != null && !listaReservas.isEmpty()) {
            Log.i(TAG, "Resumen de reservas actualizadas:");
            for (int i = 0; i < Math.min(3, listaReservas.size()); i++) {
                Reserva r = listaReservas.get(i);
                Log.i(TAG, "  [" + i + "] " + r.getNombre() + " - " +
                        r.getOrigen() + " → " + r.getDestino() + " - " + r.getEstadoReserva());
            }
            if (listaReservas.size() > 3) {
                Log.i(TAG, "  ... y " + (listaReservas.size() - 3) + " más");
            }
        } else {
            Log.w(TAG, "⚠️ Lista de reservas actualizada está " +
                    (listaReservas == null ? "NULL" : "VACÍA"));
        }

        Log.d(TAG, "Notificando cambio de dataset");
        notifyDataSetChanged();
        Log.i(TAG, "=== ACTUALIZACIÓN COMPLETADA ===");
    }

    // Método para diagnóstico del estado del adapter
    public void logEstadoCompleto() {
        Log.i(TAG, "=== DIAGNÓSTICO HISTORIAL CONDUCTOR ===");
        Log.d(TAG, "Total reservas: " + (listaReservas != null ? listaReservas.size() : "null"));
        Log.d(TAG, "Listener: " + (listener != null ? "PRESENTE" : "AUSENTE"));

        if (listaReservas != null) {
            // ✅ Registrar evento de diagnóstico
            registrarDiagnosticoAnalitico(listaReservas.size());

            for (int i = 0; i < listaReservas.size(); i++) {
                Reserva r = listaReservas.get(i);
                Log.d(TAG, String.format("Reserva [%d]: %s - %s → %s - %s - $%s",
                        i, r.getNombre(), r.getOrigen(), r.getDestino(),
                        r.getEstadoReserva(), r.getPrecio()));
            }
        }
        Log.i(TAG, "======================================");
    }

    static class ReservaViewHolder extends RecyclerView.ViewHolder {
        private static final String TAG = "HistorialConductorVH";
        private final TextView tvFecha, tvEstado, tvPasajero, tvTelefono, tvRuta, tvPuesto, tvPrecio;

        public ReservaViewHolder(@NonNull View itemView) {
            super(itemView);
            Log.d(TAG, "ViewHolder creado para posición: " + getAdapterPosition());

            // ✅ Registrar evento de creación de ViewHolder
            registrarEventoViewHolderAnalitico("viewholder_historial_creado", getAdapterPosition());

            // Inicializar vistas
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            tvPasajero = itemView.findViewById(R.id.tvPasajero);
            tvTelefono = itemView.findViewById(R.id.tvTelefono);
            tvRuta = itemView.findViewById(R.id.tvRuta);
            tvPuesto = itemView.findViewById(R.id.tvPuesto);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);

            // Verificar que todas las vistas se encontraron
            int vistasEncontradas = 0;
            int vistasFaltantes = 0;

            if (tvFecha != null) vistasEncontradas++; else {
                Log.e(TAG, "❌ tvFecha es null");
                vistasFaltantes++;
                MyApp.logError(new NullPointerException("tvFecha es null en HistorialConductorVH"));
            }
            if (tvEstado != null) vistasEncontradas++; else {
                Log.e(TAG, "❌ tvEstado es null");
                vistasFaltantes++;
                MyApp.logError(new NullPointerException("tvEstado es null en HistorialConductorVH"));
            }
            if (tvPasajero != null) vistasEncontradas++; else {
                Log.e(TAG, "❌ tvPasajero es null");
                vistasFaltantes++;
                MyApp.logError(new NullPointerException("tvPasajero es null en HistorialConductorVH"));
            }
            if (tvTelefono != null) vistasEncontradas++; else {
                Log.e(TAG, "❌ tvTelefono es null");
                vistasFaltantes++;
                MyApp.logError(new NullPointerException("tvTelefono es null en HistorialConductorVH"));
            }
            if (tvRuta != null) vistasEncontradas++; else {
                Log.e(TAG, "❌ tvRuta es null");
                vistasFaltantes++;
                MyApp.logError(new NullPointerException("tvRuta es null en HistorialConductorVH"));
            }
            if (tvPuesto != null) vistasEncontradas++; else {
                Log.e(TAG, "❌ tvPuesto es null");
                vistasFaltantes++;
                MyApp.logError(new NullPointerException("tvPuesto es null en HistorialConductorVH"));
            }
            if (tvPrecio != null) vistasEncontradas++; else {
                Log.e(TAG, "❌ tvPrecio es null");
                vistasFaltantes++;
                MyApp.logError(new NullPointerException("tvPrecio es null en HistorialConductorVH"));
            }

            Log.i(TAG, "Vistas inicializadas: " + vistasEncontradas + " OK, " + vistasFaltantes + " FALTANTES");

            // ✅ Registrar evento de inicialización de vistas
            if (vistasFaltantes > 0) {
                registrarErrorVistasAnalitico(vistasFaltantes, vistasEncontradas);
            }

            // Verificar botón de detalles
            View btnDetalles = itemView.findViewById(R.id.btnDetalles);
            if (btnDetalles != null) {
                Log.d(TAG, "Botón detalles encontrado");
            } else {
                Log.e(TAG, "❌ btnDetalles NO encontrado");
                MyApp.logError(new NullPointerException("btnDetalles es null en HistorialConductorVH"));
            }
        }

        public void bind(Reserva reserva, OnReservaClickListener listener) {
            int position = getAdapterPosition();
            Log.d(TAG, "bind iniciado - Posición: " + position +
                    ", Pasajero: " + reserva.getNombre() +
                    ", Ruta: " + reserva.getOrigen() + " → " + reserva.getDestino());

            // ✅ Registrar evento de binding
            registrarBindReservaAnalitico(reserva, position);

            try {
                // Fecha y horario
                String fechaTexto = reserva.getFechaReserva() + " - " + reserva.getHorarioId();
                if (tvFecha != null) {
                    tvFecha.setText(fechaTexto);
                    Log.v(TAG, "Fecha establecida: " + fechaTexto);
                }

                // Estado
                String estado = reserva.getEstadoReserva();
                if (tvEstado != null) {
                    tvEstado.setText(estado);
                    Log.v(TAG, "Estado establecido: " + estado);

                    // Configurar color según estado
                    int colorEstado = getColorEstado(estado);
                    tvEstado.setTextColor(colorEstado);
                    Log.v(TAG, "Color de estado aplicado: " + colorEstado);
                }

                // Datos del pasajero
                if (tvPasajero != null) {
                    tvPasajero.setText(reserva.getNombre());
                    Log.v(TAG, "Nombre pasajero establecido: " + reserva.getNombre());
                }

                if (tvTelefono != null) {
                    tvTelefono.setText(reserva.getTelefono());
                    Log.v(TAG, "Teléfono establecido: " + reserva.getTelefono());
                }

                // Ruta
                String rutaTexto = reserva.getOrigen() + " → " + reserva.getDestino();
                if (tvRuta != null) {
                    tvRuta.setText(rutaTexto);
                    Log.v(TAG, "Ruta establecida: " + rutaTexto);
                }

                // Puesto
                String puestoTexto = "Puesto " + reserva.getPuestoReservado();
                if (tvPuesto != null) {
                    tvPuesto.setText(puestoTexto);
                    Log.v(TAG, "Puesto establecido: " + puestoTexto);
                }

                // Precio
                String precioTexto = "$" + reserva.getPrecio();
                if (tvPrecio != null) {
                    tvPrecio.setText(precioTexto);
                    Log.v(TAG, "Precio establecido: " + precioTexto);
                }

                // Configurar listeners
                configurarListeners(reserva, listener, position);

                Log.i(TAG, "bind completado exitosamente para posición: " + position);

                // ✅ Registrar evento de binding exitoso
                registrarBindExitosoAnalitico(position);

            } catch (Exception e) {
                Log.e(TAG, "❌ Error crítico en bind - Posición: " + position +
                        ", Error: " + e.getMessage(), e);

                // ✅ Usar MyApp para logging de errores
                MyApp.logError(e);

                // ✅ Registrar evento de error en binding
                registrarErrorBindAnalitico(position, e.getMessage());

                establecerValoresPorDefecto();
            }
        }

        private void configurarListeners(Reserva reserva, OnReservaClickListener listener, int position) {
            Log.d(TAG, "Configurando listeners para posición: " + position);

            // Listener para el item completo
            itemView.setOnClickListener(v -> {
                Log.i(TAG, "📋 Item clickeado - Posición: " + position +
                        ", Pasajero: " + reserva.getNombre());

                // ✅ Registrar evento de click en item
                registrarClickItemAnalitico(reserva, position, "item_completo");

                if (listener != null) {
                    listener.onReservaClick(reserva);
                    Log.d(TAG, "Listener onReservaClick ejecutado");
                } else {
                    Log.e(TAG, "❌ Listener es NULO - No se puede ejecutar onReservaClick");
                    MyApp.logError(new NullPointerException("Listener es null en click item histórico"));
                }
            });

            // Listener para el botón de detalles
            View btnDetalles = itemView.findViewById(R.id.btnDetalles);
            if (btnDetalles != null) {
                btnDetalles.setOnClickListener(v -> {
                    Log.i(TAG, "🔍 Botón Detalles clickeado - Posición: " + position +
                            ", Pasajero: " + reserva.getNombre());

                    // ✅ Registrar evento de click en botón detalles
                    registrarClickItemAnalitico(reserva, position, "boton_detalles");

                    if (listener != null) {
                        listener.onVerDetallesClick(reserva);
                        Log.d(TAG, "Listener onVerDetallesClick ejecutado");
                    } else {
                        Log.e(TAG, "❌ Listener es NULO - No se puede ejecutar onVerDetallesClick");
                        MyApp.logError(new NullPointerException("Listener es null en click detalles histórico"));
                    }
                });
                Log.v(TAG, "Listener del botón detalles configurado");
            } else {
                Log.e(TAG, "❌ btnDetalles es null - No se puede configurar listener");
            }

            Log.v(TAG, "Todos los listeners configurados para posición: " + position);
        }

        private int getColorEstado(String estado) {
            try {
                Log.v(TAG, "Obteniendo color para estado: " + estado);
                int color;

                if (estado != null) {
                    switch (estado.toUpperCase()) {
                        case "CONFIRMADA":
                            color = itemView.getContext().getColor(R.color.status_confirmed);
                            Log.v(TAG, "Estado CONFIRMADA - color confirmado");
                            break;
                        case "CANCELADA":
                            color = itemView.getContext().getColor(R.color.status_cancelled);
                            Log.v(TAG, "Estado CANCELADA - color cancelado");
                            break;
                        case "PENDIENTE":
                            color = itemView.getContext().getColor(R.color.status_pending);
                            Log.v(TAG, "Estado PENDIENTE - color pendiente");
                            break;
                        default:
                            color = itemView.getContext().getColor(R.color.text_secondary);
                            Log.w(TAG, "Estado DESCONOCIDO: " + estado + " - color por defecto");
                            break;
                    }
                } else {
                    color = itemView.getContext().getColor(R.color.text_secondary);
                    Log.w(TAG, "Estado es NULL - color por defecto");
                }

                return color;
            } catch (Exception e) {
                Log.e(TAG, "Error obteniendo color de estado: " + e.getMessage());
                MyApp.logError(e);
                return itemView.getContext().getColor(R.color.text_secondary);
            }
        }

        private void establecerValoresPorDefecto() {
            Log.w(TAG, "Estableciendo valores por defecto debido a error");
            if (tvFecha != null) tvFecha.setText("Fecha no disponible");
            if (tvEstado != null) tvEstado.setText("Estado no disponible");
            if (tvPasajero != null) tvPasajero.setText("Pasajero no disponible");
            if (tvTelefono != null) tvTelefono.setText("Teléfono no disponible");
            if (tvRuta != null) tvRuta.setText("Ruta no disponible");
            if (tvPuesto != null) tvPuesto.setText("Puesto no disponible");
            if (tvPrecio != null) tvPrecio.setText("$0");

            // ✅ Registrar evento de valores por defecto
            registrarValoresPorDefectoAnalitico(getAdapterPosition());
        }

        /**
         * ✅ MÉTODO AUXILIAR: Registrar eventos analíticos usando MyApp
         */
        private void registrarEventoViewHolderAnalitico(String evento, Integer posicion) {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("user_id", MyApp.getCurrentUserId());
                params.put("adapter", "HistorialConductorAdapter");
                params.put("viewholder_posicion", posicion != null ? posicion : -1);
                params.put("timestamp", System.currentTimeMillis());

                MyApp.logEvent(evento, params);
                Log.d(TAG, "📊 Evento ViewHolder registrado: " + evento);
            } catch (Exception e) {
                Log.e(TAG, "❌ Error registrando evento ViewHolder: " + e.getMessage());
            }
        }

        /**
         * ✅ MÉTODO AUXILIAR: Registrar binding de reserva
         */
        private void registrarBindReservaAnalitico(Reserva reserva, int posicion) {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("user_id", MyApp.getCurrentUserId());
                params.put("reserva_id", reserva.getIdReserva() != null ? reserva.getIdReserva() : "N/A");
                params.put("reserva_pasajero", reserva.getNombre() != null ? reserva.getNombre() : "N/A");
                params.put("reserva_estado", reserva.getEstadoReserva() != null ? reserva.getEstadoReserva() : "N/A");
                params.put("reserva_posicion", posicion);
                params.put("timestamp", System.currentTimeMillis());

                MyApp.logEvent("viewholder_bind_reserva", params);
                Log.d(TAG, "📊 Bind de reserva registrado en analytics");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error registrando bind de reserva: " + e.getMessage());
            }
        }

        /**
         * ✅ MÉTODO AUXILIAR: Registrar binding exitoso
         */
        private void registrarBindExitosoAnalitico(int posicion) {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("user_id", MyApp.getCurrentUserId());
                params.put("reserva_posicion", posicion);
                params.put("timestamp", System.currentTimeMillis());

                MyApp.logEvent("viewholder_bind_exitoso", params);
                Log.d(TAG, "📊 Bind exitoso registrado en analytics");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error registrando bind exitoso: " + e.getMessage());
            }
        }

        /**
         * ✅ MÉTODO AUXILIAR: Registrar error en binding
         */
        private void registrarErrorBindAnalitico(int posicion, String error) {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("user_id", MyApp.getCurrentUserId());
                params.put("reserva_posicion", posicion);
                params.put("error_mensaje", error != null ? error : "N/A");
                params.put("timestamp", System.currentTimeMillis());

                MyApp.logEvent("viewholder_bind_error", params);
                Log.d(TAG, "📊 Error en bind registrado en analytics");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error registrando error en bind: " + e.getMessage());
            }
        }

        /**
         * ✅ MÉTODO AUXILIAR: Registrar click en item
         */
        private void registrarClickItemAnalitico(Reserva reserva, int posicion, String tipoClick) {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("user_id", MyApp.getCurrentUserId());
                params.put("reserva_id", reserva.getIdReserva() != null ? reserva.getIdReserva() : "N/A");
                params.put("reserva_pasajero", reserva.getNombre() != null ? reserva.getNombre() : "N/A");
                params.put("reserva_posicion", posicion);
                params.put("tipo_click", tipoClick);
                params.put("timestamp", System.currentTimeMillis());

                MyApp.logEvent("historial_conductor_click_item", params);
                Log.d(TAG, "📊 Click en item histórico registrado en analytics");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error registrando click en item: " + e.getMessage());
            }
        }

        /**
         * ✅ MÉTODO AUXILIAR: Registrar error en vistas
         */
        private void registrarErrorVistasAnalitico(int vistasFaltantes, int vistasEncontradas) {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("user_id", MyApp.getCurrentUserId());
                params.put("vistas_faltantes", vistasFaltantes);
                params.put("vistas_encontradas", vistasEncontradas);
                params.put("timestamp", System.currentTimeMillis());

                MyApp.logEvent("viewholder_vistas_error", params);
                Log.d(TAG, "📊 Error en vistas registrado en analytics");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error registrando error en vistas: " + e.getMessage());
            }
        }

        /**
         * ✅ MÉTODO AUXILIAR: Registrar valores por defecto
         */
        private void registrarValoresPorDefectoAnalitico(int posicion) {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("user_id", MyApp.getCurrentUserId());
                params.put("reserva_posicion", posicion);
                params.put("timestamp", System.currentTimeMillis());

                MyApp.logEvent("viewholder_valores_defecto", params);
                Log.d(TAG, "📊 Valores por defecto registrados en analytics");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error registrando valores por defecto: " + e.getMessage());
            }
        }
    }

    /**
     * ✅ MÉTODO AUXILIAR: Registrar eventos analíticos usando MyApp
     */
    private void registrarEventoAnalitico(String evento, Integer count, Integer count2) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("user_id", MyApp.getCurrentUserId());
            params.put("adapter", "HistorialConductorAdapter");

            if (count != null) {
                params.put("count", count);
            }
            if (count2 != null) {
                params.put("count2", count2);
            }

            params.put("timestamp", System.currentTimeMillis());

            MyApp.logEvent(evento, params);
            Log.d(TAG, "📊 Evento analítico registrado: " + evento);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando evento analítico: " + e.getMessage());
        }
    }

    /**
     * ✅ MÉTODO AUXILIAR: Registrar reserva bind usando MyApp
     */
    private void registrarReservaBindAnalitico(Reserva reserva, int posicion) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("user_id", MyApp.getCurrentUserId());
            params.put("reserva_id", reserva.getIdReserva() != null ? reserva.getIdReserva() : "N/A");
            params.put("reserva_pasajero", reserva.getNombre() != null ? reserva.getNombre() : "N/A");
            params.put("reserva_estado", reserva.getEstadoReserva() != null ? reserva.getEstadoReserva() : "N/A");
            params.put("reserva_posicion", posicion);
            params.put("timestamp", System.currentTimeMillis());
            params.put("adapter", "HistorialConductorAdapter");

            MyApp.logEvent("adapter_reserva_bind", params);
            Log.d(TAG, "📊 Reserva bind registrada en analytics");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando reserva bind: " + e.getMessage());
        }
    }

    /**
     * ✅ MÉTODO AUXILIAR: Registrar actualización de lista usando MyApp
     */
    private void registrarActualizacionListaAnalitico(int tamañoAnterior, int tamañoNuevo) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("user_id", MyApp.getCurrentUserId());
            params.put("tamaño_anterior", tamañoAnterior);
            params.put("tamaño_nuevo", tamañoNuevo);
            params.put("diferencia", tamañoNuevo - tamañoAnterior);
            params.put("timestamp", System.currentTimeMillis());
            params.put("adapter", "HistorialConductorAdapter");

            MyApp.logEvent("adapter_lista_actualizada", params);
            Log.d(TAG, "📊 Actualización de lista registrada en analytics");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando actualización de lista: " + e.getMessage());
        }
    }

    /**
     * ✅ MÉTODO AUXILIAR: Registrar diagnóstico usando MyApp
     */
    private void registrarDiagnosticoAnalitico(int totalReservas) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("user_id", MyApp.getCurrentUserId());
            params.put("total_reservas", totalReservas);
            params.put("timestamp", System.currentTimeMillis());
            params.put("adapter", "HistorialConductorAdapter");

            MyApp.logEvent("adapter_diagnostico_historial", params);
            Log.d(TAG, "📊 Diagnóstico de adapter registrado en analytics");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registrando diagnóstico: " + e.getMessage());
        }
    }
}