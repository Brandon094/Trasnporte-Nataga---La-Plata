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
import com.google.firebase.auth.*;

public class InicioDeSesion extends AppCompatActivity {

    private EditText editTextUsuario, editTextPassword;
    private Button buttonIngresar, buttonRegistro;
    private com.google.android.gms.common.SignInButton btnGoogleSignIn;
    private ImageButton btnMostrarContrasena;
    private IniciarService iniciarService;

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
        btnMostrarContrasena = findViewById(R.id.btnMostrarContrasena);

        // Configurar botón para mostrar/ocultar contraseña
        btnMostrarContrasena.setOnClickListener(v -> {
            if (editTextPassword.getTransformationMethod() instanceof PasswordTransformationMethod) {
                editTextPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                btnMostrarContrasena.setImageResource(R.drawable.baseline_remove_red_eye_24);
            } else {
                editTextPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                btnMostrarContrasena.setImageResource(R.drawable.baseline_visibility_off_24);
            }
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
                        Toast.makeText(InicioDeSesion.this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
                        irAInicioUsuarios();
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
        } else {
            Log.e("InicioDeSesion", "Error: buttonRegistro es null. Verifica el ID en el XML.");
        }

        // Manejar inicio de sesión con Google
        btnGoogleSignIn.setOnClickListener(v -> {
            iniciarService.iniciarSesionGoogle(new IniciarService.LoginCallback() {
                @Override
                public void onLoginSuccess() {
                    Toast.makeText(InicioDeSesion.this, "Inicio de sesión con Google exitoso", Toast.LENGTH_SHORT).show();
                    irAInicioUsuarios();
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
                    Toast.makeText(InicioDeSesion.this, "Inicio de sesión con Google exitoso", Toast.LENGTH_SHORT).show();
                    irAInicioUsuarios();
                }
                @Override
                public void onLoginFailure(String error) {
                    Toast.makeText(InicioDeSesion.this, "Error: " + error, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    /**
     * Redirige a la actividad principal de usuarios tras iniciar sesión.
     */
    private void irAInicioUsuarios() {
        Intent intent = new Intent(InicioDeSesion.this, InicioUsuarios.class);
        startActivity(intent);
        finish();
    }
}
