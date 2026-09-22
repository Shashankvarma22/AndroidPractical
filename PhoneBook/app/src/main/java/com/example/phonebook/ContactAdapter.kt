package com.example.phonebook

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactAdapter(
    private val contacts: MutableList<Contact>,
    private val onEditClick: (Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textName: TextView = view.findViewById(R.id.textName)
        val textPhone: TextView = view.findViewById(R.id.textPhone)
        val buttonEdit: ImageButton = view.findViewById(R.id.buttonEdit)
        val buttonDelete: ImageButton = view.findViewById(R.id.buttonDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.textName.text = contact.name
        
        // Display multiple numbers comma-separated
        holder.textPhone.text = if (contact.numbers.isNotEmpty()) {
            contact.numbers.joinToString(", ")
        } else {
            contact.phoneNumber // Fallback
        }
        
        holder.buttonEdit.setOnClickListener {
            onEditClick(holder.bindingAdapterPosition)
        }
        
        holder.buttonDelete.setOnClickListener {
            onDeleteClick(holder.bindingAdapterPosition)
        }
    }

    override fun getItemCount() = contacts.size
}
