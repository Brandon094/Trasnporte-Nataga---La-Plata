package com.chopcode.trasnportenataga_laplata.activities.driver;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.models.Conductor;
import com.chopcode.trasnportenataga_laplata.models.Vehiculo;
import com.chopcode.trasnportenataga_laplata.services.user.UserService;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class EditarPerfilConductor extends AppCompatActivity {

    // Servicios
    private UserService userService;
    private DatabaseReference vehiculoRef;

    // Views
    private TextInputEditText etCorreo, etNombre, etTelefono, etPlaca, etModelo, etColor, etCapacidad, etAnio;
    private TextView tvCorreoActual, tvNombreActual, tvTelefonoActual, tvPlacaActual, tvModeloActual, tvColorActual, tvCapacidadActual, tvAnioActual;
    private Button btnCancelar, btnGuardarCambios;

    // Datos
    private String userId;
    private Conductor conductorActual;
    private Vehiculo vehiculoActual;
    private String vehiculoId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);

        // Inicializar servicios
        userService = new UserService();
        vehiculoRef = FirebaseDatabase.getInstance().getReference("vehiculos");

        // Obtener usuario actual
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = currentUser.getUid();

        // Inicializar vistas
        initViews();

        // Cargar datos del conductor y vehículo
        cargarDatosConductor();

        // Configurar listeners
        configurarListeners();
    }

    private void initViews() {
        // Textos de valores actuales
        tvCorreoActual = findViewById(R.id.tvCorreoActual);
        tvNombreActual = findViewById(R.id.tvNombreActual);
        tvTelefonoActual = findViewById(R.id.tvTelefonoActual);
        tvPlacaActual = findViewById(R.id.tvPlacaActual);
        tvModeloActual = findViewById(R.id.tvModeloActual);
        tvColorActual = findViewById(R.id.tvColorActual);
        tvCapacidadActual = findViewById(R.id.tvCapacidadActual);
        tvAnioActual = findViewById(R.id.tvAnioActual);

        // Campos de entrada
        etCorreo = findViewById(R.id.etCorreo);
        etNombre = findViewById(R.id.etNombre);
        etTelefono = findViewById(R.id.etTelefono);
        etPlaca = findViewById(R.id.etPlaca);
        etModelo = findViewById(R.id.etModelo);
        etColor = findViewById(R.id.etColor);
        etCapacidad = findViewById(R.id.etCapacidad);
        etAnio = findViewById(R.id.etAnio);

        // Botones
        btnCancelar = findViewById(R.id.btnCancelar);
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios);
    }

    private void cargarDatosConductor() {
        // Verificar si es conductor
        userService.checkIfUserIsDriver(userId, new UserService.DriverCheckCallback() {
            @Override
            public void onDriverCheckComplete(boolean isDriver) {
                if (!isDriver) {
                    Toast.makeText(EditarPerfilConductor.this,
                            "El usuario no está registrado como conductor", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                // Cargar datos del conductor
                cargarDatosDriver();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(EditarPerfilConductor.this,
                        "Error verificando conductor: " + error, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void cargarDatosDriver() {
        // Cargar datos del conductor desde la base de datos
        DatabaseReference conductorRef = FirebaseDatabase.getInstance()
                .getReference("conductores")
                .child(userId);

        conductorRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    conductorActual = snapshot.getValue(Conductor.class);
                    if (conductorActual != null) {
                        conductorActual.setId(userId);

                        // Obtener ID del vehículo
                        vehiculoId = conductorActual.getVehiculoId();

                        // Actualizar datos del conductor en la UI
                        actualizarUIDatosConductor();

                        // Cargar datos del vehículo si existe
                        if (vehiculoId != null && !vehiculoId.isEmpty()) {
                            cargarDatosVehiculo();
                        } else {
                            // Si no tiene vehículo, mostrar campos vacíos
                            inicializarCamposVehiculoVacios();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(EditarPerfilConductor.this,
                        "Error cargando datos del conductor: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void cargarDatosVehiculo() {
        vehiculoRef.child(vehiculoId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    vehiculoActual = snapshot.getValue(Vehiculo.class);
                    if (vehiculoActual != null) {
                        vehiculoActual.setId(vehiculoId);
                        actualizarUIDatosVehiculo();
                    }
                } else {
                    inicializarCamposVehiculoVacios();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(EditarPerfilConductor.this,
                        "Error cargando datos del vehículo: " + error.getMessage(), Toast.LENGTH_LONG).show();
                inicializarCamposVehiculoVacios();
            }
        });
    }

    private void actualizarUIDatosConductor() {
        // Actualizar textos de valores actuales
        tvNombreActual.setText("Nombre actual: " +
                (conductorActual.getNombre() != null ? conductorActual.getNombre() : "No definido"));
        tvTelefonoActual.setText("Teléfono actual: " +
                (conductorActual.getTelefono() != null ? conductorActual.getTelefono() : "No definido"));
        tvCorreoActual.setText("Correo actual: " +
                (conductorActual.getEmail() != null ? conductorActual.getEmail() : "No definido"));

        // Llenar campos editables
        if (conductorActual.getNombre() != null) etNombre.setText("");
        if (conductorActual.getTelefono() != null) etTelefono.setText("");
        if (conductorActual.getEmail() != null) etCorreo.setText(conductorActual.getEmail());
    }

    private void actualizarUIDatosVehiculo() {
        if (vehiculoActual != null) {
            tvPlacaActual.setText("Placa actual: " +
                    (vehiculoActual.getPlaca() != null ? vehiculoActual.getPlaca() : "No definida"));
            tvModeloActual.setText("Modelo actual: " +
                    (vehiculoActual.getModelo() != null ? vehiculoActual.getModelo() : "No definido"));
            tvColorActual.setText("Color actual: " +
                    (vehiculoActual.getColor() != null ? vehiculoActual.getColor() : "No definido"));
            tvCapacidadActual.setText("Capacidad actual: " + vehiculoActual.getCapacidad());
            tvAnioActual.setText("Año actual: " +
                    (vehiculoActual.getAno() != null ? vehiculoActual.getAno() : "No definido"));

            // Llenar campos editables
            if (vehiculoActual.getPlaca() != null) etPlaca.setText("");
            if (vehiculoActual.getModelo() != null) etModelo.setText("");
            if (vehiculoActual.getColor() != null) etColor.setText("");
            etCapacidad.setText("");
            if (vehiculoActual.getAno() != null) etAnio.setText("");
        }
    }

    private void inicializarCamposVehiculoVacios() {
        tvPlacaActual.setText("Placa actual: No definida");
        tvModeloActual.setText("Modelo actual: No definido");
        tvColorActual.setText("Color actual: No definido");
        tvCapacidadActual.setText("Capacidad actual: 0");
        tvAnioActual.setText("Año actual: No definido");

        etPlaca.setText("");
        etModelo.setText("");
        etColor.setText("");
        etCapacidad.setText("");
        etAnio.setText("");
    }

    private void configurarListeners() {
        // Botón Cancelar
        btnCancelar.setOnClickListener(v -> {
            finish();
        });

        // Botón Guardar Cambios
        btnGuardarCambios.setOnClickListener(v -> {
            guardarCambios();
        });
    }

    private void guardarCambios() {
        // Validar campos obligatorios
        String nombre = etNombre.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        String placa = etPlaca.getText().toString().trim();
        String modelo = etModelo.getText().toString().trim();
        String color = etColor.getText().toString().trim();
        String capacidadStr = etCapacidad.getText().toString().trim();
        String anio = etAnio.getText().toString().trim();

        if (TextUtils.isEmpty(nombre)) {
            etNombre.setError("El nombre es obligatorio");
            return;
        }

        if (TextUtils.isEmpty(telefono)) {
            etTelefono.setError("El teléfono es obligatorio");
            return;
        }

        if (TextUtils.isEmpty(placa)) {
            etPlaca.setError("La placa es obligatoria");
            return;
        }

        // Validar capacidad
        int capacidad = 0;
        if (!TextUtils.isEmpty(capacidadStr)) {
            try {
                capacidad = Integer.parseInt(capacidadStr);
                if (capacidad <= 0) {
                    etCapacidad.setError("La capacidad debe ser mayor a 0");
                    return;
                }
            } catch (NumberFormatException e) {
                etCapacidad.setError("Formato inválido");
                return;
            }
        }

        // Mostrar progreso
        btnGuardarCambios.setEnabled(false);
        btnGuardarCambios.setText("Guardando...");

        // Actualizar conductor y vehículo
        actualizarConductorYVehiculo(nombre, telefono, placa, modelo, color, capacidad, anio);
    }

    private void actualizarConductorYVehiculo(String nombre, String telefono, String placa,
                                              String modelo, String color, int capacidad, String anio) {
        // Primero actualizar/conseguir vehículo
        actualizarVehiculo(placa, modelo, color, capacidad, anio, new VehiculoCallback() {
            @Override
            public void onVehiculoActualizado(String vehiculoId) {
                // Luego actualizar conductor con el ID del vehículo
                actualizarConductor(nombre, telefono, vehiculoId);
            }

            @Override
            public void onError(String error) {
                btnGuardarCambios.setEnabled(true);
                btnGuardarCambios.setText("Guardar");
                Toast.makeText(EditarPerfilConductor.this,
                        "Error al guardar vehículo: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void actualizarVehiculo(String placa, String modelo, String color, int capacidad,
                                    String anio, VehiculoCallback callback) {
        // Si ya existe un vehículo, actualizarlo. Si no, crear uno nuevo.
        if (vehiculoActual != null && vehiculoId != null) {
            // Actualizar vehículo existente
            vehiculoActual.setPlaca(placa);
            vehiculoActual.setModelo(modelo);
            vehiculoActual.setColor(color);
            vehiculoActual.setCapacidad(capacidad);
            vehiculoActual.setAno(anio);
            vehiculoActual.setEstado("activo");

            vehiculoRef.child(vehiculoId).setValue(vehiculoActual)
                    .addOnSuccessListener(aVoid -> callback.onVehiculoActualizado(vehiculoId))
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
        } else {
            // Crear nuevo vehículo
            String nuevoVehiculoId = vehiculoRef.push().getKey();
            Vehiculo nuevoVehiculo = new Vehiculo(nuevoVehiculoId, placa, modelo, color, anio,
                    capacidad, userId, "activo");

            vehiculoRef.child(nuevoVehiculoId).setValue(nuevoVehiculo)
                    .addOnSuccessListener(aVoid -> callback.onVehiculoActualizado(nuevoVehiculoId))
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
        }
    }

    private void actualizarConductor(String nombre, String telefono, String vehiculoId) {
        DatabaseReference conductorRef = FirebaseDatabase.getInstance()
                .getReference("conductores")
                .child(userId);

        // Actualizar datos del conductor
        conductorRef.child("nombre").setValue(nombre);
        conductorRef.child("telefono").setValue(telefono);
        conductorRef.child("vehiculoId").setValue(vehiculoId);
        conductorRef.child("placaVehiculo").setValue(etPlaca.getText().toString().trim());

        // También actualizar en la colección de usuarios para consistencia
        DatabaseReference usuarioRef = FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .child(userId);

        usuarioRef.child("nombre").setValue(nombre);
        usuarioRef.child("telefono").setValue(telefono);

        // Éxito
        btnGuardarCambios.setEnabled(true);
        btnGuardarCambios.setText("Guardar");
        Toast.makeText(this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
        finish();
    }

    // Interface para callback del vehículo
    private interface VehiculoCallback {
        void onVehiculoActualizado(String vehiculoId);
        void onError(String error);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Limpiar recursos si es necesario
    }
}