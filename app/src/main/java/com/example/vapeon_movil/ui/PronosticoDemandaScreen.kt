package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.components.GraficaDemanda
import com.example.vapeon_movil.entities.CriteriosPronosticoDemanda
import com.example.vapeon_movil.entities.EstadoPronosticoDemanda
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.ui.theme.VapeOnBackground
import com.example.vapeon_movil.ui.theme.VapeOnCardBackground
import com.example.vapeon_movil.ui.theme.VapeOnGold
import com.example.vapeon_movil.ui.theme.VapeOnRed
import com.example.vapeon_movil.ui.theme.VapeOnTextPrimary
import com.example.vapeon_movil.ui.theme.VapeOnTextSecondary
import com.example.vapeon_movil.ui.theme.alfaAccesible
import com.example.vapeon_movil.viewmodels.PronosticoDemandaViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PronosticoDemandaScreen(
    usuario: Usuario?,
    viewModel: PronosticoDemandaViewModel,
    volver: () -> Unit
) {
    BackHandler(onBack = volver)
    LaunchedEffect(usuario?.id, usuario?.rol) { viewModel.cargar(usuario) }
    val resultado = viewModel.resultado
    val serie = resultado?.serie
    var menuExpandido by remember { mutableStateOf(false) }
    val formatoFecha = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    Scaffold(
        containerColor = VapeOnBackground,
        topBar = {
            TopAppBar(
                title = { Text("Pronóstico de demanda", color = VapeOnGold, fontWeight = FontWeight.Bold) },
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
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Modelo Holt ajustado exclusivamente con pedidos ENTREGADO y ventas históricas reales.",
                color = VapeOnTextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            when {
                viewModel.cargando -> CircularProgressIndicator(color = VapeOnGold, modifier = Modifier.align(Alignment.CenterHorizontally))
                viewModel.error != null -> {
                    Text(viewModel.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.cargar(usuario) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Reintentar")
                    }
                }
                viewModel.series.isEmpty() -> Text(
                    "No existen ventas entregadas con snapshots válidos para analizar.", color = VapeOnTextPrimary
                )
                else -> {
                    ExposedDropdownMenuBox(expanded = menuExpandido, onExpandedChange = { menuExpandido = it }) {
                        OutlinedTextField(
                            value = serie?.let { "${it.nombre} · ${it.marca}" }.orEmpty(),
                            onValueChange = {}, readOnly = true, label = { Text("Producto histórico") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menuExpandido) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = menuExpandido, onDismissRequest = { menuExpandido = false }) {
                            viewModel.series.forEach { producto ->
                                DropdownMenuItem(
                                    text = { Text("${producto.nombre} · ${producto.marca}") },
                                    onClick = { viewModel.seleccionarProducto(producto.productoId); menuExpandido = false }
                                )
                            }
                        }
                    }

                    Text("Horizonte futuro", color = VapeOnTextPrimary, fontWeight = FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CriteriosPronosticoDemanda.HORIZONTES_PERMITIDOS.forEach { dias ->
                            Button(
                                onClick = { viewModel.cambiarHorizonte(dias) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (viewModel.horizonte == dias) VapeOnRed else VapeOnCardBackground
                                ),
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            ) { Text("$dias días") }
                        }
                    }

                    if (serie != null && resultado != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(serie.nombre, color = VapeOnGold, style = MaterialTheme.typography.titleLarge)
                                Text("Marca histórica: ${serie.marca}", color = VapeOnTextPrimary)
                                Text("ID: ${serie.productoId}", color = VapeOnTextSecondary, style = MaterialTheme.typography.bodySmall)
                                Text("Período: ${serie.primeraFecha?.format(formatoFecha)} – ${serie.ultimaFecha?.format(formatoFecha)}", color = VapeOnTextPrimary)
                                Text("Observaciones: ${serie.observaciones.size} / ${CriteriosPronosticoDemanda.MINIMO_OBSERVACIONES} mínimas", color = VapeOnTextPrimary)
                                Text("Días con demanda: ${serie.diasConDemanda} / ${CriteriosPronosticoDemanda.MINIMO_DIAS_CON_DEMANDA} mínimos", color = VapeOnTextPrimary)
                                Text("Unidades vendidas: ${serie.unidadesVendidas}", color = VapeOnTextPrimary)
                            }
                        }

                        if (serie.nombre.trim().equals("pruebas", true)) {
                            Text(
                                "Advertencia: este es un producto histórico real cuyo nombre podría corresponder a datos de desarrollo; no fue excluido del análisis.",
                                color = VapeOnGold, style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (resultado.estado == EstadoPronosticoDemanda.DATOS_INSUFICIENTES) {
                            Card(colors = CardDefaults.cardColors(containerColor = VapeOnRed.copy(alpha = alfaAccesible(0.25f)))) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text("Datos insuficientes para generar un pronóstico confiable.", color = VapeOnTextPrimary,
                                        fontWeight = FontWeight.Bold)
                                    Text(resultado.motivo.orEmpty(), color = VapeOnTextSecondary)
                                }
                            }
                        } else {
                            Card(colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text("Modelo entrenado", color = VapeOnGold, fontWeight = FontWeight.Bold)
                                    Text("Entrenamiento: ${resultado.observacionesEntrenamiento} días", color = VapeOnTextPrimary)
                                    Text("Validación: ${resultado.observacionesValidacion} días", color = VapeOnTextPrimary)
                                    Text("Pronóstico: ${resultado.futuro.size} días", color = VapeOnTextPrimary)
                                    Text("α: ${formato(resultado.alpha)}   β: ${formato(resultado.beta)}", color = VapeOnTextPrimary)
                                    Text("MAE: ${formato(resultado.mae)} unidades", color = VapeOnTextPrimary)
                                    Text("RMSE: ${formato(resultado.rmse)} unidades", color = VapeOnTextPrimary)
                                }
                            }
                        }

                        Card(colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground)) {
                            GraficaDemanda(
                                historico = serie.observaciones,
                                validacion = resultado.validacion,
                                pronostico = resultado.futuro,
                                modifier = Modifier.fillMaxWidth().padding(14.dp)
                            )
                        }
                        if (viewModel.documentosInvalidos > 0) {
                            Text("Documentos descartados por datos inválidos: ${viewModel.documentosInvalidos}",
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            Spacer(Modifier.heightIn(min = 8.dp))
        }
    }
}

private fun formato(valor: Double?): String = valor?.let { String.format(Locale.ROOT, "%.3f", it) } ?: "—"
