package com.example.glocation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices

class MainActivity : AppCompatActivity() {
    private lateinit var mapWebView: WebView
    private lateinit var tvCoordinates: TextView
    private lateinit var btnGetLocation: Button

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val granted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (granted) {
                getCurrentLocation()
            } else {
                Toast.makeText(
                    this,
                    "Location permission is required.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapWebView = findViewById(R.id.mapWebView)
        tvCoordinates = findViewById(R.id.tvCoordinates)
        btnGetLocation = findViewById(R.id.btnGetLocation)

        mapWebView.settings.javaScriptEnabled = true
        mapWebView.settings.domStorageEnabled = true

        btnGetLocation.setOnClickListener {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            getCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun getCurrentLocation() {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->

                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude

                        tvCoordinates.text =
                            "Latitude: $latitude\nLongitude: $longitude"

                        val mapUrl =
                            "https://www.openstreetmap.org/" +
                                    "?mlat=$latitude&mlon=$longitude" +
                                    "#map=16/$latitude/$longitude"

                        mapWebView.loadUrl(mapUrl)
                    } else {
                        Toast.makeText(
                            this,
                            "Location unavailable. Enable device location and try again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        this,
                        "Could not get location: ${exception.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        } catch (exception: SecurityException) {
            Toast.makeText(
                this,
                "Location permission is missing.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}