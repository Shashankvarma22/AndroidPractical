package com.example.phonebook

import android.content.Context
import android.net.Uri

object ContactLookup {
    private const val AUTHORITY = "com.example.phonebook.provider"
    val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/contacts")

    fun getContactName(context: Context, phoneNumber: String): String? {
        if (phoneNumber.isBlank()) return null
        return try {
            context.contentResolver.query(
                CONTENT_URI,
                arrayOf("name", "number"),
                "number = ?",
                arrayOf(phoneNumber),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex("name")
                    if (nameIdx != -1 && !cursor.isNull(nameIdx)) {
                        val name = cursor.getString(nameIdx)
                        if (name.isNotBlank()) name else null
                    } else null
                } else null
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactLookup", "Failed to resolve contact name via ContentResolver", e)
            null
        }
    }
}
