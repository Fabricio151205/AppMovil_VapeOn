package com.example.vapeon_movil.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.vapeon_movil.R
import com.example.vapeon_movil.auth.GoogleAuthManager
import com.example.vapeon_movil.auth.SesionGoogle
import com.example.vapeon_movil.services.PerfilGoogleContainer
import com.example.vapeon_movil.services.UsuarioAutenticadoService
import com.example.vapeon_movil.services.dto.FirestoreStringValue
import com.example.vapeon_movil.services.dto.UsuarioDocumento
import com.example.vapeon_movil.utils.RetrofitClient
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.example.vapeon_movil.viewmodels.UsuarioViewModel
import com.example.vapeon_movil.ui.theme.*

@Composable
fun LoginScreen(
    irRegistro: () -> Unit,
    irRecuperar: () -> Unit,
    irHome: () -> Unit,
    irAdmin: () -> Unit,
    usuarioViewModel: UsuarioViewModel
) {
    // Estado local solo de UI (campos del formulario)
    var correo by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    // Estado del ViewModel (login, carga, error)
    val cargando = usuarioViewModel.cargandoLogin
    val mensajeError = usuarioViewModel.errorLogin ?: ""

    val actividad = LocalActivity.current
    val googleAuth = remember(actividad) { actividad?.let(::GoogleAuthManager) }

    val servicioAutenticado = remember {
        RetrofitClient.retrofit.create(
            UsuarioAutenticadoService::class.java
        )
    }

    val scope = rememberCoroutineScope()
    var cargandoGoogle by remember { mutableStateOf(false) }
    var mensajeGoogle by rememberSaveable { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = VapeOnBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo VapeOn oficial
            Image(
                painter = painterResource(id = R.drawable.logo_vapeon),
                contentDescription = "Logo Vapeon",
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
                        .clickable(enabled = !cargandoGoogle) { irRecuperar() }
                        .padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Botón principal: INICIAR SESIÓN (estilo píldora roja)
            Button(
                onClick = {
                    if (cargandoGoogle) return@Button
                    mensajeGoogle = null
                    usuarioViewModel.iniciarSesion(
                        correo = correo,
                        password = password,
                        onExito = { esAdmin ->
                            if (esAdmin) irAdmin() else irHome()
                        }
                    )
                },
                enabled = !cargando && !cargandoGoogle,
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
                    modifier = Modifier.clickable(enabled = !cargandoGoogle) { irRegistro() }
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
                    if (googleAuth == null || cargandoGoogle || usuarioViewModel.cargandoLogin) {
                        return@Button
                    }
                    usuarioViewModel.limpiarError()
                    mensajeGoogle = null
                    cargandoGoogle = true
                    scope.launch {
                        try {
                            val sesion = googleAuth.iniciarSesionConGoogle()
                            var accesoCompletado = false

                            try {
                                val perfil = obtenerOCrearPerfilGoogle(
                                    sesion = sesion,
                                    servicio = servicioAutenticado
                                )

                                // Verificar que el documento recuperado sea suyo
                                check(
                                    perfil.name.substringAfterLast("/") == sesion.uid
                                ) {
                                    "El perfil no corresponde al usuario autenticado"
                                }

                                // En este flujo solo admitimos CLIENTE.
                                // Los administradores requieren autorización separada.
                                check(
                                    perfil.fields.rol?.stringValue == "CLIENTE"
                                ) {
                                    "El perfil requiere revisión"
                                }

                                check(
                                    perfil.fields.correo?.stringValue
                                        ?.equals(sesion.correo, ignoreCase = true) == true
                                ) {
                                    "El correo del perfil no coincide"
                                }

                                usuarioViewModel.establecerSesionGoogle(perfil)

                                accesoCompletado = true
                                irHome()

                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                mensajeGoogle =
                                    "No se pudo completar el acceso con Google."
                            } finally {
                                if (!accesoCompletado) {
                                    googleAuth.cerrarSesionProvisional(sesion)
                                }
                            }

                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: GetCredentialCancellationException) {
                            mensajeGoogle = "Se canceló el acceso con Google."
                        } catch (_: NoCredentialException) {
                            mensajeGoogle = "No hay una cuenta de Google disponible en este dispositivo."
                        } catch (_: FirebaseAuthUserCollisionException) {
                            mensajeGoogle = "El correo ya tiene una cuenta y requiere vinculación."
                        } catch (_: Exception) {
                            mensajeGoogle = "No se pudo continuar con Google. Inténtalo de nuevo."
                        } finally {
                            cargandoGoogle = false
                        }
                    }
                },
                enabled = googleAuth != null && !cargando && !cargandoGoogle,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VapeOnRedDark,
                    contentColor = VapeOnTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (cargandoGoogle) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = VapeOnTextPrimary
                    )
                } else {
                    Text(
                        text = "Continuar con Google",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            mensajeGoogle?.let { mensaje ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = mensaje,
                    color = VapeOnTextPrimary,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Botón social: Continuar con Facebook
            Button(
                onClick = {}, enabled = false,
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
                    text = "Facebook · No disponible",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}


suspend fun obtenerOCrearPerfilGoogle(
    sesion: SesionGoogle,
    servicio: UsuarioAutenticadoService
): UsuarioDocumento {

    val autorizacion = "Bearer ${sesion.tokenFirebase}"

    // Primero consultar si existe el perfil
    try {
        return servicio.obtenerPerfil(
            uid = sesion.uid,
            autorizacion = autorizacion
        )
    } catch (error: CancellationException) {
        throw error
    } catch (error: retrofit2.HttpException) {
        if (error.code() != 404) throw error
    }

    // El perfil todavía no existe
    val nuevoPerfil = PerfilGoogleContainer(
        fields = mapOf(
            "nombre" to FirestoreStringValue(
                sesion.nombre.ifBlank { "Usuario" }
            ),
            "correo" to FirestoreStringValue(
                sesion.correo
            ),
            "rol" to FirestoreStringValue("CLIENTE")
        )
    )

    return try {
        servicio.crearPerfilGoogle(
            uid = sesion.uid,
            autorizacion = autorizacion,
            body = nuevoPerfil
        )
    } catch (error: CancellationException) {
        throw error
    } catch (error: retrofit2.HttpException) {
        if (error.code() == 409) {
            // Otro intento pudo crear el mismo perfil
            servicio.obtenerPerfil(
                uid = sesion.uid,
                autorizacion = autorizacion
            )
        } else {
            throw error
        }
    }
}

