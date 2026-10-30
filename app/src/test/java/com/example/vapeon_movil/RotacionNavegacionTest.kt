package com.example.vapeon_movil

import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.entities.Producto
import com.example.vapeon_movil.viewmodels.ProductoViewModel
import org.junit.Assert.*
import org.junit.Test

class RotacionNavegacionTest {
    private val cliente = Usuario(id = "cliente1", rol = "CLIENTE")
    private val admin = Usuario(id = "admin1", rol = "ADMIN")
    private val rutasCliente = listOf("home", "catalogo", "detalle", "carrito", "checkout",
        "historialCompras", "detallePedido", "tarjetas") + RUTAS_COMPRA + RUTAS_REGISTRO_TARJETA
    private val rutasAdmin = listOf("admin", "catalogo", "detalle", "agregarProducto",
        "editarProducto", "pedidosAdmin", "detallePedidoAdmin")

    @Test fun conservaTodasLasRutasDelClienteConLaMismaSesion() {
        rutasCliente.forEach { ruta ->
            assertEquals(ruta, pantallaRestauradaSegura(ruta, cliente, claveSesionNavegacion(cliente)))
        }
    }

    @Test fun conservaTodasLasRutasDelAdminConLaMismaSesion() {
        rutasAdmin.forEach { ruta ->
            assertEquals(ruta, pantallaRestauradaSegura(ruta, admin, claveSesionNavegacion(admin)))
        }
    }

    @Test fun rutaGuardadaSinSesionNoConcedeAcceso() {
        (rutasCliente + rutasAdmin).distinct().forEach { ruta ->
            assertEquals("login", pantallaRestauradaSegura(ruta, null, claveSesionNavegacion(admin)))
            assertEquals("login", pantallaRestauradaSegura(ruta, null, claveSesionNavegacion(cliente)))
        }
    }

    @Test fun noRestauraPantallasDeOtroRol() {
        listOf("admin", "agregarProducto", "editarProducto", "pedidosAdmin", "detallePedidoAdmin").forEach {
            assertEquals("home", pantallaRestauradaSegura(it, cliente, claveSesionNavegacion(cliente)))
        }
        rutasCliente.filter { it !in setOf("catalogo", "detalle") }.forEach {
            assertEquals("admin", pantallaRestauradaSegura(it, admin, claveSesionNavegacion(admin)))
        }
    }

    @Test fun seleccionDeOtraSesionNoAbreDetalleNiEdicion() {
        val otroCliente = cliente.copy(id = "cliente2")
        val otroAdmin = admin.copy(id = "admin2")
        listOf("detalle", "detallePedido", "checkout").forEach {
            assertEquals("home", pantallaRestauradaSegura(it, otroCliente, claveSesionNavegacion(cliente)))
        }
        listOf("detalle", "editarProducto", "detallePedidoAdmin").forEach {
            assertEquals("admin", pantallaRestauradaSegura(it, otroAdmin, claveSesionNavegacion(admin)))
        }
        assertEquals("home", pantallaRestauradaSegura("detalle", cliente, null))
        val rolCambiado = admin.copy(rol = "CLIENTE")
        assertEquals("home", pantallaRestauradaSegura("detalle", rolCambiado, claveSesionNavegacion(admin)))
    }

    @Test fun usuarioInvalidoYRutaDesconocidaTienenSalidaSegura() {
        listOf(cliente.copy(id = ""), cliente.copy(rol = "OTRO")).forEach {
            assertNull(claveSesionNavegacion(it))
            assertEquals("login", pantallaRestauradaSegura("catalogo", it, "anterior"))
        }
        assertEquals("home", pantallaRestauradaSegura("rutaInexistente", cliente, claveSesionNavegacion(cliente)))
        assertEquals("admin", pantallaRestauradaSegura("rutaInexistente", admin, claveSesionNavegacion(admin)))
        assertEquals("registro", pantallaRestauradaSegura("registro", null, null))
    }

    @Test fun propietarioGuardadoSoloContieneIdYRol() {
        val usuario = cliente.copy(nombre = "Nombre privado", telefono = "987654321",
            correo = "privado@example.com", password = "secreto")
        assertEquals("cliente1:CLIENTE", claveSesionNavegacion(usuario))
        assertEquals(claveSesionNavegacion(cliente), claveSesionNavegacion(usuario))
    }

    @Test fun productoSeleccionadoSeMantieneParaLaMismaSesion() {
        val vm = ProductoViewModel()
        val producto = Producto("p1", "Producto", "Marca", 70.0, 5, "Descripción")
        val propietario = claveSesionNavegacion(cliente).orEmpty()
        vm.seleccionarProductoParaRuta(propietario, producto)
        vm.actualizarSesionProducto(propietario)
        vm.recuperarProductoParaRuta(propietario, producto.id)
        assertSame(producto, vm.productoParaRuta)
        assertFalse(vm.cargandoProductoRuta)
        assertNull(vm.errorProductoRuta)
    }

    @Test fun cambiarDeUsuarioOInvalidarSesionDescartaProductoAnterior() {
        val vm = ProductoViewModel()
        val producto = Producto("p1", "Producto", "Marca", 70.0, 5, "Descripción")
        vm.seleccionarProductoParaRuta(claveSesionNavegacion(cliente).orEmpty(), producto)
        vm.actualizarSesionProducto(claveSesionNavegacion(cliente.copy(id = "cliente2")))
        assertNull(vm.productoParaRuta)
        assertEquals("cliente2:CLIENTE", vm.propietarioProductoRuta)
        vm.seleccionarProductoParaRuta(claveSesionNavegacion(admin).orEmpty(), producto)
        vm.actualizarSesionProducto(null)
        assertNull(vm.productoParaRuta)
        assertNull(vm.propietarioProductoRuta)
    }

    @Test fun productoSinIdValidoPresentaErrorControlado() {
        val vm = ProductoViewModel()
        listOf("", "productos/p1").forEach { id ->
            vm.recuperarProductoParaRuta(claveSesionNavegacion(cliente).orEmpty(), id)
            assertNull(vm.productoParaRuta)
            assertNotNull(vm.errorProductoRuta)
            assertFalse(vm.cargandoProductoRuta)
        }
    }
}
