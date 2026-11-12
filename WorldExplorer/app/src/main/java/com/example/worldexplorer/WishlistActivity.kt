package com.example.worldexplorer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class WishlistActivity : AppCompatActivity() {

    private lateinit var adapter: CountryAdapter
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance("https://fire1-c8839-default-rtdb.firebaseio.com")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wishlist)

        val rvWishlist = findViewById<RecyclerView>(R.id.rvWishlist)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvEmptyWishlist = findViewById<TextView>(R.id.tvEmptyWishlist)

        rvWishlist.layoutManager = LinearLayoutManager(this)
        adapter = CountryAdapter(emptyList()) { country ->
            val intent = Intent(this, CountryDetailActivity::class.java)
            intent.putExtra("country", country)
            startActivity(intent)
        }
        rvWishlist.adapter = adapter

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        progressBar.visibility = View.VISIBLE
        tvEmptyWishlist.visibility = View.GONE

        val ref = database.getReference("users").child(currentUser.uid).child("wishlist")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                progressBar.visibility = View.GONE
                val list = mutableListOf<Country>()
                for (child in snapshot.children) {
                    val name = child.child("name").getValue(String::class.java) ?: ""
                    val code = child.child("code").getValue(String::class.java) ?: child.key ?: ""
                    val flagUrl = child.child("flagUrl").getValue(String::class.java) ?: ""
                    val region = child.child("region").getValue(String::class.java) ?: "N/A"
                    val capital = child.child("capital").getValue(String::class.java) ?: "N/A"
                    val population = child.child("population").getValue(Long::class.java) ?: 0L
                    val currencies = child.child("currencies").getValue(String::class.java) ?: "N/A"
                    val languages = child.child("languages").getValue(String::class.java) ?: "N/A"

                    list.add(
                        Country(
                            name = name,
                            code = code,
                            flagUrl = flagUrl,
                            region = region,
                            capital = capital,
                            population = population,
                            currencies = currencies,
                            languages = languages
                        )
                    )
                }

                adapter.updateData(list)
                if (list.isEmpty()) {
                    tvEmptyWishlist.visibility = View.VISIBLE
                } else {
                    tvEmptyWishlist.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@WishlistActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
