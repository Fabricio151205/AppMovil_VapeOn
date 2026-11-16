package com.example.vapeon_movil

import com.example.vapeon_movil.entities.Proveedor
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.services.ProveedorService
import com.example.vapeon_movil.services.dto.ProveedorContainer
import com.example.vapeon_movil.services.dto.ProveedorDocumento
import com.example.vapeon_movil.services.dto.ProveedorListResponse
import com.example.vapeon_movil.services.dto.aFirestore
import com.example.vapeon_movil.services.dto.aProveedor
import com.example.vapeon_movil.services.listarTodosProveedores
import com.example.vapeon_movil.viewmodels.FormularioProveedor
import com.example.vapeon_movil.viewmodels.ProveedorViewModel
import com.example.vapeon_movil.viewmodels.rucPeruanoValido
import com.example.vapeon_movil.viewmodels.validarFormularioProveedor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test

class ProveedorTest {
    private val admin = Usuario(id = "a1", rol = "ADMIN")
    private val cliente = Usuario(id = "c1", rol = "CLIENTE")
    private val formularioValido = FormularioProveedor(
        razonSocial = "Vape Distribution SAC", ruc = "20131312955", contacto = "Juan Pérez",
        telefono = "987654321", correo = "contacto@empresa.com", direccion = "Av. Principal 123",
        ciudad = "Lima", tipoProducto = "Vapes y recargas"
    )

    @Test fun validaRucPeruano() {
        assertTrue(rucPeruanoValido("20131312955"))
        listOf("20131312954", "2013131295", "201313129555", "2013131295A", " 20131312955")
            .forEach { assertFalse(it, rucPeruanoValido(it)) }
    }

    @Test fun validaTodosLosCamposObligatorios() {
        assertFalse(validarFormularioProveedor(formularioValido).hayErrores())
        assertFalse(validarFormularioProveedor(formularioValido.copy(observaciones = "")).hayErrores())
        assertNotNull(validarFormularioProveedor(formularioValido.copy(razonSocial = " ")).razonSocial)
        assertNotNull(validarFormularioProveedor(formularioValido.copy(contacto = "")).contacto)
        assertNotNull(validarFormularioProveedor(formularioValido.copy(telefono = "123")).telefono)
        assertNotNull(validarFormularioProveedor(formularioValido.copy(correo = "correo-invalido")).correo)
        assertNotNull(validarFormularioProveedor(formularioValido.copy(direccion = "")).direccion)
        assertNotNull(validarFormularioProveedor(formularioValido.copy(ciudad = "")).ciudad)
        assertNotNull(validarFormularioProveedor(formularioValido.copy(tipoProducto = "")).tipoProducto)
    }

    @Test fun dtoConservaEntidadYExtraeIdSinPersistirlo() {
        val proveedor = proveedor("proveedor-1")
        val body = proveedor.aFirestore()
        assertFalse(body.fields.containsKey("id"))
        assertEquals(proveedor, ProveedorDocumento(
            name = "projects/db/databases/(default)/documents/proveedores/proveedor-1",
            fields = body.fields
        ).aProveedor())
    }

    @Test fun servicioRecorrePaginasYDeduplica() = runBlocking {
        val servicio = FakeProveedorService().apply {
            paginas[null] = ProveedorListResponse(listOf(documento("p1")), "segunda")
            paginas["segunda"] = ProveedorListResponse(listOf(documento("p1"), documento("p2")))
        }
        val resultado = servicio.listarTodosProveedores()
        assertEquals(listOf(null, "segunda"), servicio.tokens)
        assertEquals(2, resultado.documents?.size)
    }

    @Test fun adminCargaListaYListaVacia() {
        val servicio = FakeProveedorService(documentos = mutableListOf(documento("p1")))
        val vm = vm(servicio)
        vm.cargarProveedores(admin)
        assertEquals(1, vm.proveedores.size)
        servicio.documentos.clear()
        vm.cargarProveedores(admin)
        assertTrue(vm.proveedores.isEmpty())
    }

    @Test fun clienteNoPuedeCargarNiRegistrar() {
        val servicio = FakeProveedorService()
        val vm = vm(servicio)
        vm.cargarProveedores(cliente)
        vm.actualizarFormulario(formularioValido)
        vm.registrarProveedor(cliente)
        assertEquals(0, servicio.listados)
        assertEquals(0, servicio.creaciones)
        assertNotNull(vm.error)
    }

    @Test fun errorDeCargaEsVisible() {
        val vm = vm(FakeProveedorService(errorAlListar = true))
        vm.cargarProveedores(admin)
        assertNotNull(vm.error)
        assertFalse(vm.cargando)
    }

    @Test fun adminRegistraLimpiaFormularioYRecarga() {
        val servicio = FakeProveedorService()
        val vm = vm(servicio)
        vm.actualizarFormulario(formularioValido)
        vm.registrarProveedor(admin)
        assertEquals(1, servicio.creaciones)
        assertEquals("uuid-fijo", servicio.ids.single())
        assertTrue(vm.registroExitoso)
        assertEquals(FormularioProveedor(), vm.formulario)
        assertEquals(1, vm.proveedores.size)
    }

    @Test fun registroFallidoConservaFormularioYUuidParaReintento() {
        val servicio = FakeProveedorService(fallosCreacion = 1)
        val vm = vm(servicio)
        vm.actualizarFormulario(formularioValido)
        vm.registrarProveedor(admin)
        assertEquals(formularioValido, vm.formulario)
        assertNotNull(vm.error)
        vm.registrarProveedor(admin)
        assertEquals(listOf("uuid-fijo", "uuid-fijo"), servicio.ids)
        assertTrue(vm.registroExitoso)
    }

    @Test fun dobleEnvioSoloCreaUnaSolicitud() = runBlocking {
        val bloqueo = CompletableDeferred<Unit>()
        val servicio = FakeProveedorService(bloqueo = bloqueo)
        val vm = vm(servicio)
        vm.actualizarFormulario(formularioValido)
        vm.registrarProveedor(admin)
        vm.registrarProveedor(admin)
        assertEquals(1, servicio.creaciones)
        bloqueo.complete(Unit)
        yield()
        assertEquals(1, servicio.creaciones)
    }

    private fun vm(service: ProveedorService) = ProveedorViewModel(
        service = service, dispatcher = Dispatchers.Unconfined,
        ahora = { "2026-11-16T12:00:00Z" }, nuevoId = { "uuid-fijo" }
    )

    private fun proveedor(id: String) = Proveedor(
        id, "Vape Distribution SAC", "20131312955", "Juan Pérez", "987654321",
        "contacto@empresa.com", "Av. Principal 123", "Lima", "Vapes y recargas", "",
        "2026-11-16T12:00:00Z"
    )

    private fun documento(id: String) = ProveedorDocumento(
        "projects/db/databases/(default)/documents/proveedores/$id", proveedor(id).aFirestore().fields
    )

    private inner class FakeProveedorService(
        val documentos: MutableList<ProveedorDocumento> = mutableListOf(),
        private val errorAlListar: Boolean = false,
        private var fallosCreacion: Int = 0,
        private val bloqueo: CompletableDeferred<Unit>? = null
    ) : ProveedorService {
        val paginas = mutableMapOf<String?, ProveedorListResponse>()
        val tokens = mutableListOf<String?>()
        val ids = mutableListOf<String>()
        var listados = 0
        var creaciones = 0

        override suspend fun listarProveedores(pageToken: String?): ProveedorListResponse {
            listados++
            tokens += pageToken
            if (errorAlListar) error("sin red")
            return paginas[pageToken] ?: ProveedorListResponse(documentos.toList())
        }

        override suspend fun mostrarProveedor(id: String): ProveedorDocumento =
            documentos.first { it.name.endsWith("/$id") }

        override suspend fun crearProveedor(body: ProveedorContainer, documentId: String): ProveedorDocumento {
            creaciones++
            ids += documentId
            if (fallosCreacion-- > 0) error("fallo")
            bloqueo?.await()
            return ProveedorDocumento(
                "projects/db/databases/(default)/documents/proveedores/$documentId", body.fields
            ).also { documentos += it }
        }
    }
}
