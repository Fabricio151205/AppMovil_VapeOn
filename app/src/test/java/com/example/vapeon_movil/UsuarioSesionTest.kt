package com.example.vapeon_movil

import com.example.vapeon_movil.auth.ErrorAutenticacion
import com.example.vapeon_movil.auth.SesionFirebase
import com.example.vapeon_movil.auth.TipoErrorAutenticacion
import com.example.vapeon_movil.services.UsuarioService
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.viewmodels.UsuarioViewModel
import com.example.vapeon_movil.viewmodels.correoRegistroValido
import com.example.vapeon_movil.viewmodels.dniClienteValido
import com.example.vapeon_movil.viewmodels.telefonoRegistroValido
import com.example.vapeon_movil.viewmodels.validarRegistro
import com.example.vapeon_movil.viewmodels.validarMayoriaEdad
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class UsuarioSesionTest {
    private class ServicioUsuariosPrueba : UsuarioService {
        val documentos = mutableListOf<UsuarioDocumento>()
        val creados = mutableListOf<UsuarioContainer>()
        var consultas = 0
        var falloConsulta = false
        var falloMostrar = false
        var falloCreacion = false
        var perderRespuestaCreacion = false
        var respuestaPerfilPendiente: CompletableDeferred<UsuarioDocumento>? = null

        override suspend fun listarUsuarios(pageToken: String?): UsuarioListResponse {
            consultas++
            if (falloConsulta) error("Sin conexión")
            return UsuarioListResponse(documentos.toList())
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
        override suspend fun mostrarUsuario(id: String): UsuarioDocumento {
            if (falloMostrar) error("Sin conexión")
            respuestaPerfilPendiente?.let { return it.await() }
            return documentos.firstOrNull { it.name.endsWith("/$id") }
                ?: throw HttpException(
                    Response.error<Any>(
                        404,
                        ResponseBody.create(MediaType.parse("application/json"), "{}")
                    )
                )
        }
        override suspend fun actualizarUsuario(id: String, body: UsuarioContainer): UsuarioDocumento =
            error("No se utiliza")
        override suspend fun eliminarUsuario(id: String): Any = error("No se utiliza")
    }

    private fun documento(
        id: String = "A",
        correo: String = "a@example.com",
        rol: String = "CLIENTE"
    ) = UsuarioDocumento(
        "usuarios/$id",
        UsuarioCampos(
            correo = FirestoreStringValue(correo),
            rol = FirestoreStringValue(rol)
        )
    )

    private fun modelo(
        servicio: ServicioUsuariosPrueba = ServicioUsuariosPrueba(),
        auth: FakeAutenticacionFirebase = FakeAutenticacionFirebase()
    ) = UsuarioViewModel(servicio, Dispatchers.Unconfined, auth)

    // ─── Registro ───────────────────────────────────────────────────────────

    @Test fun registroConRespuestaPerdidaNoCreaOtraCuentaAlReintentar() {
        val servicio = ServicioUsuariosPrueba().apply { perderRespuestaCreacion = true }
        val auth = FakeAutenticacionFirebase()
        val vm = modelo(servicio, auth)
        var exitos = 0
        repeat(2) {
            vm.registrarUsuario("Ana", "Pérez", "ana@example.com", "912345678", "12345678", "01/01/2000", "x", "x") { exitos++ }
        }
        assertEquals(1, servicio.creados.size)
        assertEquals(1, exitos)
        assertTrue(vm.registroCompletado)
        assertEquals(1, auth.cuentas.size)
        assertNull(auth.sesion)
    }

    @Test fun registroBloqueaPulsacionesDuplicadasMientrasCarga() {
        val pendiente = CompletableDeferred<Unit>()
        val servicio = ServicioUsuariosPrueba()
        val auth = FakeAutenticacionFirebase().apply { esperaRegistro = pendiente }
        val vm = modelo(servicio, auth)
        repeat(2) { vm.registrarUsuario("Ana", "Pérez", "ana@example.com", "912345678", "12345678", "01/01/2000", "x", "x") }
        assertTrue(vm.cargandoRegistro)
        pendiente.complete(Unit)
        assertEquals(1, servicio.creados.size)
        assertTrue(vm.registroCompletado)
    }

    private fun errorRegistro(telefono: String = "912345678", correo: String = "a@example.com",
        dni: String = "12345678", fecha: String = "01/01/2000", password: String = "x", confirmacion: String = password,
        hoy: LocalDate = LocalDate.of(2026, 10, 30)) =
        validarRegistro("Ana", "Pérez", correo, telefono, dni, fecha, password, confirmacion, hoy)

    @Test fun dniDeOchoDigitosEsValido() {
        assertTrue(dniClienteValido("12345678"))
        assertNull(errorRegistro(dni = "12345678"))
    }

    @Test fun dniInvalidoEsRechazadoPorRegistro() {
        listOf("", "1234567", "123456789", "1234ABCD", "ABC12345").forEach {
            assertNotNull("Debe rechazar DNI: '$it'", errorRegistro(dni = it))
        }
    }

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

    @Test fun fechaClaramenteAdultaEsAceptada() {
        assertNull(errorRegistro(fecha = "29/02/2000"))
    }

    @Test fun menorDeDieciochoEsRechazado() {
        listOf("30/10/2025", "30/10/2009").forEach {
            assertEquals("Debes tener 18 años cumplidos o más para registrarte.", errorRegistro(fecha = it))
        }
    }

    @Test fun exactamenteDieciochoCumplidosHoyEsAceptado() {
        assertNull(errorRegistro(fecha = "30/10/2008"))
    }

    @Test fun cumpleDieciochoMananaEsRechazado() {
        assertEquals("Debes tener 18 años cumplidos o más para registrarte.",
            errorRegistro(fecha = "31/10/2008"))
    }

    @Test fun cumplioDieciochoAyerEsAceptado() {
        assertNull(errorRegistro(fecha = "29/10/2008"))
    }

    @Test fun reglaReutilizableDeMayoriaConservaLosLimitesDeRno01() {
        val hoy = LocalDate.of(2026, 10, 30)
        assertNull(validarMayoriaEdad("30/10/2008", hoy))
        assertNull(validarMayoriaEdad("29/10/2008", hoy))
        assertEquals("Debes tener 18 años cumplidos o más para registrarte.",
            validarMayoriaEdad("31/10/2008", hoy))
        listOf("", "31/02/2000", "01/01/2999").forEach {
            assertEquals("Ingresa una fecha de nacimiento válida en formato dd/mm/aaaa.",
                validarMayoriaEdad(it, hoy))
        }
    }

    @Test fun fechaFuturaEsRechazada() {
        assertEquals("Ingresa una fecha de nacimiento válida en formato dd/mm/aaaa.",
            errorRegistro(fecha = "31/10/2026"))
    }

    @Test fun fechaImposibleEsRechazada() {
        assertEquals("Ingresa una fecha de nacimiento válida en formato dd/mm/aaaa.",
            errorRegistro(fecha = "31/02/2000"))
    }

    @Test fun fechaVaciaEsRechazada() {
        assertEquals("Ingresa una fecha de nacimiento válida en formato dd/mm/aaaa.", errorRegistro(fecha = ""))
    }

    @Test fun nacimientoBisiestoCuentaAnosCompletos() {
        assertNotNull(errorRegistro(fecha = "29/02/2008", hoy = LocalDate.of(2026, 2, 28)))
        assertNull(errorRegistro(fecha = "29/02/2008", hoy = LocalDate.of(2026, 3, 1)))
    }

    @Test fun registroDeMenorNoConsultaNiCreaUsuarios() {
        val servicio = ServicioUsuariosPrueba()
        val auth = FakeAutenticacionFirebase()
        val vm = modelo(servicio, auth)
        val nacimiento = LocalDate.now().minusYears(1).format(DateTimeFormatter.ofPattern("dd/MM/uuuu"))
        vm.registrarUsuario("Ana", "Pérez", "ana@example.com", "912345678", "12345678", nacimiento, "x", "x")
        assertEquals("Debes tener 18 años cumplidos o más para registrarte.", vm.errorRegistro)
        assertEquals(0, servicio.consultas)
        assertTrue(servicio.creados.isEmpty())
        assertTrue(auth.cuentas.isEmpty())
        assertFalse(vm.registroCompletado)
        assertFalse(vm.cargandoRegistro)
    }

    @Test fun contrasenasDiferentesSonRechazadasSinImponerLongitudMinima() {
        assertNotNull(errorRegistro(password = "x", confirmacion = "y"))
        assertNotNull(errorRegistro(password = " x ", confirmacion = "x"))
        assertNull(errorRegistro(password = "x"))
        assertNotNull(errorRegistro(password = ""))
    }

    @Test fun nombreApellidoYCorreoSonObligatorios() {
        assertNotNull(validarRegistro(" ", "Pérez", "a@example.com", "912345678", "12345678", "01/01/2000", "x", "x"))
        assertNotNull(validarRegistro("Ana", " ", "a@example.com", "912345678", "12345678", "01/01/2000", "x", "x"))
        assertNotNull(errorRegistro(correo = " "))
    }

    @Test fun registroYLoginConservanExactamenteLaContrasenaEnFirebase() {
        val servicio = ServicioUsuariosPrueba()
        val auth = FakeAutenticacionFirebase()
        val vm = modelo(servicio, auth)
        var registrado = false
        vm.registrarUsuario("Ana", "Pérez", " Ana.Perez@Example.com ", "912345678",
            "12345678", "01/01/2000", " 1234 ", " 1234 ") { registrado = true }
        assertTrue(registrado)
        assertEquals("Ana.Perez@Example.com", servicio.creados.single().fields.correo?.stringValue)
        assertEquals("12345678", servicio.creados.single().fields.dni?.stringValue)
        // La contraseña nunca se escribe en Firestore.
        assertNull(servicio.creados.single().fields.password)
        assertEquals(" 1234 ", auth.cuentas.single().contrasena)

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
        val auth = FakeAutenticacionFirebase().apply {
            agregarCuenta("existente", "ana@example.com", "otra")
        }
        val vm = modelo(servicio, auth)
        vm.registrarUsuario("Ana", "Pérez", "ana@example.com", "912345678",
            "12345678", "01/01/2000", "x", "x") { fail("No debe registrar duplicados") }
        assertTrue(servicio.creados.isEmpty())
        assertNotNull(vm.errorRegistro)
        assertFalse(vm.cargandoRegistro)
    }

    @Test fun noConfirmaRegistroSiElPerfilNoSePuedeCrear() {
        val servicio = ServicioUsuariosPrueba().apply { falloCreacion = true }
        val auth = FakeAutenticacionFirebase()
        val vm = modelo(servicio, auth)
        vm.registrarUsuario("Ana", "Pérez", "ana@example.com", "912345678",
            "12345678", "01/01/2000", "x", "x") { fail("Falló el guardado") }
        assertTrue(servicio.creados.isEmpty())
        assertNotNull(vm.errorRegistro)
        assertFalse(vm.cargandoRegistro)
        assertFalse(vm.registroCompletado)
        // No se deja una sesión Firebase incompleta.
        assertNull(auth.sesion)
    }

    @Test fun formularioInvalidoNoEnviaRegistro() {
        val servicio = ServicioUsuariosPrueba()
        val auth = FakeAutenticacionFirebase()
        val vm = modelo(servicio, auth)
        vm.registrarUsuario("Ana", "Pérez", "ana@example.com", "9abc45678",
            "12345678", "01/01/2000", "x", "x") { fail("Formulario inválido") }
        assertTrue(servicio.creados.isEmpty())
        assertTrue(auth.cuentas.isEmpty())
        assertNotNull(vm.errorRegistro)
    }

    // ─── Login y sesión ─────────────────────────────────────────────────────

    @Test fun cerrarSesionLimpiaUsuarioRolEstadosYCierraFirebase() {
        val servicio = ServicioUsuariosPrueba().apply {
            documentos += documento(rol = "ADMIN")
        }
        val auth = FakeAutenticacionFirebase().apply {
            agregarCuenta("A", "a@example.com", " 1234 ")
        }
        val vm = modelo(servicio, auth)
        vm.iniciarSesion("a@example.com", " 1234 ", {})
        assertNotNull(vm.usuarioActual)
        assertTrue(vm.esAdmin)
        vm.cerrarSesion()
        assertNull(vm.usuarioActual)
        assertFalse(vm.esAdmin)
        assertFalse(vm.cargandoLogin)
        assertNull(vm.errorLogin)
        assertNull(auth.sesion)
    }

    @Test fun loginFallidoOVacioNoConservaSesionAnterior() {
        val servicio = ServicioUsuariosPrueba().apply {
            documentos += documento(rol = "ADMIN")
        }
        val auth = FakeAutenticacionFirebase().apply {
            agregarCuenta("A", "a@example.com", " 1234 ")
        }
        val vm = modelo(servicio, auth)
        listOf("incorrecta", "").forEach { password ->
            vm.iniciarSesion("a@example.com", " 1234 ", {})
            assertNotNull(vm.usuarioActual)
            vm.iniciarSesion("a@example.com", password, { fail("Login inválido") })
            assertNull(vm.usuarioActual)
            assertFalse(vm.esAdmin)
            assertNotNull(vm.errorLogin)
            assertNull(auth.sesion)
        }
    }

    @Test fun errorDeRedEnNuevoLoginNoConservaSesionAnterior() {
        val servicio = ServicioUsuariosPrueba().apply { documentos += documento() }
        val auth = FakeAutenticacionFirebase().apply {
            agregarCuenta("A", "a@example.com", " 1234 ")
            agregarCuenta("B", "b@example.com", "otra")
        }
        val vm = modelo(servicio, auth)
        vm.iniciarSesion("a@example.com", " 1234 ", {})
        servicio.falloMostrar = true
        vm.iniciarSesion("b@example.com", "otra", { fail("La consulta falló") })
        assertNull(vm.usuarioActual)
        assertNotNull(vm.errorLogin)
        assertFalse(vm.cargandoLogin)
        assertNull(auth.sesion)
    }

    @Test fun perfilInexistenteCierraLaSesionFirebase() {
        val servicio = ServicioUsuariosPrueba()
        val auth = FakeAutenticacionFirebase().apply {
            agregarCuenta("sin-perfil", "huerfano@example.com", "x")
        }
        val vm = modelo(servicio, auth)
        vm.iniciarSesion("huerfano@example.com", "x", { fail("No existe perfil") })
        assertNull(vm.usuarioActual)
        assertNotNull(vm.errorLogin)
        assertNull(auth.sesion)
    }

    @Test fun cerrarSesionCancelaLoginPendienteYPermiteEntrarComoB() {
        val servicio = ServicioUsuariosPrueba().apply {
            documentos += documento(id = "A", correo = "a@example.com")
            documentos += documento(id = "B", correo = "b@example.com")
        }
        val auth = FakeAutenticacionFirebase().apply {
            agregarCuenta("A", "a@example.com", " 1234 ")
            agregarCuenta("B", "b@example.com", "b")
        }
        val pendiente = CompletableDeferred<UsuarioDocumento>()
        servicio.respuestaPerfilPendiente = pendiente
        val vm = modelo(servicio, auth)
        vm.iniciarSesion("a@example.com", " 1234 ", { fail("Sesión cancelada") })
        assertTrue(vm.cargandoLogin)
        vm.cerrarSesion()
        pendiente.complete(servicio.documentos.first { it.name.endsWith("/A") })
        assertNull(vm.usuarioActual)
        assertFalse(vm.cargandoLogin)
        servicio.respuestaPerfilPendiente = null
        vm.iniciarSesion("b@example.com", "b", {})
        assertEquals("B", vm.usuarioActual?.id)
    }

    @Test fun restauraSesionPersistidaConPerfil() {
        val servicio = ServicioUsuariosPrueba().apply { documentos += documento(id = "A") }
        val auth = FakeAutenticacionFirebase().apply {
            agregarCuenta("A", "a@example.com", "x")
            sesion = SesionFirebase("A", "a@example.com", true)
        }
        val vm = modelo(servicio, auth)
        vm.restaurarSesion()
        assertEquals("A", vm.usuarioActual?.id)
        assertTrue(vm.restauracionCompletada)
        assertFalse(vm.restaurandoSesion)
    }

    @Test fun restauracionSinPerfilCierraLaSesionFirebase() {
        val auth = FakeAutenticacionFirebase().apply {
            sesion = SesionFirebase("fantasma", "fantasma@example.com", true)
        }
        val vm = modelo(ServicioUsuariosPrueba(), auth)
        vm.restaurarSesion()
        assertNull(vm.usuarioActual)
        assertNull(auth.sesion)
        assertTrue(vm.restauracionCompletada)
    }

    // ─── Recuperación de contraseña ─────────────────────────────────────────

    @Test fun recuperacionEnviaCorreoReal() {
        val auth = FakeAutenticacionFirebase()
        val vm = modelo(ServicioUsuariosPrueba(), auth)
        var exito = false
        vm.enviarCorreoRestablecimiento("ana@example.com", { exito = true })
        assertTrue(exito)
        assertEquals(listOf("ana@example.com"), auth.correosRestablecimiento)
    }

    @Test fun recuperacionNoRevelaSiElCorreoNoExiste() {
        val auth = FakeAutenticacionFirebase().apply {
            falloRestablecimiento = ErrorAutenticacion(
                TipoErrorAutenticacion.USUARIO_NO_ENCONTRADO,
                "No existe una cuenta con ese correo."
            )
        }
        val vm = modelo(ServicioUsuariosPrueba(), auth)
        var exito = false
        var error: String? = null
        vm.enviarCorreoRestablecimiento("desconocido@example.com",
            onExito = { exito = true }, onError = { error = it })
        assertTrue(exito)
        assertNull(error)
    }

    @Test fun recuperacionRechazaCorreoInvalidoSinLlamarFirebase() {
        val auth = FakeAutenticacionFirebase()
        val vm = modelo(ServicioUsuariosPrueba(), auth)
        var error: String? = null
        vm.enviarCorreoRestablecimiento("no-es-correo", onExito = { fail("Inválido") },
            onError = { error = it })
        assertNotNull(error)
        assertTrue(auth.correosRestablecimiento.isEmpty())
    }
}
