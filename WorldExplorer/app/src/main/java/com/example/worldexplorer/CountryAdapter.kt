package com.example.worldexplorer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class CountryAdapter(
    private var countries: List<Country>,
    private val onItemClick: (Country) -> Unit
) : RecyclerView.Adapter<CountryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgFlag: ImageView = itemView.findViewById(R.id.imgFlag)
        val tvCountryName: TextView = itemView.findViewById(R.id.tvCountryName)
        val tvRegion: TextView = itemView.findViewById(R.id.tvRegion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_country, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val country = countries[position]
        holder.tvCountryName.text = country.name
        holder.tvRegion.text = country.region

        val flagUrl = country.flagUrl.ifBlank {
            if (country.code.length == 2) "https://flags.restcountries.com/v5/w320/${country.code.lowercase()}.png" else ""
        }

        if (flagUrl.isNotBlank()) {
            Glide.with(holder.itemView.context)
                .load(flagUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(holder.imgFlag)
        } else {
            Glide.with(holder.itemView.context).clear(holder.imgFlag)
            holder.imgFlag.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        holder.itemView.setOnClickListener {
            onItemClick(country)
        }
    }

    override fun getItemCount(): Int = countries.size

    fun updateData(newCountries: List<Country>) {
        countries = newCountries
        notifyDataSetChanged()
    }
}
