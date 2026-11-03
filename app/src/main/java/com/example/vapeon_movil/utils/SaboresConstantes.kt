package com.example.vapeon_movil.utils

import com.example.vapeon_movil.entities.SaborItem
import java.util.Locale

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
 * Clave estable para comparar identidades de marca sin alterar el texto guardado.
 * Los alias son explícitos: no se aplican coincidencias parciales ni aproximadas.
 */
fun claveMarca(marca: String): String = when (val clave = marca.trim().lowercase(Locale.ROOT)) {
    "lifepood" -> "lifepod"
    else -> clave
}

fun mismaMarca(primera: String, segunda: String): Boolean =
    claveMarca(primera) == claveMarca(segunda)

/** Conserva la primera etiqueta legible de cada identidad, en el orden recibido. */
fun marcasUnicas(marcas: Iterable<String>): List<String> = marcas.map { it.trim() }
    .filter { it.isNotBlank() }
    .distinctBy(::claveMarca)

/**
 * Filtra una lista de SaborItem por el nombre de marca especificado.
 */
fun obtenerSaboresPorMarca(marca: String, listaSabores: List<SaborItem> = emptyList()): List<String> {
    if (marca.isBlank()) return emptyList()
    return listaSabores.filter { mismaMarca(it.marca, marca) }
        .map { it.nombre }.distinct()
}


