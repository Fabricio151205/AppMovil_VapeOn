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
import com.example.vapeon_movil.services.PagoService
import com.example.vapeon_movil.services.TarjetaService
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.utils.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.coroutineContext
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
    private val pagoService = RetrofitClient.retrofit.create(PagoService::class.java)
    private val tarjetaService = RetrofitClient.retrofit.create(TarjetaService::class.java)
    private val prefsPago = application.getSharedPreferences("vapeon_pagos_simulados", Context.MODE_PRIVATE)
    private val recuperacionPago = RecuperacionPago(
        { prefsPago.getString(it, null) },
        { clave, json -> prefsPago.edit().putString(clave, json).commit() },
        { prefsPago.edit().remove(it).commit() }
    )
    private var operacionPago: Job? = null
    private var intentoDigital by mutableStateOf<IntentoPagoDigital?>(null)
    val pagoActual: Pago? get() = intentoDigital?.pago
    val tieneIntentoDigital: Boolean get() = intentoDigital != null
    var cargandoPago by mutableStateOf(false)
        private set
    var errorPago by mutableStateOf<String?>(null)
        private set
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
        val propietarioDigital = usuario?.takeIf { it.id.isNotBlank() && it.rol.equals("CLIENTE", true) }?.id
        if (recuperacionPago.actualizarUsuario(propietarioDigital)) {
            operacionPago?.cancel()
            cargandoPago = false
            errorPago = null
            intentoDigital = recuperacionPago.intento
        }
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
        if (tieneIntentoDigital || cargandoPago) {
            error = "Resuelve el pago digital pendiente antes de iniciar otra compra."
            return
        }
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

    private fun ejecutarPago(usuario: Usuario?, accion: suspend (Long) -> Unit) {
        actualizarUsuarioCheckout(usuario)
        if (usuario == null || !usuario.rol.equals("CLIENTE", true) || usuario.id.isBlank()) {
            errorPago = "Inicia sesión como cliente para operar un pago."
            return
        }
        if (requiereComprobacion || cargando) {
            errorPago = "Primero comprueba la compra en efectivo pendiente."
            return
        }
        val token = recuperacionPago.iniciarOperacion() ?: return
        cargandoPago = true
        errorPago = null
        operacionPago = viewModelScope.launch {
            try {
                accion(token)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (recuperacionPago.vigente(token)) errorPago =
                    (if (e is IllegalArgumentException || e is IllegalStateException) e.message else null)
                        ?: "No se pudo comprobar la respuesta. Conservamos el mismo intento; pulsa Comprobar pago."
            } finally {
                if (recuperacionPago.vigente(token)) cargandoPago = false
                recuperacionPago.finalizarOperacion(token)
            }
        }
    }

    private fun conservarDigital(intento: IntentoPagoDigital, token: Long) {
        check(recuperacionPago.vigente(token)) { "La sesión cambió." }
        check(recuperacionPago.guardar(intento)) { "No se pudo conservar el intento digital en el dispositivo." }
        intentoDigital = intento
    }

    fun iniciarPagoDigital(usuario: Usuario?, carrito: CarritoViewModel, productos: ProductoViewModel,
        direccion: String, metodo: String, tarjetaId: String? = null) {
        ejecutarPago(usuario) { token ->
            // Si la respuesta anterior se perdió, se comprueba el mismo ID, nunca otra operación.
            intentoDigital?.let {
                val doc = obtenerOCrearPago(pagoService, it)
                coroutineContext.ensureActive()
                conservarDigital(it.copy(pago = doc.aPagoDelUsuario(it.pago.usuarioId)), token)
                return@ejecutarPago
            }
            require(metodo in listOf("YAPE", "TARJETA")) { "Método digital inválido." }
            val cliente = requireNotNull(usuario)
            require(cliente.nombre.isNotBlank() && cliente.telefono.isNotBlank() && direccion.isNotBlank()) {
                "Completa los datos y la dirección de entrega."
            }
            if (metodo == "TARJETA") {
                require(!tarjetaId.isNullOrBlank() && '/' !in tarjetaId) { "Selecciona una tarjeta propia." }
                val documentoTarjeta = tarjetaService.mostrarTarjeta(tarjetaId)
                coroutineContext.ensureActive()
                validarTarjetaParaPago(documentoTarjeta, cliente.id, tarjetaId)
            } else require(tarjetaId == null) { "Yape no utiliza tarjeta." }
            val items = carrito.items.toList()
            val subtotal = Pedido.subtotal(items)
            val delivery = BigDecimal.valueOf(carrito.resumen.delivery).setScale(2)
            val lineas = items.map { item ->
                val doc = productos.consultarProducto(item.productoId)
                coroutineContext.ensureActive()
                prepararDescuentoStockParaCompra(doc, item.cantidad)
                val precio = leerPrecioProductoParaCompra(doc)
                require(precio != null && precio.signum() > 0 && precio.compareTo(BigDecimal.valueOf(item.precioUnit)) == 0) {
                    "El precio cambió o es inválido. Revisa el carrito antes de pagar."
                }
                require(doc.name.substringAfterLast('/') == item.productoId && !doc.updateTime.isNullOrBlank()) {
                    "No se pudo verificar el producto."
                }
                val nombre = doc.fields.nombre?.asString().orEmpty()
                val marca = doc.fields.marca?.asString().orEmpty()
                require(nombre.isNotBlank() && marca.isNotBlank()) { "Producto incompleto." }
                ProductoPedido(item.productoId, nombre, marca, item.cantidad, precio, precio * BigDecimal(item.cantidad)) to
                    doc.name.substringBeforeLast("/productos/")
            }
            val ruta = lineas.first().second
            require(lineas.all { it.second == ruta }) { "Inventario inconsistente." }
            val pagoId = UUID.randomUUID().toString()
            val pedidoId = UUID.randomUUID().toString()
            val fecha = Instant.now().toString()
            val pago = Pago(pagoId, pedidoId, cliente.id, metodo, "SIM-$metodo-$pagoId", subtotal + delivery,
                fecha, tarjetaId = tarjetaId)
            val borrador = Pedido(pedidoId, cliente.id, "${cliente.nombre} ${cliente.apellido}".trim(),
                cliente.telefono.trim(), fecha, lineas.map { it.first }, subtotal, delivery, pago.monto, pago.monto,
                BigDecimal.ZERO.setScale(2), direccion.trim(), metodo, pagoId = pagoId, estadoPago = "PENDIENTE",
                numeroOperacion = pago.numeroOperacion, modalidadPago = "SIMULADO")
            val intento = IntentoPagoDigital(pago, borrador, ruta)
            conservarDigital(intento, token) // Antes de cualquier escritura remota.
            val doc = obtenerOCrearPago(pagoService, intento)
            coroutineContext.ensureActive()
            conservarDigital(intento.copy(pago = doc.aPagoDelUsuario(cliente.id)), token)
        }
    }

    fun comprobarPagoDigital(usuario: Usuario?) {
        ejecutarPago(usuario) { token ->
            val intento = requireNotNull(intentoDigital) { "No hay un pago pendiente." }
            val doc = obtenerOCrearPago(pagoService, intento)
            coroutineContext.ensureActive()
            conservarDigital(intento.copy(pago = doc.aPagoDelUsuario(intento.pago.usuarioId)), token)
        }
    }

    fun simularResultadoPago(usuario: Usuario?, resultado: String) {
        ejecutarPago(usuario) { token ->
            val intento = requireNotNull(intentoDigital) { "No hay un pago pendiente." }
            val documento = resolverResultadoPago(pagoService, intento, resultado)
            coroutineContext.ensureActive()
            conservarDigital(intento.copy(pago = documento.aPagoDelUsuario(intento.pago.usuarioId)), token)
        }
    }

    fun descartarPagoRechazado(usuario: Usuario?) {
        ejecutarPago(usuario) { token ->
            val intento = requireNotNull(intentoDigital)
            val documento = pagoService.mostrarPago(intento.pago.id)
            validarDocumentoDelIntento(documento, intento)
            require(documento.aPagoDelUsuario(intento.pago.usuarioId).estadoPago == "RECHAZADO") {
                "Solo se puede dejar atrás un pago rechazado de forma definitiva."
            }
            coroutineContext.ensureActive()
            check(recuperacionPago.vigente(token) && recuperacionPago.descartar()) { "No se pudo retirar el intento local." }
            intentoDigital = null
        }
    }

    fun registrarPedidoDigital(usuario: Usuario?, carrito: CarritoViewModel, productos: ProductoViewModel) {
        ejecutarPago(usuario) { token ->
            var intento = requireNotNull(intentoDigital) { "No hay un pago digital." }
            val docPago = pagoService.mostrarPago(intento.pago.id)
            validarDocumentoDelIntento(docPago, intento)
            val pago = docPago.aPagoDelUsuario(intento.pago.usuarioId)
            // RNO02 se aplica incluso si el callback se invoca sin pasar por la interfaz.
            require(pago.estadoPago == "CONFIRMADO") { "El pago debe estar CONFIRMADO para registrar el pedido." }
            intento = intento.copy(pago = pago)
            conservarDigital(intento, token)
            val pedido = intento.pedido.copy(estadoPago = "CONFIRMADO")
            if (pago.estadoRegistroPedido == "REGISTRADO") {
                val real = recuperarPedidoDigitalRegistrado(service, intento)
                finalizarDigital(intento, real, carrito, productos, token)
                return@ejecutarPago
            }
            // Un commit anterior puede haber llegado tarde; no se genera otro pago ni otro pedido.
            validarPagoParaPedido(docPago, pedido)
            val docs = pedido.productos.map {
                productos.consultarProducto(it.productoId).also { coroutineContext.ensureActive() }
            }
            val commit = prepararCommitDigital(docPago, pedido, docs, intento.ruta)
            conservarDigital(intento.copy(commit = commit), token)
            coroutineContext.ensureActive()
            service.confirmarPedido(commit)
            coroutineContext.ensureActive()
            finalizarDigital(intento, pedido, carrito, productos, token)
        }
    }

    private fun finalizarDigital(intento: IntentoPagoDigital, pedido: Pedido, carrito: CarritoViewModel,
        productos: ProductoViewModel, token: Long) {
        check(recuperacionPago.vigente(token)) { "La sesión cambió." }
        pedidoConfirmado = pedido
        conservarDigital(intento.copy(pago = intento.pago.copy(estadoRegistroPedido = "REGISTRADO"),
            pedidoRegistrado = true), token)
        check(carrito.limpiarCarrito()) { "Pedido registrado. Reintenta únicamente la limpieza local." }
        check(recuperacionPago.descartar()) { "Pedido registrado y carrito vacío. Falta retirar el intento local." }
        intentoDigital = null
        productos.cargarProductos()
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

/** Borrador inmutable de una única compra; jamás contiene PAN, CVV ni PIN. */
internal data class IntentoPagoDigital(
    val pago: Pago, val pedido: Pedido, val ruta: String,
    val commit: PedidoCommit? = null, val pedidoRegistrado: Boolean = false
)

internal fun validarIntentoDigital(intento: IntentoPagoDigital) {
    val pago = intento.pago
    val pedido = intento.pedido
    pago.validar()
    pedido.aDocumento("local").aPedido()
    require(Regex("projects/[^/]+/databases/[^/]+/documents").matches(intento.ruta)) { "Ruta de compra inválida." }
    require(pedido.id == pago.pedidoId && pedido.usuarioId == pago.usuarioId && pedido.pagoId == pago.id &&
        pedido.total == pago.monto && pedido.montoPagado == pago.monto && pedido.vuelto.signum() == 0 &&
        pedido.metodoPago == pago.metodoPago && pedido.numeroOperacion == pago.numeroOperacion &&
        pedido.modalidadPago == "SIMULADO") { "El intento digital no corresponde al pago." }
    require(pedido.productos.map { it.productoId }.distinct().size == pedido.productos.size &&
        pedido.productos.all { it.precioUnitario.signum() > 0 && it.subtotal == it.precioUnitario * BigDecimal(it.cantidad) } &&
        pedido.productos.fold(BigDecimal.ZERO.setScale(2)) { suma, item -> suma + item.subtotal } == pedido.subtotal &&
        pedido.total == pedido.subtotal + pedido.delivery) { "Importes de compra inconsistentes." }
    if (intento.pedidoRegistrado) require(pago.estadoRegistroPedido == "REGISTRADO" && pago.estadoPago == "CONFIRMADO")
}

/** Misma estrategia de preferencias de RF22, con claves separadas y bloqueo del intento digital. */
internal class RecuperacionPago(private val leer: (String) -> String?,
    private val guardar: (String, String) -> Boolean, private val eliminar: (String) -> Boolean) {
    private val gson = Gson()
    private var usuarioId: String? = null
    private var generacion = 0L
    private var ocupada = false
    var intento: IntentoPagoDigital? = null
        private set

    fun actualizarUsuario(id: String?): Boolean {
        val nuevo = id?.takeIf { it.isNotBlank() }
        if (usuarioId == nuevo) return false
        generacion++
        ocupada = false
        usuarioId = nuevo
        intento = null
        if (nuevo != null) {
            val json = leer("digital_$nuevo") ?: return true
            try {
                val propietario = JsonParser().parse(json).asJsonObject.getAsJsonObject("pago").get("usuarioId").asString
                if (propietario != nuevo) return true // Nunca interpretar ni eliminar el intento de otro usuario.
                val recuperado = requireNotNull(gson.fromJson(json, IntentoPagoDigital::class.java))
                validarIntentoDigital(recuperado)
                intento = recuperado
            } catch (_: Exception) { eliminar("digital_$nuevo") }
        }
        return true
    }

    fun iniciarOperacion(): Long? {
        if (usuarioId == null || ocupada) return null
        ocupada = true
        return generacion
    }
    fun vigente(token: Long): Boolean = usuarioId != null && generacion == token
    fun finalizarOperacion(token: Long) { if (vigente(token)) ocupada = false }
    fun guardar(valor: IntentoPagoDigital): Boolean {
        val id = usuarioId ?: return false
        if (valor.pago.usuarioId != id || valor.pedido.usuarioId != id) return false
        validarIntentoDigital(valor)
        if (!guardar("digital_$id", gson.toJson(valor))) return false
        intento = valor
        return true
    }
    fun descartar(): Boolean {
        val id = usuarioId ?: return false
        if (!eliminar("digital_$id")) return false
        intento = null
        return true
    }
}

internal fun validarDocumentoDelIntento(documento: PagoDocumento, intento: IntentoPagoDigital): Pago {
    val pago = documento.aPagoDelUsuario(intento.pago.usuarioId)
    require(documento.name == "${intento.ruta}/pagos/${intento.pago.id}" && pago.mismoIntento(intento.pago)) {
        "La respuesta no corresponde al mismo intento de pago."
    }
    if (intento.pago.estadoPago != "PENDIENTE") require(pago.estadoPago == intento.pago.estadoPago) {
        "El resultado financiero cambió de forma inesperada."
    }
    if (intento.pago.estadoRegistroPedido == "REGISTRADO") require(pago.estadoRegistroPedido == "REGISTRADO") {
        "El registro del pedido cambió de forma inesperada."
    }
    return pago
}

internal fun validarTarjetaParaPago(documento: TarjetaDocumento, usuarioId: String, tarjetaId: String,
    hoy: java.time.YearMonth = java.time.YearMonth.now()): Tarjeta {
    val tarjeta = documento.aTarjetaDelUsuario(usuarioId)
    require(tarjeta.id == tarjetaId && !tarjeta.estaVencida(hoy)) { "La tarjeta está vencida o no corresponde." }
    return tarjeta
}

internal suspend fun recuperarPedidoDigitalRegistrado(service: PedidoService, intento: IntentoPagoDigital): Pedido {
    require(intento.pago.estadoPago == "CONFIRMADO" && intento.pago.estadoRegistroPedido == "REGISTRADO") {
        "El pago no acredita un pedido registrado."
    }
    val pedido = intento.pedido.copy(estadoPago = "CONFIRMADO")
    val real = service.mostrarPedido(pedido.id).aPedidoDelUsuario(pedido.usuarioId)
    currentCoroutineContext().ensureActive()
    require(real.copy(estado = pedido.estado) == pedido) { "El pedido guardado no coincide con el intento." }
    return real
}

internal suspend fun obtenerOCrearPago(service: PagoService, intento: IntentoPagoDigital): PagoDocumento {
    val documento = try { service.mostrarPago(intento.pago.id) }
    catch (e: HttpException) {
        if (e.code() != 404) throw e
        require(intento.pago.estadoPago == "PENDIENTE") { "No se encontró el pago ya resuelto; no se creará otro." }
        currentCoroutineContext().ensureActive()
        try { service.crearPago(intento.pago.aFirestore(), intento.pago.id) }
        catch (conflicto: HttpException) {
            if (conflicto.code() != 409) throw conflicto
            service.mostrarPago(intento.pago.id)
        }
    }
    currentCoroutineContext().ensureActive()
    validarDocumentoDelIntento(documento, intento)
    return documento
}

internal suspend fun resolverResultadoPago(service: PagoService, intento: IntentoPagoDigital, resultado: String): PagoDocumento {
    require(resultado in listOf("CONFIRMADO", "RECHAZADO")) { "Resultado simulado inválido." }
    val documento = obtenerOCrearPago(service, intento)
    val actual = documento.aPagoDelUsuario(intento.pago.usuarioId)
    if (actual.estadoPago != "PENDIENTE") return documento // Los resultados terminales no se vuelven a ejecutar.
    val version = requireNotNull(documento.updateTime?.takeIf { it.isNotBlank() }) { "Falta la versión del pago." }
    val cuerpo = PagoContainer(mapOf("estadoPago" to FirestoreValor(stringValue = resultado),
        "fechaResultado" to FirestoreValor(timestampValue = Instant.now().toString())))
    currentCoroutineContext().ensureActive()
    val respuesta = service.actualizarResultado(actual.id, cuerpo, version)
    currentCoroutineContext().ensureActive()
    val confirmado = validarDocumentoDelIntento(respuesta, intento)
    require(confirmado.estadoPago == resultado) { "La respuesta no confirma el resultado solicitado." }
    return respuesta
}

/** Mismo commit que RF22 más el consumo condicionado del pago. No ejecuta escrituras por separado. */
internal fun prepararCommitDigital(pago: PagoDocumento, pedido: Pedido, productos: List<ProductoDocumento>, ruta: String): PedidoCommit {
    validarPagoParaPedido(pago, pedido)
    require(pago.name == "$ruta/pagos/${pedido.pagoId}") { "Ruta del pago inválida." }
    require(productos.size == pedido.productos.size) { "Inventario incompleto." }
    val stock = pedido.productos.zip(productos).map { (linea, doc) ->
        require(doc.name == "$ruta/productos/${linea.productoId}" && !doc.updateTime.isNullOrBlank()) { "Producto o versión inválidos." }
        val precio = leerPrecioProductoParaCompra(doc)
        require(precio != null && precio.compareTo(linea.precioUnitario) == 0) {
            "El precio cambió después del pago. Conservamos el pago confirmado sin registrar otro pedido."
        }
        prepararDescuentoStockParaCompra(doc, linea.cantidad)
    }
    return PedidoCommit(listOf(PedidoEscritura(pedido.aDocumento(ruta), PedidoPrecondicion(exists = false))) +
        stock + escrituraRegistroPago(pago, pedido))
}
