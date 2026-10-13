package com.example.vapeon_movil.entities

/**
 * Resumen financiero calculado en tiempo real desde los ítems del carrito.
 *
 * @param subTotal  Suma de (precioUnit × cantidad) de todos los ítems.
 * @param delivery  Costo fijo de envío (S/. 5.00 por defecto).
 * @param igv       IGV calculado sobre el subTotal (18% — actualmente incluido en precio).
 * @param total     subTotal + delivery (el IGV ya está incluido en el precio de venta).
 */
data class ResumenPago(
    val subTotal: Double,
    val delivery: Double = 5.0,
    val igv: Double = 0.0,       // IGV incluido en precio; se muestra informativo
    val total: Double
) {
    companion object {
        private const val DELIVERY_FIJO = 5.0

        /**
         * Calcula el [ResumenPago] a partir de la lista de ítems del carrito.
         * El IGV (18%) ya está incluido en el precio unitario de cada producto,
         * por lo que se muestra como S/. 0 en el desglose (precio con IGV incluido).
         */
        fun calcular(items: List<ItemCarrito>): ResumenPago {
            val subTotal = items.sumOf { it.precioUnit * it.cantidad }
            return ResumenPago(
                subTotal  = subTotal,
                delivery  = DELIVERY_FIJO,
                igv       = 0.0,
                total     = subTotal + DELIVERY_FIJO
            )
        }
    }
}
