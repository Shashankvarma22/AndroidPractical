package com.example.phonebook

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri

class PhoneBookProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.example.phonebook.provider"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/contacts")

        private const val CONTACTS = 1
        private const val CONTACT_ID = 2

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "contacts", CONTACTS)
            addURI(AUTHORITY, "contacts/#", CONTACT_ID)
        }
    }

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(): Boolean {
        dbHelper = DatabaseHelper(context!!)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val db = dbHelper.readableDatabase
        val qb = SQLiteQueryBuilder()
        qb.tables = "${DatabaseHelper.TABLE_CONTACTS} c LEFT JOIN ${DatabaseHelper.TABLE_PHONE_NUMBERS} p ON c.${DatabaseHelper.COLUMN_ID} = p.${DatabaseHelper.COLUMN_PN_CONTACT_ID}"

        val projectionMap = mapOf(
            "_id" to "c.${DatabaseHelper.COLUMN_ID} AS _id",
            "name" to "c.${DatabaseHelper.COLUMN_CONTACT_NAME} AS name",
            "number" to "p.${DatabaseHelper.COLUMN_PN_NUMBER} AS number",
            "phone_number" to "p.${DatabaseHelper.COLUMN_PN_NUMBER} AS phone_number",
            "phoneNumber" to "p.${DatabaseHelper.COLUMN_PN_NUMBER} AS phoneNumber"
        )
        qb.setProjectionMap(projectionMap)

        when (uriMatcher.match(uri)) {
            CONTACTS -> { }
            CONTACT_ID -> {
                val id = ContentUris.parseId(uri)
                qb.appendWhere("c.${DatabaseHelper.COLUMN_ID} = $id")
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        val cursor = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder)
        cursor.setNotificationUri(context?.contentResolver, uri)
        return cursor
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            CONTACTS -> "vnd.android.cursor.dir/vnd.$AUTHORITY.contacts"
            CONTACT_ID -> "vnd.android.cursor.item/vnd.$AUTHORITY.contacts"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        when (uriMatcher.match(uri)) {
            CONTACTS -> {
                val name = values?.getAsString("name")
                    ?: values?.getAsString(DatabaseHelper.COLUMN_CONTACT_NAME)
                    ?: ""
                val numberStr = values?.getAsString("number")
                    ?: values?.getAsString("numbers")
                    ?: values?.getAsString("phone_number")
                    ?: values?.getAsString("phoneNumber")
                    ?: ""
                val numbers = if (numberStr.isNotEmpty()) {
                    numberStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                } else {
                    emptyList()
                }

                val id = dbHelper.insertContact(name, numbers)
                if (id == -1L) return null

                val itemUri = ContentUris.withAppendedId(CONTENT_URI, id)
                context?.contentResolver?.notifyChange(uri, null)
                context?.contentResolver?.notifyChange(itemUri, null)
                return itemUri
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        var count = 0
        when (uriMatcher.match(uri)) {
            CONTACTS -> {
                val db = dbHelper.readableDatabase
                val cursor = db.query(
                    DatabaseHelper.TABLE_CONTACTS,
                    arrayOf(DatabaseHelper.COLUMN_ID),
                    selection,
                    selectionArgs,
                    null,
                    null,
                    null
                )
                cursor.use {
                    while (it.moveToNext()) {
                        val id = it.getLong(0)
                        dbHelper.deleteContact(id)
                        count++
                    }
                }
            }
            CONTACT_ID -> {
                val id = ContentUris.parseId(uri)
                dbHelper.deleteContact(id)
                count = 1
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        if (count > 0) {
            context?.contentResolver?.notifyChange(uri, null)
        }
        return count
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int {
        var count = 0
        val targetIds = mutableListOf<Long>()

        when (uriMatcher.match(uri)) {
            CONTACTS -> {
                val db = dbHelper.readableDatabase
                val cursor = db.query(
                    DatabaseHelper.TABLE_CONTACTS,
                    arrayOf(DatabaseHelper.COLUMN_ID),
                    selection,
                    selectionArgs,
                    null,
                    null,
                    null
                )
                cursor.use {
                    while (it.moveToNext()) {
                        targetIds.add(it.getLong(0))
                    }
                }
            }
            CONTACT_ID -> {
                targetIds.add(ContentUris.parseId(uri))
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        val allContacts = dbHelper.getAllContacts().associateBy { it.id }

        for (id in targetIds) {
            val existing = allContacts[id] ?: continue
            val newName = values?.getAsString("name")
                ?: values?.getAsString(DatabaseHelper.COLUMN_CONTACT_NAME)
                ?: existing.name

            val hasNumberKey = values != null && (
                values.containsKey("number") ||
                values.containsKey("numbers") ||
                values.containsKey("phone_number") ||
                values.containsKey("phoneNumber")
            )

            val newNumbers = if (values != null && hasNumberKey) {
                val numberStr = values.getAsString("number")
                    ?: values.getAsString("numbers")
                    ?: values.getAsString("phone_number")
                    ?: values.getAsString("phoneNumber")
                    ?: ""
                if (numberStr.isNotEmpty()) {
                    numberStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                } else emptyList()
            } else {
                existing.numbers
            }

            dbHelper.updateContact(id, newName, newNumbers)
            count++
        }

        if (count > 0) {
            context?.contentResolver?.notifyChange(uri, null)
        }
        return count
    }
}
