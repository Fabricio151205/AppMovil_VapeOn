package com.example.vapeon_movil.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.services.UsuarioService
import com.example.vapeon_movil.services.listarTodosUsuarios
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.utils.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale
import java.util.UUID

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
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
) : ViewModel() {

    // ─── Servicio de red ───────────────────────────────────────────────────
    private var operacionLogin: Job? = null
    private var registroPendiente: Pair<String, UsuarioContainer>? = null
    private var operacionRegistro: Job? = null
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

    fun registrarUsuario(
        nombre: String, apellidos: String, correo: String, telefono: String,
        fechaNacimiento: String, password: String, passwordConfirm: String,
        onExito: (() -> Unit)? = null
    ) {
        if (cargandoRegistro) return
        errorRegistro = validarRegistro(nombre, apellidos, correo, telefono, fechaNacimiento,
            password, passwordConfirm)
        if (errorRegistro != null) return
        // Estructuramos el objeto según lo que pide la API REST de Firestore.
        val cuerpo = UsuarioContainer(UsuarioCampos(
            nombre = FirestoreStringValue(nombre.trim()),
            apellido = FirestoreStringValue(apellidos.trim()),
            telefono = FirestoreStringValue(telefono),
            fechaNacimiento = FirestoreStringValue(fechaNacimiento.trim()),
            correo = FirestoreStringValue(correo.trim()),
            password = FirestoreStringValue(password),
            rol = FirestoreStringValue("CLIENTE")
        ))
        val intento = registroPendiente ?: (UUID.randomUUID().toString() to cuerpo).also { registroPendiente = it }
        if (intento.second != cuerpo) {
            errorRegistro = "El registro anterior requiere comprobación. Reintenta con los mismos datos antes de iniciar otro."
            return
        }
        cargandoRegistro = true
        operacionRegistro = viewModelScope.launch(dispatcher) {
            try {
                // Consulta previa, no una garantía atómica de unicidad entre dispositivos.
                val existentes = usuarioService.listarTodosUsuarios()
                coroutineContext.ensureActive()
                val existente = existentes.documents.orEmpty().firstOrNull {
                    it.fields.correo?.stringValue?.trim().equals(correo.trim(), ignoreCase = true)
                }
                if (existente != null) {
                    if (existente.name.substringAfterLast('/') == intento.first) {
                        registroPendiente = null
                        registroCompletado = true
                        onExito?.invoke()
                    } else {
                        registroPendiente = null
                        errorRegistro = "Ya existe una cuenta con ese correo."
                    }
                    return@launch
                }
                // Consumimos el servicio de la API REST y volvemos al Login.
                usuarioService.crearUsuario(cuerpo, intento.first)
                coroutineContext.ensureActive()
                registroPendiente = null
                registroCompletado = true
                onExito?.invoke()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                coroutineContext.ensureActive()
                if (e is retrofit2.HttpException && e.code() in listOf(400, 401, 403)) registroPendiente = null
                errorRegistro = "No se pudo confirmar el registro. Reintenta con los mismos datos; se comprobará el mismo intento."
            } finally {
                if (coroutineContext[Job]?.isCancelled != true) cargandoRegistro = false
            }
        }
    }

    // ─── Acciones ──────────────────────────────────────────────────────────

    /**
     * Intenta autenticar al usuario buscando en la colección "usuarios" de Firestore.
     *
     * Nota: La autenticación se realiza comparando credenciales en cliente porque
     * el proyecto usa Firestore REST API directamente (sin Firebase Auth).
     * En producción se recomienda migrar a Firebase Authentication.
     *
     * @param correo    Correo electrónico ingresado.
     * @param password  Contraseña ingresada (en texto plano — ver nota anterior).
     * @param onExito   Callback con `esAdmin: Boolean` cuando el login es exitoso.
     * @param onError   Callback con el mensaje de error cuando el login falla.
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
        // ── Validar que no se envíen campos vacíos ──────────────────────────
        val correoLimpio = correo.trim()

        if (correoLimpio.isEmpty() || password.isBlank()) {
            errorLogin = "Por favor, ingresa tu correo y contraseña"
            onError(errorLogin!!)
            return
        }

        cargandoLogin = true
        operacionLogin = viewModelScope.launch(dispatcher) {
            try {
                val respuesta = usuarioService.listarTodosUsuarios()
                coroutineContext.ensureActive()
                val doc = respuesta.documents?.firstOrNull { doc ->
                    val docCorreo = doc.fields.correo?.stringValue?.trim() ?: ""
                    val docPassword = doc.fields.password?.stringValue ?: ""

                    docCorreo.isNotEmpty() && docPassword.isNotEmpty() &&
                    docCorreo.equals(correoLimpio, ignoreCase = true) &&
                    docPassword == password
                }

                if (doc != null) {
                    usuarioActual = Usuario(
                        id              = doc.name.substringAfterLast("/"),
                        nombre          = doc.fields.nombre?.stringValue ?: "",
                        apellido        = doc.fields.apellido?.stringValue ?: "",
                        telefono        = doc.fields.telefono?.stringValue ?: "",
                        fechaNacimiento = doc.fields.fechaNacimiento?.stringValue ?: "",
                        correo          = doc.fields.correo?.stringValue ?: "",
                        password        = "",       // No se persiste la contraseña en memoria
                        rol             = doc.fields.rol?.stringValue ?: "CLIENTE"
                    )
                    esAdmin = usuarioActual?.rol?.equals("ADMIN", ignoreCase = true) == true
                    onExito(esAdmin)
                } else {
                    errorLogin = "Correo o contraseña incorrectos"
                    onError(errorLogin!!)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                errorLogin = "Error al conectar con el servidor. Inténtalo de nuevo."
                onError(errorLogin!!)
            } finally {
                if (coroutineContext[Job]?.isCancelled != true) cargandoLogin = false
            }
        }
    }

    /**
     * Cierra la sesión del usuario actual.
     * Limpia todos los datos de sesión del estado observable.
     */
    fun cerrarSesion() {
        operacionRegistro?.cancel()
        operacionRegistro = null
        registroPendiente = null
        registroCompletado = false
        cargandoRegistro = false
        operacionLogin?.cancel()
        operacionLogin = null
        usuarioActual = null
        esAdmin = false
        cargandoLogin = false
        errorLogin = null
        errorRegistro = null
    }

    /** Limpia el mensaje de error una vez que la UI lo ha mostrado */
    fun limpiarError() {
        errorLogin = null
    }
}

internal fun telefonoRegistroValido(telefono: String): Boolean = Regex("9[0-9]{8}").matches(telefono)

internal fun correoRegistroValido(correo: String): Boolean =
    Regex("[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+").matches(correo.trim()) &&
        !correo.trim().substringBefore('@').let { it.startsWith('.') || it.endsWith('.') || ".." in it }

internal fun validarRegistro(
    nombre: String, apellidos: String, correo: String, telefono: String,
    fechaNacimiento: String, password: String, passwordConfirm: String,
    hoy: LocalDate = LocalDate.now()
): String? {
    if (nombre.isBlank() || apellidos.isBlank() || correo.isBlank() || password.isBlank())
        return "Por favor completa todos los campos requeridos."
    if (!telefonoRegistroValido(telefono))
        return "El teléfono debe tener exactamente 9 dígitos y comenzar con 9, sin espacios ni otros caracteres."
    if (!correoRegistroValido(correo)) return "Ingresa un correo electrónico válido."
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
    if (password != passwordConfirm) return "Las contraseñas no coinciden."
    return null
}
