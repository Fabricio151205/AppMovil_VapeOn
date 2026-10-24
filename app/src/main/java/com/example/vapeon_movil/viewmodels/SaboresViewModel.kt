package com.example.vapeon_movil.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vapeon_movil.entities.SaborItem
import com.example.vapeon_movil.services.SaborService
import com.example.vapeon_movil.services.dto.SaborContainer
import com.example.vapeon_movil.services.listarTodosSabores
import com.example.vapeon_movil.utils.RetrofitClient
import kotlinx.coroutines.launch

/**
 * ViewModel para la gestión del catálogo de sabores con persistencia exclusiva en Google Cloud Firestore.
 */
class SaboresViewModel(
    private val service: SaborService = RetrofitClient.saborService
) : ViewModel() {

    /** Lista completa de sabores cargada desde Firestore */
    var listaSabores by mutableStateOf<List<SaborItem>>(emptyList())
        private set

    /** Filtro de marca activo */
    var marcaFiltroSeleccionada by mutableStateOf("Todas")
        private set

    /** Término de búsqueda por texto */
    var busquedaQuery by mutableStateOf("")
        private set

    /** Estado de carga asíncrona */
    var cargando by mutableStateOf(false)
        private set

    /** Mensaje de error transitorio para la UI */
    var mensajeError by mutableStateOf<String?>(null)
        private set

    /** Señal de operación exitosa */
    var operacionExitosa by mutableStateOf(false)
        private set

    init {
        cargarSabores()
    }

    fun cargarSabores() {
        viewModelScope.launch {
            cargando = true
            try {
                val res = service.listarTodosSabores()
                listaSabores = res.documents.orEmpty()
                    .map { it.toEntity() }
                    .filter { it.nombre.isNotBlank() }
                mensajeError = null
            } catch (e: Exception) {
                mensajeError = "No se pudieron cargar los sabores desde la base de datos."
            } finally {
                cargando = false
            }
        }
    }

    fun cambiarFiltroMarca(marca: String) {
        marcaFiltroSeleccionada = marca
    }

    fun cambiarBusqueda(query: String) {
        busquedaQuery = query
    }

    /** Agrega un nuevo sabor al catálogo en Firestore */
    fun agregarSabor(nombre: String, marca: String, stock: Int = 0) {
        val nLimpio = nombre.trim()
        val mLimpio = marca.trim()
        if (nLimpio.isBlank() || mLimpio.isBlank()) {
            mensajeError = "Nombre y marca son obligatorios"
            return
        }
        if (stock < 0) {
            mensajeError = "El stock debe ser mayor o igual a 0"
            return
        }
        val duplicado = listaSabores.any {
            it.nombre.equals(nLimpio, ignoreCase = true) &&
                    it.marca.equals(mLimpio, ignoreCase = true)
        }
        if (duplicado) {
            mensajeError = "Ya existe '$nLimpio' para la marca '$mLimpio'"
            return
        }

        viewModelScope.launch {
            cargando = true
            try {
                val nuevoSabor = SaborItem(nombre = nLimpio, marca = mLimpio, stock = stock)
                service.crearSabor(SaborContainer.fromEntity(nuevoSabor))
                cargarSabores()
                mensajeError = null
                operacionExitosa = true
            } catch (e: Exception) {
                mensajeError = "Error al guardar el sabor en la base de datos."
            } finally {
                cargando = false
            }
        }
    }

    /** Edita un sabor existente en Firestore */
    fun editarSabor(saborAntiguo: SaborItem, nuevoNombre: String, nuevaMarca: String, nuevoStock: Int = saborAntiguo.stock) {
        val nLimpio = nuevoNombre.trim()
        val mLimpio = nuevaMarca.trim()
        if (nLimpio.isBlank() || mLimpio.isBlank()) {
            mensajeError = "Nombre y marca son obligatorios"
            return
        }
        if (nuevoStock < 0) {
            mensajeError = "El stock debe ser mayor o igual a 0"
            return
        }

        viewModelScope.launch {
            cargando = true
            try {
                val saborEditado = SaborItem(id = saborAntiguo.id, nombre = nLimpio, marca = mLimpio, stock = nuevoStock)
                if (saborAntiguo.id.isNotBlank()) {
                    service.actualizarSabor(id = saborAntiguo.id, body = SaborContainer.fromEntity(saborEditado))
                }
                cargarSabores()
                mensajeError = null
                operacionExitosa = true
            } catch (e: Exception) {
                mensajeError = "Error al actualizar el sabor en la base de datos."
            } finally {
                cargando = false
            }
        }
    }

    /** Elimina un sabor de la base de datos Firestore */
    fun eliminarSabor(saborItem: SaborItem) {
        viewModelScope.launch {
            cargando = true
            try {
                if (saborItem.id.isNotBlank()) {
                    service.eliminarSabor(saborItem.id)
                }
                cargarSabores()
                operacionExitosa = true
            } catch (e: Exception) {
                mensajeError = "Error al eliminar el sabor de la base de datos."
            } finally {
                cargando = false
            }
        }
    }

    fun consumirExito() { operacionExitosa = false }
    fun limpiarError() { mensajeError = null }
}
