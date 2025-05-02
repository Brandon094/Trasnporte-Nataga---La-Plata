package com.chopcode.trasnportenataga_laplata.services;

import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;

import com.chopcode.trasnportenataga_laplata.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class Utils {

    public static void configurarVisibilidadContraseña(TextInputLayout passwordInputLayout, TextInputEditText editTextPassword) {
        // Icono inicial
        passwordInputLayout.setEndIconDrawable(R.drawable.baseline_visibility_off_24);

        // Listener para alternar visibilidad
        passwordInputLayout.setEndIconOnClickListener(v -> {
            if (editTextPassword.getTransformationMethod() instanceof PasswordTransformationMethod) {
                editTextPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                passwordInputLayout.setEndIconDrawable(R.drawable.baseline_remove_red_eye_24);
            } else {
                editTextPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                passwordInputLayout.setEndIconDrawable(R.drawable.baseline_visibility_off_24);
            }

            // Coloca el cursor al final del texto
            editTextPassword.setSelection(editTextPassword.getText().length());
        });
    }
}
