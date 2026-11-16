package com.example.vapeon_movil

import com.example.vapeon_movil.entities.Favorito
import com.example.vapeon_movil.entities.PopularidadProducto
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.services.FavoritoService
import com.example.vapeon_movil.services.dto.FavoritoConsulta
import com.example.vapeon_movil.services.dto.FavoritoConsultaRespuesta
import com.example.vapeon_movil.services.dto.FavoritoContainer
import com.example.vapeon_movil.services.dto.FavoritoDocumento
import com.example.vapeon_movil.services.dto.aFavoritoDelUsuario
import com.example.vapeon_movil.services.dto.aFirestore
import com.example.vapeon_movil.services.dto.agregarPorProducto
import com.example.vapeon_movil.services.dto.consultaFavoritosDelUsuario
import com.example.vapeon_movil.services.dto.consultaTodosLosFavoritos
import com.example.vapeon_movil.viewmodels.FavoritosAdminViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    /** Servicio inerte: estas pruebas no ejecutan peticiones de red. */
    private val servicioFalso = object : FavoritoService {
        override suspend fun crearFavorito(
            body: FavoritoContainer,
            documentId: String
        ): FavoritoDocumento = error("No usado en pruebas")

        override suspend fun consultarFavoritos(
            body: FavoritoConsulta
        ): List<FavoritoConsultaRespuesta> = error("No usado en pruebas")

        override suspend fun mostrarFavorito(id: String): FavoritoDocumento =
            error("No usado en pruebas")

        override suspend fun eliminarFavorito(id: String): Any = error("No usado en pruebas")
    }

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
        assertEquals("usuarioId", consulta.where!!.fieldFilter.field.fieldPath)
        assertEquals("EQUAL", consulta.where!!.fieldFilter.op)
        assertEquals("A", consulta.where!!.fieldFilter.value.stringValue)
    }

    @Test
    fun consultaAdministrativaNoFiltraPorUsuario() {
        val consulta = consultaTodosLosFavoritos().structuredQuery
        assertEquals("favoritos", consulta.from.single().collectionId)
        assertNull(consulta.where)
    }

    @Test
    fun agregacionCuentaFavoritosPorProductoYOrdenaDescendente() {
        val ranking = listOf(
            documento(favorito(id = "f1", usuarioId = "A", productoId = "p2")),
            documento(favorito(id = "f2", usuarioId = "B", productoId = "p1")),
            documento(favorito(id = "f3", usuarioId = "C", productoId = "p2"))
        ).agregarPorProducto()

        assertEquals(
            listOf(PopularidadProducto("p2", 2), PopularidadProducto("p1", 1)),
            ranking.popularidad
        )
        assertEquals(0, ranking.invalidos)
    }

    @Test
    fun agregacionDescartaDocumentosInvalidosSinExponerClientes() {
        val ranking = listOf(
            FavoritoDocumento("favoritos/roto", emptyMap()),
            documento(favorito(id = "f1", usuarioId = "A", productoId = "p1"))
        ).agregarPorProducto()

        assertEquals(listOf(PopularidadProducto("p1", 1)), ranking.popularidad)
        assertEquals(1, ranking.invalidos)
    }

    @Test(expected = IllegalArgumentException::class)
    fun popularidadRequiereFavoritosPositivos() {
        PopularidadProducto("p1", 0)
    }

    @Test
    fun rankingAdministrativoSoloAceptaAdministradores() {
        val viewModel = FavoritosAdminViewModel(
            service = servicioFalso,
            dispatcher = kotlinx.coroutines.Dispatchers.Unconfined
        )

        viewModel.actualizarUsuario(Usuario(id = "c1", rol = "CLIENTE"))
        assertNull(viewModel.adminId)

        viewModel.actualizarUsuario(Usuario(id = "a1", rol = "ADMIN"))
        assertEquals("a1", viewModel.adminId)
        assertTrue(viewModel.popularidad.isEmpty())
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
