package com.example.phonebook

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView

class ConversationActivity : AppCompatActivity() {

    private lateinit var adapter: MessageAdapter
    private val messages = mutableListOf<Sms>()
    private val dbHelper by lazy { DatabaseHelper(this) }
    private var address: String = ""
    private var contactName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_conversation)

        address = intent.getStringExtra("address") ?: ""
        
        // Resolve saved contact name via ContentResolver from PhoneBookProvider
        val resolvedName = ContactLookup.getContactName(this, address)
        contactName = resolvedName ?: intent.getStringExtra("contactName")

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (resolvedName != null) {
            supportActionBar?.title = resolvedName
            supportActionBar?.subtitle = address
        } else {
            supportActionBar?.title = address
            supportActionBar?.subtitle = null
        }

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.conversation_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        loadMessages()

        findViewById<ImageButton>(R.id.btnSend).setOnClickListener {
            sendMessage()
        }
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(messages)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewMessages)
        recyclerView.adapter = adapter
    }

    private fun loadMessages() {
        messages.clear()
        val rawMessages = dbHelper.getMessagesByAddress(address)
        val resolvedMessages = rawMessages.map { sms ->
            if (contactName != null) sms.copy(contactName = contactName) else sms
        }
        messages.addAll(resolvedMessages)
        adapter.notifyDataSetChanged()
        if (messages.isNotEmpty()) {
            findViewById<RecyclerView>(R.id.recyclerViewMessages).scrollToPosition(messages.size - 1)
        }
    }

    private fun sendMessage() {
        val editMessage = findViewById<EditText>(R.id.editMessage)
        val text = editMessage.text.toString().trim()
        if (text.isNotEmpty()) {
            val sms = Sms(
                phoneNumber = address,
                message = text,
                date = System.currentTimeMillis(),
                type = 2 // Sent
            )
            dbHelper.insertSms(sms)
            editMessage.text.clear()
            loadMessages()
        } else {
            Toast.makeText(this, "Cannot send empty message", Toast.LENGTH_SHORT).show()
        }
    }
}
