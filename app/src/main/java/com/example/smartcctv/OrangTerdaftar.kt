// File: OrangTerdaftarActivity.kt
package com.example.smartcctv

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartcctv.api.ApiClient
import com.example.smartcctv.data.OrangTerdaftarAdapter
import com.example.smartcctv.data.Person
import com.example.smartcctv.databinding.ActivityOrangTerdaftarBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OrangTerdaftar : AppCompatActivity() {

    private lateinit var binding: ActivityOrangTerdaftarBinding
    private lateinit var personAdapter: OrangTerdaftarAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrangTerdaftarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        fetchPersonsData()

        // Fungsi untuk tombol kembali
        binding.btnBackDetail.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        // Ganti aksi klik untuk membuka PersonDetailActivity
        personAdapter = OrangTerdaftarAdapter(emptyList()) { person ->
            val intent = Intent(this, PersonDetailActivity::class.java)
            // Kirim ID orang yang diklik ke activity selanjutnya
            intent.putExtra("PERSON_ID", person.idPerson)
            startActivity(intent)
        }

        binding.recyclerAllPersons.apply {
            layoutManager = LinearLayoutManager(this@OrangTerdaftar)
            adapter = personAdapter
        }
    }

    private fun fetchPersonsData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.instance.getAllPersons()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val personList = response.body() ?: emptyList()
                        binding.countPerson.text = personList.size.toString()
                        personAdapter.updateData(personList)
                    } else {
                        Toast.makeText(this@OrangTerdaftar, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("OrangTerdaftarActivity", "Error: ${e.message}")
                    Toast.makeText(this@OrangTerdaftar, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}