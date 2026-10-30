package com.example.vapeon_movil

import com.example.vapeon_movil.entities.*
import com.example.vapeon_movil.services.TarjetaService
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.viewmodels.TarjetaViewModel
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.time.YearMonth

class TarjetaTest {
    private val hoy = YearMonth.of(2026, 10)
    private val cliente = Usuario(id = "A", rol = "CLIENTE")
    private fun tarjeta(id: String = "t1", usuario: String = "A", mes: Int = 10, anio: Int = 2026) =
        Tarjeta(id, usuario, "Ana Pérez", "VISA", "0042", mes, anio, "2026-10-30T12:00:00Z")
    private fun documento(tarjeta: Tarjeta) = TarjetaDocumento("tarjetas/${tarjeta.id}", tarjeta.aFirestore().fields)
    private fun error(titular: String = "Ana", marca: String = "VISA", ultimos: String = "0042",
        mes: String = "10", anio: String = "2026") = validarRegistroTarjeta(titular, marca, ultimos, mes, anio, hoy)

    private class Servicio : TarjetaService {
        val documentos = linkedMapOf<String, TarjetaDocumento>()
        var altas = 0
        var lecturas = 0
        var consultas = 0
        var ultimaConsulta: TarjetaConsulta? = null
        var esperaRegistro: CompletableDeferred<Unit>? = null
        var esperaSeleccion: CompletableDeferred<Unit>? = null
        var perderRespuesta = false
        var fallarLista = false
        override suspend fun crearTarjeta(body: TarjetaContainer, documentId: String): TarjetaDocumento {
            altas++
            esperaRegistro?.await()
            if (documentos.containsKey(documentId)) throw HttpException(Response.error<Any>(409, ResponseBody.create(null, "")))
            val documento = TarjetaDocumento("tarjetas/$documentId", body.fields)
            documentos[documentId] = documento
            if (perderRespuesta) { perderRespuesta = false; throw java.io.IOException("Respuesta perdida") }
            return documento
        }
        override suspend fun consultarTarjetas(body: TarjetaConsulta): List<TarjetaConsultaRespuesta> {
            consultas++
            ultimaConsulta = body
            if (fallarLista) throw java.io.IOException("Sin conexión")
            // Intencionalmente incluye tarjetas ajenas para comprobar la defensa del ViewModel.
            return documentos.values.map { TarjetaConsultaRespuesta(it) }
        }
        override suspend fun mostrarTarjeta(id: String): TarjetaDocumento {
            lecturas++
            esperaSeleccion?.await()
            return documentos.getValue(id)
        }
    }
    private fun vm(servicio: Servicio = Servicio()) = TarjetaViewModel(servicio, Dispatchers.Unconfined) { hoy }

    @Test fun titularVacioRechazado() { assertNotNull(error(titular = " ")) }
    @Test fun marcaVaciaRechazada() { assertNotNull(error(marca = "")) }
    @Test fun ultimosVaciosRechazados() { assertNotNull(error(ultimos = "")) }
    @Test fun tresDigitosRechazados() { assertNotNull(error(ultimos = "123")) }
    @Test fun cincoDigitosRechazados() { assertNotNull(error(ultimos = "12345")) }
    @Test fun letrasEnUltimosRechazadas() { assertNotNull(error(ultimos = "12a4")) }
    @Test fun mesCeroRechazado() { assertNotNull(error(mes = "0")) }
    @Test fun mesTreceRechazado() { assertNotNull(error(mes = "13")) }
    @Test fun tarjetaVencidaRechazada() { assertNotNull(error(mes = "9")) }
    @Test fun tarjetaValidaAceptada() { assertNull(error(anio = "2027")) }
    @Test fun venceEsteMesAceptadaHastaFinDeMes() {
        assertNull(error())
        assertFalse(tarjeta().estaVencida(hoy))
        assertTrue(tarjeta().estaVencida(hoy.plusMonths(1)))
    }
    @Test fun anioPasadoRechazado() { assertNotNull(error(anio = "2025")) }
    @Test fun aniosInvalidosRechazados() {
        listOf("", "26", "0000", "10000", "202x", "NaN", "2026.0").forEach { assertNotNull(error(anio = it)) }
    }
    @Test fun mesVacioOLetrasRechazados() {
        listOf("", "x", "1.0", "-1").forEach { assertNotNull(error(mes = it)) }
    }
    @Test fun noAceptaNumeroCompletoNiEspaciosEnUltimos() {
        listOf("1234567890123456", " 1234", "12 4").forEach { assertNotNull(error(ultimos = it)) }
    }
    @Test fun conversionCorrectaAFirestore() {
        val json = Gson().toJsonTree(tarjeta().aFirestore()).asJsonObject.getAsJsonObject("fields")
        assertEquals("A", json.getAsJsonObject("usuarioId").get("stringValue").asString)
        assertEquals("0042", json.getAsJsonObject("ultimos4").get("stringValue").asString)
        assertEquals("10", json.getAsJsonObject("vencimientoMes").get("integerValue").asString)
        assertEquals("2026", json.getAsJsonObject("vencimientoAnio").get("integerValue").asString)
        assertEquals("2026-10-30T12:00:00Z", json.getAsJsonObject("fechaCreacion").get("timestampValue").asString)
        assertEquals("SIMULADO", json.getAsJsonObject("modalidad").get("stringValue").asString)
    }
    @Test fun lecturaCorrectaDesdeFirestore() {
        val original = documento(tarjeta())
        val leido = Gson().fromJson(Gson().toJson(original), TarjetaDocumento::class.java)
        assertEquals(tarjeta(), leido.aTarjetaDelUsuario("A"))
    }
    @Test fun tarjetaAjenaRechazadaAntesDeInterpretarDatos() {
        val ajena = TarjetaDocumento("tarjetas/t1", mapOf("usuarioId" to FirestoreValor(stringValue = "B")))
        assertThrows(IllegalArgumentException::class.java) { ajena.aTarjetaDelUsuario("A") }
    }
    @Test fun listadoSoloDevuelveTarjetasPropias() {
        val servicio = Servicio().apply {
            documentos["t1"] = documento(tarjeta())
            documentos["t2"] = documento(tarjeta("t2", "B"))
        }
        val vm = vm(servicio)
        vm.cargarTarjetas(cliente)
        assertEquals(listOf("t1"), vm.tarjetas.map { it.id })
        assertEquals("usuarioId", servicio.ultimaConsulta!!.structuredQuery.where.fieldFilter.field.fieldPath)
        assertEquals("A", servicio.ultimaConsulta!!.structuredQuery.where.fieldFilter.value.stringValue)
        assertEquals("EQUAL", servicio.ultimaConsulta!!.structuredQuery.where.fieldFilter.op)
    }
    @Test fun serializacionSoloContieneLosNueveCamposAprobados() {
        val json = Gson().toJsonTree(tarjeta().aFirestore()).asJsonObject.getAsJsonObject("fields")
        assertEquals(setOf("id", "usuarioId", "titular", "marca", "ultimos4", "vencimientoMes",
            "vencimientoAnio", "fechaCreacion", "modalidad"), json.keySet())
        assertFalse(json.has("numeroCompleto"))
        assertFalse(json.has("numeroTarjeta"))
    }
    @Test fun modeloNoTieneCvvPinNiCodigoDeSeguridad() {
        val campos = Tarjeta::class.java.declaredFields.map { it.name.lowercase() }
        assertFalse(campos.any { it in listOf("cvv", "cvc", "pin", "codigoseguridad", "numerocompleto", "numerotarjeta") })
    }
    @Test fun cambiarUsuarioLimpiaSeleccionYListado() {
        val servicio = Servicio().apply { documentos["t1"] = documento(tarjeta()) }
        val vm = vm(servicio)
        vm.cargarTarjetas(cliente)
        vm.seleccionarTarjeta(cliente, "t1")
        assertNotNull(vm.tarjetaSeleccionada)
        vm.actualizarUsuario(Usuario(id = "B"))
        assertNull(vm.tarjetaSeleccionada)
        assertTrue(vm.tarjetas.isEmpty())
    }
    @Test fun seleccionHaceLecturaFrescaYRechazaPropietarioCambiado() {
        val servicio = Servicio().apply { documentos["t1"] = documento(tarjeta()) }
        val vm = vm(servicio)
        vm.cargarTarjetas(cliente)
        servicio.documentos["t1"] = documento(tarjeta(usuario = "B"))
        vm.seleccionarTarjeta(cliente, "t1")
        assertEquals(1, servicio.lecturas)
        assertNull(vm.tarjetaSeleccionada)
        assertNotNull(vm.error)
    }
    @Test fun seleccionRechazaTarjetaVencidaAunqueEsteEnLista() {
        val servicio = Servicio().apply { documentos["t1"] = documento(tarjeta(mes = 9)) }
        val vm = vm(servicio)
        vm.cargarTarjetas(cliente)
        assertEquals(1, vm.tarjetas.size)
        vm.seleccionarTarjeta(cliente, "t1")
        assertNull(vm.tarjetaSeleccionada)
        assertNotNull(vm.error)
    }
    @Test fun seleccionAceptaTarjetaPropiaQueVenceEsteMes() {
        val servicio = Servicio().apply { documentos["t1"] = documento(tarjeta()) }
        val vm = vm(servicio)
        vm.seleccionarTarjeta(cliente, "t1")
        assertEquals(tarjeta(), vm.tarjetaSeleccionada)
    }
    @Test fun sinSesionOAdminNoPuedeListarRegistrarNiSeleccionar() {
        val servicio = Servicio()
        val vm = vm(servicio)
        listOf(null, Usuario(id = "admin", rol = "ADMIN"), Usuario(id = "")).forEach { usuario ->
            vm.cargarTarjetas(usuario)
            vm.registrarTarjeta(usuario, "Ana", "VISA", "1234", "10", "2026")
            vm.seleccionarTarjeta(usuario, "t1")
        }
        assertEquals(0, servicio.altas + servicio.consultas + servicio.lecturas)
        assertNull(vm.tarjetaSeleccionada)
    }
    @Test fun registroUsaElUsuarioActualYValidaAntesDeRed() {
        val servicio = Servicio()
        val vm = vm(servicio)
        vm.registrarTarjeta(cliente, "", "VISA", "1234", "10", "2026")
        assertEquals(0, servicio.altas)
        vm.registrarTarjeta(cliente, " Ana ", " VISA ", "0042", "10", "2026")
        assertNull(vm.error)
        val registrada = vm.tarjetas.single()
        assertEquals("A", registrada.usuarioId)
        assertEquals("Ana", registrada.titular)
        assertEquals(registrada.id, servicio.documentos.keys.single())
    }
    @Test fun doblePulsacionNoDuplicaRegistro() {
        val servicio = Servicio().apply { esperaRegistro = CompletableDeferred() }
        val vm = vm(servicio)
        repeat(2) { vm.registrarTarjeta(cliente, "Ana", "VISA", "0042", "10", "2026") }
        assertEquals(1, servicio.altas)
        assertTrue(vm.registrando)
        servicio.esperaRegistro!!.complete(Unit)
        assertEquals(1, vm.tarjetas.size)
        assertFalse(vm.registrando)
    }
    @Test fun respuestaPerdidaReutilizaIdSinDuplicarTarjeta() {
        val servicio = Servicio().apply { perderRespuesta = true }
        val vm = vm(servicio)
        vm.registrarTarjeta(cliente, "Ana", "VISA", "0042", "10", "2026")
        val id = vm.registroPendiente!!.id
        assertNotNull(vm.error)
        vm.registrarTarjeta(cliente, "Ana", "VISA", "0042", "10", "2026")
        assertNull(vm.error)
        assertNull(vm.registroPendiente)
        assertEquals(listOf(id), servicio.documentos.keys.toList())
        assertEquals(id, vm.tarjetas.single().id)
    }
    @Test fun respuestaPendienteNoRestauraSeleccionDeOtraSesion() {
        val servicio = Servicio().apply {
            documentos["t1"] = documento(tarjeta())
            esperaSeleccion = CompletableDeferred()
        }
        val vm = vm(servicio)
        vm.seleccionarTarjeta(cliente, "t1")
        vm.actualizarUsuario(Usuario(id = "B"))
        servicio.esperaSeleccion!!.complete(Unit)
        assertNull(vm.tarjetaSeleccionada)
        assertFalse(vm.seleccionando)
    }
    @Test fun logoutLimpiaSeleccionRegistroPendienteYMensajes() {
        val servicio = Servicio().apply { perderRespuesta = true }
        val vm = vm(servicio)
        vm.registrarTarjeta(cliente, "Ana", "VISA", "0042", "10", "2026")
        vm.actualizarUsuario(null)
        assertNull(vm.registroPendiente)
        assertNull(vm.tarjetaSeleccionada)
        assertNull(vm.error)
        assertNull(vm.mensaje)
    }
    @Test fun errorDeCargaPermiteReintentar() {
        val servicio = Servicio().apply { fallarLista = true }
        val vm = vm(servicio)
        vm.cargarTarjetas(cliente)
        assertNotNull(vm.error)
        assertFalse(vm.cargando)
        servicio.fallarLista = false
        vm.cargarTarjetas(cliente)
        assertNull(vm.error)
        assertTrue(vm.tarjetas.isEmpty())
    }
    @Test fun documentoConIdInconsistenteEsRechazado() {
        assertThrows(IllegalArgumentException::class.java) {
            documento(tarjeta()).copy(name = "tarjetas/otro").aTarjetaDelUsuario("A")
        }
    }
    @Test fun tarjetaMalformadaPropiaSeInformaSinExponerAjena() {
        val servicio = Servicio().apply {
            documentos["t1"] = documento(tarjeta())
            documentos["mal"] = TarjetaDocumento("tarjetas/mal", mapOf("usuarioId" to FirestoreValor(stringValue = "A")))
            documentos["ajena"] = TarjetaDocumento("tarjetas/ajena", mapOf("usuarioId" to FirestoreValor(stringValue = "B")))
        }
        val vm = vm(servicio)
        vm.cargarTarjetas(cliente)
        assertEquals(1, vm.tarjetas.size)
        assertTrue(vm.error!!.contains("1 tarjetas"))
    }
    @Test fun rutaTarjetasProtegidaPorSesionYRol() {
        assertEquals("tarjetas", pantallaRestauradaSegura("tarjetas", cliente, "A:CLIENTE"))
        assertEquals("admin", pantallaRestauradaSegura("tarjetas", Usuario(id = "admin", rol = "ADMIN"), "admin:ADMIN"))
        assertEquals("login", pantallaRestauradaSegura("tarjetas", null, "A:CLIENTE"))
        assertEquals("home", pantallaRestauradaSegura("tarjetas", cliente, "B:CLIENTE"))
    }
}
