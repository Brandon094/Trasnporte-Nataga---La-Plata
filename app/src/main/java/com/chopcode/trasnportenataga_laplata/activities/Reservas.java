package com.chopcode.trasnportenataga_laplata.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;
import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.services.HorarioService;
import com.chopcode.trasnportenataga_laplata.services.ReservaService;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Actividad para la gestión de reservas de asientos en un transporte.
 * Permite seleccionar una ruta, visualizar los asientos disponibles y confirmar la reserva.
 */
public class Reservas extends AppCompatActivity {
    /** Iconos de los asientos */
    private static final int VECTOR_ASIENTO_DISPONIBLE = R.drawable.asiento_disponible;
    private static final int VECTOR_ASIENTO_SELECCIONADO = R.drawable.asiento_seleccionado;
    private static final int VECTOR_ASIENTO_OCUPADO = R.drawable.asiento_ocupado;
    private Button btnConfirmar;
    private Integer asientoSeleccionado = null;
    private RadioGroup radioGroupRuta;
    private String rutaSeleccionada, horarioId, horarioHora;
    private ReservaService reservaService;
    private HorarioService horarioService;
    private Map<Integer, MaterialButton> mapaAsientos = new HashMap<>();
    /**
     * Método que se ejecuta al crear la actividad. Inicializa la UI y carga datos previos.
     * @param savedInstanceState Estado guardado de la actividad en caso de recreación.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservas);
        // Inicializar servicios
        reservaService = new ReservaService();
        horarioService = new HorarioService();
        // Referencias a la UI
        btnConfirmar = findViewById(R.id.buttonConfirmar);
        radioGroupRuta = findViewById(R.id.radioGroupRuta);

        if (savedInstanceState != null) {
            asientoSeleccionado = savedInstanceState.getInt("asientoSeleccionado", -1);
            if (asientoSeleccionado == -1) asientoSeleccionado = null;
            rutaSeleccionada = savedInstanceState.getString("rutaSeleccionada");
        }
        // Selecion de ruta
        radioGroupRuta.setOnCheckedChangeListener((group, checkedId) -> {
            rutaSeleccionada = (checkedId == R.id.rbNatagaLaPlata) ? "Natagá -> La Plata" : "La Plata -> Natagá";
            buscarHorarioConfigurar();
        });

        if (rutaSeleccionada != null) {
            buscarHorarioConfigurar();
        }
        // Accion del boton de confirmacion
        btnConfirmar.setOnClickListener(v -> validacionesReserva(horarioId, horarioHora));
    }
    /**
     * Llamado al metodo obtenerHorarioMasProximo
     */
    private void buscarHorarioConfigurar() {
        horarioService.obtenerHorarioMasProximo(rutaSeleccionada,
                new HorarioService.HorarioEncontradoCallback() {
                    @Override
                    public void onHorarioEncontrado(String id, String hora) {
                        horarioId = id;
                        horarioHora = hora;
                        configurarSeleccionAsientos();
                        cargarAsientosDesdeFirebase(horarioId);
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(Reservas.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
    }
    /**
     * Carga la disponibilidad de los asientos desde Firebase y actualiza la UI.
     */
    private void cargarAsientosDesdeFirebase(String horarioId) {
        if (rutaSeleccionada == null) return;

        reservaService.obtenerAsientosOcupados(horarioId, new ReservaService.AsientosCallback() {
            @Override
            public void onAsientosObtenidos(int[] asientosOcupados) {
                Set<Integer> ocupados = new HashSet<>();
                for (int asiento : asientosOcupados) {
                    ocupados.add(asiento);
                }

                for (Map.Entry<Integer, MaterialButton> entry : mapaAsientos.entrySet()) {
                    int numAsiento = entry.getKey();
                    MaterialButton btn = entry.getValue();

                    if (ocupados.contains(numAsiento)) {
                        btn.setIcon(ContextCompat.getDrawable(Reservas.this, VECTOR_ASIENTO_OCUPADO));
                        btn.setEnabled(false);
                    } else {
                        btn.setIcon(ContextCompat.getDrawable(Reservas.this, VECTOR_ASIENTO_DISPONIBLE));
                        btn.setEnabled(true);

                        btn.setOnClickListener(v -> {
                            if (asientoSeleccionado != null && mapaAsientos.containsKey(asientoSeleccionado)) {
                                mapaAsientos.get(asientoSeleccionado).setIcon(ContextCompat.getDrawable(Reservas.this, VECTOR_ASIENTO_DISPONIBLE));
                            }

                            asientoSeleccionado = numAsiento;
                            btn.setIcon(ContextCompat.getDrawable(Reservas.this, VECTOR_ASIENTO_SELECCIONADO));
                            Toast.makeText(Reservas.this, "Asiento seleccionado: " + asientoSeleccionado, Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(Reservas.this, "Error al obtener disponibilidad: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    /**
     * Configura la selección de asientos y gestiona la lógica de clic en cada uno.
     */
    private void configurarSeleccionAsientos() {
        int[] botonesAsientos = {
                R.id.btnAsiento1, R.id.btnAsiento2, R.id.btnAsiento3, R.id.btnAsiento4,
                R.id.btnAsiento5, R.id.btnAsiento6, R.id.btnAsiento7, R.id.btnAsiento8,
                R.id.btnAsiento9, R.id.btnAsiento10, R.id.btnAsiento11, R.id.btnAsiento12,
                R.id.btnAsiento13, R.id.btnAsiento14
        };

        for (int i = 0; i < botonesAsientos.length; i++) {
            MaterialButton btnAsiento = findViewById(botonesAsientos[i]);
            int numeroAsiento = i + 1;
            btnAsiento.setTag(numeroAsiento);
            btnAsiento.setVisibility(View.VISIBLE);
            mapaAsientos.put(numeroAsiento, btnAsiento);
        }
    }
    /**
     * Valida que el usuario haya seleccionado una ruta y un asiento antes de continuar.
     */
    private void validacionesReserva(String horarioId, String horarioHora) {
        if (rutaSeleccionada == null) {
            Toast.makeText(this, "Selecciona una ruta", Toast.LENGTH_SHORT).show();
            return;
        }
        if (asientoSeleccionado == null) {
            Toast.makeText(this, "Selecciona un asiento", Toast.LENGTH_SHORT).show();
            return;
        }
        enviarConfirmarReserva(horarioId,horarioHora);
    }
    /**
     * Enviar la informacion a la interfaz de confirmarReserva
     */
    private void enviarConfirmarReserva(String horarioId, String horarioHora) {
        Intent confirmarReserva = new Intent(Reservas.this, ConfirmarReserva.class);
        confirmarReserva.putExtra("asientoSeleccionado", asientoSeleccionado);
        confirmarReserva.putExtra("rutaSelecionada", rutaSeleccionada);
        confirmarReserva.putExtra("horarioId", horarioId);
        confirmarReserva.putExtra("horarioHora", horarioHora);
        startActivity(confirmarReserva);
    }
}
