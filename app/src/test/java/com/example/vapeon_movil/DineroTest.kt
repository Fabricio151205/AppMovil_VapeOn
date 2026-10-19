package com.example.vapeon_movil

import com.example.vapeon_movil.utils.*
import java.math.BigDecimal
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class DineroTest {
    @Test fun conservaCentimosYCeros() {
        assertEquals("S/ 70.90", moneda(70.90))
        assertEquals("S/ 70.00", moneda(70.0))
        assertEquals("S/ 0.00", moneda(BigDecimal.ZERO))
        assertEquals("S/ 24.50", moneda(BigDecimal("24.50")))
    }
    @Test fun formatoNoDependeDelIdiomaNiCambiaElValor() {
        val anterior = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val original = BigDecimal("10.235")
            assertEquals("10.24", importe(original))
            assertEquals(BigDecimal("10.235"), original)
        } finally { Locale.setDefault(anterior) }
    }
    @Test fun noPresentaNumerosNoFinitosComoPrecios() {
        assertEquals("No disponible", moneda(Double.NaN))
        assertEquals("No disponible", moneda(Double.POSITIVE_INFINITY))
    }
}
