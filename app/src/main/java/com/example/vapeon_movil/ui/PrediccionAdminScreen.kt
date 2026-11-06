package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.ui.theme.*

@Composable
fun PrediccionAdminScreen(
    volver: () -> Unit,
    irPronosticoDemanda: () -> Unit,
    irAlertasStock: () -> Unit,
    irSinMovimiento: () -> Unit,
    irSugerenciasReabastecimiento: () -> Unit
) {
    BackHandler(onBack = volver)

    Surface(
        color = VapeOnBackground,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ENCABEZADO SUPERIOR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = volver) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver al panel",
                        tint = VapeOnTextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Predicción e Inteligencia AI",
                    color = VapeOnTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // BOTÓN 1: GRÁFICO DE VENTAS Y PRONÓSTICO AI
            // ==========================================
            BotonOpcionPrediccion(
                titulo = "Gráfico de Ventas y Pronóstico AI",
                subtitulo = "Modelo Holt entrenado con ventas reales entregadas e histograma de demanda.",
                icono = Icons.AutoMirrored.Filled.TrendingUp,
                onClick = irPronosticoDemanda
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // BOTÓN 2: ALERTAS DE STOCK CRÍTICO Y AGOTADOS
            // ==========================================
            BotonOpcionPrediccion(
                titulo = "Alertas de Stock Crítico y Agotados",
                subtitulo = "Monitoreo de productos con stock <= 5 unidades o agotados.",
                icono = Icons.Default.Warning,
                onClick = irAlertasStock
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // BOTÓN 3: PRODUCTOS SIN MOVIMIENTO
            // ==========================================
            BotonOpcionPrediccion(
                titulo = "Productos Sin Movimiento / Antiguos",
                subtitulo = "Detección de productos de baja rotación o sin ventas recientes.",
                icono = Icons.Default.HourglassEmpty,
                onClick = irSinMovimiento
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // BOTÓN 4: SUGERENCIAS DE REABASTECIMIENTO
            // ==========================================
            BotonOpcionPrediccion(
                titulo = "Sugerencias de Reabastecimiento",
                subtitulo = "Estimación automatizada de orden de compra y reposición por marca.",
                icono = Icons.Default.ShoppingBag,
                onClick = irSugerenciasReabastecimiento
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BotonOpcionPrediccion(
    titulo: String,
    subtitulo: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VapeOnRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = VapeOnTextPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    color = VapeOnTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitulo,
                    color = VapeOnTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
