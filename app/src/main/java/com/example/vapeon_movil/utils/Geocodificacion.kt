package com.example.vapeon_movil.utils

/** Longitud máxima aceptada por el formulario de direcciones. */
internal const val MAX_LONGITUD_DIRECCION = 160

/**
 * Construye un texto de dirección legible a partir de los datos que devuelve
 * el geocodificador inverso de Android.
 *
 * Se prefiere la línea completa del sistema; si no está disponible o excede el
 * límite del formulario, se compone con calle, número, distrito y ciudad.
 * Devuelve `null` cuando no hay ningún dato aprovechable.
 */
internal fun componerDireccionLegible(
    lineaPrincipal: String?,
    calle: String?,
    numero: String?,
    distrito: String?,
    ciudad: String?
): String? {
    fun limpio(valor: String?): String? = valor?.trim()?.takeIf { it.isNotBlank() }

    val principal = limpio(lineaPrincipal)
    if (principal != null && principal.length <= MAX_LONGITUD_DIRECCION) return principal

    val via = listOfNotNull(limpio(calle), limpio(numero))
        .joinToString(" ")
        .trim()
        .takeIf { it.isNotBlank() }

    val partes = buildList {
        via?.let { add(it) }
        limpio(distrito)?.let { if (!contains(it)) add(it) }
        limpio(ciudad)?.let { if (!contains(it)) add(it) }
    }

    if (partes.isEmpty()) return principal?.take(MAX_LONGITUD_DIRECCION)

    return partes.joinToString(", ")
        .take(MAX_LONGITUD_DIRECCION)
        .trim()
        .ifBlank { null }
}
