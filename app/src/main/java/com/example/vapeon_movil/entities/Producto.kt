package com.example.vapeon_movil.entities

data class Producto(
    val id: String = "",
    val nombre: String,
    val marca: String,
    val precio: Double = 0.0,
    val stock: Int = 0,
    val descripcion: String,
    val sabor: String = "",
    val stockPorSabor: Map<String, Int> = emptyMap()
) {
    /**
     * Convierte el mapa interno en una lista de SaborItem vinculados a la marca del producto.
     */
    val listaSaboresItems: List<SaborItem>
        get() = if (stockPorSabor.isNotEmpty()) {
            stockPorSabor.map { (nombreSabor, cantidadStock) ->
                SaborItem(nombre = nombreSabor, marca = marca, stock = cantidadStock)
            }
        } else if (sabor.isNotBlank()) {
            sabor.split(",").map { it.trim() }.filter { it.isNotBlank() }.map { nombreSabor ->
                SaborItem(nombre = nombreSabor, marca = marca, stock = stock)
            }
        } else {
            emptyList()
        }

    /**
     * Retorna el stock total disponible.
     * Si se definió inventario individual por sabor, calcula la suma de todos los sabores.
     * Si no, utiliza el stock general por defecto.
     */
    val stockTotal: Int
        get() = if (stockPorSabor.isNotEmpty()) stockPorSabor.values.sum() else stock

    /**
     * Obtiene el stock disponible para un sabor específico.
     */
    fun obtenerStockDeSabor(nombreSabor: String): Int {
        return stockPorSabor.entries.find { it.key.equals(nombreSabor, ignoreCase = true) }?.value
            ?: if (stockPorSabor.isEmpty()) stock else 0
    }
}
