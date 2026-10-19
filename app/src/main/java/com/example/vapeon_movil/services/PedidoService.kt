package com.example.vapeon_movil.services

import com.example.vapeon_movil.services.dto.PedidoCommit
import com.example.vapeon_movil.services.dto.PedidoCommitRespuesta
import com.example.vapeon_movil.services.dto.PedidoDocumento
import com.example.vapeon_movil.services.dto.PedidoConsulta
import com.example.vapeon_movil.services.dto.PedidoConsultaRespuesta
import com.example.vapeon_movil.services.dto.PedidoEstadoActualizacion
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PATCH

interface PedidoService {
    @PATCH("pedidos/{id}?updateMask.fieldPaths=estado&currentDocument.exists=true")
    suspend fun actualizarEstadoPedido(
        @Path("id") id: String,
        @Body body: PedidoEstadoActualizacion
    ): PedidoDocumento

    @POST("../documents:runQuery")
    suspend fun consultarPedidos(@Body body: PedidoConsulta): List<PedidoConsultaRespuesta>

    // Ruta relativa al mismo RetrofitClient; no se crea otra conexión Firebase.
    @POST("../documents:commit")
    suspend fun confirmarPedido(@Body body: PedidoCommit): PedidoCommitRespuesta

    // Permite comprobar un intento cuya respuesta se perdió, sin duplicar la compra.
    @GET("pedidos/{id}")
    suspend fun mostrarPedido(@Path("id") id: String): PedidoDocumento
}
