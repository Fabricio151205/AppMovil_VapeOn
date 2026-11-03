package com.example.vapeon_movil.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.ui.theme.*

@Composable
fun AgregarTarjetaTitularScreen(titular: String, cambiarTitular: (String) -> Unit, ultimos4: String,
    registrando: Boolean, pendiente: Boolean, error: String?, volver: () -> Unit, registrar: () -> Unit) {
    PasoCheckout("Nombre del titular", volver, bloqueado = registrando) {
        Text("Tarjeta simulada · **** **** **** $ultimos4", color = VapeOnTextPrimary)
        OutlinedTextField(titular, cambiarTitular, enabled = !registrando && !pendiente,
            label = { Text("Titular") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (registrando) CircularProgressIndicator(color = VapeOnGold)
        if (pendiente && !registrando) Text("Reintenta para comprobar el mismo registro.", color = VapeOnTextSecondary)
        Button(onClick = registrar, enabled = !registrando && titular.isNotBlank(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (pendiente) "Reintentar registro" else "Registrar tarjeta")
        }
    }
}
