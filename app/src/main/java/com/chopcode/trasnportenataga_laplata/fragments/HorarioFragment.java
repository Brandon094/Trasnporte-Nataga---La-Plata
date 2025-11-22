package com.chopcode.trasnportenataga_laplata.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.activities.passenger.CrearReservas;
import com.chopcode.trasnportenataga_laplata.adapters.horarios.HorarioAdapter;
import com.chopcode.trasnportenataga_laplata.managers.AuthManager;
import com.chopcode.trasnportenataga_laplata.models.Horario;
import com.chopcode.trasnportenataga_laplata.models.Usuario;

import java.util.ArrayList;
import java.util.List;

public class HorarioFragment extends Fragment implements HorarioAdapter.OnReservarClickListener {

    // ✅ NUEVO: Tag para logs
    private static final String TAG = "HorarioFragment";

    private static final String ARG_HORARIOS = "horarios";
    private static final String ARG_TITULO = "titulo";

    private RecyclerView recyclerView;
    private HorarioAdapter adapter;
    private List<Horario> horarios = new ArrayList<>();
    private String titulo;
    private AuthManager authManager;

    // AGREGAR ESTA INTERFAZ
    public interface OnUsuarioDataListener {
        Usuario getUsuarioActual();
    }

    private OnUsuarioDataListener usuarioDataListener;

    // AGREGAR ESTE MÉTODO
    public void setUsuarioDataListener(OnUsuarioDataListener listener) {
        Log.d(TAG, "🔧 Configurando usuarioDataListener");
        this.usuarioDataListener = listener;
        Log.d(TAG, "✅ usuarioDataListener configurado correctamente");
    }

    public static HorarioFragment newInstance(List<Horario> horarios, String titulo) {
        Log.d(TAG, "🏗️ Creando nueva instancia de HorarioFragment");
        Log.d(TAG, "   - Título: " + titulo);
        Log.d(TAG, "   - Cantidad horarios: " + (horarios != null ? horarios.size() : 0));

        HorarioFragment fragment = new HorarioFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_HORARIOS, new ArrayList<>(horarios));
        args.putString(ARG_TITULO, titulo);
        fragment.setArguments(args);

        Log.d(TAG, "✅ Instancia de HorarioFragment creada exitosamente");
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "🚀 onCreate - Iniciando fragment de horarios");

        authManager = AuthManager.getInstance();
        Log.d(TAG, "✅ AuthManager inicializado");

        // Cargar datos iniciales desde los argumentos
        if (getArguments() != null) {
            List<Horario> horariosArgs = (List<Horario>) getArguments().getSerializable(ARG_HORARIOS);
            if (horariosArgs != null) {
                horarios.clear();
                horarios.addAll(horariosArgs);
                Log.d(TAG, "📋 Horarios cargados desde arguments: " + horarios.size() + " elementos");

                // Log detallado de horarios cargados
                for (int i = 0; i < Math.min(horarios.size(), 3); i++) {
                    Horario h = horarios.get(i);
                    Log.d(TAG, "   - Horario " + (i+1) + ": " + h.getHora() + " | ID: " + h.getId());
                }
                if (horarios.size() > 3) {
                    Log.d(TAG, "   - ... y " + (horarios.size() - 3) + " horarios más");
                }
            } else {
                Log.w(TAG, "⚠️ horariosArgs es null - lista vacía");
            }

            titulo = getArguments().getString(ARG_TITULO);
            Log.d(TAG, "🏷️ Título del fragment: " + titulo);
        } else {
            Log.w(TAG, "⚠️ No hay arguments en el fragment");
        }

        Log.d(TAG, "✅ Fragment creado correctamente");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "🎨 onCreateView - Creando vista del fragment");

        View view = inflater.inflate(R.layout.fragment_horarios, container, false);
        Log.d(TAG, "✅ Layout inflado: fragment_horarios");

        recyclerView = view.findViewById(R.id.recyclerViewHorarios);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        Log.d(TAG, "✅ RecyclerView configurado con LinearLayoutManager");

        // Pasar this como listener para los clics de reserva
        adapter = new HorarioAdapter(horarios, this);
        recyclerView.setAdapter(adapter);
        Log.d(TAG, "✅ Adapter configurado con " + horarios.size() + " horarios");

        Log.d(TAG, "🎯 Fragment completamente inicializado para: " + titulo);
        Log.d(TAG, "   - Horarios mostrados: " + horarios.size());
        Log.d(TAG, "   - Listener de reserva: " + (this != null ? "CONFIGURADO" : "NULL"));

        return view;
    }

    public void actualizarHorarios(List<Horario> nuevosHorarios) {
        Log.d(TAG, "🔄 Actualizando horarios en el fragment");
        Log.d(TAG, "   - Horarios actuales: " + horarios.size());
        Log.d(TAG, "   - Nuevos horarios: " + (nuevosHorarios != null ? nuevosHorarios.size() : "null"));

        if (adapter != null) {
            horarios.clear();
            if (nuevosHorarios != null) {
                horarios.addAll(nuevosHorarios);
            }
            adapter.actualizarHorarios(horarios);
            Log.d(TAG, "✅ Adapter actualizado - nuevos horarios: " + horarios.size());

            // Log de los primeros horarios actualizados
            if (horarios.size() > 0) {
                Log.d(TAG, "📋 Primeros horarios actualizados:");
                for (int i = 0; i < Math.min(horarios.size(), 2); i++) {
                    Horario h = horarios.get(i);
                    Log.d(TAG, "   - " + h.getHora() + " | " + h.getRuta());
                }
            }
        } else {
            // Si el adapter aún no está creado, guardar los datos para cuando se cree
            Log.d(TAG, "⏳ Adapter no está listo - guardando datos para inicialización posterior");
            horarios.clear();
            if (nuevosHorarios != null) {
                horarios.addAll(nuevosHorarios);
            }
            Log.d(TAG, "✅ Datos guardados para inicialización: " + horarios.size() + " horarios");
        }
    }

    @Override
    public void onReservarClick(Horario horario) {
        Log.d(TAG, "🎯 Click en botón Reservar para horario:");
        Log.d(TAG, "   - ID: " + horario.getId());
        Log.d(TAG, "   - Hora: " + horario.getHora());
        Log.d(TAG, "   - Ruta: " + horario.getRuta());
        Log.d(TAG, "   - Título fragment: " + titulo);

        if (!authManager.isUserLoggedIn()) {
            Log.w(TAG, "⚠️ Usuario no autenticado - redirigiendo a login");
            Toast.makeText(getContext(), "Debes iniciar sesión para reservar", Toast.LENGTH_SHORT).show();
            authManager.redirectToLogin(getActivity());
            return;
        }

        Log.d(TAG, "✅ Usuario autenticado - procediendo con reserva");
        navegarACrearReservas(horario);
    }

    private void navegarACrearReservas(Horario horario) {
        Log.d(TAG, "🔄 Navegando a actividad CrearReservas");

        try {
            Intent intent = new Intent(getActivity(), CrearReservas.class);
            intent.putExtra("horarioId", horario.getId());
            intent.putExtra("horarioHora", horario.getHora());
            intent.putExtra("rutaSeleccionada", titulo); // Usamos el título como ruta

            Log.d(TAG, "📦 Datos pasados a CrearReservas:");
            Log.d(TAG, "   - horarioId: " + horario.getId());
            Log.d(TAG, "   - horarioHora: " + horario.getHora());
            Log.d(TAG, "   - rutaSeleccionada: " + titulo);

            // AGREGAR ESTO: Obtener y pasar datos del usuario
            if (usuarioDataListener != null) {
                Log.d(TAG, "👤 Obteniendo datos del usuario desde listener...");
                Usuario usuario = usuarioDataListener.getUsuarioActual();
                if (usuario != null) {
                    intent.putExtra("usuarioId", usuario.getId());
                    intent.putExtra("usuarioNombre", usuario.getNombre());
                    intent.putExtra("usuarioTelefono", usuario.getTelefono());
                    intent.putExtra("usuarioEmail", usuario.getEmail());

                    Log.d(TAG, "✅ Datos de usuario pasados a reserva:");
                    Log.d(TAG, "   - usuarioId: " + usuario.getId());
                    Log.d(TAG, "   - usuarioNombre: " + usuario.getNombre());
                    Log.d(TAG, "   - usuarioTelefono: " + usuario.getTelefono());
                    Log.d(TAG, "   - usuarioEmail: " + usuario.getEmail());
                } else {
                    Log.w(TAG, "⚠️ Usuario es null en el listener - datos no pasados");
                }
            } else {
                Log.w(TAG, "⚠️ No hay usuarioDataListener configurado - datos de usuario no disponibles");
            }

            startActivity(intent);
            Log.d(TAG, "🎯 Actividad CrearReservas iniciada exitosamente");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error al navegar a CrearReservas: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error al abrir la reserva", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "📱 onResume - Fragment visible");
        Log.d(TAG, "   - Horarios actuales: " + horarios.size());
        Log.d(TAG, "   - Título: " + titulo);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "📱 onPause - Fragment en segundo plano");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "📱 onDestroyView - Vista del fragment destruida");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "📱 onDestroy - Fragment destruido");
    }

    // ✅ NUEVO MÉTODO: Obtener información del fragment para debugging
    public void logFragmentInfo() {
        Log.d(TAG, "📊 INFORMACIÓN DEL FRAGMENT:");
        Log.d(TAG, "   - Título: " + titulo);
        Log.d(TAG, "   - Horarios: " + horarios.size());
        Log.d(TAG, "   - Adapter: " + (adapter != null ? "INICIALIZADO" : "NULL"));
        Log.d(TAG, "   - RecyclerView: " + (recyclerView != null ? "INICIALIZADO" : "NULL"));
        Log.d(TAG, "   - UsuarioDataListener: " + (usuarioDataListener != null ? "CONFIGURADO" : "NULL"));

        if (horarios.size() > 0) {
            Log.d(TAG, "   - Primer horario: " + horarios.get(0).getHora() + " | " + horarios.get(0).getRuta());
        }
    }
}