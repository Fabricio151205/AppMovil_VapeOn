package com.example.vapeon_movil.services

import com.example.vapeon_movil.entities.Pedido
import com.example.vapeon_movil.entities.ConfiguracionFidelidad
import com.example.vapeon_movil.entities.prepararEntregaRecompensa
import com.example.vapeon_movil.entities.procesarPedidoParaFidelidad
import java.time.Instant
import com.example.vapeon_movil.services.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FidelidadService {
    @GET("configuracionFidelidad/actual")
    suspend fun configuracion(): Response<FidelidadDocumento>

    @GET("fidelidades/{usuarioId}")
    suspend fun fidelidad(@Path("usuarioId") usuarioId: String): Response<FidelidadDocumento>

    @GET("eventosFidelidad/{pedidoId}")
    suspend fun evento(@Path("pedidoId") pedidoId: String): Response<FidelidadDocumento>

    @GET("recompensasFidelidad/{id}")
    suspend fun recompensa(@Path("id") id: String): Response<FidelidadDocumento>

    @GET("fidelidades")
    suspend fun listarFidelidades(@Query("pageToken") pageToken: String? = null): FidelidadLista

    @POST("../documents:commit")
    suspend fun commit(@Body body: FidelidadCommit): FidelidadCommitRespuesta
}

suspend fun FidelidadService.listarTodasFidelidades(): List<FidelidadDocumento> {
    val resultado = mutableListOf<FidelidadDocumento>()
    val tokens = mutableSetOf<String>()
    var token: String? = null
    do {
        val pagina = listarFidelidades(token)
        resultado += pagina.documents.orEmpty()
        token = pagina.nextPageToken?.takeIf { it.isNotBlank() }
        check(token == null || tokens.add(token)) { "Firestore repitió un token de fidelidad." }
    } while (token != null)
    return resultado.distinctBy { it.name }
}

internal fun <T> Response<T>.cuerpoOpcional(): T? = when {
    isSuccessful -> body()
    code() == 404 -> null
    else -> throw retrofit2.HttpException(this)
}

data class AcreditacionEntregaRemota(val eventoCreado: Boolean, val yaProcesado: Boolean)
data class EntregaRecompensaRemota(
    val fidelidad: com.example.vapeon_movil.entities.FidelidadCliente,
    val recompensa: com.example.vapeon_movil.entities.RecompensaFidelidad,
    val configuracionUpdateTime: String? = null
)

/**
 * Confirma en una sola operación Firestore el estado ENTREGADO y su efecto de fidelidad.
 * El marcador con ID de pedido y `exists=false` hace idempotente la acreditación entre dispositivos.
 */
suspend fun acreditarEntregaAtomica(
    servicio: FidelidadService,
    documentoPedido: PedidoDocumento,
    pedidoEntregado: Pedido
): AcreditacionEntregaRemota {
    require(pedidoEntregado.estado == "ENTREGADO" && pedidoEntregado.productos.isNotEmpty())
    require(documentoPedido.name.substringAfterLast('/') == pedidoEntregado.id)
    require(!documentoPedido.updateTime.isNullOrBlank())

    val eventoExistente = servicio.evento(pedidoEntregado.id).cuerpoOpcional() != null
    if (eventoExistente) return AcreditacionEntregaRemota(false, true)

    val fidelidadDoc = servicio.fidelidad(pedidoEntregado.usuarioId).cuerpoOpcional()
    val fidelidad = fidelidadDoc?.aFidelidad()
    val configuracion = servicio.configuracion().cuerpoOpcional()?.aConfiguracion()
    val resultado = requireNotNull(procesarPedidoParaFidelidad(
        pedidoEntregado, fidelidad, eventoExistente = false, configuracion = configuracion
    ))
    val ruta = documentoPedido.name.substringBeforeLast("/pedidos/")
    val writes = mutableListOf(
        FidelidadEscritura(
            update = FidelidadDocumento(
                documentoPedido.name,
                mapOf("estado" to FirestoreValor(stringValue = "ENTREGADO"))
            ),
            currentDocument = FidelidadPrecondicion(updateTime = documentoPedido.updateTime),
            updateMask = FidelidadMascara(listOf("estado"))
        ),
        FidelidadEscritura(
            update = requireNotNull(resultado.evento).aDocumento(ruta),
            currentDocument = FidelidadPrecondicion(exists = false)
        )
    )
    if (resultado.fidelidadNueva != resultado.fidelidadAnterior) {
        writes += FidelidadEscritura(
            update = resultado.fidelidadNueva.aDocumento(ruta),
            currentDocument = fidelidadDoc?.updateTime?.let { FidelidadPrecondicion(updateTime = it) }
                ?: FidelidadPrecondicion(exists = false),
            updateMask = fidelidadDoc?.let {
                FidelidadMascara(listOf(
                    "usuarioId", "puntosActuales", "numeroCiclo",
                    "recompensaPendienteId", "fechaActualizacion"
                ))
            }
        )
    }
    resultado.recompensaNueva?.let {
        writes += FidelidadEscritura(
            update = it.aDocumento(ruta),
            currentDocument = FidelidadPrecondicion(exists = false)
        )
    }
    servicio.commit(FidelidadCommit(writes))
    return AcreditacionEntregaRemota(true, false)
}

suspend fun entregarRecompensaAtomica(
    servicio: FidelidadService,
    usuarioId: String,
    adminId: String,
    ahora: String = Instant.now().toString(),
    configuracionSeleccionada: ConfiguracionFidelidad? = null
): EntregaRecompensaRemota {
    require(usuarioId.isNotBlank() && !usuarioId.contains('/'))
    require(adminId.isNotBlank())
    val fidelidadDoc = servicio.fidelidad(usuarioId).cuerpoOpcional()
        ?: throw IllegalArgumentException("El cliente no tiene recompensa pendiente.")
    val fidelidad = fidelidadDoc.aFidelidad()
    val recompensaId = fidelidad.recompensaPendienteId
        ?: throw IllegalArgumentException("El cliente no tiene recompensa pendiente.")
    val recompensaDoc = servicio.recompensa(recompensaId).cuerpoOpcional()
        ?: throw IllegalArgumentException("La recompensa pendiente no está disponible.")
    val recompensa = recompensaDoc.aRecompensa()
    val requiereConfiguracion = recompensa.productoId.isNullOrBlank() ||
        recompensa.nombreProducto.isNullOrBlank() || recompensa.marcaProducto.isNullOrBlank() ||
        recompensa.categoriaProducto == null || recompensa.precioReferencia == null
    val config = if (requiereConfiguracion) {
        requireNotNull(configuracionSeleccionada) {
            "Configura un producto antes de entregar la recompensa."
        }.also {
            require(!it.updateTime.isNullOrBlank()) {
                "Recarga la configuración antes de entregar la recompensa."
            }
        }
    } else null
    require(!fidelidadDoc.updateTime.isNullOrBlank() && !recompensaDoc.updateTime.isNullOrBlank()) {
        "Recarga la información antes de entregar la recompensa."
    }
    val entrega = prepararEntregaRecompensa(
        fidelidad, recompensa, config, adminId, ahora
    )
    val escrituras = mutableListOf(
        FidelidadEscritura(
            entrega.fidelidad.aDocumento(RUTA_DOCUMENTOS_FIRESTORE),
            FidelidadPrecondicion(updateTime = fidelidadDoc.updateTime),
            FidelidadMascara(listOf(
                "usuarioId", "puntosActuales", "numeroCiclo",
                "recompensaPendienteId", "fechaActualizacion"
            ))
        ),
        FidelidadEscritura(
            entrega.recompensa.aDocumento(RUTA_DOCUMENTOS_FIRESTORE),
            FidelidadPrecondicion(updateTime = recompensaDoc.updateTime),
            FidelidadMascara(listOf(
                "usuarioId", "numeroCiclo", "estado", "productoId", "nombreProducto",
                "marcaProducto", "categoriaProducto", "precioReferencia", "fechaGeneracion",
                "fechaEntrega", "entregadaPor"
            ))
        )
    )
    // Si la recompensa todavía no tenía snapshot, reescribe los mismos valores
    // seleccionados con su updateTime. Si otro ADMIN cambió la configuración,
    // Firestore rechaza atómicamente todo el commit por versión obsoleta.
    config?.let {
        escrituras += FidelidadEscritura(
            update = it.aDocumento(RUTA_DOCUMENTOS_FIRESTORE),
            currentDocument = FidelidadPrecondicion(updateTime = it.updateTime),
            updateMask = FidelidadMascara(listOf(
                "productoId", "nombre", "marca", "categoria", "precioReferencia",
                "configuradoPor", "fechaConfiguracion"
            ))
        )
    }
    val respuesta = servicio.commit(FidelidadCommit(escrituras))
    val versionConfiguracion = if (config != null) {
        respuesta.writeResults.getOrNull(escrituras.lastIndex)?.updateTime
            ?: throw IllegalStateException("Firestore no devolvió la versión de la configuración.")
    } else null
    return EntregaRecompensaRemota(
        entrega.fidelidad,
        entrega.recompensa,
        versionConfiguracion
    )
}
