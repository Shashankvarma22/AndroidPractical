package com.example.phonebook

data class Sms(
    val phoneNumber: String,
    val message: String,
    val date: Long = 0,
    val read: Int = 0,
    val seen: Int = 0,
    val type: Int = 0,
    val dateSent: Long = 0,
    val status: Int = -1
)
