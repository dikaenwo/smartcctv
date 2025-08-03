// File: Person.kt
package com.example.smartcctv.data

import com.google.gson.annotations.SerializedName

data class Person(
    @SerializedName("id_person")
    val idPerson: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("nomor_induk")
    val nomorInduk: String,

    @SerializedName("role")
    val role: String,

    @SerializedName("photo")
    val photo: String?, // Dibuat nullable jika foto bisa kosong

    @SerializedName("created_at")
    val createdAt: String
)