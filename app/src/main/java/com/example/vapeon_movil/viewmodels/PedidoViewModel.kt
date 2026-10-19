package com.example.vapeon_movil.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vapeon_movil.entities.*
import com.example.vapeon_movil.services.PedidoService
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.utils.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import com.google.gson.Gson
import com.google.gson.JsonParser

class PedidoViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        val ESTADOS_ENVIO = listOf("REGISTRADO", "EN CAMINO", "ENTREGADO")
    }
    internal data class Intento(val pedido: Pedido, val commit: PedidoCommit, val confirmado: Boolean = false)
    private val prefs = application.getSharedPreferences("vapeon_checkout", Context.MODE_PRIVATE)
    private val recuperacion = RecuperacionCheckout(
        leer = { prefs.getString(it, null) },
        guardar = { clave, json -> prefs.edit().putString(clave, json).commit() },
        eliminar = { prefs.edit().remove(it).commit() }
    )
    private var operacionCheckout: Job? = null
    private val service = RetrofitClient.retrofit.create(PedidoService::class.java)
    private var pedidoPendiente: Pedido? = null
    private var commitPendiente: PedidoCommit? = null
    var cargando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var pedidoConfirmado by mutableStateOf<Pedido?>(null)
        private set
    var requiereComprobacion by mutableStateOf(false)
        private set

    // RF26: estados independientes; no se reutiliza ni altera el estado del checkout.
    var usuarioHistorialId by mutableStateOf<String?>(null)
        private set
    var historial by mutableStateOf<List<Pedido>>(emptyList())
        private set
    var cargandoHistorial by mutableStateOf(false)
        private set
    var errorHistorial by mutableStateOf<String?>(null)
        private set
    var invalidosHistorial by mutableIntStateOf(0)
        private set
    var invalidosAdmin by mutableIntStateOf(0)
        private set
    var detallePedido by mutableStateOf<Pedido?>(null)
        private set
    var cargandoDetalle by mutableStateOf(false)
        private set
    var errorDetalle by mutableStateOf<String?>(null)
        private set
    private var cargaHistorial: Job? = null
    private var cargaDetalle: Job? = null
    private var detalleSolicitadoId: String? = null

    fun actualizarUsuarioHistorial(usuario: Usuario?) {
        val id = usuario?.takeIf { it.rol.equals("CLIENTE", true) && it.id.isNotBlank() }?.id
        if (usuarioHistorialId == id) return
        cargaHistorial?.cancel()
        cargaDetalle?.cancel()
        usuarioHistorialId = id
        historial = emptyList()
        invalidosHistorial = 0
        detallePedido = null
        detalleSolicitadoId = null
        errorHistorial = null
        errorDetalle = null
        cargandoHistorial = false
        cargandoDetalle = false
    }

    fun cargarHistorial(usuarioId: String) {
        if (usuarioId.isBlank() || usuarioId != usuarioHistorialId) return
        cargaHistorial?.cancel()
        historial = emptyList()
        invalidosHistorial = 0
        errorHistorial = null
        cargandoHistorial = true
        cargaHistorial = viewModelScope.launch {
            try {
                val respuesta = service.consultarPedidos(consultaPedidosDelUsuario(usuarioId))
                val (pedidos, invalidos) = respuesta.mapNotNull { it.document }.leerPedidosValidos(usuarioId)
                if (usuarioHistorialId == usuarioId) {
                    historial = pedidos
                    invalidosHistorial = invalidos
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (usuarioHistorialId == usuarioId) errorHistorial =
                    mensajeCargaPedido(e, "tus compras")
            } finally {
                if (usuarioHistorialId == usuarioId && coroutineContext[Job]?.isCancelled != true)
                    cargandoHistorial = false
            }
        }
    }

    fun cargarDetallePedido(usuarioId: String, pedidoId: String) {
        if (usuarioId.isBlank() || usuarioId != usuarioHistorialId) return
        cargaDetalle?.cancel()
        detallePedido = null // Nunca mostrar el detalle previo mientras se valida otro pedido.
        detalleSolicitadoId = pedidoId
        errorDetalle = null
        if (pedidoId.isBlank() || pedidoId.contains('/')) {
            errorDetalle = "ID de pedido inválido."
            cargandoDetalle = false
            return
        }
        cargandoDetalle = true
        cargaDetalle = viewModelScope.launch {
            try {
                val doc = service.mostrarPedido(pedidoId)
                val pedido = doc.aPedidoDelUsuario(usuarioId) // Comprueba pertenencia antes de exponer datos.
                require(pedido.id == pedidoId) { "ID de pedido inválido." }
                if (usuarioHistorialId == usuarioId && detalleSolicitadoId == pedidoId) detallePedido = pedido
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (usuarioHistorialId == usuarioId && detalleSolicitadoId == pedidoId)
                    errorDetalle = if (e is IllegalArgumentException) e.message
                    else mensajeCargaPedido(e, "el pedido")
            } finally {
                if (usuarioHistorialId == usuarioId && detalleSolicitadoId == pedidoId &&
                    coroutineContext[Job]?.isCancelled != true) cargandoDetalle = false
            }
        }
    }

    // RF28: administración independiente del checkout y del historial del cliente.
    var usuarioAdminId by mutableStateOf<String?>(null)
        private set
    var pedidosAdmin by mutableStateOf<List<Pedido>>(emptyList())
        private set
    var detalleAdmin by mutableStateOf<Pedido?>(null)
        private set
    var cargandoPedidosAdmin by mutableStateOf(false)
        private set
    var cargandoDetalleAdmin by mutableStateOf(false)
        private set
    var guardandoEstado by mutableStateOf(false)
        private set
    var errorPedidosAdmin by mutableStateOf<String?>(null)
        private set
    var errorDetalleAdmin by mutableStateOf<String?>(null)
        private set
    var errorEstado by mutableStateOf<String?>(null)
        private set
    var mensajeEstado by mutableStateOf<String?>(null)
        private set
    private var cargaPedidosAdmin: Job? = null
    private var cargaDetalleAdmin: Job? = null
    private var guardadoEstado: Job? = null
    private var detalleAdminSolicitado: String? = null

    fun actualizarUsuarioAdmin(usuario: Usuario?) {
        val id = usuario?.takeIf { it.rol.equals("ADMIN", true) && it.id.isNotBlank() }?.id
        if (usuarioAdminId == id) return
        cargaPedidosAdmin?.cancel()
        cargaDetalleAdmin?.cancel()
        guardadoEstado?.cancel()
        usuarioAdminId = id
        pedidosAdmin = emptyList()
        invalidosAdmin = 0
        detalleAdmin = null
        detalleAdminSolicitado = null
        cargandoPedidosAdmin = false
        cargandoDetalleAdmin = false
        guardandoEstado = false
        errorPedidosAdmin = null
        errorDetalleAdmin = null
        errorEstado = null
        mensajeEstado = null
    }

    fun cargarPedidosAdmin(usuario: Usuario?) {
        actualizarUsuarioAdmin(usuario)
        val adminId = usuarioAdminId ?: run {
            errorPedidosAdmin = "Solo un administrador puede consultar los pedidos."
            return
        }
        cargaPedidosAdmin?.cancel()
        pedidosAdmin = emptyList()
        invalidosAdmin = 0
        errorPedidosAdmin = null
        cargandoPedidosAdmin = true
        cargaPedidosAdmin = viewModelScope.launch {
            try {
                val respuesta = service.consultarPedidos(PedidoConsulta(
                    PedidoConsultaEstructurada(listOf(PedidoColeccion("pedidos")))))
                val (pedidos, invalidos) = respuesta.mapNotNull { it.document }.leerPedidosValidos()
                if (usuarioAdminId == adminId) {
                    pedidosAdmin = pedidos
                    invalidosAdmin = invalidos
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (usuarioAdminId == adminId) errorPedidosAdmin =
                    mensajeCargaPedido(e, "los pedidos")
            } finally {
                if (usuarioAdminId == adminId && coroutineContext[Job]?.isCancelled != true)
                    cargandoPedidosAdmin = false
            }
        }
    }

    fun cargarDetalleAdmin(usuario: Usuario?, pedidoId: String) {
        actualizarUsuarioAdmin(usuario)
        val adminId = usuarioAdminId ?: run {
            errorDetalleAdmin = "Solo un administrador puede consultar este detalle."
            return
        }
        if (guardandoEstado) return
        cargaDetalleAdmin?.cancel()
        detalleAdmin = null
        detalleAdminSolicitado = pedidoId
        errorDetalleAdmin = null
        errorEstado = null
        mensajeEstado = null
        if (pedidoId.isBlank() || pedidoId.contains('/')) {
            errorDetalleAdmin = "ID de pedido inválido."
            cargandoDetalleAdmin = false
            return
        }
        cargandoDetalleAdmin = true
        cargaDetalleAdmin = viewModelScope.launch {
            try {
                val pedido = service.mostrarPedido(pedidoId).aPedido()
                require(pedido.id == pedidoId) { "ID de pedido inválido." }
                if (usuarioAdminId == adminId && detalleAdminSolicitado == pedidoId) detalleAdmin = pedido
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (usuarioAdminId == adminId && detalleAdminSolicitado == pedidoId)
                    errorDetalleAdmin = mensajeCargaPedido(e, "el pedido")
            } finally {
                if (usuarioAdminId == adminId && detalleAdminSolicitado == pedidoId &&
                    coroutineContext[Job]?.isCancelled != true) cargandoDetalleAdmin = false
            }
        }
    }

    fun actualizarEstadoPedido(usuario: Usuario?, pedidoId: String, nuevoEstado: String) {
        if (guardandoEstado) return
        errorEstado = null
        mensajeEstado = null
        try {
            validarCambioEstadoPedido(usuario, pedidoId, nuevoEstado)
            require(usuario?.id == usuarioAdminId) { "La sesión del administrador cambió." }
            require(!cargandoDetalleAdmin && detalleAdmin?.id == pedidoId && detalleAdminSolicitado == pedidoId) {
                "Abre el detalle del pedido antes de cambiar su estado."
            }
        } catch (e: IllegalArgumentException) {
            errorEstado = e.message
            return
        }
        val adminId = usuarioAdminId
        guardandoEstado = true
        guardadoEstado = viewModelScope.launch {
            try {
                val documento = service.actualizarEstadoPedido(pedidoId, PedidoEstadoActualizacion(
                    mapOf("estado" to FirestoreValor(stringValue = nuevoEstado))))
                val actualizado = documento.aPedido()
                require(actualizado.id == pedidoId && actualizado.estado == nuevoEstado) {
                    "La respuesta no confirma el estado solicitado. Actualiza el pedido."
                }
                if (usuarioAdminId == adminId && detalleAdminSolicitado == pedidoId) {
                    detalleAdmin = actualizado
                    pedidosAdmin = pedidosAdmin.map { if (it.id == pedidoId) actualizado else it }
                    mensajeEstado = "Estado guardado correctamente."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (usuarioAdminId == adminId && detalleAdminSolicitado == pedidoId)
                    errorEstado = "No se pudo confirmar el guardado. Actualiza el pedido para comprobar el estado en Firestore."
            } finally {
                if (usuarioAdminId == adminId && coroutineContext[Job]?.isCancelled != true) guardandoEstado = false
            }
        }
    }

    fun actualizarUsuarioCheckout(usuario: Usuario?) {
        val id = usuario?.takeIf { it.id.isNotBlank() && it.rol.equals("CLIENTE", true) }?.id
        if (!recuperacion.actualizarUsuario(id)) return
        // Cancelar no borra el intento: la petición podría haber llegado a Firestore.
        operacionCheckout?.cancel()
        operacionCheckout = null
        cargando = false
        error = null
        pedidoConfirmado = recuperacion.intento?.takeIf { it.confirmado }?.pedido
        pedidoPendiente = recuperacion.intento?.pedido
        commitPendiente = recuperacion.intento?.commit
        requiereComprobacion = recuperacion.intento != null
    }

    fun iniciarCheckout() {
        if (!cargando && !requiereComprobacion) {
            error = null
            pedidoConfirmado = null
        }
    }

    fun confirmar(
        usuario: Usuario?, carrito: CarritoViewModel, productos: ProductoViewModel,
        direccion: String, montoTexto: String
    ) {
        actualizarUsuarioCheckout(usuario)
        if (cargando || (pedidoConfirmado != null && !requiereComprobacion)) return
        cargando = true // Antes de launch para impedir dobles pulsaciones.
        error = null
        val comprobandoIntentoAnterior = requiereComprobacion
        operacionCheckout = viewModelScope.launch {
            try {
                require(usuario != null && usuario.id.isNotBlank() &&
                    usuario.rol.equals("CLIENTE", true)) { "Inicia sesión como cliente para comprar." }

                pedidoConfirmado?.let { confirmado ->
                    // Ya confirmado en Firestore: solo completar la persistencia local.
                    terminarCompra(confirmado, carrito, productos)
                    return@launch
                }

                // Tras una respuesta perdida solo se comprueba el mismo ID. No se crea otro pedido.
                if (requiereComprobacion) {
                    val pendiente = requireNotNull(pedidoPendiente)
                    require(pendiente.usuarioId == usuario.id) { "El pedido pertenece a otra sesión." }
                    try {
                        val guardado = service.mostrarPedido(pendiente.id)
                        coroutineContext.ensureActive()
                        require(guardado.fields["usuarioId"]?.stringValue == usuario.id) {
                            "No se pudo verificar el propietario del pedido."
                        }
                        terminarCompra(pendiente, carrito, productos)
                        return@launch
                    } catch (e: HttpException) {
                        coroutineContext.ensureActive()
                        if (e.code() != 404) throw e
                    }
                    // Repetir el mismo commit con exists=false es seguro incluso si el anterior llega tarde.
                } else {
                    require(usuario.nombre.isNotBlank() && usuario.telefono.isNotBlank()) {
                        "Completa tu nombre y teléfono antes de comprar."
                    }
                    require(direccion.isNotBlank()) { "Ingresa la dirección de entrega." }
                    val items = carrito.items.toList()
                    val subtotal = Pedido.subtotal(items)
                    val delivery = BigDecimal.valueOf(carrito.resumen.delivery).setScale(2)
                    val total = subtotal + delivery
                    val monto = Pedido.leerMonto(montoTexto)
                        ?: throw IllegalArgumentException("Ingresa un monto numérico válido con máximo dos decimales.")
                    val vuelto = Pedido.calcularVuelto(monto, total)
                    val escriturasStock = mutableListOf<PedidoEscritura>()
                    val lineas = items.map { item ->
                        val doc = productos.consultarProducto(item.productoId)
                        coroutineContext.ensureActive()
                        val escrituraStock = prepararDescuentoStockParaCompra(doc, item.cantidad)
                        val precio = leerPrecioProductoParaCompra(doc)
                        require(precio != null && precio.signum() > 0) { "Precio inválido para ${item.nombre}." }
                        require(precio.compareTo(BigDecimal.valueOf(item.precioUnit)) == 0) {
                            "El precio de ${item.nombre} cambió. Elimínalo del carrito y agrégalo nuevamente desde el catálogo."
                        }
                        val nombre = doc.fields.nombre?.stringValue.orEmpty()
                        val marca = doc.fields.marca?.stringValue.orEmpty()
                        require(nombre.isNotBlank() && marca.isNotBlank() && !doc.updateTime.isNullOrBlank()) {
                            "Datos incompletos para ${item.nombre}."
                        }
                        escriturasStock += escrituraStock
                        ProductoPedido(item.productoId, nombre, marca, item.cantidad, precio,
                            precio.multiply(BigDecimal(item.cantidad)))
                    }
                    val pedido = Pedido(UUID.randomUUID().toString(), usuario.id,
                        "${usuario.nombre} ${usuario.apellido}".trim(), usuario.telefono.trim(),
                        Instant.now().toString(), lineas, subtotal, delivery, total, monto, vuelto, direccion.trim())
                    // Se obtiene la ruta desde un documento real, sin duplicar PROJECT_ID.
                    val ruta = escriturasStock.first().update.name.substringBeforeLast("/productos/")
                    pedidoPendiente = pedido
                    commitPendiente = PedidoCommit(listOf(PedidoEscritura(
                        pedido.aDocumento(ruta), PedidoPrecondicion(exists = false)
                    )) + escriturasStock)
                }

                coroutineContext.ensureActive()
                check(recuperacion.guardar(Intento(requireNotNull(pedidoPendiente),
                    requireNotNull(commitPendiente)))) {
                    "No se pudo conservar el intento de compra en el dispositivo."
                }
                requiereComprobacion = true
                service.confirmarPedido(requireNotNull(commitPendiente))
                coroutineContext.ensureActive()
                terminarCompra(requireNotNull(pedidoPendiente), carrito, productos)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                coroutineContext.ensureActive()
                if (e is HttpException && e.code() in listOf(400, 403, 404, 409, 412)) {
                    // Un commit rechazado no aplica ninguna escritura. Un reintento ambiguo
                    // conserva el ID hasta comprobarlo, incluso si hubo conflicto.
                    if (e.code() == 409 || e.code() == 412) {
                        // Diferencia un conflicto de stock de un pedido ya confirmado.
                        try {
                            val pendiente = requireNotNull(pedidoPendiente)
                            val guardado = service.mostrarPedido(pendiente.id)
                            coroutineContext.ensureActive()
                            if (guardado.fields["usuarioId"]?.stringValue == pendiente.usuarioId) {
                                terminarCompra(pendiente, carrito, productos)
                                return@launch
                            }
                        } catch (verificacion: HttpException) {
                            coroutineContext.ensureActive()
                            if (verificacion.code() == 404) descartarIntento()
                        } catch (verificacion: Exception) {
                            if (verificacion is CancellationException) throw verificacion
                            // Mantener el ID si no se pudo comprobar el resultado.
                        }
                    } else if (!comprobandoIntentoAnterior) {
                        descartarIntento()
                    }
                    error = when (e.code()) {
                        403 -> "Firestore rechazó la operación. Revisa los permisos. Tu carrito se conserva."
                        404 -> "Un producto ya no está disponible. Tu carrito se conserva."
                        409, 412 -> if (requiereComprobacion)
                            "No se pudo verificar el resultado. Pulsa Comprobar pedido."
                            else "El inventario cambió durante la compra. Revisa el stock y vuelve a confirmar."
                        else -> "Firestore rechazó los datos del pedido. Tu carrito se conserva."
                    }
                } else {
                    error = if (requiereComprobacion)
                        "No se pudo confirmar la respuesta. Conservamos tu carrito; pulsa Comprobar pedido antes de repetir la compra."
                    else e.message ?: "No se pudo registrar el pedido. Tu carrito se conserva."
                }
            } finally {
                if (coroutineContext[Job]?.isCancelled != true) cargando = false
            }
        }
    }

    private fun terminarCompra(pedido: Pedido, carrito: CarritoViewModel, productos: ProductoViewModel) {
        pedidoConfirmado = pedido
        requiereComprobacion = true
        if (!recuperacion.guardar(Intento(pedido, requireNotNull(commitPendiente), confirmado = true))) {
            error = "El pedido ya fue registrado. No se pudo conservar la confirmación local. Reintenta la limpieza."
            return
        }
        if (!carrito.limpiarCarrito()) {
            error = "El pedido ya fue registrado y el stock descontado. No se pudo limpiar el carrito guardado; reintenta la limpieza."
            return
        }
        if (!recuperacion.descartar()) {
            error = "El pedido ya fue registrado y el carrito vaciado. Falta retirar el comprobante local; reintenta la limpieza."
            return
        }
        requiereComprobacion = false
        pedidoPendiente = null
        commitPendiente = null
        error = null
        productos.cargarProductos()
    }

    private fun descartarIntento() {
        requiereComprobacion = false
        pedidoPendiente = null
        commitPendiente = null
        recuperacion.descartar()
    }
}

/** Usa las mismas preferencias; las funciones permiten probarlas sin Android ni otra biblioteca. */
internal class RecuperacionCheckout(
    private val leer: (String) -> String?,
    private val guardar: (String, String) -> Boolean,
    private val eliminar: (String) -> Boolean
) {
    private val gson = Gson()
    private var usuarioId: String? = null
    var intento: PedidoViewModel.Intento? = null
        private set

    fun actualizarUsuario(id: String?): Boolean {
        val actual = id?.takeIf { it.isNotBlank() }
        if (usuarioId == actual) return false
        usuarioId = actual
        intento = null
        if (actual != null) {
            intento = leerValido("intento_$actual")?.takeIf { it.pedido.usuarioId == actual }
                // Compatibilidad con el registro global anterior: no se activa para otro cliente.
                ?: leerValido("intento")?.takeIf { it.pedido.usuarioId == actual }
        }
        return true
    }

    private fun leerValido(clave: String): PedidoViewModel.Intento? {
        val json = leer(clave) ?: return null
        return try {
            val propietario = JsonParser().parse(json).asJsonObject
                .getAsJsonObject("pedido").get("usuarioId").asString
            require(propietario.isNotBlank())
            // No reconstruir el pedido ni su commit si pertenecen a otra sesión.
            if (propietario != usuarioId) return null
            val valor = requireNotNull(gson.fromJson(json, PedidoViewModel.Intento::class.java))
            require(valor.pedido.usuarioId.isNotBlank() && valor.pedido.id.isNotBlank())
            // Gson puede crear objetos incompletos aun cuando los campos Kotlin no sean anulables.
            valor.pedido.aDocumento("local").aPedido()
            require(valor.commit.writes.size >= 2)
            valor.commit.writes.forEach {
                require(it.update.name.isNotBlank() && it.update.fields.isNotEmpty())
                require(it.currentDocument.exists != null || !it.currentDocument.updateTime.isNullOrBlank())
            }
            val documento = valor.commit.writes.first().update
            require(documento.fields["usuarioId"]?.stringValue == valor.pedido.usuarioId &&
                documento.name.endsWith("/pedidos/${valor.pedido.id}"))
            valor
        } catch (_: Exception) {
            eliminar(clave) // Solo el registro inválido; nunca clear() sobre las preferencias.
            null
        }
    }

    fun guardar(valor: PedidoViewModel.Intento): Boolean {
        val id = usuarioId ?: return false
        if (valor.pedido.usuarioId != id) return false
        if (!guardar("intento_$id", gson.toJson(valor))) return false
        intento = valor
        // Retira el registro antiguo únicamente después de conservar el mismo intento por usuario.
        if (leerValido("intento")?.pedido?.usuarioId == id) eliminar("intento")
        return true
    }

    fun descartar(): Boolean {
        val id = usuarioId ?: return false
        if (leerValido("intento")?.pedido?.usuarioId == id && !eliminar("intento")) return false
        if (!eliminar("intento_$id")) return false
        intento = null
        return true
    }
}

/** Validación de RF28 antes de cualquier escritura de estado. */
internal fun validarCambioEstadoPedido(usuario: Usuario?, pedidoId: String, nuevoEstado: String) {
    require(usuario != null && usuario.id.isNotBlank() && usuario.rol.equals("ADMIN", true)) {
        "Solo un administrador puede cambiar el estado de un pedido."
    }
    require(pedidoId.isNotBlank() && !pedidoId.contains('/')) { "ID de pedido inválido." }
    require(nuevoEstado in PedidoViewModel.ESTADOS_ENVIO) { "Estado de envío inválido." }
}

// RF22: admite los tipos numéricos actuales y los documentos antiguos con texto.
internal fun leerPrecioProductoParaCompra(doc: ProductoDocumento): BigDecimal? =
    Pedido.leerMonto(doc.fields.precio?.asString().orEmpty())

/** Prepara una escritura para el mismo commit atómico, sin ejecutar otra petición. */
internal fun prepararDescuentoStockParaCompra(doc: ProductoDocumento, cantidad: Int): PedidoEscritura {
    val nuevoStock = Pedido.validarStock(doc.fields.stock?.asString(), cantidad)
    return PedidoEscritura(
        PedidoDocumento(doc.name, mapOf("stock" to FirestoreValor(integerValue = nuevoStock.toString()))),
        PedidoPrecondicion(updateTime = doc.updateTime), PedidoMascara(listOf("stock"))
    )
}

internal fun mensajeCargaPedido(error: Exception, contenido: String): String = when {
    error is HttpException && error.code() in listOf(401, 403) -> "Firestore no permite consultar $contenido. Revisa los permisos."
    error is HttpException && error.code() == 404 -> "El pedido ya no está disponible."
    error is IllegalArgumentException || error is java.time.DateTimeException -> "El pedido contiene datos inválidos y no se puede mostrar."
    else -> "No se pudo cargar $contenido. Comprueba la conexión y reintenta."
}
