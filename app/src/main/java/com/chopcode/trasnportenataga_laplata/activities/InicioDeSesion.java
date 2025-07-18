package com.chopcode.trasnportenataga_laplata.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.widget.*;
import com.chopcode.trasnportenataga_laplata.R;
import com.chopcode.trasnportenataga_laplata.services.IniciarService;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.*;

public class InicioDeSesion extends AppCompatActivity {

    private EditText editTextUsuario, editTextPassword;
    private Button buttonIngresar;
    private Button btnGoogleSignIn;
    private IniciarService iniciarService;
    private TextView buttonRegistro, olvidasteContraseña;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio_de_sesion);

        // Inicializar IniciarService, pasando la actividad actual
        iniciarService = new IniciarService(this);

        // Referenciar elementos de UI
        editTextUsuario = findViewById(R.id.editTextUsuario);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonIngresar = findViewById(R.id.buttonIngresar);
        buttonRegistro = findViewById(R.id.buttonRegistro);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        TextInputLayout passwordInputLayout = findViewById(R.id.passwordInputLayout);
        TextInputEditText editTextPassword = findViewById(R.id.editTextPassword);

        // Establecer el icono inicial (contraseña oculta)
        passwordInputLayout.setEndIconDrawable(R.drawable.baseline_visibility_off_24);

        // Manejar clic en el icono de visibilidad
        passwordInputLayout.setEndIconOnClickListener(v -> {
            if (editTextPassword.getTransformationMethod() instanceof PasswordTransformationMethod) {
                // Si está oculta, mostrarla
                editTextPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                passwordInputLayout.setEndIconDrawable(R.drawable.baseline_remove_red_eye_24);
            } else {
                // Si está visible, ocultarla
                editTextPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                passwordInputLayout.setEndIconDrawable(R.drawable.baseline_visibility_off_24);
            }
            // Mover cursor al final
            editTextPassword.setSelection(editTextPassword.getText().length());
        });

        // Manejar inicio de sesión con correo y contraseña
        buttonIngresar.setOnClickListener(v -> {
            String correo = editTextUsuario.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (correo.isEmpty() || password.isEmpty()) {
                Toast.makeText(InicioDeSesion.this, "Ingresa correo y contraseña", Toast.LENGTH_SHORT).show();
            } else {
                iniciarService.iniciarSesionCorreo(correo, password, new IniciarService.LoginCallback() {
                    @Override
                    public void onLoginSuccess() {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            iniciarService.detectarTipoUsuario(user, new IniciarService.TipoUsuarioCallback() {
                                @Override
                                public void onTipoDetectado(String tipo) {
                                    if (tipo.equals("conductor")) {
                                        irAInicioConductor();
                                    } else {
                                        irAInicioUsuarios();
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    Toast.makeText(InicioDeSesion.this, "Error al detectar tipo de usuario: " + error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                    @Override
                    public void onLoginFailure(String error) {
                        Toast.makeText(InicioDeSesion.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        // Manejar botón de registro
        if (buttonRegistro != null) {
            buttonRegistro.setOnClickListener(v -> {
                Intent intent = new Intent(InicioDeSesion.this, RegistroUsuarios.class);
                startActivity(intent);
            });
        }

        // Manejar inicio de sesión con Google
        btnGoogleSignIn.setOnClickListener(v -> {
            iniciarService.iniciarSesionGoogle(new IniciarService.LoginCallback() {
                @Override
                public void onLoginSuccess() {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        iniciarService.detectarTipoUsuario(user, new IniciarService.TipoUsuarioCallback() {
                            @Override
                            public void onTipoDetectado(String tipo) {
                                if (tipo.equals("conductor")) {
                                    irAInicioConductor();
                                } else {
                                    irAInicioUsuarios();
                                }
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(InicioDeSesion.this, "Error al detectar tipo de usuario: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                @Override
                public void onLoginFailure(String error) {
                    Toast.makeText(InicioDeSesion.this, "Error: " + error, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    // Recibir el resultado del One Tap Sign-In de Google
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IniciarService.REQ_ONE_TAP) {
            iniciarService.manejarResultadoGoogle(data, new IniciarService.LoginCallback() {
                @Override
                public void onLoginSuccess() {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        iniciarService.detectarTipoUsuario(user, new IniciarService.TipoUsuarioCallback() {
                            @Override
                            public void onTipoDetectado(String tipo) {
                                if (tipo.equals("conductor")) {
                                    irAInicioConductor();
                                } else {
                                    irAInicioUsuarios();
                                }
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(InicioDeSesion.this, "Error al detectar tipo de usuario: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                @Override
                public void onLoginFailure(String error) {
                    Toast.makeText(InicioDeSesion.this, "Error: " + error, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    /**
     * Redirige a la actividad principal de usuarios o a reservas tras iniciar sesión.
     */
    private void irAInicioUsuarios() {
        // Verificar si el usuario intentó hacer una reserva antes de iniciar sesión
        boolean volverAReserva = getIntent().getBooleanExtra("volverAReserva", false);

        if (volverAReserva) {
            // Si vino de intentar reservar, llevarlo directamente a reservas
            Intent intent = new Intent(InicioDeSesion.this, Reservas.class);
            startActivity(intent);
        } else {
            // Caso normal: ir a la pantalla principal
            Intent intent = new Intent(InicioDeSesion.this, InicioUsuarios.class);
            startActivity(intent);
        }
        finish();
    }
    private void irAInicioConductor() {
        Intent intent = new Intent(InicioDeSesion.this, InicioConductor.class);
        startActivity(intent);
        finish();
    }
}
