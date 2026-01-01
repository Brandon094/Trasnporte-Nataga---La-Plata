package com.chopcode.trasnportenataga_laplata.activities.passenger.reservation.confirmReservation;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.activities.passenger.InicioUsuariosActivity;
import com.chopcode.trasnportenataga_laplata.config.MyApp;
import com.chopcode.trasnportenataga_laplata.managers.analytics.ReservationAnalyticsHelper;
import com.chopcode.trasnportenataga_laplata.managers.reservations.ReservationUserManager;
import com.chopcode.trasnportenataga_laplata.managers.reservations.confirmation.ConfirmationDataProcessor;
import com.chopcode.trasnportenataga_laplata.managers.reservations.confirmation.ConfirmationUIManager;
import com.chopcode.trasnportenataga_laplata.managers.reservations.confirmation.ReservationConfirmationManager;
import com.chopcode.trasnportenataga_laplata.managers.reservations.confirmation.ConfirmationAnalyticsHelper;
import com.chopcode.trasnportenataga_laplata.managers.reservations.confirmation.ConfirmationDialogManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class ConfirmarReservaActivity extends AppCompatActivity implements
        ConfirmationUIManager.ConfirmationListener,
        ReservationConfirmationManager.ConfirmationCallback,
        ConfirmationDialogManager.DialogCallback {

    private static final String TAG = "ConfirmarReserva";

    // UI Elements
    private MaterialButton btnConfirmarReserva, btnCancelar;
    private MaterialToolbar topAppBar;

    // Managers
    private ReservationAnalyticsHelper analyticsHelper;
    private ReservationUserManager userManager;
    private ConfirmationDataProcessor dataProcessor;
    private ConfirmationUIManager uiManager;
    private ReservationConfirmationManager confirmationManager;
    private ConfirmationAnalyticsHelper confirmationAnalytics;
    private ConfirmationDialogManager dialogManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Inicializar analytics
        analyticsHelper = new ReservationAnalyticsHelper("ConfirmarReserva");
        analyticsHelper.logPantallaInicio();

        // ✅ Inicializar managers
        initializeManagers();

        setContentView(R.layout.activity_confirmar_reserva);

        // ✅ Inicializar vistas
        initializeViews();

        // ✅ Configurar UI
        configureToolbar();
        configureUIManager();
        loadDataIntoUI();
    }

    private void initializeManagers() {
        // Inicializar managers existentes
        userManager = new ReservationUserManager(analyticsHelper);
        dataProcessor = new ConfirmationDataProcessor(analyticsHelper);

        // Inicializar nuevos managers
        uiManager = new ConfirmationUIManager(dataProcessor, analyticsHelper);
        confirmationManager = new ReservationConfirmationManager(this, analyticsHelper);
        confirmationAnalytics = new ConfirmationAnalyticsHelper(analyticsHelper, dataProcessor);
        dialogManager = new ConfirmationDialogManager(this, confirmationAnalytics);

        // Configurar callbacks
        confirmationManager.setDataProcessor(dataProcessor);
        confirmationManager.setConfirmationCallback(this);
    }

    private void initializeViews() {
        // Toolbar
        topAppBar = findViewById(R.id.topAppBar);

        // Botones
        btnConfirmarReserva = findViewById(R.id.btnConfirmarReserva);
        btnCancelar = findViewById(R.id.btnCancelar);

        // Inicializar referencias de vistas para el UIManager
        initializeViewReferences();
    }

    private void initializeViewReferences() {
        // Obtener referencias de vistas DEL NUEVO LAYOUT
        TextView tvOrigen = findViewById(R.id.tvOrigen);
        TextView tvDestino = findViewById(R.id.tvDestino);
        TextView tvFecha = findViewById(R.id.tvFecha);
        TextView tvHora = findViewById(R.id.tvHora);
        TextView tvTiempoEstimado = findViewById(R.id.tvTiempoEstimado);
        TextView tvPrecio = findViewById(R.id.tvPrecio);
        TextView tvAsiento = findViewById(R.id.tvAsiento);
        TextView tvUsuario = findViewById(R.id.tvUsuario);
        TextView tvTelefonoP = findViewById(R.id.tvTelefonoP);
        TextView tvConductor = findViewById(R.id.tvConductor);
        TextView tvTelefonoC = findViewById(R.id.tvTelefonoC);
        TextView tvPlaca = findViewById(R.id.tvPlaca);

        // IMPORTANTE: IDs CORRECTOS después de la corrección
        MaterialCardView cardEfectivo = findViewById(R.id.cardEfectivo);
        MaterialCardView cardTransferencia = findViewById(R.id.cardTransferencia);
        ExtendedFloatingActionButton fabAyuda = findViewById(R.id.fabAyuda);

        // Configurar UIManager con las nuevas vistas
        uiManager.setNewViewReferences(
                tvOrigen, tvDestino, tvFecha, tvHora, tvTiempoEstimado,
                tvPrecio, tvAsiento, tvUsuario, tvTelefonoP,
                tvConductor, tvTelefonoC, tvPlaca,
                cardEfectivo, cardTransferencia, fabAyuda
        );

        uiManager.setConfirmationListener(this);
    }

    private void configureToolbar() {
        setSupportActionBar(topAppBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        topAppBar.setNavigationOnClickListener(v -> {
            confirmationAnalytics.logButtonClick("navegacion_atras");
            onBackPressed();
        });
    }

    private void configureUIManager() {
        // Configurar listeners de botones
        btnConfirmarReserva.setOnClickListener(v -> {
            confirmationAnalytics.logButtonClick("confirmar_reserva");
            onConfirmButtonClicked();
        });

        btnCancelar.setOnClickListener(v -> {
            confirmationAnalytics.logButtonClick("cancelar_reserva");
            onCancelButtonClicked();
        });

        // Configurar listeners del NUEVO UI Manager
        uiManager.setupNewListeners(); // <-- Cambiar a este método
    }

    private void loadDataIntoUI() {
        // ✅ Procesar datos del Intent
        processIntentData();

        // ✅ Cargar datos del usuario
        loadAuthenticatedUser();
    }

    private void processIntentData() {
        Intent intent = getIntent();
        dataProcessor.processIntentData(intent);

        // ✅ Actualizar userManager con datos del usuario
        String usuarioId = intent.getStringExtra("usuarioId");
        String usuarioNombre = intent.getStringExtra("usuarioNombre");
        String usuarioTelefono = intent.getStringExtra("usuarioTelefono");

        if (usuarioNombre != null) {
            userManager.updateFromIntent(usuarioId, usuarioNombre, usuarioTelefono);
            Log.d(TAG, "✅ Datos de usuario actualizados desde Intent");
            updateUIWithUserData();
        }
    }

    private void loadAuthenticatedUser() {
        userManager.loadAuthenticatedUser(new ReservationUserManager.UserDataCallback() {
            @Override
            public void onUserDataLoaded(String usuarioId, String usuarioNombre, String usuarioTelefono) {
                Log.d(TAG, "✅ Usuario cargado: " + usuarioNombre);
                updateUIWithUserData();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error cargando usuario: " + error);
                confirmationAnalytics.logError("carga_usuario", error);
            }
        });
    }

    private void updateUIWithUserData() {
        runOnUiThread(() -> {
            // Primero actualizar el dataProcessor con los datos del usuario
            dataProcessor.setUsuarioNombre(userManager.getUsuarioNombre());
            dataProcessor.setUsuarioTelefono(userManager.getUsuarioTelefono());
            dataProcessor.setUsuarioId(userManager.getUsuarioId());

            // Luego cargar los datos en la UI
            uiManager.loadDataIntoNewUI(
                    userManager.getUsuarioNombre(),
                    userManager.getUsuarioTelefono()
            );

            // Log para debugging
            Log.d(TAG, dataProcessor.getSummary());
        });
    }

    @Override
    public void onHelpRequested() {
        // Implementar funcionalidad de ayuda
        // Por ejemplo, mostrar un diálogo o navegar a FAQ
        Toast.makeText(this, "¿En qué podemos ayudarte?", Toast.LENGTH_SHORT).show();
        confirmationAnalytics.logButtonClick("fab_ayuda");
    }

    // ✅ Implementación de ConfirmationUIManager.ConfirmationListener
    @Override
    public void onConfirmButtonClicked() {
        if (validateForm()) {
            confirmationManager.confirmReservation();
        } else {
            Toast.makeText(this, "Por favor selecciona un método de pago", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCancelButtonClicked() {
        dialogManager.showCancellationDialog(this);
    }

    @Override
    public void onPaymentMethodChanged(String metodoPago) {
        // Log adicional si es necesario
        Log.d(TAG, "Método de pago cambiado a: " + metodoPago);
    }

    // ✅ Implementación de ReservationConfirmationManager.ConfirmationCallback
    @Override
    public void onConfirmationStarted() {
        runOnUiThread(() -> {
            btnConfirmarReserva.setEnabled(false);
            btnConfirmarReserva.setText("Procesando...");
        });
    }

    @Override
    public void onConfirmationSuccess() {
        confirmationAnalytics.logNavigation("InicioUsuariosActivity");
        navigateToHome();
    }

    @Override
    public void onConfirmationError(String error) {
        runOnUiThread(() -> {
            btnConfirmarReserva.setEnabled(true);
            btnConfirmarReserva.setText("Confirmar Reserva");
        });
    }

    @Override
    public void onButtonStateChanged(boolean enabled, String text) {
        runOnUiThread(() -> {
            btnConfirmarReserva.setEnabled(enabled);
            btnConfirmarReserva.setText(text);
        });
    }

    // ✅ Implementación de ConfirmationDialogManager.DialogCallback
    @Override
    public void onPositiveAction() {
        finish();
    }

    @Override
    public void onNegativeAction() {
        // Dialogo cerrado sin acción
    }

    private boolean validateForm() {
        String metodoPago = dataProcessor.getMetodoPago();
        boolean isValid = metodoPago != null && !metodoPago.isEmpty();

        if (isValid) {
            confirmationAnalytics.logValidationSuccess();
        } else {
            confirmationAnalytics.logValidationFailed("sin_metodo_pago");
        }

        return isValid;
    }

    private void navigateToHome() {
        try {
            Intent intent = new Intent(this, InicioUsuariosActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error navegando a inicio: " + e.getMessage());
            confirmationAnalytics.logError("navegacion", e.getMessage());
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        confirmationAnalytics.logNavigation("boton_back_fisico");
        dialogManager.showCancellationDialog(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (confirmationManager != null) {
            confirmationManager.cleanup();
        }
        confirmationAnalytics.logScreenEvent("destroy");
    }
}