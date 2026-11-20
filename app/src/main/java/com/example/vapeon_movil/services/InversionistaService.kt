package com.example.vapeon_movil.services

import com.example.vapeon_movil.services.dto.*
import retrofit2.http.*

interface InversionistaService {
    @GET("inversionistas")
    suspend fun listar(@Query("pageToken") pageToken: String? = null): InversionistaPagina

    @GET("inversionistas/{id}")
    suspend fun mostrar(@Path("id") id: String): InversionistaDocumento

    @POST("inversionistas")
    suspend fun crear(@Body body: InversionistaContainer, @Query("documentId") id: String): InversionistaDocumento

    @PATCH("inversionistas/{id}")
    suspend fun actualizar(
        @Path("id") id: String,
        @Body body: InversionistaContainer,
        @Query("currentDocument.updateTime") version: String,
        @Query("updateMask.fieldPaths") campos: List<String> = CAMPOS_INVERSIONISTA
    ): InversionistaDocumento

    @DELETE("inversionistas/{id}")
    suspend fun eliminar(@Path("id") id: String, @Query("currentDocument.updateTime") version: String): Any
}
