package com.example.vapeon_movil

import com.example.vapeon_movil.entities.*
import com.example.vapeon_movil.services.PedidoService
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.viewmodels.leerPrecioProductoParaCompra
import com.example.vapeon_movil.viewmodels.prepararDescuentoStockParaCompra
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.net.URI
import retrofit2.http.POST

class PedidoTest {
    @Test fun efectivoYVueltoConCentimos() {
        assertEquals(BigDecimal("24.50"), Pedido.calcularVuelto(
            Pedido.leerMonto("100,00")!!, BigDecimal("75.50")))
        assertEquals(BigDecimal("0.00"), Pedido.calcularVuelto(BigDecimal("75.50"), BigDecimal("75.50")))
        assertThrows(IllegalArgumentException::class.java) {
            Pedido.calcularVuelto(BigDecimal("50.00"), BigDecimal("75.00"))
        }
        listOf("", "abc", "NaN", "Infinity", "-1", "1e2", "1.234", "1,2.3").forEach {
            assertNull("Debe rechazar $it", Pedido.leerMonto(it))
        }
    }

    @Test fun stockNuncaNegativo() {
        assertEquals(2, Pedido.validarStock("5", 3))
        assertEquals(0, Pedido.validarStock("3", 3))
        listOf("2", "-1", "abc", null).forEach { stock ->
            assertThrows(IllegalArgumentException::class.java) { Pedido.validarStock(stock, 3) }
        }
        assertThrows(IllegalArgumentException::class.java) { Pedido.validarStock("5", 0) }
    }

    @Test fun subtotalExactoYCarritoValido() {
        val item = ItemCarrito("p1", "Producto", "Marca", 0.10, 3)
        assertEquals(BigDecimal("0.30"), Pedido.subtotal(listOf(item)))
        listOf(emptyList(), listOf(item.copy(cantidad = 0)), listOf(item, item),
            listOf(item.copy(precioUnit = Double.NaN)), listOf(item.copy(precioUnit = 0.0))).forEach {
            assertThrows(IllegalArgumentException::class.java) { Pedido.subtotal(it) }
        }
    }

    @Test fun commitConCopiaYPrecondiciones() {
        val precio = BigDecimal("70.00")
        val pedido = Pedido("orden1", "cliente1", "Ana", "987654321", "2026-10-14T15:00:00Z",
            listOf(ProductoPedido("p1", "Nombre original", "Marca", 1, precio, precio)),
            precio, BigDecimal("5.00"), BigDecimal("75.00"), BigDecimal("100.00"),
            BigDecimal("25.00"), "Calle 123")
        val ruta = "projects/dbvapeon/databases/(default)/documents"
        val commit = PedidoCommit(listOf(
            PedidoEscritura(pedido.aDocumento(ruta), PedidoPrecondicion(exists = false)),
            prepararDescuentoStockParaCompra(ProductoDocumento("$ruta/productos/p1",
                ProductoCampos(stock = FirestoreValue(integerValue = "5")), pedido.fecha), 3)
        ))
        val json = Gson().toJsonTree(commit).asJsonObject.getAsJsonArray("writes")
        assertEquals(2, json.size())
        assertFalse(json[0].asJsonObject["currentDocument"].asJsonObject["exists"].asBoolean)
        val fields = json[0].asJsonObject["update"].asJsonObject["fields"].asJsonObject
        assertEquals("cliente1", fields["usuarioId"].asJsonObject["stringValue"].asString)
        assertEquals("EFECTIVO", fields["metodoPago"].asJsonObject["stringValue"].asString)
        assertEquals("REGISTRADO", fields["estado"].asJsonObject["stringValue"].asString)
        assertFalse(fields.has("password"))
        assertEquals("stock", json[1].asJsonObject["updateMask"].asJsonObject["fieldPaths"].asJsonArray[0].asString)
        val stock = json[1].asJsonObject["update"].asJsonObject["fields"].asJsonObject["stock"].asJsonObject
        assertEquals(setOf("integerValue"), stock.keySet())
        assertEquals("2", stock["integerValue"].asString)
        assertEquals(pedido.fecha, json[1].asJsonObject["currentDocument"].asJsonObject["updateTime"].asString)
        val restaurado = Gson().fromJson(Gson().toJson(pedido), Pedido::class.java)
        assertEquals(pedido, restaurado)
    }

    @Test fun rutaCommitUsaLaMismaBase() {
        val ruta = PedidoService::class.java.methods.first { it.name == "confirmarPedido" }
            .getAnnotation(POST::class.java)!!.value
        val base = URI("https://firestore.googleapis.com/v1/projects/dbvapeon/databases/(default)/documents/")
        assertEquals("https://firestore.googleapis.com/v1/projects/dbvapeon/databases/(default)/documents:commit",
            base.resolve(ruta).toString())
    }

    @Test fun productoSerializaStockEnteroYPrecioDouble() {
        val body = ProductoContainer(ProductoCampos(
            precio = FirestoreValue(doubleValue = 70.90),
            stock = FirestoreValue(integerValue = "5")
        ))
        val campos = Gson().toJsonTree(body).asJsonObject["fields"].asJsonObject
        assertEquals(setOf("integerValue"), campos["stock"].asJsonObject.keySet())
        assertEquals("5", campos["stock"].asJsonObject["integerValue"].asString)
        assertEquals(setOf("doubleValue"), campos["precio"].asJsonObject.keySet())
        assertEquals(70.90, campos["precio"].asJsonObject["doubleValue"].asDouble, 0.0)
    }

    @Test fun rf22LeeProductoNumericoYSuDescuento() {
        // Respuesta realista REST: precio decimal y stock entero, sin stringValue.
        val json = """{
            "name":"projects/dbvapeon/databases/(default)/documents/productos/p1",
            "updateTime":"2026-10-14T12:00:00Z",
            "fields":{"precio":{"doubleValue":70.90},"stock":{"integerValue":"5"}}
        }"""
        val doc = Gson().fromJson(json, ProductoDocumento::class.java)
        assertEquals(BigDecimal("70.90"), leerPrecioProductoParaCompra(doc))
        val descuento = prepararDescuentoStockParaCompra(doc, 3)
        assertEquals(doc.name, descuento.update.name)
        assertEquals(setOf("stock"), descuento.update.fields.keys)
        assertEquals("2", descuento.update.fields["stock"]?.integerValue)
        assertNull(descuento.update.fields["stock"]?.doubleValue)
        assertNull(descuento.update.fields["stock"]?.stringValue)
        assertEquals(doc.updateTime, descuento.currentDocument.updateTime)
        assertEquals(listOf("stock"), descuento.updateMask?.fieldPaths)
    }

    @Test fun rf22ConservaCompatibilidadConProductosDeTexto() {
        val doc = ProductoDocumento("productos/p1", ProductoCampos(
            precio = FirestoreValue(stringValue = "70.90"),
            stock = FirestoreValue(stringValue = "5")
        ), "2026-10-14T12:00:00Z")
        assertEquals(BigDecimal("70.90"), leerPrecioProductoParaCompra(doc))
        assertEquals("2", prepararDescuentoStockParaCompra(doc, 3).update.fields["stock"]?.integerValue)
    }

    @Test fun descuentoEnteroValidaStockDisponible() {
        val doc = ProductoDocumento("productos/p1", ProductoCampos(
            stock = FirestoreValue(integerValue = "3")
        ), "2026-10-14T12:00:00Z")
        assertEquals("0", prepararDescuentoStockParaCompra(doc, 3).update.fields["stock"]?.integerValue)
        assertThrows(IllegalArgumentException::class.java) { prepararDescuentoStockParaCompra(doc, 4) }
        assertThrows(IllegalArgumentException::class.java) { prepararDescuentoStockParaCompra(doc, 0) }
        val invalido = doc.copy(fields = ProductoCampos(stock = FirestoreValue(doubleValue = 3.5)))
        assertThrows(IllegalArgumentException::class.java) { prepararDescuentoStockParaCompra(invalido, 1) }
    }
}
