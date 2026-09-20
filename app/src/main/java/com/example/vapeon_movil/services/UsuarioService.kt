package com.example.vapeon_movil.services

import com.example.vapeon_movil.entities.Usuario
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PATCH // Importación para el método de actualización de Firebase REST
import retrofit2.http.Path

interface UsuarioService {

    // Para listar todos los usuarios desde el REST de Firebase
    @GET("usuarios")
    suspend fun listarUsuarios(): FirestoreResponse

    // Para mostrar un usuario en específico usando su ID del documento
    @GET("usuarios/{id}")
    suspend fun mostrarUsuario(@Path("id") id: String): FirestoreDocument

    // Para guardar un nuevo usuario usando la API de Firebase
    @POST("usuarios")
    suspend fun crearUsuario(@Body firestoreFields: FirestoreFieldsContainer): Any

    // Para actualizar un usuario existente (Usamos PATCH o PUT según la REST API de Firebase)
    // En Firestore se pasa el ID del documento en la ruta y se envían los campos actualizados
    @PATCH("usuarios/{id}")
    suspend fun actualizarUsuario(
        @Path("id") id: String, 
        @Body firestoreFields: FirestoreFieldsContainer
    ): FirestoreDocument

    // Para eliminar un usuario por completo de la base de datos mediante su ID
    @DELETE("usuarios/{id}")
    suspend fun eliminarUsuario(@Path("id") id: String): Any
}

// 📑 CLASES DE APOYO PARA EL FORMATO JSON QUE EXIGE LA REST API DE FIREBASE:
// Firebase Firestore guarda los datos bajo un nodo interno llamado "fields"
data class FirestoreFieldsContainer(
    val fields: UsuarioFields
)

data class UsuarioFields(
    val nombre: FirestoreStringValue,
    val apellido: FirestoreStringValue,
    val telefono: FirestoreStringValue,
    val fechaNacimiento: FirestoreStringValue,
    val correo: FirestoreStringValue,
    val password: FirestoreStringValue,
    val rol: FirestoreStringValue
)

data class FirestoreStringValue(
    val stringValue: String
)

data class FirestoreResponse(
    val documents: List<FirestoreDocument>?
)

data class FirestoreDocument(
    val name: String,
    val fields: UsuarioFields
)
