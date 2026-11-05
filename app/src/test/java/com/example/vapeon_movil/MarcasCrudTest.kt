package com.example.vapeon_movil

import com.example.vapeon_movil.entities.Marca
import com.example.vapeon_movil.services.CAMPOS_MARCA
import com.example.vapeon_movil.services.dto.FirestoreValue
import com.example.vapeon_movil.services.dto.MarcaCampos
import com.example.vapeon_movil.services.dto.MarcaContainer
import com.example.vapeon_movil.services.dto.MarcaDocumento
import com.example.vapeon_movil.utils.claveMarca
import com.example.vapeon_movil.utils.idMarca
import com.example.vapeon_movil.viewmodels.errorNombreMarca
import org.junit.Assert.*
import org.junit.Test

class MarcasCrudTest {
    @Test fun documentoSeConvierteAEntidad() {
        val marca = MarcaDocumento("projects/x/databases/(default)/documents/marcas/m1", MarcaCampos(FirestoreValue(stringValue = "Oxbar"))).toEntity()
        assertEquals(Marca("m1", "Oxbar"), marca)
    }

    @Test fun escrituraSoloIncluyeNombreLimpio() {
        val fields = MarcaContainer.fromEntity(Marca(nombre = "  Oxbar  ")).fields
        assertEquals("Oxbar", fields.nombre?.stringValue)
        assertEquals(listOf("nombre"), CAMPOS_MARCA)
    }

    @Test fun nombreVacioEsRechazado() {
        assertNotNull(errorNombreMarca("   ", emptyList()))
    }

    @Test fun duplicadoIgnoraMayusculasYEspacios() {
        val existentes = listOf(Marca("1", "Oxbar"))
        assertNotNull(errorNombreMarca(" OXBAR ", existentes))
    }

    @Test fun aliasHistoricoEsMismaIdentidad() {
        val existentes = listOf(Marca("1", "LifePod"))
        assertNotNull(errorNombreMarca("lifepood", existentes))
        assertEquals("lifepod", claveMarca(" LIFEPOOD "))
    }

    @Test fun nombresDiferentesNoUsanCoincidenciaAproximada() {
        val existentes = listOf(Marca("1", "Oxbar"))
        assertNull(errorNombreMarca("Oxbar Plus", existentes))
        assertNull(errorNombreMarca("Ox", existentes))
    }

    @Test fun edicionExcluyeElDocumentoActual() {
        val existentes = listOf(Marca("1", "Oxbar"))
        assertNull(errorNombreMarca(" oxbar ", existentes, excluirId = "1"))
    }

    @Test fun idMarcaEsEstableYSeguroParaFirestore() {
        assertEquals("lifepod", idMarca("LifePod"))
        assertEquals("lifepod", idMarca(" LIFEPOOD "))
        assertEquals("oxbar_plus", idMarca("Oxbar Plus"))
        assertEquals("marca", idMarca("   "))
    }
}
