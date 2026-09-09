package com.example.layoutdemo.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.layoutdemo.Movie
import com.example.layoutdemo.MovieAdapter
import com.example.layoutdemo.R

class RecycleViewFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_recycle, container, false)
        val recycleView = view.findViewById<RecyclerView>(R.id.recycleView)
        
        recycleView.layoutManager = LinearLayoutManager(requireContext())
        recycleView.adapter = MovieAdapter(Movie.getMovieList())
        
        return view
    }
}
