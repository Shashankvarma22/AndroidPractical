package com.example.fragmentsdemo

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.net.HttpURLConnection
import java.net.URL

class DownloadWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        private const val TAG = "DownloadWorker"
        const val KEY_DOWNLOAD_URL = "DownloadUrl"
        const val KEY_FILE_NAME = "DownloadFileName"
        const val KEY_RESULT_MESSAGE = "ResultMessage"
    }

    override fun doWork(): Result {
        val fileUrl = inputData.getString(KEY_DOWNLOAD_URL)
        val fileName = inputData.getString(KEY_FILE_NAME) ?: "downloaded_file.jpg"

        if (fileUrl.isNullOrBlank()) {
            Log.e(TAG, "No download URL provided")
            return Result.failure(workDataOf(KEY_RESULT_MESSAGE to "No URL"))
        }

        Log.d(TAG, "Starting download: $fileUrl")

        var connection: HttpURLConnection? = null
        try {
            val url = URL(fileUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Server returned HTTP ${connection.responseCode}")
                return Result.failure(workDataOf(KEY_RESULT_MESSAGE to "Server error"))
            }

            val resolver = applicationContext.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val uri = resolver.insert(collection, contentValues) ?: return Result.failure()

            connection.inputStream.use { input ->
                resolver.openOutputStream(uri)?.use { output ->
                    input.copyTo(output)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            Log.d(TAG, "Download successful: $fileName")
            return Result.success(workDataOf(KEY_RESULT_MESSAGE to "Downloaded $fileName"))

        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            return Result.failure(workDataOf(KEY_RESULT_MESSAGE to "Error: ${e.message}"))
        } finally {
            connection?.disconnect()
        }
    }
}
