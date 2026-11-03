package com.example.vapeon_movil.ui

import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.ui.theme.VapeOnTextPrimary

@Composable
fun YapeScreen(volver: () -> Unit, iniciarPago: () -> Unit) {
    PasoCheckout("Yape", volver) {
        Text("Pago SIMULADO. No hay conexión real con Yape y no se transfiere dinero.", color = VapeOnTextPrimary)
        Button(onClick = iniciarPago, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Continuar con Yape SIMULADO")
        }
    }
}
