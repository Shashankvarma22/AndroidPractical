package com.example.volleydemo

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.RequestQueue
import com.android.volley.toolbox.ImageRequest

class MovieAdapter(
    private val movieList: List<MovieItem>,
    private val requestQueue: RequestQueue,
    private val onItemClick: (MovieItem) -> Unit
) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivItemPoster: ImageView = itemView.findViewById(R.id.ivItemPoster)
        val tvItemTitle: TextView = itemView.findViewById(R.id.tvItemTitle)
        val tvItemYearType: TextView = itemView.findViewById(R.id.tvItemYearType)
        val tvItemImdbId: TextView = itemView.findViewById(R.id.tvItemImdbId)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = movieList[position]
        val context = holder.itemView.context

        holder.tvItemTitle.text = movie.title
        holder.tvItemYearType.text = context.getString(R.string.label_year_type, movie.year, movie.type.replaceFirstChar { it.uppercase() })
        holder.tvItemImdbId.text = context.getString(R.string.label_imdb_id, movie.imdbID)

        holder.itemView.setOnClickListener {
            onItemClick(movie)
        }

        // Reset poster to placeholder before loading
        holder.ivItemPoster.setImageResource(android.R.drawable.ic_menu_gallery)
        if (movie.poster != "N/A" && movie.poster.isNotBlank()) {
            val imageRequest = ImageRequest(
                movie.poster,
                { bitmap ->
                    holder.ivItemPoster.setImageBitmap(bitmap)
                },
                0,
                0,
                ImageView.ScaleType.FIT_CENTER,
                Bitmap.Config.RGB_565,
                {
                    holder.ivItemPoster.setImageResource(android.R.drawable.ic_menu_report_image)
                }
            )
            requestQueue.add(imageRequest)
        }
    }

    override fun getItemCount(): Int = movieList.size
}