package com.chopcode.rutago_app.managers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.chopcode.rutago_app.activities.common.InicioDeSesion;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthManager {
    // ✅ NUEVO: Tag para logs
    private static final String TAG = "AuthManager";

    private static AuthManager instance;
    private FirebaseAuth auth;

    private AuthManager() {
        Log.d(TAG, "🚀 Constructor - Inicializando AuthManager singleton");
        auth = FirebaseAuth.getInstance();
        Log.d(TAG, "✅ FirebaseAuth instancia obtenida");
    }

    public static AuthManager getInstance() {
        Log.d(TAG, "🔍 Solicitando instancia de AuthManager");
        if (instance == null) {
            Log.d(TAG, "🆕 Creando nueva instancia de AuthManager (primera vez)");
            instance = new AuthManager();
        } else {
            Log.d(TAG, "✅ Retornando instancia existente de AuthManager");
        }
        return instance;
    }

    public FirebaseUser getCurrentUser() {
        FirebaseUser user = auth.getCurrentUser();
        Log.d(TAG, "👤 Obteniendo usuario actual - Existe: " + (user != null));
        if (user != null) {
            Log.d(TAG, "   - UID: " + user.getUid());
            Log.d(TAG, "   - Email: " + user.getEmail());
            Log.d(TAG, "   - Nombre: " + user.getDisplayName());
        } else {
            Log.d(TAG, "⚠️ No hay usuario autenticado actualmente");
        }
        return user;
    }

    public boolean isUserLoggedIn() {
        boolean isLoggedIn = getCurrentUser() != null;
        Log.d(TAG, "🔐 Verificando si usuario está logeado: " + isLoggedIn);
        return isLoggedIn;
    }

    public boolean validateLogin(Context context) {
        Log.d(TAG, "🔍 Validando login del usuario...");
        boolean isLoggedIn = isUserLoggedIn();

        if (!isLoggedIn) {
            Log.w(TAG, "❌ Usuario no autenticado - redirigiendo a login");
            Toast.makeText(context, "Debes iniciar sesión", Toast.LENGTH_SHORT).show();
            redirectToLogin(context);
            return false;
        }

        Log.d(TAG, "✅ Usuario validado correctamente");
        return true;
    }

    public void redirectToLogin(Context context) {
        Log.d(TAG, "🔄 Redirigiendo a pantalla de login");
        Log.d(TAG, "   - Context: " + context.getClass().getSimpleName());

        try {
            Intent intent = new Intent(context, InicioDeSesion.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            Log.d(TAG, "✅ Intent creado - flags: NEW_TASK | CLEAR_TASK");

            context.startActivity(intent);
            Log.d(TAG, "🎯 Actividad de login iniciada exitosamente");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error redirigiendo a login: " + e.getMessage(), e);
            Toast.makeText(context, "Error al redirigir: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void signOut(Context context) {
        Log.d(TAG, "🚪 Iniciando cierre de sesión...");

        FirebaseUser currentUser = getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "👤 Cerrando sesión para usuario: " + currentUser.getEmail());
        } else {
            Log.w(TAG, "⚠️ No hay usuario para cerrar sesión, pero procediendo igual");
        }

        try {
            auth.signOut();
            Log.d(TAG, "✅ Sesión cerrada en Firebase Auth");

            // Verificar que realmente se cerró la sesión
            boolean stillLoggedIn = isUserLoggedIn();
            if (stillLoggedIn) {
                Log.e(TAG, "❌ ERROR: La sesión no se cerró correctamente");
            } else {
                Log.d(TAG, "✅ Verificación: Sesión cerrada correctamente");
            }

            redirectToLogin(context);
            Log.d(TAG, "🎯 Redirección a login después de cerrar sesión");

        } catch (Exception e) {
            Log.e(TAG, "💥 Error crítico cerrando sesión: " + e.getMessage(), e);
            Toast.makeText(context, "Error al cerrar sesión: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public String getUserId() {
        FirebaseUser user = getCurrentUser();
        String userId = user != null ? user.getUid() : null;

        Log.d(TAG, "🆔 Obteniendo UserId: " + (userId != null ? userId : "NULL"));

        if (userId == null) {
            Log.w(TAG, "⚠️ UserId es null - usuario no autenticado");
        }

        return userId;
    }

    // ✅ NUEVO MÉTODO: Verificar estado de autenticación detallado
    public void logAuthStatus() {
        FirebaseUser user = getCurrentUser();
        Log.d(TAG, "📊 ESTADO DE AUTENTICACIÓN:");
        Log.d(TAG, "   - Usuario autenticado: " + (user != null));

        if (user != null) {
            Log.d(TAG, "   - UID: " + user.getUid());
            Log.d(TAG, "   - Email: " + user.getEmail());
            Log.d(TAG, "   - Verificado: " + user.isEmailVerified());
            Log.d(TAG, "   - Provider: " + user.getProviderId());
            Log.d(TAG, "   - Display Name: " + user.getDisplayName());
            Log.d(TAG, "   - Phone: " + user.getPhoneNumber());
        } else {
            Log.d(TAG, "   - No hay sesión activa");
        }
    }

    // ✅ NUEVO MÉTODO: Verificar si el usuario está verificado por email
    public boolean isEmailVerified() {
        FirebaseUser user = getCurrentUser();
        boolean isVerified = user != null && user.isEmailVerified();

        Log.d(TAG, "📧 Verificación de email: " + isVerified);
        if (user != null) {
            Log.d(TAG, "   - Email: " + user.getEmail());
            Log.d(TAG, "   - Verificado: " + isVerified);
        }

        return isVerified;
    }
}