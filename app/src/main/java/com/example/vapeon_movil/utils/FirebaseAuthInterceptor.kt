
package com.example.vapeon_movil.utils

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class FirebaseAuthInterceptor : Interceptor {

    override fun intercept(
        chain: Interceptor.Chain
    ): Response {

        val solicitud = chain.request()
        val usuario = FirebaseAuth.getInstance().currentUser

        // Respetar tokens enviados explícitamente.
        if (solicitud.header("Authorization") != null) {
            return chain.proceed(solicitud)
        }

        // Si todavía no existe una sesión Firebase,
        // la solicitud continúa sin token.
        if (usuario == null) {
            return chain.proceed(solicitud)
        }

        val token = try {
            runBlocking {
                usuario.getIdToken(false).await().token
            }
        } catch (e: Exception) {
            throw IOException(
                "No se pudo obtener el token de Firebase",
                e
            )
        }

        if (token.isNullOrBlank()) {
            throw IOException(
                "Firebase no devolvió un token válido"
            )
        }

        val solicitudAutenticada = solicitud.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(solicitudAutenticada)
    }
}
