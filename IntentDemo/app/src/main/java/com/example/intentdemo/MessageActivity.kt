package com.example.intentdemo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MessageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        val tvMessage = findViewById<TextView>(R.id.tvMessage)
        val btnOK = findViewById<Button>(R.id.btnOK)

        val message = intent.getStringExtra(MainActivity.MESSAGE)
        val success = intent.getBooleanExtra(MainActivity.SUCCESS, false)

        tvMessage.text = message

        btnOK.setOnClickListener {

            if (success) {

                val intent = Intent(this, MainActivityPage::class.java)
                startActivity(intent)

                finish()

            } else {

                finish()

            }

        }
    }
}