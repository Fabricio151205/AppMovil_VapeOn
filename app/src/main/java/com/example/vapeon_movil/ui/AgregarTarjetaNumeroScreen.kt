package com.example.vapeon_movil.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.R
import com.example.vapeon_movil.ui.theme.*

internal fun numeroTarjetaSimuladoValido(numero: String): Boolean = Regex("[0-9]{13,19}").matches(numero)

/**
 * Transforma el texto bruto de dígitos a formato "4444 4444 4444 4444" manteniendo el cursor alineado.
 */
class CardNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.filter { it.isDigit() }.take(16)
        val out = StringBuilder()
        for (i in raw.indices) {
            out.append(raw[i])
            if (i % 4 == 3 && i != 15 && i != raw.length - 1) {
                out.append(" ")
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val clamped = offset.coerceAtMost(raw.length)
                val spaces = (clamped - 1) / 4
                return (clamped + spaces).coerceAtMost(out.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val clamped = offset.coerceAtMost(out.length)
                val spaces = clamped / 5
                return (clamped - spaces).coerceAtMost(raw.length)
            }
        }

        return TransformedText(AnnotatedString(out.toString()), offsetMapping)
    }
}

@Composable
fun AgregarTarjetaNumeroScreen(
    marca: String,
    cambiarMarca: (String) -> Unit,
    volver: () -> Unit,
    continuar: (String) -> Unit
) {
    var numeroDigitos by remember { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf("") }

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
                    text = "Agregar tarjeta nueva",
                    color = VapeOnTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // INDICADOR DE PASOS 1 - 2 - 3 - 4
            IndicadorPasosTarjeta(pasoActual = 1)

            Spacer(modifier = Modifier.height(16.dp))

            // VISTA PREVIA TARJETA DELANTERA (ESTÁTICA SEGÚN CAPTURA)
            VistaTarjetaDelante()

            Spacer(modifier = Modifier.height(24.dp))

            // TÍTULO Y SUBTÍTULO DE LA SECCIÓN
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Text(
                        text = "Numero de la Tarjeta",
                        color = VapeOnTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ingresar los 16 digitos de su tarjeta",
                        color = VapeOnTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // CAMPO DE ENTRADA DEL NÚMERO DE TARJETA
            OutlinedTextField(
                value = numeroDigitos,
                onValueChange = { input ->
                    val soloDigitos = input.filter { it.isDigit() }.take(16)
                    numeroDigitos = soloDigitos
                    if (mensajeError.isNotEmpty()) mensajeError = ""

                    val detectada = when {
                        soloDigitos.startsWith("4") -> "Visa"
                        soloDigitos.startsWith("5") -> "MasterCard"
                        soloDigitos.startsWith("3") -> "American Express"
                        else -> "Visa"
                    }
                    if (marca != detectada) cambiarMarca(detectada)
                },
                placeholder = {
                    Text(
                        text = "Ej.: 9582 2569 1234 4561",
                        color = VapeOnTextSecondary,
                        fontSize = 15.sp
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = CardNumberVisualTransformation(),
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

            Spacer(modifier = Modifier.height(16.dp))

            // MENSAJE DE CIFRADO DE SEGURIDAD CON ÍCONO DE CANDADO
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Seguridad",
                    tint = VapeOnTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tus datos estan cifrados de extremo a extremo con seguridad militar",
                    color = VapeOnTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(40.dp))

            // BOTÓN PRINCIPAL: CONTINUAR
            Button(
                onClick = {
                    if (numeroDigitos.length < 13) {
                        mensajeError = "Por favor ingresa un número de tarjeta válido (13 a 16 dígitos)"
                        return@Button
                    }
                    mensajeError = ""
                    val ultimos = numeroDigitos.takeLast(4)
                    continuar(ultimos)
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VapeOnRed,
                    contentColor = VapeOnTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "CONTINUAR",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ==========================================
// COMPONENTES AUXILIARES COMPARTIDOS (ESTÁTICOS)
// ==========================================

@Composable
internal fun IndicadorPasosTarjeta(pasoActual: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        (1..4).forEach { paso ->
            val activo = paso <= pasoActual
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (activo) VapeOnRed else VapeOnRedDark),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = paso.toString(),
                    color = VapeOnTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (paso < 4) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "--------",
                    color = VapeOnTextSecondary,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
internal fun VistaTarjetaDelante() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(22.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.tarjeta_muestra_delante),
            contentDescription = "Tarjeta Delantera",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "VAPEON PAY",
                color = VapeOnTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 1.sp
            )

            Text(
                text = "9582 2569 1234 4561",
                color = VapeOnTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                letterSpacing = 1.sp,
                maxLines = 1
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Titular:", color = VapeOnTextSecondary, fontSize = 10.sp)
                    Text(
                        text = "Fabricio Alvarez",
                        color = VapeOnTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }

                Column {
                    Text("Vence el:", color = VapeOnTextSecondary, fontSize = 10.sp)
                    Text(
                        text = "30 / 03",
                        color = VapeOnTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
internal fun VistaTarjetaAtras() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(22.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.tarjeta_muestra_atras),
            contentDescription = "Tarjeta Trasera",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
