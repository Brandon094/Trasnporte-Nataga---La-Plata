package com.chopcode.trasnportenataga_laplata.services.notifications;

import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.chopcode.trasnportenataga_laplata.activities.common.InicioDeSesion;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.chopcode.trasnportenataga_laplata.R;

import java.util.HashMap;
import java.util.Map;

public class NotificationService extends FirebaseMessagingService {

    private static final String TAG = "NotificationService";
    private static final String CHANNEL_ID = "high_priority_channel";
    private static final String CHANNEL_NAME = "Notificaciones Importantes";
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_ID = "user_id";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "🔄 Inicializando NotificationService");
        // Crear el canal inmediatamente
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationManager notificationManager =
                    (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            createNotificationChannel(notificationManager);
        }
    }
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "📨 MENSAJE RECIBIDO DE: " + remoteMessage.getFrom());

        // Check if message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "📊 Datos del mensaje: " + remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "🔔 Notificación - Título: " + remoteMessage.getNotification().getTitle());
            Log.d(TAG, "🔔 Notificación - Cuerpo: " + remoteMessage.getNotification().getBody());
            sendNotification(
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody(),
                    remoteMessage.getData()
            );
        }
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "🔑 NUEVO TOKEN FCM: " + token);
        sendRegistrationToServer(token);
    }

    private void handleDataMessage(Map<String, String> data) {
        try {
            String title = data.get("title");
            String message = data.get("message");
            String type = data.get("type");

            Log.d(TAG, "📝 Procesando datos - Título: " + title);
            Log.d(TAG, "📝 Procesando datos - Mensaje: " + message);
            Log.d(TAG, "📝 Procesando datos - Tipo: " + type);

            // Si no hay título/mensaje en la notificación, usar los datos
            if (title != null && message != null) {
                sendNotification(title, message, data);
            }

            // Handle different notification types
            if (type != null) {
                switch (type) {
                    case "alert":
                        handleAlertNotification(data);
                        break;
                    case "update":
                        handleUpdateNotification(data);
                        break;
                    case "promotion":
                        handlePromotionNotification(data);
                        break;
                    case "reserva_confirmada":
                        handleReservaConfirmada(data);
                        break;
                    case "nueva_reserva":
                        handleNuevaReserva(data);
                        break;
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error handling data message: " + e.getMessage());
        }
    }

    private void handleAlertNotification(Map<String, String> data) {
        Log.d(TAG, "🔄 Processing alert notification");
    }

    private void handleUpdateNotification(Map<String, String> data) {
        Log.d(TAG, "🔄 Processing update notification");
    }

    private void handlePromotionNotification(Map<String, String> data) {
        Log.d(TAG, "🔄 Processing promotion notification");
    }

    private void handleReservaConfirmada(Map<String, String> data) {
        Log.d(TAG, "✅ Processing reserva confirmada notification");
        // Lógica específica para reservas confirmadas
    }

    private void handleNuevaReserva(Map<String, String> data) {
        Log.d(TAG, "🚗 Processing nueva reserva notification");
        // Lógica específica para nuevas reservas
    }

    private void sendNotification(String title, String messageBody, Map<String, String> data) {
        try {
            Log.d(TAG, "🎯 Creando notificación: " + title);

            // Intent para cuando se hace clic en la notificación
            Intent intent = new Intent(this, InicioDeSesion.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            // Add data to intent
            if (data != null) {
                for (Map.Entry<String, String> entry : data.entrySet()) {
                    intent.putExtra(entry.getKey(), entry.getValue());
                }
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            // ✅ CORREGIDO: Usar ícono por defecto de Android si no tienes ic_notification
            int smallIcon = R.drawable.ic_launcher_foreground;
            if (smallIcon == 0) {
                smallIcon = android.R.drawable.ic_dialog_info; // Ícono por defecto
            }

            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this, CHANNEL_ID)
                            .setSmallIcon(smallIcon)
                            .setContentTitle(title != null ? title : getString(R.string.app_name))
                            .setContentText(messageBody)
                            .setAutoCancel(true)
                            .setSound(defaultSoundUri)
                            .setContentIntent(pendingIntent)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setVibrate(new long[]{0, 500, 200, 500});

            android.app.NotificationManager notificationManager =
                    (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            // Create notification channel for Android O and above
            createNotificationChannel(notificationManager);

            // Generate unique ID for each notification
            int notificationId = (int) System.currentTimeMillis();
            notificationManager.notify(notificationId, notificationBuilder.build());

            Log.d(TAG, "✅ Notificación mostrada: " + title);

        } catch (Exception e) {
            Log.e(TAG, "❌ Error creando notificación: " + e.getMessage());
        }
    }

    private void createNotificationChannel(android.app.NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    android.app.NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Channel for Transporte notifications");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "✅ Canal de notificaciones creado");
        }
    }

    private void sendRegistrationToServer(String token) {
        Log.d(TAG, "🔑 Token FCM generado en servicio: " + token);

        // Solo loguear el token - El guardado real se hará desde InicioDeSesion
        String userId = obtenerUserIdActual();

        if (userId != null && !userId.isEmpty() && !userId.equals("current_user_id")) {
            Log.d(TAG, "👤 Token generado para userId: " + userId);
            Log.d(TAG, "💡 Nota: El token se guardará cuando el usuario inicie sesión en InicioDeSesion");
        } else {
            Log.w(TAG, "⚠️ Token generado pero userId no disponible aún. Se guardará al iniciar sesión.");
            Log.d(TAG, "🔑 Token para guardar más tarde: " + token.substring(0, 30) + "...");
        }
    }

    /**
     * ✅ NUEVO MÉTODO: Obtener el ID del usuario actual desde SharedPreferences
     */
    private String obtenerUserIdActual() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String userId = prefs.getString(KEY_USER_ID, null);

            if (userId != null) {
                Log.d(TAG, "👤 UserId obtenido: " + userId);
                return userId;
            } else {
                Log.w(TAG, "⚠️ UserId no encontrado en SharedPreferences");
                return "current_user_id"; // Fallback
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error obteniendo userId: " + e.getMessage());
            return "current_user_id";
        }
    }
}