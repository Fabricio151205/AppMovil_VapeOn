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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.viewmodels.FormularioProveedor
import com.example.vapeon_movil.viewmodels.ProveedorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarProveedorScreen(
    usuario: Usuario?,
    proveedorViewModel: ProveedorViewModel,
    cancelar: () -> Unit,
    registrado: () -> Unit
) {
    BackHandler(enabled = !proveedorViewModel.guardando, onBack = cancelar)
    val formulario = proveedorViewModel.formulario
    val errores = proveedorViewModel.erroresFormulario

    LaunchedEffect(proveedorViewModel.registroExitoso) {
        if (proveedorViewModel.registroExitoso) {
            proveedorViewModel.consumirRegistroExitoso()
            registrado()
        }
    }

    fun actualizar(cambio: FormularioProveedor.() -> FormularioProveedor) {
        proveedorViewModel.actualizarFormulario(formulario.cambio())
    }

    Scaffold(
        containerColor = VapeOnBackground,
        topBar = {
            TopAppBar(
                title = { Text("REGISTRAR PROVEEDOR", color = VapeOnGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = cancelar, enabled = !proveedorViewModel.guardando) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cancelar registro", tint = VapeOnTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VapeOnBackground)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CampoProveedor("Razón social", formulario.razonSocial,
                { actualizar { copy(razonSocial = it) } }, errores.razonSocial)
            CampoProveedor("RUC", formulario.ruc,
                { valor -> actualizar { copy(ruc = valor.filter(Char::isDigit).take(11)) } },
                errores.ruc, KeyboardType.Number)
            CampoProveedor("Nombre del contacto", formulario.contacto,
                { actualizar { copy(contacto = it) } }, errores.contacto)
            CampoProveedor("Teléfono", formulario.telefono,
                { valor -> actualizar { copy(telefono = valor.filter(Char::isDigit).take(9)) } },
                errores.telefono, KeyboardType.Phone)
            CampoProveedor("Correo electrónico", formulario.correo,
                { actualizar { copy(correo = it) } }, errores.correo, KeyboardType.Email)
            CampoProveedor("Dirección", formulario.direccion,
                { actualizar { copy(direccion = it) } }, errores.direccion)
            CampoProveedor("Ciudad", formulario.ciudad,
                { actualizar { copy(ciudad = it) } }, errores.ciudad)
            CampoProveedor("Tipo de producto", formulario.tipoProducto,
                { actualizar { copy(tipoProducto = it) } }, errores.tipoProducto)
            CampoProveedor("Observaciones (opcional)", formulario.observaciones,
                { actualizar { copy(observaciones = it) } }, null, lineas = 4)

            proveedorViewModel.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = cancelar, enabled = !proveedorViewModel.guardando,
                    modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(50)
                ) { Text("CANCELAR", color = VapeOnTextPrimary) }
                Button(
                    onClick = { proveedorViewModel.registrarProveedor(usuario) },
                    enabled = !proveedorViewModel.guardando,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = VapeOnRed)
                ) {
                    if (proveedorViewModel.guardando) CircularProgressIndicator(
                        modifier = Modifier.size(22.dp), color = VapeOnTextPrimary, strokeWidth = 2.dp
                    ) else Text("REGISTRAR", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CampoProveedor(
    etiqueta: String,
    valor: String,
    cambiar: (String) -> Unit,
    error: String?,
    teclado: KeyboardType = KeyboardType.Text,
    lineas: Int = 1
) {
    OutlinedTextField(
        value = valor,
        onValueChange = cambiar,
        label = { Text(etiqueta) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = lineas == 1,
        minLines = lineas,
        isError = error != null,
        supportingText = error?.let { mensaje -> { Text(mensaje, color = MaterialTheme.colorScheme.error) } },
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = VapeOnTextPrimary, unfocusedTextColor = VapeOnTextPrimary,
            focusedBorderColor = VapeOnGold, unfocusedBorderColor = VapeOnInputBorder,
            focusedLabelColor = VapeOnGold, unfocusedLabelColor = VapeOnTextSecondary
        )
    )
}
