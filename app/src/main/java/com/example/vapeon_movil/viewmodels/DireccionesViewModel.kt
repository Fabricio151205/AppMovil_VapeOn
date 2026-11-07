package com.example.vapeon_movil.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vapeon_movil.entities.Direccion
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.entities.claveDireccion
import com.example.vapeon_movil.entities.errorNuevaDireccion
import com.example.vapeon_movil.services.DireccionService
import com.example.vapeon_movil.services.dto.aDireccionDelUsuario
import com.example.vapeon_movil.services.dto.aFirestore
import com.example.vapeon_movil.services.dto.consultaDireccionesDelUsuario
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
 * CRUD de las direcciones de entrega del cliente (colección `direcciones`).
 *
 * Cada operación vuelve a comprobar el propietario del documento para que una
 * lista manipulada o un ID ajeno no permita leer, editar ni eliminar datos de
 * otro cliente. El estado se reinicia cuando cambia la sesión.
 */
class DireccionesViewModel(
    private val service: DireccionService = RetrofitClient.retrofit.create(DireccionService::class.java),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
) : ViewModel() {
    var usuarioId by mutableStateOf<String?>(null)
        private set
    var direcciones by mutableStateOf<List<Direccion>>(emptyList())
        private set
    var cargando by mutableStateOf(false)
        private set
    var guardando by mutableStateOf(false)
        private set
    var eliminandoId by mutableStateOf<String?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var mensaje by mutableStateOf<String?>(null)
        private set
    var operacionExitosa by mutableStateOf(false)
        private set

    private var generacion = 0
    private var carga: Job? = null
    private var guardado: Job? = null
    private var eliminacion: Job? = null
    /** Reutiliza el ID tras un fallo de red para que reintentar no duplique la dirección. */
    private var pendiente: Direccion? = null

    fun actualizarUsuario(usuario: Usuario?) {
        val id = usuario?.takeIf { it.rol.equals("CLIENTE", true) && it.id.isNotBlank() }?.id
        if (id == usuarioId) return
        generacion++
        carga?.cancel(); guardado?.cancel(); eliminacion?.cancel()
        usuarioId = id
        direcciones = emptyList()
        pendiente = null
        cargando = false; guardando = false; eliminandoId = null
        error = null; mensaje = null; operacionExitosa = false
    }

    private fun cliente(usuario: Usuario?): String? {
        actualizarUsuario(usuario)
        return usuarioId.also {
            if (it == null) error = "Inicia sesión como cliente para gestionar tus direcciones."
        }
    }

    fun cargarDirecciones(usuario: Usuario?) {
        val id = cliente(usuario) ?: return
        carga?.cancel()
        val sesion = generacion
        cargando = true
        error = null
        carga = viewModelScope.launch(dispatcher) {
            try {
                val documentos = service.consultarDirecciones(consultaDireccionesDelUsuario(id))
                    .mapNotNull { it.document }
                var invalidas = 0
                val propias = documentos
                    .filter { it.fields["usuarioId"]?.stringValue == id }
                    .mapNotNull {
                        try { it.aDireccionDelUsuario(id) }
                        catch (_: Exception) { invalidas++; null }
                    }
                    .distinctBy { it.id }
                    .sortedBy { claveDireccion(it.alias) }
                coroutineContext.ensureActive()
                if (generacion == sesion) {
                    direcciones = propias
                    if (invalidas > 0) error = "No se pudieron interpretar $invalidas direcciones."
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (generacion == sesion) error = "No se pudieron cargar tus direcciones. Reintenta."
            } finally {
                if (generacion == sesion && coroutineContext[Job]?.isCancelled != true) cargando = false
            }
        }
    }

    fun agregarDireccion(
        usuario: Usuario?,
        alias: String,
        direccion: String,
        referencia: String,
        latitud: Double? = null,
        longitud: Double? = null
    ) {
        val id = cliente(usuario) ?: return
        if (guardando) return
        error = errorNuevaDireccion(
            alias, direccion, referencia, direcciones,
            latitud = latitud, longitud = longitud
        )
        if (error != null) return

        val aliasLimpio = alias.trim()
        val direccionLimpia = direccion.trim()
        val referenciaLimpia = referencia.trim()
        val anterior = pendiente
        val nueva = if (anterior != null && anterior.usuarioId == id &&
            anterior.alias == aliasLimpio && anterior.direccion == direccionLimpia &&
            anterior.referencia == referenciaLimpia &&
            anterior.latitud == latitud && anterior.longitud == longitud
        ) {
            anterior
        } else {
            Direccion(
                id = UUID.randomUUID().toString(),
                usuarioId = id,
                alias = aliasLimpio,
                direccion = direccionLimpia,
                referencia = referenciaLimpia,
                fechaCreacion = Instant.now().toString(),
                latitud = latitud,
                longitud = longitud
            )
        }
        pendiente = nueva
        val sesion = generacion
        guardando = true
        mensaje = null
        guardado = viewModelScope.launch(dispatcher) {
            try {
                val documento = try {
                    service.crearDireccion(nueva.aFirestore(), nueva.id)
                } catch (e: HttpException) {
                    if (e.code() != 409) throw e
                    service.mostrarDireccion(nueva.id)
                }
                require(documento.aDireccionDelUsuario(id) == nueva) {
                    "El registro no coincide con la dirección enviada."
                }
                coroutineContext.ensureActive()
                if (generacion == sesion) {
                    pendiente = null
                    direcciones = (direcciones + nueva)
                        .distinctBy { it.id }
                        .sortedBy { claveDireccion(it.alias) }
                    operacionExitosa = true
                    mensaje = "Dirección guardada."
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (generacion == sesion) {
                    if (e is IllegalArgumentException) {
                        pendiente = null
                        error = e.message ?: "No se pudo validar la dirección guardada."
                    } else {
                        if (e is HttpException && e.code() in listOf(400, 401, 403)) pendiente = null
                        error = "No se pudo guardar la dirección. Reintenta."
                    }
                }
            } finally {
                if (generacion == sesion) guardando = false
            }
        }
    }

    fun editarDireccion(
        usuario: Usuario?,
        original: Direccion,
        alias: String,
        direccion: String,
        referencia: String,
        latitud: Double? = null,
        longitud: Double? = null
    ) {
        val id = cliente(usuario) ?: return
        if (guardando) return
        if (original.id.isBlank() || '/' in original.id) {
            error = "La dirección no tiene un identificador válido."
            return
        }
        errorNuevaDireccion(
            alias, direccion, referencia, direcciones,
            excluirId = original.id, latitud = latitud, longitud = longitud
        )?.let {
            error = it
            return
        }
        val actualizada = original.copy(
            alias = alias.trim(),
            direccion = direccion.trim(),
            referencia = referencia.trim(),
            latitud = latitud,
            longitud = longitud
        )
        if (actualizada == original) {
            operacionExitosa = true
            return
        }
        val sesion = generacion
        guardando = true
        mensaje = null
        guardado = viewModelScope.launch(dispatcher) {
            try {
                val remota = service.mostrarDireccion(original.id).aDireccionDelUsuario(id)
                require(remota.id == original.id) { "ID de dirección inválido." }
                val documento = service.actualizarDireccion(original.id, actualizada.aFirestore())
                require(documento.aDireccionDelUsuario(id) == actualizada) {
                    "La actualización no coincide con la dirección enviada."
                }
                coroutineContext.ensureActive()
                if (generacion == sesion) {
                    direcciones = direcciones
                        .map { if (it.id == original.id) actualizada else it }
                        .sortedBy { claveDireccion(it.alias) }
                    if (pendiente?.id == original.id) pendiente = actualizada
                    operacionExitosa = true
                    mensaje = "Dirección actualizada."
                }
            } catch (e: CancellationException) { throw e }
            catch (e: IllegalArgumentException) {
                if (generacion == sesion) error = e.message ?: "No tienes acceso a esta dirección."
            } catch (_: Exception) {
                if (generacion == sesion) error = "No se pudo actualizar la dirección. Reintenta."
            } finally {
                if (generacion == sesion) guardando = false
            }
        }
    }

    fun eliminarDireccion(usuario: Usuario?, direccion: Direccion) {
        val id = cliente(usuario) ?: return
        if (eliminandoId != null) return
        if (direccion.id.isBlank() || '/' in direccion.id) {
            error = "La dirección no tiene un identificador válido."
            return
        }
        val sesion = generacion
        eliminandoId = direccion.id
        error = null
        mensaje = null
        eliminacion = viewModelScope.launch(dispatcher) {
            try {
                val remota = service.mostrarDireccion(direccion.id).aDireccionDelUsuario(id)
                require(remota.id == direccion.id) { "ID de dirección inválido." }
                service.eliminarDireccion(direccion.id)
                coroutineContext.ensureActive()
                if (generacion == sesion) {
                    direcciones = direcciones.filterNot { it.id == direccion.id }
                    if (pendiente?.id == direccion.id) pendiente = null
                    mensaje = "Dirección eliminada."
                }
            } catch (e: CancellationException) { throw e }
            catch (e: IllegalArgumentException) {
                if (generacion == sesion) error = e.message ?: "No tienes acceso a esta dirección."
            } catch (_: Exception) {
                if (generacion == sesion) error = "No se pudo eliminar la dirección. Reintenta."
            } finally {
                if (generacion == sesion) eliminandoId = null
            }
        }
    }

    fun consumirExito() { operacionExitosa = false }
    fun limpiarError() { error = null }
    fun consumirMensaje() { mensaje = null }
}
