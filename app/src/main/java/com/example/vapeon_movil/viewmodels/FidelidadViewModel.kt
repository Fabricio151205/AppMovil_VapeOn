package com.example.vapeon_movil.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vapeon_movil.entities.*
import com.example.vapeon_movil.services.*
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.utils.RetrofitClient
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

internal enum class TipoErrorFidelidad { CONFLICTO_VERSION, ERROR_RED, ERROR_DESCONOCIDO }

internal fun clasificarErrorFidelidad(error: Throwable): TipoErrorFidelidad {
    if (error is IOException) return TipoErrorFidelidad.ERROR_RED
    if (error !is HttpException) return TipoErrorFidelidad.ERROR_DESCONOCIDO
    if (error.code() == 409 || error.code() == 412) return TipoErrorFidelidad.CONFLICTO_VERSION
    val estadoFirestore = runCatching { error.response()?.errorBody()?.string().orEmpty() }.getOrDefault("")
    return if (error.code() == 400 &&
        (estadoFirestore.contains("FAILED_PRECONDITION") || estadoFirestore.contains("ABORTED"))) {
        TipoErrorFidelidad.CONFLICTO_VERSION
    } else TipoErrorFidelidad.ERROR_DESCONOCIDO
}

class FidelidadViewModel(
    private val fidelidadService: FidelidadService = RetrofitClient.retrofit.create(FidelidadService::class.java),
    private val usuarioService: UsuarioService = RetrofitClient.retrofit.create(UsuarioService::class.java),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    var miFidelidad by mutableStateOf<FidelidadCliente?>(null)
        private set
    var clientes by mutableStateOf<List<ClienteFidelidad>>(emptyList())
        private set
    var configuracion by mutableStateOf<ConfiguracionFidelidad?>(null)
        private set
    var cargando by mutableStateOf(false)
        private set
    var guardando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var mensaje by mutableStateOf<String?>(null)
        private set
    var conflicto by mutableStateOf(false)
        private set

    private var propietario: String? = null

    fun cargarMiFidelidad(usuario: Usuario?) {
        val cliente = usuario?.takeIf {
            it.id.isNotBlank() && it.rol.equals("CLIENTE", true)
        } ?: run {
            limpiar()
            error = "Solo un cliente puede consultar su fidelidad."
            return
        }
        if (cargando) return
        propietario = cliente.id
        cargando = true
        error = null
        viewModelScope.launch(dispatcher) {
            try {
                val leida = fidelidadService.fidelidad(cliente.id).cuerpoOpcional()?.aFidelidad()
                    ?: FidelidadCliente(cliente.id, fechaActualizacion = "")
                if (propietario == cliente.id) miFidelidad = leida
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (propietario == cliente.id) error = "No se pudo cargar tu fidelidad."
            } finally {
                if (propietario == cliente.id) cargando = false
            }
        }
    }

    fun cargarAdmin(admin: Usuario?) {
        val valido = admin?.takeIf { it.id.isNotBlank() && it.rol.equals("ADMIN", true) }
            ?: run {
                limpiar()
                error = "Solo un administrador puede gestionar fidelidad."
                return
            }
        if (cargando) return
        propietario = valido.id
        cargando = true
        error = null
        mensaje = null
        conflicto = false
        viewModelScope.launch(dispatcher) {
            try {
                val config = fidelidadService.configuracion().cuerpoOpcional()?.aConfiguracion()
                val estados = fidelidadService.listarTodasFidelidades()
                    .mapNotNull { runCatching { it.aFidelidad() }.getOrNull() }
                    .associateBy { it.usuarioId }
                val usuarios = usuarioService.listarTodosUsuarios().documents.orEmpty()
                    .map(::documentoAUsuario)
                    .filter { it.rol.equals("CLIENTE", true) }
                val ranking = usuarios.map { usuario ->
                    val estado = estados[usuario.id]
                        ?: FidelidadCliente(usuario.id, fechaActualizacion = "")
                    val recompensa = estado.recompensaPendienteId?.let { id ->
                        fidelidadService.recompensa(id).cuerpoOpcional()?.aRecompensa()
                    }
                    ClienteFidelidad(usuario, estado, recompensa)
                }
                if (propietario == valido.id) {
                    configuracion = config
                    clientes = ordenarClientesPorFidelidad(ranking)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (propietario == valido.id) error = "No se pudo cargar la gestión de fidelidad."
            } finally {
                if (propietario == valido.id) cargando = false
            }
        }
    }

    fun configurarRecompensa(admin: Usuario?, producto: Producto) {
        if (guardando) return
        val adminId = admin?.takeIf { it.id.isNotBlank() && it.rol.equals("ADMIN", true) }?.id
            ?: run { error = "Solo un administrador puede configurar la recompensa."; return }
        val precio = runCatching { BigDecimal.valueOf(producto.precio).setScale(2) }.getOrNull()
        if (producto.id.isBlank() || producto.nombre.isBlank() || precio == null || precio.signum() < 0) {
            error = "Selecciona un producto válido."
            return
        }
        guardando = true
        error = null
        mensaje = null
        conflicto = false
        viewModelScope.launch(dispatcher) {
            try {
                val actual = fidelidadService.configuracion().cuerpoOpcional()
                val nueva = ConfiguracionFidelidad(
                    producto.id, producto.nombre, producto.marca, producto.categoria,
                    precio, adminId, Instant.now().toString(), actual?.updateTime
                )
                val write = FidelidadEscritura(
                    nueva.aDocumento(RUTA_DOCUMENTOS_FIRESTORE),
                    actual?.updateTime?.let { FidelidadPrecondicion(updateTime = it) }
                        ?: FidelidadPrecondicion(exists = false),
                    actual?.let {
                        FidelidadMascara(listOf(
                            "productoId", "nombre", "marca", "categoria", "precioReferencia",
                            "configuradoPor", "fechaConfiguracion"
                        ))
                    }
                )
                val respuesta = fidelidadService.commit(FidelidadCommit(listOf(write)))
                val version = respuesta.writeResults.firstOrNull()?.updateTime
                    ?: throw IllegalStateException("Firestore no devolvió la versión de la configuración.")
                configuracion = nueva.copy(updateTime = version)
                mensaje = "Recompensa configurada correctamente."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                registrarErrorOperacion(e, "No se pudo configurar la recompensa.")
            } finally {
                guardando = false
            }
        }
    }

    fun entregarRecompensa(admin: Usuario?, usuarioId: String) {
        if (guardando) return
        val adminId = admin?.takeIf { it.id.isNotBlank() && it.rol.equals("ADMIN", true) }?.id
            ?: run { error = "Solo un administrador puede entregar recompensas."; return }
        if (usuarioId.isBlank() || usuarioId.contains('/')) {
            error = "Cliente inválido."
            return
        }
        guardando = true
        error = null
        mensaje = null
        conflicto = false
        viewModelScope.launch(dispatcher) {
            try {
                val entrega = entregarRecompensaAtomica(
                    fidelidadService, usuarioId, adminId, Instant.now().toString(), configuracion
                )
                clientes = clientes.map {
                    if (it.usuario.id == usuarioId) it.copy(
                        fidelidad = entrega.fidelidad,
                        recompensa = entrega.recompensa
                    ) else it
                }.let(::ordenarClientesPorFidelidad)
                entrega.configuracionUpdateTime?.let { version ->
                    configuracion = configuracion?.copy(updateTime = version)
                }
                mensaje = "Recompensa entregada correctamente."
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalArgumentException) {
                error = e.message
            } catch (e: Exception) {
                registrarErrorOperacion(e, "No se pudo registrar la entrega.")
            } finally {
                guardando = false
            }
        }
    }

    fun limpiarMensajes() {
        error = null
        mensaje = null
        conflicto = false
    }

    private fun registrarErrorOperacion(excepcion: Exception, mensajeDesconocido: String) {
        when (clasificarErrorFidelidad(excepcion)) {
            TipoErrorFidelidad.CONFLICTO_VERSION -> {
                conflicto = true
                error = "Los datos cambiaron desde que los cargaste. Actualiza la información e inténtalo nuevamente."
            }
            TipoErrorFidelidad.ERROR_RED -> {
                conflicto = false
                error = "No se pudo conectar. Verifica tu conexión e inténtalo nuevamente."
            }
            TipoErrorFidelidad.ERROR_DESCONOCIDO -> {
                conflicto = false
                error = mensajeDesconocido
            }
        }
    }

    fun limpiar() {
        propietario = null
        miFidelidad = null
        clientes = emptyList()
        configuracion = null
        cargando = false
        guardando = false
        error = null
        mensaje = null
        conflicto = false
    }
}
