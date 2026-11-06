package com.example.vapeon_movil.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.ui.theme.*

@Composable
fun SeleccionDireccionScreen(
    direccion: String,
    cambiarDireccion: (String) -> Unit,
    volver: () -> Unit,
    continuar: () -> Unit
) {
    var direccionSeleccionada by remember {
        mutableStateOf(if (direccion.isNotBlank()) direccion else "Jr. Arcomayo KM 3.53")
    }

    val listaDirecciones = listOf(
        Pair("Jr. Arcomayo KM 3.53", "Ultimo envio hace tres dias"),
        Pair("Jr. Miguel Iglesias 1053", "Ultimo envio hace cinco dias"),
        Pair("Via de Evitamiento 562", "Ultimo envio hace una semana")
    )

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
                    text = "Selecciona una\nDirección",
                    color = VapeOnTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // LISTA DE DIRECCIONES GUARDADAS
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                listaDirecciones.forEach { (dirNombre, dirDetalle) ->
                    val esSeleccionada = direccionSeleccionada.equals(dirNombre, ignoreCase = true)

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (esSeleccionada) VapeOnSelected else VapeOnCardNotSelected
                        ),
                        border = BorderStroke(1.dp, VapeOnCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                direccionSeleccionada = dirNombre
                                cambiarDireccion(dirNombre)
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Text(
                                text = dirNombre,
                                color = VapeOnTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = dirDetalle,
                                color = VapeOnTextSecondary,
                                fontSize = 13.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            HorizontalDivider(color = VapeOnGold, thickness = 2.dp)

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Ver Ubicación",
                                color = VapeOnTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(30.dp))

            // FILA DE BOTONES INFERIORES: CONTINUAR Y AGREGAR NUEVA DIRECCIÓN
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        cambiarDireccion(direccionSeleccionada)
                        continuar()
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

                Button(
                    onClick = { /* Acción para agregar dirección */ },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VapeOnRedDark,
                        contentColor = VapeOnTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "AGREGAR\nNUEVA DIRECCIÓN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
