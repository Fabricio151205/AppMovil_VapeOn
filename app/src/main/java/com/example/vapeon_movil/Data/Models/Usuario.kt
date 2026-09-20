package com.example.vapeon_movil.Data.Models

data class Usuario(
    val id: String = "",              // Añadimos un ID para identificar al usuario en la BD
    val nombre: String = "",
    val apellido: String = "",
    val telefono: String = "",
    val fechaNacimiento: String = "",
    val correo: String = "",
    val password: String = "",
    val rol: String = "CLIENTE"       // Rol CLIENTE por defecto
)