package com.example.vapeon_movil.entities

data class Producto(
    val id: String = "",
    val nombre: String,
    val marca: String,
    val precio: Double = 0.0,
    val stock: Int = 0,
    val descripcion: String
)