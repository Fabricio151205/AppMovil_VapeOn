package com.example.vapeon_movil.services

import com.example.vapeon_movil.services.dto.SaborContainer
import com.example.vapeon_movil.services.dto.SaborDocumento
import com.example.vapeon_movil.services.dto.SaborListResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// ─────────────────────────────────────────────
// Contrato HTTP — Colección "sabores" en Firestore
// DTOs de transporte en: services/dto/SaborDto.kt
// ─────────────────────────────────────────────
interface SaborService {

    @GET("sabores")
    suspend fun listarSabores(@Query("pageToken") pageToken: String? = null): SaborListResponse

    @GET("sabores/{id}")
    suspend fun mostrarSabor(@Path("id") id: String): SaborDocumento

    @POST("sabores")
    suspend fun crearSabor(
        @Body body: SaborContainer,
        @Query("documentId") documentId: String? = null
    ): SaborDocumento

    @PATCH("sabores/{id}")
    suspend fun actualizarSabor(
        @Path("id") id: String,
        @Body body: SaborContainer,
        @Query("updateMask.fieldPaths") campos: List<String> = listOf("nombre", "marca", "stock")
    ): SaborDocumento

    @DELETE("sabores/{id}")
    suspend fun eliminarSabor(@Path("id") id: String): Any
}

suspend fun SaborService.listarTodosSabores(): SaborListResponse {
    val documentos = mutableListOf<SaborDocumento>()
    val tokens = mutableSetOf<String>()
    var token: String? = null
    do {
        val pagina = listarSabores(token)
        documentos += pagina.documents.orEmpty()
        token = pagina.nextPageToken?.takeIf { it.isNotBlank() }
        check(token == null || tokens.add(token)) { "Firestore repitió un token de sabores." }
    } while (token != null)
    return SaborListResponse(documentos.distinctBy { it.name })
}
