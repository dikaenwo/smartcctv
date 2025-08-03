package com.example.smartcctv.api

import com.example.smartcctv.data.LoginRequest
import com.example.smartcctv.data.Person
import com.example.smartcctv.response.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("login.php") // Sesuaikan dengan nama file PHP Anda
    suspend fun loginUser(@Body request: LoginRequest): Response<LoginResponse>

    @GET("persons.php")
    suspend fun getAllPersons(): Response<List<Person>>

    @GET("person.php")
    suspend fun getPersonById(@Query("id") personId: Int): Response<Person>
}