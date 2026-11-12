package com.example.worldexplorer

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import java.util.Locale

class CountryDetailActivity : AppCompatActivity() {

    private var country: Country? = null
    private var isWishlisted = false
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance("https://fire1-c8839-default-rtdb.firebaseio.com")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_country_detail)

        country = intent.getSerializableExtra("country") as? Country
        val currentCountry = country

        if (currentCountry == null) {
            Toast.makeText(this, "Failed to load country details", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val imgFlag = findViewById<ImageView>(R.id.imgDetailFlag)
        val tvName = findViewById<TextView>(R.id.tvDetailName)
        val tvCapital = findViewById<TextView>(R.id.tvDetailCapital)
        val tvRegion = findViewById<TextView>(R.id.tvDetailRegion)
        val tvPopulation = findViewById<TextView>(R.id.tvDetailPopulation)
        val tvCurrencies = findViewById<TextView>(R.id.tvDetailCurrencies)
        val tvLanguages = findViewById<TextView>(R.id.tvDetailLanguages)
        val btnWishlist = findViewById<Button>(R.id.btnWishlist)

        tvName.text = currentCountry.name
        tvCapital.text = "Capital: ${currentCountry.capital}"
        tvRegion.text = "Region: ${currentCountry.region}"

        val formattedPop = NumberFormat.getInstance(Locale.US).format(currentCountry.population)
        tvPopulation.text = "Population: $formattedPop"
        tvCurrencies.text = "Currencies: ${currentCountry.currencies}"
        tvLanguages.text = "Languages: ${currentCountry.languages}"

        val flagUrl = currentCountry.flagUrl.ifBlank {
            if (currentCountry.code.length == 2) "https://flags.restcountries.com/v5/w320/${currentCountry.code.lowercase()}.png" else ""
        }

        if (flagUrl.isNotBlank()) {
            Glide.with(this)
                .load(flagUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(imgFlag)
        } else {
            Glide.with(this).clear(imgFlag)
            imgFlag.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        val currentUser = auth.currentUser
        if (currentUser != null && currentCountry.code.isNotEmpty()) {
            val ref = database.getReference("users")
                .child(currentUser.uid)
                .child("wishlist")
                .child(currentCountry.code)

            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isWishlisted = snapshot.exists()
                    if (isWishlisted) {
                        btnWishlist.text = "Remove from Wish-to-Visit"
                    } else {
                        btnWishlist.text = "Add to Wish-to-Visit"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@CountryDetailActivity, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })

            btnWishlist.setOnClickListener {
                if (isWishlisted) {
                    ref.removeValue().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Removed from wishlist", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Failed to remove: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    val map = hashMapOf(
                        "name" to currentCountry.name,
                        "code" to currentCountry.code,
                        "flagUrl" to flagUrl,
                        "region" to currentCountry.region,
                        "capital" to currentCountry.capital,
                        "population" to currentCountry.population,
                        "currencies" to currentCountry.currencies,
                        "languages" to currentCountry.languages
                    )
                    ref.setValue(map).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Added to wishlist", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Failed to add: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } else {
            btnWishlist.isEnabled = false
        }
    }
}
