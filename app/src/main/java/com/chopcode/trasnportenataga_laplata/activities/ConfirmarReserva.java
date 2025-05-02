package com.chopcode.trasnportenataga_laplata.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.models.Conductor;
import com.chopcode.trasnportenataga_laplata.models.Pasajero;
import com.chopcode.trasnportenataga_laplata.services.ReservaService;
import com.chopcode.trasnportenataga_laplata.services.UsuarioService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.UUID;

public class ConfirmarReserva extends AppCompatActivity {

    private TextView tvAsiento, tvHora, tvPrecio, tvOrigen, tvDestino, tvTiempo, tvMetodoPago,
    tvEstado, tvTelefonoC, tvTelefonoP, tvPlaca, tvConductor, tvUsuario;
    private Button btnConfirmarReserva, btnCancelar;
    private int asientoSeleccionado;
    private String horarioId, horarioHora, origen, destino, tiempoEstimado, metodoPago,
            estadoReserva, placa, usuarioConductor, telefonoC;

    private double precio;
    private UsuarioService usuarioService;
    private ReservaService reservaService;
    private DatabaseReference databaseReference;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmar_reserva);

        // ✅ Inicializar servicios
        usuarioService = new UsuarioService();
        reservaService = new ReservaService();

        /** Dividir el origen y el destino */
        String rutaSelecionada = getIntent().getStringExtra("rutaSelecionada");
        // Dividir el origen y el destino  usando "->" como delimitador
        String[] partes = rutaSelecionada.split(" -> ");
        // Asignar valores a las variables origen y destino
        if(partes.length == 2){
            origen = partes[0].trim(); //Nataga o la plata
            destino = partes[1].trim();//La plata o nataga
        }else {
            origen = "Desconocido";
            destino = "Desconocido";
        }

        // Recibir datos enviados desde la actividad anterior
        asientoSeleccionado = getIntent().getIntExtra("asientoSeleccionado", -1);
        horarioId = getIntent().getStringExtra("horarioId");
        horarioHora = getIntent().getStringExtra("horarioHora");
        /** Informacion estatica
         * precio
         * tiempoEstimado
         * metodoPago
         */
        precio = 12000;
        tiempoEstimado = "60 min ";
        metodoPago = "Efectivo";
        estadoReserva = "Por confirmar";

        databaseReference = FirebaseDatabase.getInstance().getReference("reservas");

        // Inicializar FirebaseAuth para obtener datos del usuario actual
        auth = FirebaseAuth.getInstance();

        // Referencias a los elementos de la UI
        tvAsiento = findViewById(R.id.tvAsientoReservado);
        tvHora = findViewById(R.id.tvHoraSalida);
        tvPrecio = findViewById(R.id.tvPrecio);
        tvOrigen = findViewById(R.id.tvOrigen);
        tvDestino = findViewById(R.id.tvDestino);
        tvTiempo = findViewById(R.id.tvTiempoEstimado);
        tvMetodoPago = findViewById(R.id.tvMetodoPago);
        tvEstado = findViewById(R.id.chipEstadoReserva);
        tvPlaca = findViewById(R.id.tvPlaca);
        // Datos Conductor
        tvConductor = findViewById(R.id.tvConductor);
        tvTelefonoC = findViewById(R.id.tvTelefonoC);
        // Datos Pasajero
        tvTelefonoP = findViewById(R.id.tvTelefonoP);
        tvUsuario = findViewById(R.id.tvUsuario);
        btnConfirmarReserva = findViewById(R.id.btnConfirmarReserva);
        btnCancelar = findViewById(R.id.btnCancelar);

        // Mostrar datos de la reserva en la interfaz
        tvAsiento.setText("\uD83D\uDCBA Asiento reservado:" + asientoSeleccionado);
        tvHora.setText("\uD83D\uDD52 Hora de salida: " + (horarioHora != null ? horarioHora : "No" +
                " disponible"));
        tvPrecio.setText("\uD83D\uDCB0 Precio: " + precio);
        tvOrigen.setText("\uD83D\uDCCD Origen: " + origen);
        tvDestino.setText("\uD83C\uDFC1 Destino: " + destino);
        tvTiempo.setText("⏳ Tiempo Estimado: " + tiempoEstimado);
        tvMetodoPago.setText("\uD83D\uDCB3 Método de Pago: " + metodoPago);
        tvEstado.setText("\uD83D\uDCCC Estado de la Reserva: " + estadoReserva);

        // 🔹 Cargar información del usuario
        cargarInfoUsuario();
        // 🔹 Cargar información del conductor
        cargarInfoConductor();

        // Confirmar la reserva y guardar en firebase
        btnConfirmarReserva.setOnClickListener(v -> registrarReserva());
        // Cancelar reserva y volver a la pantalla anterior
        btnCancelar.setOnClickListener(v -> finish());
    }

    /**
     * Metodo para obtener la informacion del usuario desde un callBack*/
    private  void cargarInfoUsuario(){
        usuarioService.cargarInformacionPasajero(new UsuarioService.UsuarioCallback() {
            @Override
            public void onUsuarioCargado(Pasajero pasajero) {
                tvUsuario.setText("👤 Nombre Pasajero: " + pasajero.getNombre());
                tvTelefonoP.setText("\uD83D\uDCDE Telefono Pasajero: " + pasajero.getTelefono());
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ConfirmarReserva.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    /**
     *  Metodo para obtener la informacion del conductor desde un callBack
     */
    private void cargarInfoConductor(){
        usuarioService.cargarInformacionConductor(new UsuarioService.ConductorCallback() {
            @Override
            public void onConductorCargado(Conductor conductor) {
                tvConductor.setText("\uD83D\uDC68\u200D✈️ Nombre Conductor: " + conductor.getNombre());
                tvPlaca.setText("\uD83D\uDE98 Placa del Vehículo: " + conductor.getPlacaVehiculo());
                tvTelefonoC.setText("\uD83D\uDCDE Teléfono Conductor: " + conductor.getTelefono());

               usuarioConductor = conductor.getNombre();
               placa = conductor.getPlacaVehiculo();
               telefonoC = conductor.getTelefono();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getApplicationContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    /**
     * Metodo para registrar la reserva en firebase
     */
    public void registrarReserva(){
        // Confirmar reserva y actualizar Firebase
        reservaService.actualizarDisponibilidadAsientos(
                this, horarioId, asientoSeleccionado, origen, destino, tiempoEstimado,
                metodoPago, estadoReserva, placa, precio, usuarioConductor, telefonoC,
                new ReservaService.ReservaCallback() {
                    @Override
                    public void onReservaExitosa() {
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