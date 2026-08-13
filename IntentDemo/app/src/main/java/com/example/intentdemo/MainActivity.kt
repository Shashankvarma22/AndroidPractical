package com.example.intentdemo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        const val MESSAGE = "MESSAGE"
        const val SUCCESS = "SUCCESS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {

            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            etUsername.error = null
            etPassword.error = null

            if (username.isEmpty()) {
                etUsername.error = getString(R.string.username_required)
                etUsername.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                etPassword.error = getString(R.string.password_required)
                etPassword.requestFocus()
                return@setOnClickListener
            }

            if (username != "admin" || password != "password") {
                Toast.makeText(
                    this,
                    getString(R.string.invalid_credentials),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val intent = Intent(this, MessageActivity::class.java)
            intent.putExtra(
                MESSAGE,
                getString(R.string.login_success)
            )
            intent.putExtra(SUCCESS, true)

            startActivity(intent)
        }
    }
}