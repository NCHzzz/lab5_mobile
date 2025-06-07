package com.example.healthconnectwearapp.presentation

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

class DataLayerService : WearableListenerService() {
    private val TAG = "DataLayerService"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "DataLayerService created")
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "onDataChanged called in service with ${dataEvents.count} events")

        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataItem = event.dataItem
                Log.d(TAG, "Data item path in service: ${dataItem.uri.path}")

                if (dataItem.uri.path == "/health_data") {
                    try {
                        val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                        val steps = dataMap.getLong("steps", 0L)
                        val calories = dataMap.getDouble("calories", 0.0)

                        Log.d(TAG, "Service received - steps: $steps, calories: $calories")

                        // If you need to send a local broadcast to update the UI, you can do it here
                        // This is useful if the app might not be in foreground
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing data in service: ${e.message}", e)
                    }
                }
            }
        }
    }
}