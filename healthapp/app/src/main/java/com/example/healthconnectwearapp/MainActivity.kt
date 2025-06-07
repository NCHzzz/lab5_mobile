package com.example.healthconnectwearapp

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.healthconnectwearapp.data.Achievement
import com.example.healthconnectwearapp.service.StepGeneratorService
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    private lateinit var statusText: TextView
    private lateinit var generateFakeDataButton: Button
    private lateinit var stepsProgressBar: ProgressBar
    private lateinit var stepsText: TextView
    private lateinit var caloriesText: TextView
    private lateinit var achievementsText: TextView
    private var stepGeneratorService: StepGeneratorService? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isServiceBound = false
    private val dataClient by lazy { Wearable.getDataClient(this) }
    private val scope = CoroutineScope(Dispatchers.IO)
    private var lastAchievementSent = 0

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        arrayOf(
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.FOREGROUND_SERVICE_HEALTH,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACTIVITY_RECOGNITION
        )
    } else {
        arrayOf(
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.ACTIVITY_RECOGNITION
        )
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startStepGeneratorService()
        } else {
            statusText.text = "Permissions required to track steps"
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as StepGeneratorService.LocalBinder
            stepGeneratorService = binder.getService()
            isServiceBound = true
            startStepUpdates()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            stepGeneratorService = null
            isServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.status_text)
        generateFakeDataButton = findViewById(R.id.generate_fake_data_button)
        stepsProgressBar = findViewById(R.id.steps_progress_bar)
        stepsText = findViewById(R.id.steps_text)
        caloriesText = findViewById(R.id.calories_text)
        achievementsText = findViewById(R.id.achievements_text)

        generateFakeDataButton.setOnClickListener {
            if (checkAndRequestPermissions()) {
                startStepGeneratorService()
            }
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        return if (missingPermissions.isEmpty()) {
            true
        } else {
            permissionLauncher.launch(missingPermissions.toTypedArray())
            false
        }
    }

    private fun startStepGeneratorService() {
        val serviceIntent = Intent(this, StepGeneratorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun startStepUpdates() {
        handler.post(object : Runnable {
            override fun run() {
                stepGeneratorService?.let { service ->
                    val steps = service.getCurrentSteps()
                    val calories = service.getCaloriesBurned()
                    
                    stepsText.text = "Steps: $steps"
                    caloriesText.text = "Calories: %.1f".format(calories)
                    
                    // Update achievements
                    val achievements = listOf(
                        Achievement("First Steps", 100, steps >= 100),
                        Achievement("Getting Started", 500, steps >= 500),
                        Achievement("Halfway There", 1000, steps >= 1000),
                        Achievement("Step Master", 2000, steps >= 2000)
                    )
                    
                    val achievementsList = achievements
                        .filter { it.isCompleted }
                        .joinToString("\n") { "🏆 ${it.name}" }
                    
                    achievementsText.text = if (achievementsList.isNotEmpty()) {
                        "Achievements:\n$achievementsList"
                    } else {
                        "No achievements yet"
                    }
                    
                    // Update progress bar (0-2000 steps)
                    stepsProgressBar.progress = (steps * 100 / 2000).coerceAtMost(100)

                    // Send data to watch
                    sendDataToWatch(steps, calories)

                    // Check for new achievements
                    val newAchievements = achievements.filter { it.isCompleted && it.stepsRequired > lastAchievementSent }
                    if (newAchievements.isNotEmpty()) {
                        lastAchievementSent = newAchievements.maxOf { it.stepsRequired }
                        sendAchievementToWatch(newAchievements.last())
                    }
                }
                handler.postDelayed(this, 1000) // Update every second
            }
        })
    }

    private fun sendDataToWatch(steps: Int, calories: Double) {
        scope.launch {
            try {
                val request = PutDataMapRequest.create("/health_data").apply {
                    dataMap.putLong("steps", steps.toLong())
                    dataMap.putDouble("calories", calories)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }.asPutDataRequest()
                
                dataClient.putDataItem(request).await()
                Log.d(TAG, "Data sent to watch - steps: $steps, calories: $calories")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending data to watch", e)
            }
        }
    }

    private fun sendAchievementToWatch(achievement: Achievement) {
        scope.launch {
            try {
                val request = PutDataMapRequest.create("/achievement").apply {
                    dataMap.putString("name", achievement.name)
                    dataMap.putInt("steps_required", achievement.stepsRequired)
                }.asPutDataRequest()
                
                dataClient.putDataItem(request).await()
                Log.d(TAG, "Achievement sent to watch: ${achievement.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending achievement to watch", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val TAG = "HealthConnectWearApp"
    }
}