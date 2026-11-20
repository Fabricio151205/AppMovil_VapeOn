package com.example.vapeon_movil

import com.example.vapeon_movil.entities.*
import com.example.vapeon_movil.services.PedidoService
import com.example.vapeon_movil.services.ProductoService
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.viewmodels.NuevaVentaViewModel
import com.example.vapeon_movil.viewmodels.ProductoViewModel
import kotlinx.coroutines.Dispatchers
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.math.BigDecimal

class NuevaVentaTest {
    private val producto = Producto("p1", "Producto", "Marca", 10.0, 5, "Descripción")
    private val linea = LineaVentaAdmin("p1", "Producto", "Marca", BigDecimal("10.00"), 2)

    private fun pedido(delivery: Boolean = false): Pedido = construirPedidoVentaAdmin(
        "venta", "admin", "Cliente", if (delivery) "987654321" else "",
        "2026-11-19T12:00:00Z", listOf(linea), delivery, if (delivery) "Calle 1" else ""
    )

    @Test fun ventaEnTiendaSeLeeSinTelefonoYEntraEnCaja() {
        val pedido = pedido()
        val leido = pedido.aDocumento("projects/test/databases/(default)/documents").aPedido()
        assertEquals(pedido, leido)
        assertEquals("ENTREGADO", leido.estado)
        assertEquals(BigDecimal("0.00"), leido.delivery)
        assertEquals("admin", leido.registradoPor)
        assertEquals(BigDecimal("20.00"), calcularCaja(listOf(leido)).total)
    }

    @Test fun ventaConDeliveryQuedaPendienteYNoSumaEnCaja() {
        val pedido = pedido(true)
        assertEquals("REGISTRADO", pedido.estado)
        assertEquals(BigDecimal("25.00"), pedido.total)
        assertEquals(0, calcularCaja(listOf(pedido)).total.signum())
    }

    @Test fun deliveryExigeDatosYElMontoDebeCubrirElTotal() {
        assertNotNull(errorVentaAdmin("Cliente", "", true, "Calle", listOf(linea), ""))
        assertNotNull(errorVentaAdmin("Cliente", "987654321", true, "", listOf(linea), ""))
        assertNotNull(errorVentaAdmin("Cliente", "123", false, "", listOf(linea), ""))
        assertNotNull(errorVentaAdmin("Cliente", "", false, "", listOf(linea), "19.99"))
    }

    @Test fun clienteNormalNoPuedePrepararUnaVenta() {
        val vm = NuevaVentaViewModel(ServicioPedidos(), Dispatchers.Unconfined)
        vm.actualizarAdmin(Usuario(id = "cliente", rol = "CLIENTE"))
        assertNull(vm.adminId)
    }

    @Test fun laRutaDeVentaSoloSeRestauraParaLaSesionAdministrativa() {
        val admin = Usuario(id = "admin", rol = "ADMIN")
        val cliente = Usuario(id = "cliente", rol = "CLIENTE")
        assertEquals("nuevaVenta", pantallaRestauradaSegura("nuevaVenta", admin, claveSesionNavegacion(admin)))
        assertEquals("home", pantallaRestauradaSegura("nuevaVenta", cliente, claveSesionNavegacion(cliente)))
        assertEquals("login", pantallaRestauradaSegura("nuevaVenta", null, null))
    }

    @Test fun respuestaPerdidaSeRecuperaSinDuplicarStockNiPedido() {
        val servicio = ServicioPedidos()
        val productos = ProductoViewModel(ServicioProductos(), Dispatchers.Unconfined)
        val vm = NuevaVentaViewModel(servicio, Dispatchers.Unconfined)
        vm.actualizarAdmin(Usuario(id = "admin", rol = "ADMIN"))
        vm.actualizarNombreCliente("Cliente")
        vm.agregarProducto(producto)
        vm.confirmar(productos)
        assertTrue(vm.requiereComprobacion)
        assertEquals(1, servicio.commits.size)
        assertEquals(2, servicio.commits.single().writes.size)
        assertEquals(listOf("stock"), servicio.commits.single().writes.last().updateMask?.fieldPaths)
        assertEquals("4", servicio.commits.single().writes.last().update.fields["stock"]?.integerValue)
        vm.actualizarNombreCliente("No debe cambiar")
        assertEquals("Cliente", vm.nombreCliente)
        vm.confirmar(productos)
        assertFalse(vm.requiereComprobacion)
        assertNotNull(vm.ventaRegistrada)
        assertEquals(1, servicio.commits.size)
    }

    @Test fun pagoConTarjetaValidaTitularDigitosYVencimiento() {
        fun error(
            titular: String = "Juan Perez",
            ultimos4: String = "1234",
            vencimiento: String = "12/30",
            metodo: String = "TARJETA"
        ) = errorVentaAdmin("Cliente", "", false, "", listOf(linea), "",
            metodo, titular, ultimos4, vencimiento)

        assertNull(error())
        assertNotNull(error(vencimiento = "01/20"))
        assertNotNull(error(ultimos4 = "123"))
        assertNotNull(error(titular = " "))
        assertNotNull(error(metodo = "TRANSFERENCIA"))
        assertNotNull(leerVencimientoTarjeta("12/30"))
        assertNull(leerVencimientoTarjeta("13/30"))
    }

    @Test fun ventaConTarjetaNoGuardaDatosSensiblesEnElPedido() {
        val pedido = construirPedidoVentaAdmin(
            "venta-tarjeta", "admin", "Cliente", "", "2026-11-19T12:00:00Z",
            listOf(linea), false, "", metodoPago = "TARJETA",
            titularTarjeta = "Juan Perez", ultimos4Tarjeta = "1234", vencimientoTarjeta = "12/30"
        )
        assertEquals("TARJETA", pedido.metodoPago)
        val campos = pedido.aDocumento("projects/test/databases/(default)/documents").fields
        assertNull(campos["titularTarjeta"])
        assertNull(campos["ultimos4Tarjeta"])
        assertNull(campos["vencimientoTarjeta"])
    }

    @Test fun validacionSenalaCadaCampoDelFormulario() {
        val errores = validarVentaAdmin(
            nombreCliente = "A", telefono = "123", conDelivery = true,
            direccion = "x", lineas = emptyList(), montoTexto = "1",
            metodoPago = "TARJETA", titularTarjeta = "", ultimos4Tarjeta = "", vencimientoTarjeta = ""
        )
        assertNotNull(errores.nombre)
        assertNotNull(errores.telefono)
        assertNotNull(errores.direccion)
        assertNotNull(errores.productos)
        assertNotNull(errores.titularTarjeta)
        assertNotNull(errores.ultimos4Tarjeta)
        assertNotNull(errores.vencimientoTarjeta)
        assertTrue(errores.hayErrores())
    }

    @Test fun elViewModelNormalizaLosDatosDeTarjeta() {
        val vm = NuevaVentaViewModel(ServicioPedidos(), Dispatchers.Unconfined)
        vm.actualizarAdmin(Usuario(id = "admin", rol = "ADMIN"))
        vm.actualizarMetodoPago("TARJETA")
        assertEquals("TARJETA", vm.metodoPago)
        vm.actualizarUltimos4Tarjeta("12ab34")
        assertEquals("1234", vm.ultimos4Tarjeta)
        vm.actualizarVencimientoTarjeta("1230")
        assertEquals("12/30", vm.vencimientoTarjeta)
        vm.actualizarTitularTarjeta("Juan Perez")
        assertEquals("Juan Perez", vm.titularTarjeta)
    }

    private class ServicioPedidos : PedidoService {
        val commits = mutableListOf<PedidoCommit>()
        private var documento: PedidoDocumento? = null
        override suspend fun confirmarPedido(body: PedidoCommit): PedidoCommitRespuesta {
            commits += body
            documento = body.writes.first().update
            throw IOException("Respuesta perdida después de guardar")
        }
        override suspend fun mostrarPedido(id: String): PedidoDocumento = documento
            ?: throw HttpException(Response.error<Any>(404, ResponseBody.create(null, "{}")))
        override suspend fun consultarPedidos(body: PedidoConsulta): List<PedidoConsultaRespuesta> = emptyList()
        override suspend fun actualizarEstadoPedido(id: String, body: PedidoEstadoActualizacion,
            updateTime: String, campo: String): PedidoDocumento = error("No usado")
    }

    private class ServicioProductos : ProductoService {
        private val producto = ProductoDocumento(
            "projects/test/databases/(default)/documents/productos/p1",
            ProductoCampos(nombre = FirestoreValue(stringValue = "Producto"),
                marca = FirestoreValue(stringValue = "Marca"), precio = FirestoreValue(doubleValue = 10.0),
                stock = FirestoreValue(integerValue = "5")), "2026-11-19T12:00:00Z"
        )
        override suspend fun mostrarProducto(id: String) = producto
        override suspend fun listarProductos(pageToken: String?) = ProductoListResponse(listOf(producto))
        override suspend fun crearProducto(body: ProductoContainer, documentId: String?): Any = error("No usado")
        override suspend fun actualizarProducto(id: String, body: ProductoContainer, updateTime: String,
            campos: List<String>): ProductoDocumento = error("No usado")
        override suspend fun eliminarProducto(id: String): Any = error("No usado")
    }
}
