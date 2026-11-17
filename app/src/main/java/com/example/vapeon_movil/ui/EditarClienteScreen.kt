package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.viewmodels.ClientesViewModel
import com.example.vapeon_movil.viewmodels.FormularioCliente

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarClienteScreen(
    admin: Usuario?,
    viewModel: ClientesViewModel,
    volver: () -> Unit,
    guardado: () -> Unit,
    reintentarCarga: () -> Unit = {}
) {
    BackHandler(enabled = !viewModel.guardando, onBack = volver)
    val formulario = viewModel.formulario
    val errores = viewModel.erroresFormulario
    Scaffold(
        containerColor = VapeOnBackground,
        topBar = { TopAppBar(
            title = { Text("EDITAR CLIENTE", color = VapeOnGold, fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = volver, enabled = !viewModel.guardando) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar", tint = VapeOnTextPrimary)
            } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = VapeOnBackground)
        ) }
    ) { padding ->
        if (viewModel.cargandoDetalle) Box(
            Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center
        ) { CircularProgressIndicator(color = VapeOnGold) }
        else if (viewModel.clienteSeleccionado == null) Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.Center
        ) {
            Text(viewModel.errorDetalle ?: "El cliente no está disponible.", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            Button(onClick = reintentarCarga, colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed)) {
                Text("Reintentar")
            }
        } else Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CampoEdicionCliente("Nombre", formulario.nombre, errores.nombre, KeyboardType.Text) {
                viewModel.actualizarFormulario(formulario.copy(nombre = it))
            }
            CampoEdicionCliente("Apellido", formulario.apellido, errores.apellido, KeyboardType.Text) {
                viewModel.actualizarFormulario(formulario.copy(apellido = it))
            }
            CampoEdicionCliente("Correo", formulario.correo, errores.correo, KeyboardType.Email) {
                viewModel.actualizarFormulario(formulario.copy(correo = it))
            }
            CampoEdicionCliente("DNI (opcional)", formulario.dni, errores.dni, KeyboardType.Number) {
                viewModel.actualizarFormulario(formulario.copy(dni = it))
            }
            CampoEdicionCliente("Teléfono", formulario.telefono, errores.telefono, KeyboardType.Phone) {
                viewModel.actualizarFormulario(formulario.copy(telefono = it))
            }
            CampoEdicionCliente("Fecha de nacimiento (dd/mm/aaaa)", formulario.fechaNacimiento,
                errores.fechaNacimiento, KeyboardType.Text) {
                viewModel.actualizarFormulario(formulario.copy(fechaNacimiento = it))
            }
            viewModel.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (viewModel.conflicto) {
                OutlinedButton(
                    onClick = { viewModel.recargarClienteParaEdicion(admin) },
                    enabled = !viewModel.guardando && !viewModel.cargandoDetalle,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) { Text("RECARGAR INFORMACIÓN", color = VapeOnGold) }
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { viewModel.guardarCambios(admin, guardado) },
                enabled = !viewModel.guardando,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed)
            ) {
                if (viewModel.guardando) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = VapeOnTextPrimary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                }
                Text("GUARDAR CAMBIOS", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CampoEdicionCliente(
    etiqueta: String, valor: String, error: String?, teclado: KeyboardType, cambiar: (String) -> Unit
) {
    OutlinedTextField(
        value = valor, onValueChange = cambiar, modifier = Modifier.fillMaxWidth(),
        label = { Text(etiqueta) }, isError = error != null, singleLine = true,
        supportingText = error?.let { { Text(it) } }, keyboardOptions = KeyboardOptions(keyboardType = teclado),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VapeOnGold, focusedLabelColor = VapeOnGold,
            focusedTextColor = VapeOnTextPrimary, unfocusedTextColor = VapeOnTextPrimary
        )
    )
}
