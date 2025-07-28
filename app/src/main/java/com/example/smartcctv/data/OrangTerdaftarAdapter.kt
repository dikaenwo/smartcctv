package com.example.smartcctv.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartcctv.R

class OrangTerdaftarAdapter(private val orangList: List<OrangTerdaftarData>) :
    RecyclerView.Adapter<OrangTerdaftarAdapter.EventViewHolder>() {

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val username: TextView = view.findViewById(R.id.userName)
        val status: TextView = view.findViewById(R.id.status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_orang_terdaftar, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val item = orangList[position]
        holder.username.text = item.name
        holder.status.text = item.status
    }

    override fun getItemCount(): Int = orangList.size
}
