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
