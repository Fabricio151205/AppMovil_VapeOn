package com.example.vapeon_movil

import com.example.vapeon_movil.entities.Pedido
import com.example.vapeon_movil.entities.ProductoPedido
import com.example.vapeon_movil.services.PedidoService
import com.example.vapeon_movil.services.dto.*
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import retrofit2.http.POST
import java.math.BigDecimal
import java.net.URI

class HistorialPedidoTest {
    private val ruta = "projects/dbvapeon/databases/(default)/documents"

    // Documentos de prueba solo en test; la aplicación obtiene sus datos de Firestore.
    private fun documento(id: String, usuario: String, fecha: String): PedidoDocumento {
        val precio = BigDecimal("70.00")
        return Pedido(id, usuario, "Cliente", "987654321", fecha,
            listOf(ProductoPedido("producto1", "Nombre al comprar", "Marca", 1, precio, precio)),
            precio, BigDecimal("5.00"), BigDecimal("75.00"), BigDecimal("100.00"),
            BigDecimal("25.00"), "Dirección original").aDocumento(ruta)
    }

    @Test fun consultaFiltradaPorUsuarioEnFirestore() {
        val consulta = Gson().toJsonTree(consultaPedidosDelUsuario("clienteA")).asJsonObject
            .getAsJsonObject("structuredQuery")
        assertEquals("pedidos", consulta.getAsJsonArray("from")[0].asJsonObject["collectionId"].asString)
        val filtro = consulta.getAsJsonObject("where").getAsJsonObject("fieldFilter")
        assertEquals("usuarioId", filtro.getAsJsonObject("field")["fieldPath"].asString)
        assertEquals("EQUAL", filtro["op"].asString)
        assertEquals("clienteA", filtro.getAsJsonObject("value")["stringValue"].asString)
        assertThrows(IllegalArgumentException::class.java) { consultaPedidosDelUsuario("") }
        val endpoint = PedidoService::class.java.methods.first { it.name == "consultarPedidos" }
            .getAnnotation(POST::class.java)!!.value
        assertEquals("https://firestore.googleapis.com/v1/$ruta:runQuery",
            URI("https://firestore.googleapis.com/v1/$ruta/").resolve(endpoint).toString())
    }

    @Test fun historialSoloDelClienteYOrdenadoPorInstanteReal() {
        val documentos = listOf(
            documento("antiguo", "clienteA", "2026-10-12T12:00:00Z"),
            documento("ajeno", "clienteB", "2026-10-16T12:00:00Z"),
            documento("medio", "clienteA", "2026-10-14T11:00:00.001Z"),
            documento("reciente", "clienteA", "2026-10-14T11:00:00.010Z")
        )
        assertEquals(listOf("reciente", "medio", "antiguo"), documentos.pedidosDelUsuario("clienteA").map { it.id })
        assertTrue(documentos.pedidosDelUsuario("sinCompras").isEmpty())
    }

    @Test fun detalleAjenoSeRechazaAntesDeLeerSusDatos() {
        val ajeno = PedidoDocumento("$ruta/pedidos/ajeno",
            mapOf("usuarioId" to FirestoreValor(stringValue = "clienteB")))
        val error = assertThrows(IllegalArgumentException::class.java) { ajeno.aPedidoDelUsuario("clienteA") }
        assertEquals("No tienes acceso a este pedido.", error.message)
    }

    @Test fun detalleConservaLosDatosGuardadosSinRecalcular() {
        val doc = documento("compra", "clienteA", "2026-10-14T12:00:00Z")
        // Un importe guardado se debe mostrar como está, sin consultar el catálogo.
        val campos = doc.fields + ("total" to FirestoreValor(doubleValue = 80.0))
        val json = Gson().toJson(PedidoDocumento(doc.name, campos))
        val pedido = Gson().fromJson(json, PedidoDocumento::class.java).aPedidoDelUsuario("clienteA")
        assertEquals("compra", pedido.id)
        assertEquals("Nombre al comprar", pedido.productos.single().nombre)
        assertEquals(1, pedido.productos.single().cantidad)
        assertEquals(BigDecimal("70.00"), pedido.productos.single().precioUnitario)
        assertEquals(BigDecimal("70.00"), pedido.productos.single().subtotal)
        assertEquals(BigDecimal("70.00"), pedido.subtotal)
        assertEquals(BigDecimal("5.00"), pedido.delivery)
        assertEquals(BigDecimal("80.00"), pedido.total)
        assertEquals(BigDecimal("100.00"), pedido.montoPagado)
        assertEquals(BigDecimal("25.00"), pedido.vuelto)
        assertEquals("EFECTIVO", pedido.metodoPago)
        assertEquals("REGISTRADO", pedido.estado)
        assertEquals("Dirección original", pedido.direccionEntrega)
    }

    @Test fun respuestaVaciaEInformacionIncompleta() {
        val vacia = Gson().fromJson("{\"readTime\":\"2026-10-14T12:00:00Z\"}", PedidoConsultaRespuesta::class.java)
        assertNull(vacia.document)
        assertTrue(emptyList<PedidoDocumento>().pedidosDelUsuario("clienteA").isEmpty())
        val doc = documento("compra", "clienteA", "2026-10-14T12:00:00Z")
        assertThrows(IllegalArgumentException::class.java) {
            PedidoDocumento(doc.name, doc.fields - "fecha").aPedidoDelUsuario("clienteA")
        }
    }
}
