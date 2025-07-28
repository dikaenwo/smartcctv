package com.example.smartcctv.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartcctv.R

class TableAdapter(private val data: List<TableRowData>) :
    RecyclerView.Adapter<TableAdapter.TableViewHolder>() {

    inner class TableViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val time: TextView = view.findViewById(R.id.textTime)
        val location: TextView = view.findViewById(R.id.textLocation)
        val name: TextView = view.findViewById(R.id.textName)
        val status: TextView = view.findViewById(R.id.textStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_table_row, parent, false)
        return TableViewHolder(view)
    }

    override fun onBindViewHolder(holder: TableViewHolder, position: Int) {
        val item = data[position]
        holder.time.text = item.time
        holder.location.text = item.location
        holder.name.text = item.name
        holder.status.text = item.status

        // Set background status warna
        holder.status.setBackgroundResource(
            if (item.status == "Dikenali") R.drawable.bg_status_green else R.drawable.bg_status_red
        )

        // Set background row: cek apakah ini item terakhir
        val background = if (position == itemCount - 1) {
            R.drawable.bg_item_table_row
        } else {
            R.drawable.bg_gradient
        }
        holder.itemView.setBackgroundResource(background)
    }

    override fun getItemCount() = data.size
}
