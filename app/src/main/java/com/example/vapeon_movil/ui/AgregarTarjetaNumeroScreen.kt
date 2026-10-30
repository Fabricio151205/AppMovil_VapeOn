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

internal fun numeroTarjetaSimuladoValido(numero: String): Boolean = Regex("[0-9]{13,19}").matches(numero)

@Composable
fun AgregarTarjetaNumeroScreen(marca: String, cambiarMarca: (String) -> Unit, volver: () -> Unit,
    continuar: (String) -> Unit) {
    // Solo memoria de esta vista. No utilizar rememberSaveable, ViewModel ni persistencia para el número.
    var numero by remember { mutableStateOf("") }
    PasoCheckout("Número de tarjeta", volver) {
        Text("Simulación: utiliza datos de prueba. Solo se guardarán los últimos cuatro dígitos.", color = VapeOnTextSecondary)
        OutlinedTextField(numero, { valor ->
            if (valor.length <= 19 && valor.all { it in '0'..'9' }) numero = valor
        }, label = { Text("Número de tarjeta de prueba") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(marca, cambiarMarca, label = { Text("Marca") }, singleLine = true,
            modifier = Modifier.fillMaxWidth())
        Text("Introduce entre 13 y 19 dígitos. Es una validación de formato, no bancaria.", color = VapeOnTextSecondary)
        Button(onClick = {
            val ultimos = numero.takeLast(4)
            numero = ""
            continuar(ultimos)
        }, enabled = numeroTarjetaSimuladoValido(numero) && marca.isNotBlank(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Continuar") }
    }
}
