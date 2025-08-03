package com.example.smartcctv

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.smartcctv.api.ApiClient
import com.example.smartcctv.data.Person
import com.example.smartcctv.databinding.ActivityPersonDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PersonDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPersonDetailBinding

    companion object {
        const val PERSON_ID = "PERSON_ID" // Kunci untuk Intent Extra
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Mengambil ID dari intent
        val personId = intent.getIntExtra(PERSON_ID, -1)

        if (personId != -1) {
            fetchPersonDetails(personId)
        } else {
            Toast.makeText(this, "Error: ID tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish() // Tutup activity jika tidak ada ID
        }

        // Fungsi tombol kembali
        binding.btnBackDetail.setOnClickListener {
            finish()
        }
    }

    private fun fetchPersonDetails(id: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.instance.getPersonById(id)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        response.body()?.let { person ->
                            populateUi(person)
                        }
                    } else {
                        Toast.makeText(this@PersonDetailActivity, "Gagal memuat detail", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("PersonDetailActivity", "Error: ${e.message}")
                    Toast.makeText(this@PersonDetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun populateUi(person: Person) {
        binding.personName.text = person.name
        binding.personNomorInduk.text = person.nomorInduk
        binding.personRole.text = person.role

        Log.d("DETAIL_PHOTO", "URL Foto yang akan dimuat: ${person.photo}")

        Glide.with(this)
            .load(person.photo)
            .placeholder(R.drawable.ic_deteksi_orang)
            .error(R.drawable.ic_deteksi_orang)
            .into(binding.photoPerson)
    }
}