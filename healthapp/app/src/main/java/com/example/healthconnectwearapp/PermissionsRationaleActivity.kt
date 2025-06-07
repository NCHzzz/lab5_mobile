package com.example.healthconnectwearapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class PermissionsRationaleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions_rationale)

        val rationaleText = findViewById<TextView>(R.id.rationale_text)
        rationaleText.text = "This app requires access to your health data to display steps on your Wear OS device. Your data is securely handled and not shared without your consent."
    }
}