package com.chopcode.trasnportenataga_laplata.managers;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
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
    private final OkHttpClient httpClient;

    // ✅ NUEVO: Constantes para FCM
    private static final String FCM_API_URL = "https://fcm.googleapis.com/fcm/send";
    private static final String FCM_SERVER_KEY = "BAq-S8bthzR18EdgK6lzrZhdSxMqaJhON_EZ-FkbfK9LGjQRl6oJMTdSc87RfE0uKQqBJYkZWK0RWzGoxfL5l6I"; // Reemplaza con tu Server Key de Firebase
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private NotificationManager() {
        db = FirebaseFirestore.getInstance();
        httpClient = new OkHttpClient(); // ✅ NUEVO: Cliente HTTP para enviar a FCM
    }

    public static synchronized NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }

    /**
     * ✅ NOTIFICACIÓN 1: Pasajero → Conductor
     * Cuando el pasajero confirma una reserva, se notifica al conductor
     */
    public void notificarNuevaReservaAlConductor(String conductorId, String pasajeroNombre,
                                                 String ruta, String fechaHora, int asiento,
                                                 double precio, String metodoPago) {
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

        // 1. Guardar en Firestore (para mostrar dentro de la app)
        guardarNotificacionEnBaseDeDatos(conductorId, notificationData);

        // 2. ✅ NUEVO: Enviar notificación PUSH a la barra de estado
        enviarNotificacionPush(conductorId, "🚗 Nueva Reserva",
                String.format("%s reservó asiento A%d para %s", pasajeroNombre, asiento, ruta),
                notificationData);

        Log.d(TAG, "📲 Notificación de NUEVA RESERVA enviada al conductor: " + conductorId);
    }

    /**
     * ✅ NOTIFICACIÓN 2: Conductor → Pasajero
     * Cuando el conductor cambia el estado de la reserva a "Confirmada", se notifica al pasajero
     */
    public void notificarReservaConfirmadaAlPasajero(String pasajeroId, String conductorNombre,
                                                     String ruta, String fechaHora, int asiento,
                                                     String vehiculoPlaca, String vehiculoModelo) {
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

        // 1. Guardar en Firestore
        guardarNotificacionEnBaseDeDatos(pasajeroId, notificationData);

        // 2. ✅ NUEVO: Enviar notificación PUSH a la barra de estado
        enviarNotificacionPush(pasajeroId, "✅ Reserva Confirmada",
                String.format("Tu reserva para %s ha sido confirmada por %s", ruta, conductorNombre),
                notificationData);

        Log.d(TAG, "📲 Notificación de RESERVA CONFIRMADA enviada al pasajero: " + pasajeroId);
    }

    /**
     * ✅ NOTIFICACIÓN 3: Conductor → Pasajero (OPCIONAL)
     * Cuando el conductor cancela la reserva, se notifica al pasajero
     */
    public void notificarReservaCanceladaAlPasajero(String pasajeroId, String conductorNombre,
                                                    String ruta, String motivo) {
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

        // 1. Guardar en Firestore
        guardarNotificacionEnBaseDeDatos(pasajeroId, notificationData);

        // 2. ✅ NUEVO: Enviar notificación PUSH a la barra de estado
        enviarNotificacionPush(pasajeroId, "❌ Reserva Cancelada",
                String.format("Tu reserva para %s fue cancelada", ruta),
                notificationData);

        Log.d(TAG, "📲 Notificación de RESERVA CANCELADA enviada al pasajero: " + pasajeroId);
    }

    /**
     * ✅ NUEVO MÉTODO: Enviar notificación push a la barra de estado
     */
    private void enviarNotificacionPush(String userId, String title, String body, Map<String, Object> data) {
        // Primero obtener el token FCM del usuario destino
        getUserToken(userId, new OnTokenReceivedListener() {
            @Override
            public void onTokenReceived(String token) {
                Log.d(TAG, "📤 Enviando notificación PUSH a: " + userId);
                Log.d(TAG, "   - Token: " + token);
                Log.d(TAG, "   - Título: " + title);
                Log.d(TAG, "   - Mensaje: " + body);

                // Enviar mensaje FCM
                enviarMensajeFCM(token, title, body, data);
            }

            @Override
            public void onError(Exception exception) {
                Log.e(TAG, "❌ No se pudo obtener token para notificación PUSH: " + exception.getMessage());
            }
        });
    }

    /**
     * ✅ NUEVO MÉTODO: Enviar mensaje FCM usando HTTP request
     */
    private void enviarMensajeFCM(String token, String title, String body, Map<String, Object> data) {
        try {
            // Crear el cuerpo del mensaje JSON para FCM
            JSONObject message = new JSONObject();
            message.put("to", token);

            // Configurar notificación (aparece en barra de estado)
            JSONObject notification = new JSONObject();
            notification.put("title", title);
            notification.put("body", body);
            notification.put("sound", "default");
            notification.put("click_action", "OPEN_NOTIFICATION");
            message.put("notification", notification);

            // Configurar datos adicionales (para cuando la app está en primer plano)
            JSONObject dataJson = new JSONObject();
            dataJson.put("title", title);
            dataJson.put("message", body);
            dataJson.put("type", data.get("type"));
            dataJson.put("timestamp", System.currentTimeMillis());

            // Agregar datos específicos de la reserva
            if (data.containsKey("reserva_ruta")) {
                dataJson.put("reserva_ruta", data.get("reserva_ruta"));
            }
            if (data.containsKey("reserva_asiento")) {
                dataJson.put("reserva_asiento", data.get("reserva_asiento"));
            }
            if (data.containsKey("conductor_nombre")) {
                dataJson.put("conductor_nombre", data.get("conductor_nombre"));
            }
            if (data.containsKey("reserva_pasajero")) {
                dataJson.put("reserva_pasajero", data.get("reserva_pasajero"));
            }

            message.put("data", dataJson);

            // Configurar prioridad alta para que llegue inmediatamente
            JSONObject android = new JSONObject();
            JSONObject priority = new JSONObject();
            priority.put("priority", "high");
            android.put("priority", priority);
            message.put("android", android);

            // Crear la solicitud HTTP
            RequestBody requestBody = RequestBody.create(message.toString(), JSON);
            Request request = new Request.Builder()
                    .url(FCM_API_URL)
                    .post(requestBody)
                    .addHeader("Authorization", "key=" + FCM_SERVER_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            // Enviar la solicitud asíncronamente
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "❌ Error enviando notificación FCM: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "✅ Notificación PUSH enviada exitosamente");
                    } else {
                        Log.e(TAG, "❌ Error en respuesta FCM: " + response.code() + " - " + response.body().string());
                    }
                    response.close();
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "❌ Error creando JSON para FCM: " + e.getMessage());
        }
    }

    /**
     * Guarda la notificación en Firestore
     */
    private void guardarNotificacionEnBaseDeDatos(String userId, Map<String, Object> notificationData) {
        String notificationId = db.collection("users")
                .document(userId)
                .collection("notifications")
                .document().getId();

        notificationData.put("id", notificationId);

        db.collection("users")
                .document(userId)
                .collection("notifications")
                .document(notificationId)
                .set(notificationData)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "✅ Notificación guardada en base de datos para usuario: " + userId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "❌ Error guardando notificación: " + e.getMessage()));
    }

    /**
     * Obtiene el token FCM del usuario actual (para futuras notificaciones push)
     */
    public void getCurrentUserToken(OnTokenReceivedListener listener) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();
                        Log.d(TAG, "FCM Token: " + token);
                        listener.onTokenReceived(token);
                    } else {
                        Log.e(TAG, "Failed to get FCM token");
                        listener.onError(task.getException());
                    }
                });
    }

    /**
     * Obtiene el token FCM de un usuario específico desde Firestore
     */
    public void getUserToken(String userId, OnTokenReceivedListener listener) {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String token = documentSnapshot.getString("fcmToken");
                        if (token != null) {
                            listener.onTokenReceived(token);
                        } else {
                            listener.onError(new Exception("Token FCM no encontrado para el usuario: " + userId));
                        }
                    } else {
                        listener.onError(new Exception("Usuario no encontrado: " + userId));
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    /**
     * Marca una notificación como leída
     */
    public void markNotificationAsRead(String userId, String notificationId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("read", true);
        updates.put("readAt", System.currentTimeMillis());

        db.collection("users")
                .document(userId)
                .collection("notifications")
                .document(notificationId)
                .update(updates)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Notification marked as read"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error marking notification as read: " + e.getMessage()));
    }

    public interface OnTokenReceivedListener {
        void onTokenReceived(String token);
        void onError(Exception exception);
    }
}