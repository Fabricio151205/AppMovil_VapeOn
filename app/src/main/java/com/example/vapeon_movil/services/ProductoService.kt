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

// ─────────────────────────────────────────────
// Contrato HTTP — Colección "productos" en Firestore
// DTOs de transporte en: services/dto/ProductoDto.kt
// ─────────────────────────────────────────────
interface ProductoService {

    // Listar todos los productos
    @GET("productos")
    suspend fun listarProductos(): ProductoListResponse

    // Crear un producto nuevo
    @POST("productos")
    suspend fun crearProducto(@Body body: ProductoContainer): Any

    // Actualizar un producto existente por su ID
    @PATCH("productos/{id}")
    suspend fun actualizarProducto(
        @Path("id") id: String,
        @Body body: ProductoContainer
    ): ProductoDocumento

    // Eliminar un producto por su ID
    @DELETE("productos/{id}")
    suspend fun eliminarProducto(@Path("id") id: String): Any
}