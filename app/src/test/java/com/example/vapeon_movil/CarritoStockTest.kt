package com.example.vapeon_movil

import com.example.vapeon_movil.entities.ItemCarrito
import com.example.vapeon_movil.entities.Producto
import com.example.vapeon_movil.viewmodels.CarritoViewModel
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class CarritoStockTest {
    private val producto = Producto("p1", "Producto", "Marca", 70.9, 3, "Descripción")

    @Test fun falloDeLimpiezaBloqueaOtraCompraHastaConfirmarEliminacionLocal() {
        var permiteEliminar = false
        val vm = CarritoViewModel { permiteEliminar }
        vm.agregarProducto(producto)
        assertFalse(vm.limpiarCarrito())
        assertTrue(vm.limpiezaPendiente)
        vm.agregarProducto(producto)
        assertTrue(vm.items.isEmpty())
        assertFalse(vm.puedeComprar)
        permiteEliminar = true
        assertTrue(vm.limpiarCarrito())
        assertFalse(vm.limpiezaPendiente)
    }

    @Test fun noAgregaAgotados() {
        val vm = CarritoViewModel()
        vm.agregarProducto(producto.copy(stock = 0))
        assertTrue(vm.items.isEmpty())
        assertNotNull(vm.aviso)
    }

    @Test fun agregarEIncrementarNoSuperanStockConocido() {
        val vm = CarritoViewModel()
        repeat(5) { vm.agregarProducto(producto) }
        vm.incrementar("p1")
        assertEquals(3, vm.items.single().cantidad)
        assertTrue(vm.puedeComprar)
    }

    @Test fun disminuirConservaMinimoUnoYEliminarEsExplicito() {
        val vm = CarritoViewModel()
        vm.agregarProducto(producto)
        vm.decrementar("p1")
        assertEquals(1, vm.items.single().cantidad)
        vm.eliminarItem("p1")
        assertTrue(vm.items.isEmpty())
    }

    @Test fun restauracionCorrigeCantidadExcesivaYNegativa() {
        val vm = CarritoViewModel()
        val item = ItemCarrito("p1", "Producto", "Marca", 70.9, 7, stockDisponible = 3)
        vm.restaurarItems(Gson().toJson(listOf(item)))
        assertEquals(3, vm.items.single().cantidad)
        vm.restaurarItems(Gson().toJson(listOf(item.copy(cantidad = -1))))
        assertEquals(1, vm.items.single().cantidad)
    }

    @Test fun inventarioFrescoCorrigeStockRestauradoAntiguo() {
        val vm = CarritoViewModel()
        vm.restaurarItems(Gson().toJson(listOf(ItemCarrito("p1", "Producto", "Marca", 70.9, 7))))
        assertFalse(vm.puedeComprar)
        vm.actualizarInventario(listOf(producto))
        assertEquals(3, vm.items.single().cantidad)
        assertTrue(vm.puedeComprar)
    }

    @Test fun productosEliminadosOAgotadosSeRetiranConAviso() {
        val vm = CarritoViewModel()
        vm.agregarProducto(producto)
        vm.actualizarInventario(listOf(producto.copy(stock = 0)))
        assertTrue(vm.items.isEmpty())
        assertNotNull(vm.aviso)
        vm.agregarProducto(producto.copy(id = "p2"))
        vm.actualizarInventario(emptyList())
        assertTrue(vm.items.isEmpty())
    }

    @Test fun jsonCorruptoDuplicadosYDatosInvalidosNoRompenCarrito() {
        val vm = CarritoViewModel()
        vm.restaurarItems("{roto")
        assertTrue(vm.items.isEmpty())
        val item = ItemCarrito("p1", "Producto", "Marca", 70.9, 1, stockDisponible = 3)
        vm.restaurarItems(Gson().toJson(listOf(item, item, item.copy(productoId = "", precioUnit = -1.0))))
        assertEquals(1, vm.items.size)
    }
}
