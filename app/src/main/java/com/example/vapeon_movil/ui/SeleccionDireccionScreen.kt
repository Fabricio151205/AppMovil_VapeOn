package com.example.vapeon_movil.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.ui.theme.*

@Composable
fun SeleccionDireccionScreen(direccion: String, cambiarDireccion: (String) -> Unit,
    volver: () -> Unit, continuar: () -> Unit) {
    PasoCheckout("Selección de dirección", volver) {
        Text("Dirección para esta compra", color = VapeOnTextPrimary)
        OutlinedTextField(value = direccion, onValueChange = cambiarDireccion,
            label = { Text("Dirección de entrega") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = continuar, enabled = direccion.isNotBlank(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Usar esta dirección") }
    }
}
