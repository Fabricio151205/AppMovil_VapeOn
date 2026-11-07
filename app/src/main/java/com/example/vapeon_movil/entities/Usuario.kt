package com.example.vapeon_movil.entities

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

data class Usuario(
    val id: String = "",
    val nombre: String = "",
    val apellido: String = "",
    val telefono: String = "",
    val fechaNacimiento: String = "",
    val correo: String = "",
    val password: String = "",
    val rol: String = "CLIENTE",
    val dni: String? = null,
    val createTime: String? = null,
    val updateTime: String? = null
)

private val FORMATO_FECHA_NACIMIENTO: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT)
        .withResolverStyle(ResolverStyle.STRICT)

/** Interpreta una fecha de nacimiento en formato estricto dd/MM/yyyy. */
fun fechaNacimientoLocal(fecha: String): LocalDate? = runCatching {
    val texto = fecha.trim()
    require(Regex("[0-9]{2}/[0-9]{2}/[0-9]{4}").matches(texto))
    val valor = LocalDate.parse(texto, FORMATO_FECHA_NACIMIENTO)
    require(valor.year > 0)
    valor
}.getOrNull()

/** Edad cumplida en años a partir de la fecha de nacimiento real. */
fun edadDesde(fechaNacimiento: String, hoy: LocalDate = LocalDate.now()): Int? =
    fechaNacimientoLocal(fechaNacimiento)?.let { Period.between(it, hoy).years }

/** Condición que la interfaz muestra junto al rol del usuario. */
fun esMayorDeEdad(fechaNacimiento: String, hoy: LocalDate = LocalDate.now()): Boolean =
    (edadDesde(fechaNacimiento, hoy) ?: -1) >= 18

/** Texto legible para el rol del usuario. */
fun rolLegible(rol: String): String = when (rol.trim().uppercase(Locale.ROOT)) {
    "ADMIN" -> "Administrador"
    "CLIENTE" -> "Cliente"
    else -> rol.trim().ifBlank { "Usuario" }
}
