package com.example.vapeon_movil

import com.example.vapeon_movil.services.UsuarioService
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.viewmodels.UsuarioViewModel
import com.example.vapeon_movil.viewmodels.correoRegistroValido
import com.example.vapeon_movil.viewmodels.telefonoRegistroValido
import com.example.vapeon_movil.viewmodels.validarRegistro
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class UsuarioSesionTest {
    private class ServicioUsuariosPrueba : UsuarioService {
        val documentos = mutableListOf<UsuarioDocumento>()
        val creados = mutableListOf<UsuarioContainer>()
        var falloConsulta = false
        var falloCreacion = false
        var perderRespuestaCreacion = false
        var respuestaPendiente: CompletableDeferred<UsuarioListResponse>? = null

        override suspend fun listarUsuarios(pageToken: String?): UsuarioListResponse {
            if (falloConsulta) error("Sin conexión")
            return respuestaPendiente?.await() ?: UsuarioListResponse(documentos.toList())
        }
        override suspend fun crearUsuario(body: UsuarioContainer, documentId: String?): Any {
            if (falloCreacion) error("Sin conexión")
            creados += body
            documentos += UsuarioDocumento("usuarios/${documentId ?: "nuevo"}", body.fields)
            if (perderRespuestaCreacion) {
                perderRespuestaCreacion = false
                throw java.io.IOException("Respuesta perdida")
            }
            return Unit
        }
        override suspend fun mostrarUsuario(id: String): UsuarioDocumento = error("No se utiliza")
        override suspend fun actualizarUsuario(id: String, body: UsuarioContainer): UsuarioDocumento =
            error("No se utiliza")
        override suspend fun eliminarUsuario(id: String): Any = error("No se utiliza")
    }

    private fun documento(id: String = "A", correo: String = "a@example.com",
        password: String = " 1234 ", rol: String = "CLIENTE") = UsuarioDocumento("usuarios/$id",
        UsuarioCampos(correo = FirestoreStringValue(correo), password = FirestoreStringValue(password),
            rol = FirestoreStringValue(rol)))

    @Test fun registroConRespuestaPerdidaNoCreaOtraCuentaAlReintentar() {
        val servicio = ServicioUsuariosPrueba().apply { perderRespuestaCreacion = true }
        val vm = UsuarioViewModel(servicio, Dispatchers.Unconfined)
        var exitos = 0
        repeat(2) {
            vm.registrarUsuario("Ana", "Pérez", "ana@example.com", "912345678", "01/01/2000", "x", "x") { exitos++ }
        }
        assertEquals(1, servicio.creados.size)
        assertEquals(1, exitos)
        assertTrue(vm.registroCompletado)
    }

    @Test fun registroBloqueaPulsacionesDuplicadasMientrasCarga() {
        val pendiente = CompletableDeferred<UsuarioListResponse>()
        val servicio = ServicioUsuariosPrueba().apply { respuestaPendiente = pendiente }
        val vm = UsuarioViewModel(servicio, Dispatchers.Unconfined)
        repeat(2) { vm.registrarUsuario("Ana", "Pérez", "ana@example.com", "912345678", "01/01/2000", "x", "x") }
        assertTrue(vm.cargandoRegistro)
        pendiente.complete(UsuarioListResponse(emptyList()))
        assertEquals(1, servicio.creados.size)
        assertTrue(vm.registroCompletado)
    }

    private fun errorRegistro(telefono: String = "912345678", correo: String = "a@example.com",
        fecha: String = "01/01/2000", password: String = "x", confirmacion: String = password) =
        validarRegistro("Ana", "Pérez", correo, telefono, fecha, password, confirmacion)

    @Test fun telefonosValidos() {
        listOf("912345678", "987654321").forEach { assertTrue(telefonoRegistroValido(it)) }
    }

    @Test fun telefonoConLetrasEsRechazado() {
        assertFalse(telefonoRegistroValido("9abc45678"))
        assertNotNull(errorRegistro(telefono = "9abc45678"))
    }

    @Test fun longitudIncorrectaEsRechazada() {
        listOf("", "91234567", "9123456789").forEach {
            assertFalse(telefonoRegistroValido(it))
            assertNotNull(errorRegistro(telefono = it))
        }
    }

    @Test fun telefonoDebeComenzarConNueve() {
        assertFalse(telefonoRegistroValido("812345678"))
    }

    @Test fun noSeRecortanNiAceptanCaracteresAdicionalesDelTelefono() {
        listOf("+51912345678", "9 12345678", " 912345678", "912345678 ", "９12345678").forEach {
            assertFalse(telefonoRegistroValido(it))
        }
    }

    @Test fun correosValidosConRecorteExterior() {
        listOf("ana@example.com", " Ana.Perez+compras@Example.com ", "ana@correo.edu.pe").forEach {
            assertTrue(correoRegistroValido(it))
            assertNull(errorRegistro(correo = it))
        }
    }

    @Test fun correosInvalidosSonRechazados() {
        listOf("", "ana", "ana@", "@example.com", "ana@example", "a@@example.com",
            "ana perez@example.com", "ana@example..com", ".ana@example.com", "ana..p@example.com").forEach {
            assertFalse(it, correoRegistroValido(it))
            assertNotNull(errorRegistro(correo = it))
        }
    }

    @Test fun fechasInvalidasVaciasOFuturasSonRechazadas() {
        listOf("", "31/02/2000", "29/02/2025", "00/12/2000", "15/13/2000", "01/01/0000",
            "2000-01-01", "1/1/2000", "mañana", "01/01/2999").forEach {
            assertNotNull(it, errorRegistro(fecha = it))
        }
    }

    @Test fun fechaValidaSinAgregarMayoriaDeEdad() {
        assertNull(errorRegistro(fecha = "29/02/2000"))
        val fechaReciente = LocalDate.now().minusYears(1).format(DateTimeFormatter.ofPattern("dd/MM/uuuu"))
        assertNull(errorRegistro(fecha = fechaReciente))
    }

    @Test fun contrasenasDiferentesSonRechazadasSinImponerLongitudMinima() {
        assertNotNull(errorRegistro(password = "x", confirmacion = "y"))
        assertNotNull(errorRegistro(password = " x ", confirmacion = "x"))
        assertNull(errorRegistro(password = "x"))
        assertNotNull(errorRegistro(password = ""))
    }

    @Test fun nombreApellidoYCorreoSonObligatorios() {
        assertNotNull(validarRegistro(" ", "Pérez", "a@example.com", "912345678", "01/01/2000", "x", "x"))
        assertNotNull(validarRegistro("Ana", " ", "a@example.com", "912345678", "01/01/2000", "x", "x"))
        assertNotNull(errorRegistro(correo = " "))
    }

    @Test fun registroYLoginConservanExactamenteLaContrasenaConEspacios() {
        val servicio = ServicioUsuariosPrueba()
        val vm = UsuarioViewModel(servicio, Dispatchers.Unconfined)
        var registrado = false
        vm.registrarUsuario("Ana", "Pérez", " Ana.Perez@Example.com ", "912345678",
            "01/01/2000", " 1234 ", " 1234 ") { registrado = true }
        assertTrue(registrado)
        assertEquals("Ana.Perez@Example.com", servicio.creados.single().fields.correo?.stringValue)
        assertEquals(" 1234 ", servicio.creados.single().fields.password?.stringValue)
        var autenticado = false
        vm.iniciarSesion(" ana.perez@example.com ", " 1234 ", { autenticado = true })
        assertTrue(autenticado)
        assertNotNull(vm.usuarioActual)
        vm.iniciarSesion("ana.perez@example.com", "1234", { fail("Contraseña modificada") })
        assertNull(vm.usuarioActual)
        assertNotNull(vm.errorLogin)
    }

    @Test fun correoDuplicadoNoGeneraOtroDocumento() {
        val servicio = ServicioUsuariosPrueba()
        servicio.documentos += documento(correo = " Ana@Example.com ")
        val vm = UsuarioViewModel(servicio, Dispatchers.Unconfined)
        vm.registrarUsuario("Ana", "Pérez", "ana@example.com", "912345678",
            "01/01/2000", "x", "x") { fail("No debe registrar duplicados") }
        assertTrue(servicio.creados.isEmpty())
        assertNotNull(vm.errorRegistro)
        assertFalse(vm.cargandoRegistro)
    }

    @Test fun noRegistraSiNoPuedeComprobarDuplicados() {
        val servicio = ServicioUsuariosPrueba().apply { falloConsulta = true }
        val vm = UsuarioViewModel(servicio, Dispatchers.Unconfined)
        vm.registrarUsuario("Ana", "Pérez", "ana@example.com", "912345678",
            "01/01/2000", "x", "x") { fail("La consulta falló") }
        assertTrue(servicio.creados.isEmpty())
        assertNotNull(vm.errorRegistro)
        assertFalse(vm.cargandoRegistro)
    }

    @Test fun formularioInvalidoNoEnviaRegistro() {
        val servicio = ServicioUsuariosPrueba()
        val vm = UsuarioViewModel(servicio, Dispatchers.Unconfined)
        vm.registrarUsuario("Ana", "Pérez", "ana@example.com", "9abc45678",
            "01/01/2000", "x", "x") { fail("Formulario inválido") }
        assertTrue(servicio.creados.isEmpty())
        assertNotNull(vm.errorRegistro)
    }

    @Test fun errorAlCrearSeMuestraSinConfirmarRegistro() {
        val servicio = ServicioUsuariosPrueba().apply { falloCreacion = true }
        val vm = UsuarioViewModel(servicio, Dispatchers.Unconfined)
        vm.registrarUsuario("Ana", "Pérez", "ana@example.com", "912345678",
            "01/01/2000", "x", "x") { fail("Falló el guardado") }
        assertNotNull(vm.errorRegistro)
        assertFalse(vm.cargandoRegistro)
    }

    @Test fun cerrarSesionLimpiaUsuarioRolYEstadosDeLogin() {
        val servicio = ServicioUsuariosPrueba().apply { documentos += documento(rol = "ADMIN") }
        val vm = UsuarioViewModel(servicio, Dispatchers.Unconfined)
        vm.iniciarSesion("a@example.com", " 1234 ", {})
        assertNotNull(vm.usuarioActual)
        assertTrue(vm.esAdmin)
        vm.cerrarSesion()
        assertNull(vm.usuarioActual)
        assertFalse(vm.esAdmin)
        assertFalse(vm.cargandoLogin)
        assertNull(vm.errorLogin)
    }

    @Test fun loginFallidoOVacioNoConservaSesionAnterior() {
        val servicio = ServicioUsuariosPrueba().apply { documentos += documento(rol = "ADMIN") }
        val vm = UsuarioViewModel(servicio, Dispatchers.Unconfined)
        listOf("incorrecta", "").forEach { password ->
            vm.iniciarSesion("a@example.com", " 1234 ", {})
            assertNotNull(vm.usuarioActual)
            vm.iniciarSesion("a@example.com", password, { fail("Login inválido") })
            assertNull(vm.usuarioActual)
            assertFalse(vm.esAdmin)
            assertNotNull(vm.errorLogin)
        }
    }

    @Test fun errorDeRedEnNuevoLoginNoConservaSesionAnterior() {
        val servicio = ServicioUsuariosPrueba().apply { documentos += documento() }
        val vm = UsuarioViewModel(servicio, Dispatchers.Unconfined)
        vm.iniciarSesion("a@example.com", " 1234 ", {})
        servicio.falloConsulta = true
        vm.iniciarSesion("b@example.com", "otra", { fail("La consulta falló") })
        assertNull(vm.usuarioActual)
        assertNotNull(vm.errorLogin)
        assertFalse(vm.cargandoLogin)
    }

    @Test fun cerrarSesionCancelaLoginPendienteYPermiteEntrarComoB() {
        val servicio = ServicioUsuariosPrueba()
        val pendiente = CompletableDeferred<UsuarioListResponse>()
        servicio.respuestaPendiente = pendiente
        val vm = UsuarioViewModel(servicio, Dispatchers.Unconfined)
        vm.iniciarSesion("a@example.com", " 1234 ", { fail("Sesión cancelada") })
        assertTrue(vm.cargandoLogin)
        vm.cerrarSesion()
        pendiente.complete(UsuarioListResponse(listOf(documento())))
        assertNull(vm.usuarioActual)
        assertFalse(vm.cargandoLogin)
        servicio.respuestaPendiente = null
        servicio.documentos += documento("B", "b@example.com", "b")
        vm.iniciarSesion("b@example.com", "b", {})
        assertEquals("B", vm.usuarioActual?.id)
    }
}
