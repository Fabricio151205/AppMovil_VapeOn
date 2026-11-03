package com.example.vapeon_movil.services

import com.example.vapeon_movil.services.dto.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TarjetaService {
    @POST("tarjetas")
    suspend fun crearTarjeta(@Body body: TarjetaContainer, @Query("documentId") documentId: String): TarjetaDocumento

    @POST("../documents:runQuery")
    suspend fun consultarTarjetas(@Body body: TarjetaConsulta): List<TarjetaConsultaRespuesta>

    // Selección: lectura fresca para verificar propietario y vencimiento.
    @GET("tarjetas/{id}")
    suspend fun mostrarTarjeta(@Path("id") id: String): TarjetaDocumento
}
