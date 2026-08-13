package com.example.intentdemo

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

            if (isOn) {
                Toast.makeText(
                    context,
                    "Airplane Mode ON",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    context,
                    "Airplane Mode OFF",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}