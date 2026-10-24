package com.example.vapeon_movil

import com.example.vapeon_movil.entities.SaborItem
import com.example.vapeon_movil.services.dto.FirestoreValue
import com.example.vapeon_movil.services.dto.SaborCampos
import com.example.vapeon_movil.services.dto.SaborContainer
import com.example.vapeon_movil.services.dto.SaborDocumento
import com.example.vapeon_movil.utils.obtenerSaboresPorMarca
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaboresCrudTest {

    @Test
    fun saborDocumentoMapeaAEntidad() {
        val doc = SaborDocumento(
            name = "projects/dbvapeon/databases/(default)/documents/sabores/sabor_123",
            fields = SaborCampos(
                nombre = FirestoreValue(stringValue = "Mango Passion"),
                marca = FirestoreValue(stringValue = "LifePod"),
                stock = FirestoreValue(integerValue = "15")
            )
        )

        val entidad = doc.toEntity()

        assertEquals("sabor_123", entidad.id)
        assertEquals("Mango Passion", entidad.nombre)
        assertEquals("LifePod", entidad.marca)
        assertEquals(15, entidad.stock)
    }

    @Test
    fun saborContainerGeneraEstructuraFirestoreCorrecta() {
        val item = SaborItem(id = "99", nombre = "Cool Mint", marca = "Oxbar", stock = 5)
        val container = SaborContainer.fromEntity(item)

        assertEquals("Cool Mint", container.fields.nombre?.stringValue)
        assertEquals("Oxbar", container.fields.marca?.stringValue)
        assertEquals("5", container.fields.stock?.integerValue)
    }

    @Test
    fun filtradoPorMarcaRetornaSaboresCorrectos() {
        val listaPrueba = listOf(
            SaborItem(nombre = "Banana Ice", marca = "LifePod", stock = 10),
            SaborItem(nombre = "Cool Mint", marca = "Oxbar", stock = 5),
            SaborItem(nombre = "Watermelon Ice", marca = "LifePod", stock = 2)
        )

        val saboresLifePod = obtenerSaboresPorMarca("LifePod", listaPrueba)
        assertEquals(2, saboresLifePod.size)
        assertTrue(saboresLifePod.contains("Banana Ice"))
        assertTrue(saboresLifePod.contains("Watermelon Ice"))
    }
}
