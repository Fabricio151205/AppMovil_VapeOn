package com.example.vapeon_movil.services

import com.example.vapeon_movil.services.dto.ProductoContainer
import com.example.vapeon_movil.services.dto.ProductoDocumento
import com.example.vapeon_movil.services.dto.ProductoListResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// ─────────────────────────────────────────────
// Contrato HTTP — Colección "productos" en Firestore
// DTOs de transporte en: services/dto/ProductoDto.kt
// ─────────────────────────────────────────────
interface ProductoService {

    // Listar todos los productos
    @GET("productos")
    suspend fun listarProductos(@Query("pageToken") pageToken: String? = null): ProductoListResponse

    @GET("productos/{id}")
    suspend fun mostrarProducto(@Path("id") id: String): ProductoDocumento

    // Crear un producto nuevo
    @POST("productos")
    suspend fun crearProducto(@Body body: ProductoContainer, @Query("documentId") documentId: String? = null): Any

    // Actualizar un producto existente por su ID
    @PATCH("productos/{id}")
    suspend fun actualizarProducto(
        @Path("id") id: String,
        @Body body: ProductoContainer,
        @Query("currentDocument.updateTime") updateTime: String,
        @Query("updateMask.fieldPaths") campos: List<String>
    ): ProductoDocumento

    // Eliminar un producto por su ID
    @DELETE("productos/{id}")
    suspend fun eliminarProducto(@Path("id") id: String): Any
}

/** Reúne todas las páginas; un fallo se propaga y no publica un inventario parcial. */
suspend fun ProductoService.listarTodosProductos(): ProductoListResponse {
    val documentos = mutableListOf<ProductoDocumento>()
    val tokens = mutableSetOf<String>()
    var token: String? = null
    do {
        val pagina = listarProductos(token)
        documentos += pagina.documents.orEmpty()
        token = pagina.nextPageToken?.takeIf { it.isNotBlank() }
        check(token == null || tokens.add(token)) { "Firestore repitió un token de productos." }
    } while (token != null)
    return ProductoListResponse(documentos.distinctBy { it.name })
}
