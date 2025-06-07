package com.example.recap_exercise

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.app.PendingIntent

class ForegroundService : Service() {
    private var currentCount = 0
    private val maxCount = 10

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()
    }

    private fun startForegroundWithNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "channelId")
            .setContentTitle("Service Demo Running")
            .setContentText("Foreground service is performing background task...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setProgress(maxCount, currentCount, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop",
                createStopPendingIntent()
            )
            .build()

        startForeground(1, notification)
        Log.d("ForegroundService", "Started foreground service with notification")
    }

    private fun createStopPendingIntent(): PendingIntent {
        val stopIntent = Intent(this, ForegroundService::class.java).apply {
            action = "STOP_SERVICE"
        }
        return PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            Log.d("ForegroundService", "Stop action received")
            stopSelf()
            return START_NOT_STICKY
        }

        CoroutineScope(Dispatchers.Default).launch {
            for (i in 1..maxCount) {
                currentCount = i
                delay(1000)
                Log.d("ForegroundService", "Count: $i")
                updateNotification(i)
            }
            Log.d("ForegroundService", "Task completed, stopping service")
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun updateNotification(progress: Int) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "channelId")
            .setContentTitle("Service Demo Running")
            .setContentText("Progress: $progress/$maxCount")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setProgress(maxCount, progress, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop",
                createStopPendingIntent()
            )
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ForegroundService", "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}