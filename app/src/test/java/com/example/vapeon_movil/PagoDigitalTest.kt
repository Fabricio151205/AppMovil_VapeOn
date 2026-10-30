package com.example.vapeon_movil

import com.example.vapeon_movil.entities.*
import com.example.vapeon_movil.services.PagoService
import com.example.vapeon_movil.services.PedidoService
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.viewmodels.*
import kotlinx.coroutines.*
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

private fun falloHttp(codigo: Int) = HttpException(Response.error<Any>(codigo, ResponseBody.create(null, "")))

private class PagosPrueba : PagoService {
    val documentos = mutableMapOf<String, PagoDocumento>()
    var altas = 0
    var resultados = 0
    var perderAlta = false
    var perderResultado = false
    var espera: CompletableDeferred<Unit>? = null
    var version = 0
    override suspend fun crearPago(body: PagoContainer, documentId: String): PagoDocumento {
        altas++
        espera?.await()
        if (documentos.containsKey(documentId)) throw falloHttp(409)
        val doc = PagoDocumento("projects/prueba/databases/(default)/documents/pagos/$documentId", body.fields, "v${++version}")
        documentos[documentId] = doc
        if (perderAlta) { perderAlta = false; throw java.io.IOException("Respuesta perdida") }
        return doc
    }
    override suspend fun mostrarPago(id: String) = documentos[id] ?: throw falloHttp(404)
    override suspend fun actualizarResultado(id: String, body: PagoContainer, updateTime: String,
        campos: List<String>): PagoDocumento {
        resultados++
        val actual = documentos.getValue(id)
        if (actual.updateTime != updateTime) throw falloHttp(412)
        assertEquals(listOf("estadoPago", "fechaResultado"), campos)
        val nuevo = actual.copy(fields = actual.fields + body.fields, updateTime = "v${++version}")
        documentos[id] = nuevo
        if (perderResultado) { perderResultado = false; throw java.io.IOException("Respuesta perdida") }
        return nuevo
    }
}

private fun productoCompra(stock: Int = 5, precio: Double = 10.0) = ProductoDocumento(
    "projects/prueba/databases/(default)/documents/productos/p1", ProductoCampos(
        precio = FirestoreValue(doubleValue = precio), stock = FirestoreValue(integerValue = stock.toString())
    ), "version-producto")

class PagoDigitalTest {
    @Test fun crearPendienteYReintentarUtilizaUnSoloDocumento() = runBlocking {
        val servicio = PagosPrueba()
        repeat(2) { obtenerOCrearPago(servicio, intentoDigitalPrueba()) }
        assertEquals(1, servicio.altas)
        assertEquals("PENDIENTE", servicio.documentos.getValue("pago1").aPagoDelUsuario("A").estadoPago)
    }
    @Test fun respuestaPerdidaDelAltaSeCompruebaSinNuevoPost() = runBlocking {
        val servicio = PagosPrueba().apply { perderAlta = true }
        try { obtenerOCrearPago(servicio, intentoDigitalPrueba()); fail() } catch (_: java.io.IOException) { }
        val recuperado = obtenerOCrearPago(servicio, intentoDigitalPrueba())
        assertEquals("SIM-YAPE-pago1", recuperado.aPagoDelUsuario("A").numeroOperacion)
        assertEquals(1, servicio.altas)
    }
    @Test fun confirmadoSePersisteYNoSeVuelveAConfirmar() = runBlocking {
        val servicio = PagosPrueba()
        repeat(2) { resolverResultadoPago(servicio, intentoDigitalPrueba(), "CONFIRMADO") }
        assertEquals(1, servicio.resultados)
        assertEquals("CONFIRMADO", servicio.documentos.getValue("pago1").aPagoDelUsuario("A").estadoPago)
    }
    @Test fun rechazoEsTerminalNoSeConvierteEnAprobacion() = runBlocking {
        val servicio = PagosPrueba()
        resolverResultadoPago(servicio, intentoDigitalPrueba(), "RECHAZADO")
        val doc = resolverResultadoPago(servicio, intentoDigitalPrueba(), "CONFIRMADO")
        assertEquals("RECHAZADO", doc.aPagoDelUsuario("A").estadoPago)
        assertEquals(1, servicio.resultados)
    }
    @Test fun respuestaPerdidaDeConfirmacionNoRepiteResultado() = runBlocking {
        val servicio = PagosPrueba().apply { perderResultado = true }
        try { resolverResultadoPago(servicio, intentoDigitalPrueba(), "CONFIRMADO"); fail() }
        catch (_: java.io.IOException) { }
        val doc = resolverResultadoPago(servicio, intentoDigitalPrueba(), "CONFIRMADO")
        assertEquals("CONFIRMADO", doc.aPagoDelUsuario("A").estadoPago)
        assertEquals(1, servicio.resultados)
    }
    @Test fun doblePulsacionConBloqueoNoIniciaDosAltas() = runBlocking {
        val servicio = PagosPrueba().apply { espera = CompletableDeferred() }
        val memoria = mutableMapOf<String, String>()
        val recuperacion = RecuperacionPago({ memoria[it] }, { key, value -> memoria[key] = value; true }, { true })
        recuperacion.actualizarUsuario("A")
        recuperacion.guardar(intentoDigitalPrueba())
        val trabajos = (1..2).map {
            launch(start = CoroutineStart.UNDISPATCHED) {
                val token = recuperacion.iniciarOperacion() ?: return@launch
                try { obtenerOCrearPago(servicio, recuperacion.intento!!) }
                finally { recuperacion.finalizarOperacion(token) }
            }
        }
        assertEquals(1, servicio.altas)
        servicio.espera!!.complete(Unit)
        trabajos.joinAll()
        assertEquals(1, servicio.documentos.size)
    }
    @Test fun pendienteYRechazadoNoPreparanNingunaEscritura() {
        listOf("PENDIENTE", "RECHAZADO").forEach { estado ->
            assertThrows(IllegalArgumentException::class.java) {
                prepararCommitDigital(documentoPago(pagoPrueba(estado)), pedidoDigitalPrueba(), listOf(productoCompra()),
                    intentoDigitalPrueba().ruta)
            }
        }
    }
    @Test fun confirmadoPreparaUnCommitConPedidoStockYPago() {
        val doc = documentoPago(pagoPrueba("CONFIRMADO"))
        val commit = prepararCommitDigital(doc, pedidoDigitalPrueba(), listOf(productoCompra()), intentoDigitalPrueba().ruta)
        assertEquals(3, commit.writes.size)
        assertEquals(false, commit.writes.first().currentDocument.exists)
        assertEquals("version-producto", commit.writes[1].currentDocument.updateTime)
        assertEquals("4", commit.writes[1].update.fields["stock"]?.integerValue)
        assertEquals(doc.updateTime, commit.writes.last().currentDocument.updateTime)
        assertEquals(listOf("estadoRegistroPedido"), commit.writes.last().updateMask?.fieldPaths)
    }
    @Test fun confirmadoConStockInsuficienteConservaPago() {
        val pago = documentoPago(pagoPrueba("CONFIRMADO"))
        assertThrows(IllegalArgumentException::class.java) {
            prepararCommitDigital(pago, pedidoDigitalPrueba(), listOf(productoCompra(stock = 0)), intentoDigitalPrueba().ruta)
        }
        assertEquals("CONFIRMADO", pago.aPagoDelUsuario("A").estadoPago)
        assertEquals("PENDIENTE", pago.aPagoDelUsuario("A").estadoRegistroPedido)
    }
    @Test fun confirmadoConCambioDePrecioNoCambiaImporteNiOperacion() {
        val pago = documentoPago(pagoPrueba("CONFIRMADO"))
        assertThrows(IllegalArgumentException::class.java) {
            prepararCommitDigital(pago, pedidoDigitalPrueba(), listOf(productoCompra(precio = 11.0)), intentoDigitalPrueba().ruta)
        }
        assertEquals(pagoPrueba("CONFIRMADO"), pago.aPagoDelUsuario("A"))
    }
    @Test fun unPagoNoPuedeVincularseAOtroPedido() {
        assertThrows(IllegalArgumentException::class.java) {
            prepararCommitDigital(documentoPago(pagoPrueba("CONFIRMADO")), pedidoDigitalPrueba().copy(id = "pedido2"),
                listOf(productoCompra()), intentoDigitalPrueba().ruta)
        }
    }
    @Test fun pagoResueltoDesaparecidoNoSeRecrea() = runBlocking {
        val servicio = PagosPrueba()
        try { obtenerOCrearPago(servicio, intentoDigitalPrueba("CONFIRMADO")); fail() }
        catch (_: IllegalArgumentException) { }
        assertEquals(0, servicio.altas)
    }
    @Test fun respuestaDeOtroUsuarioNoSeUtiliza() = runBlocking {
        val servicio = PagosPrueba().apply { documentos["pago1"] = documentoPago(pagoPrueba(usuario = "B")) }
        try { obtenerOCrearPago(servicio, intentoDigitalPrueba()); fail() } catch (_: IllegalArgumentException) { }
        assertEquals(0, servicio.altas)
        assertEquals(0, servicio.resultados)
    }
    @Test fun tarjetaAjenaOVencidaNoSirveParaIniciarPago() {
        val tarjeta = Tarjeta("t1", "B", "Titular", "Marca", "1234", 1, 2020, "2026-10-30T12:00:00Z")
        assertThrows(IllegalArgumentException::class.java) {
            validarTarjetaParaPago(TarjetaDocumento("tarjetas/t1", tarjeta.aFirestore().fields), "A", "t1")
        }
        val propiaVencida = tarjeta.copy(usuarioId = "A")
        assertThrows(IllegalArgumentException::class.java) {
            validarTarjetaParaPago(TarjetaDocumento("tarjetas/t1", propiaVencida.aFirestore().fields), "A", "t1")
        }
    }
    @Test fun fechaVersionFaltanteBloqueaCommit() {
        assertThrows(IllegalArgumentException::class.java) {
            prepararCommitDigital(documentoPago(pagoPrueba("CONFIRMADO")).copy(updateTime = null), pedidoDigitalPrueba(),
                listOf(productoCompra()), intentoDigitalPrueba().ruta)
        }
    }
    @Test fun tarjetaPropiaVigenteSeValidaParaPagoSinCredenciales() {
        val tarjeta = Tarjeta("t1", "A", "Titular", "Marca", "1234", 12, 9999, "2026-10-30T12:00:00Z")
        assertEquals(tarjeta, validarTarjetaParaPago(TarjetaDocumento("tarjetas/t1", tarjeta.aFirestore().fields), "A", "t1"))
    }

    @Test fun respuestaPerdidaDelCommitSeRecuperaSinRepetirPagoNiStock() = runBlocking {
        val pagoService = PagosPrueba()
        val intento = intentoDigitalPrueba()
        val confirmado = resolverResultadoPago(pagoService, intento, "CONFIRMADO")
        val pedido = pedidoDigitalPrueba()
        var stock = 5
        var commits = 0
        var pedidoGuardado: PedidoDocumento? = null
        var perderRespuesta = true
        val pedidos = object : PedidoService {
            override suspend fun consultarPedidos(body: PedidoConsulta) = emptyList<PedidoConsultaRespuesta>()
            override suspend fun mostrarPedido(id: String) = pedidoGuardado ?: throw falloHttp(404)
            override suspend fun actualizarEstadoPedido(id: String, body: PedidoEstadoActualizacion): PedidoDocumento = error("No se utiliza")
            override suspend fun confirmarPedido(body: PedidoCommit): PedidoCommitRespuesta {
                commits++
                // Comprobar TODAS las precondiciones antes de aplicar cualquier escritura.
                if (pedidoGuardado != null) throw falloHttp(409)
                val docPago = pagoService.documentos.getValue("pago1")
                if (docPago.updateTime != body.writes.last().currentDocument.updateTime) throw falloHttp(412)
                assertEquals(false, body.writes.first().currentDocument.exists)
                assertEquals("version-producto", body.writes[1].currentDocument.updateTime)
                pedidoGuardado = body.writes.first().update
                stock = body.writes[1].update.fields.getValue("stock").integerValue!!.toInt()
                pagoService.documentos["pago1"] = docPago.copy(fields = docPago.fields + body.writes.last().update.fields,
                    updateTime = "version-consumida")
                if (perderRespuesta) { perderRespuesta = false; throw java.io.IOException("Respuesta perdida") }
                return PedidoCommitRespuesta("2026-10-30T12:02:00Z")
            }
        }
        val commit = prepararCommitDigital(confirmado, pedido, listOf(productoCompra()), intento.ruta)
        try { pedidos.confirmarPedido(commit); fail() } catch (_: java.io.IOException) { }
        val recuperado = obtenerOCrearPago(pagoService, intento).aPagoDelUsuario("A")
        val real = recuperarPedidoDigitalRegistrado(pedidos, intento.copy(pago = recuperado))
        assertEquals(pedido.id, real.id)
        assertEquals(4, stock)
        assertEquals(1, commits)
        assertEquals(1, pagoService.altas)
        assertEquals(1, pagoService.resultados)
        // Un segundo consumo se rechaza antes de preparar o enviar otro commit.
        assertThrows(IllegalArgumentException::class.java) {
            prepararCommitDigital(pagoService.documentos.getValue("pago1"), pedido, listOf(productoCompra()), intento.ruta)
        }
        assertEquals(4, stock)
    }
    @Test fun recuperacionNoAceptaPedidoAjenoOModificado() = runBlocking {
        val registrado = pagoPrueba("CONFIRMADO").copy(estadoRegistroPedido = "REGISTRADO")
        val service = object : PedidoService {
            override suspend fun mostrarPedido(id: String) = pedidoDigitalPrueba().copy(usuarioId = "B").aDocumento("ruta")
            override suspend fun consultarPedidos(body: PedidoConsulta) = emptyList<PedidoConsultaRespuesta>()
            override suspend fun actualizarEstadoPedido(id: String, body: PedidoEstadoActualizacion): PedidoDocumento = error("No se utiliza")
            override suspend fun confirmarPedido(body: PedidoCommit): PedidoCommitRespuesta = error("No debe registrar")
        }
        try { recuperarPedidoDigitalRegistrado(service, intentoDigitalPrueba().copy(pago = registrado)); fail() }
        catch (_: IllegalArgumentException) { }
    }
}
