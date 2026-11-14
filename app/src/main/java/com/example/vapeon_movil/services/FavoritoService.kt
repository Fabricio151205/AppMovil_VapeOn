package com.example.vapeon_movil.services

import com.example.vapeon_movil.services.dto.FavoritoConsulta
import com.example.vapeon_movil.services.dto.FavoritoConsultaRespuesta
import com.example.vapeon_movil.services.dto.FavoritoContainer
import com.example.vapeon_movil.services.dto.FavoritoDocumento
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// ─────────────────────────────────────────────
// Contrato HTTP — Colección "favoritos" en Firestore
// DTOs de transporte en: services/dto/FavoritoDto.kt
// ─────────────────────────────────────────────
interface FavoritoService {

    @POST("favoritos")
    suspend fun crearFavorito(
        @Body body: FavoritoContainer,
        @Query("documentId") documentId: String
    ): FavoritoDocumento

    @POST("../documents:runQuery")
    suspend fun consultarFavoritos(@Body body: FavoritoConsulta): List<FavoritoConsultaRespuesta>

    @GET("favoritos/{id}")
    suspend fun mostrarFavorito(@Path("id") id: String): FavoritoDocumento

    @DELETE("favoritos/{id}")
    suspend fun eliminarFavorito(@Path("id") id: String): Any
}
