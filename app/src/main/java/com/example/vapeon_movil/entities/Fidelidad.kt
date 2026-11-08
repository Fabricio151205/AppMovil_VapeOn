package com.example.vapeon_movil.entities

import java.math.BigDecimal
import java.time.Instant
import java.util.Locale

const val MAX_PUNTOS_FIDELIDAD = 6

enum class EstadoRecompensaFidelidad { PENDIENTE, ENTREGADA }

data class ConfiguracionFidelidad(
    val productoId: String,
    val nombre: String,
    val marca: String,
    val categoria: String,
    val precioReferencia: BigDecimal,
    val configuradoPor: String,
    val fechaConfiguracion: String,
    val updateTime: String? = null
)

data class FidelidadCliente(
    val usuarioId: String,
    val puntosActuales: Int = 0,
    val numeroCiclo: Int = 1,
    val recompensaPendienteId: String? = null,
    val fechaActualizacion: String,
    val updateTime: String? = null
) {
    init {
        require(usuarioId.isNotBlank())
        require(puntosActuales in 0..MAX_PUNTOS_FIDELIDAD)
        require(numeroCiclo >= 1)
    }
}

data class EventoFidelidad(
    val pedidoId: String,
    val usuarioId: String,
    val numeroCiclo: Int,
    val fechaPedido: String,
    val fechaAcreditacion: String,
    val resultado: String
)

data class RecompensaFidelidad(
    val id: String,
    val usuarioId: String,
    val numeroCiclo: Int,
    val estado: EstadoRecompensaFidelidad,
    val productoId: String? = null,
    val nombreProducto: String? = null,
    val marcaProducto: String? = null,
    val categoriaProducto: String? = null,
    val precioReferencia: BigDecimal? = null,
    val fechaGeneracion: String,
    val fechaEntrega: String? = null,
    val entregadaPor: String? = null,
    val updateTime: String? = null
)

data class ClienteFidelidad(
    val usuario: Usuario,
    val fidelidad: FidelidadCliente,
    val recompensa: RecompensaFidelidad? = null
)

data class ResultadoAcreditacionFidelidad(
    val fidelidadAnterior: FidelidadCliente,
    val fidelidadNueva: FidelidadCliente,
    val evento: EventoFidelidad?,
    val recompensaNueva: RecompensaFidelidad?,
    val yaProcesado: Boolean = false
)

fun procesarPedidoParaFidelidad(
    pedido: Pedido,
    fidelidadActual: FidelidadCliente?,
    eventoExistente: Boolean,
    configuracion: ConfiguracionFidelidad?,
    ahora: String = Instant.now().toString()
): ResultadoAcreditacionFidelidad? {
    if (pedido.estado != "ENTREGADO" || pedido.productos.isEmpty()) return null
    val actual = fidelidadActual ?: FidelidadCliente(
        usuarioId = pedido.usuarioId,
        fechaActualizacion = ahora
    )
    require(actual.usuarioId == pedido.usuarioId)
    if (eventoExistente) return ResultadoAcreditacionFidelidad(actual, actual, null, null, true)

    if (actual.puntosActuales == MAX_PUNTOS_FIDELIDAD &&
        !actual.recompensaPendienteId.isNullOrBlank()
    ) {
        return ResultadoAcreditacionFidelidad(
            actual,
            actual,
            EventoFidelidad(
                pedido.id, pedido.usuarioId, actual.numeroCiclo, pedido.fecha, ahora,
                "OMITIDO_RECOMPENSA_PENDIENTE"
            ),
            null
        )
    }

    val puntos = (actual.puntosActuales + 1).coerceAtMost(MAX_PUNTOS_FIDELIDAD)
    val recompensaId = if (puntos == MAX_PUNTOS_FIDELIDAD) {
        "${pedido.usuarioId}_ciclo_${actual.numeroCiclo}"
    } else null
    val nueva = actual.copy(
        puntosActuales = puntos,
        recompensaPendienteId = recompensaId,
        fechaActualizacion = ahora
    )
    val recompensa = recompensaId?.let { id ->
        RecompensaFidelidad(
            id = id,
            usuarioId = pedido.usuarioId,
            numeroCiclo = actual.numeroCiclo,
            estado = EstadoRecompensaFidelidad.PENDIENTE,
            productoId = configuracion?.productoId,
            nombreProducto = configuracion?.nombre,
            marcaProducto = configuracion?.marca,
            categoriaProducto = configuracion?.categoria,
            precioReferencia = configuracion?.precioReferencia,
            fechaGeneracion = ahora
        )
    }
    return ResultadoAcreditacionFidelidad(
        actual,
        nueva,
        EventoFidelidad(pedido.id, pedido.usuarioId, actual.numeroCiclo, pedido.fecha, ahora, "ACREDITADO"),
        recompensa
    )
}

data class ResultadoEntregaRecompensa(
    val fidelidad: FidelidadCliente,
    val recompensa: RecompensaFidelidad
)

fun prepararEntregaRecompensa(
    fidelidad: FidelidadCliente,
    recompensa: RecompensaFidelidad,
    configuracion: ConfiguracionFidelidad?,
    adminId: String,
    ahora: String = Instant.now().toString()
): ResultadoEntregaRecompensa {
    require(adminId.isNotBlank())
    require(fidelidad.puntosActuales == MAX_PUNTOS_FIDELIDAD)
    require(fidelidad.recompensaPendienteId == recompensa.id)
    require(recompensa.estado == EstadoRecompensaFidelidad.PENDIENTE)
    val productoId = recompensa.productoId ?: configuracion?.productoId
    val nombreProducto = recompensa.nombreProducto ?: configuracion?.nombre
    val marcaProducto = recompensa.marcaProducto ?: configuracion?.marca
    val categoriaProducto = recompensa.categoriaProducto ?: configuracion?.categoria
    val precioReferencia = recompensa.precioReferencia ?: configuracion?.precioReferencia
    require(!productoId.isNullOrBlank() && !nombreProducto.isNullOrBlank() &&
        !marcaProducto.isNullOrBlank() && categoriaProducto != null && precioReferencia != null) {
        "Configura un producto antes de entregar la recompensa."
    }
    val entregada = recompensa.copy(
        estado = EstadoRecompensaFidelidad.ENTREGADA,
        // Una recompensa generada conserva su snapshot. La configuración actual
        // solo completa recompensas que llegaron a 6 sin producto configurado.
        productoId = productoId,
        nombreProducto = nombreProducto,
        marcaProducto = marcaProducto,
        categoriaProducto = categoriaProducto,
        precioReferencia = precioReferencia,
        fechaEntrega = ahora,
        entregadaPor = adminId
    )
    return ResultadoEntregaRecompensa(
        fidelidad.copy(
            puntosActuales = 0,
            numeroCiclo = fidelidad.numeroCiclo + 1,
            recompensaPendienteId = null,
            fechaActualizacion = ahora
        ),
        entregada
    )
}

fun ordenarClientesPorFidelidad(clientes: List<ClienteFidelidad>): List<ClienteFidelidad> =
    clientes.filter { it.usuario.rol.equals("CLIENTE", true) }
        .sortedWith(
            compareByDescending<ClienteFidelidad> { it.fidelidad.puntosActuales }
                .thenBy { "${it.usuario.nombre} ${it.usuario.apellido}".trim().lowercase(Locale.ROOT) }
                .thenBy { it.usuario.id }
        )
