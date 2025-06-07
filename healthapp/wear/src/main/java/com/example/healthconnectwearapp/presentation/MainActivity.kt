package com.example.healthconnectwearapp.presentation

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.MaterialTheme
//import androidx.wear.compose.material.Surface
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.example.healthconnectwearapp.databinding.ActivityMainBinding

class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {

    private lateinit var binding: ActivityMainBinding
    private val TAG = "WearMainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Reset all values to default
        resetUI()
        
        Log.d(TAG, "onCreate: Watch app initialized")
    }

    private fun resetUI() {
        binding.tvSteps.text = "Steps: 0"
        binding.tvCalories.text = "Calories: 0"
        binding.tvAchievement.text = "No achievements yet"
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
        Log.d(TAG, "onResume: Data listener registered")
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
        Log.d(TAG, "onPause: Data listener removed")
    }

    @SuppressLint("SetTextI18n")
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "onDataChanged called with ${dataEvents.count} events")

        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataItem = event.dataItem
                Log.d(TAG, "Data item path: ${dataItem.uri.path}")

                when (dataItem.uri.path) {
                    "/health_data" -> {
                        try {
                            val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                            val steps = dataMap.getLong("steps", 0L)
                            val calories = dataMap.getDouble("calories", 0.0)
                            val timestamp = dataMap.getLong("timestamp", 0L)

                            Log.d(TAG, "Received health data - steps: $steps, calories: $calories")

                            runOnUiThread {
                                binding.tvSteps.text = "Steps: $steps"
                                binding.tvCalories.text = "Calories: ${String.format("%.1f", calories)}"
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing health data: ${e.message}", e)
                        }
                    }
                    "/achievement" -> {
                        try {
                            val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                            val name = dataMap.getString("name", "")
                            val stepsRequired = dataMap.getInt("steps_required", 0)

                            Log.d(TAG, "Received achievement - name: $name, steps: $stepsRequired")

                            runOnUiThread {
                                binding.tvAchievement.text = "🏆 $name"
                                Toast.makeText(this, "Achievement Unlocked: $name!", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing achievement: ${e.message}", e)
                        }
                    }
                }
            } else if (event.type == DataEvent.TYPE_DELETED) {
                Log.d(TAG, "Data item deleted: ${event.dataItem.uri.path}")
            }
        }
    }
}