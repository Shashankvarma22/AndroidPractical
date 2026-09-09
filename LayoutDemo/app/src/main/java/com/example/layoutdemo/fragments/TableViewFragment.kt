package com.example.layoutdemo.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.layoutdemo.Movie
import com.example.layoutdemo.R

class TableViewFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_table, container, false)
        val tableView = view.findViewById<TableLayout>(R.id.tableView)
        
        populateTable(tableView, Movie.getMovieList())
        
        return view
    }

    private fun populateTable(table: TableLayout, movies: List<Movie>) {
        for (movie in movies) {
            val row = TableRow(requireContext())
            val image = ImageView(requireContext())
            image.setImageResource(movie.image)
            image.layoutParams = TableRow.LayoutParams(150, 200)

            val text = TextView(requireContext())
            text.text = "${movie.name}\nYear: ${movie.year}"
            text.textSize = 18f
            text.setPadding(20, 20, 20, 20)

            row.addView(image)
            row.addView(text)
            table.addView(row)
        }
    }
}
