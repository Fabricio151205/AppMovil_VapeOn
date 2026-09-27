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
import com.example.vapeon_movil.services.FirestoreFieldsContainer
import com.example.vapeon_movil.services.FirestoreStringValue
import com.example.vapeon_movil.services.UsuarioFields
import com.example.vapeon_movil.services.UsuarioService
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.utils.RetrofitClient
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    volverLogin: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val usuarioService = remember {
        RetrofitClient.retrofit.create(UsuarioService::class.java)
    }

    var nombre by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var fechaNacimiento by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }

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
                .padding(horizontal = 28.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logotipo oficial de VapeON
            Image(
                painter = painterResource(id = R.drawable.logo_vapeon),
                contentDescription = "Logo VapeON",
                modifier = Modifier
                    .size(150.dp)
                    .padding(bottom = 6.dp)
            )

            // Título: REGISTRATE EN VAPEON (con acento en VapeOnGold)
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = VapeOnTextPrimary)) {
                        append("REGISTRATE EN ")
                    }
                    withStyle(style = SpanStyle(color = VapeOnGold)) {
                        append("VAPEON")
                    }
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Campo: Nombres
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                placeholder = {
                    Text(
                        text = "Nombres",
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

            Spacer(modifier = Modifier.height(12.dp))

            // Campo: Apellidos
            OutlinedTextField(
                value = apellidos,
                onValueChange = { apellidos = it },
                placeholder = {
                    Text(
                        text = "Apellidos",
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

            Spacer(modifier = Modifier.height(12.dp))

            // Campo: Correo o Email
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

            Spacer(modifier = Modifier.height(12.dp))

            // Campo: Telefono
            OutlinedTextField(
                value = telefono,
                onValueChange = { telefono = it },
                placeholder = {
                    Text(
                        text = "Telefono",
                        color = VapeOnTextSecondary,
                        fontSize = 15.sp
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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

            if (telefono.isNotEmpty() && !telefono.startsWith("9")) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "El número de teléfono debe empezar con 9",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Campo: Fecha de Nacimiento (Control ético +18)
            OutlinedTextField(
                value = fechaNacimiento,
                onValueChange = { fechaNacimiento = it },
                placeholder = {
                    Text(
                        text = "Fecha de Nacimiento",
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

            Spacer(modifier = Modifier.height(12.dp))

            // Campo: Password
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

            // Campo: Confirmar Password
            OutlinedTextField(
                value = passwordConfirm,
                onValueChange = { passwordConfirm = it },
                placeholder = {
                    Text(
                        text = "Confirmar Paswword",
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

            if (passwordConfirm.isNotEmpty() && passwordConfirm != password) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Las contraseñas no coinciden",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            if (mensajeError.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = mensajeError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Botón principal: REGISTRARSE (Estilo píldora roja)
            Button(
                onClick = {
                    if (nombre.isBlank() || apellidos.isBlank() || correo.isBlank() || password.isBlank()) {
                        mensajeError = "Por favor completa todos los campos requeridos"
                        return@Button
                    }

                    if (!telefono.startsWith("9")) {
                        mensajeError = "El número de teléfono debe empezar con 9"
                        return@Button
                    }

                    if (password != passwordConfirm) {
                        mensajeError = "Las contraseñas no coinciden"
                        return@Button
                    }

                    cargando = true
                    mensajeError = ""

                    // Estructuramos el objeto según lo que pide la API REST de Firebase
                    val usuarioFirebaseFormat = FirestoreFieldsContainer(
                        fields = UsuarioFields(
                            nombre = FirestoreStringValue(nombre.trim()),
                            apellido = FirestoreStringValue(apellidos.trim()),
                            telefono = FirestoreStringValue(telefono.trim()),
                            fechaNacimiento = FirestoreStringValue(fechaNacimiento.trim()),
                            correo = FirestoreStringValue(correo.trim()),
                            password = FirestoreStringValue(password),
                            rol = FirestoreStringValue("CLIENTE")
                        )
                    )

                    scope.launch {
                        try {
                            // Consumimos el servicio de la API REST
                            usuarioService.crearUsuario(usuarioFirebaseFormat)
                            println("¡Usuario registrado exitosamente en la API REST de Firebase Firestore!")
                            cargando = false
                            // Volvemos al Login
                            volverLogin()
                        } catch (e: Exception) {
                            cargando = false
                            mensajeError = "Error al registrar: ${e.localizedMessage}"
                            println("Error en la API REST de Firebase: ${e.localizedMessage}")
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
                        text = "REGISTRARSE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Enlace: ¿Tienes una cuenta? Inicia Sesion Aquí
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¿Tienes una cuenta? ",
                    color = VapeOnTextPrimary,
                    fontSize = 14.sp
                )
                Text(
                    text = "Inicia Sesion Aquí",
                    color = VapeOnGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { volverLogin() }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

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

            Spacer(modifier = Modifier.height(20.dp))

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
