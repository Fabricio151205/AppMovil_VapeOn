package com.example.vapeon_movil.entities

import java.text.Normalizer
import java.util.Locale

/**
 * Dirección de entrega guardada por un cliente en la colección `direcciones`.
 *
 * [direccion] es el texto que se envía al mapa y al pedido; [alias] es solo una
 * etiqueta corta para identificarla en la lista (Casa, Trabajo, etc.).
 * [latitud] y [longitud] son el punto exacto que el cliente confirmó sobre el
 * mapa. Pueden ser nulos en direcciones antiguas guardadas antes de esta
 * función; en ese caso el mapa vuelve a geocodificar el texto.
 */
data class Direccion(
    val id: String = "",
    val usuarioId: String = "",
    val alias: String = "",
    val direccion: String = "",
    val referencia: String = "",
    val fechaCreacion: String = "",
    val latitud: Double? = null,
    val longitud: Double? = null
) {
    val tieneUbicacion: Boolean
        get() = latitud != null && longitud != null
}

/** Clave de comparación tolerante a mayúsculas, tildes y espacios repetidos. */
internal fun claveDireccion(valor: String): String =
    Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")

internal fun errorAliasDireccion(
    alias: String,
    existentes: List<Direccion>,
    excluirId: String? = null
): String? {
    val limpio = alias.trim()
    if (limpio.isBlank()) return "El alias es obligatorio."
    if (limpio.length > 30) return "El alias no puede superar los 30 caracteres."
    if (existentes.any { it.id != excluirId && claveDireccion(it.alias) == claveDireccion(limpio) })
        return "Ya tienes una dirección con ese alias."
    return null
}

internal fun errorTextoDireccion(direccion: String): String? {
    val limpio = direccion.trim()
    if (limpio.isBlank()) return "La dirección es obligatoria."
    if (limpio.length < 5) return "Ingresa una dirección más completa."
    if (limpio.length > 160) return "La dirección no puede superar los 160 caracteres."
    return null
}

/** Comprueba que el punto elegido en el mapa sea una coordenada terrestre válida. */
internal fun errorUbicacionDireccion(latitud: Double?, longitud: Double?): String? {
    if (latitud == null || longitud == null)
        return "Confirma la ubicación exacta en el mapa."
    if (!latitud.isFinite() || !longitud.isFinite() ||
        latitud !in -90.0..90.0 || longitud !in -180.0..180.0
    ) return "La ubicación seleccionada no es válida."
    return null
}

/** Valida un alta o edición completa antes de tocar la red. */
internal fun errorNuevaDireccion(
    alias: String,
    direccion: String,
    referencia: String,
    existentes: List<Direccion>,
    excluirId: String? = null,
    latitud: Double? = null,
    longitud: Double? = null
): String? {
    errorAliasDireccion(alias, existentes, excluirId)?.let { return it }
    errorTextoDireccion(direccion)?.let { return it }
    if (referencia.trim().length > 120) return "La referencia no puede superar los 120 caracteres."
    if (existentes.any { it.id != excluirId && claveDireccion(it.direccion) == claveDireccion(direccion) })
        return "Ya tienes guardada esa dirección."
    errorUbicacionDireccion(latitud, longitud)?.let { return it }
    return null
}
