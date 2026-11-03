package com.example.vapeon_movil

import com.example.vapeon_movil.entities.*
import com.example.vapeon_movil.services.PagoService
import com.example.vapeon_movil.services.dto.*
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import retrofit2.http.PATCH

internal fun pagoPrueba(estado: String = "PENDIENTE", usuario: String = "A", metodo: String = "YAPE") =
    Pago("pago1", "pedido1", usuario, metodo, "SIM-$metodo-pago1", BigDecimal("15.00"),
        "2026-10-30T12:00:00Z", estadoPago = estado,
        fechaResultado = if (estado == "PENDIENTE") null else "2026-10-30T12:01:00Z",
        tarjetaId = if (metodo == "TARJETA") "tarjeta1" else null)

internal fun pedidoDigitalPrueba() = Pedido("pedido1", "A", "Ana", "912345678", "2026-10-30T12:00:00Z",
    listOf(ProductoPedido("p1", "Producto", "Marca", 1, BigDecimal("10.00"), BigDecimal("10.00"))),
    BigDecimal("10.00"), BigDecimal("5.00"), BigDecimal("15.00"), BigDecimal("15.00"), BigDecimal("0.00"),
    "Dirección", "YAPE", pagoId = "pago1", estadoPago = "CONFIRMADO", numeroOperacion = "SIM-YAPE-pago1",
    modalidadPago = "SIMULADO")

internal fun documentoPago(pago: Pago) = PagoDocumento("projects/prueba/databases/(default)/documents/pagos/${pago.id}",
    pago.aFirestore().fields, "2026-10-30T12:01:00Z")

class PagoTest {
    @Test fun pendienteSeSerializaSinResultadoNiTarjeta() {
        val fields = pagoPrueba().aFirestore().fields
        assertEquals("PENDIENTE", fields["estadoPago"]?.stringValue)
        assertEquals("SIMULADO", fields["modalidad"]?.stringValue)
        assertEquals("PEN", fields["moneda"]?.stringValue)
        assertFalse(fields.containsKey("fechaResultado"))
        assertFalse(fields.containsKey("tarjetaId"))
        assertEquals(15.0, fields["monto"]?.doubleValue)
    }
    @Test fun confirmadoYRechazadoConservanIdentidad() {
        val original = pagoPrueba()
        listOf("CONFIRMADO", "RECHAZADO").forEach {
            val resultado = pagoPrueba(it)
            resultado.validar()
            assertTrue(original.mismoIntento(resultado))
            assertEquals(original.numeroOperacion, resultado.numeroOperacion)
        }
    }
    @Test fun lecturaFirestoreConservaTodosLosDatos() {
        val doc = documentoPago(pagoPrueba("CONFIRMADO", metodo = "TARJETA"))
        val copia = Gson().fromJson(Gson().toJson(doc), PagoDocumento::class.java)
        assertEquals(pagoPrueba("CONFIRMADO", metodo = "TARJETA"), copia.aPagoDelUsuario("A"))
    }
    @Test fun propietarioSeValidaAntesDeLeerImportes() {
        assertThrows(IllegalArgumentException::class.java) {
            PagoDocumento("pagos/ajeno", mapOf("usuarioId" to FirestoreValor(stringValue = "B"))).aPagoDelUsuario("A")
        }
    }
    @Test fun identidadDelDocumentoDebeCoincidir() {
        assertThrows(IllegalArgumentException::class.java) { documentoPago(pagoPrueba()).copy(name = "pagos/otro").aPagoDelUsuario("A") }
    }
    @Test fun numeroOperacionNoPuedeCambiar() {
        assertThrows(IllegalArgumentException::class.java) { pagoPrueba().copy(numeroOperacion = "otra").validar() }
    }
    @Test fun estadoFinancieroInvalidoEsRechazado() {
        assertThrows(IllegalArgumentException::class.java) { pagoPrueba().copy(estadoPago = "ENTREGADO").validar() }
    }
    @Test fun tarjetaSoloParaMetodoTarjeta() {
        assertThrows(IllegalArgumentException::class.java) { pagoPrueba().copy(tarjetaId = "t1").validar() }
        assertThrows(IllegalArgumentException::class.java) { pagoPrueba(metodo = "TARJETA").copy(tarjetaId = null).validar() }
    }
    @Test fun resultadoExigeFecha() {
        assertThrows(IllegalArgumentException::class.java) { pagoPrueba("CONFIRMADO").copy(fechaResultado = null).validar() }
    }
    @Test fun pendienteYRechazadoNoAutorizanPedido() {
        listOf("PENDIENTE", "RECHAZADO").forEach { estado ->
            assertThrows(IllegalArgumentException::class.java) { validarPagoParaPedido(documentoPago(pagoPrueba(estado)), pedidoDigitalPrueba()) }
        }
    }
    @Test fun confirmadoPermitePrepararRegistroConVersion() {
        val doc = documentoPago(pagoPrueba("CONFIRMADO"))
        val escritura = escrituraRegistroPago(doc, pedidoDigitalPrueba())
        assertEquals(doc.updateTime, escritura.currentDocument.updateTime)
        assertEquals(listOf("estadoRegistroPedido"), escritura.updateMask?.fieldPaths)
        assertEquals(setOf("estadoRegistroPedido"), escritura.update.fields.keys)
    }
    @Test fun mismoPagoNoAutorizaOtroPedidoNiImporte() {
        val doc = documentoPago(pagoPrueba("CONFIRMADO"))
        listOf(pedidoDigitalPrueba().copy(id = "otro"), pedidoDigitalPrueba().copy(total = BigDecimal("16.00")),
            pedidoDigitalPrueba().copy(pagoId = "otro"), pedidoDigitalPrueba().copy(usuarioId = "B")).forEach {
            assertThrows(IllegalArgumentException::class.java) { validarPagoParaPedido(doc, it) }
        }
    }
    @Test fun pagoYaRegistradoNoPuedeConsumirseOtraVez() {
        val doc = documentoPago(pagoPrueba("CONFIRMADO").copy(estadoRegistroPedido = "REGISTRADO"))
        assertThrows(IllegalArgumentException::class.java) { validarPagoParaPedido(doc, pedidoDigitalPrueba()) }
    }
    @Test fun patchDeResultadoIncluyePrecondicionEnContrato() {
        assertNotNull(PagoService::class.java.methods.single { it.name == "actualizarResultado" }.getAnnotation(PATCH::class.java))
    }
    @Test fun pedidoHistoricoNoSeConvierteEnPagoConfirmado() {
        val efectivo = pedidoDigitalPrueba().copy(metodoPago = "EFECTIVO", pagoId = null,
            estadoPago = null, numeroOperacion = null, modalidadPago = null)
        val doc = efectivo.aDocumento("projects/prueba/databases/(default)/documents")
        assertFalse(doc.fields.containsKey("estadoPago"))
        assertNull(doc.aPedidoDelUsuario("A").estadoPago)
    }
    @Test fun pedidoDigitalConservaResumenFinancieroYEnvioIndependiente() {
        val pedido = pedidoDigitalPrueba()
        assertEquals(pedido, pedido.aDocumento("ruta").aPedidoDelUsuario("A"))
        assertEquals("REGISTRADO", pedido.estado)
        assertEquals("CONFIRMADO", pedido.estadoPago)
    }
    @Test fun pagoNoSerializaCredenciales() {
        val fields = pagoPrueba(metodo = "TARJETA").aFirestore().fields.keys
        assertEquals(setOf("id", "pedidoId", "usuarioId", "metodoPago", "modalidad", "estadoPago",
            "numeroOperacion", "monto", "moneda", "fechaCreacion", "estadoRegistroPedido", "tarjetaId"), fields)
    }
}
