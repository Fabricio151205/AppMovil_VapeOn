package com.example.vapeon_movil.services

import com.example.vapeon_movil.services.dto.DireccionConsulta
import com.example.vapeon_movil.services.dto.DireccionConsultaRespuesta
import com.example.vapeon_movil.services.dto.DireccionContainer
import com.example.vapeon_movil.services.dto.DireccionDocumento
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

internal val CAMPOS_DIRECCION = listOf("alias", "direccion", "referencia", "latitud", "longitud")

// ─────────────────────────────────────────────
// Contrato HTTP — Colección "direcciones" en Firestore
// DTOs de transporte en: services/dto/DireccionDto.kt
// ─────────────────────────────────────────────
interface DireccionService {

    @POST("direcciones")
    suspend fun crearDireccion(
        @Body body: DireccionContainer,
        @Query("documentId") documentId: String
    ): DireccionDocumento

    @POST("../documents:runQuery")
    suspend fun consultarDirecciones(@Body body: DireccionConsulta): List<DireccionConsultaRespuesta>

    @GET("direcciones/{id}")
    suspend fun mostrarDireccion(@Path("id") id: String): DireccionDocumento

    @PATCH("direcciones/{id}")
    suspend fun actualizarDireccion(
        @Path("id") id: String,
        @Body body: DireccionContainer,
        @Query("updateMask.fieldPaths") campos: List<String> = CAMPOS_DIRECCION
    ): DireccionDocumento

    @DELETE("direcciones/{id}")
    suspend fun eliminarDireccion(@Path("id") id: String): Any
}
