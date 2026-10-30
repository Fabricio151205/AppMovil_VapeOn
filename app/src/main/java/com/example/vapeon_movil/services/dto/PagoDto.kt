package com.example.vapeon_movil.services.dto

import com.example.vapeon_movil.entities.Pago
import com.example.vapeon_movil.entities.Pedido
import java.math.BigDecimal
import java.math.RoundingMode

data class PagoContainer(val fields: Map<String, FirestoreValor>)
data class PagoDocumento(val name: String, val fields: Map<String, FirestoreValor>, val updateTime: String? = null)

fun Pago.aFirestore(): PagoContainer {
    validar()
    fun texto(valor: String) = FirestoreValor(stringValue = valor)
    val campos = linkedMapOf(
        "id" to texto(id), "pedidoId" to texto(pedidoId), "usuarioId" to texto(usuarioId),
        "metodoPago" to texto(metodoPago), "modalidad" to texto(modalidad), "estadoPago" to texto(estadoPago),
        "numeroOperacion" to texto(numeroOperacion), "monto" to FirestoreValor(doubleValue = monto.toDouble()),
        "moneda" to texto(moneda), "fechaCreacion" to FirestoreValor(timestampValue = fechaCreacion),
        "estadoRegistroPedido" to texto(estadoRegistroPedido)
    )
    fechaResultado?.let { campos["fechaResultado"] = FirestoreValor(timestampValue = it) }
    tarjetaId?.let { campos["tarjetaId"] = texto(it) }
    return PagoContainer(campos)
}

fun PagoDocumento.aPagoDelUsuario(usuarioId: String): Pago {
    require(usuarioId.isNotBlank() && fields["usuarioId"]?.stringValue == usuarioId) { "No tienes acceso a este pago." }
    fun texto(campo: String) = requireNotNull(fields[campo]?.stringValue?.takeIf { it.isNotBlank() }) {
        "Pago incompleto: $campo."
    }
    val importe = fields["monto"]?.let {
        it.doubleValue?.takeIf { valor -> valor.isFinite() }?.let(BigDecimal::valueOf)
            ?: it.integerValue?.toBigDecimalOrNull()
    } ?: throw IllegalArgumentException("Importe de pago inválido.")
    val pago = Pago(texto("id"), texto("pedidoId"), usuarioId, texto("metodoPago"), texto("numeroOperacion"),
        importe.setScale(2, RoundingMode.UNNECESSARY), requireNotNull(fields["fechaCreacion"]?.timestampValue),
        texto("modalidad"), texto("estadoPago"), texto("moneda"), fields["fechaResultado"]?.timestampValue,
        texto("estadoRegistroPedido"), fields["tarjetaId"]?.stringValue)
    require(name.substringAfterLast('/') == pago.id) { "ID de pago inválido." }
    pago.validar()
    return pago
}

/** RNO02: se ejecuta antes de preparar cualquier escritura de pedido o inventario. */
internal fun validarPagoParaPedido(documento: PagoDocumento, pedido: Pedido): Pago {
    val pago = documento.aPagoDelUsuario(pedido.usuarioId)
    require(pago.estadoPago == "CONFIRMADO") { "El pago debe estar CONFIRMADO para registrar el pedido." }
    require(pago.estadoRegistroPedido == "PENDIENTE") { "El pago ya fue utilizado para registrar un pedido." }
    require(pago.pedidoId == pedido.id && pago.id == pedido.pagoId && pago.monto == pedido.total &&
        pago.metodoPago == pedido.metodoPago && pago.numeroOperacion == pedido.numeroOperacion &&
        pedido.estadoPago == "CONFIRMADO" && pedido.modalidadPago == "SIMULADO") { "El pago no corresponde a este pedido." }
    require(!documento.updateTime.isNullOrBlank()) { "Falta la versión del pago." }
    return pago
}

internal fun escrituraRegistroPago(documento: PagoDocumento, pedido: Pedido): PedidoEscritura {
    validarPagoParaPedido(documento, pedido)
    return PedidoEscritura(PedidoDocumento(documento.name,
        mapOf("estadoRegistroPedido" to FirestoreValor(stringValue = "REGISTRADO"))),
        PedidoPrecondicion(updateTime = documento.updateTime), PedidoMascara(listOf("estadoRegistroPedido")))
}
