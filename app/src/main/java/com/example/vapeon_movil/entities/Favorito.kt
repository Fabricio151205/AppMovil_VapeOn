package com.example.vapeon_movil.entities

/**
 * Producto guardado como favorito por un cliente en la colección `favoritos`.
 *
 * Solo se persiste la relación entre el cliente y el producto. Los datos que se
 * muestran en pantalla (nombre, marca, precio, stock e imagen) se resuelven con
 * el catálogo vigente para no conservar copias desactualizadas.
 */
data class Favorito(
    val id: String = "",
    val usuarioId: String = "",
    val productoId: String = "",
    val fechaCreacion: String = ""
) {
    val esValido: Boolean
        get() = id.isNotBlank() && usuarioId.isNotBlank() && productoId.isNotBlank()
}

/**
 * Popularidad agregada de un producto según la cantidad de favoritos.
 * No contiene `usuarioId`: el administrador solo ve el conteo por producto.
 */
data class PopularidadProducto(
    val productoId: String,
    val favoritos: Int
) {
    init {
        require(productoId.isNotBlank()) { "El producto de popularidad es inválido." }
        require(favoritos > 0) { "La popularidad debe ser mayor que cero." }
    }
}

