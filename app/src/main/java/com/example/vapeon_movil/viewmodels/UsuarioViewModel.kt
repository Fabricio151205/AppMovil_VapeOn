package com.example.vapeon_movil.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vapeon_movil.auth.AutenticacionFirebase
import com.example.vapeon_movil.auth.ErrorAutenticacion
import com.example.vapeon_movil.auth.FirebaseAutenticacion
import com.example.vapeon_movil.auth.TipoErrorAutenticacion
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.entities.edadDesde
import com.example.vapeon_movil.services.UsuarioPerfilService
import com.example.vapeon_movil.services.UsuarioService
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.utils.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

/**
 * ViewModel de sesión del Usuario.
 *
 * Gestiona el ciclo completo de autenticación: login, datos del usuario activo
 * y cierre de sesión. Al vivir a nivel de Activity (escoped en VapeONApp),
 * el rol y los datos del usuario persisten mientras la app está activa.
 *
 * Pantallas consumidoras: LoginScreen, VapeONApp (para determinar la ruta inicial).
 */
class UsuarioViewModel(
    private val usuarioService: UsuarioService = RetrofitClient.retrofit.create(UsuarioService::class.java),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val autenticacion: AutenticacionFirebase = FirebaseAutenticacion(),
    private val perfilService: UsuarioPerfilService =
        RetrofitClient.retrofit.create(UsuarioPerfilService::class.java)
) : ViewModel() {

    private data class RegistroPendiente(
        val correo: String,
        val cuerpo: UsuarioContainer
    )

    private data class IdentidadRegistro(
        val uid: String,
        val perfilExistente: Boolean
    )

    // ─── Servicio de red ───────────────────────────────────────────────────
    private var operacionLogin: Job? = null
    private var operacionRegistro: Job? = null
    private var operacionRestauracion: Job? = null
    private var registroPendiente: RegistroPendiente? = null
    private val correosRegistrados = mutableSetOf<String>()
    var registroCompletado by mutableStateOf(false)
        private set
    fun consumirRegistroCompletado() { registroCompletado = false }

    // ─── Estado observable ─────────────────────────────────────────────────

    /** Datos completos del usuario que inició sesión; null si no hay sesión activa */
    var usuarioActual by mutableStateOf<Usuario?>(null)
        private set

    /** true si el usuario tiene rol ADMIN */
    var esAdmin by mutableStateOf(false)
        private set

    /** true mientras se ejecuta la petición de login */
    var cargandoLogin by mutableStateOf(false)
        private set

    /** Mensaje de error del último intento de login; null si no hubo error */
    var errorLogin by mutableStateOf<String?>(null)
        private set

    var cargandoRegistro by mutableStateOf(false)
        private set
    var errorRegistro by mutableStateOf<String?>(null)
        private set

    /** true mientras se intenta recuperar la sesión persistida de Firebase. */
    var restaurandoSesion by mutableStateOf(false)
        private set

    /** true cuando ya se intentó restaurar la sesión al menos una vez. */
    var restauracionCompletada by mutableStateOf(false)
        private set

    fun registrarUsuario(
        nombre: String, apellidos: String, correo: String, telefono: String,
        dni: String, fechaNacimiento: String, password: String, passwordConfirm: String,
        onExito: (() -> Unit)? = null
    ) {
        if (cargandoRegistro) return
        errorRegistro = validarRegistro(nombre, apellidos, correo, telefono, dni, fechaNacimiento,
            password, passwordConfirm)
        if (errorRegistro != null) return

        val correoLimpio = correo.trim()
        if (correoLimpio.lowercase(Locale.ROOT) in correosRegistrados) {
            errorRegistro = "Ya existe una cuenta con ese correo."
            return
        }
        // La contraseña nunca forma parte del documento de Firestore.
        val cuerpo = UsuarioContainer(UsuarioCampos(
            nombre = FirestoreStringValue(nombre.trim()),
            apellido = FirestoreStringValue(apellidos.trim()),
            telefono = FirestoreStringValue(telefono),
            dni = FirestoreStringValue(dni),
            fechaNacimiento = FirestoreStringValue(fechaNacimiento.trim()),
            correo = FirestoreStringValue(correoLimpio),
            rol = FirestoreStringValue("CLIENTE")
        ))
        val pendiente = RegistroPendiente(correoLimpio, cuerpo)
        registroPendiente = pendiente
        cargandoRegistro = true
        errorRegistro = null

        operacionRegistro = viewModelScope.launch(dispatcher) {
            var perfilConfirmado = false
            try {
                val identidad = crearOReutilizarIdentidad(correoLimpio, password)
                coroutineContext.ensureActive()
                perfilConfirmado = identidad.perfilExistente ||
                    asegurarPerfil(identidad.uid, cuerpo)
                coroutineContext.ensureActive()
                if (!perfilConfirmado) {
                    errorRegistro =
                        "No se pudo crear tu perfil. Reintenta con los mismos datos; no se duplicará la cuenta."
                    return@launch
                }
                enviarVerificacionSegura()
                // El registro vuelve al login: no se deja una sesión abierta implícita.
                autenticacion.cerrarSesion()
                correosRegistrados += correoLimpio.lowercase(Locale.ROOT)
                registroPendiente = null
                registroCompletado = true
                onExito?.invoke()
            } catch (e: CancellationException) {
                throw e
            } catch (e: ErrorAutenticacion) {
                coroutineContext.ensureActive()
                errorRegistro = e.message
            } catch (e: HttpException) {
                coroutineContext.ensureActive()
                if (e.code() in listOf(400, 401, 403)) registroPendiente = null
                errorRegistro =
                    "No se pudo confirmar el registro. Reintenta con los mismos datos; se comprobará el mismo intento."
            } catch (e: Exception) {
                coroutineContext.ensureActive()
                errorRegistro =
                    "No se pudo confirmar el registro. Reintenta con los mismos datos; se comprobará el mismo intento."
            } finally {
                if (!perfilConfirmado) runCatching { autenticacion.cerrarSesion() }
                if (coroutineContext[Job]?.isCancelled != true) cargandoRegistro = false
            }
        }
    }

    /**
     * Crea la identidad en Firebase Authentication.
     *
     * Si un intento anterior ya la creó (por ejemplo, se perdió la respuesta al
     * guardar el perfil), se reutiliza la misma cuenta en lugar de duplicarla.
     */
    private suspend fun crearOReutilizarIdentidad(
        correo: String,
        password: String
    ): IdentidadRegistro {
        return try {
            IdentidadRegistro(autenticacion.registrar(correo, password).uid, perfilExistente = false)
        } catch (e: ErrorAutenticacion) {
            if (e.tipo != TipoErrorAutenticacion.CORREO_YA_REGISTRADO ||
                registroPendiente?.correo != correo
            ) throw e

            // Reintento de un registro parcial: solo el dueño de la contraseña
            // puede recuperar la misma identidad.
            val sesion = try {
                autenticacion.iniciarSesion(correo, password)
            } catch (login: ErrorAutenticacion) {
                throw e
            }
            val existente = try {
                usuarioService.mostrarUsuario(sesion.uid)
            } catch (e404: HttpException) {
                if (e404.code() == 404) null else throw e404
            }
            IdentidadRegistro(sesion.uid, perfilExistente = existente != null)
        }
    }

    /** Crea `usuarios/{uid}` una sola vez; una respuesta perdida no duplica datos. */
    private suspend fun asegurarPerfil(uid: String, cuerpo: UsuarioContainer): Boolean {
        return try {
            usuarioService.crearUsuario(cuerpo, uid)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            try {
                val existente = usuarioService.mostrarUsuario(uid)
                existente.name.substringAfterLast('/') == uid
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (_: Exception) {
                false
            }
        }
    }

    private suspend fun enviarVerificacionSegura() {
        try {
            autenticacion.enviarVerificacionCorreo()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // La verificación puede reenviarse al iniciar sesión; no invalida el registro.
        }
    }

    // ─── Acciones ──────────────────────────────────────────────────────────

    /**
     * Autentica con Firebase Authentication y carga `usuarios/{uid}`.
     *
     * La contraseña se valida únicamente en Firebase; Firestore solo guarda el
     * perfil. El rol se lee del documento protegido por reglas del servidor.
     *
     * @param onExito Callback con `esAdmin: Boolean` cuando el login es exitoso.
     * @param onError Callback con el mensaje de error cuando el login falla.
     */
    fun iniciarSesion(
        correo: String,
        password: String,
        onExito: (esAdmin: Boolean) -> Unit,
        onError: (mensaje: String) -> Unit = {}
    ) {
        if (cargandoLogin) return
        // Un intento nuevo nunca conserva la identidad de un login anterior.
        cerrarSesion()
        val correoLimpio = correo.trim()

        if (correoLimpio.isEmpty() || password.isBlank()) {
            errorLogin = "Por favor, ingresa tu correo y contraseña"
            onError(errorLogin!!)
            return
        }

        cargandoLogin = true
        operacionLogin = viewModelScope.launch(dispatcher) {
            try {
                val sesion = autenticacion.iniciarSesion(correoLimpio, password)
                coroutineContext.ensureActive()
                val documento = cargarPerfil(sesion.uid)
                aplicarPerfil(documento)
                onExito(esAdmin)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ErrorAutenticacion) {
                limpiarSesionLocal()
                autenticacion.cerrarSesion()
                errorLogin = e.message
                onError(e.message)
            } catch (_: Exception) {
                limpiarSesionLocal()
                autenticacion.cerrarSesion()
                errorLogin = "No se pudo cargar tu perfil. Inténtalo de nuevo."
                onError(errorLogin!!)
            } finally {
                if (coroutineContext[Job]?.isCancelled != true) cargandoLogin = false
            }
        }
    }

    /**
     * Restaura la sesión persistida de Firebase al abrir la aplicación.
     *
     * Sin perfil válido no se concede acceso: una sesión incompleta se cierra.
     */
    fun restaurarSesion() {
        if (restauracionCompletada || operacionRestauracion?.isActive == true) return
        val sesion = runCatching { autenticacion.sesionActual() }.getOrNull()
        if (sesion == null) {
            restaurandoSesion = false
            restauracionCompletada = true
            return
        }
        restaurandoSesion = true
        operacionRestauracion = viewModelScope.launch(dispatcher) {
            try {
                val documento = cargarPerfil(sesion.uid)
                coroutineContext.ensureActive()
                aplicarPerfil(documento)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ErrorAutenticacion) {
                // Perfil inexistente o sin permiso: la identidad no sirve para VapeOn.
                if (e.tipo == TipoErrorAutenticacion.USUARIO_NO_ENCONTRADO) {
                    autenticacion.cerrarSesion()
                    limpiarSesionLocal()
                }
            } catch (_: Exception) {
                // Error temporal: se conserva la sesión de Firebase para reintentar.
            } finally {
                if (coroutineContext[Job]?.isCancelled != true) {
                    restaurandoSesion = false
                    restauracionCompletada = true
                }
            }
        }
    }

    private suspend fun cargarPerfil(uid: String): UsuarioDocumento {
        val documento = try {
            usuarioService.mostrarUsuario(uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            val mensaje = when (e.code()) {
                404 -> "No se encontró el perfil de este usuario en VapeOn."
                401, 403 -> "No tienes permiso para acceder a este perfil."
                else -> "No se pudo cargar tu perfil. Revisa tu conexión."
            }
            throw ErrorAutenticacion(TipoErrorAutenticacion.USUARIO_NO_ENCONTRADO, mensaje, e)
        }
        if (documento.name.substringAfterLast('/') != uid) {
            throw ErrorAutenticacion(
                TipoErrorAutenticacion.USUARIO_NO_ENCONTRADO,
                "El perfil no corresponde al usuario autenticado."
            )
        }
        return documento
    }

    private fun aplicarPerfil(documento: UsuarioDocumento) {
        val campos = documento.fields
        usuarioActual = Usuario(
            id = documento.name.substringAfterLast('/'),
            nombre = campos.nombre?.stringValue.orEmpty(),
            apellido = campos.apellido?.stringValue.orEmpty(),
            telefono = campos.telefono?.stringValue.orEmpty(),
            fechaNacimiento = campos.fechaNacimiento?.stringValue.orEmpty(),
            correo = campos.correo?.stringValue.orEmpty(),
            password = "", // Nunca se conserva la contraseña en memoria.
            rol = campos.rol?.stringValue?.takeIf { it.isNotBlank() } ?: "CLIENTE",
            dni = campos.dni?.stringValue,
            createTime = documento.createTime,
            updateTime = documento.updateTime
        )
        esAdmin = usuarioActual?.rol?.equals("ADMIN", ignoreCase = true) == true
        errorLogin = null
    }

    private fun limpiarSesionLocal() {
        usuarioActual = null
        esAdmin = false
        errorLogin = null
    }

    /**
     * Cierra la sesión del usuario actual.
     * Limpia el estado observable y la sesión persistida de Firebase.
     */
    fun cerrarSesion() {
        operacionRegistro?.cancel()
        operacionRegistro = null
        operacionRestauracion?.cancel()
        operacionRestauracion = null
        registroPendiente = null
        registroCompletado = false
        cargandoRegistro = false
        restaurandoSesion = false
        operacionLogin?.cancel()
        operacionLogin = null
        usuarioActual = null
        esAdmin = false
        cargandoLogin = false
        errorLogin = null
        errorRegistro = null
        runCatching { autenticacion.cerrarSesion() }
    }

    /** Limpia el mensaje de error una vez que la UI lo ha mostrado */
    fun limpiarError() {
        errorLogin = null
    }

    /**
     * Envía un correo real de restablecimiento con Firebase Authentication.
     *
     * Por seguridad no se revela si el correo existe: la respuesta es la misma
     * para cuentas registradas y no registradas.
     */
    fun enviarCorreoRestablecimiento(
        correo: String,
        onExito: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val correoLimpio = correo.trim()
        if (correoLimpio.isEmpty()) {
            onError("Ingresa tu correo electrónico.")
            return
        }
        if (!correoRegistroValido(correoLimpio)) {
            onError("Ingresa un correo electrónico válido.")
            return
        }
        viewModelScope.launch(dispatcher) {
            try {
                autenticacion.enviarCorreoRestablecimiento(correoLimpio)
                onExito()
            } catch (e: CancellationException) {
                throw e
            } catch (e: ErrorAutenticacion) {
                if (e.tipo == TipoErrorAutenticacion.USUARIO_NO_ENCONTRADO) {
                    // No se filtra la existencia de cuentas.
                    onExito()
                } else {
                    onError(e.message)
                }
            } catch (_: Exception) {
                onError("No se pudo enviar el correo. Revisa tu conexión e inténtalo de nuevo.")
            }
        }
    }

    /**
     * Actualiza únicamente los campos editables del perfil.
     *
     * El PATCH usa una máscara explícita para no escribir `password` ni `rol`.
     * Si cambia el correo, primero se sincroniza con Firebase Authentication.
     */
    fun actualizarPerfil(
        nombre: String,
        apellido: String,
        telefono: String,
        fechaNacimiento: String,
        correo: String,
        campoEditado: String? = null,
        onExito: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val actual = usuarioActual
        if (actual == null) {
            onError("No hay una sesión activa.")
            return
        }
        val error = if (campoEditado != null) {
            val valorCampo = when (campoEditado.lowercase(Locale.ROOT)) {
                "nombre", "nombres" -> nombre
                "apellido", "apellidos" -> apellido
                "correo", "email" -> correo
                "telefono", "celular" -> telefono
                "fechanacimiento", "fecha_nacimiento" -> fechaNacimiento
                else -> ""
            }
            validarCampoPerfil(campoEditado, valorCampo)
        } else {
            validarPerfil(nombre, apellido, correo, telefono, fechaNacimiento)
        }
        if (error != null) {
            onError(error)
            return
        }
        viewModelScope.launch(dispatcher) {
            try {
                val correoLimpio = correo.trim()
                val correoAutenticado = autenticacion.sesionActual()?.correo
                if (correoAutenticado != null &&
                    !correoLimpio.equals(correoAutenticado, ignoreCase = true)
                ) {
                    try {
                        autenticacion.actualizarCorreo(correoLimpio)
                        enviarVerificacionSegura()
                    } catch (e: ErrorAutenticacion) {
                        coroutineContext.ensureActive()
                        onError(e.message)
                        return@launch
                    }
                }
                val campos = UsuarioContainer(
                    UsuarioCampos(
                        nombre = FirestoreStringValue(nombre.trim()),
                        apellido = FirestoreStringValue(apellido.trim()),
                        telefono = FirestoreStringValue(telefono),
                        fechaNacimiento = FirestoreStringValue(fechaNacimiento.trim()),
                        correo = FirestoreStringValue(correoLimpio)
                    )
                )
                val actualizado = perfilService.actualizarPerfil(
                    id = actual.id,
                    campos = CAMPOS_PERFIL_EDITABLES,
                    updateTime = actual.updateTime,
                    body = campos
                )
                coroutineContext.ensureActive()
                usuarioActual = actual.copy(
                    nombre = nombre.trim(),
                    apellido = apellido.trim(),
                    telefono = telefono,
                    fechaNacimiento = fechaNacimiento.trim(),
                    correo = correoLimpio,
                    updateTime = actualizado.updateTime ?: actual.updateTime
                )
                esAdmin = usuarioActual?.rol?.equals("ADMIN", ignoreCase = true) == true
                onExito()
            } catch (e: CancellationException) {
                throw e
            } catch (e: ErrorAutenticacion) {
                coroutineContext.ensureActive()
                onError(e.message)
            } catch (_: Exception) {
                onError("No se pudo actualizar el perfil. Inténtalo de nuevo.")
            }
        }
    }
    /**
     * Aplica el perfil ya validado por el flujo de Google Sign-In.
     * El rol se lee del documento protegido por las reglas de Firestore.
     */
    fun establecerSesionGoogle(
        documento: UsuarioDocumento
    ) {
        aplicarPerfil(documento)
    }
}

private val CAMPOS_PERFIL_EDITABLES =
    listOf("nombre", "apellido", "telefono", "fechaNacimiento", "correo")

internal fun telefonoRegistroValido(telefono: String): Boolean = Regex("9[0-9]{8}").matches(telefono)

internal fun correoRegistroValido(correo: String): Boolean =
    Regex("[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+").matches(correo.trim()) &&
        !correo.trim().substringBefore('@').let { it.startsWith('.') || it.endsWith('.') || ".." in it }

/** Regla única de RNO01 para registro y acceso al flujo de compra. */
internal fun validarMayoriaEdad(
    fechaNacimiento: String,
    hoy: LocalDate = LocalDate.now()
): String? {
    val fecha = runCatching {
        val texto = fechaNacimiento.trim()
        require(Regex("[0-9]{2}/[0-9]{2}/[0-9]{4}").matches(texto))
        LocalDate.parse(texto, DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT))
    }.getOrNull()
    if (fecha == null || fecha.year <= 0 || fecha.isAfter(hoy))
        return "Ingresa una fecha de nacimiento válida en formato dd/mm/aaaa."
    if (Period.between(fecha, hoy).years < 18)
        return "Debes tener 18 años cumplidos o más para registrarte."
    return null
}

internal fun validarRegistro(
    nombre: String, apellidos: String, correo: String, telefono: String,
    dni: String, fechaNacimiento: String, password: String, passwordConfirm: String,
    hoy: LocalDate = LocalDate.now()
): String? {
    if (nombre.isBlank() || apellidos.isBlank() || correo.isBlank() || password.isBlank())
        return "Por favor completa todos los campos requeridos."
    if (!telefonoRegistroValido(telefono))
        return "El teléfono debe tener exactamente 9 dígitos y comenzar con 9, sin espacios ni otros caracteres."
    if (dni.isBlank() || !dniClienteValido(dni))
        return "El DNI debe tener exactamente 8 dígitos, sin espacios ni otros caracteres."
    if (!correoRegistroValido(correo)) return "Ingresa un correo electrónico válido."
    validarMayoriaEdad(fechaNacimiento, hoy)?.let { return it }
    if (password != passwordConfirm) return "Las contraseñas no coinciden."
    return null
}

internal fun validarCampoPerfil(
    campo: String,
    valor: String,
    hoy: LocalDate = LocalDate.now()
): String? {
    val limpio = valor.trim()
    return when (campo.lowercase(Locale.ROOT)) {
        "nombre", "nombres" -> if (limpio.isBlank()) "Ingresa tus nombres." else null
        "apellido", "apellidos" -> if (limpio.isBlank()) "Ingresa tus apellidos." else null
        "correo", "email" -> {
            if (limpio.isBlank()) "Ingresa tu correo electrónico."
            else if (!correoRegistroValido(limpio)) "Ingresa un correo electrónico válido."
            else null
        }
        "telefono", "celular" -> {
            if (limpio.isBlank()) "Ingresa tu número de teléfono."
            else if (!telefonoRegistroValido(limpio)) "El teléfono debe tener exactamente 9 dígitos y comenzar con 9."
            else null
        }
        "fechanacimiento", "fecha_nacimiento" -> {
            validarMayoriaEdad(limpio, hoy)
        }
        else -> null
    }
}

/**
 * Valida los datos editables del perfil. Mantiene las mismas reglas que el
 * registro para no degradar la integridad de los datos del usuario.
 */
internal fun validarPerfil(
    nombre: String,
    apellido: String,
    correo: String,
    telefono: String,
    fechaNacimiento: String,
    hoy: LocalDate = LocalDate.now()
): String? {
    if (nombre.isBlank() || apellido.isBlank() || correo.isBlank() || telefono.isBlank())
        return "Por favor completa todos los campos del perfil."
    if (!telefonoRegistroValido(telefono))
        return "El teléfono debe tener exactamente 9 dígitos y comenzar con 9."
    if (!correoRegistroValido(correo)) return "Ingresa un correo electrónico válido."
    val edad = edadDesde(fechaNacimiento, hoy)
        ?: return "Ingresa una fecha de nacimiento válida en formato dd/mm/aaaa."
    if (edad < 18) return "El perfil requiere ser mayor de 18 años."
    return null
}



