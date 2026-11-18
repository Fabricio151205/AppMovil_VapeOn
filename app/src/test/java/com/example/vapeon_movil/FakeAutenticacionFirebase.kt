package com.example.vapeon_movil

import com.example.vapeon_movil.auth.AutenticacionFirebase
import com.example.vapeon_movil.auth.ErrorAutenticacion
import com.example.vapeon_movil.auth.SesionFirebase
import com.example.vapeon_movil.auth.TipoErrorAutenticacion
import kotlinx.coroutines.CompletableDeferred

/**
 * Implementación en memoria de Firebase Authentication para pruebas unitarias.
 * Nunca se conecta a Firebase real.
 */
class FakeAutenticacionFirebase : AutenticacionFirebase {

    data class Cuenta(
        val uid: String,
        var correo: String,
        var contrasena: String
    )

    val cuentas = mutableListOf<Cuenta>()
    val correosRestablecimiento = mutableListOf<String>()
    var llamadasRestablecimiento = 0
    var sesion: SesionFirebase? = null
    var verificacionesEnviadas = 0
    var falloRegistro: ErrorAutenticacion? = null
    var falloLogin: ErrorAutenticacion? = null
    var falloRestablecimiento: ErrorAutenticacion? = null
    var esperaRegistro: CompletableDeferred<Unit>? = null
    var esperaRestablecimiento: CompletableDeferred<Unit>? = null
    private var secuencia = 0

    fun agregarCuenta(uid: String, correo: String, contrasena: String): Cuenta =
        Cuenta(uid, correo.trim(), contrasena).also { cuentas += it }

    override suspend fun iniciarSesion(correo: String, contrasena: String): SesionFirebase {
        falloLogin?.let { throw it }
        val cuenta = cuentas.firstOrNull {
            it.correo.equals(correo.trim(), ignoreCase = true) && it.contrasena == contrasena
        } ?: throw ErrorAutenticacion(
            TipoErrorAutenticacion.CREDENCIALES_INVALIDAS,
            "Correo o contraseña incorrectos."
        )
        return SesionFirebase(cuenta.uid, cuenta.correo, true).also { sesion = it }
    }

    override suspend fun registrar(correo: String, contrasena: String): SesionFirebase {
        falloRegistro?.let { throw it }
        esperaRegistro?.await()
        if (cuentas.any { it.correo.equals(correo.trim(), ignoreCase = true) }) {
            throw ErrorAutenticacion(
                TipoErrorAutenticacion.CORREO_YA_REGISTRADO,
                "Ya existe una cuenta con ese correo."
            )
        }
        val cuenta = Cuenta("uid-${++secuencia}", correo.trim(), contrasena)
        cuentas += cuenta
        return SesionFirebase(cuenta.uid, cuenta.correo, false).also { sesion = it }
    }

    override suspend fun enviarVerificacionCorreo() {
        verificacionesEnviadas++
    }

    override suspend fun enviarCorreoRestablecimiento(correo: String) {
        llamadasRestablecimiento++
        falloRestablecimiento?.let { throw it }
        esperaRestablecimiento?.await()
        correosRestablecimiento += correo.trim()
    }

    override suspend fun actualizarCorreo(nuevoCorreo: String) {
        val actual = sesion ?: throw ErrorAutenticacion(
            TipoErrorAutenticacion.USUARIO_NO_ENCONTRADO,
            "No hay una sesión de Firebase activa."
        )
        val correoLimpio = nuevoCorreo.trim()
        if (cuentas.any {
                it.uid != actual.uid && it.correo.equals(correoLimpio, ignoreCase = true)
            }
        ) {
            throw ErrorAutenticacion(
                TipoErrorAutenticacion.CORREO_YA_REGISTRADO,
                "Ya existe una cuenta con ese correo."
            )
        }
        cuentas.firstOrNull { it.uid == actual.uid }?.correo = correoLimpio
        sesion = actual.copy(correo = correoLimpio)
    }

    override suspend fun tokenActual(forzarRenovacion: Boolean): String? =
        sesion?.uid?.let { "token-$it" }

    override fun sesionActual(): SesionFirebase? = sesion

    override fun cerrarSesion() {
        sesion = null
    }
}
