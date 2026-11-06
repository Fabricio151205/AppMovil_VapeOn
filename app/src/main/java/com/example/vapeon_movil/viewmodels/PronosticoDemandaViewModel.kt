package com.example.vapeon_movil.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vapeon_movil.entities.CriteriosPronosticoDemanda
import com.example.vapeon_movil.entities.ResultadoPronosticoDemanda
import com.example.vapeon_movil.entities.SerieDemandaProducto
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.entities.construirSeriesDemanda
import com.example.vapeon_movil.entities.generarPronosticoDemanda
import com.example.vapeon_movil.services.PedidoService
import com.example.vapeon_movil.services.dto.PedidoColeccion
import com.example.vapeon_movil.services.dto.PedidoConsulta
import com.example.vapeon_movil.services.dto.PedidoConsultaEstructurada
import com.example.vapeon_movil.services.dto.leerPedidosValidos
import com.example.vapeon_movil.utils.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PronosticoDemandaViewModel(
    private val service: PedidoService = RetrofitClient.retrofit.create(PedidoService::class.java),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    var series by mutableStateOf<List<SerieDemandaProducto>>(emptyList())
        private set
    var productoSeleccionadoId by mutableStateOf("")
        private set
    var horizonte by mutableIntStateOf(CriteriosPronosticoDemanda.HORIZONTE_PREDETERMINADO)
        private set
    var resultado by mutableStateOf<ResultadoPronosticoDemanda?>(null)
        private set
    var cargando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var documentosInvalidos by mutableIntStateOf(0)
        private set

    private var adminId: String? = null
    private var carga: Job? = null

    fun cargar(usuario: Usuario?) {
        val id = usuario?.takeIf { it.rol.equals("ADMIN", true) && it.id.isNotBlank() }?.id
        if (id == null) {
            limpiar()
            error = "Solo un administrador puede consultar el pronóstico de demanda."
            return
        }
        if (adminId != id) {
            limpiar()
            adminId = id
        }
        carga?.cancel()
        cargando = true
        error = null
        carga = viewModelScope.launch {
            try {
                val consulta = PedidoConsulta(PedidoConsultaEstructurada(listOf(PedidoColeccion("pedidos"))))
                val respuesta = withContext(dispatcher) { service.consultarPedidos(consulta) }
                val (pedidos, invalidos) = respuesta.mapNotNull { it.document }.leerPedidosValidos()
                if (adminId != id) return@launch
                documentosInvalidos = invalidos
                series = withContext(dispatcher) { construirSeriesDemanda(pedidos) }
                if (series.none { it.productoId == productoSeleccionadoId }) {
                    productoSeleccionadoId = series.firstOrNull()?.productoId.orEmpty()
                }
                recalcular()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (adminId == id) error = "No se pudo cargar el historial real de ventas. Inténtalo nuevamente."
            } finally {
                if (adminId == id) cargando = false
            }
        }
    }

    fun seleccionarProducto(productoId: String) {
        if (series.none { it.productoId == productoId }) return
        productoSeleccionadoId = productoId
        recalcular()
    }

    fun cambiarHorizonte(nuevoHorizonte: Int) {
        if (nuevoHorizonte !in CriteriosPronosticoDemanda.HORIZONTES_PERMITIDOS) return
        horizonte = nuevoHorizonte
        recalcular()
    }

    private fun recalcular() {
        val serie = series.firstOrNull { it.productoId == productoSeleccionadoId }
        resultado = serie?.let { generarPronosticoDemanda(it, horizonte) }
    }

    private fun limpiar() {
        carga?.cancel()
        adminId = null
        series = emptyList()
        productoSeleccionadoId = ""
        resultado = null
        documentosInvalidos = 0
        cargando = false
        error = null
    }
}
