package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.entities.MAX_PUNTOS_FIDELIDAD
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.viewmodels.FidelidadViewModel

@Composable
fun MiFidelidadScreen(
    usuario: Usuario,
    fidelidadViewModel: FidelidadViewModel,
    volver: () -> Unit
) {
    LaunchedEffect(usuario.id) { fidelidadViewModel.cargarMiFidelidad(usuario) }
    BackHandler(onBack = volver)
    val puntos = fidelidadViewModel.miFidelidad?.puntosActuales ?: 0
    val faltan = (MAX_PUNTOS_FIDELIDAD - puntos).coerceAtLeast(0)

    Surface(Modifier.fillMaxSize(), color = VapeOnBackground) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = volver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar", tint = VapeOnTextPrimary)
                }
                Text("MI FIDELIDAD", color = VapeOnGold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(28.dp))
            when {
                fidelidadViewModel.cargando -> CircularProgressIndicator(color = VapeOnGold)
                fidelidadViewModel.error != null -> EstadoFidelidad(
                    fidelidadViewModel.error.orEmpty(), "Reintentar"
                ) { fidelidadViewModel.cargarMiFidelidad(usuario) }
                else -> Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = VapeOnGold,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "$puntos / $MAX_PUNTOS_FIDELIDAD puntos",
                            color = VapeOnTextPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.height(20.dp))
                        LinearProgressIndicator(
                            progress = { puntos / MAX_PUNTOS_FIDELIDAD.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(12.dp),
                            color = VapeOnGold,
                            trackColor = VapeOnInputBackground
                        )
                        Spacer(Modifier.height(22.dp))
                        Text(
                            text = when {
                                puntos == MAX_PUNTOS_FIDELIDAD -> "RECOMPENSA DISPONIBLE"
                                faltan == 1 -> "Te falta 1 compra para obtener tu recompensa."
                                else -> "Te faltan $faltan compras para obtener tu recompensa."
                            },
                            color = if (puntos == MAX_PUNTOS_FIDELIDAD) VapeOnGold else VapeOnTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        if (puntos == MAX_PUNTOS_FIDELIDAD) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Espera la entrega de tu recompensa por parte del administrador.",
                                color = VapeOnTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        } else if (puntos == 0 && (fidelidadViewModel.miFidelidad?.numeroCiclo ?: 1) > 1) {
                            Spacer(Modifier.height(10.dp))
                            Text("Nuevo ciclo de fidelidad.", color = VapeOnTextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EstadoFidelidad(mensaje: String, accion: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(mensaje, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed)) {
            Text(accion)
        }
    }
}
