package com.example.smartcctv
import android.Manifest
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.Toast // Import Toast untuk notifikasi
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartcctv.api.ApiClient
import com.example.smartcctv.data.*
import com.example.smartcctv.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var recorder: MediaRecorder? = null
    private lateinit var audioFile: File
    private lateinit var orangTerdaftarAdapter: OrangTerdaftarAdapter
    private var isRecording = false // Flag untuk status rekam

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Minta permission rekam audio dan penyimpanan
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.RECORD_AUDIO
                // WRITE_EXTERNAL_STORAGE dan READ_EXTERNAL_STORAGE tidak diperlukan untuk API 29+ jika menyimpan ke cache
            ),
            0
        )

        val micButton = findViewById<LinearLayout>(R.id.btnGunakanMic)
        micButton.setOnClickListener {
            if (!isRecording) {
                startRecording()

                // Otomatis berhenti dan upload setelah 5 detik
                Handler(Looper.getMainLooper()).postDelayed({
                    if (isRecording) {
                        stopRecording()
                        uploadAudioToRaspberryPi()
                    }
                }, 5000) // 5 detik
            }
        }

        binding.btnProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.viewAllPersons.setOnClickListener {
            val intent = Intent(this, OrangTerdaftar::class.java)
            startActivity(intent)
        }

        // --- Kode untuk RecyclerViews dan WebView (tidak diubah) ---
        setupRecyclerViews()
        setupWebView()
        fetchRegisteredPersons()
    }

    private fun startRecording() {
        // Tentukan direktori penyimpanan di cache internal aplikasi
        val dir = File(externalCacheDir, "audio")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        // Gunakan format .mp4 agar lebih kompatibel
        audioFile = File(dir, "recorded_audio.mp4")

        // CEK VERSI ANDROID UNTUK KOMPATIBILITAS
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder() // <-- Gunakan constructor lama untuk API < 31
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
        // UBAH: Sesuaikan media type dengan format file .mp4
        val mediaType = "audio/mp4".toMediaTypeOrNull()
        val requestBody = audioFile.asRequestBody(mediaType)

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("audio", audioFile.name, requestBody)
            .build()

        // Pastikan IP Address dan Port sudah benar
        val request = Request.Builder()
            .url("http://10.12.12.1" +
                    "59:5050/audio/upload")
            .post(multipartBody)
            .build()

        Log.d("UPLOAD", "Mengirim audio ke server...")
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("UPLOAD", "Gagal upload audio", e)
                // Menampilkan pesan error di UI thread
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
                response.close() // Penting untuk menutup response body
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

                        // --- UPDATE JUMLAH DATA DI SINI ---
                        binding.countPerson.text = personList.size.toString()

                        // Ambil 5 data pertama untuk ditampilkan di RecyclerView
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

    // Fungsi bantuan untuk merapikan kode onCreate
    private fun setupRecyclerViews() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerTable)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val data = listOf(
            TableRowData("14:32:15", "Lab Multimedia", "Rezki Andika", "Dikenali"),
            TableRowData("14:32:15", "Lab Jaringan", "Unknown", "Unknown"),
            TableRowData("14:32:15", "Lab Multimedia", "Cha Eunwoo", "Dikenali"),
        )
        recyclerView.adapter = TableAdapter(data)

        val recycler = findViewById<RecyclerView>(R.id.recyclerRealtime)
        recycler.layoutManager = LinearLayoutManager(this)
        val eventList = listOf(
            RealtimeDetectionData("Lab Multimedia: Dr. Ahmad Rifai masuk lab", "14:32:15"),
            RealtimeDetectionData("Lab Mobile: Sampah ditemukan", "15:45:45"),
            RealtimeDetectionData("Lab Jaringan: Tidak ada ancaman terdeteksi", "13:23:02")
        )
        recycler.adapter = RealtimeDetectionAdapter(eventList)

        orangTerdaftarAdapter = OrangTerdaftarAdapter(emptyList()) { person ->
            // Aksi saat item di MainActivity di-klik
            val intent = Intent(this, PersonDetailActivity::class.java)
            intent.putExtra("PERSON_ID", person.idPerson)
            startActivity(intent)
        }
        binding.recyclerOrang.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false) // Set horizontal
        binding.recyclerOrang.adapter = orangTerdaftarAdapter




//        val recyclerLab = findViewById<RecyclerView>(R.id.recyclerLab)
//        recyclerLab.layoutManager = LinearLayoutManager(this)
//        val labTerdaftarList = listOf(
//            OrangTerdaftarData("Lab.Multimedia", "Lantai 2, Gedung Elektro"),
//            OrangTerdaftarData("Lab. Mobile", "Lantai 2, Gedung Elektro"),
//            OrangTerdaftarData("Lab Jaringan", "Lantai 2, Gedung Elektro"),
//        )
//        recyclerLab.adapter = OrangTerdaftarAdapter(labTerdaftarList)
    }

    private fun setupWebView() {
        val webView = findViewById<WebView>(R.id.webView)
        webView.settings.javaScriptEnabled = true
        // Pastikan IP Address ini bisa diakses dari HP Anda
        webView.loadUrl("http://10.12.12.159:5000/video_feed")
    }
}
