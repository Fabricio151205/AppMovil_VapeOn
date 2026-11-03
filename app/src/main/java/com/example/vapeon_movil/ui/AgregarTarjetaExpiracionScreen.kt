package com.example.vapeon_movil.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.entities.validarRegistroTarjeta
import com.example.vapeon_movil.ui.theme.*
import java.time.YearMonth

internal fun vencimientoTarjetaSimuladaValido(mes: String, anio: String, hoy: YearMonth = YearMonth.now()): Boolean =
    validarRegistroTarjeta("Titular de prueba", "Marca", "0000", mes, anio, hoy) == null

@Composable
fun AgregarTarjetaExpiracionScreen(mes: String, anio: String, cambiarMes: (String) -> Unit,
    cambiarAnio: (String) -> Unit, volver: () -> Unit, continuar: () -> Unit) {
    PasoCheckout("Fecha de expiración", volver) {
        OutlinedTextField(mes, cambiarMes, label = { Text("Mes (MM)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(anio, cambiarAnio, label = { Text("Año (AAAA)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        val valido = vencimientoTarjetaSimuladaValido(mes, anio)
        if (!valido && (mes.isNotBlank() || anio.isNotBlank()))
            Text("Ingresa mes y año válidos de una tarjeta no vencida.", color = MaterialTheme.colorScheme.error)
        Text("La tarjeta es válida durante todo su mes de vencimiento.", color = VapeOnTextSecondary)
        Button(onClick = continuar, enabled = valido, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Continuar")
        }
    }
}
