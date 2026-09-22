package com.example.phonebook

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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

        migrateSmsIfNeeded()
        loadSms()
        setupRecyclerView()

        findViewById<FloatingActionButton>(R.id.fabAddSms).setOnClickListener {
            showNewSmsDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        loadSms()
    }

    private fun migrateSmsIfNeeded() {
        if (prefs.getBoolean("sms_db_migrated", false)) return

        val json = prefs.getString("sms_list", null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<Sms>>() {}.type
                val oldSmsList: List<Sms> = gson.fromJson(json, type)
                
                val db = dbHelper.writableDatabase
                db.beginTransaction()
                try {
                    for (oldSms in oldSmsList) {
                        dbHelper.insertSms(oldSms)
                    }
                    db.setTransactionSuccessful()
                    prefs.edit().putBoolean("sms_db_migrated", true).apply()
                } finally {
                    db.endTransaction()
                }
            } catch (e: Exception) {
                android.util.Log.e("SmsActivity", "SMS Migration failed", e)
            }
        } else {
            prefs.edit().putBoolean("sms_db_migrated", true).apply()
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
            onItemClick = { position ->
                val sms = smsList[position]
                val intent = Intent(this, ConversationActivity::class.java).apply {
                    putExtra("address", sms.phoneNumber)
                    putExtra("contactName", sms.contactName)
                }
                startActivity(intent)
            },
            onDeleteClick = { position -> showDeleteConfirmation(position) }
        )
        findViewById<RecyclerView>(R.id.recyclerViewSms).adapter = adapter
    }

    private fun loadSms() {
        smsList.clear()
        val rawThreads = dbHelper.getConversationThreads()
        val resolvedThreads = rawThreads.map { sms ->
            val resolvedName = ContactLookup.getContactName(this, sms.phoneNumber)
            sms.copy(contactName = resolvedName)
        }
        smsList.addAll(resolvedThreads)
        if (smsList.isEmpty() && !prefs.getBoolean("sms_db_migrated", false)) {
            val defaults = listOf(
                Sms("123456", "Hello from SMS Manager!", date = System.currentTimeMillis(), type = 1),
                Sms("987654", "This is a test message.", date = System.currentTimeMillis(), type = 1)
            )
            for (sms in defaults) {
                dbHelper.insertSms(sms)
            }
            smsList.clear()
            val reloadedThreads = dbHelper.getConversationThreads().map { sms ->
                val resolvedName = ContactLookup.getContactName(this, sms.phoneNumber)
                sms.copy(contactName = resolvedName)
            }
            smsList.addAll(reloadedThreads)
        }
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
    }



    // --- Original App-Specific Export/Import ---

    private fun exportSmsToFile() {
        try {
            val allSms = dbHelper.getAllSms()
            val file = File(getExternalFilesDir(null), "sms_backup.json")
            file.writeText(gson.toJson(allSms))
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
            val allSms = dbHelper.getAllSms()
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(gson.toJson(allSms).toByteArray())
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
        
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(DatabaseHelper.TABLE_MESSAGES, null, null)
            for (sms in importedSms) {
                dbHelper.insertSms(sms)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        loadSms()
    }

    // --- CRUD Dialogs ---

    private fun showNewSmsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sms, null)
        val editPhone = dialogView.findViewById<AutoCompleteTextView>(R.id.editSmsPhone)
        val editMessage = dialogView.findViewById<EditText>(R.id.editSmsMessage)

        // Query PhoneBookProvider via ContentResolver for contact suggestions
        val contactDisplayList = mutableListOf<String>()
        val contactNumberMap = mutableMapOf<String, String>()

        try {
            contentResolver.query(
                ContactLookup.CONTENT_URI,
                arrayOf("name", "number"),
                null,
                null,
                null
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex("name")
                val numberIdx = cursor.getColumnIndex("number")

                while (cursor.moveToNext()) {
                    val name = if (nameIdx != -1 && !cursor.isNull(nameIdx)) cursor.getString(nameIdx) else ""
                    val number = if (numberIdx != -1 && !cursor.isNull(numberIdx)) cursor.getString(numberIdx) else ""

                    if (number.isNotBlank()) {
                        val display = if (name.isNotBlank()) "$name ($number)" else number
                        contactDisplayList.add(display)
                        contactNumberMap[display] = number
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SmsActivity", "Failed to query contacts for SMS dropdown", e)
        }

        if (contactDisplayList.isNotEmpty()) {
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, contactDisplayList)
            editPhone.setAdapter(adapter)
            editPhone.threshold = 0
            editPhone.setOnClickListener {
                editPhone.showDropDown()
            }
            editPhone.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    editPhone.showDropDown()
                }
            }
            editPhone.setOnItemClickListener { parent, _, position, _ ->
                val selectedDisplay = parent.getItemAtPosition(position).toString()
                val selectedNumber = contactNumberMap[selectedDisplay]
                    ?: selectedDisplay.substringAfterLast("(").substringBefore(")").trim()
                editPhone.setText(selectedNumber)
                editPhone.setSelection(selectedNumber.length)
            }
        }

        AlertDialog.Builder(this)
            .setTitle("New SMS")
            .setView(dialogView)
            .setPositiveButton("Send") { _, _ ->
                val phone = editPhone.text.toString().trim()
                val message = editMessage.text.toString().trim()

                if (phone.isNotEmpty() && message.isNotEmpty()) {
                    dbHelper.insertSms(Sms(phoneNumber = phone, message = message, date = System.currentTimeMillis(), type = 2))
                    loadSms()
                } else {
                    Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmation(position: Int) {
        val sms = smsList[position]
        AlertDialog.Builder(this)
            .setTitle("Delete Conversation")
            .setMessage("Are you sure you want to delete all messages with ${sms.contactName ?: sms.phoneNumber}?")
            .setPositiveButton("Delete") { _, _ ->
                dbHelper.deleteConversation(sms.phoneNumber)
                loadSms()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
