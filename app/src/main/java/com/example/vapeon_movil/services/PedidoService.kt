package com.example.vapeon_movil.services

import com.example.vapeon_movil.services.dto.PedidoCommit
import com.example.vapeon_movil.services.dto.PedidoCommitRespuesta
import com.example.vapeon_movil.services.dto.PedidoDocumento
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PedidoService {
    // Ruta relativa al mismo RetrofitClient; no se crea otra conexión Firebase.
    @POST("../documents:commit")
    suspend fun confirmarPedido(@Body body: PedidoCommit): PedidoCommitRespuesta

    // Permite comprobar un intento cuya respuesta se perdió, sin duplicar la compra.
    @GET("pedidos/{id}")
    suspend fun mostrarPedido(@Path("id") id: String): PedidoDocumento
}
