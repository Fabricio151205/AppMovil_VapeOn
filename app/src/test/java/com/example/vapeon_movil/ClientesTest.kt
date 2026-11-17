package com.example.vapeon_movil

import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.services.UsuarioClientesService
import com.example.vapeon_movil.services.UsuarioService
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.viewmodels.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ClientesTest {
    private val admin = Usuario(id = "admin", rol = "ADMIN")
    private val cliente = Usuario(id = "c1", rol = "CLIENTE")

    private fun doc(id: String, rol: String = "CLIENTE", nombre: String = "Ana",
        apellido: String = "Pérez", correo: String = "$id@correo.com", dni: String? = "12345678",
        telefono: String = "987654321", fecha: String = "01/01/1990",
        createTime: String? = "2026-01-01T00:00:00Z", updateTime: String? = "2026-01-02T00:00:00Z",
        password: String? = "secreto") = UsuarioDocumento(
        "usuarios/$id", UsuarioCampos(
            nombre = FirestoreStringValue(nombre), apellido = FirestoreStringValue(apellido),
            correo = FirestoreStringValue(correo), dni = dni?.let(::FirestoreStringValue),
            telefono = FirestoreStringValue(telefono), fechaNacimiento = FirestoreStringValue(fecha),
            rol = FirestoreStringValue(rol), password = password?.let(::FirestoreStringValue)
        ), createTime, updateTime
    )

    private class UsuariosFake(
        val paginas: Map<String?, UsuarioListResponse> = mapOf(null to UsuarioListResponse(emptyList())),
        val falla: Boolean = false,
        val individuales: Map<String, UsuarioDocumento> = emptyMap(),
        val fallaDetalle: Throwable? = null
    ) : UsuarioService {
        val tokens = mutableListOf<String?>()
        override suspend fun listarUsuarios(pageToken: String?): UsuarioListResponse {
            tokens += pageToken
            if (falla) error("red")
            return paginas[pageToken] ?: UsuarioListResponse(emptyList())
        }
        override suspend fun mostrarUsuario(id: String): UsuarioDocumento {
            fallaDetalle?.let { throw it }
            return individuales[id] ?: throw HttpException(Response.error<Any>(404,
                ResponseBody.create(MediaType.parse("application/json"), "{}")))
        }
        override suspend fun crearUsuario(body: UsuarioContainer, documentId: String?) = error("No se usa")
        override suspend fun actualizarUsuario(id: String, body: UsuarioContainer) = error("No se usa")
        override suspend fun eliminarUsuario(id: String) = error("No se usa")
    }

    private class ClientesFake(var respuesta: UsuarioDocumento? = null, var fallo: Throwable? = null) : UsuarioClientesService {
        var llamadas = 0; var campos = emptyList<String>(); var version = ""; var body: UsuarioContainer? = null
        var espera: CompletableDeferred<Unit>? = null
        override suspend fun actualizarCliente(id: String, campos: List<String>, updateTime: String,
            body: UsuarioContainer): UsuarioDocumento {
            llamadas++; this.campos = campos; version = updateTime; this.body = body
            espera?.await(); fallo?.let { throw it }
            return respuesta ?: UsuarioDocumento("usuarios/$id", body.fields,
                "2026-01-01T00:00:00Z", "2026-02-01T00:00:00Z")
        }
    }

    private fun vm(usuarios: UsuariosFake, clientes: ClientesFake = ClientesFake()) =
        ClientesViewModel(usuarios, clientes, Dispatchers.Unconfined)

    @Test fun adminCargaPaginasDeduplicaExcluyeAdminYOrdena() {
        val service = UsuariosFake(mapOf(
            null to UsuarioListResponse(listOf(doc("2", nombre = "Zoe"), doc("a", rol = "ADMIN")), "p2"),
            "p2" to UsuarioListResponse(listOf(doc("2", nombre = "Zoe"), doc("1", nombre = "Ana")))
        ))
        val model = vm(service); model.cargarClientes(admin)
        assertEquals(listOf(null, "p2"), service.tokens)
        assertEquals(listOf("1", "2"), model.clientes.map { it.id })
        assertTrue(model.clientes.all { it.rol == "CLIENTE" && it.password.isEmpty() })
    }

    @Test fun clienteYSinSesionNoPuedenCargarNiVerNiEditar() {
        listOf(cliente, null).forEach { actor ->
            val model = vm(UsuariosFake(mapOf(null to UsuarioListResponse(listOf(doc("1"))))))
            model.cargarClientes(actor); assertTrue(model.clientes.isEmpty()); assertNotNull(model.error)
            assertFalse(model.seleccionarCliente(actor, "1")); assertFalse(model.prepararEdicion(actor))
        }
    }

    @Test fun listaVaciaYErrorSeRepresentanSinDatosFicticios() {
        val vacio = vm(UsuariosFake()); vacio.cargarClientes(admin); assertTrue(vacio.clientes.isEmpty())
        val error = vm(UsuariosFake(falla = true)); error.cargarClientes(admin)
        assertTrue(error.clientes.isEmpty()); assertNotNull(error.error)
    }

    @Test fun busquedaCubreNombreApellidoCompletoCorreoDniCasoYDniNulo() {
        val model = vm(UsuariosFake(mapOf(null to UsuarioListResponse(listOf(
            doc("1", nombre = "María", apellido = "López", correo = "maria@correo.com", dni = "87654321"),
            doc("2", nombre = "Carlos", apellido = "Ríos", dni = null)
        )))))
        model.cargarClientes(admin)
        listOf("MARÍA", "lópez", "maría lópez", "maria@", "8765").forEach {
            model.cambiarBusqueda(it); assertEquals("1", model.clientesFiltrados.single().id)
        }
        model.cambiarBusqueda("inexistente"); assertTrue(model.clientesFiltrados.isEmpty())
    }

    @Test fun detalleConservaAusenciasPeroNuncaPassword() {
        val model = vm(UsuariosFake(mapOf(null to UsuarioListResponse(listOf(
            doc("1", dni = null, createTime = null, password = "muy-secreto")
        )))))
        model.cargarClientes(admin); assertTrue(model.seleccionarCliente(admin, "1"))
        assertNull(model.clienteSeleccionado!!.dni); assertNull(model.clienteSeleccionado!!.createTime)
        assertEquals("", model.clienteSeleccionado!!.password)
        assertFalse(model.seleccionarCliente(admin, "no-existe"))
    }

    @Test fun validacionesIncluyenDniOpcionalCorreoTelefonoYMayoriaEdad() {
        val valido = FormularioCliente("Ana", "Pérez", "ana@correo.com", "", "987654321", "01/01/1990")
        assertFalse(validarFormularioCliente(valido).hayErrores())
        assertTrue(validarFormularioCliente(valido.copy(nombre = "")).hayErrores())
        assertTrue(validarFormularioCliente(valido.copy(apellido = "")).hayErrores())
        assertTrue(validarFormularioCliente(valido.copy(correo = "mal")).hayErrores())
        assertTrue(validarFormularioCliente(valido.copy(dni = "12A")).hayErrores())
        assertTrue(validarFormularioCliente(valido.copy(telefono = "123")).hayErrores())
        assertTrue(validarFormularioCliente(valido.copy(fechaNacimiento = "01/01/2020")).hayErrores())
    }

    @Test fun patchEnviaSoloCamposModificadosMascaraYVersionSinPasswordNiRol() {
        val usuarios = UsuariosFake(mapOf(null to UsuarioListResponse(listOf(doc("1")))))
        val api = ClientesFake(respuesta = doc("1", nombre = "Nueva", dni = "12345678",
            updateTime = "2026-03-01T00:00:00Z"))
        val model = vm(usuarios, api); model.cargarClientes(admin); model.seleccionarCliente(admin, "1")
        model.prepararEdicion(admin); model.actualizarFormulario(model.formulario.copy(nombre = "Nueva"))
        model.guardarCambios(admin)
        assertEquals(listOf("nombre"), api.campos); assertEquals("2026-01-02T00:00:00Z", api.version)
        assertEquals("Nueva", api.body!!.fields.nombre!!.stringValue)
        assertNull(api.body!!.fields.password); assertNull(api.body!!.fields.rol)
    }

    @Test fun correoPropioEsAceptadoYOtroCorreoDuplicadoEsRechazado() {
        val usuarios = UsuariosFake(mapOf(null to UsuarioListResponse(listOf(doc("1"), doc("2", correo = "otro@correo.com")))))
        val api = ClientesFake(respuesta = doc("1", nombre = "Nueva"))
        val model = vm(usuarios, api); model.cargarClientes(admin); model.seleccionarCliente(admin, "1"); model.prepararEdicion(admin)
        model.actualizarFormulario(model.formulario.copy(nombre = "Nueva")); model.guardarCambios(admin)
        assertEquals(1, api.llamadas)
        model.actualizarFormulario(model.formulario.copy(correo = "otro@correo.com")); model.guardarCambios(admin)
        assertEquals(1, api.llamadas); assertNotNull(model.erroresFormulario.correo)
    }

    @Test fun conflictoYErrorRedNoCambianEstadoLocal() {
        listOf(
            HttpException(Response.error<Any>(412, ResponseBody.create(MediaType.parse("application/json"), "{}"))),
            IllegalStateException("red")
        ).forEachIndexed { indice, fallo ->
            val api = ClientesFake(fallo = fallo)
            val model = vm(UsuariosFake(mapOf(null to UsuarioListResponse(listOf(doc("1"))))), api)
            model.cargarClientes(admin); model.seleccionarCliente(admin, "1"); model.prepararEdicion(admin)
            model.actualizarFormulario(model.formulario.copy(nombre = "Cambio")); model.guardarCambios(admin)
            assertEquals("Ana", model.clienteSeleccionado!!.nombre); assertNotNull(model.error)
            assertEquals(indice == 0, model.conflicto)
        }
    }

    @Test fun failedPreconditionEnHttp400EsConflictoYNoCambiaEstadoLocal() {
        val cuerpo = """{"error":{"status":"FAILED_PRECONDITION","message":"stale"}}"""
        val api = ClientesFake(fallo = HttpException(Response.error<Any>(400,
            ResponseBody.create(MediaType.parse("application/json"), cuerpo))))
        val model = vm(UsuariosFake(mapOf(null to UsuarioListResponse(listOf(doc("1"))))), api)
        model.cargarClientes(admin); model.seleccionarCliente(admin, "1"); model.prepararEdicion(admin)
        model.actualizarFormulario(model.formulario.copy(nombre = "Cambio")); model.guardarCambios(admin)
        assertTrue(model.conflicto)
        assertEquals("Ana", model.clienteSeleccionado!!.nombre)
        assertTrue(model.error!!.contains("modificados desde otro dispositivo"))
    }

    @Test fun restauraClientePorIdSinGuardarObjetoCompletoYPreparaEdicion() {
        val recuperado = doc("restaurado", nombre = "Lucía", password = "no-debe-copiarse")
        val model = vm(UsuariosFake(individuales = mapOf("restaurado" to recuperado)))
        model.recuperarCliente(admin, "restaurado", paraEditar = true)
        assertEquals("restaurado", model.clienteSeleccionado!!.id)
        assertEquals("", model.clienteSeleccionado!!.password)
        assertEquals("Lucía", model.formulario.nombre)
        assertFalse(model.cargandoDetalle)
        assertNull(model.errorDetalle)
    }

    @Test fun restauracionInexistenteYUsuarioAdminSonEstadosControlados() {
        val inexistente = vm(UsuariosFake())
        inexistente.recuperarCliente(admin, "ausente")
        assertNull(inexistente.clienteSeleccionado); assertNotNull(inexistente.errorDetalle)

        val usuarioAdmin = doc("otro-admin", rol = "ADMIN")
        val noCliente = vm(UsuariosFake(individuales = mapOf("otro-admin" to usuarioAdmin)))
        noCliente.recuperarCliente(admin, "otro-admin")
        assertNull(noCliente.clienteSeleccionado)
        assertTrue(noCliente.errorDetalle!!.contains("no es un cliente"))
    }

    @Test fun dobleEnvioQuedaBloqueado() {
        val api = ClientesFake().also { it.espera = CompletableDeferred() }
        val model = vm(UsuariosFake(mapOf(null to UsuarioListResponse(listOf(doc("1"))))), api)
        model.cargarClientes(admin); model.seleccionarCliente(admin, "1"); model.prepararEdicion(admin)
        model.actualizarFormulario(model.formulario.copy(nombre = "Cambio"))
        model.guardarCambios(admin); model.guardarCambios(admin)
        assertEquals(1, api.llamadas); api.espera!!.complete(Unit)
    }
}
