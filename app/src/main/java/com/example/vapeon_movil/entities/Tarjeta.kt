package com.example.vapeon_movil.entities

import java.time.YearMonth

/** Registro exclusivamente simulado: no contiene número completo ni códigos de seguridad. */
data class Tarjeta(
    val id: String,
    val usuarioId: String,
    val titular: String,
    val marca: String,
    val ultimos4: String,
    val vencimientoMes: Int,
    val vencimientoAnio: Int,
    val fechaCreacion: String,
    val modalidad: String = "SIMULADO"
) {
    fun estaVencida(hoy: YearMonth = YearMonth.now()): Boolean =
        YearMonth.of(vencimientoAnio, vencimientoMes).isBefore(hoy)
}

internal fun validarRegistroTarjeta(
    titular: String, marca: String, ultimos4: String, mes: String, anio: String,
    hoy: YearMonth = YearMonth.now()
): String? {
    if (titular.isBlank()) return "Ingresa el titular de la tarjeta."
    if (marca.isBlank()) return "Ingresa la marca de la tarjeta."
    if (!Regex("[0-9]{4}").matches(ultimos4)) return "Ingresa exactamente los últimos 4 dígitos numéricos."
    if (!Regex("[0-9]{1,2}").matches(mes) || mes.toIntOrNull() !in 1..12)
        return "Ingresa un mes entre 1 y 12."
    if (!Regex("[0-9]{4}").matches(anio) || anio.toIntOrNull() !in 1..9999)
        return "Ingresa un año válido de cuatro dígitos."
    if (YearMonth.of(anio.toInt(), mes.toInt()).isBefore(hoy)) return "La tarjeta está vencida."
    return null
}
