package com.chopcode.trasnportenataga_laplata.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;
import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.services.ReservaService;

public class Reservas extends AppCompatActivity {

    private Button btnConfirmar;
    private Integer asientoSeleccionado = null;
    private RadioGroup radioGroupRuta;
    private String rutaSeleccionada;
    private ReservaService reservaService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservas);

        // Inicializar el servicio de reservas
        reservaService = new ReservaService();

        // Referencias de UI
        btnConfirmar = findViewById(R.id.buttonConfirmar);
        radioGroupRuta = findViewById(R.id.radioGroupRuta);

        // Evento para seleccionar la ruta
        radioGroupRuta.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbNatagaLaPlata) {
                rutaSeleccionada = "Natagá -> La Plata";
            } else if (checkedId == R.id.rbLaPlataNataga) {
                rutaSeleccionada = "La Plata -> Natagá";
            }
        });

        // Evento para confirmar la reserva
        btnConfirmar.setOnClickListener(v -> {
            if (asientoSeleccionado == null && rutaSeleccionada == null) {
                Toast.makeText(this, "Selecciona una ruta y un asiento", Toast.LENGTH_SHORT).show();
            } else if (asientoSeleccionado == null) {
                Toast.makeText(this, "Selecciona un asiento", Toast.LENGTH_SHORT).show();
            } else if (rutaSeleccionada == null) {
                Toast.makeText(this, "Selecciona una ruta", Toast.LENGTH_SHORT).show();
            } else {
                buscarHorarioYConfirmar();
            }
        });

        configurarSeleccionAsientos();
    }

    // 🔥 Llama a ReservaService para buscar el horario más próximo
    private void buscarHorarioYConfirmar() {
        reservaService.obtenerHorarioMasProximo(rutaSeleccionada, new ReservaService.HorarioCallback() {
            @Override
            public void onHorarioEncontrado(String horarioId, String horarioHora) {
                enviarAConfirmarReserva(horarioId, horarioHora);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(Reservas.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🔥 Enviar datos a la pantalla de Confirmar Reserva
    private void enviarAConfirmarReserva(String horarioId, String horarioHora) {
        Intent confirmarReserva = new Intent(Reservas.this, ConfirmarReserva.class);
        confirmarReserva.putExtra("asientoSeleccionado", asientoSeleccionado);
        confirmarReserva.putExtra("horarioId", horarioId);
        confirmarReserva.putExtra("horarioHora", horarioHora);
        startActivity(confirmarReserva);
    }

    // 🔥 Captura la selección de asientos
    private void configurarSeleccionAsientos() {
        int[] botonesAsientos = {
                R.id.btnAsiento1, R.id.btnAsiento2, R.id.btnAsiento3, R.id.btnAsiento4,
                R.id.btnAsiento5, R.id.btnAsiento6, R.id.btnAsiento7, R.id.btnAsiento8,
                R.id.btnAsiento9, R.id.btnAsiento10, R.id.btnAsiento11, R.id.btnAsiento12,
                R.id.btnAsiento13, R.id.btnAsiento14
        };

        for (int i = 0; i < botonesAsientos.length; i++) {
            Button btnAsiento = findViewById(botonesAsientos[i]);
            btnAsiento.setTag(i + 1);

            btnAsiento.setOnClickListener(v -> {
                int numeroAsiento = (int) btnAsiento.getTag();
                if (asientoSeleccionado != null && asientoSeleccionado == numeroAsiento) {
                    btnAsiento.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                    asientoSeleccionado = null;
                } else {
                    restaurarColoresAsientos();
                    asientoSeleccionado = numeroAsiento;
                    btnAsiento.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                    Toast.makeText(this, "Asiento seleccionado: " + asientoSeleccionado, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // 🔥 Restaurar colores de los asientos
    private void restaurarColoresAsientos() {
        int[] botonesAsientos = {
                R.id.btnAsiento1, R.id.btnAsiento2, R.id.btnAsiento3, R.id.btnAsiento4,
                R.id.btnAsiento5, R.id.btnAsiento6, R.id.btnAsiento7, R.id.btnAsiento8,
                R.id.btnAsiento9, R.id.btnAsiento10, R.id.btnAsiento11, R.id.btnAsiento12,
                R.id.btnAsiento13, R.id.btnAsiento14
        };

        for (int id : botonesAsientos) {
            Button btnAsiento = findViewById(id);
            btnAsiento.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
        }
    }
}