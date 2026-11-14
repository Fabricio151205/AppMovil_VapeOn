package com.example.vapeon_movil

import com.example.vapeon_movil.entities.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class EstadisticasTest {
    private fun linea(
        productoId: String = "p1",
        nombre: String = "Producto histórico",
        marca: String = "Oxbar",
        precio: String = "70.00",
        cantidad: Int = 2,
        categoria: String? = "Pods"
    ) = ProductoPedido(
        productoId,
        nombre,
        marca,
        cantidad,
        BigDecimal(precio),
        BigDecimal(precio) * BigDecimal(cantidad),
        categoria
    )

    private fun pedido(
        id: String = "p1",
        fecha: String = "2026-10-29T12:00:00Z",
        estado: String = "ENTREGADO",
        metodoPago: String = "EFECTIVO",
        usuarioId: String = "cliente-1",
        lineas: List<ProductoPedido> = listOf(linea())
    ): Pedido {
        val subtotal = lineas.fold(BigDecimal.ZERO) { suma, item -> suma + item.subtotal }
        return Pedido(
            id = id,
            usuarioId = usuarioId,
            nombreCliente = "Cliente",
            telefono = "987654321",
            fecha = fecha,
            productos = lineas,
            subtotal = subtotal,
            delivery = BigDecimal("5.00"),
            total = subtotal + BigDecimal("5.00"),
            montoPagado = BigDecimal("1000.00"),
            vuelto = BigDecimal("855.00"),
            direccionEntrega = "Dirección",
            metodoPago = metodoPago,
            estado = estado
        )
    }

    private fun dinero(esperado: String, actual: BigDecimal) =
        assertEquals(0, BigDecimal(esperado).compareTo(actual))

    @Test
    fun rangosDeDiaMesYAnioSonInclusivos() {
        val ancla = LocalDate.of(2026, 10, 29)
        assertEquals(RangoEstadistica(ancla, ancla), rangoEstadistica(PeriodoEstadistica.DIA, ancla))
        assertEquals(
            RangoEstadistica(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)),
            rangoEstadistica(PeriodoEstadistica.MES, ancla)
        )
        assertEquals(
            RangoEstadistica(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)),
            rangoEstadistica(PeriodoEstadistica.ANIO, ancla)
        )
    }

    @Test
    fun diaSoloIncluyeEntregadosDelDiaLocalLima() {
        val pedidos = listOf(
            pedido("a", "2026-10-29T12:00:00Z"),
            pedido("b", "2026-10-29T12:00:00Z", estado = "REGISTRADO"),
            pedido("c", "2026-10-30T12:00:00Z"),
            pedido("d", "2026-10-30T04:59:59Z")
        )
        val estadisticas = calcularEstadisticas(
            pedidos,
            PeriodoEstadistica.DIA,
            LocalDate.of(2026, 10, 29)
        )
        assertEquals(2, estadisticas.pedidos)
        assertEquals(4, estadisticas.unidades)
        dinero("290.00", estadisticas.total)
        dinero("145.00", estadisticas.ticketPromedio)
        assertEquals(1, estadisticas.clientes)
        assertEquals(4, estadisticas.serie.size)
    }

    @Test
    fun mesAgrupaPorDiaYCalculaPromedioMejorDiaYSerieCompleta() {
        val pedidos = listOf(
            pedido("a", "2026-10-01T12:00:00Z",
                lineas = listOf(linea(cantidad = 1, precio = "10.00"))),
            pedido("b", "2026-10-15T12:00:00Z",
                lineas = listOf(linea(cantidad = 2, precio = "20.00"))),
            pedido("c", "2026-10-15T18:00:00Z",
                lineas = listOf(linea(cantidad = 3, precio = "30.00")))
        )
        val estadisticas = calcularEstadisticas(
            pedidos,
            PeriodoEstadistica.MES,
            LocalDate.of(2026, 10, 15)
        )
        assertEquals(31, estadisticas.serie.size)
        assertEquals(3, estadisticas.pedidos)
        assertEquals(6, estadisticas.unidades)
        dinero("155.00", estadisticas.total)
        dinero("77.50", estadisticas.promedio!!)
        assertEquals("15", estadisticas.mejorPunto?.etiqueta)
        dinero("140.00", estadisticas.mejorPunto!!.total)
        dinero("15.00", estadisticas.serie[0].total)
        assertEquals(2, estadisticas.serie[14].pedidos)
        dinero("140.00", estadisticas.serie[14].total)
        assertEquals(0, estadisticas.serie[30].pedidos)
    }

    @Test
    fun anioAgrupaPorMesYRespetaElCambioDeDiaEnLima() {
        val pedidos = listOf(
            pedido("antes", "2026-01-01T04:59:59Z"),
            pedido("inicio", "2026-01-01T05:00:00Z"),
            pedido("diciembre", "2026-12-31T12:00:00Z")
        )
        val estadisticas = calcularEstadisticas(
            pedidos,
            PeriodoEstadistica.ANIO,
            LocalDate.of(2026, 6, 1)
        )
        assertEquals(2, estadisticas.pedidos)
        assertEquals(12, estadisticas.serie.size)
        assertEquals("Ene", estadisticas.serie[0].etiqueta)
        assertEquals("Dic", estadisticas.serie[11].etiqueta)
        dinero("145.00", estadisticas.serie[0].total)
        dinero("145.00", estadisticas.serie[11].total)
    }

    @Test
    fun variacionComparaConElPeriodoAnterior() {
        val pedidos = listOf(
            pedido("anterior", "2026-09-15T12:00:00Z"),
            pedido("actual1", "2026-10-10T12:00:00Z"),
            pedido("actual2", "2026-10-11T12:00:00Z")
        )
        val estadisticas = calcularEstadisticas(
            pedidos,
            PeriodoEstadistica.MES,
            LocalDate.of(2026, 10, 15)
        )
        dinero("145.00", estadisticas.totalAnterior)
        dinero("290.00", estadisticas.total)
        dinero("100.0", estadisticas.variacionPorcentual!!)
    }

    @Test
    fun agrupaMarcasCategoriasYMetodosDePagoSinMezclarSnapshots() {
        val pedidos = listOf(
            pedido("a", "2026-10-10T12:00:00Z", metodoPago = "YAPE",
                lineas = listOf(linea(productoId = "p1", nombre = "Pod", marca = "LifePood",
                    categoria = "Pods", cantidad = 2, precio = "50.00"))),
            pedido("b", "2026-10-11T12:00:00Z", metodoPago = "TARJETA",
                lineas = listOf(linea(productoId = "p2", nombre = "Kit", marca = "Oxbar",
                    categoria = "Kits", cantidad = 1, precio = "70.00")))
        )
        val estadisticas = calcularEstadisticas(
            pedidos,
            PeriodoEstadistica.MES,
            LocalDate.of(2026, 10, 15)
        )
        assertEquals(setOf("LifePood", "Oxbar"), estadisticas.marcas.map { it.marca }.toSet())
        assertEquals(setOf("Pods", "Kits"), estadisticas.categorias.map { it.categoria }.toSet())
        assertEquals(setOf("YAPE", "TARJETA"), estadisticas.metodosPago.map { it.metodo }.toSet())
        assertEquals(2, estadisticas.metodosPago.sumOf { it.pedidos })
        dinero("105.00", estadisticas.metodosPago.first { it.metodo == "YAPE" }.total)
        dinero("75.00", estadisticas.metodosPago.first { it.metodo == "TARJETA" }.total)
        assertEquals("Pod", estadisticas.topProductos.first().nombre)
    }

    @Test
    fun periodoSinVentasDevuelveCerosYSerieCompleta() {
        val estadisticas = calcularEstadisticas(
            emptyList(),
            PeriodoEstadistica.MES,
            LocalDate.of(2026, 10, 15)
        )
        assertEquals(0, estadisticas.pedidos)
        dinero("0", estadisticas.total)
        assertEquals(31, estadisticas.serie.size)
        assertTrue(estadisticas.serie.all { it.total.signum() == 0 })
        assertNull(estadisticas.mejorPunto)
        assertNull(estadisticas.promedio)
        assertNull(estadisticas.variacionPorcentual)
    }

    @Test
    fun pedidosInvalidosSeExcluyenYSeContabilizan() {
        val invalido = pedido("malo").copy(total = BigDecimal("1.00"))
        val estadisticas = calcularEstadisticas(
            listOf(pedido("bueno"), invalido),
            PeriodoEstadistica.DIA,
            LocalDate.of(2026, 10, 29)
        )
        assertEquals(1, estadisticas.pedidos)
        assertEquals(1, estadisticas.caja.invalidos)
    }

    @Test
    fun clienteNoAccedeYAdminSiALaRutaDeEstadisticas() {
        val cliente = Usuario(id = "cliente", rol = "CLIENTE")
        val admin = Usuario(id = "admin", rol = "ADMIN")
        assertEquals("home", pantallaRestauradaSegura("estadisticas", cliente, "cliente:CLIENTE"))
        assertEquals("estadisticas", pantallaRestauradaSegura("estadisticas", admin, "admin:ADMIN"))
    }
}
