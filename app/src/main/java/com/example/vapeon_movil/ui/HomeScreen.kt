package com.example.vapeon_movil.ui

import androidx.compose.runtime.Composable
import com.example.vapeon_movil.entities.Producto
import com.example.vapeon_movil.viewmodels.ProductoViewModel

@Composable
fun HomeScreen(
    productoViewModel: ProductoViewModel,
    irDetalleProducto: (Producto) -> Unit,
    onPerfilClick: () -> Unit = {},
    irCarrito: () -> Unit = {}
) {
    // Reutiliza la interfaz del catálogo en modo CLIENTE (esAdmin = false)
    // El ViewModel se recibe desde VapeONApp para mantener un único estado compartido
    CatalogoScreen(
        productoViewModel = productoViewModel,
        irDetalleProducto = irDetalleProducto,
        esAdmin = false,
        onPerfilClick = onPerfilClick,
        irCarrito = irCarrito
    )
}