package com.example.phonebook

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
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
        loadContacts()
        setupRecyclerView()

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            showContactDialog(null)
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
        val json = prefs.getString("contacts_list", null)
        if (json != null) {
            val type = object : TypeToken<List<Contact>>() {}.type
            val savedContacts: List<Contact> = gson.fromJson(json, type)
            contacts.clear()
            contacts.addAll(savedContacts)
        } else {
            contacts.addAll(listOf(
                Contact("John Doe", "123-456-7890"),
                Contact("Jane Smith", "987-654-3210"),
                Contact("Bob Johnson", "555-123-4567")
            ))
        }
    }

    private fun saveContacts() {
        prefs.edit().putString("contacts_list", gson.toJson(contacts)).apply()
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
        contacts.clear()
        contacts.addAll(importedContacts)
        adapter.notifyDataSetChanged()
        saveContacts()
    }

    // --- CRUD Dialogs ---

    private fun showContactDialog(position: Int?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_contact, null)
        val editName = dialogView.findViewById<EditText>(R.id.editName)
        val editPhone = dialogView.findViewById<EditText>(R.id.editPhone)

        position?.let {
            val contact = contacts[it]
            editName.setText(contact.name)
            editPhone.setText(contact.phoneNumber)
        }

        AlertDialog.Builder(this)
            .setTitle(if (position == null) R.string.dialog_title_add else R.string.dialog_title_edit)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_save) { _, _ ->
                val name = editName.text.toString().trim()
                val phone = editPhone.text.toString().trim()

                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    if (position != null) {
                        contacts[position] = Contact(name, phone)
                        adapter.notifyItemChanged(position)
                    } else {
                        contacts.add(Contact(name, phone))
                        adapter.notifyItemInserted(contacts.size - 1)
                    }
                    saveContacts()
                } else {
                    Toast.makeText(this, R.string.error_empty_fields, Toast.LENGTH_SHORT).show()
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
                contacts.removeAt(position)
                adapter.notifyItemRemoved(position)
                saveContacts()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }
}
