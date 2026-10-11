package com.example.vapeon_movil.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.services.UsuarioService
import com.example.vapeon_movil.utils.RetrofitClient
import kotlinx.coroutines.launch

/**
 * ViewModel de sesión del Usuario.
 *
 * Gestiona el ciclo completo de autenticación: login, datos del usuario activo
 * y cierre de sesión. Al vivir a nivel de Activity (escoped en VapeONApp),
 * el rol y los datos del usuario persisten mientras la app está activa.
 *
 * Pantallas consumidoras: LoginScreen, VapeONApp (para determinar la ruta inicial).
 */
class UsuarioViewModel : ViewModel() {

    // ─── Servicio de red ───────────────────────────────────────────────────
    private val usuarioService: UsuarioService =
        RetrofitClient.retrofit.create(UsuarioService::class.java)

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
        // ── Validar que no se envíen campos vacíos ──────────────────────────
        val correoLimpio = correo.trim()
        val passwordLimpia = password.trim()

        if (correoLimpio.isEmpty() || passwordLimpia.isEmpty()) {
            errorLogin = "Por favor, ingresa tu correo y contraseña"
            onError(errorLogin!!)
            return
        }

        viewModelScope.launch {
            cargandoLogin = true
            errorLogin = null
            try {
                val respuesta = usuarioService.listarUsuarios()
                val doc = respuesta.documents?.firstOrNull { doc ->
                    val docCorreo = doc.fields.correo?.stringValue?.trim() ?: ""
                    val docPassword = doc.fields.password?.stringValue ?: ""

                    docCorreo.isNotEmpty() && docPassword.isNotEmpty() &&
                    docCorreo.equals(correoLimpio, ignoreCase = true) &&
                    docPassword == passwordLimpia
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
            } catch (e: Exception) {
                println("Error al iniciar sesión: ${e.localizedMessage}")
                errorLogin = "Error al conectar con el servidor. Inténtalo de nuevo."
                onError(errorLogin!!)
            } finally {
                cargandoLogin = false
            }
        }
    }

    /**
     * Cierra la sesión del usuario actual.
     * Limpia todos los datos de sesión del estado observable.
     */
    fun cerrarSesion() {
        usuarioActual = null
        esAdmin = false
        errorLogin = null
    }

    /** Limpia el mensaje de error una vez que la UI lo ha mostrado */
    fun limpiarError() {
        errorLogin = null
    }
}
