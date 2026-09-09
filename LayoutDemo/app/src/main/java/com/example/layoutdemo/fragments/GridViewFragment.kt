package com.example.layoutdemo.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.layoutdemo.Movie
import com.example.layoutdemo.R

class GridViewFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_grid, container, false)
        val gridView = view.findViewById<GridView>(R.id.gridView)
        
        populateGrid(gridView, Movie.getMovieList())
        
        return view
    }

    private fun populateGrid(gridView: GridView, movies: List<Movie>) {
        val images = movies.map { it.image }

        gridView.adapter = object : BaseAdapter() {
            override fun getCount(): Int = movies.size
            override fun getItem(position: Int): Any = movies[position]
            override fun getItemId(position: Int): Long = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val imageView = ImageView(requireContext())
                imageView.setImageResource(images[position])
                imageView.layoutParams = AbsListView.LayoutParams(300, 350)
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                return imageView
            }
        }
    }
}
