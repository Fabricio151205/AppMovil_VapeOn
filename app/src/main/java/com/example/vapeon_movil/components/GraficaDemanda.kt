package com.example.vapeon_movil.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.entities.ObservacionDemanda
import com.example.vapeon_movil.entities.PuntoPronostico
import com.example.vapeon_movil.ui.theme.VapeOnGold
import com.example.vapeon_movil.ui.theme.VapeOnRed
import com.example.vapeon_movil.ui.theme.VapeOnTextPrimary
import com.example.vapeon_movil.ui.theme.VapeOnTextSecondary
import com.example.vapeon_movil.ui.theme.alfaAccesible
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max

@Composable
fun GraficaDemanda(
    historico: List<ObservacionDemanda>,
    validacion: List<PuntoPronostico>,
    pronostico: List<PuntoPronostico>,
    modifier: Modifier = Modifier
) {
    if (historico.isEmpty()) return
    val formato = DateTimeFormatter.ofPattern("dd/MM")
    val todasFechas = historico.map { it.fecha } + validacion.map { it.fecha } + pronostico.map { it.fecha }
    val inicio = todasFechas.minOrNull() ?: return
    val fin = todasFechas.maxOrNull() ?: return
    val dias = max(1L, java.time.temporal.ChronoUnit.DAYS.between(inicio, fin))
    val maximo = max(1.0, (historico.map { it.unidades } + validacion.map { it.unidades } +
        pronostico.map { it.unidades }).maxOrNull() ?: 1.0)
    val alfaEjes = alfaAccesible(0.45f)
    val alfaCorte = alfaAccesible(0.55f)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .semantics {
                    contentDescription = "Gráfica de demanda con ${historico.size} observaciones históricas, " +
                        "${validacion.size} puntos de validación y ${pronostico.size} puntos futuros."
                }
        ) {
            val margenX = 18.dp.toPx()
            val margenY = 14.dp.toPx()
            val ancho = size.width - margenX * 2
            val alto = size.height - margenY * 2
            fun posicion(fecha: LocalDate, valor: Double): Offset {
                val x = margenX + java.time.temporal.ChronoUnit.DAYS.between(inicio, fecha).toFloat() / dias * ancho
                val y = margenY + alto - (valor.coerceAtLeast(0.0) / maximo).toFloat() * alto
                return Offset(x, y)
            }
            drawLine(VapeOnTextSecondary.copy(alpha = alfaEjes), Offset(margenX, margenY), Offset(margenX, margenY + alto))
            drawLine(VapeOnTextSecondary.copy(alpha = alfaEjes), Offset(margenX, margenY + alto), Offset(margenX + ancho, margenY + alto))

            fun dibujar(puntos: List<Pair<LocalDate, Double>>, color: Color) {
                if (puntos.isEmpty()) return
                val path = Path()
                puntos.forEachIndexed { indice, punto ->
                    val posicion = posicion(punto.first, punto.second)
                    if (indice == 0) path.moveTo(posicion.x, posicion.y) else path.lineTo(posicion.x, posicion.y)
                    drawCircle(color, radius = 3.5.dp.toPx(), center = posicion)
                }
                if (puntos.size > 1) drawPath(path, color, style = Stroke(width = 2.5.dp.toPx()))
            }
            dibujar(historico.map { it.fecha to it.unidades }, VapeOnGold)
            dibujar(validacion.map { it.fecha to it.unidades }, Color(0xFF4FC3F7))
            dibujar(pronostico.map { it.fecha to it.unidades }, VapeOnRed)

            pronostico.firstOrNull()?.let {
                val x = posicion(it.fecha, 0.0).x
                drawLine(VapeOnTextPrimary.copy(alpha = alfaCorte), Offset(x, margenY), Offset(x, margenY + alto),
                    strokeWidth = 1.dp.toPx())
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(inicio.format(formato), color = VapeOnTextSecondary, style = MaterialTheme.typography.labelSmall)
            Text(fin.format(formato), color = VapeOnTextSecondary, style = MaterialTheme.typography.labelSmall)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Leyenda(VapeOnGold, "Histórico")
            if (validacion.isNotEmpty()) Leyenda(Color(0xFF4FC3F7), "Validación")
            if (pronostico.isNotEmpty()) Leyenda(VapeOnRed, "Pronóstico")
        }
    }
}

@Composable
private fun Leyenda(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(color = color, shape = CircleShape, modifier = Modifier.size(10.dp)) {}
        Spacer(Modifier.size(5.dp))
        Text(texto, color = VapeOnTextPrimary, style = MaterialTheme.typography.labelSmall)
    }
}
