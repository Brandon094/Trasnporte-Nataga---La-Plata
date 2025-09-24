package com.chopcode.trasnportenataga_laplata.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.managers.AuthManager;
import com.chopcode.trasnportenataga_laplata.models.Usuario;
import com.chopcode.trasnportenataga_laplata.services.ReservaService;
import com.chopcode.trasnportenataga_laplata.services.UserService;
import com.google.firebase.database.*;

import java.util.List;

public class ConfirmarReserva extends AppCompatActivity {

    private TextView tvAsiento, tvHora, tvPrecio, tvOrigen, tvDestino, tvTiempo, tvMetodoPago,
            tvEstado, tvTelefonoC, tvTelefonoP, tvPlaca, tvConductor, tvUsuario;
    private Button btnConfirmarReserva, btnCancelar;
    private int asientoSeleccionado;
    private String horarioId, horarioHora, origen, destino, tiempoEstimado, metodoPago,
            estadoReserva, placa, usuarioConductor, telefonoC;

    private double precio;
    private UserService userService;
    private ReservaService reservaService;
    private DatabaseReference databaseReference;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmar_reserva);

        // ✅ Inicializar servicios
        userService = new UserService();
        reservaService = new ReservaService();
        authManager = AuthManager.getInstance();

        /** Dividir el origen y el destino */
        String rutaSelecionada = getIntent().getStringExtra("rutaSelecionada");
        // Dividir el origen y el destino usando "->" como delimitador
        String[] partes = rutaSelecionada.split(" -> ");
        // Asignar valores a las variables origen y destino
        if(partes.length == 2){
            origen = partes[0].trim(); // Nataga o la plata
            destino = partes[1].trim(); // La plata o nataga
        } else {
            origen = "Desconocido";
            destino = "Desconocido";
        }

        // Recibir datos enviados desde la actividad anterior
        asientoSeleccionado = getIntent().getIntExtra("asientoSeleccionado", -1);
        horarioId = getIntent().getStringExtra("horarioId");
        horarioHora = getIntent().getStringExtra("horarioHora");

        /** Informacion estatica */
        precio = 12000;
        tiempoEstimado = "60 min ";
        metodoPago = "Efectivo";
        estadoReserva = "Por confirmar";

        databaseReference = FirebaseDatabase.getInstance().getReference("reservas");

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
        tvHora.setText("\uD83D\uDD52 Hora de salida: " + (horarioHora != null ? horarioHora : "No disponible"));
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
     * Método para obtener la información del usuario usando loadUserData
     */
    private void cargarInfoUsuario(){
        String userId = authManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        userService.loadUserData(userId, new UserService.UserDataCallback() {
            @Override
            public void onUserDataLoaded(Usuario usuario) {
                runOnUiThread(() -> {
                    tvUsuario.setText("👤 Nombre Pasajero: " +
                            (usuario.getNombre() != null ? usuario.getNombre() : "Usuario"));
                    tvTelefonoP.setText("\uD83D\uDCDE Telefono Pasajero: " +
                            (usuario.getTelefono() != null ? usuario.getTelefono() : "No disponible"));
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(ConfirmarReserva.this, "Error cargando usuario: " + error, Toast.LENGTH_SHORT).show();
                    // Datos por defecto
                    tvUsuario.setText("👤 Nombre Pasajero: Usuario");
                    tvTelefonoP.setText("\uD83D\uDCDE Telefono Pasajero: No disponible");
                });
            }
        });
    }

    /**
     * Método para obtener la información del conductor
     */
    private void cargarInfoConductor(){
        // Buscar un conductor disponible para esta ruta/horario
        buscarConductorDisponible();
    }

    /**
     * Método para buscar un conductor disponible para la ruta
     */
    private void buscarConductorDisponible() {
        // Primero, obtener todos los conductores y encontrar uno disponible
        DatabaseReference conductoresRef = FirebaseDatabase.getInstance().getReference("conductores");

        conductoresRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot conductorSnapshot : snapshot.getChildren()) {
                    // Verificar si este conductor tiene el horario asignado
                    if (conductorSnapshot.hasChild("horariosAsignados")) {
                        for (DataSnapshot horarioSnapshot : conductorSnapshot.child("horariosAsignados").getChildren()) {
                            String horarioAsignado = horarioSnapshot.getValue(String.class);
                            if (horarioId.equals(horarioAsignado)) {
                                // Este conductor está asignado a este horario
                                String conductorId = conductorSnapshot.getKey();
                                cargarDatosConductorEspecifico(conductorId);
                                return;
                            }
                        }
                    }
                }

                // Si no se encontró conductor específico, usar el primero disponible
                if (snapshot.exists()) {
                    DataSnapshot primerConductor = snapshot.getChildren().iterator().next();
                    String conductorId = primerConductor.getKey();
                    cargarDatosConductorEspecifico(conductorId);
                } else {
                    // Usar datos por defecto si no hay conductores
                    usarConductorPorDefecto();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ConfirmarReserva.this, "Error buscando conductor: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                usarConductorPorDefecto();
            }
        });
    }

    /**
     * Cargar datos de un conductor específico
     */
    private void cargarDatosConductorEspecifico(String conductorId) {
        userService.loadDriverData(conductorId, new UserService.DriverDataCallback() {
            @Override
            public void onDriverDataLoaded(String nombre, String telefono, String placaVehiculo, List<String> horariosAsignados) {
                runOnUiThread(() -> {
                    tvConductor.setText("\uD83D\uDC68\u200D✈️ Nombre Conductor: " +
                            (nombre != null ? nombre : "Conductor Asignado"));
                    tvPlaca.setText("\uD83D\uDE98 Placa del Vehículo: " +
                            (placaVehiculo != null ? placaVehiculo : "ABC123"));
                    tvTelefonoC.setText("\uD83D\uDCDE Teléfono Conductor: " +
                            (telefono != null ? telefono : "3001234567")); // ✅ USAR TELÉFONO REAL

                    usuarioConductor = nombre != null ? nombre : "Conductor Asignado";
                    placa = placaVehiculo != null ? placaVehiculo : "ABC123";
                    telefonoC = telefono != null ? telefono : "3001234567"; // ✅ USAR TELÉFONO REAL
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(ConfirmarReserva.this, "Error cargando conductor: " + error, Toast.LENGTH_SHORT).show();
                    usarConductorPorDefecto();
                });
            }
        });
    }

    /**
     * Usar datos de conductor por defecto
     */
    private void usarConductorPorDefecto() {
        runOnUiThread(() -> {
            tvConductor.setText("\uD83D\uDC68\u200D✈️ Nombre Conductor: Conductor Asignado");
            tvPlaca.setText("\uD83D\uDE98 Placa del Vehículo: ABC123");
            tvTelefonoC.setText("\uD83D\uDCDE Teléfono Conductor: 3001234567");

            usuarioConductor = "Conductor Asignado";
            placa = "ABC123";
            telefonoC = "3001234567";
        });
    }

    /**
     * Método para registrar la reserva en firebase
     */
    public void registrarReserva(){
        String userId = authManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

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