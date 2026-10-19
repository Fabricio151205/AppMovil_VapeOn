package com.example.vapeon_movil

import com.example.vapeon_movil.entities.Pedido
import com.example.vapeon_movil.entities.ProductoPedido
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.viewmodels.PedidoViewModel
import com.example.vapeon_movil.viewmodels.RecuperacionCheckout
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class RecuperacionCheckoutTest {
    private val registros = mutableMapOf<String, String>()
    private fun recuperacion() = RecuperacionCheckout(
        leer = { registros[it] },
        guardar = { clave, json -> registros[clave] = json; true },
        eliminar = { registros.remove(it); true }
    )

    private fun intento(usuario: String = "A"): PedidoViewModel.Intento {
        val precio = BigDecimal("10.00")
        val pedido = Pedido("orden-$usuario", usuario, "Cliente", "987654321",
            "2026-10-14T15:00:00Z",
            listOf(ProductoPedido("p1", "Producto", "Marca", 1, precio, precio)),
            precio, BigDecimal("5.00"), BigDecimal("15.00"), BigDecimal("20.00"),
            BigDecimal("5.00"), "Calle 123")
        val ruta = "projects/db/databases/(default)/documents"
        return PedidoViewModel.Intento(pedido, PedidoCommit(listOf(
            PedidoEscritura(pedido.aDocumento(ruta), PedidoPrecondicion(exists = false)),
            PedidoEscritura(PedidoDocumento("$ruta/productos/p1",
                mapOf("stock" to FirestoreValor(integerValue = "4"))),
                PedidoPrecondicion(updateTime = pedido.fecha), PedidoMascara(listOf("stock")))
        )))
    }

    @Test fun recuperaElIntentoPropioDespuesDeRecrear() {
        val recuperacion = recuperacion()
        recuperacion.actualizarUsuario("A")
        assertTrue(recuperacion.guardar(intento()))
        val nueva = recuperacion()
        nueva.actualizarUsuario("A")
        assertEquals(intento(), nueva.intento)
    }

    @Test fun otroUsuarioNoRecuperaNiEliminaElRegistroGlobal() {
        val json = Gson().toJson(intento())
        registros["intento"] = json
        val recuperacion = recuperacion()
        recuperacion.actualizarUsuario("B")
        assertNull(recuperacion.intento)
        recuperacion.descartar()
        assertEquals(json, registros["intento"])
    }

    @Test fun propietarioVacioNoSeRecupera() {
        registros["intento"] = Gson().toJson(intento(""))
        val recuperacion = recuperacion()
        recuperacion.actualizarUsuario("A")
        assertNull(recuperacion.intento)
        assertFalse(registros.containsKey("intento"))
    }

    @Test fun jsonCorruptoSeLimpiaSinBloquearNiBorrarOtrosRegistros() {
        val otro = Gson().toJson(intento("B"))
        registros["intento_A"] = "{json roto"
        registros["intento_B"] = otro
        val recuperacion = recuperacion()
        recuperacion.actualizarUsuario("A")
        assertNull(recuperacion.intento)
        assertFalse(registros.containsKey("intento_A"))
        assertEquals(otro, registros["intento_B"])
    }

    @Test fun sinRegistroNoSeActivaRecuperacion() {
        val recuperacion = recuperacion()
        recuperacion.actualizarUsuario("A")
        assertNull(recuperacion.intento)
        assertTrue(registros.isEmpty())
    }

    @Test fun cambiarDeUsuarioLimpiaEstadoYPermiteVolverAlPropietario() {
        val recuperacion = recuperacion()
        recuperacion.actualizarUsuario("A")
        recuperacion.guardar(intento())
        assertNotNull(recuperacion.intento)
        recuperacion.actualizarUsuario(null)
        assertNull(recuperacion.intento)
        recuperacion.actualizarUsuario("B")
        assertNull(recuperacion.intento)
        assertTrue(registros.containsKey("intento_A"))
        recuperacion.actualizarUsuario("A")
        assertEquals(intento(), recuperacion.intento)
    }

    @Test fun dosUsuariosConservanSusPropiosIntentos() {
        val recuperacion = recuperacion()
        recuperacion.actualizarUsuario("A")
        recuperacion.guardar(intento())
        recuperacion.actualizarUsuario("B")
        recuperacion.guardar(intento("B"))
        recuperacion.descartar()
        assertFalse(registros.containsKey("intento_B"))
        assertTrue(registros.containsKey("intento_A"))
        recuperacion.actualizarUsuario("A")
        assertEquals(intento(), recuperacion.intento)
    }

    @Test fun registroAnteriorSeMigraSoloAlGuardarPorSuPropietario() {
        registros["intento"] = Gson().toJson(intento())
        val recuperacion = recuperacion()
        recuperacion.actualizarUsuario("A")
        assertEquals(intento(), recuperacion.intento)
        assertTrue(recuperacion.guardar(recuperacion.intento!!))
        assertFalse(registros.containsKey("intento"))
        assertTrue(registros.containsKey("intento_A"))
    }

    @Test fun registrosIncompletosONulosNoActivanRecuperacion() {
        listOf("null", "{}", "{\"pedido\":{\"usuarioId\":\"A\"},\"commit\":{}}").forEach {
            registros["intento_A"] = it
            val recuperacion = recuperacion()
            recuperacion.actualizarUsuario("A")
            assertNull(recuperacion.intento)
            assertFalse(registros.containsKey("intento_A"))
        }
    }

    @Test fun sesionVaciaNoRecuperaYNoSePuedeGuardarUnIntentoAjeno() {
        registros["intento"] = Gson().toJson(intento())
        val recuperacion = recuperacion()
        recuperacion.actualizarUsuario("")
        assertNull(recuperacion.intento)
        assertFalse(recuperacion.guardar(intento()))
        recuperacion.actualizarUsuario("B")
        assertFalse(recuperacion.guardar(intento()))
        assertTrue(registros.containsKey("intento"))
    }

    @Test fun repetirMismaSesionNoRecargaNiPierdeElIntento() {
        val recuperacion = recuperacion()
        recuperacion.actualizarUsuario("A")
        recuperacion.guardar(intento())
        assertFalse(recuperacion.actualizarUsuario("A"))
        assertEquals(intento(), recuperacion.intento)
    }

    @Test fun falloAlGuardarNoBorraElRegistroAnterior() {
        val json = Gson().toJson(intento())
        registros["intento"] = json
        val recuperacion = RecuperacionCheckout({ registros[it] }, { _, _ -> false },
            { registros.remove(it); true })
        recuperacion.actualizarUsuario("A")
        assertFalse(recuperacion.guardar(intento()))
        assertEquals(json, registros["intento"])
    }

    @Test fun guardarYCompletarCompraDeBConservaElIntentoGlobalDeA() {
        val json = Gson().toJson(intento())
        registros["intento"] = json
        val recuperacion = recuperacion()
        recuperacion.actualizarUsuario("B")
        assertTrue(recuperacion.guardar(intento("B")))
        recuperacion.descartar()
        assertEquals(json, registros["intento"])
        recuperacion.actualizarUsuario("A")
        assertEquals(intento(), recuperacion.intento)
    }

    @Test fun propietarioSinSesionActualNoSeRecupera() {
        val json = Gson().toJson(intento("usuario-inexistente"))
        registros["intento"] = json
        val recuperacion = recuperacion()
        recuperacion.actualizarUsuario("A")
        assertNull(recuperacion.intento)
        assertEquals(json, registros["intento"])
    }
}
