package com.example.vapeon_movil

import com.example.vapeon_movil.entities.*
import com.example.vapeon_movil.services.PedidoService
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.viewmodels.leerPrecioProductoParaCompra
import com.example.vapeon_movil.viewmodels.prepararDescuentoStockParaCompra
import com.example.vapeon_movil.viewmodels.prepararDescuentoStockPorLineas
import com.example.vapeon_movil.viewmodels.SolicitudStockCompra
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

    @Test fun snapshotNuevoConservaCategoriaYSabor() {
        val precio = BigDecimal("50.00")
        val linea = ProductoPedido("p1", "LifePod", "LifePod", 2, precio,
            BigDecimal("100.00"), categoria = "Batería", sabor = "Strawberry")
        val pedido = Pedido("orden-sabor", "cliente", "Cliente", "987654321", "2026-11-02T12:00:00Z",
            listOf(linea), BigDecimal("100.00"), BigDecimal("5.00"), BigDecimal("105.00"),
            BigDecimal("105.00"), BigDecimal("0.00"), "Dirección")
        val documento = pedido.aDocumento("projects/db/databases/(default)/documents")
        val camposLinea = documento.fields.getValue("productos").arrayValue!!.values.single().mapValue!!.fields
        assertEquals("Batería", camposLinea["categoria"]?.stringValue)
        assertEquals("Strawberry", camposLinea["sabor"]?.stringValue)
        assertEquals(pedido, documento.aPedido())
        assertEquals(BigDecimal("100.00"), documento.aPedido().productos.single().subtotal)
    }

    @Test fun pedidoHistoricoSinCategoriaNiSaborSigueCargando() {
        val precio = BigDecimal("10.00")
        val pedido = Pedido("historico", "cliente", "Cliente", "987654321", "2026-10-14T12:00:00Z",
            listOf(ProductoPedido("p1", "Nombre histórico", "Marca histórica", 1, precio, precio)),
            precio, BigDecimal("5.00"), BigDecimal("15.00"), BigDecimal("20.00"),
            BigDecimal("5.00"), "Dirección")
        val leido = pedido.aDocumento("projects/db/databases/(default)/documents").aPedido()
        assertNull(leido.productos.single().categoria)
        assertNull(leido.productos.single().sabor)
        assertEquals("Nombre histórico", leido.productos.single().nombre)
    }

    @Test fun descuentoPorSaborActualizaGeneralYMapaEnUnaSolaEscritura() {
        val doc = ProductoDocumento("projects/db/databases/(default)/documents/productos/p1", ProductoCampos(
            stock = FirestoreValue(integerValue = "5"),
            sabor = FirestoreValue(stringValue = "Strawberry, Mint"),
            stockPorSabor = mapOf("Strawberry" to 2, "Mint" to 3).aFirestoreMapa()
        ), "2026-11-02T12:00:00Z")
        val resultado = prepararDescuentoStockPorLineas(doc, listOf(
            SolicitudStockCompra("strawberry", 1), SolicitudStockCompra("Mint", 2)))
        assertEquals(listOf("Strawberry", "Mint"), resultado.saboresCanonicos)
        assertEquals(setOf("stock", "stockPorSabor"), resultado.escritura.update.fields.keys)
        assertEquals("2", resultado.escritura.update.fields["stock"]?.integerValue)
        assertEquals(mapOf("Strawberry" to 1, "Mint" to 1),
            resultado.escritura.update.fields["stockPorSabor"].aStockMap())
        assertEquals(listOf("stock", "stockPorSabor"), resultado.escritura.updateMask?.fieldPaths)
        assertEquals(doc.updateTime, resultado.escritura.currentDocument.updateTime)
    }

    @Test fun checkoutConSaborNuncaPreparaEscriturasEnColeccionSabores() {
        val doc = ProductoDocumento(
            "projects/db/databases/(default)/documents/productos/p1",
            ProductoCampos(
                stock = FirestoreValue(integerValue = "4"),
                stockPorSabor = mapOf("Fresa" to 2, "Menta" to 2).aFirestoreMapa()
            ),
            "2026-11-02T12:00:00Z"
        )

        val escritura = prepararDescuentoStockPorLineas(doc, listOf(
            SolicitudStockCompra("Fresa", 1), SolicitudStockCompra("Menta", 1)
        )).escritura

        assertTrue(escritura.update.name.endsWith("/productos/p1"))
        assertFalse(escritura.update.name.contains("/sabores/"))
        assertEquals(setOf("stock", "stockPorSabor"), escritura.update.fields.keys)
    }

    @Test fun pedidoConservaElPuntoExactoDeEntrega() {
        val pedido = pedidoDePrueba().copy(latitud = -12.0464, longitud = -77.0428)
        val documento = pedido.aDocumento("projects/db/databases/(default)/documents")
        assertEquals(-12.0464, documento.fields.getValue("latitud").doubleValue!!, 0.0)
        assertEquals(-77.0428, documento.fields.getValue("longitud").doubleValue!!, 0.0)
        val leido = documento.aPedido()
        assertEquals(pedido, leido)
        assertTrue(leido.tieneUbicacionEntrega)
        assertEquals(-12.0464, leido.latitud!!, 0.0)
        assertEquals(-77.0428, leido.longitud!!, 0.0)
    }

    @Test fun pedidoAntiguoSinCoordenadasSigueCargando() {
        val pedido = pedidoDePrueba()
        val documento = pedido.aDocumento("projects/db/databases/(default)/documents")
        assertFalse(documento.fields.containsKey("latitud"))
        assertFalse(documento.fields.containsKey("longitud"))
        val leido = documento.aPedido()
        assertNull(leido.latitud)
        assertNull(leido.longitud)
        assertFalse(leido.tieneUbicacionEntrega)
    }

    @Test fun pedidoConCoordenadasIncompletasSeRechaza() {
        val documento = pedidoDePrueba().copy(latitud = -12.0464).aDocumento("projects/db/databases/(default)/documents")
        assertThrows(IllegalArgumentException::class.java) { documento.aPedido() }
        val soloLongitud = pedidoDePrueba().copy(longitud = -77.0428)
            .aDocumento("projects/db/databases/(default)/documents")
        assertThrows(IllegalArgumentException::class.java) { soloLongitud.aPedido() }
    }

    private fun pedidoDePrueba(): Pedido {
        val precio = BigDecimal("10.00")
        return Pedido("pedido-mapa", "cliente", "Cliente", "987654321", "2026-11-06T12:00:00Z",
            listOf(ProductoPedido("p1", "Producto", "Marca", 1, precio, precio)),
            precio, BigDecimal("5.00"), BigDecimal("15.00"), BigDecimal("20.00"),
            BigDecimal("5.00"), "Av. Siempre Viva 742")
    }

    @Test fun stockInsuficienteGeneralOPorSaborBloqueaAntesDelCommit() {
        val base = ProductoDocumento("productos/p1", ProductoCampos(
            stock = FirestoreValue(integerValue = "2"),
            stockPorSabor = mapOf("Fresa" to 1, "Menta" to 2).aFirestoreMapa()
        ), "2026-11-02T12:00:00Z")
        assertThrows(IllegalArgumentException::class.java) {
            prepararDescuentoStockPorLineas(base, listOf(SolicitudStockCompra("Fresa", 2)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            prepararDescuentoStockPorLineas(base, listOf(
                SolicitudStockCompra("Fresa", 1), SolicitudStockCompra("Menta", 2)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            prepararDescuentoStockPorLineas(base, listOf(SolicitudStockCompra("Otro", 1)))
        }
    }
}
