package com.example.smartcctv.response

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("user")
    val user: User
)

data class User(
    @SerializedName("id_user")
    val idUser: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("role")
    val role: String,
    @SerializedName("photo")
    val photo: String?,
    @SerializedName("created_at")
    val createdAt: String
)
