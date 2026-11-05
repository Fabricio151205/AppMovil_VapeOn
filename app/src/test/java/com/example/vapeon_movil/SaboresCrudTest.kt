package com.example.vapeon_movil

import com.example.vapeon_movil.entities.SaborItem
import com.example.vapeon_movil.services.CAMPOS_CATALOGO_SABOR
import com.example.vapeon_movil.services.dto.FirestoreValue
import com.example.vapeon_movil.services.dto.SaborCampos
import com.example.vapeon_movil.services.dto.SaborContainer
import com.example.vapeon_movil.services.dto.SaborDocumento
import com.example.vapeon_movil.utils.obtenerSaboresPorMarca
import com.example.vapeon_movil.utils.claveMarca
import com.example.vapeon_movil.utils.mismaMarca
import com.example.vapeon_movil.utils.marcasParaFormulario
import com.example.vapeon_movil.utils.LISTA_MARCAS_DISPONIBLES
import com.example.vapeon_movil.viewmodels.SaboresViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun nuevaEscrituraDeSaborNoIncluyeStockLegado() {
        val item = SaborItem(id = "99", nombre = "Cool Mint", marca = "Oxbar", stock = 5)
        val container = SaborContainer.fromEntity(item)

        assertEquals("Cool Mint", container.fields.nombre?.stringValue)
        assertEquals("Oxbar", container.fields.marca?.stringValue)
        assertNull(container.fields.stock)
    }

    @Test
    fun patchDeSaborIncluyeNombreMarcaEImagen() {
        assertEquals(listOf("nombre", "marca", "imagenUri"), CAMPOS_CATALOGO_SABOR)
    }

    @Test
    fun saborDocumentoLeeImagenDataUri() {
        val doc = SaborDocumento(
            name = "projects/x/databases/(default)/documents/sabores/s1",
            fields = SaborCampos(
                nombre = FirestoreValue(stringValue = "Mango"),
                marca = FirestoreValue(stringValue = "Oxbar"),
                imagenUri = FirestoreValue(stringValue = "data:image/jpeg;base64,AAAA")
            )
        )
        assertEquals("data:image/jpeg;base64,AAAA", doc.toEntity().imagenUri)
    }

    @Test
    fun saborDocumentoUsaCampoImagenLegadoComoRespaldo() {
        val doc = SaborDocumento(
            name = "projects/x/databases/(default)/documents/sabores/s2",
            fields = SaborCampos(
                nombre = FirestoreValue(stringValue = "Menta"),
                marca = FirestoreValue(stringValue = "Czar"),
                imagen = FirestoreValue(stringValue = "data:image/jpeg;base64,BBBB")
            )
        )
        assertEquals("data:image/jpeg;base64,BBBB", doc.toEntity().imagenUri)
    }

    @Test
    fun escrituraIncluyeImagenCuandoExiste() {
        val item = SaborItem(
            id = "s3",
            nombre = "Uva",
            marca = "Nexa",
            imagenUri = "data:image/jpeg;base64,CCCC"
        )
        val container = SaborContainer.fromEntity(item)
        assertEquals("data:image/jpeg;base64,CCCC", container.fields.imagenUri?.stringValue)
    }

    @Test
    fun escrituraOmiteImagenCuandoEsNula() {
        val item = SaborItem(id = "s4", nombre = "Sandía", marca = "Argus")
        val container = SaborContainer.fromEntity(item)
        assertNull(container.fields.imagenUri)
    }

    @Test
    fun formularioUsaMarcasDeFirestoreYListaBaseComoRespaldo() {
        assertEquals(listOf("Oxbar"), marcasParaFormulario(listOf("Oxbar", " oxbar ")))
        assertEquals(LISTA_MARCAS_DISPONIBLES, marcasParaFormulario(emptyList()))
    }

    @Test
    fun administracionDeSaboresNoRecibeStockDeVenta() {
        val operaciones = SaboresViewModel::class.java.methods.filter {
            it.name == "agregarSabor" || it.name == "editarSabor"
        }
        assertTrue(operaciones.isNotEmpty())
        assertTrue(operaciones.all { metodo ->
            metodo.parameterTypes.none { it == Int::class.javaPrimitiveType || it == Int::class.javaObjectType }
        })
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

    @Test
    fun identidadDeMarcaUsaEspaciosMayusculasYAliasExplicito() {
        assertEquals("oxbar", claveMarca(" Oxbar "))
        assertTrue(mismaMarca("Oxbar", "OXBAR"))
        assertTrue(mismaMarca("Czar", "czar"))
        assertTrue(mismaMarca("LifePod", " LIFEPOOD "))
    }

    @Test
    fun identidadDeMarcaNoUsaCoincidenciasAproximadas() {
        listOf("ABCD" to "ABC", "Ox" to "Oxbar", "Oxbar Plus" to "Oxbar",
            "LifePod Plus" to "LifePod", "Life Pod" to "LifePod",
            "LifePooood" to "LifePod").forEach { (primera, segunda) ->
            assertTrue("$primera no debe equivaler a $segunda", !mismaMarca(primera, segunda))
        }
    }

    @Test
    fun saboresNoMezclanMarcasQueSoloCompartenSubcadena() {
        val sabores = listOf(
            SaborItem(nombre = "Correcto", marca = " abc "),
            SaborItem(nombre = "Incorrecto", marca = "ABCD")
        )
        assertEquals(listOf("Correcto"), obtenerSaboresPorMarca("ABC", sabores))
    }

    @Test
    fun saboresReconocenAliasHistoricoLifePood() {
        val sabores = listOf(SaborItem(nombre = "Fresa", marca = "LifePood"))
        assertEquals(listOf("Fresa"), obtenerSaboresPorMarca("lifepod", sabores))
    }
}
