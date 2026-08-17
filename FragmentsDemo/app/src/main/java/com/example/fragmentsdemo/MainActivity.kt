package com.example.fragmentsdemo

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.fragmentsdemo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var isLoggedIn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_dashboard, R.id.nav_settings)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // Use a custom listener to restrict access during login
        binding.bottomNav.setOnItemSelectedListener { item ->
            if (!isLoggedIn) {
                Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
                false
            } else {
                NavigationUI.onNavDestinationSelected(item, navController)
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.loginFragment) {
                binding.appBarLayout.visibility = View.GONE
                isLoggedIn = false
            } else {
                binding.appBarLayout.visibility = View.VISIBLE
                isLoggedIn = true
            }
            // Sync bottom nav selection
            binding.bottomNav.menu.findItem(destination.id)?.let {
                it.isChecked = true
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
