package com.example.vapeon_movil.services.dto

import com.example.vapeon_movil.entities.Pedido
import com.example.vapeon_movil.entities.ProductoPedido
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

data class PedidoDocumento(val name: String, val fields: Map<String, FirestoreValor>)
data class PedidoCommit(val writes: List<PedidoEscritura>)
data class PedidoEscritura(
    val update: PedidoDocumento,
    val currentDocument: PedidoPrecondicion,
    val updateMask: PedidoMascara? = null
)
data class PedidoPrecondicion(val exists: Boolean? = null, val updateTime: String? = null)
data class PedidoMascara(val fieldPaths: List<String>)
data class PedidoCommitRespuesta(val commitTime: String)

fun Pedido.aDocumento(ruta: String): PedidoDocumento {
    fun texto(valor: String) = FirestoreValor(stringValue = valor)
    fun dinero(valor: java.math.BigDecimal) = FirestoreValor(doubleValue = valor.toDouble())
    return PedidoDocumento("$ruta/pedidos/$id", mapOf(
        "id" to texto(id), "usuarioId" to texto(usuarioId),
        "nombreCliente" to texto(nombreCliente), "telefono" to texto(telefono),
        "fecha" to FirestoreValor(timestampValue = fecha),
        "productos" to FirestoreValor(arrayValue = FirestoreArray(productos.map { item ->
            FirestoreValor(mapValue = FirestoreMapa(mapOf(
                "productoId" to texto(item.productoId), "nombre" to texto(item.nombre),
                "marca" to texto(item.marca),
                "cantidad" to FirestoreValor(integerValue = item.cantidad.toString()),
                "precioUnitario" to dinero(item.precioUnitario), "subtotal" to dinero(item.subtotal)
            )))
        })),
        "subtotal" to dinero(subtotal), "delivery" to dinero(delivery), "total" to dinero(total),
        "montoPagado" to dinero(montoPagado), "vuelto" to dinero(vuelto),
        "metodoPago" to texto(metodoPago), "estado" to texto(estado),
        "direccionEntrega" to texto(direccionEntrega)
    ) + listOfNotNull(
        pagoId?.let { "pagoId" to texto(it) }, estadoPago?.let { "estadoPago" to texto(it) },
        numeroOperacion?.let { "numeroOperacion" to texto(it) }, modalidadPago?.let { "modalidadPago" to texto(it) }
    ).toMap())
}

// RF26: consulta solo los documentos del cliente; no cambia la escritura de RF22.
data class PedidoConsulta(val structuredQuery: PedidoConsultaEstructurada)
data class PedidoConsultaEstructurada(
    val from: List<PedidoColeccion>,
    val where: PedidoFiltro? = null
)
data class PedidoColeccion(val collectionId: String)
data class PedidoFiltro(val fieldFilter: PedidoFiltroCampo)
data class PedidoFiltroCampo(val field: PedidoCampo, val op: String, val value: FirestoreValor)
data class PedidoCampo(val fieldPath: String)
data class PedidoConsultaRespuesta(val document: PedidoDocumento? = null)

fun consultaPedidosDelUsuario(usuarioId: String): PedidoConsulta {
    require(usuarioId.isNotBlank()) { "Inicia sesión para consultar tus compras." }
    return PedidoConsulta(PedidoConsultaEstructurada(
        listOf(PedidoColeccion("pedidos")), PedidoFiltro(PedidoFiltroCampo(
            PedidoCampo("usuarioId"), "EQUAL", FirestoreValor(stringValue = usuarioId)
        ))
    ))
}

/** Lee los importes y las líneas guardadas; no recalcula ni consulta productos. */
fun PedidoDocumento.aPedidoDelUsuario(usuarioId: String): Pedido {
    require(usuarioId.isNotBlank() && fields["usuarioId"]?.stringValue == usuarioId) {
        "No tienes acceso a este pedido."
    }
    return aPedido()
}

/** Conversión compartida; el ViewModel verifica el rol antes de usarla para ADMIN. */
internal fun PedidoDocumento.aPedido(): Pedido {
    fun Map<String, FirestoreValor>.texto(campo: String): String =
        this[campo]?.stringValue?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("El pedido tiene datos incompletos: $campo.")
    fun Map<String, FirestoreValor>.dinero(campo: String): BigDecimal {
        val valor = this[campo]
        val numero = valor?.doubleValue?.takeIf { it.isFinite() }?.let { BigDecimal.valueOf(it) }
            ?: valor?.integerValue?.toBigDecimalOrNull()
            ?: throw IllegalArgumentException("Importe inválido en el pedido: $campo.")
        require(numero.signum() >= 0) { "Importe inválido en el pedido: $campo." }
        return numero.setScale(2, RoundingMode.UNNECESSARY)
    }
    val idDocumento = name.substringAfterLast("/")
    require(idDocumento.isNotBlank() && fields.texto("id") == idDocumento) { "ID de pedido inválido." }
    val fecha = fields["fecha"]?.timestampValue
        ?: throw IllegalArgumentException("El pedido no tiene una fecha válida.")
    Instant.parse(fecha)
    val lineas = fields["productos"]?.arrayValue?.values
        ?: throw IllegalArgumentException("El pedido no tiene productos válidos.")
    require(lineas.isNotEmpty()) { "El pedido no tiene productos válidos." }
    return Pedido(
        id = idDocumento, usuarioId = fields.texto("usuarioId"),
        nombreCliente = fields.texto("nombreCliente"), telefono = fields.texto("telefono"), fecha = fecha,
        productos = lineas.map { linea ->
            val datos = linea.mapValue?.fields
                ?: throw IllegalArgumentException("Producto del pedido inválido.")
            val cantidad = datos["cantidad"]?.integerValue?.toIntOrNull()
            require(cantidad != null && cantidad > 0) { "Cantidad del pedido inválida." }
            ProductoPedido(datos.texto("productoId"), datos.texto("nombre"), datos.texto("marca"),
                cantidad, datos.dinero("precioUnitario"), datos.dinero("subtotal"))
        },
        subtotal = fields.dinero("subtotal"), delivery = fields.dinero("delivery"),
        total = fields.dinero("total"), montoPagado = fields.dinero("montoPagado"),
        vuelto = fields.dinero("vuelto"), direccionEntrega = fields["direccionEntrega"]?.stringValue.orEmpty(),
        metodoPago = fields.texto("metodoPago"), estado = fields.texto("estado"),
        pagoId = fields["pagoId"]?.stringValue, estadoPago = fields["estadoPago"]?.stringValue,
        numeroOperacion = fields["numeroOperacion"]?.stringValue, modalidadPago = fields["modalidadPago"]?.stringValue
    )
}

fun List<PedidoDocumento>.pedidosDelUsuario(usuarioId: String): List<Pedido> {
    require(usuarioId.isNotBlank()) { "Inicia sesión para consultar tus compras." }
    return leerPedidosValidos(usuarioId).first
}

/** La cantidad de documentos inválidos se informa en la pantalla; no se ocultan silenciosamente. */
internal fun List<PedidoDocumento>.leerPedidosValidos(usuarioId: String? = null): Pair<List<Pedido>, Int> {
    val validos = mutableListOf<Pedido>()
    var invalidos = 0
    for (documento in this) {
        if (usuarioId != null && runCatching { documento.fields["usuarioId"]?.stringValue }.getOrNull() != usuarioId) continue
        try { validos += documento.aPedido() }
        catch (_: Exception) { invalidos++ }
    }
    return validos.sortedByDescending { Instant.parse(it.fecha) } to invalidos
}

// RF28: cuerpo parcial. La máscara del Service limita la escritura a estado.
data class PedidoEstadoActualizacion(val fields: Map<String, FirestoreValor>)
