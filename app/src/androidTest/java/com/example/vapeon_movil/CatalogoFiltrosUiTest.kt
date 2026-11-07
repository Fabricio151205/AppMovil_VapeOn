package com.example.vapeon_movil

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.vapeon_movil.services.ProductoService
import com.example.vapeon_movil.services.dto.FirestoreValue
import com.example.vapeon_movil.services.dto.ProductoCampos
import com.example.vapeon_movil.services.dto.ProductoContainer
import com.example.vapeon_movil.services.dto.ProductoDocumento
import com.example.vapeon_movil.services.dto.ProductoListResponse
import com.example.vapeon_movil.ui.CatalogoScreen
import com.example.vapeon_movil.ui.theme.VapeON_MovilTheme
import com.example.vapeon_movil.viewmodels.ProductoViewModel
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Verifica la conexión Compose → ProductoViewModel sin red ni escrituras en Firebase. */
@RunWith(AndroidJUnit4::class)
class CatalogoFiltrosUiTest {
    @get:Rule
    val compose = createComposeRule()

    private class Servicio : ProductoService {
        private val documentos = listOf(
            documento("a", "Kit LifePod", "LifePood", "Kits", 70.0, 2),
            documento("b", "Batería Oxbar", "Oxbar", "Baterías", 50.0, 4),
            documento("c", "Kit Nexa", "Nexa", "Kits", 90.0, 3),
            documento("d", "Kit agotado", "LifePod", "Kits", 80.0, 0)
        )

        override suspend fun listarProductos(pageToken: String?) = ProductoListResponse(documentos)
        override suspend fun mostrarProducto(id: String) = documentos.single { it.name.endsWith("/$id") }
        override suspend fun crearProducto(body: ProductoContainer, documentId: String?): Any = error("No debe escribir")
        override suspend fun actualizarProducto(id: String, body: ProductoContainer, updateTime: String,
            campos: List<String>): ProductoDocumento = error("No debe escribir")
        override suspend fun eliminarProducto(id: String): Any = error("No debe escribir")
    }

    @Test
    fun conectaControlesRf13ConFiltradoRestablecimientoBusquedaYOrden() {
        val viewModel = ProductoViewModel(Servicio(), Dispatchers.Unconfined)
        compose.setContent {
            VapeON_MovilTheme {
                CatalogoScreen(productoViewModel = viewModel, irDetalleProducto = {}, esAdmin = false)
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText("Promociones").assertDoesNotExist()
        compose.onNodeWithContentDescription("Filtros de productos").performClick()
        compose.onNodeWithTag("categoria_Kits").performClick()
        compose.onNodeWithTag("precio_minimo").performTextInput("60")
        compose.onNodeWithTag("precio_maximo").performTextInput("100")
        compose.onNodeWithTag("solo_con_stock").performClick()
        compose.onNodeWithText("Mayor a Menor").performClick()
        compose.onNodeWithText("Aplicar Filtros").performClick()

        compose.onNodeWithText("Kit Nexa").assertExists()
        compose.onNodeWithText("Kit LifePod").assertExists()
        compose.onNodeWithText("Kit agotado").assertDoesNotExist()
        val nexa = compose.onNodeWithText("Kit Nexa").fetchSemanticsNode().boundsInRoot
        val lifePod = compose.onNodeWithText("Kit LifePod").fetchSemanticsNode().boundsInRoot
        assertTrue("El precio mayor debe mostrarse primero", nexa.top < lifePod.top)

        compose.onNodeWithContentDescription("Filtros de productos").performClick()
        compose.onNodeWithTag("precio_minimo").performTextClearance()
        compose.onNodeWithTag("precio_minimo").performTextInput("100")
        compose.onNodeWithTag("precio_maximo").performTextClearance()
        compose.onNodeWithTag("precio_maximo").performTextInput("10")
        compose.onNodeWithTag("error_filtros_dialogo").assertExists()
        compose.onNodeWithText("Restablecer").performClick()

        compose.onNodeWithTag("busqueda_productos").assertTextEquals("")
        compose.onNodeWithTag("busqueda_productos").performTextInput("Nexa")
        compose.onNodeWithText("Kit Nexa").assertExists()
        compose.onNodeWithText("Kit LifePod").assertDoesNotExist()

        compose.onNodeWithTag("busqueda_productos").performTextClearance()
        compose.onNodeWithContentDescription("Filtros de productos").performClick()
        compose.onNodeWithTag("precio_minimo").assertTextEquals("")
        compose.onNodeWithTag("precio_maximo").assertTextEquals("")
        compose.onNodeWithTag("solo_con_stock").assertIsOff()
        compose.onNodeWithTag("categoria_Todas").assertExists()
        compose.onNodeWithTag("categoria_Kits").assertExists()
        compose.onNodeWithTag("categoria_Baterías").assertExists()
    }

    companion object {
        private fun documento(id: String, nombre: String, marca: String, categoria: String,
            precio: Double, stock: Int) = ProductoDocumento(
            name = "productos/$id",
            fields = ProductoCampos(
                nombre = FirestoreValue(stringValue = nombre),
                marca = FirestoreValue(stringValue = marca),
                precio = FirestoreValue(doubleValue = precio),
                stock = FirestoreValue(integerValue = stock.toString()),
                descripcion = FirestoreValue(stringValue = "Descripción de $nombre"),
                categoria = FirestoreValue(stringValue = categoria)
            ),
            updateTime = "2026-11-06T12:00:00Z"
        )
    }
}
