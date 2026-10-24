package com.example.vapeon_movil.entities

/**
 * Entidad de dominio que representa un sabor específico asociado a una marca y su stock individual.
 */
data class SaborItem(
    val nombre: String,
    val marca: String,
    val stock: Int = 1,
    val id: String = ""
)
