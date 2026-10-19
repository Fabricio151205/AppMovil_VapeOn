package com.example.vapeon_movil.utils

import java.math.BigDecimal
import java.math.RoundingMode

/** Formato de presentación; no modifica los importes ni las fórmulas de la compra. */
fun importe(valor: BigDecimal): String = valor.setScale(2, RoundingMode.HALF_UP).toPlainString()
fun importe(valor: Double): String = if (valor.isFinite()) importe(BigDecimal.valueOf(valor)) else "No disponible"
fun moneda(valor: BigDecimal): String = "S/ ${importe(valor)}"
fun moneda(valor: Double): String = if (valor.isFinite()) "S/ ${importe(valor)}" else "No disponible"
