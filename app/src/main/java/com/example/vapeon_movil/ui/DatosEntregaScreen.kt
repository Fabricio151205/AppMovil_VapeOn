package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.ui.theme.*

@Composable
fun DatosEntregaScreen(usuario: Usuario?, volver: () -> Unit, continuar: () -> Unit) {
    PasoCheckout("Datos de entrega", volver) {
        Text("Cliente: ${usuario?.nombre.orEmpty()} ${usuario?.apellido.orEmpty()}", color = VapeOnTextPrimary)
        Text("Teléfono: ${usuario?.telefono.orEmpty()}", color = VapeOnTextPrimary)
        Text("Revisa los datos del destinatario antes de indicar la dirección.", color = VapeOnTextSecondary)
        Button(onClick = continuar, enabled = !usuario?.nombre.isNullOrBlank() && !usuario?.telefono.isNullOrBlank(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Continuar") }
    }
}

/** Contenedor visual común; cada etapa conserva su propio Composable y sus callbacks. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PasoCheckout(titulo: String, volver: () -> Unit, bloqueado: Boolean = false,
    contenido: @Composable ColumnScope.() -> Unit) {
    BackHandler { if (!bloqueado) volver() }
    VapeON_MovilTheme {
        Scaffold(containerColor = VapeOnBackground, topBar = {
            TopAppBar(title = { Text("VAPEON", color = VapeOnGold) }, navigationIcon = {
                IconButton(onClick = volver, enabled = !bloqueado) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = VapeOnTextPrimary)
                }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = VapeOnBackground))
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).imePadding()
                .padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(titulo, style = MaterialTheme.typography.headlineSmall, color = VapeOnGold)
                contenido()
            }
        }
    }
}
