package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.entities.EstadisticasPeriodo
import com.example.vapeon_movil.entities.Pedido
import com.example.vapeon_movil.entities.PeriodoEstadistica
import com.example.vapeon_movil.entities.PuntoSerieEstadistica
import com.example.vapeon_movil.entities.calcularEstadisticas
import com.example.vapeon_movil.entities.moverAnclaEstadistica
import com.example.vapeon_movil.entities.zonaEstadistica
import com.example.vapeon_movil.ui.theme.VapeOnBackground
import com.example.vapeon_movil.ui.theme.VapeOnCardBackground
import com.example.vapeon_movil.ui.theme.VapeOnError
import com.example.vapeon_movil.ui.theme.VapeOnGold
import com.example.vapeon_movil.ui.theme.VapeOnRed
import com.example.vapeon_movil.ui.theme.VapeOnRedDark
import com.example.vapeon_movil.ui.theme.VapeOnTextMuted
import com.example.vapeon_movil.ui.theme.VapeOnTextPrimary
import com.example.vapeon_movil.ui.theme.VapeOnTextSecondary
import com.example.vapeon_movil.ui.theme.alfaAccesible
import com.example.vapeon_movil.utils.moneda
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadisticasScreen(
    pedidos: List<Pedido>,
    cargando: Boolean,
    error: String?,
    invalidosCarga: Int,
    volver: () -> Unit,
    reintentar: () -> Unit
) {
    BackHandler(onBack = volver)
    val hoy = remember { LocalDate.now(zonaEstadistica) }
    var tipoNombre by rememberSaveable { mutableStateOf(PeriodoEstadistica.MES.name) }
    var anclaEpoch by rememberSaveable { mutableStateOf(hoy.toEpochDay()) }
    val tipo = remember(tipoNombre) {
        PeriodoEstadistica.entries.firstOrNull { it.name == tipoNombre } ?: PeriodoEstadistica.MES
    }
    val ancla = remember(anclaEpoch) { LocalDate.ofEpochDay(anclaEpoch) }
    val estadisticas = remember(pedidos, tipo, ancla) {
        calcularEstadisticas(pedidos, tipo, ancla)
    }
    val puedeAvanzar = estadisticas.rango.fin.isBefore(hoy)
    val advertencias = invalidosCarga + estadisticas.caja.invalidos

    Scaffold(
        containerColor = VapeOnBackground,
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas", color = VapeOnGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = volver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver al panel", tint = VapeOnTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VapeOnBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Ventas entregadas · zona horaria America/Lima",
                color = VapeOnTextSecondary,
                fontSize = 13.sp
            )

            SelectorPeriodo(tipo = tipo, onCambiar = { tipoNombre = it.name })

            NavegadorPeriodo(
                etiqueta = estadisticas.etiquetaPeriodo,
                rangoTexto = estadisticas.rangoTexto,
                puedeAvanzar = puedeAvanzar,
                anterior = { anclaEpoch = moverAnclaEstadistica(tipo, ancla, -1).toEpochDay() },
                siguiente = { anclaEpoch = moverAnclaEstadistica(tipo, ancla, 1).toEpochDay() },
                irHoy = { anclaEpoch = hoy.toEpochDay() },
                esHoy = ancla == hoy
            )

            if (cargando) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = VapeOnGold)
            }

            if (error != null) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        Button(
                            onClick = reintentar,
                            colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Reintentar", color = VapeOnTextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (advertencias > 0) {
                Text(
                    "$advertencias pedido(s) con datos inválidos fueron excluidos del reporte.",
                    color = VapeOnError,
                    fontSize = 13.sp
                )
            }

            ResumenKpis(estadisticas)
            ComparacionPeriodo(estadisticas)
            EvolucionPeriodo(estadisticas)

            SeccionReporte(
                titulo = "TOP PRODUCTOS",
                vacio = "Sin productos vendidos en este periodo.",
                filas = estadisticas.topProductos.map {
                    FilaReporte(
                        titulo = it.nombre,
                        detalle = "${it.marca} · ${unidadesTexto(it.unidades)}",
                        valor = moneda(it.ingreso)
                    )
                }
            )

            SeccionReporte(
                titulo = "VENTAS POR MARCA",
                vacio = "Sin marcas vendidas en este periodo.",
                filas = estadisticas.marcas.map {
                    FilaReporte(it.marca, unidadesTexto(it.unidades), moneda(it.ingreso))
                }
            )

            SeccionReporte(
                titulo = "VENTAS POR CATEGORÍA",
                vacio = "Sin categorías vendidas en este periodo.",
                filas = estadisticas.categorias.map {
                    FilaReporte(it.categoria, unidadesTexto(it.unidades), moneda(it.ingreso))
                }
            )

            SeccionReporte(
                titulo = "MÉTODOS DE PAGO",
                vacio = "Sin pagos registrados en este periodo.",
                filas = estadisticas.metodosPago.map {
                    FilaReporte(it.metodo, pedidosTexto(it.pedidos), moneda(it.total))
                }
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SelectorPeriodo(
    tipo: PeriodoEstadistica,
    onCambiar: (PeriodoEstadistica) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PeriodoEstadistica.entries.forEach { periodo ->
            val seleccionado = periodo == tipo
            Button(
                onClick = { onCambiar(periodo) },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (seleccionado) VapeOnRed else VapeOnRedDark,
                    contentColor = VapeOnTextPrimary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
                Text(
                    periodo.etiqueta,
                    fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun NavegadorPeriodo(
    etiqueta: String,
    rangoTexto: String,
    puedeAvanzar: Boolean,
    anterior: () -> Unit,
    siguiente: () -> Unit,
    irHoy: () -> Unit,
    esHoy: Boolean
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = anterior, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.ChevronLeft, "Periodo anterior", tint = VapeOnTextPrimary)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        etiqueta,
                        color = VapeOnGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        rangoTexto,
                        color = VapeOnTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
                IconButton(
                    onClick = siguiente,
                    enabled = puedeAvanzar,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        "Periodo siguiente",
                        tint = if (puedeAvanzar) VapeOnTextPrimary else VapeOnTextMuted
                    )
                }
            }
            TextButton(
                onClick = irHoy,
                enabled = !esHoy,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(
                    "Ir a hoy",
                    color = if (esHoy) VapeOnTextMuted else VapeOnGold,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ResumenKpis(estadisticas: EstadisticasPeriodo) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiEstadistica("Total vendido", moneda(estadisticas.total), Modifier.weight(1f))
            KpiEstadistica("Pedidos entregados", estadisticas.pedidos.toString(), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiEstadistica("Unidades vendidas", estadisticas.unidades.toString(), Modifier.weight(1f))
            KpiEstadistica("Ticket promedio", moneda(estadisticas.ticketPromedio), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiEstadistica("Clientes atendidos", estadisticas.clientes.toString(), Modifier.weight(1f))
            KpiEstadistica("Delivery", moneda(estadisticas.delivery), Modifier.weight(1f))
        }
        val promedioEtiqueta = estadisticas.tipo.promedioEtiqueta
        val mejor = estadisticas.mejorPunto
        if (promedioEtiqueta != null && estadisticas.promedio != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiEstadistica(promedioEtiqueta, moneda(estadisticas.promedio), Modifier.weight(1f))
                KpiEstadistica(
                    estadisticas.tipo.etiquetaMejor,
                    mejor?.etiqueta ?: "—",
                    Modifier.weight(1f),
                    detalle = mejor?.let { moneda(it.total) }
                )
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiEstadistica(
                    estadisticas.tipo.etiquetaMejor,
                    mejor?.etiqueta ?: "—",
                    Modifier.weight(1f),
                    detalle = mejor?.let { moneda(it.total) }
                )
                KpiEstadistica("Ingresos por productos", moneda(estadisticas.productos), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun KpiEstadistica(
    titulo: String,
    valor: String,
    modifier: Modifier = Modifier,
    detalle: String? = null
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(titulo, color = VapeOnTextSecondary, fontSize = 12.sp)
            Text(valor, color = VapeOnTextPrimary, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            detalle?.let {
                Text(it, color = VapeOnGold, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ComparacionPeriodo(estadisticas: EstadisticasPeriodo) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Comparación con el periodo anterior", color = VapeOnGold, fontWeight = FontWeight.Bold)
            Text("Periodo anterior: ${moneda(estadisticas.totalAnterior)}", color = VapeOnTextPrimary)
            val variacion = estadisticas.variacionPorcentual
            if (variacion == null) {
                Text(
                    "Sin ventas entregadas en el periodo anterior.",
                    color = VapeOnTextSecondary,
                    fontSize = 13.sp
                )
            } else {
                val signo = if (variacion.signum() >= 0) "+" else ""
                Text(
                    "$signo${variacion.toPlainString()}% frente al periodo anterior",
                    color = if (variacion.signum() >= 0) VapeOnGold else VapeOnError,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EvolucionPeriodo(estadisticas: EstadisticasPeriodo) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(estadisticas.tipo.etiquetaSerie, color = VapeOnGold, fontWeight = FontWeight.Bold)
            Text(
                "Agrupado por la fecha de registro del pedido.",
                color = VapeOnTextSecondary,
                fontSize = 11.sp
            )
            if (estadisticas.pedidos == 0) {
                Text(
                    "Sin ventas entregadas en este periodo.",
                    color = VapeOnTextSecondary,
                    fontSize = 13.sp
                )
            } else {
                GraficaEstadistica(estadisticas.serie)
                estadisticas.mejorPunto?.let {
                    Text(
                        "Máximo: ${it.etiqueta} · ${moneda(it.total)} (${pedidosTexto(it.pedidos)})",
                        color = VapeOnTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun GraficaEstadistica(puntos: List<PuntoSerieEstadistica>) {
    if (puntos.isEmpty()) return
    val maximo = puntos.maxOf { it.total.toDouble() }.coerceAtLeast(1.0)
    val indiceMaximo = puntos.indices.maxByOrNull { puntos[it].total } ?: 0
    val alfaEje = alfaAccesible(0.45f)
    val etiquetas = when {
        puntos.size <= 6 -> puntos.map { it.etiqueta }
        else -> listOf(
            puntos.first().etiqueta,
            puntos[puntos.size / 2].etiqueta,
            puntos.last().etiqueta
        )
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .semantics {
                contentDescription = "Gráfica de ${puntos.size} periodos. " +
                    "Máximo en ${puntos[indiceMaximo].etiqueta} con ${moneda(puntos[indiceMaximo].total)}."
            }
    ) {
        val margen = 8.dp.toPx()
        val ancho = size.width - margen * 2
        val alto = size.height - margen * 2
        val paso = ancho / puntos.size
        val anchoBarra = (paso * 0.62f).coerceAtLeast(2f)
        val radio = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        drawLine(
            color = VapeOnTextSecondary.copy(alpha = alfaEje),
            start = Offset(margen, margen + alto),
            end = Offset(margen + ancho, margen + alto),
            strokeWidth = 1.dp.toPx()
        )
        puntos.forEachIndexed { indice, punto ->
            val altura = alto * (punto.total.toDouble() / maximo).toFloat().coerceIn(0f, 1f)
            if (altura > 0f) {
                val x = margen + paso * indice + (paso - anchoBarra) / 2
                val y = margen + alto - altura
                drawRoundRect(
                    color = if (indice == indiceMaximo) VapeOnGold else VapeOnRed,
                    topLeft = Offset(x, y),
                    size = Size(anchoBarra, altura),
                    cornerRadius = radio
                )
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        etiquetas.forEach { etiqueta ->
            Text(etiqueta, color = VapeOnTextSecondary, fontSize = 11.sp)
        }
    }
}

private data class FilaReporte(
    val titulo: String,
    val detalle: String,
    val valor: String
)

@Composable
private fun SeccionReporte(titulo: String, vacio: String, filas: List<FilaReporte>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(titulo, color = VapeOnTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (filas.isEmpty()) {
            Text(vacio, color = VapeOnTextSecondary, fontSize = 13.sp)
        } else {
            filas.forEach { fila ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(fila.titulo, color = VapeOnTextPrimary, fontWeight = FontWeight.Bold)
                            Text(fila.detalle, color = VapeOnTextSecondary, fontSize = 12.sp)
                        }
                        Text(fila.valor, color = VapeOnGold, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun unidadesTexto(unidades: Int): String =
    "$unidades ${if (unidades == 1) "unidad" else "unidades"}"

private fun pedidosTexto(pedidos: Int): String =
    "$pedidos ${if (pedidos == 1) "pedido" else "pedidos"}"
