package com.example.intentdemo

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.util.Timer
import java.util.TimerTask

class DemoService : Service() {

    private var isStarted = false
    private var counter = 0
    private var timer: Timer? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (isStarted) {
            Log.d("ServiceLog", "Service Started already")
        } else {

            isStarted = true

            timer = Timer()

            timer?.scheduleAtFixedRate(object : TimerTask() {

                override fun run() {

                    counter++

                    Log.d(
                        "ServiceLog",
                        "Counter : $counter"
                    )

                }

            }, 0, 1000)

        }

        return START_STICKY
    }

    override fun onDestroy() {

        super.onDestroy()

        timer?.cancel()
        timer = null

        isStarted = false

        Log.d(
            "ServiceLog",
            "Service Destroyed"
        )

    }

}