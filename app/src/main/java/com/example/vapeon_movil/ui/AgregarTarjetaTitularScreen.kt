package com.example.vapeon_movil.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.ui.theme.*

@Composable
fun AgregarTarjetaTitularScreen(
    titular: String,
    cambiarTitular: (String) -> Unit,
    ultimos4: String,
    registrando: Boolean,
    pendiente: Boolean,
    error: String?,
    volver: () -> Unit,
    registrar: () -> Unit
) {
    var mensajeErrorLocal by remember { mutableStateOf("") }

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
                IconButton(onClick = volver, enabled = !registrando) {
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

            // INDICADOR DE PASOS 1 - 2 - 3 - 4 (TODOS LOS PASOS ACTIVOS)
            IndicadorPasosTarjeta(pasoActual = 4)

            Spacer(modifier = Modifier.height(16.dp))

            // VISTA PREVIA TARJETA DELANTERA (ESTÁTICA)
            VistaTarjetaDelante()

            Spacer(modifier = Modifier.height(24.dp))

            // TÍTULO Y SUBTÍTULO DE LA SECCIÓN
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Text(
                        text = "Nombre del titular",
                        color = VapeOnTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ultimo paso para terminar, confirma el nombre del tirular para tu tarjeta.\nIngresa el nombre del titular como aparece en tu tarjeta",
                        color = VapeOnTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // CAMPO DE ENTRADA DEL NOMBRE DEL TITULAR
            OutlinedTextField(
                value = titular,
                onValueChange = {
                    cambiarTitular(it)
                    if (mensajeErrorLocal.isNotEmpty()) mensajeErrorLocal = ""
                },
                enabled = !registrando && !pendiente,
                placeholder = {
                    Text(
                        text = "Ej.: Fabricio Alvarez",
                        color = VapeOnTextSecondary,
                        fontSize = 15.sp
                    )
                },
                singleLine = true,
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

            val errorVisible = mensajeErrorLocal.ifEmpty { error.orEmpty() }
            if (errorVisible.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorVisible,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }

            if (registrando) {
                Spacer(modifier = Modifier.height(14.dp))
                CircularProgressIndicator(color = VapeOnGold, modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(40.dp))

            // FILA DE BOTONES: ANTERIOR Y AGREGAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Button(
                    onClick = volver,
                    enabled = !registrando,
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
                        if (titular.trim().length < 3) {
                            mensajeErrorLocal = "Por favor ingresa el nombre completo del titular (mínimo 3 letras)"
                            return@Button
                        }
                        mensajeErrorLocal = ""
                        registrar()
                    },
                    enabled = !registrando,
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
                        text = if (pendiente) "REINTENTAR" else "AGREGAR",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
