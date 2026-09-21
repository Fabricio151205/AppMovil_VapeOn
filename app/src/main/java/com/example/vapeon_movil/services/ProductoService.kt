package com.example.vapeon_movil.services

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.Path


interface ProductoService {


    // Obtener todos los productos
    @GET("productos")
    suspend fun listarProductos(): FirestoreProductosResponse



    // Crear producto nuevo
    @POST("productos")
    suspend fun crearProducto(
        @Body producto: FirestoreProductoContainer
    ): Any



    // Actualizar producto existente
    @PATCH("productos/{id}")
    suspend fun actualizarProducto(
        @Path("id") id: String,
        @Body producto: FirestoreProductoContainer
    ): FirestoreProducto



    // Eliminar producto
    @DELETE("productos/{id}")
    suspend fun eliminarProducto(
        @Path("id") id: String
    ): Any

}



// Formato que necesita Firebase REST
data class FirestoreProductoContainer(
    val fields: ProductoFields
)



data class ProductoFields(

    val nombre: FirestoreStringValue? = null,

    val marca: FirestoreStringValue? = null,

    val precio: FirestoreStringValue? = null,

    val stock: FirestoreStringValue? = null,

    val descripcion: FirestoreStringValue? = null

)



data class FirestoreProductosResponse(

    val documents: List<FirestoreProducto>?

)



data class FirestoreProducto(

    val name: String,

    val fields: ProductoFields

)