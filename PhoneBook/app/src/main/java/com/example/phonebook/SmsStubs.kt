package com.example.phonebook

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity

class ComposeSmsActivity : AppCompatActivity()
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {}
}
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {}
}
class RespondService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
