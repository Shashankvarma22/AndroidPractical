package com.example.phonebook

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SmsAdapter(
    private val smsList: List<Sms>,
    private val onItemClick: (Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<SmsAdapter.SmsViewHolder>() {

    class SmsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textRecipient: TextView = view.findViewById(R.id.textRecipient)
        val textMessage: TextView = view.findViewById(R.id.textMessage)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteSms)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sms, parent, false)
        return SmsViewHolder(view)
    }

    override fun onBindViewHolder(holder: SmsViewHolder, position: Int) {
        val sms = smsList[position]
        holder.textRecipient.text = sms.contactName ?: sms.phoneNumber
        holder.textMessage.text = sms.message
        
        holder.itemView.setOnClickListener { onItemClick(position) }
        holder.btnDelete.setOnClickListener { onDeleteClick(position) }
    }

    override fun getItemCount(): Int = smsList.size
}
