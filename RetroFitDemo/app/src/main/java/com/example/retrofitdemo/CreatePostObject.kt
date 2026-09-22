package com.example.retrofitdemo

data class CreatePostObject(
    val userId: Int,
    val title: String,
    val body: String
)