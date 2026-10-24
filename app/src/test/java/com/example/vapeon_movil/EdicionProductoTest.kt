package com.example.vapeon_movil

import com.example.vapeon_movil.services.ProductoService
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.viewmodels.ProductoViewModel
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class EdicionProductoTest {
    // Simula solo las condiciones del servidor; no escribe en Firebase.
    private class ServicioProductosPrueba : ProductoService {
        val campos = mutableMapOf(
            "id" to FirestoreValue(stringValue = "p1"),
            "nombre" to FirestoreValue(stringValue = "Producto"),
            "marca" to FirestoreValue(stringValue = "Marca"),
            "precio" to FirestoreValue(doubleValue = 70.0),
            "stock" to FirestoreValue(integerValue = "10"),
            "descripcion" to FirestoreValue(stringValue = "Descripción"),
            "campoAdicional" to FirestoreValue(stringValue = "Conservar")
        )
        var version: String? = "2026-10-14T10:00:00Z"
        var existe = true
        var lecturas = 0
        var escrituras = 0
        var ultimaVersion: String? = null
        var ultimaMascara = emptyList<String>()
        var ultimoCuerpo: ProductoContainer? = null
        var errorPatch: HttpException? = null
        var lecturaPendiente: CompletableDeferred<ProductoDocumento>? = null

        fun documento() = ProductoDocumento("projects/db/databases/(default)/documents/productos/p1",
            ProductoCampos(campos["nombre"], campos["marca"], campos["precio"], campos["stock"],
                campos["descripcion"]), version)

        override suspend fun mostrarProducto(id: String): ProductoDocumento {
            lecturas++
            lecturaPendiente?.let { return it.await() }
            if (!existe) throw fallo(404)
            return documento()
        }

        override suspend fun actualizarProducto(id: String, body: ProductoContainer,
            updateTime: String, campos: List<String>): ProductoDocumento {
            escrituras++
            ultimaVersion = updateTime
            ultimaMascara = campos
            ultimoCuerpo = body
            errorPatch?.let { throw it }
            if (!existe) throw fallo(404)
            if (updateTime != version) throw fallo(412)
            val valores = mapOf("nombre" to body.fields.nombre, "marca" to body.fields.marca,
                "precio" to body.fields.precio, "stock" to body.fields.stock,
                "descripcion" to body.fields.descripcion)
            campos.forEach { campo -> this.campos[campo] = requireNotNull(valores[campo]) }
            version = "2026-10-14T10:00:02Z"
            return documento()
        }

        override suspend fun listarProductos(pageToken: String?) = ProductoListResponse(listOf(documento()))
        override suspend fun crearProducto(body: ProductoContainer, documentId: String?): Any = error("No se utiliza")
        override suspend fun eliminarProducto(id: String): Any = error("No se utiliza")

        companion object {
            fun fallo(codigo: Int, status: String = "FAILED_PRECONDITION") = HttpException(
                Response.error<ProductoDocumento>(codigo, ResponseBody.create(
                    MediaType.parse("application/json"), "{\"error\":{\"status\":\"$status\"}}")))
        }
    }

    private fun vm(servicio: ServicioProductosPrueba) =
        ProductoViewModel(servicio, Dispatchers.Unconfined).also { it.cargarProductoEdicion("p1") }

    private fun guardar(vm: ProductoViewModel, stock: Int = 10, onExito: () -> Unit = {}) =
        vm.guardarProductoEdicion("Producto", "Marca", 75.0, stock, "Descripción", onExito = onExito)

    @Test fun actualizacionNormalUtilizaLaVersionLeida() {
        val servicio = ServicioProductosPrueba()
        val vm = vm(servicio)
        val version = vm.documentoEdicion?.updateTime
        var exitos = 0
        guardar(vm) { exitos++ }
        assertEquals(1, exitos)
        assertEquals(version, servicio.ultimaVersion)
        assertEquals(75.0, servicio.campos["precio"]?.doubleValue)
        assertEquals("10", servicio.campos["stock"]?.integerValue)
        assertNull(vm.errorEdicion)
        assertFalse(vm.guardandoEdicion)
    }

    @Test fun recrearFormularioNoRebasaSilenciosamenteLaVersionLeida() {
        val servicio = ServicioProductosPrueba()
        val vm = vm(servicio)
        servicio.version = "2026-10-14T10:00:01Z"
        servicio.campos["stock"] = FirestoreValue(integerValue = "7")
        vm.iniciarEdicion("p1") // La composición se recrea, pero conserva la versión del formulario.
        guardar(vm) { fail("No debe sobrescribir la nueva versión") }
        assertEquals("7", servicio.campos["stock"]?.integerValue)
        assertTrue(vm.requiereRecargarEdicion)
        assertEquals(1, servicio.lecturas)
    }

    @Test fun compraPosteriorRechazaEdicionAntiguaSinRestaurarStock() {
        val servicio = ServicioProductosPrueba()
        val vm = vm(servicio)
        servicio.campos["stock"] = FirestoreValue(integerValue = "7")
        servicio.version = "2026-10-14T10:00:01Z"
        var exito = false
        guardar(vm) { exito = true }
        assertFalse(exito)
        assertEquals("7", servicio.campos["stock"]?.integerValue)
        assertEquals(70.0, servicio.campos["precio"]?.doubleValue)
        assertTrue(vm.requiereRecargarEdicion)
        assertTrue(vm.errorEdicion.orEmpty().contains("modificado"))
        assertEquals(1, servicio.lecturas) // No recarga ni intenta combinar automáticamente.
        assertEquals(1, servicio.escrituras)
        assertFalse(vm.guardandoEdicion)
    }

    @Test fun cambioManualDeStockTambienExigeLaVersionOriginal() {
        val servicio = ServicioProductosPrueba()
        val vm = vm(servicio)
        servicio.campos["stock"] = FirestoreValue(integerValue = "7")
        servicio.version = "2026-10-14T10:00:01Z"
        guardar(vm, stock = 20) { fail("Versión obsoleta") }
        assertEquals("7", servicio.campos["stock"]?.integerValue)
        assertTrue(vm.requiereRecargarEdicion)
    }

    @Test fun documentoEliminadoNoSeRecreaNiSeConfirmaComoGuardado() {
        val servicio = ServicioProductosPrueba()
        val vm = vm(servicio)
        servicio.existe = false
        guardar(vm) { fail("No existe") }
        assertFalse(servicio.existe)
        assertTrue(vm.errorEdicion.orEmpty().contains("ya no existe"))
        assertTrue(vm.requiereRecargarEdicion)
    }

    @Test fun noPermiteEditarSiFaltaUpdateTime() {
        val servicio = ServicioProductosPrueba().apply { version = null }
        val vm = vm(servicio)
        assertNull(vm.documentoEdicion)
        assertNotNull(vm.errorEdicion)
        guardar(vm) { fail("No hay versión") }
        assertEquals(0, servicio.escrituras)
    }

    @Test fun edicionSoloDePrecioNoEnviaStockNiBorraCamposAdicionales() {
        val servicio = ServicioProductosPrueba()
        val vm = vm(servicio)
        guardar(vm)
        assertEquals(listOf("precio"), servicio.ultimaMascara)
        val json = Gson().toJsonTree(servicio.ultimoCuerpo).asJsonObject["fields"].asJsonObject
        assertEquals(setOf("precio"), json.entrySet().map { it.key }.toSet())
        assertEquals("p1", servicio.campos["id"]?.stringValue)
        assertEquals("Conservar", servicio.campos["campoAdicional"]?.stringValue)
        assertEquals("Producto", servicio.campos["nombre"]?.stringValue)
        assertEquals("Marca", servicio.campos["marca"]?.stringValue)
        assertEquals("Descripción", servicio.campos["descripcion"]?.stringValue)
    }

    @Test fun cuerpoCompletoMantieneStockEnteroYPrecioDecimal() {
        val servicio = ServicioProductosPrueba()
        val vm = vm(servicio)
        vm.guardarProductoEdicion("Nuevo", "Nueva marca", 75.5, 20, "Nueva descripción") {}
        assertEquals(setOf("nombre", "marca", "precio", "stock", "descripcion"), servicio.ultimaMascara.toSet())
        val fields = Gson().toJsonTree(servicio.ultimoCuerpo).asJsonObject["fields"].asJsonObject
        assertEquals("20", fields["stock"].asJsonObject["integerValue"].asString)
        assertFalse(fields["stock"].asJsonObject.has("doubleValue"))
        assertEquals(75.5, fields["precio"].asJsonObject["doubleValue"].asDouble, 0.0)
        assertEquals("Nuevo", servicio.campos["nombre"]?.stringValue)
        assertEquals("Nueva marca", servicio.campos["marca"]?.stringValue)
        assertEquals("Nueva descripción", servicio.campos["descripcion"]?.stringValue)
        assertEquals("p1", servicio.campos["id"]?.stringValue)
        assertEquals("Conservar", servicio.campos["campoAdicional"]?.stringValue)
    }

    @Test fun conflictoNoSeReintentaHastaRecargarExplicitamente() {
        val servicio = ServicioProductosPrueba()
        val vm = vm(servicio)
        servicio.version = "2026-10-14T10:00:01Z"
        servicio.campos["stock"] = FirestoreValue(integerValue = "7")
        guardar(vm) { fail("Versión obsoleta") }
        guardar(vm) { fail("Falta recargar") }
        assertEquals(1, servicio.escrituras)
        vm.cargarProductoEdicion("p1")
        assertEquals("7", vm.documentoEdicion?.fields?.stock?.integerValue)
        assertFalse(vm.requiereRecargarEdicion)
        assertEquals(1, servicio.escrituras) // La recarga no guarda automáticamente.
        var exito = false
        guardar(vm, stock = 7) { exito = true }
        assertTrue(exito)
        assertEquals("2026-10-14T10:00:01Z", servicio.ultimaVersion)
        assertEquals("7", servicio.campos["stock"]?.integerValue)
    }

    @Test fun sinCambiosNoSeEnviaUnPatchVacio() {
        val servicio = ServicioProductosPrueba()
        val vm = vm(servicio)
        vm.guardarProductoEdicion("Producto", "Marca", 70.0, 10, "Descripción") { fail("Sin cambios") }
        assertEquals(0, servicio.escrituras)
        assertNotNull(vm.errorEdicion)
    }

    @Test fun reconoceLasRespuestasDePrecondicionSinConfirmarExito() {
        listOf(400, 409, 412).forEach { codigo ->
            val servicio = ServicioProductosPrueba().apply {
                errorPatch = ServicioProductosPrueba.fallo(codigo)
            }
            val vm = vm(servicio)
            guardar(vm) { fail("Conflicto $codigo") }
            assertTrue(vm.requiereRecargarEdicion)
            assertTrue(vm.errorEdicion.orEmpty().contains("modificado"))
            assertFalse(vm.guardandoEdicion)
        }
    }

    @Test fun errorDistintoDePrecondicionNoSePresentaComoConflictoNiExito() {
        val servicio = ServicioProductosPrueba().apply {
            errorPatch = ServicioProductosPrueba.fallo(403, "PERMISSION_DENIED")
        }
        val vm = vm(servicio)
        guardar(vm) { fail("Sin permiso") }
        assertNotNull(vm.errorEdicion)
        assertFalse(vm.requiereRecargarEdicion)
        assertEquals(70.0, servicio.campos["precio"]?.doubleValue)
    }

    @Test fun cerrarEdicionCancelaLaCargaYLimpiaSuEstado() {
        val servicio = ServicioProductosPrueba()
        val pendiente = CompletableDeferred<ProductoDocumento>()
        servicio.lecturaPendiente = pendiente
        val vm = vm(servicio)
        assertTrue(vm.cargandoEdicion)
        vm.cerrarEdicion()
        pendiente.complete(servicio.documento())
        assertNull(vm.documentoEdicion)
        assertFalse(vm.cargandoEdicion)
        assertFalse(vm.guardandoEdicion)
        assertNull(vm.errorEdicion)
    }

    @Test fun retrofitEnviaPatchConPrecondicionYMascaraEnLaUrl() = runBlocking {
        val servicioPrueba = ServicioProductosPrueba()
        var url: okhttp3.HttpUrl? = null
        var metodo: String? = null
        val cliente = OkHttpClient.Builder().addInterceptor { cadena ->
            val peticion = cadena.request()
            url = peticion.url()
            metodo = peticion.method()
            okhttp3.Response.Builder().request(peticion).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(ResponseBody.create(MediaType.parse("application/json"),
                    Gson().toJson(servicioPrueba.documento()))).build()
        }.build()
        val service = Retrofit.Builder().baseUrl("https://firestore.test/documents/")
            .client(cliente).addConverterFactory(GsonConverterFactory.create()).build()
            .create(ProductoService::class.java)
        service.actualizarProducto("p1", ProductoContainer(ProductoCampos(precio = FirestoreValue(doubleValue = 75.0))),
            "2026-10-14T10:00:00Z", listOf("precio", "stock"))
        assertEquals("PATCH", metodo)
        assertEquals("/documents/productos/p1", url?.encodedPath())
        assertEquals("2026-10-14T10:00:00Z", url?.queryParameter("currentDocument.updateTime"))
        assertEquals(listOf("precio", "stock"), url?.queryParameterValues("updateMask.fieldPaths"))
        assertNull(url?.queryParameter("currentDocument.exists")) // Precondition admite solo una condición.
    }
}
