package com.example.fragmentsdemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.util.Log

class AirplaneModeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("AirplaneReceiver", "onReceive called")
        if (intent?.action == Intent.ACTION_AIRPLANE_MODE_CHANGED) {
            val isOn = intent.getBooleanExtra("state", false)
            val msg = if (isOn) "Airplane Mode ON" else "Airplane Mode OFF"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
