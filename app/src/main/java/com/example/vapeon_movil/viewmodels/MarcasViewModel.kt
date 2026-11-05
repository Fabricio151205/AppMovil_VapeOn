package com.example.vapeon_movil.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vapeon_movil.entities.Marca
import com.example.vapeon_movil.services.MarcaService
import com.example.vapeon_movil.services.dto.MarcaContainer
import com.example.vapeon_movil.services.listarTodasMarcas
import com.example.vapeon_movil.utils.RetrofitClient
import com.example.vapeon_movil.utils.claveMarca
import kotlinx.coroutines.launch

class MarcasViewModel(
    private val service: MarcaService = RetrofitClient.marcaService
) : ViewModel() {
    var listaMarcas by mutableStateOf<List<Marca>>(emptyList())
        private set
    var cargando by mutableStateOf(false)
        private set
    var mensajeError by mutableStateOf<String?>(null)
        private set
    var operacionExitosa by mutableStateOf(false)
        private set

    init { cargarMarcas() }

    fun cargarMarcas() {
        viewModelScope.launch {
            cargando = true
            try {
                listaMarcas = service.listarTodasMarcas().documents.orEmpty()
                    .map { it.toEntity() }
                    .filter { it.nombre.isNotBlank() }
                    .distinctBy { claveMarca(it.nombre) }
                    .sortedBy { it.nombre.lowercase() }
                mensajeError = null
            } catch (_: Exception) {
                mensajeError = "No se pudieron cargar las marcas."
            } finally { cargando = false }
        }
    }

    fun agregarMarca(nombre: String) {
        val limpio = nombre.trim()
        errorNombreMarca(limpio, listaMarcas)?.let { mensajeError = it; return }
        viewModelScope.launch {
            cargando = true
            try {
                service.crearMarca(MarcaContainer.fromEntity(Marca(nombre = limpio)))
                cargarMarcas()
                operacionExitosa = true
                mensajeError = null
            } catch (_: Exception) { mensajeError = "Error al guardar la marca." }
            finally { cargando = false }
        }
    }

    fun editarMarca(original: Marca, nuevoNombre: String, tieneRelaciones: Boolean) {
        val limpio = nuevoNombre.trim()
        if (tieneRelaciones && claveMarca(original.nombre) != claveMarca(limpio)) {
            mensajeError = "No se puede renombrar una marca con productos o sabores asociados."
            return
        }
        errorNombreMarca(limpio, listaMarcas, original.id)?.let { mensajeError = it; return }
        if (original.id.isBlank()) { mensajeError = "La marca no tiene un identificador válido."; return }
        viewModelScope.launch {
            cargando = true
            try {
                service.actualizarMarca(original.id, MarcaContainer.fromEntity(original.copy(nombre = limpio)))
                cargarMarcas()
                operacionExitosa = true
                mensajeError = null
            } catch (_: Exception) { mensajeError = "Error al actualizar la marca." }
            finally { cargando = false }
        }
    }

    fun eliminarMarca(marca: Marca, tieneRelaciones: Boolean) {
        if (tieneRelaciones) {
            mensajeError = "No se puede eliminar: existen productos o sabores asociados."
            return
        }
        if (marca.id.isBlank()) { mensajeError = "La marca no tiene un identificador válido."; return }
        viewModelScope.launch {
            cargando = true
            try {
                service.eliminarMarca(marca.id)
                cargarMarcas()
                operacionExitosa = true
                mensajeError = null
            } catch (_: Exception) { mensajeError = "Error al eliminar la marca." }
            finally { cargando = false }
        }
    }

    fun consumirExito() { operacionExitosa = false }
    fun limpiarError() { mensajeError = null }
}

internal fun errorNombreMarca(nombre: String, marcas: List<Marca>, excluirId: String? = null): String? {
    if (nombre.trim().isBlank()) return "El nombre de la marca es obligatorio."
    if (marcas.any { it.id != excluirId && claveMarca(it.nombre) == claveMarca(nombre) })
        return "Ya existe una marca con la misma identidad."
    return null
}
