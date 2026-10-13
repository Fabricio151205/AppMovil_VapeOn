package com.example.vapeon_movil.entities

/**
 * Representa un producto dentro del carrito de compras del CLIENTE.
 *
 * Es una entidad local (en memoria) — no persiste en Firestore en esta versión.
 * El carrito se reinicia al cerrar sesión.
 *
 * @param productoId  ID del documento en Firestore (para referencia y evitar duplicados).
 * @param nombre      Nombre comercial del producto.
 * @param marca       Marca del producto (LifePod, Oxbar, Nexa...).
 * @param precioUnit  Precio unitario como Double para cálculos de resumen.
 * @param cantidad    Cantidad de unidades seleccionadas por el usuario (mínimo 1).
 * @param imagenRes   Resource ID del drawable de imagen del producto (opcional).
 */
data class ItemCarrito(
    val productoId: String,
    val nombre: String,
    val marca: String,
    val precioUnit: Double,
    val cantidad: Int = 1,
    val imagenRes: Int? = null
)
