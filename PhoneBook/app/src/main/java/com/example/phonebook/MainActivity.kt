package com.example.phonebook

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: ContactAdapter
    private val contacts = mutableListOf<Contact>()
    private val gson = Gson()
    private val prefs by lazy { getSharedPreferences("phonebook_prefs", MODE_PRIVATE) }
    private val dbHelper by lazy { DatabaseHelper(this) }

    // SAF Export Launcher
    private val safExportLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                writeJsonToUri(uri)
            }
        }
    }

    // SAF Import Launcher
    private val safImportLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                readJsonFromUri(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        setupWindowInsets()
        migrateDataIfNeeded()
        loadContacts()
        setupRecyclerView()

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            showContactDialog(null)
        }
    }

    private fun migrateDataIfNeeded() {
        if (prefs.getBoolean("db_migrated", false)) return

        val json = prefs.getString("contacts_list", null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<Contact>>() {}.type
                val oldContacts: List<Contact> = gson.fromJson(json, type)
                
                val db = dbHelper.writableDatabase
                db.beginTransaction()
                try {
                    for (oldContact in oldContacts) {
                        val numbers = if (oldContact.numbers.isNotEmpty()) {
                            oldContact.numbers
                        } else if (oldContact.phoneNumber.isNotEmpty()) {
                            listOf(oldContact.phoneNumber)
                        } else {
                            emptyList()
                        }
                        dbHelper.insertContact(oldContact.name, numbers)
                    }
                    db.setTransactionSuccessful()
                    prefs.edit().putBoolean("db_migrated", true).apply()
                } finally {
                    db.endTransaction()
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Migration failed", e)
            }
        } else {
            prefs.edit().putBoolean("db_migrated", true).apply()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export -> {
                exportContactsToFile()
                true
            }
            R.id.action_import -> {
                importContactsFromFile()
                true
            }
            R.id.action_saf_export -> {
                triggerSafExport()
                true
            }
            R.id.action_saf_import -> {
                triggerSafImport()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupRecyclerView() {
        adapter = ContactAdapter(
            contacts,
            onEditClick = { position -> showContactDialog(position) },
            onDeleteClick = { position -> showDeleteConfirmation(position) }
        )
        findViewById<RecyclerView>(R.id.recyclerView).adapter = adapter
    }

    private fun loadContacts() {
        contacts.clear()
        contacts.addAll(dbHelper.getAllContacts())
        if (contacts.isEmpty() && !prefs.getBoolean("db_migrated", false)) {
            // If DB is empty and not migrated, we might be on a clean install with defaults
            val defaults = listOf(
                Contact(name = "John Doe", numbers = listOf("123-456-7890")),
                Contact(name = "Jane Smith", numbers = listOf("987-654-3210")),
                Contact(name = "Bob Johnson", numbers = listOf("555-123-4567"))
            )
            // Save defaults to DB
            for (c in defaults) {
                dbHelper.insertContact(c.name, c.numbers)
            }
            // Reload to get real IDs
            contacts.clear()
            contacts.addAll(dbHelper.getAllContacts())
        }
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
    }



    // --- Original App-Specific Export/Import ---

    private fun exportContactsToFile() {
        try {
            val file = File(getExternalFilesDir(null), "phonebook.json")
            file.writeText(gson.toJson(contacts))
            Toast.makeText(this, R.string.msg_exported, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.error_export_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun importContactsFromFile() {
        try {
            val file = File(getExternalFilesDir(null), "phonebook.json")
            if (file.exists()) {
                val json = file.readText()
                updateContactsList(json)
                Toast.makeText(this, R.string.msg_imported, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.error_file_not_found, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, R.string.error_import_failed, Toast.LENGTH_SHORT).show()
        }
    }

    // --- SAF Export/Import ---

    private fun triggerSafExport() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "phonebook.json")
        }
        safExportLauncher.launch(intent)
    }

    private fun triggerSafImport() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        safImportLauncher.launch(intent)
    }

    private fun writeJsonToUri(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(gson.toJson(contacts).toByteArray())
            }
            Toast.makeText(this, R.string.msg_saf_exported, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.error_export_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun readJsonFromUri(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val json = reader.readText()
                    updateContactsList(json)
                }
            }
            Toast.makeText(this, R.string.msg_saf_imported, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.error_import_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateContactsList(json: String) {
        val type = object : TypeToken<List<Contact>>() {}.type
        val importedContacts: List<Contact> = gson.fromJson(json, type)
        
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            // For a simple backup import, we clear existing contacts and insert new ones
            db.delete(DatabaseHelper.TABLE_CONTACTS, null, null)
            for (contact in importedContacts) {
                val numbers = if (contact.numbers.isNotEmpty()) {
                    contact.numbers
                } else if (contact.phoneNumber.isNotEmpty()) {
                    listOf(contact.phoneNumber)
                } else {
                    emptyList()
                }
                dbHelper.insertContact(contact.name, numbers)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        
        loadContacts()
    }

    // --- CRUD Dialogs ---

    private fun showContactDialog(position: Int?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_contact, null)
        val editName = dialogView.findViewById<EditText>(R.id.editName)
        val phoneContainer = dialogView.findViewById<LinearLayout>(R.id.phoneContainer)
        val btnAddPhone = dialogView.findViewById<Button>(R.id.btnAddPhoneField)

        val phoneFieldViews = mutableListOf<View>()

        fun addPhoneField(number: String = "") {
            val fieldView = LayoutInflater.from(this).inflate(R.layout.item_phone_field, phoneContainer, false)
            val editPhone = fieldView.findViewById<EditText>(R.id.editPhone)
            val btnRemove = fieldView.findViewById<View>(R.id.btnRemovePhone)
            
            editPhone.setText(number)
            
            btnRemove.setOnClickListener {
                if (phoneFieldViews.size > 1) {
                    phoneContainer.removeView(fieldView)
                    phoneFieldViews.remove(fieldView)
                } else {
                    Toast.makeText(this, "At least one number is required", Toast.LENGTH_SHORT).show()
                }
            }
            
            phoneContainer.addView(fieldView)
            phoneFieldViews.add(fieldView)
        }

        val existingContact = position?.let { contacts[it] }
        if (existingContact != null) {
            editName.setText(existingContact.name)
            if (existingContact.numbers.isNotEmpty()) {
                existingContact.numbers.forEach { addPhoneField(it) }
            } else {
                addPhoneField(existingContact.phoneNumber)
            }
        } else {
            addPhoneField()
        }

        btnAddPhone.setOnClickListener {
            addPhoneField()
        }

        AlertDialog.Builder(this)
            .setTitle(if (position == null) R.string.dialog_title_add else R.string.dialog_title_edit)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_save) { _, _ ->
                val name = editName.text.toString().trim()
                val numbers = phoneFieldViews.map { 
                    it.findViewById<EditText>(R.id.editPhone).text.toString().trim() 
                }.filter { it.isNotEmpty() }

                if (name.isNotEmpty() && numbers.isNotEmpty()) {
                    if (existingContact != null) {
                        dbHelper.updateContact(existingContact.id, name, numbers)
                    } else {
                        dbHelper.insertContact(name, numbers)
                    }
                    loadContacts()
                } else {
                    Toast.makeText(this, "Name and at least one number required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun showDeleteConfirmation(position: Int) {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_title_delete)
            .setMessage(R.string.dialog_message_delete)
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                val contact = contacts[position]
                dbHelper.deleteContact(contact.id)
                loadContacts()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }
}
