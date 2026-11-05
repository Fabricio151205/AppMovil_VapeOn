package com.example.vapeon_movil.services

import com.example.vapeon_movil.services.dto.MarcaContainer
import com.example.vapeon_movil.services.dto.MarcaDocumento
import com.example.vapeon_movil.services.dto.MarcaListResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

internal val CAMPOS_MARCA = listOf("nombre")

interface MarcaService {
    @GET("marcas")
    suspend fun listarMarcas(@Query("pageToken") pageToken: String? = null): MarcaListResponse

    @GET("marcas/{id}")
    suspend fun mostrarMarca(@Path("id") id: String): MarcaDocumento

    @POST("marcas")
    suspend fun crearMarca(
        @Body body: MarcaContainer,
        @Query("documentId") documentId: String? = null
    ): MarcaDocumento

    @PATCH("marcas/{id}")
    suspend fun actualizarMarca(
        @Path("id") id: String,
        @Body body: MarcaContainer,
        @Query("updateMask.fieldPaths") campos: List<String> = CAMPOS_MARCA
    ): MarcaDocumento

    @DELETE("marcas/{id}")
    suspend fun eliminarMarca(@Path("id") id: String): Any
}

suspend fun MarcaService.listarTodasMarcas(): MarcaListResponse {
    val documentos = mutableListOf<MarcaDocumento>()
    val tokens = mutableSetOf<String>()
    var token: String? = null
    do {
        val pagina = listarMarcas(token)
        documentos += pagina.documents.orEmpty()
        token = pagina.nextPageToken?.takeIf { it.isNotBlank() }
        check(token == null || tokens.add(token)) { "Firestore repitió un token de marcas." }
    } while (token != null)
    return MarcaListResponse(documentos.distinctBy { it.name })
}
