package com.example.vapeon_movil

import com.example.vapeon_movil.entities.Producto
import com.example.vapeon_movil.services.ProductoService
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.viewmodels.ProductoViewModel
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test

class FiltrosProductoTest {
    private class Servicio : ProductoService {
        val documentos = linkedMapOf<String, ProductoDocumento>()
        var alta: ProductoContainer? = null
        var edicion: ProductoContainer? = null
        var mascara = emptyList<String>()
        var version: String? = null
        override suspend fun listarProductos(pageToken: String?) = ProductoListResponse(documentos.values.toList())
        override suspend fun mostrarProducto(id: String) = documentos.getValue(id)
        override suspend fun crearProducto(body: ProductoContainer, documentId: String?): Any {
            alta = body
            return Unit
        }
        override suspend fun actualizarProducto(id: String, body: ProductoContainer, updateTime: String,
            campos: List<String>): ProductoDocumento {
            edicion = body
            mascara = campos
            version = updateTime
            return documentos.getValue(id)
        }
        override suspend fun eliminarProducto(id: String): Any = Unit
    }

    private fun documento(id: String, nombre: String, precio: Double, stock: Int, categoria: String?) =
        ProductoDocumento("productos/$id", ProductoCampos(
            nombre = FirestoreValue(stringValue = nombre), marca = FirestoreValue(stringValue = "Marca"),
            precio = FirestoreValue(doubleValue = precio), stock = FirestoreValue(integerValue = stock.toString()),
            descripcion = FirestoreValue(stringValue = "Descripción"),
            categoria = categoria?.let { FirestoreValue(stringValue = it) }
        ), "2026-10-30T10:00:00Z")

    private fun inventario(): ProductoViewModel {
        val servicio = Servicio()
        listOf(
            documento("1", "Kit Alpha", 10.0, 2, "Dispositivos"),
            documento("2", "Kit Beta", 20.0, 0, "Dispositivos"),
            documento("3", "Recarga", 30.0, 5, "Recargas"),
            documento("4", "Accesorio", 40.0, 1, "Accesorios"),
            documento("5", "Antiguo", 15.0, 3, null)
        ).forEach { servicio.documentos[it.name.substringAfterLast('/')] = it }
        return ProductoViewModel(servicio, Dispatchers.Unconfined).also { it.cargarProductos() }
    }

    private fun ids(productos: List<Producto>) = productos.map { it.id }

    @Test fun busquedaPorNombreSinFiltros() {
        assertEquals(listOf("1", "2"), ids(inventario().filtrarProductos(busqueda = "kit")))
    }
    @Test fun busquedaPorMarcaYDescripcionSeConserva() {
        val vm = inventario()
        assertEquals(5, vm.filtrarProductos(busqueda = "marca").size)
        assertEquals(5, vm.filtrarProductos(busqueda = "descripción").size)
    }
    @Test fun filtraCategoriaIndependienteDeMarca() {
        assertEquals(listOf("1", "2"), ids(inventario().filtrarProductos(categoria = " dispositivos ")))
    }
    @Test fun precioMinimoInclusivo() {
        assertEquals(listOf("2", "3", "4"), ids(inventario().filtrarProductos(precioMinimo = "20")))
    }
    @Test fun precioMaximoInclusivo() {
        assertEquals(listOf("1", "2", "5"), ids(inventario().filtrarProductos(precioMaximo = "20")))
    }
    @Test fun rangoMinimoYMaximo() {
        assertEquals(listOf("2", "3"), ids(inventario().filtrarProductos(precioMinimo = "20", precioMaximo = "30")))
    }
    @Test fun soloConStock() {
        assertEquals(listOf("1", "3", "4", "5"), ids(inventario().filtrarProductos(soloConStock = true)))
    }
    @Test fun categoriaYPrecio() {
        assertEquals(listOf("2"), ids(inventario().filtrarProductos(categoria = "Dispositivos", precioMinimo = "15")))
    }
    @Test fun categoriaYStock() {
        assertEquals(listOf("1"), ids(inventario().filtrarProductos(categoria = "Dispositivos", soloConStock = true)))
    }
    @Test fun nombreCategoriaPrecioYStockSimultaneamente() {
        assertEquals(listOf("1"), ids(inventario().filtrarProductos("kit", "Dispositivos", "10", "20", true)))
    }
    @Test fun ningunFiltroDevuelveTodoSinModificarInventario() {
        val vm = inventario()
        val original = vm.productos
        vm.filtrarProductos(categoria = "Recargas")
        assertSame(original, vm.productos)
        assertEquals(listOf("1", "2", "3", "4", "5"), ids(vm.filtrarProductos()))
    }
    @Test fun stockCeroEsExcluido() {
        assertFalse(inventario().filtrarProductos(soloConStock = true).any { it.stock == 0 })
    }
    @Test fun stockPositivoEsIncluidoSinUmbralDeBajoStock() {
        assertTrue(inventario().filtrarProductos(soloConStock = true).any { it.stock == 1 })
    }
    @Test fun limitesInvalidosSonRechazados() {
        val vm = inventario()
        listOf("NaN", "Infinity", "-Infinity", "0", "-1", "abc", "10.001").forEach {
            assertNotNull(vm.errorFiltros(it, ""))
            assertNotNull(vm.errorFiltros("", it))
            assertTrue(vm.filtrarProductos(precioMinimo = it).isEmpty())
        }
    }
    @Test fun rangoInvertidoEsRechazado() {
        assertNotNull(inventario().errorFiltros("30", "20"))
    }
    @Test fun comaDecimalYLimitesIgualesSonValidos() {
        val vm = inventario()
        assertNull(vm.errorFiltros("10,00", "10.00"))
        assertEquals(listOf("1"), ids(vm.filtrarProductos(precioMinimo = "10,00", precioMaximo = "10")))
    }
    @Test fun camposVaciosNoRestringen() {
        assertEquals(5, inventario().filtrarProductos(" ", " ", " ", " ").size)
    }
    @Test fun categoriasProcedenSolamenteDelInventario() {
        assertEquals(listOf("Accesorios", "Dispositivos", "Recargas"), inventario().categoriasDisponibles)
    }
    @Test fun productoAntiguoSinCategoriaCargaComoVacia() {
        val vm = inventario()
        assertEquals("", vm.productos.single { it.id == "5" }.categoria)
        val antiguo = Gson().fromJson("""{"fields":{"nombre":{"stringValue":"Antiguo"}}}""", ProductoContainer::class.java)
        assertNull(antiguo.fields.categoria)
    }
    @Test fun construccionAnteriorDeProductoConservaCompatibilidad() {
        val producto = Producto("id", "Nombre", "Marca", 10.0, 2, "Descripción", "Menta", mapOf("Menta" to 2))
        assertEquals("", producto.categoria)
        assertEquals(2, producto.stockPorSabor["Menta"])
    }
    @Test fun categoriaVaciaEsRechazadaAlCrear() {
        val servicio = Servicio()
        val vm = ProductoViewModel(servicio, Dispatchers.Unconfined)
        vm.crearProducto("Nombre", "Marca", 10.0, 2, "", categoria = " ")
        assertNotNull(vm.errorCreacion)
        assertNull(servicio.alta)
    }
    @Test fun precioInvalidoEsRechazadoAlCrear() {
        val servicio = Servicio()
        val vm = ProductoViewModel(servicio, Dispatchers.Unconfined)
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0, -1.0, 10.001).forEach {
            vm.crearProducto("Nombre", "Marca", it, 2, "", categoria = "Accesorios")
            assertNotNull(vm.errorCreacion)
        }
        assertNull(servicio.alta)
    }
    @Test fun categoriaSeEscribeComoStringSinCambiarTiposNumericos() {
        val servicio = Servicio()
        ProductoViewModel(servicio, Dispatchers.Unconfined)
            .crearProducto("Nombre", "Marca", 10.25, 2, "", categoria = " Accesorios ")
        val fields = Gson().toJsonTree(servicio.alta).asJsonObject.getAsJsonObject("fields")
        assertEquals("Accesorios", fields.getAsJsonObject("categoria").get("stringValue").asString)
        assertEquals(10.25, fields.getAsJsonObject("precio").get("doubleValue").asDouble, 0.0)
        assertEquals("2", fields.getAsJsonObject("stock").get("integerValue").asString)
    }

    private fun editar(categoriaOriginal: String?, categoriaNueva: String?): Pair<Servicio, ProductoViewModel> {
        val servicio = Servicio().apply { documentos["1"] = documento("1", "Nombre", 10.0, 2, categoriaOriginal) }
        val vm = ProductoViewModel(servicio, Dispatchers.Unconfined)
        vm.iniciarEdicion("1")
        vm.guardarProductoEdicion("Nombre", "Marca", 10.0, 2, "Nueva descripción", categoria = categoriaNueva)
        return servicio to vm
    }
    @Test fun antiguoPuedeConservarCategoriaVaciaAlEditar() {
        val (servicio, vm) = editar(null, "")
        assertNull(vm.errorEdicion)
        assertEquals(listOf("descripcion"), servicio.mascara)
    }
    @Test fun editarSinParametroCategoriaConservaLaActual() {
        val (servicio, vm) = editar("Accesorios", null)
        assertNull(vm.errorEdicion)
        assertFalse("categoria" in servicio.mascara)
    }
    @Test fun cambioCategoriaUsaMascaraYConservaPrecondicion() {
        val (servicio, vm) = editar(null, "Recargas")
        assertNull(vm.errorEdicion)
        assertEquals(listOf("descripcion", "categoria"), servicio.mascara)
        assertEquals("Recargas", servicio.edicion!!.fields.categoria!!.stringValue)
        assertEquals("2026-10-30T10:00:00Z", servicio.version)
        assertNull(servicio.edicion!!.fields.stock)
        assertNull(servicio.edicion!!.fields.precio)
        assertNull(servicio.edicion!!.fields.stockPorSabor)
    }
    @Test fun categoriaDefinidaNoPuedeBorrarse() {
        val (servicio, vm) = editar("Accesorios", " ")
        assertNotNull(vm.errorEdicion)
        assertNull(servicio.edicion)
    }
}
