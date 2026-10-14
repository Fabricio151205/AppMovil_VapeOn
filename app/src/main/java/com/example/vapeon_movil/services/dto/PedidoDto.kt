package com.example.vapeon_movil.services.dto

import com.example.vapeon_movil.entities.Pedido

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
    ))
}
