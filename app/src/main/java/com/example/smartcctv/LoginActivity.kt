package com.example.smartcctv

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.smartcctv.api.ApiClient
import com.example.smartcctv.data.LoginRequest
import com.example.smartcctv.databinding.ActivityLoginBinding // Import ViewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    // Deklarasikan variabel binding
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inisialisasi binding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Gunakan binding untuk mengakses view
        binding.appCompatButton.setOnClickListener {
            handleLogin()
        }
    }

    private fun handleLogin() {
        // Ambil data dari EditText menggunakan binding
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editPass.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email dan password tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        // Tampilkan loading (jika ada)
        // binding.progressBar.visibility = View.VISIBLE

        // Gunakan coroutine untuk network call
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = LoginRequest(email, password)
                val response = ApiClient.instance.loginUser(request)

                withContext(Dispatchers.Main) {
                    // Sembunyikan loading
                    // binding.progressBar.visibility = View.GONE

                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        Toast.makeText(this@LoginActivity, "Login berhasil! Selamat datang ${loginResponse?.user?.name}", Toast.LENGTH_LONG).show()

                        // Pindah ke MainActivity
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // Tangani error dari API (e.g., password salah)
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = try {
                            JSONObject(errorBody!!).getString("error")
                        } catch (e: Exception) {
                            "Terjadi kesalahan"
                        }
                        Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Sembunyikan loading
                    // binding.progressBar.visibility = View.GONE
                    Log.e("LoginActivity", "Login Exception: ${e.message}")
                    Toast.makeText(this@LoginActivity, "Gagal terhubung ke server: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}