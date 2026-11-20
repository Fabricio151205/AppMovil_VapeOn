package com.example.vapeon_movil.viewmodels

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.example.vapeon_movil.services.dto.aPedido
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vapeon_movil.entities.DELIVERY_VENTA_ADMIN
import com.example.vapeon_movil.entities.LineaVentaAdmin
import com.example.vapeon_movil.entities.METODOS_PAGO_VENTA_ADMIN
import com.example.vapeon_movil.entities.Pedido
import com.example.vapeon_movil.entities.Producto
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.entities.construirPedidoVentaAdmin
import com.example.vapeon_movil.entities.errorVentaAdmin
import com.example.vapeon_movil.entities.subtotalVentaAdmin
import com.example.vapeon_movil.entities.totalVentaAdmin
import com.example.vapeon_movil.services.PedidoService
import com.example.vapeon_movil.services.dto.PedidoCommit
import com.example.vapeon_movil.services.dto.PedidoEscritura
import com.example.vapeon_movil.services.dto.PedidoPrecondicion
import com.example.vapeon_movil.services.dto.aDocumento
import com.example.vapeon_movil.utils.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.math.BigDecimal
import java.time.Instant
import java.util.Locale
import java.util.UUID

/**
 * Venta creada desde el panel administrativo.
 *
 * Reutiliza el mismo commit atómico del checkout: el pedido y el descuento de
 * stock se escriben juntos o no se escribe nada. La venta se guarda en
 * `pedidos` con `origen = ADMIN` para que aparezca en el seguimiento y en Caja.
 *
 * Regla de negocio:
 *  - `conDelivery = false`: venta en tienda, nace `ENTREGADO` y cuenta en Caja.
 *  - `conDelivery = true`: entrega programada, nace `REGISTRADO`.
 */
class NuevaVentaViewModel(
    private val pedidoService: PedidoService = RetrofitClient.retrofit.create(PedidoService::class.java),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
) : ViewModel() {

    var adminId by mutableStateOf<String?>(null)
        private set
    var lineas by mutableStateOf<List<LineaVentaAdmin>>(emptyList())
        private set
    var conDelivery by mutableStateOf(false)
        private set
    var nombreCliente by mutableStateOf("")
        private set
    var telefono by mutableStateOf("")
        private set
    var direccion by mutableStateOf("")
        private set
    var montoRecibido by mutableStateOf("")
        private set
    var metodoPago by mutableStateOf("EFECTIVO")
        private set
    /** Datos de tarjeta solo para la simulación: nunca se envían completos a Firestore. */
    var titularTarjeta by mutableStateOf("")
        private set
    var ultimos4Tarjeta by mutableStateOf("")
        private set
    var vencimientoTarjeta by mutableStateOf("")
        private set
    var guardando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var mensaje by mutableStateOf<String?>(null)
        private set
    /** Se conserva el pedido recién creado para mostrar la confirmación. */
    var ventaRegistrada by mutableStateOf<Pedido?>(null)
        private set

    private var operacion: Job? = null
    private var preferencias: SharedPreferences? = null
    private val gson = Gson()
    private data class IntentoVenta(val pedido: Pedido, val commit: PedidoCommit)
    private var intentoPendiente: IntentoVenta? = null
    var requiereComprobacion by mutableStateOf(false)
        private set

    fun configurarPersistencia(context: Context) {
        if (preferencias == null) {
            preferencias = context.applicationContext.getSharedPreferences("venta_admin_pendiente", Context.MODE_PRIVATE)
        }
    }

    private fun guardarIntento(intento: IntentoVenta) {
        check(preferencias?.edit()?.putString(requireNotNull(adminId), gson.toJson(intento))?.commit() != false) {
            "No se pudo guardar el intento antes de enviarlo."
        }
        intentoPendiente = intento
        requiereComprobacion = true
    }

    private fun resolverIntento() {
        check(preferencias?.edit()?.remove(requireNotNull(adminId))?.commit() != false) {
            "No se pudo limpiar el intento. Vuelve a comprobar la venta."
        }
        intentoPendiente = null
        requiereComprobacion = false
    }

    private suspend fun enviarORecuperar(intento: IntentoVenta): Pedido {
        val remoto = try {
            pedidoService.mostrarPedido(intento.pedido.id)
        } catch (e: HttpException) {
            if (e.code() != 404) throw e
            null
        }
        if (remoto != null) {
            val esperado = intento.pedido.aDocumento(remoto.name.substringBeforeLast("/pedidos/"))
            check(remoto.fields == esperado.fields) { "La venta guardada requiere revisión. Conservamos el intento." }
            return remoto.aPedido()
        }
        try {
            pedidoService.confirmarPedido(intento.commit)
        } catch (e: HttpException) {
            if (e.code() in listOf(409, 412)) {
                // Otro envío del mismo intento puede haber terminado antes de este rechazo.
                val confirmado = try { pedidoService.mostrarPedido(intento.pedido.id) }
                    catch (consulta: HttpException) {
                        if (consulta.code() != 404) throw consulta
                        resolverIntento()
                        throw e
                    }
                check(confirmado.fields == intento.pedido.aDocumento(
                    confirmado.name.substringBeforeLast("/pedidos/")).fields) {
                    "La venta guardada requiere revisión. Conservamos el intento."
                }
                return confirmado.aPedido()
            }
            if (e.code() in listOf(400, 403, 404)) resolverIntento()
            throw e
        }
        return intento.pedido
    }

    val subtotal: BigDecimal get() = subtotalVentaAdmin(lineas)
    val delivery: BigDecimal get() = if (conDelivery) DELIVERY_VENTA_ADMIN else BigDecimal.ZERO.setScale(2)
    val total: BigDecimal get() = totalVentaAdmin(lineas, conDelivery)

    /** Reinicia el estado cuando cambia el administrador de la sesión. */
    fun actualizarAdmin(usuario: Usuario?) {
        val id = usuario?.takeIf { it.rol.equals("ADMIN", true) && it.id.isNotBlank() }?.id
        if (id == adminId) return
        operacion?.cancel()
        adminId = id
        limpiar()
        intentoPendiente = null
        requiereComprobacion = false
        if (id != null) {
            val guardado = preferencias?.getString(id, null)
            if (guardado != null) {
                requiereComprobacion = true
                try {
                    val intento = gson.fromJson(guardado, IntentoVenta::class.java)
                    require(intento.pedido.registradoPor == id && intento.pedido.usuarioId == id)
                    require(intento.pedido.origen == "ADMIN" && intento.commit.writes.isNotEmpty())
                    intentoPendiente = intento
                    val pedido = intento.pedido
                    nombreCliente = pedido.nombreCliente
                    telefono = pedido.telefono
                    direccion = pedido.direccionEntrega
                    montoRecibido = pedido.montoPagado.toPlainString()
                    metodoPago = pedido.metodoPago
                    conDelivery = pedido.conDelivery == true
                    lineas = pedido.productos.map {
                        LineaVentaAdmin(it.productoId, it.nombre, it.marca, it.precioUnitario,
                            it.cantidad, it.sabor, it.categoria)
                    }
                } catch (_: Exception) {
                    error = "No se pudo recuperar una venta pendiente. Revisa los pedidos antes de continuar."
                }
            }
        }
    }

    fun limpiar() {
        lineas = emptyList()
        conDelivery = false
        nombreCliente = ""
        telefono = ""
        direccion = ""
        montoRecibido = ""
        metodoPago = "EFECTIVO"
        titularTarjeta = ""
        ultimos4Tarjeta = ""
        vencimientoTarjeta = ""
        guardando = false
        error = null
        mensaje = null
        ventaRegistrada = null
    }

    fun actualizarNombreCliente(valor: String) {
        if (guardando || requiereComprobacion) return
        nombreCliente = valor
        error = null
    }

    fun actualizarTelefono(valor: String) {
        if (guardando || requiereComprobacion) return
        telefono = valor
        error = null
    }

    fun actualizarDireccion(valor: String) {
        if (guardando || requiereComprobacion) return
        direccion = valor
        error = null
    }

    fun actualizarMontoRecibido(valor: String) {
        if (guardando || requiereComprobacion) return
        montoRecibido = valor
        error = null
    }

    fun actualizarMetodoPago(valor: String) {
        if (guardando || requiereComprobacion) return
        if (valor !in METODOS_PAGO_VENTA) return
        metodoPago = valor
        error = null
    }

    fun actualizarTitularTarjeta(valor: String) {
        if (guardando || requiereComprobacion) return
        titularTarjeta = valor.take(80)
        error = null
    }

    fun actualizarUltimos4Tarjeta(valor: String) {
        if (guardando || requiereComprobacion) return
        ultimos4Tarjeta = valor.filter { it.isDigit() }.take(4)
        error = null
    }

    /** Mantiene el formato `MM/AA` mientras se escribe. */
    fun actualizarVencimientoTarjeta(valor: String) {
        if (guardando || requiereComprobacion) return
        val digitos = valor.filter { it.isDigit() }.take(4)
        vencimientoTarjeta = if (digitos.length <= 2) digitos
        else "${digitos.take(2)}/${digitos.drop(2)}"
        error = null
    }

    fun actualizarConDelivery(valor: Boolean) {
        if (guardando || requiereComprobacion) return
        if (conDelivery == valor) return
        conDelivery = valor
        if (!valor) direccion = ""
        error = null
    }

    /**
     * Agrega una unidad (o la cantidad indicada) del producto. Si el producto
     * usa sabores, [sabor] debe coincidir con una de sus claves.
     */
    fun agregarProducto(producto: Producto, sabor: String? = null, cantidad: Int = 1) {
        if (guardando || requiereComprobacion) return
        if (cantidad <= 0) {
            error = "La cantidad debe ser mayor que cero."
            return
        }
        if (producto.id.isBlank() || '/' in producto.id) {
            error = "El producto no tiene un identificador válido."
            return
        }
        val saborCanonico = when {
            producto.stockPorSabor.isNotEmpty() -> {
                val solicitado = sabor?.trim().orEmpty()
                val encontrado = producto.stockPorSabor.keys.firstOrNull {
                    it.equals(solicitado, ignoreCase = true)
                }
                if (encontrado == null) {
                    error = "Selecciona un sabor para ${producto.nombre}."
                    return
                }
                encontrado
            }
            else -> null
        }
        val disponible = if (saborCanonico != null) {
            producto.stockPorSabor[saborCanonico] ?: 0
        } else {
            producto.stock
        }
        if (disponible <= 0) {
            error = "${producto.nombre} no tiene stock disponible."
            return
        }
        val precio = Pedido.leerMonto(
            BigDecimal.valueOf(producto.precio).stripTrailingZeros().toPlainString()
        )
        if (precio == null || precio.signum() <= 0) {
            error = "El producto ${producto.nombre} no tiene un precio válido."
            return
        }
        val clave = producto.id to saborCanonico?.trim()?.lowercase(Locale.ROOT).orEmpty()
        val existente = lineas.firstOrNull { it.clave == clave }
        val nuevaCantidad = (existente?.cantidad ?: 0) + cantidad
        if (nuevaCantidad > disponible) {
            error = "Solo hay $disponible unidad(es) de ${producto.nombre}${saborCanonico?.let { " ($it)" }.orEmpty()}."
            return
        }
        val linea = LineaVentaAdmin(
            productoId = producto.id,
            nombre = producto.nombre.trim(),
            marca = producto.marca.trim(),
            precioUnitario = precio,
            cantidad = nuevaCantidad,
            sabor = saborCanonico,
            categoria = producto.categoria.trim().takeIf { it.isNotBlank() },
            stockDisponible = disponible
        )
        lineas = if (existente == null) {
            lineas + linea
        } else {
            lineas.map { if (it.clave == clave) linea else it }
        }
        error = null
    }

    /** Suma o resta una unidad; al llegar a cero la línea se elimina. */
    fun cambiarCantidad(linea: LineaVentaAdmin, delta: Int) {
        if (guardando || requiereComprobacion) return
        val actual = lineas.firstOrNull { it.clave == linea.clave } ?: return
        val nueva = actual.cantidad + delta
        when {
            nueva <= 0 -> lineas = lineas.filterNot { it.clave == linea.clave }
            nueva > actual.stockDisponible -> {
                error = "No hay más stock disponible para ${actual.nombre}."
                return
            }
            else -> lineas = lineas.map {
                if (it.clave == linea.clave) it.copy(cantidad = nueva) else it
            }
        }
        error = null
    }

    fun quitarLinea(linea: LineaVentaAdmin) {
        if (guardando || requiereComprobacion) return
        lineas = lineas.filterNot { it.clave == linea.clave }
        error = null
    }

    fun consumirVentaRegistrada() {
        ventaRegistrada = null
    }

    fun limpiarError() {
        error = null
    }

    fun consumirMensaje() {
        mensaje = null
    }

    /**
     * Relee cada producto para validar precio y stock vigentes, construye el
     * pedido y envía un único commit con el pedido y los descuentos de stock.
     */
    fun confirmar(productos: ProductoViewModel) {
        if (guardando) return
        if (requiereComprobacion && intentoPendiente == null) return
        val admin = adminId
        if (admin == null) {
            error = "Solo un administrador puede registrar una venta."
            return
        }
        val lineasActuales = lineas
        val errorFormulario = errorVentaAdmin(
            nombreCliente = nombreCliente,
            telefono = telefono,
            conDelivery = conDelivery,
            direccion = direccion,
            lineas = lineasActuales,
            montoTexto = montoRecibido,
            metodoPago = metodoPago,
            titularTarjeta = titularTarjeta,
            ultimos4Tarjeta = ultimos4Tarjeta,
            vencimientoTarjeta = vencimientoTarjeta
        )
        if (intentoPendiente == null && errorFormulario != null) {
            error = errorFormulario
            return
        }
        guardando = true
        error = null
        mensaje = null
        operacion = viewModelScope.launch(dispatcher) {
            try {
                val preparado = intentoPendiente ?: run {
                    val escriturasStock = mutableListOf<PedidoEscritura>()
                    val lineasFrescas = mutableListOf<LineaVentaAdmin>()
                    var ruta: String? = null

                    for ((productoId, grupo) in lineasActuales.groupBy { it.productoId }) {
                        val doc = productos.consultarProducto(productoId)
                        coroutineContext.ensureActive()
                        require(doc.name.substringAfterLast('/') == productoId && !doc.updateTime.isNullOrBlank()) {
                            "No se pudo verificar el producto."
                        }
                        val rutaDocumento = doc.name.substringBeforeLast("/productos/")
                        require(ruta == null || ruta == rutaDocumento) { "Inventario inconsistente." }
                        ruta = rutaDocumento

                        val precio = leerPrecioProductoParaCompra(doc)
                        require(precio != null && precio.signum() > 0) {
                            "Precio inválido para ${grupo.first().nombre}."
                        }
                        require(grupo.all { precio.compareTo(it.precioUnitario) == 0 }) {
                            "El precio de ${grupo.first().nombre} cambió. Elimínalo y agrégalo nuevamente."
                        }
                        val nombre = doc.fields.nombre?.asString().orEmpty()
                        val marca = doc.fields.marca?.asString().orEmpty()
                        require(nombre.isNotBlank() && marca.isNotBlank()) {
                            "Datos incompletos para ${grupo.first().nombre}."
                        }
                        val categoria = doc.fields.categoria?.stringValue?.trim()?.takeIf { it.isNotBlank() }
                        val descuento = prepararDescuentoStockPorLineas(
                            doc, grupo.map { SolicitudStockCompra(it.sabor, it.cantidad) }
                        )
                        grupo.zip(descuento.saboresCanonicos).forEach { (linea, sabor) ->
                            lineasFrescas += linea.copy(
                                nombre = nombre,
                                marca = marca,
                                precioUnitario = precio,
                                sabor = sabor,
                                categoria = categoria
                            )
                        }
                        escriturasStock += descuento.escritura
                    }
                    require(lineasFrescas.size == lineasActuales.size) { "Líneas de venta inconsistentes." }

                    val pedido = construirPedidoVentaAdmin(
                        id = UUID.randomUUID().toString(),
                        adminId = admin,
                        nombreCliente = nombreCliente,
                        telefono = telefono,
                        fecha = Instant.now().toString(),
                        lineas = lineasFrescas,
                        conDelivery = conDelivery,
                        direccionEntrega = direccion,
                        montoRecibido = montoRecibido.takeIf { it.isNotBlank() }?.let { texto ->
                            Pedido.leerMonto(texto)
                                ?: throw IllegalArgumentException(
                                    "Ingresa un monto recibido válido con máximo dos decimales."
                                )
                        },
                        metodoPago = metodoPago,
                        titularTarjeta = titularTarjeta,
                        ultimos4Tarjeta = ultimos4Tarjeta,
                        vencimientoTarjeta = vencimientoTarjeta
                    )
                    val commit = PedidoCommit(
                        listOf(
                            PedidoEscritura(
                                pedido.aDocumento(requireNotNull(ruta)),
                                PedidoPrecondicion(exists = false)
                            )
                        ) + escriturasStock
                    )
                    IntentoVenta(pedido, commit).also { guardarIntento(it) }
                }
                val pedido = enviarORecuperar(preparado)
                coroutineContext.ensureActive()
                resolverIntento()
                productos.cargarProductos()

                ventaRegistrada = pedido
                lineas = emptyList()
                conDelivery = false
                nombreCliente = ""
                telefono = ""
                direccion = ""
                montoRecibido = ""
                metodoPago = "EFECTIVO"
                titularTarjeta = ""
                ultimos4Tarjeta = ""
                vencimientoTarjeta = ""
                mensaje = if (pedido.estado == "ENTREGADO") {
                    "Venta registrada y sumada a Caja."
                } else {
                    "Venta registrada como pedido con delivery."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = when (e) {
                    is HttpException -> when (e.code()) {
                        403 -> "Firestore rechazó la venta. Revisa los permisos."
                        404 -> "Un producto ya no está disponible."
                        409, 412 -> "El inventario cambió. Revisa el stock y vuelve a confirmar."
                        else -> "Firestore rechazó los datos de la venta."
                    }
                    is IllegalArgumentException, is IllegalStateException ->
                        e.message ?: "Datos de la venta inválidos."
                    else -> "No se pudo registrar la venta. Comprueba la conexión y reintenta."
                }
            } finally {
                if (coroutineContext[Job]?.isCancelled != true) guardando = false
            }
        }
    }

    companion object {
        val METODOS_PAGO_VENTA = METODOS_PAGO_VENTA_ADMIN
    }
}
