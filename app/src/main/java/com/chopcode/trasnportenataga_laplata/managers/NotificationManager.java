package com.chopcode.trasnportenataga_laplata.managers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NotificationManager {

    private static final String TAG = "NotificationManager";
    private static NotificationManager instance;
    private final FirebaseFirestore db;
    private final DatabaseReference realtimeDb;
    private final OkHttpClient httpClient;
    private final ConnectivityManager connectivityManager;

    // Configuración FCM
    private static final String FCM_API_URL = "https://fcm.googleapis.com/fcm/send";
    private static final String FCM_SERVER_KEY = "BAq-S8bthzR18EdgK6lzrZhdSxMqaJhON_EZ-FkbfK9LGjQRl6oJMTdSc87RfE0uKQqBJYkZWK0RWzGoxfL5l6I";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // Reintentos configurables
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 2000;

    private NotificationManager(Context context) {
        Log.d(TAG, "🔄 CONSTRUCTOR - Inicializando NotificationManager");

        db = FirebaseFirestore.getInstance();
        realtimeDb = FirebaseDatabase.getInstance().getReference(); // ✅ NUEVO: Referencia a Realtime DB

        // ✅ Configurar persistencia offline AGGRESIVA
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();
        db.setFirestoreSettings(settings);
        Log.d(TAG, "✅ CONSTRUCTOR - Persistencia offline configurada");

        httpClient = new OkHttpClient();
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Log.d(TAG, "✅ CONSTRUCTOR - NotificationManager inicializado exitosamente");
    }

    public static synchronized NotificationManager getInstance(Context context) {
        Log.d(TAG, "📞 getInstance() - Solicitando instancia");
        if (instance == null) {
            Log.d(TAG, "🆕 getInstance() - Creando nueva instancia");
            instance = new NotificationManager(context.getApplicationContext());
        } else {
            Log.d(TAG, "♻️ getInstance() - Retornando instancia existente");
        }
        return instance;
    }

    /**
     * ✅ NOTIFICACIÓN CRÍTICA: Nueva reserva al conductor
     */
    public void notificarNuevaReservaAlConductor(String conductorId, String pasajeroNombre,
                                                 String ruta, String fechaHora, int asiento,
                                                 double precio, String metodoPago,
                                                 NotificationCallback callback) {
        Log.d(TAG, "🚀 notificarNuevaReservaAlConductor - INICIANDO");
        Log.d(TAG, "📋 DATOS RESERVA:");
        Log.d(TAG, "   - Conductor ID: " + conductorId);
        Log.d(TAG, "   - Pasajero: " + pasajeroNombre);
        Log.d(TAG, "   - Ruta: " + ruta);
        Log.d(TAG, "   - Asiento: A" + asiento);
        Log.d(TAG, "   - Fecha/Hora: " + fechaHora);

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("title", "🚗 Nueva Reserva");
        notificationData.put("message", String.format("%s reservó asiento A%d para %s", pasajeroNombre, asiento, ruta));
        notificationData.put("type", "nueva_reserva");
        notificationData.put("reserva_ruta", ruta);
        notificationData.put("reserva_fecha_hora", fechaHora);
        notificationData.put("reserva_asiento", "A" + asiento);
        notificationData.put("reserva_precio", precio);
        notificationData.put("reserva_metodo_pago", metodoPago);
        notificationData.put("reserva_pasajero", pasajeroNombre);
        notificationData.put("target_activity", "driver_home");
        notificationData.put("userType", "driver");
        notificationData.put("timestamp", System.currentTimeMillis());
        notificationData.put("read", false);
        notificationData.put("critical", true);

        Log.d(TAG, "📦 notificarNuevaReservaAlConductor - Datos de notificación preparados");

        // 1. Guardar en Firestore PRIMERO (siempre funciona offline)
        guardarNotificacionEnBaseDeDatos(conductorId, notificationData, new NotificationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "✅ notificarNuevaReservaAlConductor - Notificación guardada en BD para conductor: " + conductorId);

                // 2. Intentar enviar notificación PUSH con reintentos
                enviarNotificacionPushConReintentos(conductorId, "🚗 Nueva Reserva",
                        String.format("%s reservó asiento A%d para %s", pasajeroNombre, asiento, ruta),
                        notificationData, 0, callback);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ notificarNuevaReservaAlConductor - Error guardando notificación: " + error);
                if (callback != null) {
                    callback.onError("Error guardando notificación: " + error);
                }
            }
        });
    }

    /**
     * ✅ NOTIFICACIÓN 2: Conductor → Pasajero (Reserva Confirmada)
     */
    public void notificarReservaConfirmadaAlPasajero(String pasajeroId, String conductorNombre,
                                                     String ruta, String fechaHora, int asiento,
                                                     String vehiculoPlaca, String vehiculoModelo,
                                                     NotificationCallback callback) {
        Log.d(TAG, "🚀 notificarReservaConfirmadaAlPasajero - INICIANDO");
        Log.d(TAG, "📋 DATOS CONFIRMACIÓN:");
        Log.d(TAG, "   - Pasajero ID: " + pasajeroId);
        Log.d(TAG, "   - Conductor: " + conductorNombre);
        Log.d(TAG, "   - Ruta: " + ruta);
        Log.d(TAG, "   - Asiento: A" + asiento);
        Log.d(TAG, "   - Vehículo: " + vehiculoPlaca + " - " + vehiculoModelo);

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("title", "✅ Reserva Confirmada");
        notificationData.put("message", String.format("Tu reserva para %s ha sido confirmada por %s", ruta, conductorNombre));
        notificationData.put("type", "reserva_confirmada");
        notificationData.put("reserva_ruta", ruta);
        notificationData.put("reserva_fecha_hora", fechaHora);
        notificationData.put("reserva_asiento", "A" + asiento);
        notificationData.put("conductor_nombre", conductorNombre);
        notificationData.put("vehiculo_placa", vehiculoPlaca);
        notificationData.put("vehiculo_modelo", vehiculoModelo);
        notificationData.put("target_activity", "passenger_home");
        notificationData.put("userType", "passenger");
        notificationData.put("timestamp", System.currentTimeMillis());
        notificationData.put("read", false);
        notificationData.put("critical", true);

        Log.d(TAG, "📦 notificarReservaConfirmadaAlPasajero - Datos de confirmación preparados");

        guardarNotificacionEnBaseDeDatos(pasajeroId, notificationData, new NotificationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "✅ notificarReservaConfirmadaAlPasajero - Notificación guardada en BD para pasajero: " + pasajeroId);
                enviarNotificacionPushConReintentos(pasajeroId, "✅ Reserva Confirmada",
                        String.format("Tu reserva para %s ha sido confirmada por %s", ruta, conductorNombre),
                        notificationData, 0, callback);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ notificarReservaConfirmadaAlPasajero - Error guardando notificación: " + error);
                if (callback != null) {
                    callback.onError("Error guardando notificación: " + error);
                }
            }
        });
    }

    /**
     * ✅ NOTIFICACIÓN 3: Conductor → Pasajero (Reserva Cancelada)
     */
    public void notificarReservaCanceladaAlPasajero(String pasajeroId, String conductorNombre,
                                                    String ruta, String motivo,
                                                    NotificationCallback callback) {
        Log.d(TAG, "🚀 notificarReservaCanceladaAlPasajero - INICIANDO");
        Log.d(TAG, "📋 DATOS CANCELACIÓN:");
        Log.d(TAG, "   - Pasajero ID: " + pasajeroId);
        Log.d(TAG, "   - Conductor: " + conductorNombre);
        Log.d(TAG, "   - Ruta: " + ruta);
        Log.d(TAG, "   - Motivo: " + motivo);

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("title", "❌ Reserva Cancelada");
        notificationData.put("message", String.format("Tu reserva para %s fue cancelada", ruta));
        notificationData.put("type", "reserva_cancelada");
        notificationData.put("reserva_ruta", ruta);
        notificationData.put("conductor_nombre", conductorNombre);
        notificationData.put("motivo_cancelacion", motivo);
        notificationData.put("target_activity", "passenger_home");
        notificationData.put("userType", "passenger");
        notificationData.put("timestamp", System.currentTimeMillis());
        notificationData.put("read", false);
        notificationData.put("critical", true);

        Log.d(TAG, "📦 notificarReservaCanceladaAlPasajero - Datos de cancelación preparados");

        guardarNotificacionEnBaseDeDatos(pasajeroId, notificationData, new NotificationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "✅ notificarReservaCanceladaAlPasajero - Notificación guardada en BD para pasajero: " + pasajeroId);
                enviarNotificacionPushConReintentos(pasajeroId, "❌ Reserva Cancelada",
                        String.format("Tu reserva para %s fue cancelada", ruta),
                        notificationData, 0, callback);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ notificarReservaCanceladaAlPasajero - Error guardando notificación: " + error);
                if (callback != null) {
                    callback.onError("Error guardando notificación: " + error);
                }
            }
        });
    }

    /**
     * ✅ ENVÍO CON REINTENTOS AUTOMÁTICOS
     */
    private void enviarNotificacionPushConReintentos(String userId, String title, String body,
                                                     Map<String, Object> data, int retryCount,
                                                     NotificationCallback callback) {
        Log.d(TAG, "🔄 enviarNotificacionPushConReintentos - Intento " + (retryCount + 1) + "/" + MAX_RETRIES);
        Log.d(TAG, "📋 DETALLES ENVÍO:");
        Log.d(TAG, "   - User ID: " + userId);
        Log.d(TAG, "   - Título: " + title);
        Log.d(TAG, "   - Mensaje: " + body);

        if (!isNetworkAvailable()) {
            Log.w(TAG, "📵 enviarNotificacionPushConReintentos - Sin conexión a Internet");

            if (retryCount < MAX_RETRIES) {
                Log.d(TAG, "⏰ enviarNotificacionPushConReintentos - Reintentando en " + RETRY_DELAY_MS + "ms");
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Log.d(TAG, "🔄 enviarNotificacionPushConReintentos - Ejecutando reintento " + (retryCount + 1));
                    enviarNotificacionPushConReintentos(userId, title, body, data, retryCount + 1, callback);
                }, RETRY_DELAY_MS);
            } else {
                Log.e(TAG, "❌ enviarNotificacionPushConReintentos - Sin conexión después de " + MAX_RETRIES + " intentos");
                if (callback != null) {
                    callback.onError("Sin conexión después de " + MAX_RETRIES + " intentos");
                }
            }
            return;
        }

        Log.d(TAG, "✅ enviarNotificacionPushConReintentos - Conexión a Internet disponible");

        getUserToken(userId, new OnTokenReceivedListener() {
            @Override
            public void onTokenReceived(String token) {
                Log.d(TAG, "✅ enviarNotificacionPushConReintentos - Token FCM obtenido: " + (token != null ? token.substring(0, 20) + "..." : "null"));
                Log.d(TAG, "📤 enviarNotificacionPushConReintentos - Enviando notificación PUSH (intento " + (retryCount + 1) + ")");

                enviarMensajeFCM(token, title, body, data, new NotificationCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "🎉 enviarNotificacionPushConReintentos - Notificación PUSH enviada exitosamente");
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ enviarNotificacionPushConReintentos - Error enviando PUSH: " + error);

                        if (retryCount < MAX_RETRIES - 1) {
                            Log.d(TAG, "⏰ enviarNotificacionPushConReintentos - Reintentando en " + RETRY_DELAY_MS + "ms");
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                Log.d(TAG, "🔄 enviarNotificacionPushConReintentos - Ejecutando reintento automático");
                                enviarNotificacionPushConReintentos(userId, title, body, data, retryCount + 1, callback);
                            }, RETRY_DELAY_MS);
                        } else {
                            Log.e(TAG, "💥 enviarNotificacionPushConReintentos - Falló después de " + MAX_RETRIES + " intentos: " + error);
                            if (callback != null) {
                                callback.onError("Falló después de " + MAX_RETRIES + " intentos: " + error);
                            }
                        }
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                Log.e(TAG, "❌ enviarNotificacionPushConReintentos - Error obteniendo token: " + exception.getMessage());

                if (retryCount < MAX_RETRIES - 1) {
                    Log.d(TAG, "⏰ enviarNotificacionPushConReintentos - Reintentando obtener token en " + RETRY_DELAY_MS + "ms");
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        Log.d(TAG, "🔄 enviarNotificacionPushConReintentos - Reintentando obtener token");
                        enviarNotificacionPushConReintentos(userId, title, body, data, retryCount + 1, callback);
                    }, RETRY_DELAY_MS);
                } else {
                    Log.e(TAG, "💥 enviarNotificacionPushConReintentos - No se pudo obtener token después de " + MAX_RETRIES + " intentos");
                    if (callback != null) {
                        callback.onError("No se pudo obtener token después de " + MAX_RETRIES + " intentos");
                    }
                }
            }
        });
    }

    /**
     * ✅ VERIFICACIÓN ROBUSTA DE CONEXIÓN
     */
    private boolean isNetworkAvailable() {
        try {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            boolean isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
            Log.d(TAG, "🌐 isNetworkAvailable - Estado conexión: " + (isConnected ? "CONECTADO" : "SIN CONEXIÓN"));
            return isConnected;
        } catch (Exception e) {
            Log.e(TAG, "❌ isNetworkAvailable - Error verificando conexión: " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ MÉTODO FCM MEJORADO CON CALLBACK
     */
    private void enviarMensajeFCM(String token, String title, String body,
                                  Map<String, Object> data, NotificationCallback callback) {
        Log.d(TAG, "📨 enviarMensajeFCM - Preparando mensaje FCM");
        Log.d(TAG, "📋 DETALLES FCM:");
        Log.d(TAG, "   - Token: " + (token != null ? token.substring(0, 20) + "..." : "null"));
        Log.d(TAG, "   - Título: " + title);
        Log.d(TAG, "   - Body: " + body);

        try {
            JSONObject message = new JSONObject();
            message.put("to", token);

            // Configurar notificación
            JSONObject notification = new JSONObject();
            notification.put("title", title);
            notification.put("body", body);
            notification.put("sound", "default");
            notification.put("click_action", "OPEN_NOTIFICATION");
            message.put("notification", notification);

            // Configurar datos
            JSONObject dataJson = new JSONObject();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getValue() != null) {
                    dataJson.put(entry.getKey(), entry.getValue().toString());
                }
            }
            message.put("data", dataJson);

            // Prioridad ALTA para notificaciones críticas
            JSONObject android = new JSONObject();
            JSONObject priority = new JSONObject();
            priority.put("priority", "high");
            android.put("priority", priority);
            message.put("android", android);

            Log.d(TAG, "📦 enviarMensajeFCM - Mensaje JSON construido: " + message.toString().length() + " caracteres");

            RequestBody requestBody = RequestBody.create(message.toString(), JSON);
            Request request = new Request.Builder()
                    .url(FCM_API_URL)
                    .post(requestBody)
                    .addHeader("Authorization", "key=" + FCM_SERVER_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            Log.d(TAG, "🚀 enviarMensajeFCM - Enviando solicitud HTTP a FCM");

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "❌ enviarMensajeFCM - Falla en solicitud HTTP: " + e.getMessage());
                    if (callback != null) {
                        callback.onError("FCM failure: " + e.getMessage());
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "✅ enviarMensajeFCM - Respuesta FCM exitosa - Código: " + response.code());
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    } else {
                        String errorBody = response.body().string();
                        Log.e(TAG, "❌ enviarMensajeFCM - Error FCM - Código: " + response.code() + " - Body: " + errorBody);
                        if (callback != null) {
                            callback.onError("FCM error: " + response.code() + " - " + errorBody);
                        }
                    }
                    response.close();
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "❌ enviarMensajeFCM - Error creando JSON: " + e.getMessage());
            if (callback != null) {
                callback.onError("JSON error: " + e.getMessage());
            }
        }
    }

    /**
     * ✅ GUARDADO EN FIRESTORE CON CALLBACK
     */
    private void guardarNotificacionEnBaseDeDatos(String userId, Map<String, Object> notificationData, NotificationCallback callback) {
        Log.d(TAG, "💾 guardarNotificacionEnBaseDeDatos - Guardando en Firestore para usuario: " + userId);

        String notificationId = db.collection("users")
                .document(userId)
                .collection("notifications")
                .document().getId();

        notificationData.put("id", notificationId);
        notificationData.put("deliveryStatus", "pending");

        Log.d(TAG, "📋 guardarNotificacionEnBaseDeDatos - ID de notificación: " + notificationId);

        db.collection("users")
                .document(userId)
                .collection("notifications")
                .document(notificationId)
                .set(notificationData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ guardarNotificacionEnBaseDeDatos - Notificación guardada en Firestore exitosamente");
                    Log.d(TAG, "   - Usuario: " + userId);
                    Log.d(TAG, "   - ID Notificación: " + notificationId);
                    Log.d(TAG, "   - Tipo: " + notificationData.get("type"));
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ guardarNotificacionEnBaseDeDatos - Error guardando en Firestore: " + e.getMessage());
                    Log.e(TAG, "   - Usuario: " + userId);
                    Log.e(TAG, "   - Error: " + e.getMessage());
                    if (callback != null) {
                        callback.onError("Firestore error: " + e.getMessage());
                    }
                });
    }

    /**
     * ✅ MÉTODO CORREGIDO: OBTENER TOKEN FCM DE USUARIO DESDE REALTIME DATABASE
     */
    /**
     * ✅ MÉTODO CORREGIDO: OBTENER TOKEN FCM DE USUARIO DESDE REALTIME DATABASE
     */
    public void getUserToken(String userId, OnTokenReceivedListener listener) {
        Log.d(TAG, "🔑 getUserToken - Solicitando token FCM para usuario: " + userId);

        // ✅ PRIMERO buscar en "usuarios" (Realtime Database)
        realtimeDb.child("usuarios").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // ✅ CORREGIDO: Cambiar "fcmToken" por "tokenFCM"
                    String token = dataSnapshot.child("tokenFCM").getValue(String.class);
                    if (token != null && !token.isEmpty()) {
                        Log.d(TAG, "✅ getUserToken - Token FCM encontrado en 'usuarios': " + token.substring(0, 20) + "...");
                        listener.onTokenReceived(token);
                    } else {
                        Log.w(TAG, "⚠️ getUserToken - Token FCM no encontrado en 'usuarios', buscando en 'conductores'...");
                        // ✅ SI NO ESTÁ EN USUARIOS, BUSCAR EN CONDUCTORES
                        buscarTokenEnConductores(userId, listener);
                    }
                } else {
                    Log.w(TAG, "⚠️ getUserToken - Usuario no encontrado en 'usuarios', buscando en 'conductores'...");
                    // ✅ SI NO EXISTE EN USUARIOS, BUSCAR EN CONDUCTORES
                    buscarTokenEnConductores(userId, listener);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "❌ getUserToken - Error buscando en 'usuarios': " + databaseError.getMessage());
                // ✅ EN CASO DE ERROR, BUSCAR EN CONDUCTORES
                buscarTokenEnConductores(userId, listener);
            }
        });
    }

    /**
     * ✅ MÉTODO CORREGIDO: BUSCAR TOKEN EN CONDUCTORES
     */
    private void buscarTokenEnConductores(String userId, OnTokenReceivedListener listener) {
        Log.d(TAG, "🔍 buscarTokenEnConductores - Buscando token en 'conductores' para: " + userId);

        realtimeDb.child("conductores").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // ✅ CORREGIDO: Cambiar "fcmToken" por "tokenFCM"
                    String token = dataSnapshot.child("tokenFCM").getValue(String.class);
                    if (token != null && !token.isEmpty()) {
                        Log.d(TAG, "✅ buscarTokenEnConductores - Token FCM encontrado en 'conductores': " + token.substring(0, 20) + "...");
                        listener.onTokenReceived(token);
                    } else {
                        Log.w(TAG, "⚠️ buscarTokenEnConductores - Token FCM no encontrado en 'conductores' para: " + userId);
                        listener.onError(new Exception("Token FCM no encontrado para el usuario: " + userId));
                    }
                } else {
                    Log.e(TAG, "❌ buscarTokenEnConductores - Usuario no encontrado en 'conductores': " + userId);
                    listener.onError(new Exception("Usuario no encontrado: " + userId));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "❌ buscarTokenEnConductores - Error buscando en 'conductores': " + databaseError.getMessage());
                listener.onError(new Exception("Error accediendo a la base de datos: " + databaseError.getMessage()));
            }
        });
    }

    /**
     * ✅ MÉTODO CORREGIDO: GUARDAR TOKEN FCM DEL USUARIO ACTUAL
     */
    public void saveFCMTokenToFirestore(String userId) {
        Log.d(TAG, "💾 saveFCMTokenToFirestore - Guardando token FCM para usuario: " + userId);

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();
                        Log.d(TAG, "✅ saveFCMTokenToFirestore - Token FCM generado: " + token.substring(0, 20) + "...");

                        // ✅ CORREGIDO: Usar "tokenFCM" en lugar de "fcmToken"

                        // 1. Guardar en "usuarios" (Realtime Database)
                        realtimeDb.child("usuarios").child(userId).child("tokenFCM").setValue(token)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "✅ saveFCMTokenToFirestore - Token guardado en 'usuarios' para: " + userId);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "❌ saveFCMTokenToFirestore - Error guardando token en 'usuarios': " + e.getMessage());
                                });

                        // 2. Intentar guardar en "conductores" también (por si es conductor)
                        realtimeDb.child("conductores").child(userId).child("tokenFCM").setValue(token)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "✅ saveFCMTokenToFirestore - Token guardado en 'conductores' para: " + userId);
                                })
                                .addOnFailureListener(e -> {
                                    // Esto es normal si el usuario no es conductor
                                    Log.d(TAG, "ℹ️ saveFCMTokenToFirestore - Usuario no es conductor o error guardando: " + e.getMessage());
                                });

                    } else {
                        Log.e(TAG, "❌ saveFCMTokenToFirestore - Error generando token FCM: " +
                                (task.getException() != null ? task.getException().getMessage() : "Error desconocido"));
                    }
                });
    }

    /**
     * ✅ MARCAR NOTIFICACIÓN COMO LEÍDA
     */
    public void markNotificationAsRead(String userId, String notificationId) {
        Log.d(TAG, "📖 markNotificationAsRead - Marcando como leída - Usuario: " + userId + ", Notificación: " + notificationId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("read", true);
        updates.put("readAt", System.currentTimeMillis());

        db.collection("users")
                .document(userId)
                .collection("notifications")
                .document(notificationId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ markNotificationAsRead - Notificación marcada como leída exitosamente");
                    Log.d(TAG, "   - Usuario: " + userId);
                    Log.d(TAG, "   - Notificación: " + notificationId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ markNotificationAsRead - Error marcando notificación como leída: " + e.getMessage());
                    Log.e(TAG, "   - Usuario: " + userId);
                    Log.e(TAG, "   - Notificación: " + notificationId);
                });
    }

    // Interfaces de callback
    public interface OnTokenReceivedListener {
        void onTokenReceived(String token);
        void onError(Exception exception);
    }

    public interface NotificationCallback {
        void onSuccess();
        void onError(String error);
    }
}