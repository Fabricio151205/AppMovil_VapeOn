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
 * Identificador estable para sembrar marcas en Firestore sin duplicarlas.
 * Ejemplo: "LifePod" -> "lifepod".
 */
fun idMarca(nombre: String): String = claveMarca(nombre)
    .replace(Regex("[^a-z0-9]+"), "_")
    .trim('_')
    .ifBlank { "marca" }

/**
 * Usa la lista base solo cuando Firestore todavía no tiene marcas registradas.
 * Así los formularios de producto y sabor no quedan bloqueados en una base nueva.
 */
fun marcasParaFormulario(marcasFirestore: Iterable<String>): List<String> =
    marcasUnicas(marcasFirestore).ifEmpty { LISTA_MARCAS_DISPONIBLES }

/**
 * Filtra una lista de SaborItem por el nombre de marca especificado.
 */
fun obtenerSaboresPorMarca(marca: String, listaSabores: List<SaborItem> = emptyList()): List<String> {
    if (marca.isBlank()) return emptyList()
    return listaSabores.filter { mismaMarca(it.marca, marca) }
        .map { it.nombre }.distinct()
}


