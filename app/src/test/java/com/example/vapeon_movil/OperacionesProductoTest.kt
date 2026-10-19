package com.example.vapeon_movil

import com.example.vapeon_movil.services.ProductoService
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.viewmodels.ProductoViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class OperacionesProductoTest {
    private class Servicio : ProductoService {
        val documentos = mutableMapOf<String, ProductoDocumento>()
        var altas = 0
        var bajas = 0
        var espera: CompletableDeferred<Unit>? = null
        var perderRespuesta = false
        var falloLista = false
        override suspend fun crearProducto(body: ProductoContainer, documentId: String?): Any {
            altas++
            espera?.await()
            val id = requireNotNull(documentId)
            if (documentos.containsKey(id)) throw HttpException(Response.error<Any>(409, ResponseBody.create(null, "")))
            documentos[id] = ProductoDocumento("productos/$id", body.fields, "2026-10-14T10:00:00Z")
            if (perderRespuesta) { perderRespuesta = false; throw java.io.IOException("Respuesta perdida") }
            return Unit
        }
        override suspend fun mostrarProducto(id: String) = requireNotNull(documentos[id])
        override suspend fun listarProductos(pageToken: String?): ProductoListResponse {
            if (falloLista) throw java.io.IOException("Sin conexión")
            return ProductoListResponse(documentos.values.toList())
        }
        override suspend fun eliminarProducto(id: String): Any { bajas++; espera?.await(); documentos.remove(id); return Unit }
        override suspend fun actualizarProducto(id: String, body: ProductoContainer, updateTime: String,
            campos: List<String>): ProductoDocumento = error("No se utiliza")
    }

    @Test fun doblePulsacionNoGeneraDosAltas() {
        val servicio = Servicio().apply { espera = CompletableDeferred() }
        val vm = ProductoViewModel(servicio, Dispatchers.Unconfined)
        var exitos = 0
        repeat(2) { vm.crearProducto("Producto", "Marca", 70.9, 5, "Descripción") { exitos++ } }
        assertEquals(1, servicio.altas)
        assertTrue(vm.cargandoCreacion)
        servicio.espera!!.complete(Unit)
        assertEquals(1, servicio.documentos.size)
        assertEquals(1, exitos)
        assertFalse(vm.cargandoCreacion)
    }

    @Test fun respuestaPerdidaReutilizaIdSinDuplicarProducto() {
        val servicio = Servicio().apply { perderRespuesta = true }
        val vm = ProductoViewModel(servicio, Dispatchers.Unconfined)
        var exitos = 0
        vm.crearProducto("Producto", "Marca", 70.9, 5, "Descripción") { exitos++ }
        assertEquals(0, exitos)
        assertNotNull(vm.errorCreacion)
        vm.crearProducto("Producto", "Marca", 70.9, 5, "Descripción") { exitos++ }
        assertEquals(1, servicio.documentos.size)
        assertEquals(1, exitos)
    }

    @Test fun doblePulsacionNoDuplicaEliminacion() {
        val servicio = Servicio().apply { espera = CompletableDeferred() }
        val vm = ProductoViewModel(servicio, Dispatchers.Unconfined)
        repeat(2) { vm.eliminarProducto("p1") }
        assertEquals(1, servicio.bajas)
        servicio.espera!!.complete(Unit)
        assertTrue(vm.eliminandoIds.isEmpty())
    }

    @Test fun precioNoFinitoEsRechazadoAntesDePost() {
        val servicio = Servicio()
        val vm = ProductoViewModel(servicio, Dispatchers.Unconfined)
        listOf(Double.NaN, Double.POSITIVE_INFINITY, 70.905).forEach {
            vm.crearProducto("Producto", "Marca", it, 5, "") { fail("Precio inválido") }
        }
        assertEquals(0, servicio.altas)
    }

    @Test fun diferenciaErrorDeCargaDeListaVaciaYPermiteReintentar() {
        val servicio = Servicio().apply { falloLista = true }
        val vm = ProductoViewModel(servicio, Dispatchers.Unconfined)
        vm.cargarProductos()
        assertNotNull(vm.error)
        assertFalse(vm.inventarioCargado)
        assertFalse(vm.cargando)
        servicio.falloLista = false
        vm.cargarProductos()
        assertNull(vm.error)
        assertTrue(vm.inventarioCargado)
        assertTrue(vm.productos.isEmpty())
    }
}
