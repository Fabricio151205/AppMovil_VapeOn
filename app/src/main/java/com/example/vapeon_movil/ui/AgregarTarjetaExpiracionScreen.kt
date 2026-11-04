package com.example.vapeon_movil.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.entities.validarRegistroTarjeta
import com.example.vapeon_movil.ui.theme.*
import java.time.YearMonth

internal fun vencimientoTarjetaSimuladaValido(mes: String, anio: String, hoy: YearMonth = YearMonth.now()): Boolean =
    validarRegistroTarjeta("Titular de prueba", "Marca", "0000", mes, anio, hoy) == null

/**
 * Transforma el texto de 4 dígitos "AAMM" a formato visual "AA / MM" (Año / Mes).
 */
class ExpirationDateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.filter { it.isDigit() }.take(4)
        val out = when {
            raw.length <= 2 -> raw
            else -> "${raw.substring(0, 2)} / ${raw.substring(2)}"
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceAtMost(raw.length)
                if (clamped <= 2) return clamped
                return (clamped + 3).coerceAtMost(out.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceAtMost(out.length)
                if (clamped <= 2) return clamped
                if (clamped <= 5) return 2
                return (clamped - 3).coerceAtMost(raw.length)
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

@Composable
fun AgregarTarjetaExpiracionScreen(
    mes: String,
    anio: String,
    cambiarMes: (String) -> Unit,
    cambiarAnio: (String) -> Unit,
    volver: () -> Unit,
    continuar: () -> Unit
) {
    var expiracionDigitos by remember {
        val inicial = if (anio.isNotBlank() && mes.isNotBlank()) {
            val a = if (anio.length == 4) anio.takeLast(2) else anio
            "$a$mes"
        } else ""
        mutableStateOf(inicial)
    }

    var mensajeError by remember { mutableStateOf("") }

    val esValido = remember(expiracionDigitos) {
        if (expiracionDigitos.length == 4) {
            val a = expiracionDigitos.take(2)
            val m = expiracionDigitos.drop(2)
            val mesInt = m.toIntOrNull() ?: 0
            val anioCompleto = "20$a"
            mesInt in 1..12 && vencimientoTarjetaSimuladaValido(m, anioCompleto)
        } else false
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        color = VapeOnBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ENCABEZADO SUPERIOR CON FLECHA Y TÍTULO
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = volver) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar",
                        tint = VapeOnTextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Primero agrega una tarjeta",
                    color = VapeOnTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // INDICADOR DE PASOS 1 - 2 - 3 - 4 (PASO 2 ACTIVO)
            IndicadorPasosTarjeta(pasoActual = 2)

            Spacer(modifier = Modifier.height(16.dp))

            // VISTA PREVIA TARJETA DELANTERA (ESTÁTICA)
            VistaTarjetaDelante()

            Spacer(modifier = Modifier.height(24.dp))

            // TÍTULO Y SUBTÍTULO
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Text(
                        text = "Fecha de Expiración",
                        color = VapeOnTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ingresa el mes y año de expiración impreso en tu tarjeta",
                        color = VapeOnTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // CAMPO DE FECHA DE EXPIRACIÓN (ORDEN AÑO / MES: AA / MM)
            OutlinedTextField(
                value = expiracionDigitos,
                onValueChange = { input ->
                    val digitos = input.filter { it.isDigit() }.take(4)
                    expiracionDigitos = digitos
                    if (mensajeError.isNotEmpty()) mensajeError = ""

                    if (digitos.length == 4) {
                        val a = digitos.take(2)
                        val m = digitos.drop(2)
                        val mesInt = m.toIntOrNull() ?: 0
                        if (mesInt !in 1..12) {
                            mensajeError = "El mes de expiración debe ser entre 01 y 12"
                        } else {
                            cambiarAnio("20$a")
                            cambiarMes(m)
                        }
                    } else if (digitos.length >= 2) {
                        val a = digitos.take(2)
                        cambiarAnio("20$a")
                        cambiarMes("")
                    }
                },
                placeholder = {
                    Text(
                        text = "Ej.: 30 / 03",
                        color = VapeOnTextSecondary,
                        fontSize = 15.sp
                    )
                },
                trailingIcon = {
                    if (esValido) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Válido",
                            tint = VapeOnGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = ExpirationDateVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = VapeOnInputBackground,
                    unfocusedContainerColor = VapeOnInputBackground,
                    disabledContainerColor = VapeOnInputBackground,
                    focusedBorderColor = VapeOnGold,
                    unfocusedBorderColor = VapeOnInputBorder,
                    focusedTextColor = VapeOnTextPrimary,
                    unfocusedTextColor = VapeOnTextPrimary,
                    cursorColor = VapeOnGold
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (mensajeError.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = mensajeError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(40.dp))

            // FILA DE BOTONES: ANTERIOR Y CONTINUAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Button(
                    onClick = volver,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VapeOnRedDark,
                        contentColor = VapeOnTextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text(
                        text = "ANTERIOR",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = {
                        if (expiracionDigitos.length < 4) {
                            mensajeError = "Por favor ingresa el año y mes completos (Ej.: 30 / 03)"
                            return@Button
                        }
                        val a = expiracionDigitos.take(2)
                        val m = expiracionDigitos.drop(2)
                        val mesInt = m.toIntOrNull() ?: 0

                        if (mesInt !in 1..12) {
                            mensajeError = "El mes de expiración debe estar entre 01 y 12"
                            return@Button
                        }

                        val anioCompleto = "20$a"
                        if (!vencimientoTarjetaSimuladaValido(m, anioCompleto)) {
                            mensajeError = "La fecha de expiración ingresada ya venció"
                            return@Button
                        }

                        mensajeError = ""
                        cambiarAnio(anioCompleto)
                        cambiarMes(m)
                        continuar()
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VapeOnRed,
                        contentColor = VapeOnTextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text(
                        text = "CONTINUAR",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
