package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.entities.Marca
import com.example.vapeon_movil.entities.Producto
import com.example.vapeon_movil.entities.SaborItem
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.utils.mismaMarca
import com.example.vapeon_movil.viewmodels.MarcasViewModel

/**
 * Administración de marcas.
 *
 * [relacionesCargando] debe ser true mientras productos o sabores aún se están
 * leyendo de Firestore. Sin esa guarda se podría eliminar una marca referenciada
 * antes de que la pantalla conociera sus relaciones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarcasAdminScreen(
    marcasViewModel: MarcasViewModel,
    productos: List<Producto>,
    sabores: List<SaborItem>,
    relacionesCargando: Boolean = false,
    volver: () -> Unit,
    irAgregarMarca: () -> Unit,
    irEditarMarca: (Marca) -> Unit
) {
    BackHandler(onBack = volver)
    var eliminar by remember { mutableStateOf<Marca?>(null) }
    LaunchedEffect(Unit) { marcasViewModel.cargarMarcas() }

    Scaffold(
        containerColor = VapeOnBackground,
        topBar = {
            TopAppBar(
                title = { Text("GESTIÓN DE MARCAS", color = VapeOnGold, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = volver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar", tint = VapeOnTextPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VapeOnBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = irAgregarMarca, containerColor = VapeOnRed) {
                Icon(Icons.Default.Add, "Agregar marca", tint = VapeOnTextPrimary)
            }
        }
    ) { padding ->
        when {
            marcasViewModel.cargando && marcasViewModel.listaMarcas.isEmpty() ->
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = VapeOnGold)
                }
            marcasViewModel.mensajeError != null && marcasViewModel.listaMarcas.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(marcasViewModel.mensajeError.orEmpty(), color = MaterialTheme.colorScheme.error)
                Button(onClick = marcasViewModel::cargarMarcas) { Text("Reintentar") }
            }
            marcasViewModel.listaMarcas.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No hay marcas registradas.", color = VapeOnTextSecondary)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = marcasViewModel::sembrarMarcasIniciales,
                    enabled = !marcasViewModel.cargando,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed, contentColor = VapeOnTextPrimary)
                ) {
                    Text("Cargar marcas iniciales", fontWeight = FontWeight.Bold)
                }
            }
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Text("Marca                                  Acciones", color = VapeOnTextSecondary) }
                items(marcasViewModel.listaMarcas, key = { it.id.ifBlank { it.nombre } }) { marca ->
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(marca.nombre, color = VapeOnTextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            IconButton(onClick = { irEditarMarca(marca) }) { Icon(Icons.Default.Edit, "Editar ${marca.nombre}", tint = VapeOnGold) }
                            IconButton(onClick = { eliminar = marca }) { Icon(Icons.Default.Delete, "Eliminar ${marca.nombre}", tint = VapeOnRed) }
                        }
                    }
                }
            }
        }
    }

    eliminar?.let { marca ->
        val relacionada = productos.any { mismaMarca(it.marca, marca.nombre) } ||
            sabores.any { mismaMarca(it.marca, marca.nombre) }
        AlertDialog(
            onDismissRequest = { eliminar = null },
            title = { Text("Eliminar marca", color = VapeOnGold) },
            text = {
                Text(
                    when {
                        relacionesCargando -> "Verificando productos y sabores asociados..."
                        relacionada -> "No puede eliminarse '${marca.nombre}' porque tiene productos o sabores asociados."
                        else -> "¿Eliminar '${marca.nombre}'?"
                    },
                    color = VapeOnTextPrimary
                )
            },
            confirmButton = {
                if (!relacionada && !relacionesCargando) {
                    Button(
                        onClick = { marcasViewModel.eliminarMarca(marca, false); eliminar = null },
                        colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed)
                    ) { Text("Eliminar") }
                }
            },
            dismissButton = {
                TextButton(onClick = { eliminar = null }) {
                    Text(if (relacionada || relacionesCargando) "Entendido" else "Cancelar")
                }
            },
            containerColor = VapeOnSurface
        )
    }
}
