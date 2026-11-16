package com.example.vapeon_movil.services

import com.example.vapeon_movil.services.dto.ProveedorContainer
import com.example.vapeon_movil.services.dto.ProveedorDocumento
import com.example.vapeon_movil.services.dto.ProveedorListResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ProveedorService {
    @GET("proveedores")
    suspend fun listarProveedores(@Query("pageToken") pageToken: String? = null): ProveedorListResponse

    @GET("proveedores/{id}")
    suspend fun mostrarProveedor(@Path("id") id: String): ProveedorDocumento

    @POST("proveedores")
    suspend fun crearProveedor(
        @Body body: ProveedorContainer,
        @Query("documentId") documentId: String
    ): ProveedorDocumento
}

/** Reúne todas las páginas y rechaza un pageToken repetido para evitar ciclos. */
suspend fun ProveedorService.listarTodosProveedores(): ProveedorListResponse {
    val documentos = mutableListOf<ProveedorDocumento>()
    val tokens = mutableSetOf<String>()
    var token: String? = null
    do {
        val pagina = listarProveedores(token)
        documentos += pagina.documents.orEmpty()
        token = pagina.nextPageToken?.takeIf { it.isNotBlank() }
        check(token == null || tokens.add(token)) { "Firestore repitió un token de proveedores." }
    } while (token != null)
    return ProveedorListResponse(documents = documentos.distinctBy { it.name })
}
