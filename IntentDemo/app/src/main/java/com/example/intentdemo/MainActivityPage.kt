package com.example.intentdemo

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.TimeUnit

@UnstableApi
class MainActivityPage : AppCompatActivity() {

    private val airplaneModeReceiver = AirplaneModeReceiver()
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController?
        get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null

    // Handles the Android 13+ POST_NOTIFICATIONS permission request result.
    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                playMusicViaController()
            } else {
                Toast.makeText(this, "Notification permission required for music", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_page)

        val btnBatteryInfo = findViewById<Button>(R.id.btnBatteryInfo)
        val btnContacts = findViewById<Button>(R.id.btnContacts)
        val btnSpecificContact = findViewById<Button>(R.id.btnSpecificContact)
        val btnDial = findViewById<Button>(R.id.btnDial)
        val btnCamera = findViewById<Button>(R.id.btnCamera)
        val btnBrowser = findViewById<Button>(R.id.btnBrowser)
        val btnStartService = findViewById<Button>(R.id.btnStartService)
        val btnStopService = findViewById<Button>(R.id.btnStopService)
        val btnStartMusic = findViewById<Button>(R.id.btnStartMusic)
        val btnStopMusic = findViewById<Button>(R.id.btnStopMusic)
        val btnStartBackgroundWorker = findViewById<Button>(R.id.btnStartBackgroundWorker)
        val btnDownload = findViewById<Button>(R.id.btnDownload)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        val rootView = findViewById<android.view.View>(R.id.tvTitle)

        // Battery Information
        btnBatteryInfo.setOnClickListener {
            val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryStatus?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val percentage = level * 100 / scale
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val health = it.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
                val plugged = it.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                val voltage = it.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
                val temperature = it.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0
                val technology = it.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
                val present = it.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false)

                val message = """
                    Battery Level : $percentage%
                    Status : ${batteryStatusToString(status)}
                    Health : ${batteryHealthToString(health)}
                    Plugged : ${pluggedTypeToString(plugged)}
                    Voltage : $voltage mV
                    Temperature : $temperature °C
                    Technology : $technology
                    Battery Present : $present
                """.trimIndent()

                AlertDialog.Builder(this)
                    .setTitle("Battery Information")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }

        // Open Contacts
        btnContacts.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, getString(R.string.contacts_not_found), Toast.LENGTH_SHORT).show()
            }
        }

        // Open Specific Contact
        btnSpecificContact.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, getString(R.string.contacts_not_found), Toast.LENGTH_SHORT).show()
            }
        }

        // Open Dial Pad
        btnDial.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:9876543210"))
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, getString(R.string.dialer_not_found), Toast.LENGTH_SHORT).show()
            }
        }

        // Open Camera
        btnCamera.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, getString(R.string.camera_not_found), Toast.LENGTH_SHORT).show()
            }
        }

        // Open Browser
        btnBrowser.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/"))
            startActivity(intent)
        }

        // Start Service
        btnStartService.setOnClickListener {
            val intent = Intent(this, DemoService::class.java)
            startService(intent)
            Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT).show()
        }

        // Stop Service
        btnStopService.setOnClickListener {
            val intent = Intent(this, DemoService::class.java)
            stopService(intent)
            Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show()
        }

        // Start Music
        btnStartMusic.setOnClickListener {
            checkPermissionThenPlayMusic()
        }

        // Stop Music
        btnStopMusic.setOnClickListener {
            controller?.pause()
            val intent = Intent(this, MediaPlayerService::class.java)
            stopService(intent)
            Snackbar.make(rootView, "Music Stopped", Snackbar.LENGTH_SHORT).show()
        }

        // Background Worker
        val inputData = workDataOf("LogWorkerMessage" to "This is a log message")
        btnStartBackgroundWorker.setOnClickListener {
            val workRequest = PeriodicWorkRequestBuilder<LogWorker>(15, TimeUnit.MINUTES)
                .setInputData(inputData)
                .build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "LogWorker",
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                workRequest
            )
        }

        // Background Downloader
        btnDownload.setOnClickListener {
            val downloadInputData = workDataOf(
                DownloadWorker.KEY_DOWNLOAD_URL to "https://drive.google.com/uc?export=download&id=10Ver7gLgwy9H6_bTNnO0b0ng7hCr3Tfx",
                DownloadWorker.KEY_FILE_NAME to "hanuman.jpg"
            )
            val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(downloadInputData)
                .build()
            val workManager = WorkManager.getInstance(this)
            workManager.enqueue(downloadRequest)
            Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
            workManager.getWorkInfoByIdLiveData(downloadRequest.id).observe(this) { workInfo ->
                if (workInfo == null) return@observe
                when (workInfo.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        val msg = workInfo.outputData.getString(DownloadWorker.KEY_RESULT_MESSAGE) ?: "Download Completed"
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                    }
                    WorkInfo.State.FAILED -> {
                        val msg = workInfo.outputData.getString(DownloadWorker.KEY_RESULT_MESSAGE) ?: "Download Failed"
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
        }

        // Logout
        btnLogout.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        ContextCompat.registerReceiver(this, airplaneModeReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        initializeController()
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(airplaneModeReceiver)
        releaseController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(this, ComponentName(this, MediaPlayerService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
    }

    private fun releaseController() {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        controllerFuture = null
    }

    private fun checkPermissionThenPlayMusic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                playMusicViaController()
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            playMusicViaController()
        }
    }

    private fun playMusicViaController() {
        controller?.let {
            if (!it.isPlaying) {
                it.play()
            }
            Snackbar.make(findViewById(android.R.id.content), "Music Started", Snackbar.LENGTH_SHORT).show()
        } ?: run {
            // If controller is not ready, we wait and try again or show a message.
            // In a real app, you'd add a listener to the future.
            controllerFuture?.addListener({
                controller?.play()
                Snackbar.make(findViewById(android.R.id.content), "Music Started", Snackbar.LENGTH_SHORT).show()
            }, MoreExecutors.directExecutor())
        }
    }

    private fun batteryStatusToString(status: Int): String = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
        BatteryManager.BATTERY_STATUS_FULL -> "Full"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
        else -> "Unknown"
    }

    private fun batteryHealthToString(health: Int): String = when (health) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
        BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
        else -> "Unknown"
    }

    private fun pluggedTypeToString(type: Int): String = when (type) {
        BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
        BatteryManager.BATTERY_PLUGGED_USB -> "USB"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
        else -> "Not Plugged"
    }
}
