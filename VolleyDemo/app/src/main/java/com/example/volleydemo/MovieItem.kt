package com.example.volleydemo

import java.io.Serializable

data class MovieItem(
    val title: String,
    val year: String,
    val imdbID: String,
    val type: String,
    val poster: String
) : Serializable