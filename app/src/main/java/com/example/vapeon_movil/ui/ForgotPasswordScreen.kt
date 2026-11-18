package com.example.vapeon_movil.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.R
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.viewmodels.UsuarioViewModel
import com.example.vapeon_movil.viewmodels.correoRegistroValido

/**
 * Recuperación de contraseña con Firebase Authentication.
 *
 * Se envía un correo real de restablecimiento; no se generan códigos locales ni
 * se escribe ninguna contraseña en Firestore.
 */
@Composable
fun ForgotPasswordScreen(
    usuarioViewModel: UsuarioViewModel? = null,
    correoInicial: String = "",
    volverLogin: () -> Unit
) {
    var correo by remember { mutableStateOf(correoInicial) }
    var mensajeError by remember { mutableStateOf("") }
    var mensajeExito by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }

    fun enviar() {
        val correoLimpio = correo.trim()
        if (correoLimpio.isBlank()) {
            mensajeError = "Por favor ingresa tu correo electrónico"
            return
        }
        if (!correoRegistroValido(correoLimpio)) {
            mensajeError = "Ingresa un correo electrónico válido (ejemplo: usuario@gmail.com)"
            return
        }
        val modelo = usuarioViewModel
        if (modelo == null) {
            mensajeError = "No se pudo iniciar la recuperación. Vuelve a intentarlo."
            return
        }
        cargando = true
        mensajeError = ""
        modelo.enviarCorreoRestablecimiento(
            correo = correoLimpio,
            onExito = {
                cargando = false
                mensajeExito = correoLimpio
            },
            onError = { error ->
                cargando = false
                mensajeError = error
            }
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        color = VapeOnBackground
    ) {
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
            Image(
                painter = painterResource(id = R.drawable.logo_vapeon),
                contentDescription = "Logo VapeON",
                modifier = Modifier
                    .size(160.dp)
                    .padding(bottom = 12.dp)
            )

            if (mensajeExito == null) {
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
                    text = "No te preocupes, es posible recuperarla.\n" +
                        "Ingresa tu correo y recibirás un enlace\npara restablecer tu contraseña.",
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
                    onClick = { enviar() },
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
                            text = "ENVIAR ENLACE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            } else {
                Text(
                    text = "CORREO\nENVIADO",
                    color = VapeOnGold,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Enviamos un enlace de restablecimiento a\n$mensajeExito\n" +
                        "Revisa tu bandeja de entrada y la carpeta de spam.",
                    color = VapeOnTextPrimary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = { volverLogin() },
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
                        text = "VOLVER AL LOGIN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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

            Button(
                onClick = { volverLogin() },
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
    }
}
