package com.example.recap_exercise

import android.app.Service
import android.content.Intent
import android.os.AsyncTask
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class BackgroundService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        CoroutineScope(Dispatchers.Default).launch {
            for (i in 1..10) {
                delay(1000)
                Log.d("BackgroundService", "Count: $i")
            }
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
