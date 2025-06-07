package com.example.healthconnectwearapp.presentation

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.android.gms.wearable.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DataLayerService : Service(), DataClient.OnDataChangedListener {
    private val TAG = "DataLayerService"
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Wearable.getDataClient(this).addListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Wearable.getDataClient(this).removeListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataItem = event.dataItem
                if (dataItem.uri.path?.compareTo("/health-data") == 0) {
                    DataMapItem.fromDataItem(dataItem).dataMap.apply {
                        val steps = getInt("steps", 0)
                        val calories = getDouble("calories", 0.0)
                        Log.d(TAG, "Received health data: Steps=$steps, Calories=$calories")
                        
                        // Handle the received health data here
                        scope.launch {
                            // You can add your data processing logic here
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun sendHealthData(dataClient: DataClient, steps: Int, calories: Double) {
            val request = PutDataMapRequest.create("/health-data").apply {
                dataMap.putInt("steps", steps)
                dataMap.putDouble("calories", calories)
            }.asPutDataRequest()

            dataClient.putDataItem(request).addOnSuccessListener {
                Log.d("DataLayerService", "Health data sent successfully")
            }.addOnFailureListener { e ->
                Log.e("DataLayerService", "Failed to send health data", e)
            }
        }
    }
} 