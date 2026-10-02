package com.example.vapeon_movil.ui

import androidx.compose.runtime.Composable
import com.example.vapeon_movil.entities.Producto

@Composable
fun HomeScreen(
    irDetalleProducto: (Producto) -> Unit,
    onPerfilClick: () -> Unit = {}
) {
    // Reutiliza la interfaz del catálogo en modo CLIENTE (esAdmin = false)
    CatalogoScreen(
        irDetalleProducto = irDetalleProducto,
        esAdmin = false,
        onPerfilClick = onPerfilClick
    )
}