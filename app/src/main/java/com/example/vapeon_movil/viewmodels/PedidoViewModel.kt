package com.example.vapeon_movil.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import com.google.gson.Gson

class PedidoViewModel(application: Application) : AndroidViewModel(application) {
    private data class Intento(val pedido: Pedido, val commit: PedidoCommit)
    private val prefs = application.getSharedPreferences("vapeon_checkout", Context.MODE_PRIVATE)
    private val gson = Gson()
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

    init {
        // Conserva el ID incluso si se cierra la app durante la petición.
        prefs.getString("intento", null)?.let { json ->
            try {
                val intento = gson.fromJson(json, Intento::class.java)
                pedidoPendiente = intento.pedido
                commitPendiente = intento.commit
                requiereComprobacion = true
            } catch (e: Exception) {
                error = "No se pudo leer la compra pendiente. Revisa el pedido en Firebase antes de comprar."
                requiereComprobacion = true
            }
        }
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
        if (cargando || pedidoConfirmado != null) return
        cargando = true // Antes de launch para impedir dobles pulsaciones.
        error = null
        val comprobandoIntentoAnterior = requiereComprobacion
        viewModelScope.launch {
            try {
                require(usuario != null && usuario.id.isNotBlank() &&
                    usuario.rol.equals("CLIENTE", true)) { "Inicia sesión como cliente para comprar." }

                // Tras una respuesta perdida solo se comprueba el mismo ID. No se crea otro pedido.
                if (requiereComprobacion) {
                    val pendiente = requireNotNull(pedidoPendiente)
                    require(pendiente.usuarioId == usuario.id) { "El pedido pertenece a otra sesión." }
                    try {
                        val guardado = service.mostrarPedido(pendiente.id)
                        require(guardado.fields["usuarioId"]?.stringValue == usuario.id) {
                            "No se pudo verificar el propietario del pedido."
                        }
                        terminarCompra(pendiente, carrito, productos)
                        return@launch
                    } catch (e: HttpException) {
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
                        val nuevoStock = Pedido.validarStock(doc.fields.stock?.stringValue, item.cantidad)
                        val precio = Pedido.leerMonto(doc.fields.precio?.stringValue.orEmpty())
                        require(precio != null && precio.signum() > 0) { "Precio inválido para ${item.nombre}." }
                        require(precio.compareTo(BigDecimal.valueOf(item.precioUnit)) == 0) {
                            "El precio de ${item.nombre} cambió. Elimínalo del carrito y agrégalo nuevamente desde el catálogo."
                        }
                        val nombre = doc.fields.nombre?.stringValue.orEmpty()
                        val marca = doc.fields.marca?.stringValue.orEmpty()
                        require(nombre.isNotBlank() && marca.isNotBlank() && !doc.updateTime.isNullOrBlank()) {
                            "Datos incompletos para ${item.nombre}."
                        }
                        escriturasStock += PedidoEscritura(
                            PedidoDocumento(doc.name, mapOf("stock" to FirestoreValor(stringValue = nuevoStock.toString()))),
                            PedidoPrecondicion(updateTime = doc.updateTime), PedidoMascara(listOf("stock"))
                        )
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

                check(prefs.edit().putString("intento", gson.toJson(
                    Intento(requireNotNull(pedidoPendiente), requireNotNull(commitPendiente)))).commit()) {
                    "No se pudo conservar el intento de compra en el dispositivo."
                }
                requiereComprobacion = true
                service.confirmarPedido(requireNotNull(commitPendiente))
                terminarCompra(requireNotNull(pedidoPendiente), carrito, productos)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e is HttpException && e.code() in listOf(400, 403, 404, 409, 412)) {
                    // Un commit rechazado no aplica ninguna escritura. Un reintento ambiguo
                    // conserva el ID hasta comprobarlo, incluso si hubo conflicto.
                    if (e.code() == 409 || e.code() == 412) {
                        // Diferencia un conflicto de stock de un pedido ya confirmado.
                        try {
                            val pendiente = requireNotNull(pedidoPendiente)
                            val guardado = service.mostrarPedido(pendiente.id)
                            if (guardado.fields["usuarioId"]?.stringValue == pendiente.usuarioId) {
                                terminarCompra(pendiente, carrito, productos)
                                return@launch
                            }
                        } catch (verificacion: HttpException) {
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
                cargando = false
            }
        }
    }

    private fun terminarCompra(pedido: Pedido, carrito: CarritoViewModel, productos: ProductoViewModel) {
        pedidoConfirmado = pedido
        requiereComprobacion = false
        pedidoPendiente = null
        commitPendiente = null
        carrito.limpiarCarrito()
        prefs.edit().remove("intento").commit()
        productos.cargarProductos()
    }

    private fun descartarIntento() {
        requiereComprobacion = false
        pedidoPendiente = null
        commitPendiente = null
        prefs.edit().remove("intento").commit()
    }
}
