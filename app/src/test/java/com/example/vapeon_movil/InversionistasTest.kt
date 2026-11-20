package com.example.vapeon_movil

import com.example.vapeon_movil.entities.Inversionista
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.services.InversionistaService
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.viewmodels.InversionistasViewModel
import com.example.vapeon_movil.viewmodels.validarCamposInversionista
import com.example.vapeon_movil.viewmodels.validarInversionista
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class InversionistasTest {
    private val admin = Usuario(id = "admin", rol = "ADMIN")
    private fun modelo(id: String = "uno", documento: String = "12345678") = Inversionista(
        id, "Persona de prueba", "DNI", documento, fechaRegistro = "2026-11-19T15:00:00Z"
    )
    private fun vm(servicio: Servicio) = InversionistasViewModel(servicio, Dispatchers.Unconfined).also {
        it.actualizarSesion(admin)
    }

    @Test fun validaDocumentoObligatorioYContactosOpcionales() {
        assertNull(validarInversionista(modelo()))
        assertNotNull(validarInversionista(modelo().copy(documento = "")))
        assertNotNull(validarInversionista(modelo().copy(documento = "123")))
        assertNotNull(validarInversionista(modelo().copy(correo = "incorrecto")))
        assertNotNull(validarInversionista(modelo().copy(telefono = "123")))
        assertNull(validarInversionista(modelo().copy(tipoDocumento = "RUC", documento = "20131312955")))
        assertNotNull(validarInversionista(modelo().copy(tipoDocumento = "RUC", documento = "20131312954")))
    }

    @Test fun validacionPorCampoDetectaNombreTelefonoCorreoYObservaciones() {
        val errores = validarCamposInversionista(modelo().copy(
            nombre = "A", telefono = "123", correo = "correo incorrecto",
            observaciones = "x".repeat(2001)
        ))
        assertNotNull(errores.nombre)
        assertNotNull(errores.telefono)
        assertNotNull(errores.correo)
        assertNotNull(errores.observaciones)
        assertEquals(errores.nombre, validarInversionista(modelo().copy(nombre = "A",
            telefono = "123", correo = "correo incorrecto", observaciones = "x".repeat(2001))))
        assertTrue(errores.hayErrores())
    }

    @Test fun formularioInvalidoNoEscribeEnFirestore() {
        val servicio = Servicio()
        val vm = vm(servicio)
        vm.iniciarFormulario()
        vm.cambiarFormulario(vm.formulario!!.copy(nombre = "", documento = "123"))
        vm.guardar(admin)
        assertNotNull(vm.error)
        assertEquals(0, servicio.creaciones)
        assertTrue(servicio.documentos.isEmpty())
    }

    @Test fun creaConsultaEditaYEliminaElRegistroConVersion() {
        val servicio = Servicio()
        val vm = vm(servicio)
        vm.iniciarFormulario()
        vm.cambiarFormulario(vm.formulario!!.copy(nombre = "Ana", documento = "12345678"))
        vm.guardar(admin)
        assertNull(vm.error)
        assertEquals(1, vm.lista.size)
        assertNull(vm.formulario)
        vm.ver(vm.lista.single())
        assertEquals("Ana", vm.seleccionado?.nombre)
        vm.iniciarFormulario(vm.lista.single())
        vm.cambiarFormulario(vm.formulario!!.copy(nombre = "Ana Actualizada", correo = "ANA@example.test"))
        vm.guardar(admin)
        assertNull(vm.error)
        assertEquals("Ana Actualizada", vm.lista.single().nombre)
        assertEquals("ana@example.test", vm.lista.single().correo)
        assertEquals(1, servicio.actualizaciones)
        vm.eliminar(admin, vm.lista.single())
        assertNull(vm.error)
        assertTrue(vm.lista.isEmpty())
        assertTrue(servicio.documentos.isEmpty())
    }

    @Test fun cargaTodasLasPaginasYRechazaDocumentoDuplicado() {
        val servicio = Servicio()
        servicio.sembrar(modelo())
        servicio.sembrar(modelo("dos", "87654321"))
        val vm = vm(servicio)
        vm.cargar(admin)
        assertEquals(2, vm.lista.size)
        assertEquals(2, servicio.consultas)
        vm.iniciarFormulario()
        vm.cambiarFormulario(vm.formulario!!.copy(nombre = "Otra persona", documento = "12345678"))
        vm.guardar(admin)
        assertNotNull(vm.error)
        assertEquals(0, servicio.creaciones)
    }

    @Test fun respuestaPerdidaAlCrearReintentaElMismoId() {
        val servicio = Servicio().also { it.perderRespuesta = true }
        val vm = vm(servicio)
        vm.iniciarFormulario()
        vm.cambiarFormulario(vm.formulario!!.copy(nombre = "Ana", documento = "12345678"))
        val id = vm.formulario!!.id
        vm.guardar(admin)
        assertNotNull(vm.error)
        assertEquals(setOf(id), servicio.documentos.keys)
        vm.cancelarFormulario()
        vm.iniciarFormulario()
        assertEquals(id, vm.formulario!!.id)
        vm.guardar(admin)
        assertNull(vm.error)
        assertEquals(1, servicio.documentos.size)
        assertEquals(id, vm.lista.single().id)
    }

    @Test fun edicionConcurrenteNoSobrescribeLaVersionRemota() {
        val servicio = Servicio()
        servicio.sembrar(modelo())
        val vm = vm(servicio)
        vm.cargar(admin)
        vm.iniciarFormulario(vm.lista.single())
        vm.cambiarFormulario(vm.formulario!!.copy(nombre = "Cambio local"))
        servicio.sembrar(modelo().copy(nombre = "Cambio de otro administrador"))
        vm.guardar(admin)
        assertNotNull(vm.error)
        assertEquals("Cambio de otro administrador", servicio.documentos.getValue("uno").aInversionista().nombre)
        assertEquals(0, servicio.actualizaciones)
    }

    @Test fun clienteNoPuedeConsultarNiGuardar() {
        val servicio = Servicio()
        val vm = vm(servicio)
        val cliente = Usuario(id = "cliente", rol = "CLIENTE")
        vm.actualizarSesion(cliente)
        vm.cargar(cliente)
        vm.iniciarFormulario()
        vm.cambiarFormulario(vm.formulario!!.copy(nombre = "Ana", documento = "12345678"))
        vm.guardar(cliente)
        assertEquals(0, servicio.consultas)
        assertEquals(0, servicio.creaciones)
        assertNotNull(vm.error)
        assertEquals("home", pantallaRestauradaSegura("inversionistas", cliente, claveSesionNavegacion(cliente)))
        assertEquals("inversionistas", pantallaRestauradaSegura("inversionistas", admin, claveSesionNavegacion(admin)))
    }

    @Test fun cambiarSesionCancelaLaCargaYBorraLosDatos() {
        val servicio = Servicio().also { it.paginaPendiente = CompletableDeferred() }
        val vm = vm(servicio)
        vm.cargar(admin)
        assertTrue(vm.cargando)
        vm.actualizarSesion(null)
        servicio.paginaPendiente!!.complete(InversionistaPagina(listOf(
            InversionistaDocumento("inversionistas/uno", modelo().aFirestoreInversionista().fields, "version1")
        )))
        assertFalse(vm.cargando)
        assertTrue(vm.lista.isEmpty())
    }

    private class Servicio : InversionistaService {
        val documentos = linkedMapOf<String, InversionistaDocumento>()
        var creaciones = 0
        var actualizaciones = 0
        var consultas = 0
        var perderRespuesta = false
        var paginaPendiente: CompletableDeferred<InversionistaPagina>? = null
        private var version = 0
        private fun fallo(codigo: Int): Nothing = throw HttpException(
            Response.error<Any>(codigo, ResponseBody.create(null, "{}")))
        fun sembrar(datos: Inversionista) {
            documentos[datos.id] = InversionistaDocumento("inversionistas/${datos.id}",
                datos.aFirestoreInversionista().fields, "version${++version}")
        }
        override suspend fun listar(pageToken: String?): InversionistaPagina {
            consultas++
            paginaPendiente?.let { return it.await() }
            val posicion = pageToken?.toInt() ?: 0
            return InversionistaPagina(documentos.values.drop(posicion).take(1),
                (posicion + 1).takeIf { it < documentos.size }?.toString())
        }
        override suspend fun mostrar(id: String) = documentos[id] ?: fallo(404)
        override suspend fun crear(body: InversionistaContainer, id: String): InversionistaDocumento {
            if (id in documentos) fallo(409)
            creaciones++
            val nuevo = InversionistaDocumento("inversionistas/$id", body.fields, "version${++version}")
            documentos[id] = nuevo
            if (perderRespuesta) { perderRespuesta = false; throw IOException("Respuesta perdida") }
            return nuevo
        }
        override suspend fun actualizar(id: String, body: InversionistaContainer, version: String,
            campos: List<String>): InversionistaDocumento {
            val anterior = mostrar(id)
            if (anterior.updateTime != version) fallo(412)
            actualizaciones++
            val nuevo = anterior.copy(fields = anterior.fields + body.fields.filterKeys { it in campos },
                updateTime = "version${++this.version}")
            documentos[id] = nuevo
            return nuevo
        }
        override suspend fun eliminar(id: String, version: String): Any {
            if (mostrar(id).updateTime != version) fallo(412)
            documentos.remove(id)
            return Unit
        }
    }
}
