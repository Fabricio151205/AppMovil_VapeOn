package com.example.vapeon_movil.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.vapeon_movil.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class SesionGoogle internal constructor(
    val uid: String,
    val nombre: String,
    val correo: String,
    val tokenFirebase: String
)

class GoogleAuthManager(
    private val activity: Activity
) {
    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(activity)

    suspend fun iniciarSesionConGoogle(): SesionGoogle {
        check(accesoGoogle.tryLock()) { "Ya hay un acceso con Google en curso." }

        try {
            val opcion = GetSignInWithGoogleOption.Builder(
                activity.getString(R.string.google_web_client_id)
            ).build()
            val solicitud = GetCredentialRequest.Builder()
                .addCredentialOption(opcion)
                .build()
            val credencial = credentialManager.getCredential(activity, solicitud).credential
            require(
                credencial is CustomCredential &&
                    credencial.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) { "La credencial de Google no es válida." }

            val tokenGoogle = GoogleIdTokenCredential.createFrom(credencial.data).idToken
            val credencialFirebase = GoogleAuthProvider.getCredential(tokenGoogle, null)
            var sesionIniciada = false
            try {
                // La tarea de Firebase puede terminar aunque se cancele la corrutina.
                val usuario = withContext(NonCancellable) {
                    val resultado = auth.signInWithCredential(credencialFirebase).await()
                    sesionIniciada = true
                    requireNotNull(resultado.user) { "Firebase no devolvió un usuario." }
                }
                coroutineContext.ensureActive()
                val correo = usuario.email
                require(usuario.isEmailVerified && !correo.isNullOrBlank()) {
                    "Google no proporcionó un correo verificado."
                }
                val token = usuario.getIdToken(false).await().token
                require(!token.isNullOrBlank()) { "No se pudo obtener el token de Firebase." }

                coroutineContext.ensureActive()
                return SesionGoogle(usuario.uid, usuario.displayName.orEmpty(), correo, token)
            } catch (error: Exception) {
                if (sesionIniciada) auth.signOut()
                throw error
            }
        } finally {
            accesoGoogle.unlock()
        }
    }

    fun cerrarSesionProvisional(sesion: SesionGoogle) {
        if (auth.currentUser?.uid == sesion.uid) auth.signOut()
    }

    private companion object {
        val accesoGoogle = Mutex()
    }
}
