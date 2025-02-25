package com.chopcode.trasnportenataga_laplata.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.services.ReservaService;

public class ConfirmarReserva extends AppCompatActivity {

    private TextView tvAsiento, tvHora, tvPrecio, tvOrigen, tvDestino, tvTiempo, tvMetodoPago, tvEstado, tvPlaca, tvConductor;
    private Button btnConfirmarReserva, btnCancelar;
    private int asientoSeleccionado;
    private String horarioId;
    private ReservaService reservaService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmar_reserva);

        // Recibir datos
        asientoSeleccionado = getIntent().getIntExtra("asientoSeleccionado", -1);
        horarioId = getIntent().getStringExtra("horarioId");

        // Inicializar servicio
        reservaService = new ReservaService();

        // Referencias UI
        tvAsiento = findViewById(R.id.tvAsientoReservado);
        tvHora = findViewById(R.id.tvHoraSalida);
        btnConfirmarReserva = findViewById(R.id.btnConfirmarReserva);
        btnCancelar = findViewById(R.id.btnCancelar);

        // Mostrar el asiento seleccionado
        tvAsiento.setText("Asiento: " + asientoSeleccionado);

        // Confirmar reserva
        btnConfirmarReserva.setOnClickListener(v -> guardarReserva());

        // Cancelar reserva
        btnCancelar.setOnClickListener(v -> finish());
    }

    /**
     * 🔥 Llama al servicio para guardar la reserva
     */
    private void guardarReserva() {
        reservaService.guardarReserva(horarioId, asientoSeleccionado, new ReservaService.ReservaCallback() {
            @Override
            public void onReservaExitosa() {
                Toast.makeText(ConfirmarReserva.this, "Reserva confirmada", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(ConfirmarReserva.this, InicioUsuarios.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ConfirmarReserva.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}