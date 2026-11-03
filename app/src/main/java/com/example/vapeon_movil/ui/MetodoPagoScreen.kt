package com.example.vapeon_movil.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.ui.theme.*

@Composable
fun MetodoPagoScreen(volver: () -> Unit, efectivo: () -> Unit, tarjeta: () -> Unit, yape: () -> Unit) {
    PasoCheckout("¿Cómo desea pagar?", volver) {
        Button(onClick = efectivo, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
            Text("EFECTIVO · Pago con entrega")
        }
        OutlinedButton(onClick = tarjeta, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
            Text("TARJETA · Seleccionar o registrar", color = VapeOnGold)
        }
        OutlinedButton(onClick = yape, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
            Text("YAPE · Pago SIMULADO", color = VapeOnGold)
        }
        Text("Yape y tarjeta son simulaciones universitarias. No realizan cobros reales.", color = VapeOnTextSecondary)
    }
}
