package com.example.vapeon_movil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.entities.edadDesde
import com.example.vapeon_movil.entities.esMayorDeEdad
import com.example.vapeon_movil.entities.rolLegible
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.viewmodels.UsuarioViewModel
import kotlinx.coroutines.launch

private enum class CampoPerfil(
    val etiqueta: String,
    val icono: ImageVector,
    val tipoTeclado: KeyboardType,
    val pista: String
) {
    NOMBRES("Nombres", Icons.Default.Person, KeyboardType.Text, "Tus nombres"),
    APELLIDOS("Apellidos", Icons.Default.Person, KeyboardType.Text, "Tus apellidos"),
    CORREO("Correo electrónico", Icons.Default.Email, KeyboardType.Email, "correo@ejemplo.com"),
    TELEFONO("Teléfono", Icons.Default.Phone, KeyboardType.Phone, "9XXXXXXXX"),
    FECHA_NACIMIENTO("Fecha de nacimiento", Icons.Default.CalendarMonth, KeyboardType.Text, "dd/mm/aaaa")
}

@Composable
fun PerfilScreen(
    usuario: Usuario?,
    usuarioViewModel: UsuarioViewModel,
    volver: () -> Unit,
    cambiarContrasena: () -> Unit,
    abrirAccesibilidad: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var campoEditando by remember { mutableStateOf<CampoPerfil?>(null) }
    var valorEditado by remember { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }
    var errorDialogo by remember { mutableStateOf("") }

    val edad = edadDesde(usuario?.fechaNacimiento.orEmpty())
    val esMayor = usuario?.let { esMayorDeEdad(it.fechaNacimiento) } == true

    fun valorActual(campo: CampoPerfil): String = when (campo) {
        CampoPerfil.NOMBRES -> usuario?.nombre.orEmpty()
        CampoPerfil.APELLIDOS -> usuario?.apellido.orEmpty()
        CampoPerfil.CORREO -> usuario?.correo.orEmpty()
        CampoPerfil.TELEFONO -> usuario?.telefono.orEmpty()
        CampoPerfil.FECHA_NACIMIENTO -> usuario?.fechaNacimiento.orEmpty()
    }

    fun abrirEditor(campo: CampoPerfil) {
        campoEditando = campo
        valorEditado = valorActual(campo)
        errorDialogo = ""
    }

    fun guardarCampo(campo: CampoPerfil, nuevoValor: String) {
        val actual = usuario ?: return
        if (guardando) return
        val nombre = if (campo == CampoPerfil.NOMBRES) nuevoValor else actual.nombre
        val apellido = if (campo == CampoPerfil.APELLIDOS) nuevoValor else actual.apellido
        val correo = if (campo == CampoPerfil.CORREO) nuevoValor else actual.correo
        val telefono = if (campo == CampoPerfil.TELEFONO) nuevoValor else actual.telefono
        val fecha = if (campo == CampoPerfil.FECHA_NACIMIENTO) nuevoValor else actual.fechaNacimiento

        guardando = true
        errorDialogo = ""
        usuarioViewModel.actualizarPerfil(
            nombre = nombre,
            apellido = apellido,
            telefono = telefono,
            fechaNacimiento = fecha,
            correo = correo,
            onExito = {
                guardando = false
                campoEditando = null
                scope.launch { snackbarHostState.showSnackbar("Perfil actualizado correctamente") }
            },
            onError = { mensaje ->
                guardando = false
                errorDialogo = mensaje
            }
        )
    }

    BackHandler(enabled = !guardando, onBack = volver)

    VapeON_MovilTheme {
        Scaffold(
            containerColor = VapeOnBackground,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                EncabezadoPerfil(volver = volver)

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Mi Perfil",
                    color = VapeOnTextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Administra tu información personal",
                    color = VapeOnTextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                TarjetaIdentidad(usuario = usuario, edad = edad, esMayor = esMayor)

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "DATOS PERSONALES",
                    color = VapeOnTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                CampoPerfilCard(
                    icono = CampoPerfil.NOMBRES.icono,
                    etiqueta = "Nombres",
                    valor = usuario?.nombre.orEmpty(),
                    editable = usuario != null,
                    onClick = { abrirEditor(CampoPerfil.NOMBRES) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                CampoPerfilCard(
                    icono = CampoPerfil.APELLIDOS.icono,
                    etiqueta = "Apellidos",
                    valor = usuario?.apellido.orEmpty(),
                    editable = usuario != null,
                    onClick = { abrirEditor(CampoPerfil.APELLIDOS) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                CampoPerfilCard(
                    icono = CampoPerfil.CORREO.icono,
                    etiqueta = "Correo electrónico",
                    valor = usuario?.correo.orEmpty(),
                    editable = usuario != null,
                    onClick = { abrirEditor(CampoPerfil.CORREO) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                CampoPerfilCard(
                    icono = CampoPerfil.TELEFONO.icono,
                    etiqueta = "Teléfono",
                    valor = usuario?.telefono.orEmpty(),
                    editable = usuario != null,
                    onClick = { abrirEditor(CampoPerfil.TELEFONO) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                CampoPerfilCard(
                    icono = CampoPerfil.FECHA_NACIMIENTO.icono,
                    etiqueta = "Fecha de nacimiento",
                    valor = usuario?.fechaNacimiento.orEmpty(),
                    editable = usuario != null,
                    onClick = { abrirEditor(CampoPerfil.FECHA_NACIMIENTO) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                CampoPerfilCard(
                    icono = Icons.Default.Badge,
                    etiqueta = "Rol",
                    valor = usuario?.let { rolLegible(it.rol) }.orEmpty(),
                    editable = false,
                    onClick = {}
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = cambiarContrasena,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VapeOnRed,
                        contentColor = VapeOnTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = VapeOnTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CAMBIAR CONTRASEÑA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = abrirAccesibilidad,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, VapeOnGold),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VapeOnGold),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessibilityNew,
                        contentDescription = null,
                        tint = VapeOnGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OPCIONES DE ACCESIBILIDAD",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.4.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    campoEditando?.let { campo ->
        EditarCampoDialog(
            campo = campo,
            valorInicial = valorEditado,
            guardando = guardando,
            error = errorDialogo,
            onCancelar = {
                if (!guardando) {
                    campoEditando = null
                    errorDialogo = ""
                }
            },
            onGuardar = { nuevo -> guardarCampo(campo, nuevo) }
        )
    }
}

@Composable
private fun EncabezadoPerfil(volver: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = volver) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = VapeOnTextPrimary,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "VAPEON",
            color = VapeOnGold,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(VapeOnRed),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Perfil",
                tint = VapeOnTextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun TarjetaIdentidad(usuario: Usuario?, edad: Int?, esMayor: Boolean) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(VapeOnRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Avatar del usuario",
                    tint = VapeOnTextPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val nombreCompleto = listOfNotNull(
                usuario?.nombre?.trim()?.takeIf { it.isNotEmpty() },
                usuario?.apellido?.trim()?.takeIf { it.isNotEmpty() }
            ).joinToString(" ").uppercase()

            Text(
                text = nombreCompleto.ifBlank { "USUARIO VAPEON" },
                color = VapeOnTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = VapeOnRedDark
                ) {
                    Text(
                        text = rolLegible(usuario?.rol.orEmpty()).uppercase(),
                        color = VapeOnTextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (esMayor) VapeOnGold else VapeOnError
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (esMayor) "+18" else "-18",
                            color = VapeOnBackground,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (esMayor) "Mayor de 18 años" else "Menor de 18 años",
                            color = VapeOnBackground,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (esMayor && edad != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "$edad años",
                    color = VapeOnTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun CampoPerfilCard(
    icono: ImageVector,
    etiqueta: String,
    valor: String,
    editable: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = VapeOnCardBackground,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VapeOnSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = VapeOnGold,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = etiqueta.uppercase(),
                    color = VapeOnTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = valor.ifBlank { "No registrado" },
                    color = if (valor.isBlank()) VapeOnTextMuted else VapeOnTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            if (editable) {
                IconButton(onClick = onClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar $etiqueta",
                        tint = VapeOnGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(12.dp))
            }
        }
    }
}

@Composable
private fun EditarCampoDialog(
    campo: CampoPerfil,
    valorInicial: String,
    guardando: Boolean,
    error: String,
    onCancelar: () -> Unit,
    onGuardar: (String) -> Unit
) {
    var valor by remember(campo, valorInicial) { mutableStateOf(valorInicial) }

    AlertDialog(
        onDismissRequest = { if (!guardando) onCancelar() },
        containerColor = VapeOnSurface,
        titleContentColor = VapeOnTextPrimary,
        textContentColor = VapeOnTextPrimary,
        title = {
            Text(
                text = "Editar ${campo.etiqueta.lowercase()}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = valor,
                    onValueChange = { valor = it },
                    singleLine = true,
                    placeholder = {
                        Text(campo.pista, color = VapeOnTextSecondary, fontSize = 14.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = campo.tipoTeclado),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = VapeOnInputBackground,
                        unfocusedContainerColor = VapeOnInputBackground,
                        disabledContainerColor = VapeOnInputBackground,
                        focusedBorderColor = VapeOnGold,
                        unfocusedBorderColor = VapeOnInputBorder,
                        focusedTextColor = VapeOnTextPrimary,
                        unfocusedTextColor = VapeOnTextPrimary,
                        cursorColor = VapeOnGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
                if (campo == CampoPerfil.FECHA_NACIMIENTO) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Formato: dd/mm/aaaa. Debes ser mayor de 18 años.",
                        color = VapeOnTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onGuardar(valor) },
                enabled = !guardando
            ) {
                if (guardando) {
                    CircularProgressIndicator(
                        color = VapeOnGold,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text("Guardar", color = VapeOnGold, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar, enabled = !guardando) {
                Text("Cancelar", color = VapeOnTextSecondary)
            }
        }
    )
}
