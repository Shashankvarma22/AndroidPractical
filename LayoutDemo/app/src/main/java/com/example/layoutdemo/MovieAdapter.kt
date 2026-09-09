package com.example.layoutdemo

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MovieAdapter(
    private val movies: List<Movie>
) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    class MovieViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        val image: ImageView =
            itemView.findViewById(android.R.id.icon)

        val text: TextView =
            itemView.findViewById(android.R.id.text1)
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MovieViewHolder {

        val layout =
            LinearLayout(parent.context)

        layout.orientation =
            LinearLayout.HORIZONTAL

        layout.setPadding(
            16,
            16,
            16,
            16
        )

        val image =
            ImageView(parent.context)

        image.id = android.R.id.icon

        image.layoutParams =
            LinearLayout.LayoutParams(
                150,
                200
            )

        val text =
            TextView(parent.context)

        text.id = android.R.id.text1

        text.textSize = 18f

        text.setPadding(
            20,
            10,
            10,
            10
        )

        layout.addView(image)

        layout.addView(text)

        return MovieViewHolder(layout)
    }


    override fun onBindViewHolder(
        holder: MovieViewHolder,
        position: Int
    ) {

        val movie =
            movies[position]

        holder.image.setImageResource(
            movie.image
        )

        holder.text.text =
            "${movie.name}\nYear: ${movie.year}"
    }


    override fun getItemCount(): Int {
        return movies.size
    }
}
