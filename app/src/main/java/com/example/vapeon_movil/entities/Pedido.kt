package com.example.vapeon_movil.entities

import java.math.BigDecimal
import java.math.RoundingMode

data class ProductoPedido(
    val productoId: String,
    val nombre: String,
    val marca: String,
    val cantidad: Int,
    val precioUnitario: BigDecimal,
    val subtotal: BigDecimal
)

/** Copia de la compra: no depende de cambios posteriores en el catálogo. */
data class Pedido(
    val id: String,
    val usuarioId: String,
    val nombreCliente: String,
    val telefono: String,
    val fecha: String,
    val productos: List<ProductoPedido>,
    val subtotal: BigDecimal,
    val delivery: BigDecimal,
    val total: BigDecimal,
    val montoPagado: BigDecimal,
    val vuelto: BigDecimal,
    val direccionEntrega: String,
    val metodoPago: String = "EFECTIVO",
    val estado: String = "REGISTRADO"
) {
    companion object {
        /** Acepta coma o punto decimal, sin exponentes ni más de dos decimales. */
        fun leerMonto(texto: String): BigDecimal? {
            val limpio = texto.trim().replace(',', '.')
            if (!Regex("[0-9]+(\\.[0-9]{1,2})?").matches(limpio)) return null
            return limpio.toBigDecimalOrNull()?.takeIf { it.signum() >= 0 }
                ?.setScale(2, RoundingMode.UNNECESSARY)
        }

        fun subtotal(items: List<ItemCarrito>): BigDecimal {
            require(items.isNotEmpty()) { "Tu carrito está vacío." }
            require(items.map { it.productoId }.distinct().size == items.size) {
                "El carrito contiene productos duplicados."
            }
            return items.fold(BigDecimal.ZERO.setScale(2)) { suma, item ->
                require(item.productoId.isNotBlank() && item.nombre.isNotBlank() &&
                    item.cantidad > 0 && item.precioUnit.isFinite()) { "Producto o cantidad inválidos." }
                val precio = leerMonto(BigDecimal.valueOf(item.precioUnit).stripTrailingZeros().toPlainString())
                require(precio != null && precio.signum() > 0) { "Precio inválido para ${item.nombre}." }
                suma + precio.multiply(BigDecimal(item.cantidad))
            }
        }

        fun validarStock(stock: String?, cantidad: Int): Int {
            require(cantidad > 0) { "La cantidad debe ser mayor que cero." }
            val actual = stock?.toIntOrNull()
            require(actual != null && actual >= 0) { "El producto tiene un stock inválido." }
            require(actual >= cantidad) { "Stock insuficiente para este producto." }
            return actual - cantidad
        }

        fun calcularVuelto(monto: BigDecimal, total: BigDecimal): BigDecimal {
            require(monto >= total) { "El monto ingresado es insuficiente." }
            return monto - total
        }
    }
}
