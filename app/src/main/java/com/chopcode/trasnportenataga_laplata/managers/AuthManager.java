package com.chopcode.trasnportenataga_laplata.managers;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.chopcode.trasnportenataga_laplata.activities.InicioDeSesion;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthManager {
    private static AuthManager instance;
    private FirebaseAuth auth;

    private AuthManager() {
        auth = FirebaseAuth.getInstance();
    }

    public static AuthManager getInstance() {
        if (instance == null) {
            instance = new AuthManager();
        }
        return instance;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public boolean isUserLoggedIn() {
        return getCurrentUser() != null;
    }

    public boolean validateLogin(Context context) {
        if (!isUserLoggedIn()) {
            Toast.makeText(context, "Debes iniciar sesión", Toast.LENGTH_SHORT).show();
            redirectToLogin(context);
            return false;
        }
        return true;
    }

    public void redirectToLogin(Context context) {
        Intent intent = new Intent(context, InicioDeSesion.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    public void signOut(Context context) {
        auth.signOut();
        redirectToLogin(context);
    }

    public String getUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }
}