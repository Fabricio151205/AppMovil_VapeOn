package com.example.vapeon_movil.entities

/**
 * Entrada del catálogo maestro de sabores asociada a una marca.
 *
 * [stock] se conserva únicamente para poder leer documentos antiguos de `sabores`.
 * No representa inventario de venta: ese dato vive en `Producto.stockPorSabor`.
 */
data class SaborItem(
    val nombre: String,
    val marca: String,
    /** Campo legado de solo lectura; no debe enviarse en nuevas escrituras. */
    val stock: Int = 1,
    val id: String = ""
)
