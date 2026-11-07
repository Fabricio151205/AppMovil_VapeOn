package com.example.vapeon_movil

import com.example.vapeon_movil.entities.Direccion
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.entities.claveDireccion
import com.example.vapeon_movil.entities.errorNuevaDireccion
import com.example.vapeon_movil.services.CAMPOS_DIRECCION
import com.example.vapeon_movil.services.dto.DireccionDocumento
import com.example.vapeon_movil.services.dto.aDireccionDelUsuario
import com.example.vapeon_movil.services.dto.aFirestore
import com.example.vapeon_movil.services.dto.consultaDireccionesDelUsuario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DireccionesCrudTest {
    private val fecha = "2026-11-06T10:00:00Z"
    private val lat = -12.0464
    private val lng = -77.0428

    private fun direccion(
        id: String = "d1",
        usuarioId: String = "A",
        alias: String = "Casa",
        texto: String = "Av. Los Alamos 123, Lima",
        referencia: String = "Portón negro",
        latitud: Double? = lat,
        longitud: Double? = lng
    ) = Direccion(id, usuarioId, alias, texto, referencia, fecha, latitud, longitud)

    private fun documento(direccion: Direccion) =
        DireccionDocumento("direcciones/${direccion.id}", direccion.aFirestore().fields)

    @Test
    fun documentoSeConvierteAEntidad() {
        assertEquals(direccion(), documento(direccion()).aDireccionDelUsuario("A"))
    }

    @Test
    fun escrituraIncluyeTodosLosCamposYLaMascaraDeEdicion() {
        val campos = direccion().aFirestore().fields
        assertEquals("d1", campos["id"]?.stringValue)
        assertEquals("A", campos["usuarioId"]?.stringValue)
        assertEquals("Casa", campos["alias"]?.stringValue)
        assertEquals("Av. Los Alamos 123, Lima", campos["direccion"]?.stringValue)
        assertEquals("Portón negro", campos["referencia"]?.stringValue)
        assertEquals(fecha, campos["fechaCreacion"]?.timestampValue)
        assertEquals(lat, campos["latitud"]?.doubleValue)
        assertEquals(lng, campos["longitud"]?.doubleValue)
        assertEquals(
            listOf("alias", "direccion", "referencia", "latitud", "longitud"),
            CAMPOS_DIRECCION
        )
    }

    @Test
    fun direccionAntiguaSinCoordenadasSigueLeyendose() {
        val antigua = direccion(latitud = null, longitud = null)
        val leida = documento(antigua).aDireccionDelUsuario("A")
        assertEquals(antigua, leida)
        assertNull(leida.latitud)
        assertNull(leida.longitud)
        assertEquals(false, leida.tieneUbicacion)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unaSolaCoordenadaEsRechazada() {
        val incompleta = direccion(longitud = null)
        documento(incompleta).aDireccionDelUsuario("A")
    }

    @Test(expected = IllegalArgumentException::class)
    fun propietarioAjenoEsRechazado() {
        documento(direccion(usuarioId = "B")).aDireccionDelUsuario("A")
    }

    @Test(expected = IllegalArgumentException::class)
    fun idQueNoCoincideConElDocumentoEsRechazado() {
        DireccionDocumento("direcciones/otro", direccion().aFirestore().fields)
            .aDireccionDelUsuario("A")
    }

    @Test
    fun aliasYDireccionSonObligatorios() {
        assertNotNull(errorNuevaDireccion("", "Av. Siempre Viva 742", "", emptyList(), latitud = lat, longitud = lng))
        assertNotNull(errorNuevaDireccion("Casa", "   ", "", emptyList(), latitud = lat, longitud = lng))
        assertNotNull(errorNuevaDireccion("Casa", "Av 1", "", emptyList(), latitud = lat, longitud = lng))
        assertNull(errorNuevaDireccion("Casa", "Av. Siempre Viva 742", "", emptyList(), latitud = lat, longitud = lng))
    }

    @Test
    fun ubicacionExactaEsObligatoria() {
        assertEquals(
            "Confirma la ubicación exacta en el mapa.",
            errorNuevaDireccion("Casa", "Av. Siempre Viva 742", "", emptyList())
        )
        assertEquals(
            "Confirma la ubicación exacta en el mapa.",
            errorNuevaDireccion(
                "Casa", "Av. Siempre Viva 742", "", emptyList(),
                latitud = lat, longitud = null
            )
        )
        assertNull(
            errorNuevaDireccion(
                "Casa", "Av. Siempre Viva 742", "", emptyList(),
                latitud = lat, longitud = lng
            )
        )
    }

    @Test
    fun coordenadasFueraDeRangoSonRechazadas() {
        assertNotNull(
            errorNuevaDireccion(
                "Casa", "Av. Siempre Viva 742", "", emptyList(),
                latitud = 120.0, longitud = lng
            )
        )
        assertNotNull(
            errorNuevaDireccion(
                "Casa", "Av. Siempre Viva 742", "", emptyList(),
                latitud = lat, longitud = -200.0
            )
        )
        assertNotNull(
            errorNuevaDireccion(
                "Casa", "Av. Siempre Viva 742", "", emptyList(),
                latitud = Double.NaN, longitud = lng
            )
        )
    }

    @Test
    fun duplicadosIgnoranMayusculasTildesYEspacios() {
        val existentes = listOf(direccion(alias = "Casa", texto = "Av. José Larco 123"))
        assertEquals(
            "Ya tienes una dirección con ese alias.",
            errorNuevaDireccion(" CASA ", "Av. Nueva 456", "", existentes, latitud = lat, longitud = lng)
        )
        assertEquals(
            "Ya tienes guardada esa dirección.",
            errorNuevaDireccion("Trabajo", " AV. JOSE LARCO 123 ", "", existentes, latitud = lat, longitud = lng)
        )
    }

    @Test
    fun edicionExcluyeElDocumentoActual() {
        val existentes = listOf(direccion(id = "d1"))
        assertNull(
            errorNuevaDireccion(
                "Casa", "Av. Los Alamos 123, Lima", "Portón negro",
                existentes, excluirId = "d1", latitud = lat, longitud = lng
            )
        )
    }

    @Test
    fun referenciaDemasiadoLargaEsRechazada() {
        assertNotNull(
            errorNuevaDireccion(
                "Casa", "Av. Siempre Viva 742", "x".repeat(121), emptyList(),
                latitud = lat, longitud = lng
            )
        )
    }

    @Test
    fun consultaFiltraPorUsuarioYColeccion() {
        val consulta = consultaDireccionesDelUsuario("A").structuredQuery
        assertEquals("direcciones", consulta.from.single().collectionId)
        assertEquals("usuarioId", consulta.where.fieldFilter.field.fieldPath)
        assertEquals("EQUAL", consulta.where.fieldFilter.op)
        assertEquals("A", consulta.where.fieldFilter.value.stringValue)
    }

    @Test
    fun claveNormalizaMayusculasTildesYEspacios() {
        assertEquals("av. jose larco 123", claveDireccion("  AV.  José   Larco 123 "))
    }

    @Test
    fun rutasDeDireccionesPertenecenAlCliente() {
        val cliente = Usuario(id = "A", rol = "CLIENTE")
        val clave = claveSesionNavegacion(cliente)
        listOf("direcciones", "agregarDireccion", "editarDireccion").forEach { ruta ->
            assertEquals(ruta, pantallaRestauradaSegura(ruta, cliente, clave))
        }
        assertEquals(
            "admin",
            pantallaRestauradaSegura("direcciones", Usuario(id = "admin", rol = "ADMIN"), "admin:ADMIN")
        )
    }
}
