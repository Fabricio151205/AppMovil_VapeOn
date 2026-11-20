package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.entities.Inversionista
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.viewmodels.ErroresInversionista
import com.example.vapeon_movil.viewmodels.InversionistasViewModel
import com.example.vapeon_movil.viewmodels.validarCamposInversionista
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Gestión administrativa de inversionistas.
 *
 * La pantalla mantiene el flujo del ViewModel (lista, detalle, formulario y
 * eliminación) y concentra la presentación en tarjetas legibles, estados
 * vacíos claros y acciones con área táctil suficiente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InversionistasScreen(usuario: Usuario?, viewModel: InversionistasViewModel, volver: () -> Unit) {
    var busqueda by rememberSaveable { mutableStateOf("") }
    var eliminar by remember { mutableStateOf<Inversionista?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(usuario?.id, usuario?.rol) {
        viewModel.actualizarSesion(usuario)
        viewModel.cargar(usuario)
    }
    LaunchedEffect(viewModel.mensaje) {
        viewModel.mensaje?.let {
            eliminar = null
            snackbar.showSnackbar(it)
            viewModel.consumirMensaje()
        }
    }
    BackHandler {
        when {
            viewModel.guardando -> Unit
            eliminar != null -> eliminar = null
            viewModel.formulario != null -> viewModel.cancelarFormulario()
            viewModel.seleccionado != null -> viewModel.cerrarDetalle()
            else -> volver()
        }
    }

    val filtrados = remember(viewModel.lista, busqueda) {
        val texto = busqueda.trim()
        if (texto.isBlank()) {
            viewModel.lista
        } else {
            viewModel.lista.filter {
                it.nombre.contains(texto, true) ||
                    it.documento.contains(texto) ||
                    it.correo.contains(texto, true)
            }
        }
    }
    val bloqueado = viewModel.guardando || viewModel.cargando

    Scaffold(
        containerColor = VapeOnBackground,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Inversionistas", color = VapeOnTextPrimary, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Registros de inversión",
                            color = VapeOnTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = volver, enabled = !viewModel.guardando) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al panel",
                            tint = VapeOnTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.cargar(usuario) },
                        enabled = !bloqueado
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar lista",
                            tint = if (bloqueado) VapeOnTextMuted else VapeOnGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VapeOnBackground)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            item {
                ResumenInversionistas(
                    cantidad = viewModel.lista.size,
                    cargando = viewModel.cargando,
                    onActualizar = { viewModel.cargar(usuario) },
                    habilitado = !bloqueado
                )
            }

            item {
                Button(
                    onClick = { viewModel.iniciarFormulario() },
                    enabled = !bloqueado,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VapeOnRed,
                        contentColor = VapeOnTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Registrar inversionista", fontWeight = FontWeight.Bold)
                }
            }

            item {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    singleLine = true,
                    enabled = !bloqueado,
                    placeholder = { Text("Buscar por nombre, documento o correo") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = VapeOnTextSecondary)
                    },
                    trailingIcon = {
                        if (busqueda.isNotBlank()) {
                            TextButton(onClick = { busqueda = "" }) {
                                Text("Limpiar", color = VapeOnGold, fontSize = 12.sp)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = VapeOnTextPrimary,
                        unfocusedTextColor = VapeOnTextPrimary,
                        focusedBorderColor = VapeOnGold,
                        unfocusedBorderColor = VapeOnBorder,
                        cursorColor = VapeOnGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (viewModel.cargando) {
                item {
                    LinearProgressIndicator(
                        color = VapeOnGold,
                        trackColor = VapeOnSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                    )
                }
            }

            viewModel.error?.takeIf { viewModel.formulario == null && eliminar == null }?.let { error ->
                item {
                    AvisoInversionistas(
                        icono = Icons.Default.Info,
                        texto = error,
                        color = VapeOnError
                    )
                }
            }

            if (filtrados.isEmpty() && !viewModel.cargando) {
                item {
                    EstadoVacioInversionistas(
                        conBusqueda = busqueda.isNotBlank(),
                        onRegistrar = { viewModel.iniciarFormulario() },
                        habilitado = !bloqueado
                    )
                }
            } else {
                items(filtrados, key = { it.id }) { inversionista ->
                    TarjetaInversionista(
                        inversionista = inversionista,
                        habilitado = !bloqueado,
                        onVer = { viewModel.ver(inversionista) },
                        onEditar = { viewModel.iniciarFormulario(inversionista) },
                        onEliminar = { eliminar = inversionista }
                    )
                }
            }
        }
    }

    viewModel.seleccionado?.takeIf { viewModel.formulario == null && eliminar == null }?.let { inversionista ->
        DetalleInversionistaDialog(
            inversionista = inversionista,
            onEditar = { viewModel.iniciarFormulario(inversionista) },
            onCerrar = viewModel::cerrarDetalle
        )
    }

    viewModel.formulario?.let { formulario ->
        FormularioInversionistaDialog(
            formulario = formulario,
            esEdicion = viewModel.lista.any { it.id == formulario.id },
            guardando = viewModel.guardando,
            cargando = viewModel.cargando,
            requiereComprobacion = viewModel.altaRequiereComprobacion,
            error = viewModel.error,
            onCambiar = viewModel::cambiarFormulario,
            onGuardar = { viewModel.guardar(usuario) },
            onCancelar = viewModel::cancelarFormulario
        )
    }

    eliminar?.let { inversionista ->
        AlertDialog(
            onDismissRequest = { if (!viewModel.guardando) eliminar = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = VapeOnError) },
            title = { Text("Eliminar inversionista") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "¿Eliminar el registro de ${inversionista.nombre}? " +
                            "Esta acción es permanente.",
                        color = VapeOnTextSecondary
                    )
                    viewModel.error?.let { Text(it, color = VapeOnError, fontSize = 13.sp) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.eliminar(usuario, inversionista) },
                    enabled = !viewModel.guardando
                ) {
                    if (viewModel.guardando) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = VapeOnGold, strokeWidth = 2.dp)
                    } else {
                        Text("Eliminar", color = VapeOnError, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { eliminar = null }, enabled = !viewModel.guardando) {
                    Text("Cancelar", color = VapeOnTextSecondary)
                }
            },
            containerColor = VapeOnSurface
        )
    }
}

@Composable
private fun ResumenInversionistas(
    cantidad: Int,
    cargando: Boolean,
    onActualizar: () -> Unit,
    habilitado: Boolean
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(VapeOnRedDark, VapeOnSurface)),
                    RoundedCornerShape(20.dp)
                )
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(VapeOnGold.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = VapeOnGold,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (cantidad == 1) "1 inversionista" else "$cantidad inversionistas",
                    color = VapeOnTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = if (cargando) "Actualizando registros…" else "Datos de identidad y contacto",
                    color = VapeOnTextSecondary,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = onActualizar, enabled = habilitado) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Actualizar lista",
                    tint = if (habilitado) VapeOnGold else VapeOnTextMuted
                )
            }
        }
    }
}

@Composable
private fun TarjetaInversionista(
    inversionista: Inversionista,
    habilitado: Boolean,
    onVer: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
        border = BorderStroke(1.dp, VapeOnDivider),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = habilitado, onClick = onVer)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AvatarInversionista(nombre = inversionista.nombre)
                Column(Modifier.weight(1f)) {
                    Text(
                        text = inversionista.nombre,
                        color = VapeOnTextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    EtiquetaDocumento(
                        tipo = inversionista.tipoDocumento,
                        documento = inversionista.documento
                    )
                }
                IconButton(onClick = onEditar, enabled = habilitado) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar ${inversionista.nombre}",
                        tint = VapeOnGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onEliminar, enabled = habilitado) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar ${inversionista.nombre}",
                        tint = VapeOnError,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = VapeOnDivider)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (inversionista.telefono.isNotBlank()) {
                    DatoContactoCompacto(
                        icono = Icons.Default.Phone,
                        texto = inversionista.telefono,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (inversionista.correo.isNotBlank()) {
                    DatoContactoCompacto(
                        icono = Icons.Default.Email,
                        texto = inversionista.correo,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (inversionista.telefono.isBlank() && inversionista.correo.isBlank()) {
                    Text(
                        text = "Sin datos de contacto",
                        color = VapeOnTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarInversionista(nombre: String) {
    val iniciales = remember(nombre) { inicialesNombre(nombre) }
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(VapeOnGold.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = iniciales,
            color = VapeOnGold,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun EtiquetaDocumento(tipo: String, documento: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Badge,
            contentDescription = null,
            tint = VapeOnTextSecondary,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = "$tipo $documento",
            color = VapeOnTextSecondary,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun DatoContactoCompacto(
    icono: ImageVector,
    texto: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = VapeOnTextSecondary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = texto,
            color = VapeOnTextSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AvisoInversionistas(icono: ImageVector, texto: String, color: Color) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(text = texto, color = VapeOnTextPrimary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun EstadoVacioInversionistas(
    conBusqueda: Boolean,
    onRegistrar: () -> Unit,
    habilitado: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnSurface),
        border = BorderStroke(1.dp, VapeOnDivider),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (conBusqueda) Icons.Default.Search else Icons.Default.AccountBalance,
                contentDescription = null,
                tint = VapeOnGold,
                modifier = Modifier.size(34.dp)
            )
            Text(
                text = if (conBusqueda) "No hay coincidencias" else "Aún no hay inversionistas",
                color = VapeOnTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (conBusqueda) {
                    "Prueba con otro nombre, documento o correo."
                } else {
                    "Registra el primer inversionista para llevar su identidad y contacto."
                },
                color = VapeOnTextSecondary,
                fontSize = 13.sp
            )
            if (!conBusqueda) {
                TextButton(onClick = onRegistrar, enabled = habilitado) {
                    Text("Registrar ahora", color = VapeOnGold, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetalleInversionistaDialog(
    inversionista: Inversionista,
    onEditar: () -> Unit,
    onCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        icon = { AvatarInversionista(nombre = inversionista.nombre) },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = inversionista.nombre,
                    color = VapeOnTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(4.dp))
                EtiquetaDocumento(
                    tipo = inversionista.tipoDocumento,
                    documento = inversionista.documento
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilaDetalle(Icons.Default.Phone, "Teléfono", inversionista.telefono)
                FilaDetalle(Icons.Default.Email, "Correo", inversionista.correo)
                FilaDetalle(
                    Icons.Default.CalendarMonth,
                    "Fecha de registro",
                    fechaRegistroLegible(inversionista.fechaRegistro)
                )
                if (inversionista.observaciones.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "OBSERVACIONES",
                            color = VapeOnGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Surface(
                            color = VapeOnInputBackground,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = inversionista.observaciones,
                                color = VapeOnTextPrimary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onEditar) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Editar", color = VapeOnGold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) {
                Text("Cerrar", color = VapeOnTextSecondary)
            }
        },
        containerColor = VapeOnSurface
    )
}

@Composable
private fun FilaDetalle(icono: ImageVector, etiqueta: String, valor: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = VapeOnTextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Column {
            Text(etiqueta, color = VapeOnTextSecondary, fontSize = 11.sp)
            Text(
                text = valor.ifBlank { "Sin registrar" },
                color = if (valor.isBlank()) VapeOnTextMuted else VapeOnTextPrimary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun FormularioInversionistaDialog(
    formulario: Inversionista,
    esEdicion: Boolean,
    guardando: Boolean,
    cargando: Boolean,
    requiereComprobacion: Boolean,
    error: String?,
    onCambiar: (Inversionista) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit
) {
    var intentoGuardar by remember(formulario.id) { mutableStateOf(false) }
    val errores = if (intentoGuardar) validarCamposInversionista(formulario) else ErroresInversionista()
    val editable = !guardando && !requiereComprobacion
    AlertDialog(
        onDismissRequest = onCancelar,
        icon = {
            Icon(
                imageVector = if (esEdicion) Icons.Default.Edit else Icons.Default.Add,
                contentDescription = null,
                tint = VapeOnGold
            )
        },
        title = {
            Text(
                text = if (esEdicion) "Editar inversionista" else "Registrar inversionista",
                color = VapeOnTextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (requiereComprobacion && !guardando) {
                    AvisoInversionistas(
                        icono = Icons.Default.Info,
                        texto = "Hay un registro pendiente. Pulsa Guardar para comprobarlo o reintentar " +
                            "con los mismos datos.",
                        color = VapeOnGold
                    )
                }

                Text(
                    text = "Los campos con * son obligatorios.",
                    color = VapeOnTextSecondary,
                    fontSize = 12.sp
                )

                CampoInversionista(
                    etiqueta = "Nombre o razón social *",
                    valor = formulario.nombre,
                    enabled = editable,
                    error = errores.nombre,
                    cambiar = { onCambiar(formulario.copy(nombre = it)) }
                )

                Text(
                    text = "DOCUMENTO",
                    color = VapeOnGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("DNI", "RUC").forEach { tipo ->
                        FilterChip(
                            selected = formulario.tipoDocumento == tipo,
                            enabled = editable,
                            onClick = {
                                onCambiar(formulario.copy(tipoDocumento = tipo, documento = ""))
                            },
                            label = { Text(tipo) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = VapeOnSurfaceVariant,
                                labelColor = VapeOnTextSecondary,
                                selectedContainerColor = VapeOnGold,
                                selectedLabelColor = Color.Black,
                                disabledContainerColor = VapeOnSurfaceVariant,
                                disabledLabelColor = VapeOnTextMuted
                            )
                        )
                    }
                }
                CampoInversionista(
                    etiqueta = "Número de documento *",
                    valor = formulario.documento,
                    enabled = editable,
                    teclado = KeyboardType.Number,
                    error = errores.documento,
                    cambiar = { onCambiar(formulario.copy(documento = it)) }
                )

                HorizontalDivider(color = VapeOnDivider)

                Text(
                    text = "CONTACTO",
                    color = VapeOnGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                CampoInversionista(
                    etiqueta = "Teléfono (opcional)",
                    valor = formulario.telefono,
                    enabled = editable,
                    teclado = KeyboardType.Phone,
                    error = errores.telefono,
                    cambiar = { onCambiar(formulario.copy(telefono = it)) }
                )
                CampoInversionista(
                    etiqueta = "Correo (opcional)",
                    valor = formulario.correo,
                    enabled = editable,
                    teclado = KeyboardType.Email,
                    error = errores.correo,
                    cambiar = { onCambiar(formulario.copy(correo = it)) }
                )
                CampoInversionista(
                    etiqueta = "Observaciones (opcional)",
                    valor = formulario.observaciones,
                    enabled = editable,
                    lineas = 3,
                    error = errores.observaciones,
                    cambiar = { onCambiar(formulario.copy(observaciones = it)) }
                )

                error?.let {
                    Text(text = it, color = VapeOnError, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                intentoGuardar = true
                onGuardar()
            }, enabled = !guardando && !cargando) {
                if (guardando) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = VapeOnGold, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = if (requiereComprobacion) "Comprobar" else "Guardar",
                        color = VapeOnGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar, enabled = !guardando) {
                Text("Cancelar", color = VapeOnTextSecondary)
            }
        },
        containerColor = VapeOnSurface
    )
}

@Composable
private fun CampoInversionista(
    etiqueta: String,
    valor: String,
    enabled: Boolean,
    teclado: KeyboardType = KeyboardType.Text,
    lineas: Int = 1,
    error: String? = null,
    cambiar: (String) -> Unit
) {
    OutlinedTextField(
        value = valor,
        onValueChange = cambiar,
        enabled = enabled,
        label = { Text(etiqueta) },
        singleLine = lineas == 1,
        minLines = lineas,
        isError = error != null,
        supportingText = error?.let { mensaje -> { Text(mensaje, color = VapeOnError) } },
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = VapeOnTextPrimary,
            unfocusedTextColor = VapeOnTextPrimary,
            disabledTextColor = VapeOnTextSecondary,
            focusedBorderColor = VapeOnGold,
            unfocusedBorderColor = VapeOnInputBorder,
            disabledBorderColor = VapeOnDivider,
            focusedLabelColor = VapeOnGold,
            unfocusedLabelColor = VapeOnTextSecondary,
            disabledLabelColor = VapeOnTextMuted,
            cursorColor = VapeOnGold
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun inicialesNombre(nombre: String): String {
    val partes = nombre.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(2)
    val iniciales = partes.mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    return iniciales.ifBlank { "IN" }
}

private fun fechaRegistroLegible(fecha: String): String = runCatching {
    Instant.parse(fecha)
        .atZone(ZoneId.of("America/Lima"))
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT))
}.getOrDefault(fecha)
