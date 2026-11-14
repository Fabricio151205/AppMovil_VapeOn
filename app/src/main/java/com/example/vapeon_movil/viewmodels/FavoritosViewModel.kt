package com.example.vapeon_movil.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vapeon_movil.entities.Favorito
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.services.FavoritoService
import com.example.vapeon_movil.services.dto.aFavoritoDelUsuario
import com.example.vapeon_movil.services.dto.aFirestore
import com.example.vapeon_movil.services.dto.consultaFavoritosDelUsuario
import com.example.vapeon_movil.utils.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.Instant
import java.util.UUID

/**
 * Favoritos del cliente (colección `favoritos`).
 *
 * Solo se guarda la relación cliente ↔ producto. Los datos visibles se
 * resuelven contra el catálogo vigente, de modo que un cambio de precio o de
 * stock se refleja de inmediato en la pantalla de favoritos.
 *
 * Cada operación comprueba el propietario del documento antes de leerlo o
 * eliminarlo, y el estado se reinicia cuando cambia la sesión.
 */
class FavoritosViewModel(
    private val service: FavoritoService = RetrofitClient.retrofit.create(FavoritoService::class.java),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
) : ViewModel() {
    var usuarioId by mutableStateOf<String?>(null)
        private set
    var favoritos by mutableStateOf<List<Favorito>>(emptyList())
        private set
    var cargando by mutableStateOf(false)
        private set
    /** Producto cuyo alta/baja está en curso; evita dobles pulsaciones. */
    var operandoProductoId by mutableStateOf<String?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var mensaje by mutableStateOf<String?>(null)
        private set

    private var generacion = 0
    private var carga: Job? = null
    private var operacion: Job? = null

    fun actualizarUsuario(usuario: Usuario?) {
        val id = usuario?.takeIf { it.rol.equals("CLIENTE", true) && it.id.isNotBlank() }?.id
        if (id == usuarioId) return
        generacion++
        carga?.cancel(); operacion?.cancel()
        usuarioId = id
        favoritos = emptyList()
        cargando = false
        operandoProductoId = null
        error = null
        mensaje = null
    }

    /** Indica si el producto ya está guardado por el cliente de la sesión. */
    fun esFavorito(productoId: String): Boolean =
        favoritos.any { it.productoId == productoId.trim() }

    private fun cliente(usuario: Usuario?): String? {
        actualizarUsuario(usuario)
        return usuarioId.also {
            if (it == null) error = "Inicia sesión como cliente para gestionar tus favoritos."
        }
    }

    fun cargarFavoritos(usuario: Usuario?) {
        val id = cliente(usuario) ?: return
        carga?.cancel()
        val sesion = generacion
        cargando = true
        error = null
        carga = viewModelScope.launch(dispatcher) {
            try {
                val documentos = service.consultarFavoritos(consultaFavoritosDelUsuario(id))
                    .mapNotNull { it.document }
                var invalidos = 0
                val propios = documentos
                    .filter { it.fields["usuarioId"]?.stringValue == id }
                    .mapNotNull {
                        try { it.aFavoritoDelUsuario(id) }
                        catch (_: Exception) { invalidos++; null }
                    }
                    .distinctBy { it.productoId }
                    .sortedByDescending { it.fechaCreacion }
                coroutineContext.ensureActive()
                if (generacion == sesion) {
                    favoritos = propios
                    if (invalidos > 0) error = "No se pudieron interpretar $invalidos favoritos."
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (generacion == sesion) error = "No se pudieron cargar tus favoritos. Reintenta."
            } finally {
                if (generacion == sesion && coroutineContext[Job]?.isCancelled != true) cargando = false
            }
        }
    }

    /**
     * Agrega el producto si no estaba guardado y lo elimina si ya era favorito.
     * El corazón del detalle y la tarjeta de favoritos usan esta misma acción.
     */
    fun alternarFavorito(usuario: Usuario?, productoId: String) {
        val id = cliente(usuario) ?: return
        if (operandoProductoId != null) return
        val limpio = productoId.trim()
        if (limpio.isBlank() || '/' in limpio) {
            error = "El producto no tiene un identificador válido."
            return
        }
        val existente = favoritos.firstOrNull { it.productoId == limpio }
        if (existente != null) quitarFavorito(id, existente) else agregarFavorito(id, limpio)
    }

    fun eliminarFavorito(usuario: Usuario?, favorito: Favorito) {
        val id = cliente(usuario) ?: return
        if (operandoProductoId != null) return
        if (favorito.id.isBlank() || '/' in favorito.id) {
            error = "El favorito no tiene un identificador válido."
            return
        }
        quitarFavorito(id, favorito)
    }

    private fun agregarFavorito(usuarioId: String, productoId: String) {
        if (favoritos.any { it.productoId == productoId }) return
        val nuevo = Favorito(
            id = UUID.randomUUID().toString(),
            usuarioId = usuarioId,
            productoId = productoId,
            fechaCreacion = Instant.now().toString()
        )
        val sesion = generacion
        operandoProductoId = productoId
        error = null
        mensaje = null
        operacion = viewModelScope.launch(dispatcher) {
            try {
                val documento = try {
                    service.crearFavorito(nuevo.aFirestore(), nuevo.id)
                } catch (e: HttpException) {
                    if (e.code() != 409) throw e
                    service.mostrarFavorito(nuevo.id)
                }
                val guardado = documento.aFavoritoDelUsuario(usuarioId)
                require(guardado.id == nuevo.id && guardado.productoId == nuevo.productoId) {
                    "El registro no coincide con el favorito enviado."
                }
                coroutineContext.ensureActive()
                if (generacion == sesion) {
                    favoritos = (favoritos + guardado)
                        .distinctBy { it.productoId }
                        .sortedByDescending { it.fechaCreacion }
                    mensaje = "Producto agregado a favoritos."
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (generacion == sesion) {
                    error = if (e is IllegalArgumentException) {
                        e.message ?: "No se pudo validar el favorito guardado."
                    } else {
                        "No se pudo guardar el favorito. Reintenta."
                    }
                }
            } finally {
                if (generacion == sesion) operandoProductoId = null
            }
        }
    }

    private fun quitarFavorito(usuarioId: String, favorito: Favorito) {
        val sesion = generacion
        operandoProductoId = favorito.productoId
        error = null
        mensaje = null
        operacion = viewModelScope.launch(dispatcher) {
            try {
                val remoto = service.mostrarFavorito(favorito.id).aFavoritoDelUsuario(usuarioId)
                require(remoto.id == favorito.id) { "ID de favorito inválido." }
                service.eliminarFavorito(favorito.id)
                coroutineContext.ensureActive()
                if (generacion == sesion) {
                    favoritos = favoritos.filterNot { it.id == favorito.id }
                    mensaje = "Producto eliminado de favoritos."
                }
            } catch (e: CancellationException) { throw e }
            catch (e: IllegalArgumentException) {
                if (generacion == sesion) error = e.message ?: "No tienes acceso a este favorito."
            } catch (_: Exception) {
                if (generacion == sesion) error = "No se pudo eliminar el favorito. Reintenta."
            } finally {
                if (generacion == sesion) operandoProductoId = null
            }
        }
    }

    fun limpiarError() { error = null }
    fun consumirMensaje() { mensaje = null }
}
