package com.example.recap_exercise

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.util.Log

class BoundService : Service() {
    private val binder = LocalBinder()
    private var count = 0

    inner class LocalBinder : Binder() {
        fun getService(): BoundService = this@BoundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun getCurrentCount(): Int = count

    override fun onCreate() {
        super.onCreate()
        // Đếm trong background thread
        Thread {
            while (true) {
                Thread.sleep(1000)
                count++
                Log.d("BoundService", "Count: $count")
            }
        }.start()
    }
}



