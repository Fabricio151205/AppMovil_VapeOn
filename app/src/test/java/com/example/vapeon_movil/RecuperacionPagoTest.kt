package com.example.vapeon_movil

import com.example.vapeon_movil.viewmodels.*
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

internal fun intentoDigitalPrueba(estado: String = "PENDIENTE") = IntentoPagoDigital(pagoPrueba(estado),
    pedidoDigitalPrueba().copy(estadoPago = "PENDIENTE"), "projects/prueba/databases/(default)/documents")

class RecuperacionPagoTest {
    private val registros = mutableMapOf<String, String>()
    private fun recuperacion() = RecuperacionPago({ registros[it] }, { clave, json -> registros[clave] = json; true },
        { registros.remove(it); true })

    @Test fun cierreRecuperaIdsOperacionYCompraCongelada() {
        val inicial = recuperacion()
        inicial.actualizarUsuario("A")
        assertTrue(inicial.guardar(intentoDigitalPrueba()))
        val recreada = recuperacion()
        recreada.actualizarUsuario("A")
        assertEquals(inicial.intento, recreada.intento)
        assertEquals("SIM-YAPE-pago1", recreada.intento!!.pago.numeroOperacion)
        assertEquals("Dirección", recreada.intento!!.pedido.direccionEntrega)
    }
    @Test fun pagoConfirmadoSinPedidoSeRecuperaComoTal() {
        val inicial = recuperacion()
        inicial.actualizarUsuario("A")
        inicial.guardar(intentoDigitalPrueba("CONFIRMADO"))
        val nueva = recuperacion()
        nueva.actualizarUsuario("A")
        assertEquals("CONFIRMADO", nueva.intento!!.pago.estadoPago)
        assertEquals("PENDIENTE", nueva.intento!!.pago.estadoRegistroPedido)
    }
    @Test fun rotacionMismaSesionNoReiniciaIntentoNiBloqueo() {
        val actual = recuperacion()
        actual.actualizarUsuario("A")
        actual.guardar(intentoDigitalPrueba())
        val token = actual.iniciarOperacion()!!
        assertFalse(actual.actualizarUsuario("A"))
        assertNull(actual.iniciarOperacion())
        assertNotNull(actual.intento)
        actual.finalizarOperacion(token)
        assertNotNull(actual.iniciarOperacion())
    }
    @Test fun cambioAyBNoLeeNiBorraElIntentoAjeno() {
        val actual = recuperacion()
        actual.actualizarUsuario("A")
        actual.guardar(intentoDigitalPrueba())
        val tokenA = actual.iniciarOperacion()!!
        actual.actualizarUsuario("B")
        assertFalse(actual.vigente(tokenA))
        assertNull(actual.intento)
        assertFalse(actual.guardar(intentoDigitalPrueba()))
        actual.descartar()
        assertTrue(registros.containsKey("digital_A"))
        actual.actualizarUsuario("A")
        assertEquals("pago1", actual.intento!!.pago.id)
    }
    @Test fun jsonAjenoEnClavePropiaNoSeInterpretaNiElimina() {
        registros["digital_B"] = Gson().toJson(intentoDigitalPrueba())
        val actual = recuperacion()
        actual.actualizarUsuario("B")
        assertNull(actual.intento)
        assertTrue(registros.containsKey("digital_B"))
    }
    @Test fun falloPersistenciaNoReemplazaIntentoAnterior() {
        val json = Gson().toJson(intentoDigitalPrueba())
        registros["digital_A"] = json
        val actual = RecuperacionPago({ registros[it] }, { _, _ -> false }, { false })
        actual.actualizarUsuario("A")
        assertFalse(actual.guardar(intentoDigitalPrueba("CONFIRMADO")))
        assertEquals(json, registros["digital_A"])
        assertFalse(actual.descartar())
        assertNotNull(actual.intento)
    }
    @Test fun intentoInconsistenteNoSeRecupera() {
        registros["digital_A"] = Gson().toJson(intentoDigitalPrueba().copy(pedido = pedidoDigitalPrueba().copy(id = "otro")))
        val actual = recuperacion()
        actual.actualizarUsuario("A")
        assertNull(actual.intento)
        assertFalse(registros.containsKey("digital_A"))
    }
    @Test fun persistenciaNoIncluyeNumeroCompletoCvvNiPin() {
        val actual = recuperacion()
        actual.actualizarUsuario("A")
        actual.guardar(intentoDigitalPrueba())
        val json = registros.getValue("digital_A")
        assertFalse(json.contains("4111111111111111"))
        listOf("cvv", "cvc", "pin", "numeroCompleto", "numeroTarjeta").forEach {
            assertFalse(json.contains("\"$it\"", ignoreCase = true))
        }
    }
    @Test fun cerrarSesionCancelaTokenPeroConservaRecuperacion() {
        val actual = recuperacion()
        actual.actualizarUsuario("A")
        actual.guardar(intentoDigitalPrueba())
        val token = actual.iniciarOperacion()!!
        actual.actualizarUsuario(null)
        assertFalse(actual.vigente(token))
        assertNull(actual.intento)
        assertTrue(registros.containsKey("digital_A"))
    }
}
