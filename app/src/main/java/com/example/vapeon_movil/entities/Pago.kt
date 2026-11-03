package com.example.vapeon_movil.entities

import java.math.BigDecimal
import java.time.Instant

/** Operación exclusivamente simulada. No contiene credenciales bancarias. */
data class Pago(
    val id: String,
    val pedidoId: String,
    val usuarioId: String,
    val metodoPago: String,
    val numeroOperacion: String,
    val monto: BigDecimal,
    val fechaCreacion: String,
    val modalidad: String = "SIMULADO",
    val estadoPago: String = "PENDIENTE",
    val moneda: String = "PEN",
    val fechaResultado: String? = null,
    val estadoRegistroPedido: String = "PENDIENTE",
    val tarjetaId: String? = null
) {
    fun validar() {
        require(listOf(id, pedidoId, usuarioId).all { it.isNotBlank() && '/' !in it }) { "Identidad del pago inválida." }
        require(metodoPago in listOf("YAPE", "TARJETA") && modalidad == "SIMULADO" && moneda == "PEN") {
            "Método o modalidad de pago inválidos."
        }
        require(numeroOperacion == "SIM-$metodoPago-$id") { "Número de operación inválido." }
        require(monto.signum() > 0 && monto.stripTrailingZeros().scale() <= 2 && monto.toDouble().isFinite() &&
            BigDecimal.valueOf(monto.toDouble()).compareTo(monto) == 0) { "Importe de pago inválido." }
        require(estadoPago in listOf("PENDIENTE", "CONFIRMADO", "RECHAZADO")) { "Estado financiero inválido." }
        require(estadoRegistroPedido in listOf("PENDIENTE", "REGISTRADO")) { "Estado de registro inválido." }
        require(estadoRegistroPedido != "REGISTRADO" || estadoPago == "CONFIRMADO") { "Pago no confirmado." }
        Instant.parse(fechaCreacion)
        if (estadoPago == "PENDIENTE") require(fechaResultado == null) { "Resultado prematuro." }
        else Instant.parse(requireNotNull(fechaResultado) { "Falta fecha del resultado." })
        require(if (metodoPago == "TARJETA") !tarjetaId.isNullOrBlank() && '/' !in tarjetaId else tarjetaId == null) {
            "Referencia de tarjeta inválida."
        }
    }

    fun mismoIntento(otro: Pago): Boolean = copy(estadoPago = "PENDIENTE", fechaResultado = null,
        estadoRegistroPedido = "PENDIENTE") == otro.copy(estadoPago = "PENDIENTE", fechaResultado = null,
        estadoRegistroPedido = "PENDIENTE")
}
