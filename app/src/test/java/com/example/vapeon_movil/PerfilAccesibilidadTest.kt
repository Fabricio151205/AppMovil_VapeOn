package com.example.vapeon_movil

import com.example.vapeon_movil.entities.edadDesde
import com.example.vapeon_movil.entities.esMayorDeEdad
import com.example.vapeon_movil.entities.rolLegible
import com.example.vapeon_movil.services.UsuarioService
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.ui.theme.AjustesAccesibilidad
import com.example.vapeon_movil.ui.theme.TamanoTexto
import com.example.vapeon_movil.viewmodels.UsuarioViewModel
import com.example.vapeon_movil.viewmodels.validarPerfil
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class PerfilAccesibilidadTest {

    private fun perfil(
        telefono: String = "912345678",
        correo: String = "ana@example.com",
        fecha: String = "01/01/2000",
        hoy: LocalDate = LocalDate.of(2026, 11, 7)
    ) = validarPerfil("Ana", "Pérez", correo, telefono, fecha, hoy)

    @Test fun perfilValidoEsAceptado() {
        assertNull(perfil())
    }

    @Test fun perfilExigeCamposObligatorios() {
        assertNotNull(validarPerfil(" ", "Pérez", "ana@example.com", "912345678", "01/01/2000"))
        assertNotNull(validarPerfil("Ana", " ", "ana@example.com", "912345678", "01/01/2000"))
        assertNotNull(perfil(correo = " "))
    }

    @Test fun perfilValidaTelefonoYCorreo() {
        listOf("812345678", "91234567", "9abc45678", "9123456789").forEach {
            assertNotNull(it, perfil(telefono = it))
        }
        listOf("ana", "ana@", "@example.com", "ana@example").forEach {
            assertNotNull(it, perfil(correo = it))
        }
    }

    @Test fun perfilValidaFechaYMayoriaDeEdad() {
        assertNotNull(perfil(fecha = "31/02/2000"))
        assertNotNull(perfil(fecha = "2000-01-01"))
        assertEquals("El perfil requiere ser mayor de 18 años.",
            perfil(fecha = "08/11/2008"))
        assertNull(perfil(fecha = "07/11/2008"))
    }

    @Test fun edadYCondicionSeCalculanDesdeLaFechaReal() {
        val hoy = LocalDate.of(2026, 11, 7)
        assertEquals(26, edadDesde("01/01/2000", hoy))
        assertEquals(18, edadDesde("07/11/2008", hoy))
        assertTrue(esMayorDeEdad("07/11/2008", hoy))
        assertFalse(esMayorDeEdad("08/11/2008", hoy))
        assertNull(edadDesde("fecha inválida", hoy))
    }

    @Test fun rolSeMuestraDeFormaLegible() {
        assertEquals("Administrador", rolLegible("ADMIN"))
        assertEquals("Cliente", rolLegible("cliente"))
    }

    @Test fun accesibilidadConservaValoresPorDefectoYEscalas() {
        val ajustes = AjustesAccesibilidad()
        assertEquals(TamanoTexto.MEDIANO, ajustes.tamanoTexto)
        assertFalse(ajustes.textoNegrita)
        assertFalse(ajustes.altoContraste)
        assertFalse(ajustes.reducirTransparencia)
        assertTrue(TamanoTexto.PEQUENO.escala < TamanoTexto.MEDIANO.escala)
        assertTrue(TamanoTexto.GRANDE.escala > TamanoTexto.MEDIANO.escala)
    }

    private class ServicioPerfilPrueba(
        private val documentos: MutableList<UsuarioDocumento>
    ) : UsuarioService {
        var actualizado: UsuarioContainer? = null
        override suspend fun listarUsuarios(pageToken: String?): UsuarioListResponse =
            UsuarioListResponse(documentos.toList())
        override suspend fun crearUsuario(body: UsuarioContainer, documentId: String?): Any =
            error("No se utiliza")
        override suspend fun mostrarUsuario(id: String): UsuarioDocumento =
            documentos.first { it.name.endsWith("/$id") }
        override suspend fun actualizarUsuario(id: String, body: UsuarioContainer): UsuarioDocumento {
            actualizado = body
            val nuevo = UsuarioDocumento("usuarios/$id", body.fields)
            documentos.removeAll { it.name.endsWith("/$id") }
            documentos += nuevo
            return nuevo
        }
        override suspend fun eliminarUsuario(id: String): Any = error("No se utiliza")
    }

    private fun documento(
        id: String = "A",
        nombre: String = "Ana",
        apellido: String = "Pérez",
        telefono: String = "912345678",
        fecha: String = "01/01/2000",
        correo: String = "ana@example.com",
        password: String = "clave",
        rol: String = "ADMIN"
    ) = UsuarioDocumento(
        "usuarios/$id",
        UsuarioCampos(
            nombre = FirestoreStringValue(nombre),
            apellido = FirestoreStringValue(apellido),
            telefono = FirestoreStringValue(telefono),
            fechaNacimiento = FirestoreStringValue(fecha),
            correo = FirestoreStringValue(correo),
            password = FirestoreStringValue(password),
            rol = FirestoreStringValue(rol)
        )
    )

    @Test fun editarPerfilConservaContrasenaYRol() {
        val doc = documento()
        val servicio = ServicioPerfilPrueba(mutableListOf(doc))
        val vm = UsuarioViewModel(servicio, Dispatchers.Unconfined)
        vm.iniciarSesion("ana@example.com", "clave", {})
        assertNotNull(vm.usuarioActual)

        var exito = false
        vm.actualizarPerfil(
            nombre = "Ana María",
            apellido = "Pérez",
            telefono = "987654321",
            fechaNacimiento = "02/02/1998",
            correo = "ana.nueva@example.com",
            onExito = { exito = true },
            onError = { fail(it) }
        )

        assertTrue(exito)
        val enviado = servicio.actualizado
        assertNotNull(enviado)
        assertEquals("clave", enviado!!.fields.password?.stringValue)
        assertEquals("ADMIN", enviado.fields.rol?.stringValue)
        assertEquals("ana.nueva@example.com", enviado.fields.correo?.stringValue)
        assertEquals("Ana María", vm.usuarioActual?.nombre)
        assertEquals("987654321", vm.usuarioActual?.telefono)
        assertTrue(vm.esAdmin)
    }

    @Test fun editarPerfilRechazaCorreoDeOtroUsuario() {
        val servicio = ServicioPerfilPrueba(
            mutableListOf(
                documento(id = "A"),
                documento(id = "B", correo = "beto@example.com", rol = "CLIENTE")
            )
        )
        val vm = UsuarioViewModel(servicio, Dispatchers.Unconfined)
        vm.iniciarSesion("ana@example.com", "clave", {})

        var mensaje: String? = null
        vm.actualizarPerfil(
            nombre = "Ana",
            apellido = "Pérez",
            telefono = "912345678",
            fechaNacimiento = "01/01/2000",
            correo = "beto@example.com",
            onExito = { fail("No debe aceptar un correo duplicado") },
            onError = { mensaje = it }
        )

        assertEquals("Ya existe una cuenta con ese correo.", mensaje)
        assertNull(servicio.actualizado)
    }

    @Test fun editarPerfilNoCambiaNadaSiEsInvalido() {
        val servicio = ServicioPerfilPrueba(mutableListOf(documento()))
        val vm = UsuarioViewModel(servicio, Dispatchers.Unconfined)
        vm.iniciarSesion("ana@example.com", "clave", {})

        var mensaje: String? = null
        vm.actualizarPerfil(
            nombre = "Ana",
            apellido = "Pérez",
            telefono = "123",
            fechaNacimiento = "01/01/2000",
            correo = "ana@example.com",
            onExito = { fail("Teléfono inválido") },
            onError = { mensaje = it }
        )

        assertNotNull(mensaje)
        assertNull(servicio.actualizado)
    }
}
