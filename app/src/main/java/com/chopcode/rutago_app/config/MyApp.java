package com.chopcode.rutago_app.config;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.Map;

public class MyApp extends Application {

    private static MyApp instance;
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseMessaging firebaseMessaging;
    private FirebaseAnalytics firebaseAnalytics;
    private FirebaseCrashlytics firebaseCrashlytics;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Inicializar FirebaseApp
        FirebaseApp.initializeApp(this);

        // Inicializar cada servicio
        initializeFirebaseServices();
    }

    private void initializeFirebaseServices() {
        // 1. Authentication
        firebaseAuth = FirebaseAuth.getInstance();

        // 2. Realtime Database
        firebaseDatabase = FirebaseDatabase.getInstance();
        // Opcional: Habilitar persistencia offline
        firebaseDatabase.setPersistenceEnabled(true);

        // 3. Cloud Messaging (FCM)
        firebaseMessaging = FirebaseMessaging.getInstance();

        // 4. Analytics
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // 5. Crashlytics
        firebaseCrashlytics = FirebaseCrashlytics.getInstance();
        firebaseCrashlytics.setCrashlyticsCollectionEnabled(true);
    }

    // Singleton para acceder a la instancia
    public static MyApp getInstance() {
        return instance;
    }

    // Métodos getter
    public FirebaseAuth getFirebaseAuth() {
        return firebaseAuth;
    }

    public FirebaseDatabase getFirebaseDatabase() {
        return firebaseDatabase;
    }

    public FirebaseMessaging getFirebaseMessaging() {
        return firebaseMessaging;
    }

    public FirebaseAnalytics getFirebaseAnalytics() {
        return firebaseAnalytics;
    }

    public FirebaseCrashlytics getFirebaseCrashlytics() {
        return firebaseCrashlytics;
    }

    public static Context getAppContext() {
        return instance.getApplicationContext();
    }

    // Métodos helper para uso rápido
    public static void logEvent(String eventName, Map<String, Object> params) {
        if (getInstance() == null || params == null) {
            Log.e("MyApp", "⚠️ No se puede registrar evento: MyApp no inicializado o parámetros nulos");
            return;
        }

        try {
            Bundle bundle = new Bundle();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (entry.getValue() != null) {
                    // Convertir diferentes tipos de objetos a String para Bundle
                    bundle.putString(entry.getKey(), entry.getValue().toString());
                }
            }
            getInstance().getFirebaseAnalytics().logEvent(eventName, bundle);
            Log.d("MyApp", "📊 Evento registrado: " + eventName + " con " + params.size() + " parámetros");
        } catch (Exception e) {
            Log.e("MyApp", "❌ Error registrando evento " + eventName + ": " + e.getMessage());
        }
    }

    // ✅ MÉTODO ALTERNATIVO: Para cuando quieras usar Bundle directamente
    public static void logEventWithBundle(String eventName, Bundle bundle) {
        if (getInstance() == null || bundle == null) {
            Log.e("MyApp", "⚠️ No se puede registrar evento con Bundle: MyApp no inicializado");
            return;
        }

        try {
            getInstance().getFirebaseAnalytics().logEvent(eventName, bundle);
            Log.d("MyApp", "📊 Evento con Bundle registrado: " + eventName);
        } catch (Exception e) {
            Log.e("MyApp", "❌ Error registrando evento con Bundle " + eventName + ": " + e.getMessage());
        }
    }

    public static void logError(Exception e) {
        getInstance().getFirebaseCrashlytics().recordException(e);
    }

    public static DatabaseReference getDatabaseReference(String path) {
        return getInstance().getFirebaseDatabase().getReference(path);
    }

    public static String getCurrentUserId() {
        FirebaseUser user = getInstance().getFirebaseAuth().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public static FirebaseUser getCurrentUser() {
        return getInstance().getFirebaseAuth().getCurrentUser();
    }
}