package com.example.smartcctv

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartcctv.api.ApiClient
import com.example.smartcctv.data.*
import com.example.smartcctv.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var recorder: MediaRecorder? = null
    private lateinit var audioFile: File
    private lateinit var orangTerdaftarAdapter: OrangTerdaftarAdapter
    private var isRecording = false

    private val client = OkHttpClient()
    private val apiFlaskUrl = "http://192.168.110.87:5000"

    private lateinit var detectionButtons: List<AppCompatButton>

    override fun attachBaseContext(newBase: Context) {

        val sharedPrefs = newBase.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val language = sharedPrefs.getString("language", "in") ?: "in" // Default 'in' (Indonesia)
        applyLocale(language)
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateLanguageButtonUI()

        detectionButtons = listOf(
            binding.btnMotionDetection,
            binding.btnFireDetection,
            binding.btnTrashDetection
        )

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 0)

        binding.btnLanguage.setOnClickListener {
            // Cek bahasa yang sedang aktif
            val currentLang = getSavedLanguage()
            // Tentukan bahasa baru (kebalikannya)
            val newLang = if (currentLang == "in") "en" else "in"

            // Simpan bahasa baru & restart activity untuk menerapkan perubahan
            saveLanguage(newLang)
            recreate() // Ini akan memuat ulang activity dengan bahasa baru
        }

        binding.btnGunakanMic.setOnClickListener {
            if (!isRecording) {
                startRecording()
                Handler(Looper.getMainLooper()).postDelayed({
                    if (isRecording) {
                        stopRecording()
                        uploadAudioToRaspberryPi()
                    }
                }, 5000)
            }
        }

        binding.bantuanTeknis.setOnClickListener {
            // Nomor telepon tujuan dalam format internasional
            val phoneNumber = "+6281355649201"

            // Pesan default yang akan terisi otomatis di WhatsApp (opsional, tapi sangat membantu)
            val message = "Halo, saya butuh bantuan teknis terkait aplikasi Smart CCTV."

            try {
                // Hapus karakter selain angka dari nomor telepon
                val formattedNumber = phoneNumber.replace(Regex("[^0-9]"), "")

                // Encode pesan agar formatnya benar untuk URL
                val encodedMessage = URLEncoder.encode(message, "UTF-8")

                // Buat URL untuk WhatsApp API
                val url = "https://api.whatsapp.com/send?phone=$formattedNumber&text=$encodedMessage"

                // Buat Intent dengan action VIEW untuk membuka URL
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)

                // Mulai activity untuk membuka WhatsApp
                startActivity(intent)

            } catch (e: Exception) {
                // Tangani error jika WhatsApp tidak terinstall atau terjadi kesalahan lain
                Toast.makeText(this, "Gagal membuka WhatsApp. Pastikan aplikasi sudah terpasang.", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }

        binding.panggilDamkar.setOnClickListener {
            // Nomor telepon Damkar Makassar yang akan dituju
            val phoneNumber = "0411854444"

            // Membuat Intent dengan action ACTION_DIAL
            // Ini akan membuka aplikasi telepon dengan nomor sudah terisi, BUKAN langsung menelepon
            val dialIntent = Intent(Intent.ACTION_DIAL)

            // Menetapkan data intent ke nomor telepon dengan format "tel:"
            dialIntent.data = Uri.parse("tel:$phoneNumber")

            // Memulai activity untuk membuka dialer
            startActivity(dialIntent)
        }

        binding.btnProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.btnLogout.setOnClickListener {
            Toast.makeText(this, "Berhasil Logout", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.viewAllPersons.setOnClickListener {
            val intent = Intent(this, OrangTerdaftar::class.java)
            startActivity(intent)
        }

        setupDetectionButtons()
        setupRecyclerViews()
        setupWebView()
        fetchRegisteredPersons()
    }

    private fun setupDetectionButtons() {
        detectionButtons.forEach { button ->
            button.setOnClickListener {
                val featureName = when (button.id) {
                    R.id.btnMotionDetection -> "monitoring"
                    R.id.btnFireDetection -> "fire"
                    R.id.btnTrashDetection -> "trash"
                    else -> return@setOnClickListener
                }
                val newState = !button.isSelected
                toggleFeature(featureName, newState)
            }
        }
    }

    // --- FUNGSI INI DIHAPUS KARENA TIDAK ADA ENDPOINT GET ---
    // private fun fetchInitialFeatureStates() { ... }

    private fun toggleFeature(feature: String, state: Boolean) {
        val url = "$apiFlaskUrl/toggle_feature"
        val json = JSONObject().apply {
            put("feature", feature)
            put("state", state)
        }
        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(requestBody).build()

        Log.d("API_CALL", "Request: POST $url with body $json")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("API_CALL", "Gagal: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error: Gagal terhubung ke server", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("API_CALL", "Response: ${response.code} - $responseBody")

                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val data = JSONObject(responseBody)
                            val allFeatures = data.optJSONObject("all_features")
                            if (allFeatures != null) {
                                // INI BAGIAN KUNCINYA:
                                // Selalu sinkronkan UI berdasarkan respons 'all_features' dari server
                                syncButtonStates(allFeatures)
                            }
                        } catch (e: Exception) {
                            Log.e("API_CALL", "Error parsing JSON: $e")
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Gagal mengubah status: $responseBody", Toast.LENGTH_LONG).show()
                    }
                }
                response.close()
            }
        })
    }

    private fun syncButtonStates(features: JSONObject) {
        Log.d("UI_SYNC", "Menyinkronkan UI dengan data: $features")
        binding.btnMotionDetection.isSelected = features.optBoolean("monitoring", false)
        binding.btnFireDetection.isSelected = features.optBoolean("fire", false)
        binding.btnTrashDetection.isSelected = features.optBoolean("trash", false)
    }

    // --- Sisa kode Anda tidak berubah ---
    // ... (fungsi startRecording, stopRecording, uploadAudioToRaspberryPi, dll.)
    private fun startRecording() {
        val dir = File(externalCacheDir, "audio")
        if (!dir.exists()) dir.mkdirs()
        audioFile = File(dir, "recorded_audio.mp4")

        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        recorder = mediaRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFile.absolutePath)
            try {
                prepare()
                start()
                isRecording = true
                Log.d("AUDIO", "Rekaman dimulai...")
                Toast.makeText(this@MainActivity, "Merekam...", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Log.e("AUDIO", "Gagal memulai rekaman", e)
                isRecording = false
            }
        }
    }

    private fun stopRecording() {
        if (!isRecording) return
        recorder?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                Log.e("AUDIO", "Gagal menghentikan rekaman", e)
            }
        }
        recorder = null
        isRecording = false
        Log.d("AUDIO", "Rekaman selesai: ${audioFile.absolutePath}")
        Toast.makeText(this, "Rekaman Selesai", Toast.LENGTH_SHORT).show()
    }

    private fun uploadAudioToRaspberryPi() {
        if (!::audioFile.isInitialized || !audioFile.exists()) {
            Log.e("UPLOAD", "File audio tidak ditemukan untuk di-upload.")
            return
        }

        val client = OkHttpClient()
        val mediaType = "audio/mp4".toMediaTypeOrNull()
        val requestBody = audioFile.asRequestBody(mediaType)

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("audio", audioFile.name, requestBody)
            .build()

        val request = Request.Builder()
            .url("http://192.168.110.69:5050/audio/upload")
            .post(multipartBody)
            .build()

        Log.d("UPLOAD", "Mengirim audio ke server...")
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("UPLOAD", "Gagal upload audio", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Gagal upload: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    Log.d("UPLOAD", "Berhasil upload audio: $responseBody")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Berhasil upload!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("UPLOAD", "Gagal upload audio, response: $responseBody")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Gagal: $responseBody", Toast.LENGTH_LONG).show()
                    }
                }
                response.close()
            }
        })
    }

    private fun fetchRegisteredPersons() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.instance.getAllPersons()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val personList = response.body() ?: emptyList()
                        binding.countPerson.text = personList.size.toString()
                        orangTerdaftarAdapter.updateData(personList.take(5))
                    } else {
                        Toast.makeText(this@MainActivity, "Gagal memuat daftar orang", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("MainActivity", "Error fetching persons: ${e.message}")
                }
            }
        }
    }

    private fun setupRecyclerViews() {
        val recyclerView = binding.recyclerTable
        recyclerView.layoutManager = LinearLayoutManager(this)
        val data = listOf(
            TableRowData("14:32:15", "Lab Multimedia", "Rezki Andika", "Dikenali"),
            TableRowData("14:32:15", "Lab Jaringan", "Unknown", "Unknown"),
            TableRowData("14:32:15", "Lab Multimedia", "Cha Eunwoo", "Dikenali"),
        )
        recyclerView.adapter = TableAdapter(data)

        val recycler = binding.recyclerRealtime
        recycler.layoutManager = LinearLayoutManager(this)
        val eventList = listOf(
            RealtimeDetectionData("Lab Multimedia: Dr. Ahmad Rifai masuk lab", "14:32:15"),
            RealtimeDetectionData("Lab Mobile: Sampah ditemukan", "15:45:45"),
            RealtimeDetectionData("Lab Jaringan: Tidak ada ancaman terdeteksi", "13:23:02")
        )
        recycler.adapter = RealtimeDetectionAdapter(eventList)

        orangTerdaftarAdapter = OrangTerdaftarAdapter(emptyList()) { person ->
            val intent = Intent(this, PersonDetailActivity::class.java)
            intent.putExtra("PERSON_ID", person.idPerson)
            startActivity(intent)
        }
        binding.recyclerOrang.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerOrang.adapter = orangTerdaftarAdapter
    }

    private fun applyLocale(languageCode: String) {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    private fun saveLanguage(languageCode: String) {
        val sharedPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("language", languageCode).apply()
    }

    private fun getSavedLanguage(): String {
        val sharedPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        return sharedPrefs.getString("language", "in") ?: "in" // Default ke 'in' jika tidak ada
    }

    private fun updateLanguageButtonUI() {
        val currentLang = getSavedLanguage()
        if (currentLang == "in") {
            binding.btnLanguage.setImageResource(R.drawable.flag_indonesia)
        } else {
            binding.btnLanguage.setImageResource(R.drawable.flag_uk)
        }
    }

    // Di dalam MainActivity.kt

    // Di dalam MainActivity.kt

    private fun setupWebView() {
        val webView = binding.webView
        val btnFullscreen = binding.btnFullscreen // Ambil referensi tombol dari layout
        val urlToLoad = "$apiFlaskUrl/lempar"

        webView.settings.javaScriptEnabled = true
        webView.loadUrl(urlToLoad)

        // Listener dipasang pada TOMBOL, bukan WebView
        btnFullscreen.setOnClickListener {
            val intent = Intent(this, FullScreenWebViewActivity::class.java).apply {
                putExtra("EXTRA_URL", urlToLoad)
            }
            startActivity(intent)
        }
    }

}