package com.chopcode.trasnportenataga_laplata.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.models.Reserva;
import com.chopcode.trasnportenataga_laplata.services.HorarioService;
import com.chopcode.trasnportenataga_laplata.services.ReservaService;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class InicioConductor extends AppCompatActivity {
    private ArrayList reservas, rutas;
    private RecyclerView rvReservas, rvRutas;
    private TextView tvTotalReservasActivas, tvIngresosDiarios, tvProximaRuta;
    private Button btnPerfilConductor, btnCerrarSesion;
    private ReservaService reservaService;
    private HorarioService horarioService;
    private Reserva reserva;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio_conductor);
        // inicializar los servicios de reservas
        reservaService = new ReservaService();
        horarioService = new HorarioService();
        reserva = new Reserva();
        // Obtener id de la ruta
        String idHorario = reserva.getHorarioId();
        System.out.println(idHorario);
        // referencia a la UI
        rvReservas = findViewById(R.id.recyclerReservas);
        rvRutas = findViewById(R.id.recyclerProximasRutas);

        // TextViews
        tvIngresosDiarios = findViewById(R.id.tvTotalIngresos);
        tvTotalReservasActivas = findViewById(R.id.tvReservasActivas);
        tvProximaRuta = findViewById(R.id.tvProximaRuta);

        // Llamar metodo para obtener el id el horario

        // Llamar metodo para cargar las reservas de la ruta
        reservaService.ReservasActivas(InicioConductor.this, rvReservas, idHorario);

        // referencia a los botones
        btnPerfilConductor = findViewById(R.id.btnPerfilConductor);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

        // Accion del boton de cerrar sesion
        btnCerrarSesion.setOnClickListener(view -> cerrarSesion());
        // Accion del boton de perfil de conductor
        btnPerfilConductor.setOnClickListener(view -> irPerfilConductor());
    }
    /**
     * Metodo para redirecionar al perfil del conductor.
     */
    public void irPerfilConductor(){
        Intent intent = new Intent(InicioConductor.this, PerfilConductor.class);
        startActivity(intent);
    }
    /**
     * 🔹 Cierra la sesión y redirige a la pantalla de inicio.
     */
    private void cerrarSesion() {
        auth.signOut();
        Intent intent = new Intent(this, InicioDeSesion.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}