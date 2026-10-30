package com.example.vapeon_movil.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vapeon_movil.entities.*
import com.example.vapeon_movil.services.TarjetaService
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.utils.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.Instant
import java.time.YearMonth
import java.util.UUID

class TarjetaViewModel(
    private val service: TarjetaService = RetrofitClient.retrofit.create(TarjetaService::class.java),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val hoy: () -> YearMonth = { YearMonth.now() }
) : ViewModel() {
    var usuarioId by mutableStateOf<String?>(null)
        private set
    var tarjetas by mutableStateOf<List<Tarjeta>>(emptyList())
        private set
    var cargando by mutableStateOf(false)
        private set
    var registrando by mutableStateOf(false)
        private set
    var seleccionando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var mensaje by mutableStateOf<String?>(null)
        private set
    /** Preparada para ser consultada en una integración posterior; RF23 no la utiliza para cobrar. */
    var tarjetaSeleccionada by mutableStateOf<Tarjeta?>(null)
        private set
    var registroPendiente by mutableStateOf<Tarjeta?>(null)
        private set
    private var generacion = 0
    private var carga: Job? = null
    private var registro: Job? = null
    private var seleccion: Job? = null

    fun actualizarUsuario(usuario: Usuario?) {
        val id = usuario?.takeIf { it.rol.equals("CLIENTE", true) && it.id.isNotBlank() }?.id
        if (id == usuarioId) return
        generacion++
        carga?.cancel(); registro?.cancel(); seleccion?.cancel()
        usuarioId = id
        tarjetas = emptyList()
        tarjetaSeleccionada = null
        registroPendiente = null
        cargando = false; registrando = false; seleccionando = false
        error = null; mensaje = null
    }

    private fun cliente(usuario: Usuario?): String? {
        actualizarUsuario(usuario)
        return usuarioId.also { if (it == null) error = "Inicia sesión como cliente para acceder a tus tarjetas." }
    }

    fun cargarTarjetas(usuario: Usuario?) {
        val id = cliente(usuario) ?: return
        carga?.cancel()
        val sesion = generacion
        cargando = true
        error = null
        carga = viewModelScope.launch(dispatcher) {
            try {
                val documentos = service.consultarTarjetas(consultaTarjetasDelUsuario(id)).mapNotNull { it.document }
                var invalidas = 0
                val propias = documentos.filter { it.fields["usuarioId"]?.stringValue == id }.mapNotNull {
                    try { it.aTarjetaDelUsuario(id) }
                    catch (_: Exception) { invalidas++; null }
                }.distinctBy { it.id }.sortedByDescending { Instant.parse(it.fechaCreacion) }
                coroutineContext.ensureActive()
                if (generacion == sesion) {
                    tarjetas = propias
                    if (invalidas > 0) error = "No se pudieron interpretar $invalidas tarjetas."
                    tarjetaSeleccionada = tarjetaSeleccionada?.let { anterior ->
                        propias.firstOrNull { it.id == anterior.id && !it.estaVencida(hoy()) }
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (generacion == sesion) error = "No se pudieron cargar tus tarjetas. Reintenta."
            } finally {
                if (generacion == sesion && coroutineContext[Job]?.isCancelled != true) cargando = false
            }
        }
    }

    fun registrarTarjeta(usuario: Usuario?, titular: String, marca: String, ultimos4: String, mes: String, anio: String) {
        val id = cliente(usuario) ?: return
        if (registrando) return
        error = validarRegistroTarjeta(titular, marca, ultimos4, mes, anio, hoy())
        if (error != null) return
        val pendiente = registroPendiente
        val tarjeta = Tarjeta(pendiente?.id ?: UUID.randomUUID().toString(), id, titular.trim(), marca.trim(),
            ultimos4, mes.toInt(), anio.toInt(), pendiente?.fechaCreacion ?: Instant.now().toString())
        if (pendiente != null && tarjeta != pendiente) {
            error = "Reintenta el registro anterior con los mismos datos antes de registrar otra tarjeta."
            return
        }
        registroPendiente = tarjeta
        val sesion = generacion
        registrando = true
        mensaje = null
        registro = viewModelScope.launch(dispatcher) {
            try {
                val documento = try { service.crearTarjeta(tarjeta.aFirestore(), tarjeta.id) }
                catch (e: HttpException) {
                    if (e.code() != 409) throw e
                    service.mostrarTarjeta(tarjeta.id)
                }
                require(documento.aTarjetaDelUsuario(id) == tarjeta) { "El registro no coincide con la tarjeta enviada." }
                coroutineContext.ensureActive()
                if (generacion == sesion) {
                    registroPendiente = null
                    tarjetas = (listOf(tarjeta) + tarjetas).distinctBy { it.id }
                    mensaje = "Tarjeta simulada registrada. Puedes seleccionarla para un uso posterior."
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (generacion == sesion) {
                    if (e is HttpException && e.code() in listOf(400, 401, 403)) registroPendiente = null
                    error = "No se pudo confirmar el registro. Reintenta; se utilizará el mismo identificador."
                }
            } finally { if (generacion == sesion) registrando = false }
        }
    }

    fun seleccionarTarjeta(usuario: Usuario?, tarjetaId: String) {
        val id = cliente(usuario) ?: return
        if (seleccionando) return
        tarjetaSeleccionada = null
        mensaje = null
        error = null
        if (tarjetaId.isBlank() || '/' in tarjetaId) { error = "ID de tarjeta inválido."; return }
        val sesion = generacion
        seleccionando = true
        seleccion = viewModelScope.launch(dispatcher) {
            try {
                val tarjeta = service.mostrarTarjeta(tarjetaId).aTarjetaDelUsuario(id)
                require(tarjeta.id == tarjetaId) { "ID de tarjeta inválido." }
                require(!tarjeta.estaVencida(hoy())) { "La tarjeta está vencida." }
                coroutineContext.ensureActive()
                if (generacion == sesion) {
                    tarjetaSeleccionada = tarjeta
                    mensaje = "Tarjeta seleccionada. El pago con tarjeta todavía no está habilitado."
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (generacion == sesion) error = if (e is IllegalArgumentException) e.message
                    else "No se pudo verificar la tarjeta. Reintenta."
            } finally { if (generacion == sesion) seleccionando = false }
        }
    }
}
