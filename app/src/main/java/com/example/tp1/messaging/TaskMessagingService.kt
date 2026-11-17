package com.example.tp1.messaging

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.tp1.MainActivity
import com.example.tp1.data.repository.AuthRepository
import com.example.tp1.data.repository.RetentionRepository
import com.example.tp1.util.AnalyticsLogger
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Recibe las notificaciones push (Firebase Cloud Messaging). Con la app en
 * segundo plano las muestra el sistema; en primer plano se muestran desde acá.
 */
class TaskMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        val userId = AuthRepository().currentUserId() ?: return
        RetentionRepository().saveToken(userId, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "Mis Tareas Académicas"
        val body = message.notification?.body ?: message.data["body"] ?: return

        AnalyticsLogger(this).logPushReceived(message.data["campaign"] ?: "sin_campania")
        showNotification(this, title, body)
    }

    companion object {
        const val CHANNEL_ID = "retencion"

        fun showNotification(context: Context, title: String, body: String) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Recordatorios y novedades",
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return

            val intent = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pending = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pending)
                .build()
            manager.notify(System.currentTimeMillis().toInt(), notification)
        }
    }
}
