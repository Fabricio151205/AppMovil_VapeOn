package com.example.vapeon_movil.services

import com.example.vapeon_movil.services.dto.FirestoreStringValue
import com.example.vapeon_movil.services.dto.UsuarioContainer
import com.example.vapeon_movil.services.dto.UsuarioDocumento
import com.example.vapeon_movil.services.dto.UsuarioListResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// ─────────────────────────────────────────────
// Contrato HTTP — Colección "usuarios" en Firestore
// DTOs de transporte en: services/dto/UsuarioDto.kt
// ─────────────────────────────────────────────
interface UsuarioService {

    // Listar todos los usuarios
    @GET("usuarios")
    suspend fun listarUsuarios(@Query("pageToken") pageToken: String? = null): UsuarioListResponse

    // Obtener un usuario por su ID de documento
    @GET("usuarios/{id}")
    suspend fun mostrarUsuario(@Path("id") id: String): UsuarioDocumento

    // Crear un usuario nuevo
    @POST("usuarios")
    suspend fun crearUsuario(@Body body: UsuarioContainer, @Query("documentId") documentId: String? = null): Any

    // Actualizar un usuario existente por su ID
    @PATCH("usuarios/{id}")
    suspend fun actualizarUsuario(
        @Path("id") id: String,
        @Body body: UsuarioContainer
    ): UsuarioDocumento

    // Eliminar un usuario por su ID
    @DELETE("usuarios/{id}")
    suspend fun eliminarUsuario(@Path("id") id: String): Any
}

interface UsuarioAutenticadoService {

    // Consultar únicamente el perfil del UID autenticado
    @GET("usuarios/{uid}")
    suspend fun obtenerPerfil(
        @Path("uid") uid: String,
        @Header("Authorization") autorizacion: String
    ): UsuarioDocumento

    // Crear un nuevo perfil con el UID de Firebase
    @POST("usuarios")
    suspend fun crearPerfil(
        @Query("documentId") uid: String,
        @Header("Authorization") autorizacion: String,
        @Body body: UsuarioContainer
    ): UsuarioDocumento

    @POST("usuarios")
    suspend fun crearPerfilGoogle(
        @Query("documentId") uid: String,
        @Header("Authorization") autorizacion: String,
        @Body body: PerfilGoogleContainer
    ): UsuarioDocumento
}


/**
 * Contrato de perfil acotado: la máscara de actualización impide escribir
 * `password`, `rol` o metadatos al editar los datos personales.
 */
interface UsuarioPerfilService {
    @PATCH("usuarios/{id}")
    suspend fun actualizarPerfil(
        @Path("id") id: String,
        @Query("updateMask.fieldPaths") campos: List<String>,
        @Query("currentDocument.updateTime") updateTime: String?,
        @Body body: UsuarioContainer
    ): UsuarioDocumento
}

/** Contrato separado para que el cambio administrativo nunca reutilice el PATCH completo. */
interface UsuarioRolesService {
    @PATCH("usuarios/{id}")
    suspend fun actualizarRol(
        @Path("id") id: String,
        @Query("updateMask.fieldPaths") campo: String = "rol",
        @Query("currentDocument.updateTime") updateTime: String,
        @Body body: UsuarioContainer
    ): UsuarioDocumento
}

/** Contrato administrativo acotado: nunca envía contraseña, rol ni metadatos. */
interface UsuarioClientesService {
    @PATCH("usuarios/{id}")
    suspend fun actualizarCliente(
        @Path("id") id: String,
        @Query("updateMask.fieldPaths") campos: List<String>,
        @Query("currentDocument.updateTime") updateTime: String,
        @Body body: UsuarioContainer
    ): UsuarioDocumento
}

interface UsuarioGoogleService {
    @GET("usuarios?mask.fieldPaths=correo")
    suspend fun listarCorreos(
        @Header("Authorization") autorizacion: String,
        @Query("pageToken") pageToken: String?
    ): CorreosUsuarioResponse
}

data class CorreosUsuarioResponse(
    val documents: List<CorreoUsuarioDocumento>?,
    val nextPageToken: String?
)

data class CorreoUsuarioDocumento(val fields: CorreoUsuarioCampo?)
data class CorreoUsuarioCampo(val correo: FirestoreStringValue?)

data class PerfilGoogleContainer(
    val fields: Map<String, FirestoreStringValue>
)

suspend fun UsuarioGoogleService.existeCorreoRegistrado(correo: String, tokenFirebase: String): Boolean {
    val autorizacion = "Bearer $tokenFirebase"
    val tokens = mutableSetOf<String>()
    var paginaSiguiente: String? = null
    do {
        val pagina = listarCorreos(autorizacion, paginaSiguiente)
        if (pagina.documents.orEmpty().any {
                it.fields?.correo?.stringValue?.trim().equals(correo, ignoreCase = true)
            }) return true
        paginaSiguiente = pagina.nextPageToken?.takeIf { it.isNotBlank() }
        check(paginaSiguiente == null || tokens.add(paginaSiguiente)) {
            "Firestore repitió un token de usuarios."
        }
    } while (paginaSiguiente != null)
    return false
}

suspend fun UsuarioService.listarTodosUsuarios(): UsuarioListResponse {
    val documentos = mutableListOf<UsuarioDocumento>()
    val tokens = mutableSetOf<String>()
    var token: String? = null
    do {
        val pagina = listarUsuarios(token)
        documentos += pagina.documents.orEmpty()
        token = pagina.nextPageToken?.takeIf { it.isNotBlank() }
        check(token == null || tokens.add(token)) { "Firestore repitió un token de usuarios." }
    } while (token != null)
    return UsuarioListResponse(documentos.distinctBy { it.name })
}
