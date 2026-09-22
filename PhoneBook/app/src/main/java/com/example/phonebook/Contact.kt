package com.example.phonebook

data class Contact(
    val id: Long = -1,
    val name: String,
    val numbers: List<String> = emptyList(),
    // Keep single phoneNumber for compatibility with existing code until Step 2/3
    // Note: We use a var here so that GSON can continue to populate the property from old JSON.
    val phoneNumber: String = if (numbers.isNotEmpty()) numbers[0] else ""
)
