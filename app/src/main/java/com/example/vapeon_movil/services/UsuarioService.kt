package com.example.vapeon_movil.services

import com.example.vapeon_movil.services.dto.UsuarioContainer
import com.example.vapeon_movil.services.dto.UsuarioDocumento
import com.example.vapeon_movil.services.dto.UsuarioListResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

// ─────────────────────────────────────────────
// Contrato HTTP — Colección "usuarios" en Firestore
// DTOs de transporte en: services/dto/UsuarioDto.kt
// ─────────────────────────────────────────────
interface UsuarioService {

    // Listar todos los usuarios
    @GET("usuarios")
    suspend fun listarUsuarios(): UsuarioListResponse

    // Obtener un usuario por su ID de documento
    @GET("usuarios/{id}")
    suspend fun mostrarUsuario(@Path("id") id: String): UsuarioDocumento

    // Crear un usuario nuevo
    @POST("usuarios")
    suspend fun crearUsuario(@Body body: UsuarioContainer): Any

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
