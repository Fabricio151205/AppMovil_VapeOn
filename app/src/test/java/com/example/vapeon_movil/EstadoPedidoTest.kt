package com.example.vapeon_movil

import com.example.vapeon_movil.entities.Pedido
import com.example.vapeon_movil.entities.ProductoPedido
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.services.PedidoService
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.viewmodels.PedidoViewModel
import com.example.vapeon_movil.viewmodels.validarCambioEstadoPedido
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import retrofit2.http.PATCH
import java.math.BigDecimal

class EstadoPedidoTest {
    @Test fun patchActualizaSoloEstadoYExigeDocumentoExistente() {
        val metodo = PedidoService::class.java.methods.first { it.name == "actualizarEstadoPedido" }
        assertEquals("pedidos/{id}?updateMask.fieldPaths=estado&currentDocument.exists=true",
            metodo.getAnnotation(PATCH::class.java)!!.value)
        val json = Gson().toJsonTree(PedidoEstadoActualizacion(
            mapOf("estado" to FirestoreValor(stringValue = "EN CAMINO")))).asJsonObject
        assertEquals(setOf("fields"), json.keySet())
        val campos = json.getAsJsonObject("fields")
        assertEquals(setOf("estado"), campos.keySet())
        assertEquals("EN CAMINO", campos.getAsJsonObject("estado")["stringValue"].asString)
    }

    @Test fun soloAdminPuedeGuardarEstadosPermitidos() {
        val admin = Usuario(id = "admin1", rol = "ADMIN")
        assertEquals(listOf("REGISTRADO", "EN CAMINO", "ENTREGADO"), PedidoViewModel.ESTADOS_ENVIO)
        PedidoViewModel.ESTADOS_ENVIO.forEach { validarCambioEstadoPedido(admin, "pedido1", it) }
        listOf(null, Usuario(id = "cliente1", rol = "CLIENTE"), Usuario(id = "", rol = "ADMIN"),
            Usuario(id = "otro", rol = "OTRO")).forEach { usuario ->
            assertThrows(IllegalArgumentException::class.java) {
                validarCambioEstadoPedido(usuario, "pedido1", "ENTREGADO")
            }
        }
        listOf("", "CANCELADO", "en camino").forEach { estado ->
            assertThrows(IllegalArgumentException::class.java) { validarCambioEstadoPedido(admin, "pedido1", estado) }
        }
        listOf("", "pedidos/otro").forEach { id ->
            assertThrows(IllegalArgumentException::class.java) { validarCambioEstadoPedido(admin, id, "EN CAMINO") }
        }
    }

    @Test fun consultaAdminNoAlteraElFiltroDelCliente() {
        val gson = Gson()
        val admin = gson.toJsonTree(PedidoConsulta(PedidoConsultaEstructurada(listOf(PedidoColeccion("pedidos")))))
            .asJsonObject.getAsJsonObject("structuredQuery")
        assertFalse(admin.has("where"))
        assertEquals("pedidos", admin.getAsJsonArray("from")[0].asJsonObject["collectionId"].asString)
        val cliente = gson.toJsonTree(consultaPedidosDelUsuario("cliente1")).asJsonObject
            .getAsJsonObject("structuredQuery").getAsJsonObject("where").getAsJsonObject("fieldFilter")
        assertEquals("cliente1", cliente.getAsJsonObject("value")["stringValue"].asString)
    }

    @Test fun clienteLeeElNuevoEstadoYConservaLaRestriccionDePropiedad() {
        val precio = BigDecimal("70.00")
        val pedido = Pedido("pedido1", "cliente1", "Cliente", "987654321", "2026-10-14T12:00:00Z",
            listOf(ProductoPedido("p1", "Producto original", "Marca", 1, precio, precio)),
            precio, BigDecimal("5.00"), BigDecimal("75.00"), BigDecimal("100.00"),
            BigDecimal("25.00"), "Dirección original")
        val original = pedido.aDocumento("projects/dbvapeon/databases/(default)/documents")
        PedidoViewModel.ESTADOS_ENVIO.forEach { estado ->
            val actualizado = original.copy(fields = original.fields +
                ("estado" to FirestoreValor(stringValue = estado)))
            val leido = actualizado.aPedidoDelUsuario("cliente1")
            assertEquals(pedido.copy(estado = estado), leido)
            assertEquals(leido, actualizado.aPedido())
            assertThrows(IllegalArgumentException::class.java) { actualizado.aPedidoDelUsuario("cliente2") }
        }
    }
}
