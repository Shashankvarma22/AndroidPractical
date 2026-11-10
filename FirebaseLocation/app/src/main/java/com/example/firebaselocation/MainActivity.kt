package com.example.firebaselocation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    private val database = FirebaseDatabase.getInstance()

    private lateinit var loginContainer: LinearLayout
    private lateinit var profileContainer: LinearLayout

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvUid: TextView
    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView

    private lateinit var webView: WebView

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineLocation =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

            val coarseLocation =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (fineLocation || coarseLocation) {
                getCurrentLocation()
            } else {
                Toast.makeText(
                    this,
                    "Location permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        loginContainer = findViewById(R.id.loginContainer)
        profileContainer = findViewById(R.id.profileContainer)

        val btnGoogleSignIn =
            findViewById<Button>(R.id.btnGoogleSignIn)

        val btnReadLoginCount =
            findViewById<Button>(R.id.btnReadLoginCount)

        val btnLogout =
            findViewById<Button>(R.id.btnLogout)

        tvName = findViewById(R.id.tvName)
        tvEmail = findViewById(R.id.tvEmail)
        tvUid = findViewById(R.id.tvUid)
        tvLatitude = findViewById(R.id.tvLatitude)
        tvLongitude = findViewById(R.id.tvLongitude)

        webView = findViewById(R.id.webView)

        profileContainer.visibility = View.GONE
        loginContainer.visibility = View.VISIBLE

        btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        btnReadLoginCount.setOnClickListener {
            readLoginCount()
        }

        btnLogout.setOnClickListener {
            auth.signOut()

            profileContainer.visibility = View.GONE
            loginContainer.visibility = View.VISIBLE

            tvName.text = "Name: --"
            tvEmail.text = "Email: --"
            tvUid.text = "UID: --"
            tvLatitude.text = "Latitude: --"
            tvLongitude.text = "Longitude: --"

            webView.loadUrl("about:blank")

            Toast.makeText(
                this,
                "Logged out",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun signInWithGoogle() {

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(
                getString(R.string.default_web_client_id)
            )
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {

            try {

                val result = credentialManager.getCredential(
                    this@MainActivity,
                    request
                )

                val credential = result.credential

                if (credential is androidx.credentials.CustomCredential &&
                    credential.type ==
                    GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {

                    val googleCredential =
                        GoogleIdTokenCredential.createFrom(
                            credential.data
                        )

                    firebaseAuth(googleCredential.idToken)
                }

            } catch (e: Exception) {

                Toast.makeText(
                    this@MainActivity,
                    "Sign-in failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun firebaseAuth(idToken: String) {

        val credential =
            GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {

                    val user = auth.currentUser

                    if (user != null) {

                        tvName.text =
                            "Name: ${user.displayName}"

                        tvEmail.text =
                            "Email: ${user.email}"

                        tvUid.text =
                            "UID: ${user.uid}"

                        loginContainer.visibility = View.GONE
                        profileContainer.visibility = View.VISIBLE

                        requestLocationPermission()
                    }

                } else {

                    Toast.makeText(
                        this,
                        "Authentication failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun requestLocationPermission() {

        val fineLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineLocation || coarseLocation) {

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
                .addOnSuccessListener { location ->

                    if (location != null) {

                        val latitude = location.latitude
                        val longitude = location.longitude

                        tvLatitude.text =
                            "Latitude: $latitude"

                        tvLongitude.text =
                            "Longitude: $longitude"

                        showMap(latitude, longitude)

                        saveLoginData(
                            latitude,
                            longitude
                        )

                    } else {

                        Toast.makeText(
                            this,
                            "Location unavailable",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .addOnFailureListener {

                    Toast.makeText(
                        this,
                        "Unable to get location",
                        Toast.LENGTH_SHORT
                    ).show()
                }

        } catch (e: SecurityException) {

            Toast.makeText(
                this,
                "Location permission required",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showMap(
        latitude: Double,
        longitude: Double
    ) {

        webView.webViewClient = WebViewClient()

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        val mapUrl =
            "https://www.openstreetmap.org/?mlat=$latitude&mlon=$longitude#map=15/$latitude/$longitude"

        webView.loadUrl(mapUrl)
    }

    private fun saveLoginData(
        latitude: Double,
        longitude: Double
    ) {

        val user = auth.currentUser ?: return

        val userReference = database
            .getReference("users")
            .child(user.uid)

        val userData = mapOf(
            "uid" to user.uid,
            "name" to user.displayName,
            "email" to user.email
        )

        userReference.updateChildren(userData)

        val loginData = mapOf(
            "timestamp" to System.currentTimeMillis(),
            "latitude" to latitude,
            "longitude" to longitude
        )

        userReference
            .child("loginHistory")
            .push()
            .setValue(loginData)
    }

    private fun readLoginCount() {

        val user = auth.currentUser ?: return

        database
            .getReference("users")
            .child(user.uid)
            .child("loginHistory")
            .addListenerForSingleValueEvent(
                object : com.google.firebase.database.ValueEventListener {

                    override fun onDataChange(
                        snapshot: com.google.firebase.database.DataSnapshot
                    ) {

                        val loginCount = snapshot.childrenCount

                        Toast.makeText(
                            this@MainActivity,
                            "Login Count: $loginCount",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onCancelled(
                        error: com.google.firebase.database.DatabaseError
                    ) {

                        Toast.makeText(
                            this@MainActivity,
                            "Failed to read login count",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
    }
}