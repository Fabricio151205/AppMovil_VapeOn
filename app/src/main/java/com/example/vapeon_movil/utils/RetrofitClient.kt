
package com.example.vapeon_movil.utils

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val PROJECT_ID = "dbvapeon"

    private const val BASE_URL =
        "https://firestore.googleapis.com/v1/projects/" +
                "$PROJECT_ID/databases/(default)/documents/"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(FirebaseAuthInterceptor())
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val saborService:
            com.example.vapeon_movil.services.SaborService by lazy {
        retrofit.create(
            com.example.vapeon_movil.services.SaborService::class.java
        )
    }

    val marcaService:
            com.example.vapeon_movil.services.MarcaService by lazy {
        retrofit.create(
            com.example.vapeon_movil.services.MarcaService::class.java
        )
    }
}
