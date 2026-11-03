package com.example.vapeon_movil

import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.ui.codigoSeguridadSimuladoValido
import com.example.vapeon_movil.ui.numeroTarjetaSimuladoValido
import com.example.vapeon_movil.ui.vencimientoTarjetaSimuladaValido
import org.junit.Assert.*
import org.junit.Test
import java.time.YearMonth

class FlujoCheckoutTest {
    private val cliente = Usuario(id = "A", rol = "CLIENTE")
    private fun compra(ruta: String, entrega: Boolean = true, direccion: Boolean = true,
        confirmado: Boolean = false, pendiente: Boolean = false) =
        rutaCompraSegura(ruta, entrega, direccion, confirmado, pendiente)

    @Test fun recuperacionDigitalNuncaSeConvierteEnEfectivo() {
        listOf("PENDIENTE", "CONFIRMADO", "RECHAZADO").forEach { estado ->
            val ruta = rutaPagoDigitalSegura("home", estado, false)
            assertEquals(if (estado == "CONFIRMADO") "confirmacionPago" else "pagoSimulado", ruta)
            assertEquals(ruta, rutaCompraSegura(ruta, false, false, false, false, digitalActivo = true))
        }
    }
    @Test fun pagoEnPreparacionBloqueaCambioDeMetodoAntesDeCrearse() {
        assertEquals("pagoSimulado", rutaPagoDigitalSegura("efectivo", null, false, iniciando = true))
    }
    @Test fun rutaFinancieraSinIntentoNoFabricaConfirmacion() {
        assertEquals("metodoPago", rutaPagoDigitalSegura("confirmacionPago", null, false))
    }
    @Test fun confirmacionDigitalRealPermiteLimpiarSinVolverAEfectivo() {
        assertEquals("confirmacionPedido", rutaPagoDigitalSegura("confirmacionPago", "CONFIRMADO", true))
        assertEquals("home", rutaPagoDigitalSegura("home", null, true))
    }

    @Test fun entradaAnteriorCheckoutConduceADatosEntrega() {
        assertEquals("datosEntrega", compra("checkout"))
    }
    @Test fun rutaRestauradaNoOmiteDatosEntrega() {
        (RUTAS_COMPRA - "datosEntrega").forEach {
            assertEquals("datosEntrega", compra(it, entrega = false, direccion = false))
        }
    }
    @Test fun rutaRestauradaNoOmiteDireccion() {
        listOf("metodoPago", "efectivo", "yape", "seleccionTarjeta").forEach {
            assertEquals("seleccionDireccion", compra(it, direccion = false))
        }
    }
    @Test fun rutaCompletaSeConservaAlRotar() {
        listOf("datosEntrega", "seleccionDireccion", "metodoPago", "efectivo", "yape", "seleccionTarjeta").forEach {
            assertEquals(it, compra(it))
        }
    }
    @Test fun confirmacionExigePedidoReal() {
        assertEquals("datosEntrega", compra("confirmacionPedido"))
        assertEquals("confirmacionPedido", compra("confirmacionPedido", confirmado = true))
    }
    @Test fun exitoDeEfectivoAbreConfirmacionIndependiente() {
        assertEquals("confirmacionPedido", compra("efectivo", confirmado = true))
    }
    @Test fun recuperacionTienePrioridadSobreBorradorIncompleto() {
        RUTAS_COMPRA.forEach {
            assertEquals("efectivo", compra(it, entrega = false, direccion = false, pendiente = true))
        }
    }
    @Test fun pedidoRegistradoPendienteDeLimpiezaMantieneConfirmacion() {
        assertEquals("confirmacionPedido", compra("checkout", confirmado = true, pendiente = true))
    }
    @Test fun yapeYTarjetaSinPedidoNoSonConfirmacionNiEfectivo() {
        listOf("yape", "seleccionTarjeta").forEach { assertEquals(it, compra(it)) }
    }
    @Test fun historialYCatalogoNoSonRedirigidosPorConfirmacionAnterior() {
        listOf("home", "catalogo", "historialCompras").forEach {
            assertEquals(it, compra(it, confirmado = true))
        }
    }
    @Test fun adminNoPuedeAbrirNingunaEtapa() {
        val admin = Usuario(id = "admin", rol = "ADMIN")
        (RUTAS_COMPRA + RUTAS_REGISTRO_TARJETA).forEach {
            assertEquals("admin", pantallaRestauradaSegura(it, admin, "admin:ADMIN"))
        }
    }
    @Test fun cambioDeUsuarioNoRestauraEtapasDelAnterior() {
        (RUTAS_COMPRA + RUTAS_REGISTRO_TARJETA).forEach {
            assertEquals("home", pantallaRestauradaSegura(it, cliente, "B:CLIENTE"))
            assertEquals("login", pantallaRestauradaSegura(it, null, "A:CLIENTE"))
        }
    }
    @Test fun registroSinUltimosOMarcaVuelveAlNumero() {
        listOf("tarjetaExpiracion", "tarjetaCvv", "tarjetaTitular").forEach {
            assertEquals("tarjetaNumero", rutaRegistroTarjetaSegura(it, "", "VISA", "10", "9999", true))
            assertEquals("tarjetaNumero", rutaRegistroTarjetaSegura(it, "1234", "", "10", "9999", true))
        }
    }
    @Test fun registroSinVencimientoValidoNoAvanza() {
        assertEquals("tarjetaExpiracion", rutaRegistroTarjetaSegura("tarjetaCvv", "1234", "VISA", "13", "9999", false))
        assertEquals("tarjetaExpiracion", rutaRegistroTarjetaSegura("tarjetaTitular", "1234", "VISA", "10", "2020", true))
    }
    @Test fun titularRequierePasoDeSeguridadSinRecibirCodigo() {
        assertEquals("tarjetaCvv", rutaRegistroTarjetaSegura("tarjetaTitular", "1234", "VISA", "10", "9999", false))
        assertEquals("tarjetaTitular", rutaRegistroTarjetaSegura("tarjetaTitular", "1234", "VISA", "10", "9999", true))
    }
    @Test fun numeroSoloValidaFormatoDeSimulacion() {
        assertTrue(numeroTarjetaSimuladoValido("4111111111111111"))
        listOf("", "1234", "411111111111111x", "4111 1111 1111 1111", "1".repeat(20)).forEach {
            assertFalse(numeroTarjetaSimuladoValido(it))
        }
    }
    @Test fun cvvSoloAdmiteTresOCuatroDigitos() {
        assertTrue(codigoSeguridadSimuladoValido("123"))
        assertTrue(codigoSeguridadSimuladoValido("1234"))
        listOf("", "12", "12345", "12x").forEach { assertFalse(codigoSeguridadSimuladoValido(it)) }
    }
    @Test fun expiracionConservaReglaDelMesCompleto() {
        val hoy = YearMonth.of(2026, 10)
        assertTrue(vencimientoTarjetaSimuladaValido("10", "2026", hoy))
        assertFalse(vencimientoTarjetaSimuladaValido("09", "2026", hoy))
        assertFalse(vencimientoTarjetaSimuladaValido("0", "2027", hoy))
    }
}
