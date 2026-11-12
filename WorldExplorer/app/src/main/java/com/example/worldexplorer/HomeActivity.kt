package com.example.worldexplorer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var adapter: CountryAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var allLoadedCountries: List<Country> = emptyList()
    private var currentSelectedRegion: String = "All"

    private lateinit var rvCountries: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var etSearch: EditText
    private lateinit var chipGroupRegions: ChipGroup

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            detectUserCountry()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val btnWishlist = findViewById<Button>(R.id.btnWishlist)
        val btnDetectCountry = findViewById<Button>(R.id.btnDetectCountry)
        val btnLiveMap = findViewById<Button>(R.id.btnLiveMap)
        val btnSearch = findViewById<Button>(R.id.btnSearch)

        btnLiveMap.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }
        etSearch = findViewById(R.id.etSearch)
        chipGroupRegions = findViewById(R.id.chipGroupRegions)
        rvCountries = findViewById(R.id.rvCountries)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        rvCountries.layoutManager = LinearLayoutManager(this)
        adapter = CountryAdapter(emptyList()) { country ->
            val intent = Intent(this, CountryDetailActivity::class.java)
            intent.putExtra("country", country)
            startActivity(intent)
        }
        rvCountries.adapter = adapter

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        btnWishlist.setOnClickListener {
            startActivity(Intent(this, WishlistActivity::class.java))
        }

        btnDetectCountry.setOnClickListener {
            checkLocationPermissionsAndDetect()
        }

        btnSearch.setOnClickListener {
            performSearch()
        }

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        chipGroupRegions.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) {
                currentSelectedRegion = "All"
            } else {
                val chipId = checkedIds.first()
                currentSelectedRegion = when (chipId) {
                    R.id.chipAsia -> "Asia"
                    R.id.chipEurope -> "Europe"
                    R.id.chipAfrica -> "Africa"
                    R.id.chipAmericas -> "Americas"
                    R.id.chipOceania -> "Oceania"
                    else -> "All"
                }
            }
            applyRegionFilter()
        }

        loadAllCountries()
    }

    private fun loadAllCountries() {
        progressBar.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE
        rvCountries.visibility = View.GONE

        lifecycleScope.launch {
            when (val result = ApiClient.fetchAllCountries()) {
                is ApiResult.Success -> {
                    allLoadedCountries = result.data
                    progressBar.visibility = View.GONE
                    applyRegionFilter()
                }
                is ApiResult.Error -> {
                    progressBar.visibility = View.GONE
                    allLoadedCountries = emptyList()
                    tvEmptyState.text = "Failed to load countries:\n${result.message}"
                    tvEmptyState.visibility = View.VISIBLE
                    rvCountries.visibility = View.GONE
                    Toast.makeText(this@HomeActivity, "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun performSearch() {
        val query = etSearch.text.toString().trim()
        if (query.isEmpty()) {
            loadAllCountries()
            return
        }

        progressBar.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE
        rvCountries.visibility = View.GONE

        lifecycleScope.launch {
            when (val result = ApiClient.searchCountries(query)) {
                is ApiResult.Success -> {
                    allLoadedCountries = result.data
                    progressBar.visibility = View.GONE
                    applyRegionFilter()
                }
                is ApiResult.Error -> {
                    progressBar.visibility = View.GONE
                    allLoadedCountries = emptyList()
                    tvEmptyState.text = "Search Error:\n${result.message}"
                    tvEmptyState.visibility = View.VISIBLE
                    rvCountries.visibility = View.GONE
                    Toast.makeText(this@HomeActivity, "Search error: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun applyRegionFilter() {
        val filteredList = if (currentSelectedRegion == "All") {
            allLoadedCountries
        } else {
            allLoadedCountries.filter { country ->
                country.region.equals(currentSelectedRegion, ignoreCase = true)
            }
        }

        adapter.updateData(filteredList)

        if (filteredList.isEmpty()) {
            if (tvEmptyState.text.isNullOrEmpty()) {
                tvEmptyState.text = "No countries found"
            }
            tvEmptyState.visibility = View.VISIBLE
            rvCountries.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            rvCountries.visibility = View.VISIBLE
        }
    }

    private fun checkLocationPermissionsAndDetect() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted || coarseLocationGranted) {
            detectUserCountry()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun detectUserCountry() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Location permission missing", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        Toast.makeText(this, "Detecting your location...", Toast.LENGTH_SHORT).show()

        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val geocoder = Geocoder(this@HomeActivity, Locale.getDefault())
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        val countryCode = addresses?.firstOrNull()?.countryCode

                        if (!countryCode.isNullOrEmpty()) {
                            val apiResult = ApiClient.getCountryByCode(countryCode)
                            withContext(Dispatchers.Main) {
                                progressBar.visibility = View.GONE
                                when (apiResult) {
                                    is ApiResult.Success -> {
                                        val intent = Intent(this@HomeActivity, CountryDetailActivity::class.java)
                                        intent.putExtra("country", apiResult.data)
                                        startActivity(intent)
                                    }
                                    is ApiResult.Error -> {
                                        Toast.makeText(
                                            this@HomeActivity,
                                            "Error fetching details ($countryCode): ${apiResult.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                progressBar.visibility = View.GONE
                                Toast.makeText(
                                    this@HomeActivity,
                                    "Could not determine country code from location",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            progressBar.visibility = View.GONE
                            Toast.makeText(
                                this@HomeActivity,
                                "Geocoder error: ${e.localizedMessage}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } else {
                progressBar.visibility = View.GONE
                Toast.makeText(
                    this,
                    "Unable to get current location. Ensure location is enabled on device.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }.addOnFailureListener { e ->
            progressBar.visibility = View.GONE
            Toast.makeText(
                this,
                "Failed to obtain location: ${e.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
