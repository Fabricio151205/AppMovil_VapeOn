package com.example.vapeon_movil.services

import com.example.vapeon_movil.services.dto.PagoContainer
import com.example.vapeon_movil.services.dto.PagoDocumento
import retrofit2.http.*

interface PagoService {
    @POST("pagos")
    suspend fun crearPago(@Body body: PagoContainer, @Query("documentId") documentId: String): PagoDocumento

    @GET("pagos/{id}")
    suspend fun mostrarPago(@Path("id") id: String): PagoDocumento

    @PATCH("pagos/{id}")
    suspend fun actualizarResultado(@Path("id") id: String, @Body body: PagoContainer,
        @Query("currentDocument.updateTime") updateTime: String,
        @Query("updateMask.fieldPaths") campos: List<String> = listOf("estadoPago", "fechaResultado")): PagoDocumento
}
