package com.example.healthconnectwearapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.healthconnectwearapp.MainActivity
import com.example.healthconnectwearapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class StepGeneratorService : Service() {
    private val binder = LocalBinder()
    private var totalSteps = 0
    private var stepGenerationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    
    private val quotes = listOf(
        "Every step counts!",
        "Keep going, you're doing great!",
        "Small steps lead to big achievements!",
        "You're making progress!",
        "Stay motivated!"
    )

    inner class LocalBinder : Binder() {
        fun getService(): StepGeneratorService = this@StepGeneratorService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        startStepGeneration()
    }

    private fun startStepGeneration() {
        stepGenerationJob = scope.launch {
            while (true) {
                // Generate 1-5 steps every second
                val newSteps = Random.nextInt(1, 10)
                totalSteps += newSteps
                delay(200) // Wait for 1 second
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Step Tracker",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): android.app.Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Step Tracker")
            .setContentText("Current steps: $totalSteps")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun getCurrentSteps(): Int = totalSteps

    fun getCaloriesBurned(): Double = totalSteps * 0.04

    fun getPoints(): Int = totalSteps / 100

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        stepGenerationJob?.cancel()
    }

    companion object {
        private const val CHANNEL_ID = "StepTrackerChannel"
        private const val NOTIFICATION_ID = 1
    }
} 