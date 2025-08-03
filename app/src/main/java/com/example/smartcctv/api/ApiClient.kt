package com.example.smartcctv.api
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // PENTING: Ganti URL ini dengan alamat IP dan path folder API Anda
    // Gunakan http://10.0.2.2/ jika menjalankan di emulator Android
    // Ganti 10.0.2.2 dengan IP address PC jika menggunakan HP fisik
    private const val BASE_URL = "http://10.12.12.39/BackendOptaGuard/"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}