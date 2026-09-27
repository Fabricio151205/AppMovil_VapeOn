package com.example.vapeon_movil

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.services.UsuarioService
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.utils.RetrofitClient
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    irRegistro: () -> Unit,
    irRecuperar: () -> Unit,
    irHome: () -> Unit,
    irAdmin: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val usuarioService = remember {
        RetrofitClient.retrofit.create(UsuarioService::class.java)
    }

    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Variable para manejar estados de error visibles en consola o textos
    var mensajeError by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = VapeOnBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo VapeOn oficial
            Image(
                painter = painterResource(id = R.drawable.logo_vapeon),
                contentDescription = "Logo VapeON",
                modifier = Modifier
                    .size(170.dp)
                    .padding(bottom = 8.dp)
            )

            // Título: BIENVENIDO A VAPEON (con acento en VapeOnGold)
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = VapeOnTextPrimary)) {
                        append("BIENVENIDO A ")
                    }
                    withStyle(style = SpanStyle(color = VapeOnGold)) {
                        append("VAPEON")
                    }
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Campo de entrada: Correo o Email
            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it },
                placeholder = {
                    Text(
                        text = "Correo o Email",
                        color = VapeOnTextSecondary,
                        fontSize = 15.sp
                    )
                },
                singleLine = true,
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

            // Campo de entrada: Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = {
                    Text(
                        text = "Password",
                        color = VapeOnTextSecondary,
                        fontSize = 15.sp
                    )
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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

            Spacer(modifier = Modifier.height(12.dp))

            // Enlace: ¿Olvidaste tu contraseña? (alineado a la derecha)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    color = VapeOnTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier
                        .clickable { irRecuperar() }
                        .padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Botón principal: INICIAR SESIÓN (estilo píldora roja)
            Button(
                onClick = {
                    cargando = true
                    mensajeError = ""

                    scope.launch {
                        try {
                            val respuesta = usuarioService.listarUsuarios()
                            val listaUsuarios = respuesta.documents

                            val usuarioValido = listaUsuarios?.find { doc ->
                                doc.fields.correo.stringValue == correo &&
                                        doc.fields.password.stringValue == password
                            }

                            cargando = false

                            if (usuarioValido != null) {
                                val rol = usuarioValido.fields.rol.stringValue
                                if (rol == "ADMIN") {
                                    irAdmin()
                                } else {
                                    irHome()
                                }
                            } else {
                                mensajeError = "Correo o contraseña incorrectos"
                                println(mensajeError)
                            }
                        } catch (e: Exception) {
                            cargando = false
                            mensajeError = "Error de conexión: ${e.localizedMessage}"
                            println(mensajeError)
                        }
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
                        text = "INICIAR SESIÓN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            if (mensajeError.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = mensajeError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Enlace: ¿No tienes una cuenta? Crea un Usuario
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¿No tienes una cuenta? ",
                    color = VapeOnTextPrimary,
                    fontSize = 14.sp
                )
                Text(
                    text = "Crea un Usuario",
                    color = VapeOnGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { irRegistro() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Divisor con "o"
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

            // Botón social: Continuar con Google
            Button(
                onClick = {
                    // Acción social: Continuar con Google
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
                    text = "Continuar con Google",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Botón social: Continuar con Facebook
            Button(
                onClick = {
                    // Acción social: Continuar con Facebook
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
                    text = "Continuar con Facebook",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}