package com.example.worldexplorer

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

class MapActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var webView: WebView
    private lateinit var tvLocationCoords: TextView
    private lateinit var progressBarMap: ProgressBar
    private lateinit var btnRefreshLocation: Button

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            fetchLocationAndShowMap()
        } else {
            progressBarMap.visibility = View.GONE
            tvLocationCoords.text = "Location permission denied"
            Toast.makeText(this, "Location permission is required to display your position on the map.", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        webView = findViewById(R.id.webView)
        tvLocationCoords = findViewById(R.id.tvLocationCoords)
        progressBarMap = findViewById(R.id.progressBarMap)
        btnRefreshLocation = findViewById(R.id.btnRefreshLocation)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }
        }

        btnRefreshLocation.setOnClickListener {
            checkPermissionAndFetchLocation()
        }

        checkPermissionAndFetchLocation()
    }

    private fun checkPermissionAndFetchLocation() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            fetchLocationAndShowMap()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocationAndShowMap() {
        progressBarMap.visibility = View.VISIBLE
        tvLocationCoords.text = "Retrieving fresh location..."

        val cancellationTokenSource = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { freshLocation ->
            if (freshLocation != null) {
                updateMapAndUI(freshLocation.latitude, freshLocation.longitude)
            } else {
                fetchLastLocationFallback()
            }
        }.addOnFailureListener {
            fetchLastLocationFallback()
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLastLocationFallback() {
        fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
            if (lastLocation != null) {
                updateMapAndUI(lastLocation.latitude, lastLocation.longitude)
            } else {
                progressBarMap.visibility = View.GONE
                tvLocationCoords.text = "Location unavailable. Please check GPS settings."
                Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            progressBarMap.visibility = View.GONE
            tvLocationCoords.text = "Failed to obtain location"
            Toast.makeText(this, "Location error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateMapAndUI(lat: Double, lon: Double) {
        progressBarMap.visibility = View.GONE
        tvLocationCoords.text = String.format(Locale.US, "Latitude: %.6f, Longitude: %.6f", lat, lon)

        val mapUrl = "https://www.openstreetmap.org/?mlat=$lat&mlon=$lon#map=16/$lat/$lon"
        webView.loadUrl(mapUrl)
    }
}
