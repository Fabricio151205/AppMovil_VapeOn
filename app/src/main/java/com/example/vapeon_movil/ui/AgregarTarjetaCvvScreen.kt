package com.example.vapeon_movil.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.ui.theme.*

internal fun codigoSeguridadSimuladoValido(codigo: String): Boolean = Regex("[0-9]{3,4}").matches(codigo)

@Composable
fun AgregarTarjetaCvvScreen(volver: () -> Unit, continuar: () -> Unit) {
    // Transitorio: nunca sale de esta vista ni se guarda al recrear la Activity.
    var codigo by remember { mutableStateOf("") }
    PasoCheckout("Código de seguridad", volver) {
        Text("Paso visual de simulación. Utiliza un código de prueba; no se guardará ni se enviará.", color = VapeOnTextSecondary)
        OutlinedTextField(codigo, { valor ->
            if (valor.length <= 4 && valor.all { it in '0'..'9' }) codigo = valor
        }, label = { Text("CVV/CVC de prueba") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Button(onClick = { codigo = ""; continuar() }, enabled = codigoSeguridadSimuladoValido(codigo),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Continuar") }
    }
}
