package com.example.vapeon_movil.entities

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

/** Proyección de snapshots históricos; nunca utiliza precios o inventario del catálogo. */
data class VentaCaja(val pedido: Pedido, val productos: List<ProductoPedido>) {
    val importe: BigDecimal get() = productos.fold(BigDecimal.ZERO) { suma, linea -> suma + linea.subtotal }
}
data class CajaTotal(val pedidos: List<Pedido>, val invalidos: Int = 0) {
    val productos: BigDecimal get() = pedidos.fold(BigDecimal.ZERO) { suma, pedido -> suma + pedido.subtotal }
    val delivery: BigDecimal get() = pedidos.fold(BigDecimal.ZERO) { suma, pedido -> suma + pedido.delivery }
    val total: BigDecimal get() = productos + delivery
    val marcas: List<String> get() = marcasDeCaja(pedidos.flatMap { it.productos }.map { it.marca })
    fun ventas(marca: String = ""): List<VentaCaja> = pedidos.mapNotNull { pedido ->
        val lineas = pedido.productos.filter { marca.isBlank() || claveMarcaCaja(it.marca) == claveMarcaCaja(marca) }
        if (lineas.isEmpty()) null else VentaCaja(pedido, lineas)
    }
}
/** Equivalencia histórica exclusiva de Caja; no cambia los datos guardados. */
internal fun claveMarcaCaja(marca: String): String = when (val clave = marca.trim().lowercase(Locale.ROOT)) {
    "lifepood" -> "lifepod"
    else -> clave
}

/** Misma clave que el filtro de ventas, conservando una etiqueta de la fuente. */
internal fun marcasDeCaja(marcas: List<String>): List<String> = marcas.map { it.trim() }
    .filter { it.isNotBlank() }.sorted().distinctBy(::claveMarcaCaja)
private val zonaCaja = ZoneId.of("America/Lima")
private val formatoCaja = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)
fun fechaCaja(texto: String): LocalDate = LocalDate.parse(texto, formatoCaja)

/** Rango inclusivo por días locales. Un fallo no se interpreta como una Caja vacía. */
fun calcularCaja(pedidos: List<Pedido>, desde: String = "", hasta: String = ""): CajaTotal {
    val inicio = desde.takeIf { it.isNotBlank() }?.let(::fechaCaja)
    val fin = hasta.takeIf { it.isNotBlank() }?.let(::fechaCaja)
    require(inicio == null || fin == null || inicio <= fin) { "Desde no puede ser posterior a Hasta." }
    var invalidos = 0
    val validos = pedidos.distinctBy { it.id }.filter { pedido ->
        if (pedido.estado != "ENTREGADO") return@filter false
        try {
            val instante = Instant.parse(pedido.fecha)
            val dentro = (inicio == null || instante >= inicio.atStartOfDay(zonaCaja).toInstant()) &&
                (fin == null || instante < fin.plusDays(1).atStartOfDay(zonaCaja).toInstant())
            if (!dentro) return@filter false
            require(pedido.id.isNotBlank() && pedido.productos.isNotEmpty())
            require(pedido.productos.all { it.productoId.isNotBlank() && it.marca.isNotBlank() && it.cantidad > 0 &&
                it.precioUnitario.signum() > 0 && it.subtotal.compareTo(it.precioUnitario * BigDecimal(it.cantidad)) == 0 })
            val suma = pedido.productos.fold(BigDecimal.ZERO) { valor, linea -> valor + linea.subtotal }
            require(suma.compareTo(pedido.subtotal) == 0 && pedido.delivery.signum() >= 0 &&
                (suma + pedido.delivery).compareTo(pedido.total) == 0)
            true
        } catch (_: Exception) { invalidos++; false }
    }.sortedWith(compareByDescending<Pedido> { Instant.parse(it.fecha) }.thenBy { it.id })
    return CajaTotal(validos, invalidos)
}
