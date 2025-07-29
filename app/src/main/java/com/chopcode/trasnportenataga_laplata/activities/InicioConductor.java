package com.chopcode.trasnportenataga_laplata.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.adapters.ReservaAdapter;
import com.chopcode.trasnportenataga_laplata.models.Reserva;
import com.chopcode.trasnportenataga_laplata.services.HorarioService;
import com.chopcode.trasnportenataga_laplata.services.ReservaService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class InicioConductor extends AppCompatActivity {
    private RecyclerView rvReservas, rvProximasRutas;
    private TextView tvTotalIngresos, tvReservasActivas, tvProximaRuta, tvConductor, tvPlacaVehiculo;
    private TextView tvEmptyReservas, tvEmptyRutas;
    private MaterialButton btnPerfilConductor, btnCerrarSesion;
    private ReservaService reservaService;
    private HorarioService horarioService;
    private FirebaseAuth auth;
    private ReservaAdapter adapter;
    private List<Reserva> listaReservas = new ArrayList<>();
    private double totalIngresos = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio_conductor);

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance();
        if (!validarLogIn()) return;

        // Inicializar servicios
        reservaService = new ReservaService();
        horarioService = new HorarioService();

        // Configurar vistas
        inicializarVistas();
        configurarRecyclerView();
        configurarBotones();

        // Cargar datos
        cargarDatosConductor();
        cargarReservas();
        cargarProximasRutas();
    }

    private void inicializarVistas() {
        tvConductor = findViewById(R.id.tvConductor);
        tvPlacaVehiculo = findViewById(R.id.tvPlacaVehiculo);
        tvTotalIngresos = findViewById(R.id.tvTotalIngresos);
        tvReservasActivas = findViewById(R.id.tvReservasActivas);
        tvProximaRuta = findViewById(R.id.tvProximaRuta);
        //tvEmptyReservas = findViewById(R.id.tvEmptyReservas);
        //tvEmptyRutas = findViewById(R.id.tvEmptyRutas);
        btnPerfilConductor = findViewById(R.id.btnPerfilConductor);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
    }

    private void configurarRecyclerView() {
        rvReservas = findViewById(R.id.recyclerReservas);
        rvProximasRutas = findViewById(R.id.recyclerProximasRutas);

        // Configurar RecyclerView de reservas
        adapter = new ReservaAdapter(listaReservas, new ReservaAdapter.OnReservaClickListener() {
            @Override
            public void onConfirmarClick(Reserva reserva) {
                mostrarDialogoConfirmacion(reserva, true);
            }

            @Override
            public void onCancelarClick(Reserva reserva) {
                mostrarDialogoConfirmacion(reserva, false);
            }
        });

        rvReservas.setLayoutManager(new LinearLayoutManager(this));
        rvReservas.setAdapter(adapter);

        // Configurar RecyclerView de rutas
        rvProximasRutas.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        // Configurar adapter para rutas aquí...
    }

    private void mostrarDialogoConfirmacion(Reserva reserva, boolean esConfirmacion) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(esConfirmacion ? "Confirmar Reserva" : "Cancelar Reserva")
                .setMessage(esConfirmacion ?
                        "¿Confirmar reserva de " + reserva.getNombre() + "?" :
                        "¿Cancelar reserva de " + reserva.getNombre() + "?")
                .setPositiveButton(esConfirmacion ? "Confirmar" : "Cancelar", (dialog, which) -> {
                    if (esConfirmacion) {
                        confirmarReserva(reserva);
                    } else {
                        cancelarReserva(reserva);
                    }
                })
                .setNegativeButton("Volver", null)
                .show();
    }

    private void cargarDatosConductor() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            DatabaseReference conductorRef = FirebaseDatabase.getInstance()
                    .getReference("conductores")
                    .child(user.getUid());

            conductorRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String nombre = snapshot.child("nombre").getValue(String.class);
                        String placa = snapshot.child("placaVehiculo").getValue(String.class);

                        tvConductor.setText(nombre);
                        tvPlacaVehiculo.setText(getString(R.string.placaVehiculo, placa));

                        // Calcular ingresos si hay datos
                        if (snapshot.hasChild("ingresosDiarios")) {
                            totalIngresos = snapshot.child("ingresosDiarios").getValue(Double.class);
                            tvTotalIngresos.setText(getString(R.string.ingresosDiarios, totalIngresos));
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("InicioConductor", "Error al cargar datos del conductor", error.toException());
                }
            });
        }
    }

    private void cargarReservas() {
        reservaService.cargarReservas(new ReservaService.ReservaCargadaCallback() {
            @Override
            public void onCargaExitosa(List<Reserva> reservas) {
                listaReservas.clear();
                for (Reserva reserva : reservas) {
                    if ("Por confirmar".equals(reserva.getEstadoReserva())) {
                        listaReservas.add(reserva);
                    }
                }

                //adapter.notifyDataSetChanged();
                //tvReservasActivas.setText(getString(R.string.reservas_pendientes, listaReservas.size()));

                // Mostrar mensaje si no hay reservas
                tvEmptyReservas.setVisibility(listaReservas.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String mensaje) {
                Toast.makeText(InicioConductor.this,
                        "Error al cargar reservas: " + mensaje, Toast.LENGTH_SHORT).show();
                Log.e("InicioConductor", mensaje);
                tvEmptyReservas.setVisibility(View.VISIBLE);
            }
        });
    }

    private void cargarProximasRutas() {
        // Implementar carga de rutas programadas
    }

    private void confirmarReserva(Reserva reserva) {
        actualizarEstadoReserva(reserva, "Confirmada");
    }

    private void cancelarReserva(Reserva reserva) {
        actualizarEstadoReserva(reserva, "Cancelada");
    }

    private void actualizarEstadoReserva(Reserva reserva, String nuevoEstado) {
        DatabaseReference reservaRef = FirebaseDatabase.getInstance()
                .getReference("reservas")
                .child(reserva.getIdReserva())
                .child("estadoReserva");

        reservaRef.setValue(nuevoEstado)
                .addOnSuccessListener(aVoid -> {
                    String mensaje = "Reserva " + nuevoEstado.toLowerCase();
                    Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();

                    // Actualizar ingresos si se confirma
                    if ("Confirmada".equals(nuevoEstado)) {
                        actualizarIngresos(reserva.getPrecio());
                    }

                    cargarReservas();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al actualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void actualizarIngresos(double monto) {
        totalIngresos += monto;
        tvTotalIngresos.setText(getString(R.string.ingresosDiarios, totalIngresos));

        // Actualizar en Firebase
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            DatabaseReference conductorRef = FirebaseDatabase.getInstance()
                    .getReference("conductores")
                    .child(user.getUid())
                    .child("ingresosDiarios");

            conductorRef.setValue(totalIngresos);
        }
    }

    private void configurarBotones() {
        btnCerrarSesion.setOnClickListener(view -> cerrarSesion());
        btnPerfilConductor.setOnClickListener(view -> irPerfilConductor());
    }

    private void irPerfilConductor() {
        if (validarLogIn()) {
            startActivity(new Intent(this, PerfilConductor.class));
        }
    }

    private boolean validarLogIn() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, InicioDeSesion.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return false;
        }
        return true;
    }

    private void cerrarSesion() {
        auth.signOut();
        startActivity(new Intent(this, InicioDeSesion.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}