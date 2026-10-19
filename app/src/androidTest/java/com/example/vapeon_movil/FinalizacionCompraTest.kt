package com.example.vapeon_movil

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.vapeon_movil.entities.*
import com.example.vapeon_movil.services.ProductoService
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.viewmodels.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.util.UUID

/** Usa preferencias reales con una clave exclusiva de prueba; no envía compras a Firebase. */
@RunWith(AndroidJUnit4::class)
class FinalizacionCompraTest {
    @Test fun cambiarUsuarioNoCorrigeCarritoConInventarioDeLaSesionAnterior() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val prefs = app.getSharedPreferences("vapeon_carrito_prefs", Context.MODE_PRIVATE)
        val sufijo = UUID.randomUUID().toString()
        val usuarioA = "prueba_a_$sufijo"
        val usuarioB = "prueba_b_$sufijo"
        val claveB = "carrito_items_$usuarioB"
        val item = ItemCarrito("p1", "Producto", "Marca", 70.9, 1, stockDisponible = 3)
        check(prefs.edit().putString(claveB, Gson().toJson(listOf(item))).commit())
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val vm = CarritoViewModel()
                vm.cargarCarrito(app, usuarioA)
                vm.actualizarInventario(emptyList())
                vm.cargarCarrito(app, usuarioB)
                assertEquals(listOf(item), vm.items)
                vm.actualizarInventario(listOf(Producto("p1", "Producto", "Marca", 70.9, 2, "")))
                assertEquals(2, vm.items.single().stockDisponible)
            }
        } finally { prefs.edit().remove(claveB).remove("carrito_items_$usuarioA").commit() }
    }

    @Test fun pedidoConfirmadoSobreviveAFalloLocalYRecreacionSinRepetirCommit() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val usuario = Usuario(id = "prueba_i12_${UUID.randomUUID()}", rol = "CLIENTE")
        val prefs = app.getSharedPreferences("vapeon_checkout", Context.MODE_PRIVATE)
        val clave = "intento_${usuario.id}"
        val importe = BigDecimal("10.00")
        val pedido = Pedido("pedido_prueba", usuario.id, "Prueba", "912345678", "2026-10-14T12:00:00Z",
            listOf(ProductoPedido("p1", "Producto", "Marca", 1, importe, importe)),
            importe, BigDecimal.ZERO, importe, importe, BigDecimal.ZERO, "Dirección de prueba")
        val ruta = "projects/prueba/databases/(default)/documents"
        val commit = PedidoCommit(listOf(
            PedidoEscritura(pedido.aDocumento(ruta), PedidoPrecondicion(exists = false)),
            PedidoEscritura(PedidoDocumento("$ruta/productos/p1", mapOf("stock" to FirestoreValor(integerValue = "2"))),
                PedidoPrecondicion(updateTime = pedido.fecha), PedidoMascara(listOf("stock")))
        ))
        // Representa un pedido ya confirmado, pendiente únicamente de limpieza local.
        check(prefs.edit().putString(clave, Gson().toJson(PedidoViewModel.Intento(pedido, commit, true))).commit())
        val servicio = object : ProductoService {
            override suspend fun listarProductos(pageToken: String?) = ProductoListResponse(emptyList())
            override suspend fun mostrarProducto(id: String): ProductoDocumento = error("No debe consultar un producto")
            override suspend fun crearProducto(body: ProductoContainer, documentId: String?): Any = error("No debe crear")
            override suspend fun actualizarProducto(id: String, body: ProductoContainer, updateTime: String,
                campos: List<String>): ProductoDocumento = error("No debe actualizar")
            override suspend fun eliminarProducto(id: String): Any = error("No debe eliminar")
        }
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val productos = ProductoViewModel(servicio, Dispatchers.Unconfined)
                val carrito = CarritoViewModel { false }
                val vm = PedidoViewModel(app)
                vm.actualizarUsuarioCheckout(usuario)
                vm.confirmar(usuario, carrito, productos, "", "")
                assertEquals(pedido.id, vm.pedidoConfirmado?.id)
                assertTrue(vm.requiereComprobacion)
                assertTrue(carrito.limpiezaPendiente)
                assertTrue(prefs.contains(clave))
                assertFalse(vm.cargando)
                // Nueva instancia: recupera la confirmación y limpia sin otra petición de compra.
                val nueva = PedidoViewModel(app)
                nueva.actualizarUsuarioCheckout(usuario)
                assertEquals(pedido.id, nueva.pedidoConfirmado?.id)
                nueva.confirmar(usuario, CarritoViewModel { true }, productos, "", "")
                assertFalse(nueva.requiereComprobacion)
                assertFalse(nueva.cargando)
                assertNull(nueva.error)
                assertFalse(prefs.contains(clave))
            }
        } finally { prefs.edit().remove(clave).commit() }
    }
}
