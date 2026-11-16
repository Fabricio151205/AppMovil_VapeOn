package com.example.vapeon_movil.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vapeon_movil.entities.PopularidadProducto
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.services.FavoritoService
import com.example.vapeon_movil.services.dto.agregarPorProducto
import com.example.vapeon_movil.services.dto.consultaTodosLosFavoritos
import com.example.vapeon_movil.utils.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/**
 * Ranking administrativo de productos según la cantidad de favoritos.
 *
 * Solo se conserva el conteo agregado por producto: nunca se expone
 * `usuarioId` ni ningún dato del cliente. Los documentos inválidos se
 * descartan y se informan por separado para que el administrador sepa que el
 * ranking puede estar incompleto.
 */
class FavoritosAdminViewModel(
    private val service: FavoritoService = RetrofitClient.retrofit.create(FavoritoService::class.java),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
) : ViewModel() {
    var adminId by mutableStateOf<String?>(null)
        private set
    var popularidad by mutableStateOf<List<PopularidadProducto>>(emptyList())
        private set
    var documentosInvalidos by mutableStateOf(0)
        private set
    var cargando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var generacion = 0
    private var carga: Job? = null

    fun actualizarUsuario(usuario: Usuario?) {
        val id = usuario?.takeIf { it.rol.equals("ADMIN", true) && it.id.isNotBlank() }?.id
        if (id == adminId) return
        generacion++
        carga?.cancel()
        adminId = id
        popularidad = emptyList()
        documentosInvalidos = 0
        cargando = false
        error = null
    }

    /** Carga todos los favoritos y publica el ranking agregado por producto. */
    fun cargarRanking(usuario: Usuario?) {
        admin(usuario) ?: return
        carga?.cancel()
        val sesion = generacion
        cargando = true
        error = null
        carga = viewModelScope.launch(dispatcher) {
            try {
                val documentos = service.consultarFavoritos(consultaTodosLosFavoritos())
                    .mapNotNull { it.document }
                coroutineContext.ensureActive()
                val ranking = documentos.agregarPorProducto()
                if (generacion == sesion) {
                    popularidad = ranking.popularidad
                    documentosInvalidos = ranking.invalidos
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (generacion == sesion) {
                    error = "No se pudo cargar el ranking de favoritos. Reintenta."
                }
            } finally {
                if (generacion == sesion && coroutineContext[Job]?.isCancelled != true) {
                    cargando = false
                }
            }
        }
    }

    private fun admin(usuario: Usuario?): String? {
        actualizarUsuario(usuario)
        return adminId.also {
            if (it == null) {
                error = "Inicia sesión como administrador para ver los productos más populares."
            }
        }
    }

    fun limpiarError() {
        error = null
    }
}
