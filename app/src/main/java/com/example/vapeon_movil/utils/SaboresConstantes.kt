package com.example.vapeon_movil.utils

import com.example.vapeon_movil.entities.SaborItem

/**
 * Lista de marcas oficiales disponibles en VapeON.
 */
val LISTA_MARCAS_DISPONIBLES = listOf(
    "LifePod",
    "Oxbar",
    "Nexa",
    "Czar",
    "Argus",
    "EASE"
)

/**
 * Filtra una lista de SaborItem por el nombre de marca especificado.
 */
fun obtenerSaboresPorMarca(marca: String, listaSabores: List<SaborItem> = emptyList()): List<String> {
    if (marca.isBlank()) return emptyList()
    val marcaLimpia = marca.trim().lowercase()
    return listaSabores.filter {
        it.marca.lowercase().contains(marcaLimpia) || marcaLimpia.contains(it.marca.lowercase())
    }.map { it.nombre }.distinct()
}


