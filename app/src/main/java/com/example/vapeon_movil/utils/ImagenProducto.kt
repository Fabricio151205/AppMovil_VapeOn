package com.example.vapeon_movil.utils

import com.example.vapeon_movil.entities.ItemCarrito
import com.example.vapeon_movil.entities.Producto
import com.example.vapeon_movil.entities.SaborItem

/**
 * Resuelve estrictamente la imagen (data URI base64) de un sabor que coincide exactamente con [nombresSabor].
 * A diferencia de [imagenDeSabor], no devuelve una imagen por defecto de la marca si el sabor no coincide.
 */
fun imagenDeSaborEstricto(
    marca: String,
    nombresSabor: List<String>,
    sabores: List<SaborItem>
): String? {
    if (marca.isBlank() || sabores.isEmpty()) return null

    val imagenesMarca = sabores.filter {
        mismaMarca(it.marca, marca) && !it.imagenUri.isNullOrBlank()
    }
    if (imagenesMarca.isEmpty()) return null

    nombresSabor.map { it.trim() }
        .filter { it.isNotBlank() }
        .forEach { nombre ->
            imagenesMarca.firstOrNull { it.nombre.equals(nombre, ignoreCase = true) }
                ?.imagenUri
                ?.let { return it }
        }

    return null
}

/**
 * Resuelve la imagen (data URI base64) del sabor que corresponde a [marca] y a
 * alguno de los nombres en [nombresSabor].
 *
 * La búsqueda ignora mayúsculas/minúsculas y usa [mismaMarca] para tolerar
 * variantes conocidas de la marca. Si ninguno de los nombres coincide pero la
 * marca sí tiene imágenes registradas, se devuelve la primera imagen de esa
 * marca para no dejar la vista sin imagen.
 */
fun imagenDeSabor(
    marca: String,
    nombresSabor: List<String>,
    sabores: List<SaborItem>
): String? {
    if (marca.isBlank() || sabores.isEmpty()) return null

    val imagenesMarca = sabores.filter {
        mismaMarca(it.marca, marca) && !it.imagenUri.isNullOrBlank()
    }
    if (imagenesMarca.isEmpty()) return null

    nombresSabor.map { it.trim() }
        .filter { it.isNotBlank() }
        .forEach { nombre ->
            imagenesMarca.firstOrNull { it.nombre.equals(nombre, ignoreCase = true) }
                ?.imagenUri
                ?.let { return it }
        }

    return imagenesMarca.firstOrNull()?.imagenUri
}

/**
 * Imagen representativa de un [Producto] a partir de su marca y de los sabores
 * declarados en `stockPorSabor` o en el campo legado `sabor`.
 */
fun imagenProducto(producto: Producto, sabores: List<SaborItem>): String? {
    val nombresSabor = when {
        producto.stockPorSabor.isNotEmpty() -> producto.stockPorSabor.keys.toList()
        producto.sabor.isNotBlank() -> producto.sabor.split(",").map { it.trim() }
        else -> emptyList()
    }
    return imagenDeSabor(producto.marca, nombresSabor, sabores)
}

/**
 * Imagen de una línea del carrito. Prioriza el sabor elegido por el cliente.
 */
fun imagenItemCarrito(item: ItemCarrito, sabores: List<SaborItem>): String? =
    imagenDeSabor(item.marca, listOfNotNull(item.saborSeleccionado), sabores)
