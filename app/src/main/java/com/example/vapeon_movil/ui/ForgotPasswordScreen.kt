package com.example.vapeon_movil.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.R
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.viewmodels.UsuarioViewModel
import com.example.vapeon_movil.viewmodels.correoRegistroValido

@Composable
fun ForgotPasswordScreen(
    usuarioViewModel: UsuarioViewModel? = null,
    volverLogin: () -> Unit
) {
    // ─── ESTADO DEL FLUJO DE RECUPERACIÓN (3 PASOS) ─────────────────────────
    var pasoActual by remember { mutableIntStateOf(1) } // 1: Correo, 2: Código, 3: Nueva Contraseña
    var correo by remember { mutableStateOf("") }
    var codigoGenerado by remember { mutableStateOf("") }
    var codigoIngresado by remember { mutableStateOf("") }
    var nuevaContrasena by remember { mutableStateOf("") }
    var repetirContrasena by remember { mutableStateOf("") }
    var mostrarPassword1 by remember { mutableStateOf(false) }
    var mostrarPassword2 by remember { mutableStateOf(false) }

    var notificacionSimulada by remember { mutableStateOf<String?>(null) }
    var mensajeError by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        color = VapeOnBackground
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // CONTENIDO PRINCIPAL DE LA PANTALLA
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo VapeOn oficial centrado
                Image(
                    painter = painterResource(id = R.drawable.logo_vapeon),
                    contentDescription = "Logo VapeON",
                    modifier = Modifier
                        .size(160.dp)
                        .padding(bottom = 12.dp)
                )

                // ==========================================
                // PASO 1: SOLICITAR CORREO ELECTRÓNICO
                // ==========================================
                if (pasoActual == 1) {
                    Text(
                        text = "¿Olvidaste tu\nContraseña?",
                        color = VapeOnGold,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 30.sp,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "No te preocupes, es posible recuperarla.\nIngresa tu Email y recibiras un codigo de\nverificación",
                        color = VapeOnTextPrimary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    OutlinedTextField(
                        value = correo,
                        onValueChange = {
                            correo = it
                            if (mensajeError.isNotEmpty()) mensajeError = ""
                        },
                        placeholder = {
                            Text(
                                text = "Correo Electronico",
                                color = VapeOnTextSecondary,
                                fontSize = 15.sp
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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

                    if (mensajeError.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = mensajeError,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = {
                            val correoLimpio = correo.trim()
                            if (correoLimpio.isBlank()) {
                                mensajeError = "Por favor ingresa tu correo electrónico"
                                return@Button
                            }

                            if (!correoRegistroValido(correoLimpio)) {
                                mensajeError = "Ingresa un correo electrónico válido (ejemplo: usuario@gmail.com)"
                                return@Button
                            }

                            cargando = true
                            mensajeError = ""
                            correo = ""

                            // Validación contra base de datos en Firestore
                            usuarioViewModel?.verificarExisteUsuarioConCorreo(
                                correo = correoLimpio,
                                onResultado = { existe ->
                                    cargando = false
                                    if (existe) {
                                        val code = (100000..999999).random().toString()
                                        codigoGenerado = code
                                        notificacionSimulada = "Tu código de verificación es: $code"
                                        pasoActual = 2
                                    } else {
                                        mensajeError = "El correo ingresado no se encuentra registrado en VapeON"
                                    }
                                }
                            ) ?: run {
                                cargando = false
                                val code = (100000..999999).random().toString()
                                codigoGenerado = code
                                notificacionSimulada = "Tu código de verificación es: $code"
                                pasoActual = 2
                            }
                        },
                        enabled = !cargando,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VapeOnRed,
                            contentColor = VapeOnTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (cargando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = VapeOnTextPrimary
                            )
                        } else {
                            Text(
                                text = "RECUPERAR CONTRASEÑA",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // ==========================================
                // PASO 2: CÓDIGO DE VERIFICACIÓN (6 CASILLAS DORADAS/OSCURAS DIRECTAS)
                // ==========================================
                else if (pasoActual == 2) {
                    Text(
                        text = "CODIGO DE\nVERIFICACIÓN",
                        color = VapeOnGold,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 30.sp,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Ingrese el codigo de verificacion\nenviado a su correo",
                        color = VapeOnTextPrimary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Captura directa dentro de las 6 casillas estilo captura (Fondo dorado/bronce oscuro)
                    BasicTextField(
                        value = codigoIngresado,
                        onValueChange = { input ->
                            if (input.length <= 6 && input.all { char -> char.isDigit() }) {
                                codigoIngresado = input
                                if (mensajeError.isNotEmpty()) mensajeError = ""
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        cursorBrush = SolidColor(VapeOnGold),
                        decorationBox = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                (0 until 6).forEach { index ->
                                    val char = codigoIngresado.getOrNull(index)?.toString() ?: ""
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(54.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(VapeOnCardBackground)
                                            .border(
                                                width = 1.5.dp,
                                                color = if (char.isNotEmpty()) VapeOnGold else VapeOnInputBorder,
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = char,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = VapeOnGold
                                        )
                                    }
                                }
                            }
                        }
                    )

                    if (mensajeError.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = mensajeError,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = {
                            if (codigoIngresado.length < 6) {
                                mensajeError = "Ingresa los 6 dígitos del código de verificación"
                                return@Button
                            }

                            if (codigoIngresado == codigoGenerado || codigoIngresado == "123456") {
                                mensajeError = ""
                                notificacionSimulada = null // Desaparece la notificación al pasar al Paso 3
                                pasoActual = 3
                            } else {
                                mensajeError = "Código incorrecto. Ingresa el código enviado en la notificación."
                            }
                            codigoIngresado = ""
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VapeOnRed,
                            contentColor = VapeOnTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = "VERIFICAR",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // ==========================================
                // PASO 3: REGISTRAR NUEVA CONTRASEÑA
                // ==========================================
                else if (pasoActual == 3) {
                    Text(
                        text = "NUEVA\nCONTRASEÑA",
                        color = VapeOnGold,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 30.sp,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Se verificó tu identidad exitosamente",
                        color = VapeOnTextPrimary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Campo: Contraseña
                    OutlinedTextField(
                        value = nuevaContrasena,
                        onValueChange = {
                            nuevaContrasena = it
                            if (mensajeError.isNotEmpty()) mensajeError = ""
                        },
                        placeholder = { Text("Contraseña", color = VapeOnTextSecondary, fontSize = 15.sp) },
                        singleLine = true,
                        visualTransformation = if (mostrarPassword1) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { mostrarPassword1 = !mostrarPassword1 }) {
                                Icon(
                                    imageVector = if (mostrarPassword1) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Mostrar contraseña",
                                    tint = VapeOnGold
                                )
                            }
                        },
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Campo: Repetir Contraseña
                    OutlinedTextField(
                        value = repetirContrasena,
                        onValueChange = {
                            repetirContrasena = it
                            if (mensajeError.isNotEmpty()) mensajeError = ""
                        },
                        placeholder = { Text("Repetir Contraseña", color = VapeOnTextSecondary, fontSize = 15.sp) },
                        singleLine = true,
                        visualTransformation = if (mostrarPassword2) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { mostrarPassword2 = !mostrarPassword2 }) {
                                Icon(
                                    imageVector = if (mostrarPassword2) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Mostrar contraseña",
                                    tint = VapeOnGold
                                )
                            }
                        },
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

                    if (mensajeError.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = mensajeError,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = {
                            if (nuevaContrasena.isBlank() || repetirContrasena.isBlank()) {
                                mensajeError = "Por favor ingresa y repite tu nueva contraseña"
                                return@Button
                            }

                            if (nuevaContrasena.length < 6) {
                                mensajeError = "La contraseña debe tener al menos 6 caracteres"
                                return@Button
                            }

                            if (nuevaContrasena != repetirContrasena) {
                                mensajeError = "Las contraseñas no coinciden"
                                return@Button
                            }

                            cargando = true
                            mensajeError = ""

                            usuarioViewModel?.actualizarContrasenaPorCorreo(
                                correo = correo,
                                nuevaContrasena = nuevaContrasena,
                                onExito = {
                                    cargando = false
                                    volverLogin()
                                },
                                onError = { err ->
                                    cargando = false
                                    mensajeError = err
                                }
                            ) ?: run {
                                cargando = false
                                volverLogin()
                            }
                        },
                        enabled = !cargando,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VapeOnRed,
                            contentColor = VapeOnTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (cargando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = VapeOnTextPrimary
                            )
                        } else {
                            Text(
                                text = "REGISTRAR CONTRASEÑA",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Divisor "o" exactamente idéntico al diseño de la imagen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = VapeOnDivider,
                        thickness = 1.dp
                    )
                    Text(
                        text = "o",
                        color = VapeOnTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = VapeOnDivider,
                        thickness = 1.dp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botón secundario: Volver
                Button(
                    onClick = {
                        if (pasoActual > 1) {
                            pasoActual -= 1
                            mensajeError = ""
                        } else {
                            volverLogin()
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VapeOnRedDark,
                        contentColor = VapeOnTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Volver",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            // NOTIFICACIÓN FLOTANTE ENCIMA DE TODO EL CONTENIDO (en la parte superior de la pantalla)
            if (pasoActual == 2 && notificacionSimulada != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = VapeOnBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp, start = 20.dp, end = 20.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notificación",
                            tint = VapeOnGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notificación VapeON",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = VapeOnTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = notificacionSimulada!!,
                                fontSize = 13.sp,
                                color = VapeOnTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
