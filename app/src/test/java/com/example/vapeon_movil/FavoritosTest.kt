package com.example.vapeon_movil

import com.example.vapeon_movil.entities.Favorito
import com.example.vapeon_movil.services.dto.FavoritoDocumento
import com.example.vapeon_movil.services.dto.aFavoritoDelUsuario
import com.example.vapeon_movil.services.dto.aFirestore
import com.example.vapeon_movil.services.dto.consultaFavoritosDelUsuario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritosTest {
    private val fecha = "2026-11-14T10:00:00Z"

    private fun favorito(
        id: String = "f1",
        usuarioId: String = "A",
        productoId: String = "p1"
    ) = Favorito(id, usuarioId, productoId, fecha)

    private fun documento(favorito: Favorito) =
        FavoritoDocumento("favoritos/${favorito.id}", favorito.aFirestore().fields)

    @Test
    fun documentoSeConvierteAEntidad() {
        assertEquals(favorito(), documento(favorito()).aFavoritoDelUsuario("A"))
    }

    @Test
    fun escrituraIncluyeTodosLosCampos() {
        val campos = favorito().aFirestore().fields
        assertEquals("f1", campos["id"]?.stringValue)
        assertEquals("A", campos["usuarioId"]?.stringValue)
        assertEquals("p1", campos["productoId"]?.stringValue)
        assertEquals(fecha, campos["fechaCreacion"]?.timestampValue)
    }

    @Test
    fun consultaFiltraPorUsuarioEnLaColeccionFavoritos() {
        val consulta = consultaFavoritosDelUsuario("A").structuredQuery
        assertEquals("favoritos", consulta.from.single().collectionId)
        assertEquals("usuarioId", consulta.where.fieldFilter.field.fieldPath)
        assertEquals("EQUAL", consulta.where.fieldFilter.op)
        assertEquals("A", consulta.where.fieldFilter.value.stringValue)
    }

    @Test(expected = IllegalArgumentException::class)
    fun consultaSinUsuarioEsRechazada() {
        consultaFavoritosDelUsuario("   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun propietarioAjenoEsRechazado() {
        documento(favorito(usuarioId = "B")).aFavoritoDelUsuario("A")
    }

    @Test(expected = IllegalArgumentException::class)
    fun idQueNoCoincideConElDocumentoEsRechazado() {
        FavoritoDocumento("favoritos/otro", favorito().aFirestore().fields)
            .aFavoritoDelUsuario("A")
    }

    @Test(expected = IllegalArgumentException::class)
    fun productoVacioEsRechazado() {
        documento(favorito(productoId = " ")).aFavoritoDelUsuario("A")
    }

    @Test(expected = IllegalArgumentException::class)
    fun fechaInvalidaEsRechazada() {
        val campos = favorito().aFirestore().fields
            .toMutableMap()
            .apply { put("fechaCreacion", com.example.vapeon_movil.services.dto.FirestoreValor(timestampValue = "no-es-fecha")) }
        FavoritoDocumento("favoritos/f1", campos).aFavoritoDelUsuario("A")
    }

    @Test
    fun entidadValidaRequiereRelacionCompleta() {
        assertTrue(favorito().esValido)
        assertFalse(favorito(id = "").esValido)
        assertFalse(favorito(usuarioId = "").esValido)
        assertFalse(favorito(productoId = "").esValido)
    }
}
