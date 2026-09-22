package com.example.phonebook

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "phonebook.db"
        private const val DATABASE_VERSION = 1

        // Table Names
        const val TABLE_CONTACTS = "contacts"
        const val TABLE_PHONE_NUMBERS = "phone_numbers"
        const val TABLE_MESSAGES = "messages"

        // Common Column
        const val COLUMN_ID = "_id"

        // Contacts Columns
        const val COLUMN_CONTACT_NAME = "name"

        // Phone Numbers Columns
        const val COLUMN_PN_CONTACT_ID = "contact_id"
        const val COLUMN_PN_NUMBER = "number"

        // Messages Columns
        const val COLUMN_MSG_CONTACT_ID = "contact_id"
        const val COLUMN_MSG_ADDRESS = "address"
        const val COLUMN_MSG_BODY = "body"
        const val COLUMN_MSG_DATE = "date"
        const val COLUMN_MSG_TYPE = "type"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createContactsTable = ("CREATE TABLE $TABLE_CONTACTS (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_CONTACT_NAME TEXT NOT NULL)")

        val createPhoneNumbersTable = ("CREATE TABLE $TABLE_PHONE_NUMBERS (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_PN_CONTACT_ID INTEGER, " +
                "$COLUMN_PN_NUMBER TEXT NOT NULL, " +
                "FOREIGN KEY($COLUMN_PN_CONTACT_ID) REFERENCES $TABLE_CONTACTS($COLUMN_ID) ON DELETE CASCADE)")

        val createMessagesTable = ("CREATE TABLE $TABLE_MESSAGES (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_MSG_CONTACT_ID INTEGER, " +
                "$COLUMN_MSG_ADDRESS TEXT NOT NULL, " +
                "$COLUMN_MSG_BODY TEXT, " +
                "$COLUMN_MSG_DATE INTEGER, " +
                "$COLUMN_MSG_TYPE INTEGER, " +
                "FOREIGN KEY($COLUMN_MSG_CONTACT_ID) REFERENCES $TABLE_CONTACTS($COLUMN_ID) ON DELETE SET NULL)")

        db.execSQL(createContactsTable)
        db.execSQL(createPhoneNumbersTable)
        db.execSQL(createMessagesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Minimal upgrade strategy
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PHONE_NUMBERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CONTACTS")
        onCreate(db)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    // --- Helper Methods ---

    fun insertContact(name: String, numbers: List<String>): Long {
        val db = this.writableDatabase
        db.beginTransaction()
        var contactId: Long = -1
        try {
            val contactValues = ContentValues().apply {
                put(COLUMN_CONTACT_NAME, name)
            }
            contactId = db.insert(TABLE_CONTACTS, null, contactValues)

            if (contactId != -1L) {
                for (number in numbers) {
                    val pnValues = ContentValues().apply {
                        put(COLUMN_PN_CONTACT_ID, contactId)
                        put(COLUMN_PN_NUMBER, number)
                    }
                    db.insert(TABLE_PHONE_NUMBERS, null, pnValues)
                }
                linkMessagesToContact(db, contactId, numbers)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return contactId
    }

    fun updateContact(id: Long, name: String, numbers: List<String>) {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            val contactValues = ContentValues().apply {
                put(COLUMN_CONTACT_NAME, name)
            }
            db.update(TABLE_CONTACTS, contactValues, "$COLUMN_ID = ?", arrayOf(id.toString()))

            // Refresh phone numbers: delete old and insert new
            db.delete(TABLE_PHONE_NUMBERS, "$COLUMN_PN_CONTACT_ID = ?", arrayOf(id.toString()))
            for (number in numbers) {
                val pnValues = ContentValues().apply {
                    put(COLUMN_PN_CONTACT_ID, id)
                    put(COLUMN_PN_NUMBER, number)
                }
                db.insert(TABLE_PHONE_NUMBERS, null, pnValues)
            }
            
            // Relink existing messages
            linkMessagesToContact(db, id, numbers)
            
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteContact(id: Long) {
        val db = this.writableDatabase
        db.delete(TABLE_CONTACTS, "$COLUMN_ID = ?", arrayOf(id.toString()))
        // phone_numbers are deleted via CASCADE
        // messages.contact_id is set to NULL via SET NULL
    }

    fun getAllContacts(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val db = this.readableDatabase
        
        val cursor = db.rawQuery("SELECT * FROM $TABLE_CONTACTS", null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTACT_NAME))
                
                // Get numbers for this contact
                val numbers = mutableListOf<String>()
                val numCursor = db.query(TABLE_PHONE_NUMBERS, arrayOf(COLUMN_PN_NUMBER), 
                    "$COLUMN_PN_CONTACT_ID = ?", arrayOf(id.toString()), null, null, null)
                
                if (numCursor.moveToFirst()) {
                    do {
                        numbers.add(numCursor.getString(0))
                    } while (numCursor.moveToNext())
                }
                numCursor.close()
                
                contacts.add(Contact(id, name, numbers))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return contacts
    }

    fun insertSms(sms: Sms): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_MSG_ADDRESS, sms.phoneNumber)
            put(COLUMN_MSG_BODY, sms.message)
            put(COLUMN_MSG_DATE, sms.date)
            put(COLUMN_MSG_TYPE, sms.type)
            
            // Try to find matching contact
            val contactId = findContactIdByNumber(db, sms.phoneNumber)
            if (contactId != null) {
                put(COLUMN_MSG_CONTACT_ID, contactId)
            }
        }
        return db.insert(TABLE_MESSAGES, null, values)
    }

    fun deleteSms(id: Long) {
        val db = this.writableDatabase
        db.delete(TABLE_MESSAGES, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun deleteConversation(address: String) {
        val db = this.writableDatabase
        db.delete(TABLE_MESSAGES, "$COLUMN_MSG_ADDRESS = ?", arrayOf(address))
    }

    fun getAllSms(): List<Sms> {
        return getSmsList("SELECT m.*, c.$COLUMN_CONTACT_NAME " +
                "FROM $TABLE_MESSAGES m " +
                "LEFT JOIN $TABLE_CONTACTS c ON m.$COLUMN_MSG_CONTACT_ID = c.$COLUMN_ID " +
                "ORDER BY m.$COLUMN_MSG_DATE DESC")
    }

    fun getConversationThreads(): List<Sms> {
        val query = "SELECT m.*, c.$COLUMN_CONTACT_NAME " +
                "FROM $TABLE_MESSAGES m " +
                "LEFT JOIN $TABLE_CONTACTS c ON m.$COLUMN_MSG_CONTACT_ID = c.$COLUMN_ID " +
                "WHERE m.$COLUMN_ID IN (" +
                "SELECT $COLUMN_ID FROM $TABLE_MESSAGES m1 " +
                "WHERE m1.$COLUMN_MSG_DATE = (" +
                "SELECT MAX(m2.$COLUMN_MSG_DATE) FROM $TABLE_MESSAGES m2 WHERE m2.$COLUMN_MSG_ADDRESS = m1.$COLUMN_MSG_ADDRESS" +
                ") " +
                "GROUP BY m1.$COLUMN_MSG_ADDRESS" +
                ") " +
                "ORDER BY m.$COLUMN_MSG_DATE DESC"
        return getSmsList(query)
    }

    fun getMessagesByAddress(address: String): List<Sms> {
        val query = "SELECT m.*, c.$COLUMN_CONTACT_NAME " +
                "FROM $TABLE_MESSAGES m " +
                "LEFT JOIN $TABLE_CONTACTS c ON m.$COLUMN_MSG_CONTACT_ID = c.$COLUMN_ID " +
                "WHERE m.$COLUMN_MSG_ADDRESS = ? " +
                "ORDER BY m.$COLUMN_MSG_DATE ASC"
        return getSmsList(query, arrayOf(address))
    }

    private fun getSmsList(query: String, args: Array<String>? = null): List<Sms> {
        val smsList = mutableListOf<Sms>()
        val db = this.readableDatabase
        val cursor = db.rawQuery(query, args)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val address = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MSG_ADDRESS))
                val body = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MSG_BODY))
                val date = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_MSG_DATE))
                val type = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MSG_TYPE))
                val contactId = if (!cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_MSG_CONTACT_ID))) {
                    cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_MSG_CONTACT_ID))
                } else null
                val contactName = if (!cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_CONTACT_NAME))) {
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTACT_NAME))
                } else null

                smsList.add(Sms(
                    phoneNumber = address,
                    message = body,
                    date = date,
                    type = type,
                    contactId = contactId,
                    contactName = contactName,
                    id = id
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return smsList
    }

    private fun findContactIdByNumber(db: SQLiteDatabase, number: String): Long? {
        val cursor = db.query(TABLE_PHONE_NUMBERS, arrayOf(COLUMN_PN_CONTACT_ID),
            "$COLUMN_PN_NUMBER = ?", arrayOf(number), null, null, null)
        
        var contactId: Long? = null
        if (cursor.moveToFirst()) {
            contactId = cursor.getLong(0)
        }
        cursor.close()
        return contactId
    }

    private fun linkMessagesToContact(db: SQLiteDatabase, contactId: Long, numbers: List<String>) {
        val values = ContentValues().apply {
            put(COLUMN_MSG_CONTACT_ID, contactId)
        }
        for (number in numbers) {
            db.update(TABLE_MESSAGES, values, "$COLUMN_MSG_ADDRESS = ?", arrayOf(number))
        }
    }
}
