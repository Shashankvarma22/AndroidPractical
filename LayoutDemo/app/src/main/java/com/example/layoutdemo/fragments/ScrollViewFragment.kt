package com.example.layoutdemo.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.layoutdemo.Movie
import com.example.layoutdemo.R

class ScrollViewFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scroll, container, false)
        val cardContainer = view.findViewById<LinearLayout>(R.id.cardContainer)
        
        populateCards(cardContainer, Movie.getMovieList())
        
        return view
    }

    private fun populateCards(container: LinearLayout, movies: List<Movie>) {
        for (movie in movies) {
            val card = CardView(requireContext())
            card.radius = 12f
            card.cardElevation = 6f

            val layout = LinearLayout(requireContext())
            layout.orientation = LinearLayout.HORIZONTAL
            layout.setPadding(16, 16, 16, 16)

            val image = ImageView(requireContext())
            image.setImageResource(movie.image)
            image.layoutParams = LinearLayout.LayoutParams(180, 240)

            val text = TextView(requireContext())
            text.text = "${movie.name}\nYear: ${movie.year}"
            text.textSize = 18f
            text.setPadding(20, 10, 10, 10)

            layout.addView(image)
            layout.addView(text)
            card.addView(layout)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 16)
            card.layoutParams = params

            container.addView(card)
        }
    }
}
