package com.example.phonebook

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private val messages: List<Sms>
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardMessage: MaterialCardView = view.findViewById(R.id.cardMessage)
        val textBody: TextView = view.findViewById(R.id.textMessageBody)
        val textDate: TextView = view.findViewById(R.id.textMessageDate)
        val root: LinearLayout = view as LinearLayout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val sms = messages[position]
        holder.textBody.text = sms.message
        
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        holder.textDate.text = sdf.format(Date(sms.date))

        val params = holder.cardMessage.layoutParams as LinearLayout.LayoutParams
        if (sms.type == 2) { // Sent
            holder.root.gravity = Gravity.END
            holder.cardMessage.setCardBackgroundColor(holder.itemView.context.getColor(androidx.appcompat.R.color.material_grey_300))
            params.marginStart = 64
            params.marginEnd = 0
        } else { // Received
            holder.root.gravity = Gravity.START
            holder.cardMessage.setCardBackgroundColor(holder.itemView.context.getColor(android.R.color.white))
            params.marginStart = 0
            params.marginEnd = 64
        }
        holder.cardMessage.layoutParams = params
    }

    override fun getItemCount(): Int = messages.size
}
