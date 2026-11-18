package com.dlanuza.mixxxdroid.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.dlanuza.mixxxdroid.MainActivity

class MusicService : Service() {

    companion object {
        const val CHANNEL_ID = "MixxxDroidPlaybackChannel"
        const val NOTIFICATION_ID = 1001

        // Callback global para detener los reproductores si la app es forzada a cerrarse
        var onStopPlayers: (() -> Unit)? = null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        // Inicia el servicio en primer plano con su notificación obligatoria
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Se dispara cuando el usuario desliza y cierra la app desde "Recientes" (Force Close)
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        onStopPlayers?.invoke() // Detiene la música de los decks
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf() // Mata el servicio
    }

    override fun onDestroy() {
        super.onDestroy()
        onStopPlayers?.invoke()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "MixxxDroid Reproducción"
            val descriptionText = "Mantiene el motor de audio activo en segundo plano"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MixxxDroid DJ")
            .setContentText("Mezclador activo en segundo plano")
            .setSmallIcon(android.R.drawable.ic_media_play) // Puedes cambiarlo por tu propio icono drawable
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}