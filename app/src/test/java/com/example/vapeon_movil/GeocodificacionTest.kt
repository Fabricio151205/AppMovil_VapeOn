package com.example.vapeon_movil

import com.example.vapeon_movil.utils.MAX_LONGITUD_DIRECCION
import com.example.vapeon_movil.utils.componerDireccionLegible
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeocodificacionTest {
    @Test
    fun prefiereLaLineaCompletaDelGeocodificador() {
        assertEquals(
            "Av. José Larco 123, Miraflores, Lima",
            componerDireccionLegible(
                lineaPrincipal = "Av. José Larco 123, Miraflores, Lima",
                calle = "José Larco",
                numero = "123",
                distrito = "Miraflores",
                ciudad = "Lima"
            )
        )
    }

    @Test
    fun componeCalleNumeroDistritoYCiudad() {
        assertEquals(
            "Av. Arequipa 456, Lince, Lima",
            componerDireccionLegible(
                lineaPrincipal = null,
                calle = "Av. Arequipa",
                numero = "456",
                distrito = "Lince",
                ciudad = "Lima"
            )
        )
    }

    @Test
    fun omiteValoresVaciosYDuplicados() {
        assertEquals(
            "Jr. de la Unión, Lima",
            componerDireccionLegible(
                lineaPrincipal = "   ",
                calle = "Jr. de la Unión",
                numero = null,
                distrito = "Lima",
                ciudad = "Lima"
            )
        )
        assertEquals(
            "Av. Siempre Viva 742",
            componerDireccionLegible(
                lineaPrincipal = null,
                calle = " Av. Siempre Viva ",
                numero = " 742 ",
                distrito = "",
                ciudad = null
            )
        )
    }

    @Test
    fun recortaLaLineaPrincipalAlLimiteDelFormulario() {
        val larga = "x".repeat(MAX_LONGITUD_DIRECCION + 40)
        val resultado = componerDireccionLegible(larga, null, null, null, null)
        assertEquals(MAX_LONGITUD_DIRECCION, resultado?.length)
    }

    @Test
    fun siLaLineaEsMuyLargaUsaLosComponentes() {
        val larga = "x".repeat(MAX_LONGITUD_DIRECCION + 40)
        assertEquals(
            "Av. Perú 100, Lima",
            componerDireccionLegible(larga, "Av. Perú", "100", "Lima", "Lima")
        )
    }

    @Test
    fun sinDatosDevuelveNull() {
        assertNull(componerDireccionLegible(null, null, null, null, null))
        assertNull(componerDireccionLegible("  ", "", " ", null, ""))
    }
}
