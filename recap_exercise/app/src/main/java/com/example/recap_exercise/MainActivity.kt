package com.example.recap_exercise

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private var boundService: BoundService? = null
    private var isBound = false
    private var updateJob: Job? = null

    // UI Elements
    private lateinit var tvStatus: TextView
    private lateinit var tvBoundServiceCount: TextView
    private lateinit var statusIndicator: View
    private lateinit var btnBackground: Button
    private lateinit var btnForeground: Button
    private lateinit var btnBound: Button

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as BoundService.LocalBinder
            boundService = localBinder.getService()
            isBound = true
            updateStatus("Connected to Bound Service", StatusType.SUCCESS)
            tvBoundServiceCount.visibility = View.VISIBLE
            btnBound.text = "Unbind Service"
            startBoundServiceUpdates()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            updateStatus("Disconnected from Bound Service", StatusType.IDLE)
            tvBoundServiceCount.visibility = View.GONE
            btnBound.text = "Bind to Service"
            stopBoundServiceUpdates()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupPermissions()
        setupNotificationChannel()
        setupClickListeners()
        updateStatus("Ready to start", StatusType.IDLE)
    }

    private fun initializeViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvBoundServiceCount = findViewById(R.id.tvBoundServiceCount)
        statusIndicator = findViewById(R.id.statusIndicator)
        btnBackground = findViewById(R.id.btnBackground)
        btnForeground = findViewById(R.id.btnForeground)
        btnBound = findViewById(R.id.btnBound)
    }

    private fun setupPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
        checkAndPromptNotificationPermission(this)
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "channelId",
                "Foreground Service",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Channel for foreground service notifications"
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun setupClickListeners() {
        btnBackground.setOnClickListener {
            updateStatus("Starting background task...", StatusType.RUNNING)
            AsyncTaskExample().execute()
        }

        btnForeground.setOnClickListener {
            updateStatus("Starting foreground service...", StatusType.RUNNING)
            val intent = Intent(this, ForegroundService::class.java)
            ContextCompat.startForegroundService(this, intent)

            // Update status after a delay
            Handler(Looper.getMainLooper()).postDelayed({
                updateStatus("Foreground service running", StatusType.SUCCESS)
            }, 1000)
        }

        btnBound.setOnClickListener {
            if (isBound) {
                unbindService(serviceConnection)
                isBound = false
            } else {
                updateStatus("Binding to service...", StatusType.RUNNING)
                val intent = Intent(this, BoundService::class.java)
                bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    private fun updateStatus(message: String, type: StatusType) {
        runOnUiThread {
            tvStatus.text = message
            val colorRes = when (type) {
                StatusType.IDLE -> R.color.status_idle
                StatusType.RUNNING -> R.color.status_running
                StatusType.SUCCESS -> R.color.status_success
                StatusType.ERROR -> R.color.status_error
            }
            statusIndicator.backgroundTintList = ContextCompat.getColorStateList(this, colorRes)
        }
    }

    private fun startBoundServiceUpdates() {
        updateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isBound) {
                boundService?.let { service ->
                    val count = service.getCurrentCount()
                    tvBoundServiceCount.text = "Bound Service Count: $count"
                }
                delay(1000)
            }
        }
    }

    private fun stopBoundServiceUpdates() {
        updateJob?.cancel()
        updateJob = null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        stopBoundServiceUpdates()
    }

    private fun checkAndPromptNotificationPermission(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!manager.areNotificationsEnabled()) {
            Toast.makeText(context, "Please enable notifications for better experience", Toast.LENGTH_LONG).show()

            val intent = Intent().apply {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    private inner class AsyncTaskExample : AsyncTask<Void, Int, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            updateStatus("Background task running...", StatusType.RUNNING)
        }

        override fun doInBackground(vararg params: Void?): String {
            for (i in 1..5) {
                Thread.sleep(1000)
                publishProgress(i)
            }
            return "Background task completed"
        }

        override fun onProgressUpdate(vararg values: Int?) {
            super.onProgressUpdate(*values)
            val progress = values[0] ?: 0
            updateStatus("Background task progress: $progress/5", StatusType.RUNNING)
            Log.d("AsyncTask", "Count: $progress")
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            updateStatus(result ?: "Task completed", StatusType.SUCCESS)
            Toast.makeText(this@MainActivity, result, Toast.LENGTH_SHORT).show()
        }
    }

    enum class StatusType {
        IDLE, RUNNING, SUCCESS, ERROR
    }
}