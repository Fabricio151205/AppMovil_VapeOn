package com.example.vapeon_movil.auth

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Tipos de error de dominio que la UI puede traducir a mensajes claros sin
 * depender de las clases concretas de Firebase.
 */
enum class TipoErrorAutenticacion {
    CREDENCIALES_INVALIDAS,
    USUARIO_NO_ENCONTRADO,
    CORREO_YA_REGISTRADO,
    CORREO_INVALIDO,
    CONTRASENA_DEBIL,
    USUARIO_DESHABILITADO,
    OPERACION_NO_PERMITIDA,
    DEMASIADOS_INTENTOS,
    REAUTENTICACION_REQUERIDA,
    SIN_CONEXION,
    CREDENCIAL_YA_EN_USO,
    CONTRASENA_YA_CONFIGURADA,
    DESCONOCIDO
}

class ErrorAutenticacion(
    val tipo: TipoErrorAutenticacion,
    override val message: String,
    val causa: Throwable? = null
) : Exception(message, causa)

/** Identidad mínima que entrega Firebase Authentication. */
data class SesionFirebase(
    val uid: String,
    val correo: String,
    val correoVerificado: Boolean
)

/**
 * Abstracción de Firebase Authentication.
 *
 * Permite que los ViewModels se prueben sin inicializar Firebase y concentra
 * aquí la traducción de errores del SDK.
 */
interface AutenticacionFirebase {

    suspend fun iniciarSesion(correo: String, contrasena: String): SesionFirebase

    suspend fun registrar(correo: String, contrasena: String): SesionFirebase

    suspend fun enviarVerificacionCorreo()

    suspend fun enviarCorreoRestablecimiento(correo: String)

    suspend fun actualizarCorreo(nuevoCorreo: String)

    /**
     * true si la cuenta ya puede iniciar sesión con correo y contraseña.
     * Las cuentas creadas solo con Google devuelven false.
     */
    fun tieneProveedorContrasena(): Boolean

    /**
     * Vincula una contraseña al usuario autenticado sin crear otra cuenta ni
     * cambiar su UID. La contraseña vive solo en Firebase Authentication.
     */
    suspend fun vincularContrasena(contrasena: String)

    suspend fun tokenActual(forzarRenovacion: Boolean = true): String?

    fun sesionActual(): SesionFirebase?

    fun cerrarSesion()
}

/**
 * Implementación real sobre Firebase Authentication.
 *
 * El proveedor de [FirebaseAuth] es perezoso: construir el ViewModel en una
 * prueba unitaria no inicializa Firebase hasta que se usa la autenticación.
 */
class FirebaseAutenticacion(
    private val proveedorAuth: () -> FirebaseAuth = { FirebaseAuth.getInstance() }
) : AutenticacionFirebase {

    private val auth: FirebaseAuth by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { proveedorAuth() }

    override suspend fun iniciarSesion(correo: String, contrasena: String): SesionFirebase =
        ejecutar {
            val resultado = auth.signInWithEmailAndPassword(correo.trim(), contrasena).await()
            resultado.user?.aSesion()
                ?: throw ErrorAutenticacion(
                    TipoErrorAutenticacion.DESCONOCIDO,
                    "Firebase no devolvió un usuario autenticado."
                )
        }

    override suspend fun registrar(correo: String, contrasena: String): SesionFirebase =
        ejecutar {
            val resultado = auth.createUserWithEmailAndPassword(correo.trim(), contrasena).await()
            resultado.user?.aSesion()
                ?: throw ErrorAutenticacion(
                    TipoErrorAutenticacion.DESCONOCIDO,
                    "Firebase no devolvió el usuario recién creado."
                )
        }

    override suspend fun enviarVerificacionCorreo() {
        val usuario = auth.currentUser
            ?: throw ErrorAutenticacion(
                TipoErrorAutenticacion.USUARIO_NO_ENCONTRADO,
                "No hay una sesión de Firebase activa."
            )
        ejecutar { usuario.sendEmailVerification().await() }
    }

    override suspend fun enviarCorreoRestablecimiento(correo: String) {
        ejecutar { auth.sendPasswordResetEmail(correo.trim()).await() }
    }

    override suspend fun actualizarCorreo(nuevoCorreo: String) {
        val usuario = auth.currentUser
            ?: throw ErrorAutenticacion(
                TipoErrorAutenticacion.USUARIO_NO_ENCONTRADO,
                "No hay una sesión de Firebase activa."
            )
        ejecutar { usuario.updateEmail(nuevoCorreo.trim()).await() }
    }

    override fun tieneProveedorContrasena(): Boolean =
        auth.currentUser?.providerData
            ?.any { it.providerId == EmailAuthProvider.PROVIDER_ID } == true

    override suspend fun vincularContrasena(contrasena: String) {
        val usuario = auth.currentUser
            ?: throw ErrorAutenticacion(
                TipoErrorAutenticacion.USUARIO_NO_ENCONTRADO,
                "No hay una sesión de Firebase activa."
            )
        val correo = usuario.email?.takeIf { it.isNotBlank() }
            ?: throw ErrorAutenticacion(
                TipoErrorAutenticacion.CORREO_INVALIDO,
                "La cuenta de Firebase no tiene un correo válido."
            )
        ejecutar {
            val credencial = EmailAuthProvider.getCredential(correo, contrasena)
            usuario.linkWithCredential(credencial).await()
        }
    }

    override suspend fun tokenActual(forzarRenovacion: Boolean): String? {
        val usuario = auth.currentUser ?: return null
        return ejecutar { usuario.getIdToken(forzarRenovacion).await().token }
    }

    override fun sesionActual(): SesionFirebase? = auth.currentUser?.aSesion()

    override fun cerrarSesion() {
        auth.signOut()
    }

    private fun FirebaseUser.aSesion(): SesionFirebase {
        val correo = email.orEmpty()
        if (uid.isBlank() || correo.isBlank()) {
            throw ErrorAutenticacion(
                TipoErrorAutenticacion.DESCONOCIDO,
                "La cuenta de Firebase no tiene UID o correo válidos."
            )
        }
        return SesionFirebase(uid = uid, correo = correo, correoVerificado = isEmailVerified)
    }

    private inline fun <T> ejecutar(bloque: () -> T): T = try {
        bloque()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        throw traducir(error)
    }

    private fun traducir(error: Exception): Exception {
        if (error is ErrorAutenticacion) return error
        if (error is CancellationException) return error

        val codigo = (error as? FirebaseAuthException)?.errorCode
        val tipo = when (codigo) {
            "ERROR_INVALID_CREDENTIAL",
            "ERROR_WRONG_PASSWORD",
            "ERROR_INVALID_LOGIN_CREDENTIALS",
            "ERROR_USER_MISMATCH" -> TipoErrorAutenticacion.CREDENCIALES_INVALIDAS

            "ERROR_USER_NOT_FOUND" -> TipoErrorAutenticacion.USUARIO_NO_ENCONTRADO
            "ERROR_EMAIL_ALREADY_IN_USE" -> TipoErrorAutenticacion.CORREO_YA_REGISTRADO
            "ERROR_INVALID_EMAIL" -> TipoErrorAutenticacion.CORREO_INVALIDO
            "ERROR_WEAK_PASSWORD" -> TipoErrorAutenticacion.CONTRASENA_DEBIL
            "ERROR_USER_DISABLED" -> TipoErrorAutenticacion.USUARIO_DESHABILITADO
            "ERROR_OPERATION_NOT_ALLOWED" -> TipoErrorAutenticacion.OPERACION_NO_PERMITIDA
            "ERROR_TOO_MANY_REQUESTS" -> TipoErrorAutenticacion.DEMASIADOS_INTENTOS
            "ERROR_REQUIRES_RECENT_LOGIN" -> TipoErrorAutenticacion.REAUTENTICACION_REQUERIDA
            "ERROR_NETWORK_REQUEST_FAILED", "ERROR_NETWORK_ERROR" -> TipoErrorAutenticacion.SIN_CONEXION
            "ERROR_CREDENTIAL_ALREADY_IN_USE" -> TipoErrorAutenticacion.CREDENCIAL_YA_EN_USO
            "ERROR_PROVIDER_ALREADY_LINKED" -> TipoErrorAutenticacion.CONTRASENA_YA_CONFIGURADA
            else -> TipoErrorAutenticacion.DESCONOCIDO
        }
        val mensaje = when (tipo) {
            TipoErrorAutenticacion.CREDENCIALES_INVALIDAS -> "Correo o contraseña incorrectos."
            TipoErrorAutenticacion.USUARIO_NO_ENCONTRADO -> "No existe una cuenta con ese correo."
            TipoErrorAutenticacion.CORREO_YA_REGISTRADO -> "Ya existe una cuenta con ese correo."
            TipoErrorAutenticacion.CORREO_INVALIDO -> "Ingresa un correo electrónico válido."
            TipoErrorAutenticacion.CONTRASENA_DEBIL -> "La contraseña es demasiado débil."
            TipoErrorAutenticacion.USUARIO_DESHABILITADO -> "Esta cuenta está deshabilitada."
            TipoErrorAutenticacion.OPERACION_NO_PERMITIDA ->
                "El inicio de sesión con correo y contraseña no está habilitado."
            TipoErrorAutenticacion.DEMASIADOS_INTENTOS ->
                "Demasiados intentos. Espera unos minutos antes de reintentar."
            TipoErrorAutenticacion.REAUTENTICACION_REQUERIDA ->
                "Por seguridad, vuelve a iniciar sesión e inténtalo de nuevo."
            TipoErrorAutenticacion.SIN_CONEXION ->
                "No se pudo conectar con el servidor. Revisa tu conexión."
            TipoErrorAutenticacion.CREDENCIAL_YA_EN_USO ->
                "Ese correo ya tiene una cuenta con contraseña. Inicia sesión con ella o usa «¿Olvidaste tu contraseña?»."
            TipoErrorAutenticacion.CONTRASENA_YA_CONFIGURADA ->
                "La contraseña ya está configurada."
            TipoErrorAutenticacion.DESCONOCIDO ->
                "No se pudo completar la operación de autenticación."
        }
        return ErrorAutenticacion(tipo, mensaje, error)
    }
}
