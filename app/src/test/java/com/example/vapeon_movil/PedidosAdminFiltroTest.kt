package com.example.vapeon_movil

import com.example.vapeon_movil.entities.Pedido
import com.example.vapeon_movil.entities.ProductoPedido
import com.example.vapeon_movil.ui.filtrarPedidosAdmin
import com.example.vapeon_movil.ui.pedidoCoincideFiltroAdmin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class PedidosAdminFiltroTest {
    @Test fun vistaPrincipalOcultaLosEntregados() {
        assertTrue(pedidoCoincideFiltroAdmin("REGISTRADO", "Todos"))
        assertTrue(pedidoCoincideFiltroAdmin("EN CAMINO", "Todos"))
        assertFalse(pedidoCoincideFiltroAdmin("ENTREGADO", "Todos"))
        assertFalse(pedidoCoincideFiltroAdmin("COMPLETADO", "Todos"))
    }

    @Test fun filtroRegistradosSoloMuestraRegistrados() {
        assertTrue(pedidoCoincideFiltroAdmin("REGISTRADO", "Registrados"))
        assertFalse(pedidoCoincideFiltroAdmin("EN CAMINO", "Registrados"))
        assertFalse(pedidoCoincideFiltroAdmin("ENTREGADO", "Registrados"))
    }

    @Test fun filtroEnCaminoAceptaElEstadoCanonico() {
        assertTrue(pedidoCoincideFiltroAdmin("EN CAMINO", "En camino"))
        assertTrue(pedidoCoincideFiltroAdmin("EN CURSO", "En camino"))
        assertFalse(pedidoCoincideFiltroAdmin("REGISTRADO", "En camino"))
        assertFalse(pedidoCoincideFiltroAdmin("ENTREGADO", "En camino"))
    }

    @Test fun filtroCompletadosSoloMuestraEntregados() {
        assertTrue(pedidoCoincideFiltroAdmin("ENTREGADO", "Completados"))
        assertTrue(pedidoCoincideFiltroAdmin("COMPLETADO", "Completados"))
        assertFalse(pedidoCoincideFiltroAdmin("REGISTRADO", "Completados"))
        assertFalse(pedidoCoincideFiltroAdmin("EN CAMINO", "Completados"))
    }

    @Test fun listaFiltradaConservaLosPedidosRealesSegunSuEstado() {
        val registrado = pedido("registrado", "REGISTRADO")
        val enCamino = pedido("en-camino", "EN CAMINO")
        val entregado = pedido("entregado", "ENTREGADO")
        val todos = listOf(registrado, enCamino, entregado)

        assertEquals(listOf(registrado, enCamino), filtrarPedidosAdmin(todos, "Todos"))
        assertEquals(listOf(registrado), filtrarPedidosAdmin(todos, "Registrados"))
        assertEquals(listOf(enCamino), filtrarPedidosAdmin(todos, "En camino"))
        assertEquals(listOf(entregado), filtrarPedidosAdmin(todos, "Completados"))
    }

    private fun pedido(id: String, estado: String): Pedido {
        val precio = BigDecimal("40.00")
        return Pedido(id, "cliente", "Cliente", "987654321", "2026-11-06T12:00:00Z",
            listOf(ProductoPedido("p1", "Producto", "Marca", 2, precio, precio)),
            precio, BigDecimal("5.00"), BigDecimal("45.00"), BigDecimal("50.00"),
            BigDecimal("5.00"), "Av. Siempre Viva 742", estado = estado)
    }
}
