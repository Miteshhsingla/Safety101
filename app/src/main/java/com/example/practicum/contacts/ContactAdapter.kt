package com.example.practicum.contacts

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.practicum.databinding.ItemUserBinding

class ContactAdapter(
    private val context: Context,
    private val list : MutableList<Contact>,
    private val onDeleteListener: OnDeleteListener) : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.binding.apply {

            val c = list[position]

            tvName.text = c.name
            tvPhone.text = c.phoneNo

            linear.setOnLongClickListener {


                MaterialAlertDialogBuilder(context)
                    .setTitle("Remove Contact")
                    .setMessage("Are you sure want to remove this contact?")
                    .setPositiveButton("YES") { dialogInterface, i ->
                        //delete the specified contact from the database
                        onDeleteListener.onDelete(c)
                        //remove the item from the list
                        list.remove(c)
                        //notify the listview that dataset has been changed
                        notifyDataSetChanged()
                        Toast.makeText(context, "Contact removed!", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("NO") { dialogInterface, i -> }
                    .show()
                false
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    //this method will update the ListView
    fun refresh(list: MutableList<Contact?>?) {
        list?.clear()
        list?.addAll(list)
        notifyDataSetChanged()
    }

    interface OnDeleteListener{
        fun onDelete(c:Contact)
    }
}