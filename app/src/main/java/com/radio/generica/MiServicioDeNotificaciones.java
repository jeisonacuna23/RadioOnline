package com.radio.generica;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MiServicioDeNotificaciones extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "radio_en_vivo_channel";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "A Voz en Cuello";
        String body = "";

        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) title = remoteMessage.getNotification().getTitle();
            if (remoteMessage.getNotification().getBody() != null) body = remoteMessage.getNotification().getBody();
        } else if (!remoteMessage.getData().isEmpty()) {
            if (remoteMessage.getData().containsKey("title")) title = remoteMessage.getData().get("title");
            if (remoteMessage.getData().containsKey("body")) body = remoteMessage.getData().get("body");
        }

        // Verificamos si es el mensaje de "transmisión finalizada"
        if (body.contains("La transmisión ha finalizado") || title.contains("Fuera del aire")) {
            // Enviamos una orden al reproductor para que se detenga
            Intent stopIntent = new Intent(this, PlaybackService.class);
            stopIntent.setAction("STOP_PLAYBACK");
            startService(stopIntent);
        }

        // Mostramos la notificación normal
        mostrarNotificacion(title, body);
    }

    private void mostrarNotificacion(String title, String body) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Avisos de Transmisión",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle(title).setContentText(body));

        notificationManager.notify(100, builder.build());
    }
}
