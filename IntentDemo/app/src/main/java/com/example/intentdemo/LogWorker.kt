package com.example.intentdemo

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class LogWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {

        val logMessage =
            inputData.getString("LogWorkerMessage")
                ?: "Default Log Message"

        Log.d("LogWorker", logMessage)

        return Result.success()
    }
}