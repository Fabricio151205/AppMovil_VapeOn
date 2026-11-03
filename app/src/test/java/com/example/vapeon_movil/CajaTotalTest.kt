package com.example.vapeon_movil

import com.example.vapeon_movil.entities.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class CajaTotalTest {
    private fun linea(marca: String = "Oxbar", precio: String = "70.00", cantidad: Int = 2) =
        ProductoPedido("p-$marca", "Producto histórico", marca, cantidad, BigDecimal(precio), BigDecimal(precio) * BigDecimal(cantidad))
    private fun pedido(id: String = "p1", estado: String = "ENTREGADO",
        fecha: String = "2026-10-29T12:00:00Z", lineas: List<ProductoPedido> = listOf(linea())): Pedido {
        val subtotal = lineas.fold(BigDecimal.ZERO) { suma, item -> suma + item.subtotal }
        return Pedido(id, "cliente", "Cliente", "987654321", fecha, lineas, subtotal, BigDecimal("5.00"),
            subtotal + BigDecimal("5.00"), BigDecimal("1000.00"), BigDecimal("855.00"), "Dirección", estado = estado)
    }
    private fun dinero(esperado: String, actual: BigDecimal) = assertEquals(0, BigDecimal(esperado).compareTo(actual))

    @Test fun categoriaYSaborHistoricosNoAlteranElCalculoDeCaja() {
        val linea = linea("LifePood", "50.00", 2).copy(categoria = "Batería", sabor = "Fresa")
        val caja = calcularCaja(listOf(pedido(lineas = listOf(linea))))
        dinero("100.00", caja.productos)
        assertEquals("Fresa", caja.ventas("LifePod").single().productos.single().sabor)
        assertEquals("Batería", caja.ventas("LifePod").single().productos.single().categoria)
    }

    @Test fun lifePoodHistoricoSeEncuentraAlSeleccionarLifePod() {
        val historico = pedido(lineas = listOf(linea("LifePood", "50.00", 2)))
        val venta = calcularCaja(listOf(historico)).ventas("LifePod").single()
        assertEquals(historico, venta.pedido)
        assertEquals("LifePood", venta.productos.single().marca)
        dinero("100.00", venta.importe)
    }

    @Test fun lifePodHistoricoSeEncuentraAlSeleccionarLifePod() {
        val caja = calcularCaja(listOf(pedido(lineas = listOf(linea("LifePod", "50.00", 2)))))
        dinero("100.00", caja.ventas("LifePod").single().importe)
    }

    @Test fun selectorDeduplicaVariantesYConservaMarcasNuevasSinVentas() {
        val marcas = marcasDeCaja(listOf("LifePood", "LifePod", " lifepood ", "Oxbar", "Vaporesso", " "))
        assertEquals(listOf("LifePod", "Oxbar", "Vaporesso"), marcas)
        val caja = calcularCaja(listOf(pedido("a", lineas = listOf(linea("LifePood"))),
            pedido("b", lineas = listOf(linea("LifePod")))))
        assertEquals(listOf("LifePod"), caja.marcas)
        assertEquals(2, caja.ventas(marcas.first()).size)
    }

    @Test fun equivalenciaEsBidireccionalYConservaEspaciosYMayusculas() {
        val caja = calcularCaja(listOf(pedido(lineas = listOf(linea(" LifePod ")))))
        assertEquals(1, caja.ventas("  LIFEPOOD  ").size)
        assertEquals(1, caja.ventas("  lifepod  ").size)
    }

    @Test fun oxbarContinuaFiltrandoSinMezclarLifePod() {
        val caja = calcularCaja(listOf(pedido(lineas = listOf(linea("LifePood"), linea(" Oxbar ", "70.00", 1)))))
        val venta = caja.ventas(" OXBAR ").single()
        assertEquals(listOf(" Oxbar "), venta.productos.map { it.marca })
        dinero("70.00", venta.importe)
    }

    @Test fun mixtoLifePoodOxbarConservaTotalesDeliveryYSnapshots() {
        val historico = pedido(lineas = listOf(linea("LifePood", "50.00", 2), linea("Oxbar", "70.00", 1)))
        val caja = calcularCaja(listOf(historico, historico))
        dinero("100.00", caja.ventas("LifePod").single().importe)
        dinero("70.00", caja.ventas("Oxbar").single().importe)
        dinero("170.00", caja.ventas().single().importe)
        dinero("170.00", caja.productos)
        dinero("5.00", caja.delivery)
        dinero("175.00", caja.total)
        assertEquals(historico.productos, caja.ventas().single().productos)
    }

    @Test fun equivalenciaNoAceptaOtrasMarcasParecidas() {
        val distintas = listOf("LifePooood", "LifePod Plus", "Life Pod", "Oxbaar")
        val caja = calcularCaja(distintas.mapIndexed { i, marca -> pedido("p$i", lineas = listOf(linea(marca))) })
        assertTrue(caja.ventas("LifePod").isEmpty())
        assertTrue(caja.ventas("Oxbar").isEmpty())
        assertEquals(distintas.sorted(), marcasDeCaja(distintas))
    }

    @Test fun registradoNoSuma() { dinero("0", calcularCaja(listOf(pedido(estado = "REGISTRADO"))).total) }
    @Test fun enCaminoNoSuma() { dinero("0", calcularCaja(listOf(pedido(estado = "EN CAMINO"))).total) }
    @Test fun entregadoSiSuma() { dinero("140", calcularCaja(listOf(pedido())).productos) }
    @Test fun deliverySeparado() {
        val caja = calcularCaja(listOf(pedido()))
        dinero("140", caja.productos); dinero("5", caja.delivery); dinero("145", caja.total)
    }
    @Test fun variasVentas() { dinero("280", calcularCaja(listOf(pedido(), pedido("p2"))).productos) }
    @Test fun pedidosNoDuplicados() { assertEquals(1, calcularCaja(listOf(pedido(), pedido())).pedidos.size) }
    @Test fun marcasDinamicas() {
        assertEquals(setOf("Oxbar", "NuevaMarca"), calcularCaja(listOf(pedido(), pedido("p2", lineas = listOf(linea("NuevaMarca"))))).marcas.toSet())
    }
    @Test fun todosSinAgrupar() { assertEquals(2, calcularCaja(listOf(pedido(), pedido("p2"))).ventas().size) }
    @Test fun filtroMarca() {
        val caja = calcularCaja(listOf(pedido(), pedido("p2", lineas = listOf(linea("Nexa")))))
        assertEquals(listOf("p1"), caja.ventas("oxbar").map { it.pedido.id })
    }
    @Test fun mixtoUsaSoloLineasDeMarca() {
        val caja = calcularCaja(listOf(pedido(lineas = listOf(linea(), linea("Nexa", "40.00")))))
        assertEquals(1, caja.ventas("Oxbar").single().productos.size)
        dinero("140", caja.ventas("Oxbar").single().importe)
        dinero("80", caja.ventas("Nexa").single().importe)
    }
    @Test fun mixtoNoDuplicaDelivery() {
        val caja = calcularCaja(listOf(pedido(lineas = listOf(linea(), linea("Nexa")))))
        dinero("5", caja.delivery); dinero("285", caja.total)
    }
    @Test fun ordenDescendenteYFechaReal() {
        val antiguo = pedido("a", fecha = "2026-10-20T12:00:00Z")
        val reciente = pedido("b")
        val ventas = calcularCaja(listOf(antiguo, reciente)).ventas()
        assertEquals(listOf("b", "a"), ventas.map { it.pedido.id })
        assertEquals(reciente.fecha, ventas.first().pedido.fecha)
    }
    @Test fun sinCamposFinancieros() { assertEquals(null, calcularCaja(listOf(pedido())).pedidos.single().estadoPago) }
    @Test fun historicoSinCategoria() { dinero("140", calcularCaja(listOf(pedido(fecha = "2020-01-01T12:00:00Z"))).productos) }
    @Test fun noDependeDelCatalogo() {
        val venta = calcularCaja(listOf(pedido(lineas = listOf(linea(precio = "37.50"))))).ventas().single()
        assertEquals("Producto histórico", venta.productos.single().nombre); dinero("75", venta.importe)
    }
    @Test fun clienteNoAccede() {
        val usuario = Usuario(id = "cliente", rol = "CLIENTE")
        listOf("cajaTotal", "ventasMarca").forEach {
            assertEquals("home", pantallaRestauradaSegura(it, usuario, "cliente:CLIENTE"))
        }
    }
    @Test fun adminAccede() {
        val usuario = Usuario(id = "admin", rol = "ADMIN")
        assertEquals("cajaTotal", pantallaRestauradaSegura("cajaTotal", usuario, "admin:ADMIN"))
    }
    @Test fun retornoConservaOrigenYListado() {
        val caja = calcularCaja(listOf(pedido()))
        val marca = "Oxbar"
        assertEquals("ventasMarca", rutaRetornoDetalleCaja(true))
        assertEquals("pedidosAdmin", rutaRetornoDetalleCaja(false))
        assertEquals(listOf("p1"), caja.ventas(marca).map { it.pedido.id })
    }
    @Test fun recargarNoAcumula() {
        val pedidos = listOf(pedido())
        assertEquals(calcularCaja(pedidos), calcularCaja(pedidos))
        dinero("140", calcularCaja(pedidos).productos)
    }
    @Test fun cambioAEntregadoAparece() {
        val original = pedido(estado = "REGISTRADO")
        assertTrue(calcularCaja(listOf(original)).pedidos.isEmpty())
        assertEquals(1, calcularCaja(listOf(original.copy(estado = "ENTREGADO"))).pedidos.size)
    }
    @Test fun cambioDesdeEntregadoDesaparece() {
        val original = pedido()
        assertEquals(1, calcularCaja(listOf(original)).pedidos.size)
        assertTrue(calcularCaja(listOf(original.copy(estado = "EN CAMINO"))).pedidos.isEmpty())
    }
    @Test fun rangoInclusivoLima() {
        val inicio = pedido("a", fecha = "2026-10-29T05:00:00Z")
        val final = pedido("b", fecha = "2026-10-30T04:59:59Z")
        val fuera = pedido("c", fecha = "2026-10-30T05:00:00Z")
        assertEquals(2, calcularCaja(listOf(inicio, final, fuera), "29/10/2026", "29/10/2026").pedidos.size)
    }
    @Test fun rangoInvalidoSeRechaza() {
        assertThrows(IllegalArgumentException::class.java) { calcularCaja(emptyList(), "30/10/2026", "29/10/2026") }
    }
    @Test fun montosInconsistentesSeAdvierten() {
        val caja = calcularCaja(listOf(pedido().copy(total = BigDecimal("1.00"))))
        assertEquals(1, caja.invalidos); assertTrue(caja.pedidos.isEmpty())
    }
    @Test fun montoPagadoYVueltoNoSeUsan() { dinero("145", calcularCaja(listOf(pedido())).total) }
    @Test fun nuevaSesionNoRestauraRutaAjena() {
        assertEquals("admin", pantallaRestauradaSegura("ventasMarca", Usuario(id = "B", rol = "ADMIN"), "A:ADMIN"))
    }
}
