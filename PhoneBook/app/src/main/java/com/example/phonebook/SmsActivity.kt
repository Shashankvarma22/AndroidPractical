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

class SmsActivity : AppCompatActivity() {

    private lateinit var adapter: SmsAdapter
    private val smsList = mutableListOf<Sms>()
    private val gson = Gson()
    private val prefs by lazy { getSharedPreferences("sms_prefs", MODE_PRIVATE) }

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
        setContentView(R.layout.activity_sms)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sms_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loadSms()
        setupRecyclerView()

        findViewById<FloatingActionButton>(R.id.fabAddSms).setOnClickListener {
            showSmsDialog(null)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.sms_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sms_export -> {
                exportSmsToFile()
                true
            }
            R.id.action_sms_import -> {
                importSmsFromFile()
                true
            }
            R.id.action_sms_saf_export -> {
                triggerSafExport()
                true
            }
            R.id.action_sms_saf_import -> {
                triggerSafImport()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        adapter = SmsAdapter(
            smsList,
            onEditClick = { position -> showSmsDialog(position) },
            onDeleteClick = { position -> showDeleteConfirmation(position) }
        )
        findViewById<RecyclerView>(R.id.recyclerViewSms).adapter = adapter
    }

    private fun loadSms() {
        val json = prefs.getString("sms_list", null)
        if (json != null) {
            val type = object : TypeToken<List<Sms>>() {}.type
            val savedSms: List<Sms> = gson.fromJson(json, type)
            smsList.clear()
            smsList.addAll(savedSms)
        } else {
            smsList.addAll(listOf(
                Sms("123456", "Hello from SMS Manager!"),
                Sms("987654", "This is a test message.")
            ))
        }
    }

    private fun saveSms() {
        prefs.edit().putString("sms_list", gson.toJson(smsList)).apply()
    }

    // --- Original App-Specific Export/Import ---

    private fun exportSmsToFile() {
        try {
            val file = File(getExternalFilesDir(null), "sms_backup.json")
            file.writeText(gson.toJson(smsList))
            Toast.makeText(this, R.string.msg_sms_exported, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.error_export_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun importSmsFromFile() {
        try {
            val file = File(getExternalFilesDir(null), "sms_backup.json")
            if (file.exists()) {
                val json = file.readText()
                updateSmsList(json)
                Toast.makeText(this, R.string.msg_sms_imported, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.error_sms_file_not_found, Toast.LENGTH_SHORT).show()
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
            putExtra(Intent.EXTRA_TITLE, "sms_backup.json")
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
                outputStream.write(gson.toJson(smsList).toByteArray())
            }
            Toast.makeText(this, R.string.msg_sms_saf_exported, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.error_export_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun readJsonFromUri(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val json = reader.readText()
                    updateSmsList(json)
                }
            }
            Toast.makeText(this, R.string.msg_sms_saf_imported, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.error_import_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSmsList(json: String) {
        val type = object : TypeToken<List<Sms>>() {}.type
        val importedSms: List<Sms> = gson.fromJson(json, type)
        smsList.clear()
        smsList.addAll(importedSms)
        adapter.notifyDataSetChanged()
        saveSms()
    }

    // --- CRUD Dialogs ---

    private fun showSmsDialog(position: Int?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sms, null)
        val editPhone = dialogView.findViewById<EditText>(R.id.editSmsPhone)
        val editMessage = dialogView.findViewById<EditText>(R.id.editSmsMessage)

        position?.let {
            val sms = smsList[it]
            editPhone.setText(sms.phoneNumber)
            editMessage.setText(sms.message)
        }

        AlertDialog.Builder(this)
            .setTitle(if (position == null) "New SMS" else "Edit SMS")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val phone = editPhone.text.toString().trim()
                val message = editMessage.text.toString().trim()

                if (phone.isNotEmpty() && message.isNotEmpty()) {
                    if (position != null) {
                        smsList[position] = Sms(phone, message)
                        adapter.notifyItemChanged(position)
                    } else {
                        smsList.add(Sms(phone, message))
                        adapter.notifyItemInserted(smsList.size - 1)
                    }
                    saveSms()
                } else {
                    Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmation(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete SMS")
            .setMessage("Are you sure you want to delete this SMS?")
            .setPositiveButton("Delete") { _, _ ->
                smsList.removeAt(position)
                adapter.notifyItemRemoved(position)
                saveSms()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
