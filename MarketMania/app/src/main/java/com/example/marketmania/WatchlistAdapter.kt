package com.example.marketmania

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WatchlistAdapter(
    private var stocks: List<StockItem>,
    private val onStockSelected: (StockItem) -> Unit
) : RecyclerView.Adapter<WatchlistAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSymbol: TextView = itemView.findViewById(R.id.tvWatchlistSymbol)
        val tvName: TextView = itemView.findViewById(R.id.tvWatchlistName)
        val tvPrice: TextView = itemView.findViewById(R.id.tvWatchlistPrice)
        val tvChange: TextView = itemView.findViewById(R.id.tvWatchlistChange)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_watchlist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val stock = stocks[position]
        holder.tvSymbol.text = stock.symbol
        holder.tvName.text = stock.companyName
        holder.tvPrice.text = if (stock.price > 0) String.format(java.util.Locale.US, "$%.2f", stock.price) else "--"

        val changeText = if (stock.changePercent >= 0) {
            holder.tvChange.setTextColor(Color.parseColor("#22C55E"))
            String.format(java.util.Locale.US, "+%.2f%%", stock.changePercent)
        } else {
            holder.tvChange.setTextColor(Color.parseColor("#EF4444"))
            String.format(java.util.Locale.US, "%.2f%%", stock.changePercent)
        }
        holder.tvChange.text = changeText

        holder.itemView.setOnClickListener {
            onStockSelected(stock)
        }
    }

    override fun getItemCount(): Int = stocks.size

    fun updateStockPrice(symbol: String, price: Double) {
        val index = stocks.indexOfFirst { it.symbol == symbol }
        if (index != -1) {
            stocks[index].price = price
            notifyItemChanged(index)
        }
    }

    fun updateStockQuote(symbol: String, price: Double, changePercent: Double) {
        val index = stocks.indexOfFirst { it.symbol == symbol }
        if (index != -1) {
            stocks[index].price = price
            stocks[index].changePercent = changePercent
            notifyItemChanged(index)
        }
    }
}