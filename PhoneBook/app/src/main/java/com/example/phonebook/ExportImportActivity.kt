package com.example.phonebook

import android.Manifest
import android.app.ProgressDialog
import android.app.role.RoleManager
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Telephony
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStreamReader

class ExportImportActivity : AppCompatActivity() {

    private val gson = Gson()

    private val contactPermissions = arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
    private val smsPermissions = arrayOf(Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS)

    private var pendingOperation: String? = null

    // SAF Launchers
    private val exportContactsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) result.data?.data?.let { uri -> exportContactsToUri(uri) }
    }
    private val exportSmsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) result.data?.data?.let { uri -> exportSmsToUri(uri) }
    }
    private val importContactsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) result.data?.data?.let { uri -> importContactsFromUri(uri) }
    }
    private val importSmsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                importSmsFromUri(uri)
            }
        }
    }

    private val roleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        if (isDefaultSmsApp()) {
            triggerImportSms()
        } else {
            Toast.makeText(this, "Default SMS app role required for import", Toast.LENGTH_SHORT).show()
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.all { it.value }) {
            when (pendingOperation) {
                "EXPORT_PHONE" -> triggerExportContacts()
                "IMPORT_PHONE" -> triggerImportContacts()
                "EXPORT_SMS" -> triggerExportSms()
                "IMPORT_SMS" -> handleImportSmsFlow()
            }
        } else {
            Toast.makeText(this, R.string.error_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_export_import)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.export_import_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<Button>(R.id.btnExportPhone).setOnClickListener { checkPermissionsAndRun("EXPORT_PHONE") }
        findViewById<Button>(R.id.btnExportSms).setOnClickListener { checkPermissionsAndRun("EXPORT_SMS") }
        findViewById<Button>(R.id.btnImportPhone).setOnClickListener { checkPermissionsAndRun("IMPORT_PHONE") }
        findViewById<Button>(R.id.btnImportSms).setOnClickListener { checkPermissionsAndRun("IMPORT_SMS") }
    }

    private fun checkPermissionsAndRun(operation: String) {
        pendingOperation = operation
        val permissions = if (operation.contains("PHONE")) contactPermissions else smsPermissions
        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            when (operation) {
                "EXPORT_PHONE" -> triggerExportContacts()
                "IMPORT_PHONE" -> triggerImportContacts()
                "EXPORT_SMS" -> triggerExportSms()
                "IMPORT_SMS" -> handleImportSmsFlow()
            }
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    // --- Contacts Logic ---

    private fun triggerExportContacts() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "phone_contacts.json")
        }
        exportContactsLauncher.launch(intent)
    }

    private fun triggerImportContacts() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        importContactsLauncher.launch(intent)
    }

    private fun exportContactsToUri(uri: Uri) {
        val contacts = mutableListOf<Contact>()
        val cursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null)
        cursor?.use {
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                contacts.add(Contact(name = it.getString(nameIdx), numbers = listOf(it.getString(numberIdx))))
            }
        }
        writeJsonToUri(uri, contacts, R.string.msg_contacts_exported)
    }

    private fun importContactsFromUri(uri: Uri) {
        try {
            val json = readTextFromUri(uri)
            val type = object : TypeToken<List<Contact>>() {}.type
            val list: List<Contact> = gson.fromJson(json, type)
            
            list.forEach { contact ->
                val ops = ArrayList<ContentProviderOperation>()
                ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build())
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.name)
                    .build())
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, contact.phoneNumber)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build())
                contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            }
            Toast.makeText(this, R.string.msg_contacts_imported, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.error_import_failed, Toast.LENGTH_SHORT).show()
        }
    }

    // --- SMS Logic ---

    private fun isDefaultSmsApp(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            roleManager.isRoleHeld(RoleManager.ROLE_SMS)
        } else {
            Telephony.Sms.getDefaultSmsPackage(this) == packageName
        }
    }

    private fun handleImportSmsFlow() {
        if (isDefaultSmsApp()) {
            triggerImportSms()
        } else {
            requestSmsRole()
        }
    }

    private fun requestSmsRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS))
            } else {
                Toast.makeText(this, "SMS Role not available", Toast.LENGTH_SHORT).show()
            }
        } else {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            roleLauncher.launch(intent)
        }
    }

    private fun triggerExportSms() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "sms_messages.json")
        }
        exportSmsLauncher.launch(intent)
    }

    private fun triggerImportSms() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        importSmsLauncher.launch(intent)
    }

    private fun exportSmsToUri(uri: Uri) {
        val smsList = mutableListOf<Sms>()
        val cursor = contentResolver.query(Telephony.Sms.CONTENT_URI, null, null, null, null)
        cursor?.use {
            val addressIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
            val readIdx = it.getColumnIndex(Telephony.Sms.READ)
            val seenIdx = it.getColumnIndex(Telephony.Sms.SEEN)
            val typeIdx = it.getColumnIndex(Telephony.Sms.TYPE)
            val dateSentIdx = it.getColumnIndex(Telephony.Sms.DATE_SENT)
            val statusIdx = it.getColumnIndex(Telephony.Sms.STATUS)
            
            while (it.moveToNext()) {
                smsList.add(Sms(
                    phoneNumber = it.getString(addressIdx) ?: "",
                    message = it.getString(bodyIdx) ?: "",
                    date = it.getLong(dateIdx),
                    read = it.getInt(readIdx),
                    seen = it.getInt(seenIdx),
                    type = it.getInt(typeIdx),
                    dateSent = if (dateSentIdx != -1) it.getLong(dateSentIdx) else 0L,
                    status = if (statusIdx != -1) it.getInt(statusIdx) else -1
                ))
            }
        }
        writeJsonToUri(uri, smsList, R.string.msg_sms_exported_device)
    }

    private fun importSmsFromUri(uri: Uri) {
        val progressDialog = ProgressDialog(this).apply {
            setTitle("SMS Import")
            setMessage("Reading SMS file...")
            setCancelable(false)
            show()
        }

        Thread {
            try {
                val json = readTextFromUri(uri)
                val typeToken = object : TypeToken<List<Sms>>() {}.type
                val list: List<Sms> = gson.fromJson(json, typeToken)
                val total = list.size
                
                runOnUiThread {
                    progressDialog.setMessage("$total messages found")
                }
                
                Thread.sleep(600)

                var successCount = 0
                var failCount = 0
                
                list.forEachIndexed { index, sms ->
                    runOnUiThread {
                        progressDialog.setMessage("Importing ${index + 1} of $total...")
                    }

                    // Normalize Data
                    val normAddress = sms.phoneNumber
                    val normBody = sms.message
                    val normDate = if (sms.date > 0) sms.date else System.currentTimeMillis()
                    val normDateSent = if (sms.dateSent > 0) sms.dateSent else normDate
                    val normType = if (sms.type > 0) sms.type else Telephony.Sms.MESSAGE_TYPE_INBOX
                    val normRead = 1
                    val normSeen = 1
                    val normStatus = if (sms.status != -1) sms.status else Telephony.Sms.STATUS_NONE

                    val values = ContentValues().apply {
                        put(Telephony.Sms.ADDRESS, normAddress)
                        put(Telephony.Sms.BODY, normBody)
                        put(Telephony.Sms.DATE, normDate)
                        put(Telephony.Sms.DATE_SENT, normDateSent)
                        put(Telephony.Sms.READ, normRead)
                        put(Telephony.Sms.SEEN, normSeen)
                        put(Telephony.Sms.TYPE, normType)
                        put(Telephony.Sms.STATUS, normStatus)
                    }
                    
                    val insertUri = when (normType) {
                        Telephony.Sms.MESSAGE_TYPE_SENT -> Telephony.Sms.Sent.CONTENT_URI
                        Telephony.Sms.MESSAGE_TYPE_DRAFT -> Telephony.Sms.Draft.CONTENT_URI
                        else -> Telephony.Sms.Inbox.CONTENT_URI
                    }
                    
                    Log.d("SMS_IMPORT", "Attempting import: address=$normAddress, date=$normDate, type=$normType, read=$normRead, seen=$normSeen")
                    
                    try {
                        val resultUri = contentResolver.insert(insertUri, values)
                        if (resultUri != null) {
                            successCount++
                            Log.d("SMS_IMPORT", "Successfully inserted: $resultUri")
                        } else {
                            failCount++
                            Log.e("SMS_IMPORT", "Insert returned null for message from $normAddress")
                        }
                    } catch (e: Exception) {
                        failCount++
                        Log.e("SMS_IMPORT", "Insert failed for message from $normAddress", e)
                    }
                }
                
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Imported $successCount messages, $failCount failed", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("SMS_IMPORT", "Import error", e)
                runOnUiThread {
                    if (progressDialog.isShowing) progressDialog.dismiss()
                    Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    // --- Helper Methods ---

    private fun writeJsonToUri(uri: Uri, data: Any, successResId: Int) {
        try {
            contentResolver.openOutputStream(uri)?.use { os ->
                os.write(gson.toJson(data).toByteArray())
            }
            Toast.makeText(this, successResId, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.error_export_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun readTextFromUri(uri: Uri): String {
        val sb = StringBuilder()
        contentResolver.openInputStream(uri)?.use { isStream ->
            BufferedReader(InputStreamReader(isStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    sb.append(line)
                    line = reader.readLine()
                }
            }
        }
        return sb.toString()
    }
}
