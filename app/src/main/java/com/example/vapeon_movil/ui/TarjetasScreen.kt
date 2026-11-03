package com.example.vapeon_movil.ui

import androidx.compose.runtime.Composable
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.viewmodels.TarjetaViewModel

/** Entrada desde el menú: comparte el listado y la selección del checkout. */
@Composable
fun TarjetasScreen(usuario: Usuario?, tarjetaViewModel: TarjetaViewModel, volver: () -> Unit,
    agregarTarjeta: () -> Unit) {
    SeleccionTarjetaScreen(usuario, tarjetaViewModel, volver, agregarTarjeta, desdeCheckout = false)
}
