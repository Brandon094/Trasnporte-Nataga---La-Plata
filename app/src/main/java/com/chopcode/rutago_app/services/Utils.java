package com.chopcode.rutago_app.services;

import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;

import com.chopcode.rutago_app.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class Utils {

    public static void configurarVisibilidadContraseña(TextInputLayout passwordInputLayout, TextInputEditText editTextPassword) {
        // Icono inicial
        passwordInputLayout.setEndIconDrawable(R.drawable.ic_visibility_off);

        // Listener para alternar visibilidad
        passwordInputLayout.setEndIconOnClickListener(v -> {
            if (editTextPassword.getTransformationMethod() instanceof PasswordTransformationMethod) {
                editTextPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                passwordInputLayout.setEndIconDrawable(R.drawable.ic_visibility_on);
            } else {
                editTextPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                passwordInputLayout.setEndIconDrawable(R.drawable.ic_visibility_off);
            }

            // Coloca el cursor al final del texto
            editTextPassword.setSelection(editTextPassword.getText().length());
        });
    }
}
