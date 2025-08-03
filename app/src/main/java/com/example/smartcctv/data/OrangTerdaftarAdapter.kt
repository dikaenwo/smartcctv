// File: OrangTerdaftarAdapter.kt
package com.example.smartcctv.data

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.smartcctv.R

class OrangTerdaftarAdapter(
    private var personList: List<Person>,
    private val onItemClick: (Person) -> Unit
) : RecyclerView.Adapter<OrangTerdaftarAdapter.PersonViewHolder>() {

    // Ganti nama ViewHolder agar lebih sesuai
    class PersonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val username: TextView = view.findViewById(R.id.userName)
        val status: TextView = view.findViewById(R.id.status)
        val userImage: ImageView = view.findViewById(R.id.photoPerson) // Ambil referensi ImageView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_orang_terdaftar, parent, false)
        return PersonViewHolder(view)
    }

    override fun onBindViewHolder(holder: PersonViewHolder, position: Int) {
        val item = personList[position]
        holder.username.text = item.name
        holder.status.text = item.role

        // Gunakan Glide untuk memuat gambar dari URL
        Glide.with(holder.itemView.context)
            .load(item.photo) // URL gambar dari API
            .placeholder(R.drawable.profile) // Gambar default saat loading
            .error(R.drawable.profile) // Gambar default jika gagal load
            .circleCrop() // Membuat gambar menjadi lingkaran
            .into(holder.userImage)

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = personList.size

    // Fungsi untuk mengupdate data di adapter
    fun updateData(newPersonList: List<Person>) {
        this.personList = newPersonList
        notifyDataSetChanged()
    }
}