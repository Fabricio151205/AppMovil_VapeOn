package com.example.vapeon_movil

import com.example.vapeon_movil.entities.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class PronosticoDemandaTest {
    private fun linea(id: String = "prod-1", nombre: String = "Producto", cantidad: Int = 1) =
        ProductoPedido(id, nombre, "Marca histórica", cantidad, BigDecimal.TEN,
            BigDecimal.TEN * BigDecimal(cantidad))

    private fun pedido(id: String, fecha: String, estado: String = "ENTREGADO",
        productos: List<ProductoPedido> = listOf(linea())) = Pedido(
        id, "usuario", "Cliente", "999999999", fecha, productos,
        productos.fold(BigDecimal.ZERO) { total, item -> total + item.subtotal }, BigDecimal("5.00"),
        productos.fold(BigDecimal("5.00")) { total, item -> total + item.subtotal },
        BigDecimal("100.00"), BigDecimal.ZERO, "Dirección", estado = estado
    )

    private fun serieSuficiente(): SerieDemandaProducto {
        val inicio = LocalDate.of(2026, 1, 1)
        return SerieDemandaProducto("p", "Producto", "Marca", (0 until 20).map {
            ObservacionDemanda(inicio.plusDays(it.toLong()), if (it % 3 == 0) 3.0 else 1.0)
        })
    }

    @Test fun serieDiariaCorrecta() {
        val serie = construirSeriesDemanda(listOf(
            pedido("a", "2026-01-01T15:00:00Z"), pedido("b", "2026-01-02T15:00:00Z")
        )).single()
        assertEquals(listOf(1.0, 1.0), serie.observaciones.map { it.unidades })
    }

    @Test fun diasSinVentasSeRellenanConCero() {
        val serie = construirSeriesDemanda(listOf(
            pedido("a", "2026-01-01T15:00:00Z"), pedido("b", "2026-01-03T15:00:00Z")
        )).single()
        assertEquals(listOf(1.0, 0.0, 1.0), serie.observaciones.map { it.unidades })
    }

    @Test fun variasVentasMismoDiaSeAcumulan() {
        val serie = construirSeriesDemanda(listOf(
            pedido("a", "2026-01-01T10:00:00Z", productos = listOf(linea(cantidad = 2))),
            pedido("b", "2026-01-01T20:00:00Z", productos = listOf(linea(cantidad = 3)))
        )).single()
        assertEquals(5.0, serie.observaciones.single().unidades, 0.0)
    }

    @Test fun agrupaExclusivamentePorProductoId() {
        val series = construirSeriesDemanda(listOf(pedido("a", "2026-01-01T15:00:00Z",
            productos = listOf(linea("uno", "Mismo nombre"), linea("dos", "Mismo nombre")))))
        assertEquals(setOf("uno", "dos"), series.map { it.productoId }.toSet())
    }

    @Test fun pedidoDuplicadoNoSeContabilizaDosVeces() {
        val repetido = pedido("mismo", "2026-01-01T15:00:00Z", productos = listOf(linea(cantidad = 2)))
        assertEquals(2.0, construirSeriesDemanda(listOf(repetido, repetido)).single().observaciones.single().unidades, 0.0)
    }

    @Test fun excluyeEstadosDistintosDeEntregado() {
        val series = construirSeriesDemanda(listOf(
            pedido("r", "2026-01-01T15:00:00Z", "REGISTRADO"),
            pedido("c", "2026-01-02T15:00:00Z", "EN CAMINO")
        ))
        assertTrue(series.isEmpty())
    }

    @Test fun menosDeCatorceObservacionesEsInsuficiente() {
        val serie = serieSuficiente().copy(observaciones = serieSuficiente().observaciones.take(13))
        assertEquals(EstadoPronosticoDemanda.DATOS_INSUFICIENTES, generarPronosticoDemanda(serie).estado)
    }

    @Test fun menosDeTresDiasConDemandaEsInsuficiente() {
        val inicio = LocalDate.of(2026, 1, 1)
        val serie = SerieDemandaProducto("p", "P", "M", (0 until 14).map {
            ObservacionDemanda(inicio.plusDays(it.toLong()), if (it in listOf(0, 13)) 1.0 else 0.0)
        })
        assertEquals(EstadoPronosticoDemanda.DATOS_INSUFICIENTES, generarPronosticoDemanda(serie).estado)
    }

    @Test fun separaEntrenamientoYValidacionCronologicamente() {
        val resultado = generarPronosticoDemanda(serieSuficiente())
        assertEquals(EstadoPronosticoDemanda.DISPONIBLE, resultado.estado)
        assertEquals(16, resultado.observacionesEntrenamiento)
        assertEquals(4, resultado.observacionesValidacion)
        assertEquals(serieSuficiente().observaciones.takeLast(4).map { it.fecha }, resultado.validacion.map { it.fecha })
    }

    @Test fun maeEsCorrecto() {
        assertEquals(1.0, calcularMae(listOf(1.0, 2.0, 3.0), listOf(2.0, 2.0, 1.0)), 0.000001)
    }

    @Test fun rmseEsCorrecto() {
        assertEquals(kotlin.math.sqrt(5.0 / 3.0),
            calcularRmse(listOf(1.0, 2.0, 3.0), listOf(2.0, 2.0, 1.0)), 0.000001)
    }

    @Test fun holtProducePronosticoNoNegativoYParametrosEntrenados() {
        val resultado = generarPronosticoDemanda(serieSuficiente(), 14)
        assertTrue(resultado.futuro.all { it.unidades >= 0.0 })
        assertTrue(resultado.alpha!! in 0.05..0.95)
        assertTrue(resultado.beta!! in 0.05..0.95)
    }

    @Test fun noGeneraPronosticoCuandoEsInsuficiente() {
        val resultado = generarPronosticoDemanda(SerieDemandaProducto("p", "P", "M",
            listOf(ObservacionDemanda(LocalDate.of(2026, 1, 1), 2.0))))
        assertTrue(resultado.futuro.isEmpty())
        assertTrue(resultado.validacion.isEmpty())
        assertNull(resultado.mae)
    }

    @Test fun productoHistoricoNoDependeDelCatalogoActual() {
        val serie = construirSeriesDemanda(listOf(pedido("a", "2026-01-01T15:00:00Z",
            productos = listOf(linea("eliminado", "Producto histórico"))))).single()
        assertEquals("eliminado", serie.productoId)
        assertEquals("Producto histórico", serie.nombre)
    }

    @Test fun productoPruebasNoEsEliminadoAutomaticamente() {
        val serie = construirSeriesDemanda(listOf(pedido("a", "2026-01-01T15:00:00Z",
            productos = listOf(linea("test-id", "pruebas"))))).single()
        assertEquals("pruebas", serie.nombre)
    }

    @Test fun variasLineasMismoProductoEnPedidoSeSuman() {
        val serie = construirSeriesDemanda(listOf(pedido("a", "2026-01-01T15:00:00Z",
            productos = listOf(linea(cantidad = 2), linea(cantidad = 4))))).single()
        assertEquals(6.0, serie.observaciones.single().unidades, 0.0)
    }

    @Test fun convierteFechaAZonaHorariaDeLima() {
        val serie = construirSeriesDemanda(listOf(pedido("a", "2026-10-15T04:30:00Z"))).single()
        assertEquals(LocalDate.of(2026, 10, 14), serie.observaciones.single().fecha)
    }

    @Test fun tramoDeEntrenamientoSinDemandaSuficienteNoProduceModelo() {
        val inicio = LocalDate.of(2026, 1, 1)
        val serie = SerieDemandaProducto("p", "P", "M", (0 until 20).map {
            ObservacionDemanda(inicio.plusDays(it.toLong()), if (it >= 17) 1.0 else 0.0)
        })
        assertEquals(EstadoPronosticoDemanda.DATOS_INSUFICIENTES, generarPronosticoDemanda(serie).estado)
    }
}
