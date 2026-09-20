package com.example.vapeon_movil.utils

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val PROJECT_ID = "dbvapeon"

    // Esta es la URL REST oficial que Google te da para conectarte a Firestore mediante HTTP
    private const val BASE_URL = "https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/(default)/documents/"

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // Convierte el JSON a clases Kotlin automáticamente
            .build()
    }
}
