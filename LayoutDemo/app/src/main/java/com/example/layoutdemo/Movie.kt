package com.example.layoutdemo

data class Movie(
    val name: String,
    val year: Int,
    val image: Int
) {
    companion object {
        fun getMovieList(): List<Movie> {
            return listOf(
                Movie("Demon Slayer", 2025, R.drawable.demon_slayer),
                Movie("Avengers End Game", 2019, R.drawable.avengers_endgame),
                Movie("Ramayana", 2026, R.drawable.ramayana),
                Movie("Bang Bang", 2014, R.drawable.bang_bang),
                Movie("Avengers DoomsDay", 2026, R.drawable.avengers_doomsday)
            )
        }
    }
}