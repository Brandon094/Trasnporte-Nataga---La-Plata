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
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.chopcode.trasnportenataga_laplata.config.MyApp;

public class NotificationManager {

    private static final String TAG = "NotificationManager";
    private static NotificationManager instance;
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

        // SOLO Realtime Database - ELIMINADO Firestore
        realtimeDb = MyApp.getDatabaseReference("");
        httpClient = new OkHttpClient();
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        Log.d(TAG, "✅ CONSTRUCTOR - NotificationManager inicializado exitosamente (SOLO RTDB)");
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
        Log.d(TAG, "🚀 notificarNuevaReservaAlConductor - INICIANDO para conductor: " + conductorId);

        // ✅ AGREGADO: Registrar Analytics event
        try {
            Map<String, Object> analyticsData = new HashMap<>();
            analyticsData.put("conductorId", conductorId);
            analyticsData.put("asiento", asiento);
            analyticsData.put("ruta", ruta);
            MyApp.logEvent("notificacion_nueva_reserva_enviada", analyticsData);
        } catch (Exception e) {
            Log.w(TAG, "⚠️ Error registrando analytics: " + e.getMessage());
        }

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

        // ✅ CAMBIADO: Usar nuevo método de nodo separado
        guardarNotificacionEnNodoSeparado(conductorId, notificationData, new NotificationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "✅ notificarNuevaReservaAlConductor - Notificación guardada en nodo separado para conductor: " + conductorId);

                // 2. Intentar enviar notificación PUSH con reintentos
                enviarNotificacionPushConReintentos(conductorId, "🚗 Nueva Reserva",
                        String.format("%s reservó asiento A%d para %s", pasajeroNombre, asiento, ruta),
                        notificationData, 0, callback);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ notificarNuevaReservaAlConductor - Error guardando notificación: " + error);
                logErrorToCrashlytics(new Exception("Error notificarNuevaReservaAlConductor: " + error));
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
        Log.d(TAG, "🚀 notificarReservaConfirmadaAlPasajero - INICIANDO para pasajero: " + pasajeroId);

        try {
            Map<String, Object> analyticsData = new HashMap<>();
            analyticsData.put("pasajeroId", pasajeroId);
            analyticsData.put("conductorNombre", conductorNombre);
            analyticsData.put("ruta", ruta);
            MyApp.logEvent("notificacion_reserva_confirmada_enviada", analyticsData);
        } catch (Exception e) {
            Log.w(TAG, "⚠️ Error registrando analytics: " + e.getMessage());
        }

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

        // ✅ CAMBIADO: Usar nuevo método de nodo separado
        guardarNotificacionEnNodoSeparado(pasajeroId, notificationData, new NotificationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "✅ notificarReservaConfirmadaAlPasajero - Notificación guardada en nodo separado para pasajero: " + pasajeroId);
                enviarNotificacionPushConReintentos(pasajeroId, "✅ Reserva Confirmada",
                        String.format("Tu reserva para %s ha sido confirmada por %s", ruta, conductorNombre),
                        notificationData, 0, callback);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ notificarReservaConfirmadaAlPasajero - Error guardando notificación: " + error);
                logErrorToCrashlytics(new Exception("Error notificarReservaConfirmadaAlPasajero: " + error));
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
        Log.d(TAG, "🚀 notificarReservaCanceladaAlPasajero - INICIANDO para pasajero: " + pasajeroId);

        try {
            Map<String, Object> analyticsData = new HashMap<>();
            analyticsData.put("pasajeroId", pasajeroId);
            analyticsData.put("conductorNombre", conductorNombre);
            analyticsData.put("motivo", motivo);
            MyApp.logEvent("notificacion_reserva_cancelada_enviada", analyticsData);
        } catch (Exception e) {
            Log.w(TAG, "⚠️ Error registrando analytics: " + e.getMessage());
        }

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

        // ✅ CAMBIADO: Usar nuevo método de nodo separado
        guardarNotificacionEnNodoSeparado(pasajeroId, notificationData, new NotificationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "✅ notificarReservaCanceladaAlPasajero - Notificación guardada en nodo separado para pasajero: " + pasajeroId);
                enviarNotificacionPushConReintentos(pasajeroId, "❌ Reserva Cancelada",
                        String.format("Tu reserva para %s fue cancelada", ruta),
                        notificationData, 0, callback);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ notificarReservaCanceladaAlPasajero - Error guardando notificación: " + error);
                logErrorToCrashlytics(new Exception("Error notificarReservaCanceladaAlPasajero: " + error));
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
        Log.d(TAG, "🔄 enviarNotificacionPushConReintentos - Intento " + (retryCount + 1) + "/" + MAX_RETRIES + " para userId: " + userId);

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
                logErrorToCrashlytics(new Exception("Sin conexión después de " + MAX_RETRIES + " intentos - UserId: " + userId));
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
                        Log.d(TAG, "🎉 enviarNotificacionPushConReintentos - Notificación PUSH enviada exitosamente a userId: " + userId);
                        try {
                            Map<String, Object> analyticsData = new HashMap<>();
                            analyticsData.put("userId", userId);
                            analyticsData.put("title", title);
                            analyticsData.put("retryCount", retryCount);
                            MyApp.logEvent("notificacion_push_enviada_exitosamente", analyticsData);
                        } catch (Exception e) {
                            Log.w(TAG, "⚠️ Error registrando analytics de éxito: " + e.getMessage());
                        }

                        if (callback != null) {
                            callback.onSuccess();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ enviarNotificacionPushConReintentos - Error enviando PUSH: " + error);
                        logErrorToCrashlytics(new Exception("Error enviando PUSH - Intento " + retryCount + ": " + error));

                        if (retryCount < MAX_RETRIES - 1) {
                            Log.d(TAG, "⏰ enviarNotificacionPushConReintentos - Reintentando en " + RETRY_DELAY_MS + "ms");
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                Log.d(TAG, "🔄 enviarNotificacionPushConReintentos - Ejecutando reintento automático");
                                enviarNotificacionPushConReintentos(userId, title, body, data, retryCount + 1, callback);
                            }, RETRY_DELAY_MS);
                        } else {
                            Log.e(TAG, "💥 enviarNotificacionPushConReintentos - Falló después de " + MAX_RETRIES + " intentos: " + error);
                            logErrorToCrashlytics(new Exception("Fallo crítico después de " + MAX_RETRIES + " intentos: " + error));
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
                logErrorToCrashlytics(exception);

                if (retryCount < MAX_RETRIES - 1) {
                    Log.d(TAG, "⏰ enviarNotificacionPushConReintentos - Reintentando obtener token en " + RETRY_DELAY_MS + "ms");
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        Log.d(TAG, "🔄 enviarNotificacionPushConReintentos - Reintentando obtener token");
                        enviarNotificacionPushConReintentos(userId, title, body, data, retryCount + 1, callback);
                    }, RETRY_DELAY_MS);
                } else {
                    Log.e(TAG, "💥 enviarNotificacionPushConReintentos - No se pudo obtener token después de " + MAX_RETRIES + " intentos");
                    logErrorToCrashlytics(new Exception("No se pudo obtener token después de " + MAX_RETRIES + " intentos"));
                    if (callback != null) {
                        callback.onError("No se pudo obtener token después de " + MAX_RETRIES + " intentos: " + exception.getMessage());
                    }
                }
            }
        });
    }

    /**
     * ✅ VERIFICACIÓN DE CONEXIÓN
     */
    private boolean isNetworkAvailable() {
        try {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            boolean isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
            Log.d(TAG, "🌐 isNetworkAvailable - Estado conexión: " + (isConnected ? "CONECTADO" : "SIN CONEXIÓN"));
            return isConnected;
        } catch (Exception e) {
            Log.e(TAG, "❌ isNetworkAvailable - Error verificando conexión: " + e.getMessage());
            logErrorToCrashlytics(e);
            return false;
        }
    }

    /**
     * ✅ MÉTODO FCM MEJORADO
     */
    private void enviarMensajeFCM(String token, String title, String body,
                                  Map<String, Object> data, NotificationCallback callback) {
        Log.d(TAG, "📨 enviarMensajeFCM - Preparando mensaje FCM para token: " + (token != null ? token.substring(0, 20) + "..." : "null"));

        try {
            if (token == null || token.isEmpty()) {
                Log.e(TAG, "❌ enviarMensajeFCM - Token vacío o nulo");
                if (callback != null) {
                    callback.onError("Token FCM vacío o nulo");
                }
                return;
            }

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
            android.put("priority", "high");
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
                    logErrorToCrashlytics(e);
                    if (callback != null) {
                        callback.onError("FCM failure: " + e.getMessage());
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "✅ enviarMensajeFCM - Respuesta FCM exitosa - Código: " + response.code());
                            if (callback != null) {
                                callback.onSuccess();
                            }
                        } else {
                            String errorBody = response.body() != null ? response.body().string() : "No body";
                            Log.e(TAG, "❌ enviarMensajeFCM - Error FCM - Código: " + response.code() + " - Body: " + errorBody);
                            Exception fcmError = new Exception("FCM Error: " + response.code() + " - " + errorBody);
                            logErrorToCrashlytics(fcmError);
                            if (callback != null) {
                                callback.onError("FCM error: " + response.code() + " - " + errorBody);
                            }
                        }
                    } finally {
                        response.close();
                    }
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "❌ enviarMensajeFCM - Error creando JSON: " + e.getMessage());
            logErrorToCrashlytics(e);
            if (callback != null) {
                callback.onError("JSON error: " + e.getMessage());
            }
        }
    }

    /**
     * ✅ NUEVA IMPLEMENTACIÓN: Guardar notificación en nodo separado "notifications"
     */
    private void guardarNotificacionEnNodoSeparado(String receiverId, Map<String, Object> notificationData,
                                                   NotificationCallback callback) {
        Log.d(TAG, "💾 guardarNotificacionEnNodoSeparado - Creando notificación para: " + receiverId);

        try {
            // Generar ID único para la notificación
            String notificationId = "notif_" + System.currentTimeMillis() + "_" + new Random().nextInt(1000);

            // ✅ DETERMINAR TIPO DE RECEPTOR (conductor o pasajero)
            String userType = (String) notificationData.get("userType");
            String targetActivity = (String) notificationData.get("target_activity");

            String receiverType;
            if ("driver".equals(userType) || "driver_home".equals(targetActivity)) {
                receiverType = "conductor";
            } else {
                receiverType = "pasajero";
            }

            // ✅ CREAR ESTRUCTURA COMPLETA DE NOTIFICACIÓN
            Map<String, Object> notificationComplete = new HashMap<>();
            notificationComplete.put("id", notificationId);
            notificationComplete.put("receiverId", receiverId);
            notificationComplete.put("receiverType", receiverType);

            // Añadir sender si está disponible en notificationData
            if (notificationData.containsKey("senderId")) {
                notificationComplete.put("senderId", notificationData.get("senderId"));
            }
            if (notificationData.containsKey("senderType")) {
                notificationComplete.put("senderType", notificationData.get("senderType"));
            }

            notificationComplete.put("type", notificationData.get("type"));
            notificationComplete.put("title", notificationData.get("title"));
            notificationComplete.put("message", notificationData.get("message"));
            notificationComplete.put("data", notificationData); // Guardar todos los datos originales

            // ✅ ESTADO Y METADATOS
            notificationComplete.put("status", "pending");
            notificationComplete.put("createdAt", System.currentTimeMillis());
            notificationComplete.put("readAt", null);
            notificationComplete.put("respondedAt", null);
            notificationComplete.put("response", null); // "aceptada", "rechazada", "pendiente"
            notificationComplete.put("responseMessage", null);
            notificationComplete.put("deliveryAttempts", 0);
            notificationComplete.put("priority", notificationData.get("critical") != null && (boolean) notificationData.get("critical") ? "high" : "normal");
            notificationComplete.put("deliveryStatus", "pending");

            Log.d(TAG, "📦 guardarNotificacionEnNodoSeparado - Notificación preparada con ID: " + notificationId);
            Log.d(TAG, "   - Receptor: " + receiverId + " (" + receiverType + ")");
            Log.d(TAG, "   - Tipo: " + notificationComplete.get("type"));
            Log.d(TAG, "   - Estado: pending");

            // ✅ GUARDAR EN NODO SEPARADO "notifications"
            DatabaseReference notificationRef =
                    realtimeDb.child("notificaciones").child(notificationId);

            notificationRef.setValue(notificationComplete)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅ guardarNotificacionEnNodoSeparado - Notificación guardada en nodo separado: " + notificationId);

                        if (callback != null) {
                            callback.onSuccess();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ guardarNotificacionEnNodoSeparado - Error guardando notificación: " + e.getMessage());
                        if (callback != null) {
                            callback.onError("Error guardando notificación: " + e.getMessage());
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "❌ guardarNotificacionEnNodoSeparado - Error inesperado: " + e.getMessage());
            logErrorToCrashlytics(e);
            if (callback != null) {
                callback.onError("Error inesperado: " + e.getMessage());
            }
        }
    }

    /**
     * ✅ ACTUALIZAR ESTADO DE NOTIFICACIÓN
     */
    public void actualizarEstadoNotificacion(String notificationId, String newStatus, String response,
                                             String responseMessage, NotificationCallback callback) {
        Log.d(TAG, "📝 actualizarEstadoNotificacion - Actualizando notificación: " + notificationId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);

        if ("responded".equals(newStatus)) {
            updates.put("response", response);
            updates.put("responseMessage", responseMessage);
            updates.put("respondedAt", System.currentTimeMillis());
        } else if ("read".equals(newStatus)) {
            updates.put("readAt", System.currentTimeMillis());
        }

        realtimeDb.child("notifications").child(notificationId)
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ actualizarEstadoNotificacion - Notificación actualizada: " + notificationId);

                    // También actualizar referencia en usuario si existe
                    actualizarReferenciaEnUsuario(notificationId, newStatus, response);

                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ actualizarEstadoNotificacion - Error actualizando: " + e.getMessage());
                    if (callback != null) {
                        callback.onError("Error actualizando notificación: " + e.getMessage());
                    }
                });
    }

    /**
     * ✅ ACTUALIZAR REFERENCIA EN USUARIO
     */
    private void actualizarReferenciaEnUsuario(String notificationId, String newStatus, String response) {
        // Buscar la notificación para obtener receiverId
        realtimeDb.child("notifications").child(notificationId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            Map<String, Object> notification = (Map<String, Object>) dataSnapshot.getValue();
                            String receiverId = (String) notification.get("receiverId");
                            String receiverType = (String) notification.get("receiverType");

                            String userNode = "conductor".equals(receiverType) ? "conductores" : "usuarios";

                            Map<String, Object> updates = new HashMap<>();
                            updates.put("status", newStatus);
                            if ("responded".equals(newStatus)) {
                                updates.put("response", response);
                            }

                            realtimeDb.child(userNode).child(receiverId)
                                    .child("notifications").child(notificationId)
                                    .updateChildren(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "💾 Referencia actualizada en usuario");
                                    })
                                    .addOnFailureListener(e -> {
                                        // No crítico
                                        Log.d(TAG, "ℹ️ No se pudo actualizar referencia en usuario");
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "❌ Error buscando notificación: " + databaseError.getMessage());
                    }
                });
    }

    /**
     * ✅ OBTENER NOTIFICACIONES DE UN USUARIO
     */
    public void obtenerNotificacionesUsuario(String userId, String userType, OnNotificationsReceivedListener listener) {
        Log.d(TAG, "📨 obtenerNotificacionesUsuario - Solicitando notificaciones para: " + userId);

        // Opción 1: Buscar por receiverId en nodo notifications
        realtimeDb.child("notifications")
                .orderByChild("receiverId")
                .equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        List<Map<String, Object>> notifications = new ArrayList<>();

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Map<String, Object> notification = (Map<String, Object>) snapshot.getValue();
                            notifications.add(notification);
                        }

                        Log.d(TAG, "✅ obtenerNotificacionesUsuario - " + notifications.size() + " notificaciones encontradas");
                        listener.onNotificationsReceived(notifications);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "❌ obtenerNotificacionesUsuario - Error: " + databaseError.getMessage());
                        listener.onError(databaseError.getMessage());
                    }
                });
    }

    /**
     * ✅ MÉTODO CORREGIDO: OBTENER TOKEN FCM DE USUARIO DESDE REALTIME DATABASE
     */
    public void getUserToken(String userId, OnTokenReceivedListener listener) {
        Log.d(TAG, "🔑 getUserToken - Solicitando token FCM para usuario: " + userId);

        // ✅ PRIMERO buscar en "usuarios" (Realtime Database)
        realtimeDb.child("usuarios").child(userId).child("tokenFCM")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String token = dataSnapshot.getValue(String.class);
                            if (token != null && !token.isEmpty()) {
                                Log.d(TAG, "✅ getUserToken - Token FCM encontrado en 'usuarios': " + token.substring(0, 20) + "...");
                                listener.onTokenReceived(token);
                            } else {
                                Log.w(TAG, "⚠️ getUserToken - Token FCM no encontrado en 'usuarios', buscando en 'conductores'...");
                                buscarTokenEnConductores(userId, listener);
                            }
                        } else {
                            Log.w(TAG, "⚠️ getUserToken - Token no encontrado en 'usuarios', buscando en 'conductores'...");
                            buscarTokenEnConductores(userId, listener);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "❌ getUserToken - Error buscando en 'usuarios': " + databaseError.getMessage());
                        buscarTokenEnConductores(userId, listener);
                    }
                });
    }

    /**
     * ✅ MÉTODO CORREGIDO: BUSCAR TOKEN EN CONDUCTORES
     */
    private void buscarTokenEnConductores(String userId, OnTokenReceivedListener listener) {
        Log.d(TAG, "🔍 buscarTokenEnConductores - Buscando token en 'conductores' para: " + userId);

        realtimeDb.child("conductores").child(userId).child("tokenFCM")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String token = dataSnapshot.getValue(String.class);
                            if (token != null && !token.isEmpty()) {
                                Log.d(TAG, "✅ buscarTokenEnConductores - Token FCM encontrado en 'conductores': " + token.substring(0, 20) + "...");
                                listener.onTokenReceived(token);
                            } else {
                                Log.w(TAG, "⚠️ buscarTokenEnConductores - Token FCM vacío en 'conductores' para: " + userId);
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
     * ✅ MÉTODO MEJORADO: GUARDAR TOKEN FCM DEL USUARIO ACTUAL
     */
    public void saveFCMTokenToRealtimeDatabase(String userId, String userType) {
        Log.d(TAG, "💾 saveFCMTokenToRealtimeDatabase - Guardando token FCM para usuario: " + userId + ", tipo: " + userType);

        MyApp.getInstance().getFirebaseMessaging().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();
                        Log.d(TAG, "✅ Token FCM generado: " + token.substring(0, 20) + "...");

                        // ✅ Guardar SOLO en el nodo correcto según userType
                        String nodoCorrecto;
                        if ("conductor".equals(userType)) {
                            nodoCorrecto = "conductores";
                            Log.d(TAG, "👨‍✈️ Guardando token para CONDUCTOR");
                        } else {
                            nodoCorrecto = "usuarios";
                            Log.d(TAG, "👤 Guardando token para PASAJERO/USUARIO");
                        }

                        realtimeDb.child(nodoCorrecto).child(userId).child("tokenFCM").setValue(token)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "✅ Token guardado en '" + nodoCorrecto + "' para: " + userId);

                                    // ✅ OPCIONAL: Verificar si existe en el otro nodo y limpiar
                                    String otroNodo = "conductor".equals(userType) ? "usuarios" : "conductores";
                                    realtimeDb.child(otroNodo).child(userId).child("tokenFCM").removeValue()
                                            .addOnSuccessListener(aVoid2 -> {
                                                Log.d(TAG, "✅ Token eliminado del nodo incorrecto '" + otroNodo + "'");
                                            })
                                            .addOnFailureListener(e -> {
                                                // Esto es normal si no existe
                                                Log.d(TAG, "ℹ️ No había token en el nodo incorrecto '" + otroNodo + "'");
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "❌ Error guardando token en '" + nodoCorrecto + "': " + e.getMessage());
                                    logErrorToCrashlytics(e);
                                });

                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Error desconocido";
                        Log.e(TAG, "❌ Error generando token FCM: " + errorMsg);
                        Exception tokenError = new Exception("Error generando token FCM: " + errorMsg);
                        logErrorToCrashlytics(tokenError);
                    }
                });
    }

    /**
     * ✅ CORREGIDO: MARCAR NOTIFICACIÓN COMO LEÍDA (ACTUALIZADO PARA NODO SEPARADO)
     */
    public void markNotificationAsRead(String notificationId, NotificationCallback callback) {
        Log.d(TAG, "📖 markNotificationAsRead - Marcando notificación como leída: " + notificationId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "read");
        updates.put("readAt", System.currentTimeMillis());

        // Actualizar en nodo notifications
        realtimeDb.child("notifications").child(notificationId)
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ markNotificationAsRead - Notificación marcada como leída en nodo principal");

                    // También actualizar referencia en usuario si existe
                    actualizarReferenciaEnUsuario(notificationId, "read", null);

                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ markNotificationAsRead - Error marcando como leída: " + e.getMessage());
                    if (callback != null) {
                        callback.onError("Error marcando como leída: " + e.getMessage());
                    }
                });
    }

    /**
     * ✅ MÉTODO DIAGNÓSTICO: VERIFICAR TOKENS
     */
    public void diagnosticarToken(String userId) {
        Log.d(TAG, "🩺 DIAGNÓSTICO - Verificando tokens para userId: " + userId);

        // Verificar en usuarios
        realtimeDb.child("usuarios").child(userId).child("tokenFCM")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String token = dataSnapshot.getValue(String.class);
                            Log.d(TAG, "✅ DIAGNÓSTICO - Token en 'usuarios': " +
                                    (token != null ? token.substring(0, 20) + "..." : "NULL"));
                        } else {
                            Log.d(TAG, "❌ DIAGNÓSTICO - No existe nodo 'tokenFCM' en 'usuarios'");
                        }

                        // Verificar en conductores
                        realtimeDb.child("conductores").child(userId).child("tokenFCM")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            String token = dataSnapshot.getValue(String.class);
                                            Log.d(TAG, "✅ DIAGNÓSTICO - Token en 'conductores': " +
                                                    (token != null ? token.substring(0, 20) + "..." : "NULL"));
                                        } else {
                                            Log.d(TAG, "❌ DIAGNÓSTICO - No existe nodo 'tokenFCM' en 'conductores'");
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        Log.e(TAG, "❌ DIAGNÓSTICO - Error verificando 'conductores': " + databaseError.getMessage());
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "❌ DIAGNÓSTICO - Error verificando 'usuarios': " + databaseError.getMessage());
                    }
                });
    }

    /**
     * ✅ MÉTODO: Registrar error en Crashlytics usando MyApp
     */
    private void logErrorToCrashlytics(Exception e) {
        try {
            MyApp.logError(e);
            Log.d(TAG, "📊 Error registrado en Crashlytics: " + e.getMessage());
        } catch (Exception crashlyticsError) {
            Log.e(TAG, "❌ Error registrando en Crashlytics: " + crashlyticsError.getMessage());
        }
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

    // ✅ NUEVA INTERFAZ PARA OBTENER NOTIFICACIONES
    public interface OnNotificationsReceivedListener {
        void onNotificationsReceived(List<Map<String, Object>> notifications);
        void onError(String error);
    }
}