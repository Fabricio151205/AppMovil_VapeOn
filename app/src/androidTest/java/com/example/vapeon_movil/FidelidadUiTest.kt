package com.example.vapeon_movil

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.example.vapeon_movil.entities.*
import com.example.vapeon_movil.services.FidelidadService
import com.example.vapeon_movil.services.UsuarioService
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.ui.FidelidadAdminScreen
import com.example.vapeon_movil.ui.MiFidelidadScreen
import com.example.vapeon_movil.ui.MenuScreen
import com.example.vapeon_movil.ui.theme.VapeON_MovilTheme
import com.example.vapeon_movil.viewmodels.FidelidadViewModel
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class FidelidadUiTest {
    @get:Rule val compose = createComposeRule()

    private val instante = "2026-11-08T00:00:00Z"

    @Test fun clienteVeUnicamenteSuProgreso() {
        val cliente = Usuario(id = "cliente1", nombre = "Ana", rol = "CLIENTE")
        val servicio = FidelidadFake(
            fidelidades = mapOf("cliente1" to FidelidadCliente(
                "cliente1", 4, 1, fechaActualizacion = instante
            ).aDocumento(RUTA_DOCUMENTOS_FIRESTORE))
        )
        val vm = FidelidadViewModel(servicio, UsuariosFake(emptyList()), Dispatchers.Unconfined)
        compose.setContent {
            VapeON_MovilTheme { MiFidelidadScreen(cliente, vm, {}) }
        }
        compose.onNodeWithText("MI FIDELIDAD").assertIsDisplayed()
        compose.onNodeWithText("4 / 6 puntos").assertIsDisplayed()
        compose.onNodeWithText("Te faltan 2 compras para obtener tu recompensa.").assertIsDisplayed()
        assertEquals(listOf("cliente1"), servicio.consultasFidelidad)
    }

    @Test fun clienteSinRegistroVeEstadoInicialCeroDeSeis() {
        val cliente = Usuario(id = "cliente1", nombre = "Ana", rol = "CLIENTE")
        val vm = FidelidadViewModel(FidelidadFake(), UsuariosFake(emptyList()), Dispatchers.Unconfined)
        compose.setContent { VapeON_MovilTheme { MiFidelidadScreen(cliente, vm, {}) } }

        compose.onNodeWithText("0 / 6 puntos").assertIsDisplayed()
        compose.onNodeWithText("Aún no tienes puntos de fidelidad.").assertIsDisplayed()
        compose.onNodeWithText(
            "Completa pedidos entregados para comenzar a acumular puntos."
        ).assertIsDisplayed()
    }

    @Test fun clienteSeisDeSeisVeSnapshotDeRecompensa() {
        val cliente = Usuario(id = "cliente1", nombre = "Ana", rol = "CLIENTE")
        val estado = FidelidadCliente("cliente1", 6, 1, "r1", instante)
        val recompensa = RecompensaFidelidad(
            "r1", "cliente1", 1, EstadoRecompensaFidelidad.PENDIENTE,
            productoId = "p1", nombreProducto = "Producto A", marcaProducto = "Marca A",
            categoriaProducto = "Kit", precioReferencia = BigDecimal("50.00"),
            fechaGeneracion = instante
        )
        val servicio = FidelidadFake(
            fidelidades = mapOf("cliente1" to estado.aDocumento(RUTA_DOCUMENTOS_FIRESTORE)),
            recompensas = mapOf("r1" to recompensa.aDocumento(RUTA_DOCUMENTOS_FIRESTORE))
        )
        val vm = FidelidadViewModel(servicio, UsuariosFake(emptyList()), Dispatchers.Unconfined)
        compose.setContent { VapeON_MovilTheme { MiFidelidadScreen(cliente, vm, {}) } }

        compose.onNodeWithText("6 / 6 puntos").assertIsDisplayed()
        compose.onNodeWithText("Recompensa de fidelidad").assertIsDisplayed()
        compose.onNodeWithText("Producto: Producto A").assertIsDisplayed()
        compose.onNodeWithText("Marca: Marca A").assertIsDisplayed()
        compose.onNodeWithText("Categoría: Kit").assertIsDisplayed()
    }

    @Test fun clienteSeisDeSeisSinProductoVeConfiguracionPendiente() {
        val cliente = Usuario(id = "cliente1", nombre = "Ana", rol = "CLIENTE")
        val estado = FidelidadCliente("cliente1", 6, 1, "r1", instante)
        val recompensa = RecompensaFidelidad(
            "r1", "cliente1", 1, EstadoRecompensaFidelidad.PENDIENTE,
            fechaGeneracion = instante
        )
        val servicio = FidelidadFake(
            fidelidades = mapOf("cliente1" to estado.aDocumento(RUTA_DOCUMENTOS_FIRESTORE)),
            recompensas = mapOf("r1" to recompensa.aDocumento(RUTA_DOCUMENTOS_FIRESTORE))
        )
        val vm = FidelidadViewModel(servicio, UsuariosFake(emptyList()), Dispatchers.Unconfined)
        compose.setContent { VapeON_MovilTheme { MiFidelidadScreen(cliente, vm, {}) } }

        compose.onNodeWithText("6 / 6 puntos").assertIsDisplayed()
        compose.onNodeWithText(
            "El administrador todavía debe configurar el producto de la recompensa."
        ).assertIsDisplayed()
        compose.onNodeWithText("Producto: Producto A").assertDoesNotExist()
    }

    @Test fun clienteNuevoCicloNoVeRecompensaAnterior() {
        val cliente = Usuario(id = "cliente1", nombre = "Ana", rol = "CLIENTE")
        val estado = FidelidadCliente("cliente1", 0, 2, null, instante)
        val recompensaAnterior = RecompensaFidelidad(
            "r-anterior", "cliente1", 1, EstadoRecompensaFidelidad.ENTREGADA,
            productoId = "p1", nombreProducto = "Producto anterior", marcaProducto = "Marca A",
            categoriaProducto = "Kit", precioReferencia = BigDecimal("50.00"),
            fechaGeneracion = instante, fechaEntrega = instante, entregadaPor = "admin"
        )
        val servicio = FidelidadFake(
            fidelidades = mapOf("cliente1" to estado.aDocumento(RUTA_DOCUMENTOS_FIRESTORE)),
            recompensas = mapOf("r-anterior" to recompensaAnterior.aDocumento(RUTA_DOCUMENTOS_FIRESTORE))
        )
        val vm = FidelidadViewModel(servicio, UsuariosFake(emptyList()), Dispatchers.Unconfined)
        compose.setContent { VapeON_MovilTheme { MiFidelidadScreen(cliente, vm, {}) } }

        compose.onNodeWithText("0 / 6 puntos").assertIsDisplayed()
        compose.onNodeWithText("Nuevo ciclo de fidelidad.").assertIsDisplayed()
        compose.onNodeWithText("Producto: Producto anterior").assertDoesNotExist()
        assertEquals(emptyList<String>(), servicio.consultasRecompensa)
    }

    @Test fun adminVeConfiguracionYRankingSinFirebaseReal() {
        val admin = Usuario(id = "admin1", nombre = "Admin", rol = "ADMIN")
        val usuario = UsuarioDocumento(
            "usuarios/cliente1",
            UsuarioCampos(
                nombre = FirestoreStringValue("Ana"),
                apellido = FirestoreStringValue("Pérez"),
                correo = FirestoreStringValue("ana@example.com"),
                rol = FirestoreStringValue("CLIENTE")
            )
        )
        val estado = FidelidadCliente("cliente1", 6, 1, "cliente1_ciclo_1", instante)
        val recompensa = RecompensaFidelidad(
            "cliente1_ciclo_1", "cliente1", 1, EstadoRecompensaFidelidad.PENDIENTE,
            fechaGeneracion = instante
        )
        val servicio = FidelidadFake(
            fidelidades = mapOf("cliente1" to estado.aDocumento(RUTA_DOCUMENTOS_FIRESTORE)),
            recompensas = mapOf(recompensa.id to recompensa.aDocumento(RUTA_DOCUMENTOS_FIRESTORE))
        )
        val vm = FidelidadViewModel(servicio, UsuariosFake(listOf(usuario)), Dispatchers.Unconfined)
        compose.setContent {
            VapeON_MovilTheme {
                FidelidadAdminScreen(
                    admin = admin,
                    fidelidadViewModel = vm,
                    productos = listOf(Producto("p1", "Oxbar Mini", "Oxbar", 50.0, 2, "")),
                    cargarProductos = {},
                    volver = {}
                )
            }
        }
        compose.onNodeWithText("Cupones de fidelidad").assertIsDisplayed()
        compose.onNodeWithText("Ana Pérez").assertIsDisplayed()
        compose.onNodeWithText("6 / 6 puntos").assertIsDisplayed()
        compose.onNodeWithText("RECOMPENSA PENDIENTE").assertIsDisplayed()
        compose.onNodeWithText("Entregar recompensa").assertIsDisplayed()
    }

    @Test fun menuClientePropagaMiFidelidadSinCrearOtraRuta() {
        var navego = false
        compose.setContent {
            VapeON_MovilTheme {
                MenuScreen(irFidelidad = { navego = true }) { }
            }
        }
        compose.onNodeWithContentDescription("Menú Principal").performClick()
        compose.onNodeWithText("Mi fidelidad").performClick()
        compose.runOnIdle { org.junit.Assert.assertTrue(navego) }
    }

    private class FidelidadFake(
        private val fidelidades: Map<String, FidelidadDocumento> = emptyMap(),
        private val recompensas: Map<String, FidelidadDocumento> = emptyMap()
    ) : FidelidadService {
        val consultasFidelidad = mutableListOf<String>()
        val consultasRecompensa = mutableListOf<String>()
        private fun respuesta(documento: FidelidadDocumento?): Response<FidelidadDocumento> =
            documento?.let { Response.success(it) }
                ?: Response.error(404, ResponseBody.create(null, ""))
        override suspend fun configuracion() = respuesta(null)
        override suspend fun fidelidad(usuarioId: String): Response<FidelidadDocumento> {
            consultasFidelidad += usuarioId
            return respuesta(fidelidades[usuarioId])
        }
        override suspend fun evento(pedidoId: String) = respuesta(null)
        override suspend fun recompensa(id: String): Response<FidelidadDocumento> {
            consultasRecompensa += id
            return respuesta(recompensas[id])
        }
        override suspend fun listarFidelidades(pageToken: String?) = FidelidadLista(fidelidades.values.toList())
        override suspend fun commit(body: FidelidadCommit) = FidelidadCommitRespuesta(
            "2026-11-08T00:00:00Z",
            body.writes.map { FidelidadResultadoEscritura("version-nueva") }
        )
    }

    private class UsuariosFake(private val documentos: List<UsuarioDocumento>) : UsuarioService {
        override suspend fun listarUsuarios(pageToken: String?) = UsuarioListResponse(documentos)
        override suspend fun mostrarUsuario(id: String): UsuarioDocumento = error("No se usa")
        override suspend fun crearUsuario(body: UsuarioContainer, documentId: String?) = error("No se usa")
        override suspend fun actualizarUsuario(id: String, body: UsuarioContainer): UsuarioDocumento = error("No se usa")
        override suspend fun eliminarUsuario(id: String) = error("No se usa")
    }
}
