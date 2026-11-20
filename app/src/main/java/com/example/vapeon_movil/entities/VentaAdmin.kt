package com.example.vapeon_movil.entities

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Reglas de negocio de una venta registrada por el administrador.
 *
 * Una venta administrativa se guarda en la misma colección `pedidos` para que
 * aparezca tanto en el seguimiento de pedidos como en Caja. La diferencia es:
 *
 *  - Sin delivery: la venta ocurre en el momento y nace `ENTREGADO`, por lo que
 *    cuenta de inmediato en Caja.
 *  - Con delivery: la venta queda `REGISTRADO` para entregarse después y sigue
 *    el flujo normal de estados del pedido.
 *
 * Este archivo es lógica pura (sin Android ni Firestore) para poder probarla.
 */

const val ORIGEN_VENTA_ADMIN = "ADMIN"
const val ESTADO_VENTA_INMEDIATA = "ENTREGADO"
const val ESTADO_VENTA_DELIVERY = "REGISTRADO"

/** Costo fijo de delivery usado por la app; se replica en la venta administrativa. */
val DELIVERY_VENTA_ADMIN: BigDecimal = BigDecimal("5.00")

/** Métodos admitidos en una venta registrada desde el panel. */
const val METODO_PAGO_EFECTIVO = "EFECTIVO"
const val METODO_PAGO_YAPE = "YAPE"
const val METODO_PAGO_TARJETA = "TARJETA"

val METODOS_PAGO_VENTA_ADMIN = listOf(METODO_PAGO_EFECTIVO, METODO_PAGO_YAPE, METODO_PAGO_TARJETA)

/** Línea de una venta administrativa; conserva el snapshot que se guardará en el pedido. */
data class LineaVentaAdmin(
    val productoId: String,
    val nombre: String,
    val marca: String,
    val precioUnitario: BigDecimal,
    val cantidad: Int,
    val sabor: String? = null,
    val categoria: String? = null,
    /** Stock conocido en la sesión; solo se usa para limitar la interfaz. */
    val stockDisponible: Int = Int.MAX_VALUE
) {
    val subtotal: BigDecimal
        get() = precioUnitario.multiply(BigDecimal(cantidad)).setScale(2, RoundingMode.UNNECESSARY)

    /** Identidad de la línea para impedir duplicados del mismo producto y sabor. */
    val clave: Pair<String, String>
        get() = productoId to sabor?.trim()?.lowercase(Locale.ROOT).orEmpty()
}

/** Estado inicial del pedido según la decisión de entrega del administrador. */
fun estadoVentaAdmin(conDelivery: Boolean): String =
    if (conDelivery) ESTADO_VENTA_DELIVERY else ESTADO_VENTA_INMEDIATA

fun subtotalVentaAdmin(lineas: List<LineaVentaAdmin>): BigDecimal =
    lineas.fold(BigDecimal.ZERO.setScale(2)) { suma, linea -> suma + linea.subtotal }

fun deliveryVentaAdmin(conDelivery: Boolean): BigDecimal =
    if (conDelivery) DELIVERY_VENTA_ADMIN else BigDecimal.ZERO.setScale(2)

fun totalVentaAdmin(lineas: List<LineaVentaAdmin>, conDelivery: Boolean): BigDecimal =
    subtotalVentaAdmin(lineas) + deliveryVentaAdmin(conDelivery)

/** Etiqueta legible para el detalle del pedido y las listas administrativas. */
fun etiquetaOrigenVenta(origen: String?, conDelivery: Boolean?): String = when {
    origen == ORIGEN_VENTA_ADMIN && conDelivery == true -> "Venta admin · Delivery"
    origen == ORIGEN_VENTA_ADMIN -> "Venta admin · Tienda"
    conDelivery == true -> "Delivery"
    else -> "Pedido del cliente"
}

/**
 * Errores por campo del formulario de venta administrativa.
 *
 * La interfaz usa esta estructura para señalar el campo exacto que debe
 * corregirse; [primero] conserva el mensaje único que ya consumía el
 * ViewModel y las pruebas existentes.
 */
data class ErroresVentaAdmin(
    val nombre: String? = null,
    val telefono: String? = null,
    val direccion: String? = null,
    val productos: String? = null,
    val metodoPago: String? = null,
    val titularTarjeta: String? = null,
    val ultimos4Tarjeta: String? = null,
    val vencimientoTarjeta: String? = null,
    val monto: String? = null
) {
    val primero: String?
        get() = nombre ?: telefono ?: direccion ?: productos ?: metodoPago ?:
            titularTarjeta ?: ultimos4Tarjeta ?: vencimientoTarjeta ?: monto

    fun hayErrores(): Boolean = primero != null
}

private val TELEFONO_VENTA = Regex("9[0-9]{8}")
private val ULTIMOS4_TARJETA = Regex("[0-9]{4}")
private val VENCIMIENTO_TARJETA = Regex("([0-9]{1,2})\\s*/\\s*([0-9]{2}|[0-9]{4})")

/** Convierte `MM/AA` o `MM/AAAA` en un mes válido; devuelve `null` si el formato es incorrecto. */
internal fun leerVencimientoTarjeta(texto: String): java.time.YearMonth? {
    val coincidencia = VENCIMIENTO_TARJETA.matchEntire(texto.trim()) ?: return null
    val mes = coincidencia.groupValues[1].toIntOrNull() ?: return null
    val anioTexto = coincidencia.groupValues[2]
    val anio = (if (anioTexto.length == 2) "20$anioTexto" else anioTexto).toIntOrNull() ?: return null
    if (mes !in 1..12 || anio !in 2000..2100) return null
    return java.time.YearMonth.of(anio, mes)
}

private fun contieneControl(texto: String): Boolean = texto.any { it.code < 32 || it.code == 127 }

/**
 * Valida el formulario antes de tocar Firestore. Devuelve el primer error en
 * español listo para mostrar o `null` cuando todo es correcto.
 */
fun errorVentaAdmin(
    nombreCliente: String,
    telefono: String,
    conDelivery: Boolean,
    direccion: String,
    lineas: List<LineaVentaAdmin>,
    montoTexto: String,
    metodoPago: String = METODO_PAGO_EFECTIVO,
    titularTarjeta: String = "",
    ultimos4Tarjeta: String = "",
    vencimientoTarjeta: String = ""
): String? = validarVentaAdmin(
    nombreCliente, telefono, conDelivery, direccion, lineas, montoTexto,
    metodoPago, titularTarjeta, ultimos4Tarjeta, vencimientoTarjeta
).primero

/** Validación completa con detalle por campo para la pantalla de venta. */
fun validarVentaAdmin(
    nombreCliente: String,
    telefono: String,
    conDelivery: Boolean,
    direccion: String,
    lineas: List<LineaVentaAdmin>,
    montoTexto: String,
    metodoPago: String = METODO_PAGO_EFECTIVO,
    titularTarjeta: String = "",
    ultimos4Tarjeta: String = "",
    vencimientoTarjeta: String = ""
): ErroresVentaAdmin {
    val nombre = nombreCliente.trim()
    val nombreError = when {
        nombre.isEmpty() -> "Ingresa el nombre del cliente."
        nombre.length < 2 -> "El nombre debe tener al menos 2 caracteres."
        nombre.length > 100 -> "El nombre admite hasta 100 caracteres."
        contieneControl(nombre) -> "El nombre contiene caracteres no válidos."
        else -> null
    }

    val tel = telefono.trim()
    val telefonoError = when {
        tel.isEmpty() && conDelivery -> "Ingresa un teléfono para coordinar el delivery."
        tel.isEmpty() -> null
        !TELEFONO_VENTA.matches(tel) -> "El teléfono debe tener 9 dígitos y comenzar con 9."
        else -> null
    }

    val dir = direccion.trim()
    val direccionError = when {
        !conDelivery -> null
        dir.isEmpty() -> "Ingresa la dirección de entrega."
        dir.length < 5 -> "La dirección debe tener al menos 5 caracteres."
        dir.length > 200 -> "La dirección admite hasta 200 caracteres."
        contieneControl(dir) -> "La dirección contiene caracteres no válidos."
        else -> null
    }

    val productosError = when {
        lineas.isEmpty() -> "Agrega al menos un producto."
        lineas.any { it.cantidad <= 0 } -> "La cantidad debe ser mayor que cero."
        lineas.any { it.productoId.isBlank() || '/' in it.productoId } ->
            "Hay un producto con un identificador inválido."
        lineas.map { it.clave }.distinct().size != lineas.size ->
            "Hay líneas duplicadas del mismo producto y sabor."
        else -> null
    }

    val metodoError = if (metodoPago !in METODOS_PAGO_VENTA_ADMIN) {
        "Selecciona un método de pago válido."
    } else null

    val titular = titularTarjeta.trim()
    val titularError = when {
        metodoPago != METODO_PAGO_TARJETA -> null
        titular.isEmpty() -> "Ingresa el nombre del titular de la tarjeta."
        titular.length < 3 -> "El titular debe tener al menos 3 caracteres."
        titular.length > 80 -> "El titular admite hasta 80 caracteres."
        contieneControl(titular) -> "El titular contiene caracteres no válidos."
        else -> null
    }
    val ultimos4Error = when {
        metodoPago != METODO_PAGO_TARJETA -> null
        !ULTIMOS4_TARJETA.matches(ultimos4Tarjeta.trim()) ->
            "Ingresa exactamente los últimos 4 dígitos de la tarjeta."
        else -> null
    }
    val vencimiento = leerVencimientoTarjeta(vencimientoTarjeta)
    val vencimientoError = when {
        metodoPago != METODO_PAGO_TARJETA -> null
        vencimiento == null -> "Ingresa el vencimiento en formato MM/AA."
        vencimiento.isBefore(java.time.YearMonth.now()) -> "La tarjeta está vencida."
        else -> null
    }

    val total = totalVentaAdmin(lineas, conDelivery)
    val montoError = if (montoTexto.isNotBlank()) {
        val monto = Pedido.leerMonto(montoTexto)
        when {
            monto == null -> "Ingresa un monto recibido válido con máximo dos decimales."
            monto < total -> "El monto recibido no cubre el total de la venta."
            else -> null
        }
    } else null

    return ErroresVentaAdmin(
        nombre = nombreError,
        telefono = telefonoError,
        direccion = direccionError,
        productos = productosError,
        metodoPago = metodoError,
        titularTarjeta = titularError,
        ultimos4Tarjeta = ultimos4Error,
        vencimientoTarjeta = vencimientoError,
        monto = montoError
    )
}

/**
 * Construye el pedido que se enviará a Firestore. `usuarioId` es el UID del
 * administrador: la venta pertenece al panel y no suplanta la identidad de un
 * cliente. El nombre y teléfono del comprador se guardan como snapshot.
 */
fun construirPedidoVentaAdmin(
    id: String,
    adminId: String,
    nombreCliente: String,
    telefono: String,
    fecha: String,
    lineas: List<LineaVentaAdmin>,
    conDelivery: Boolean,
    direccionEntrega: String,
    montoRecibido: BigDecimal? = null,
    metodoPago: String = METODO_PAGO_EFECTIVO,
    titularTarjeta: String = "",
    ultimos4Tarjeta: String = "",
    vencimientoTarjeta: String = ""
): Pedido {
    require(id.isNotBlank() && '/' !in id) { "ID de venta inválido." }
    require(adminId.isNotBlank() && '/' !in adminId) { "Administrador inválido." }
    require(nombreCliente.isNotBlank()) { "Ingresa el nombre del cliente." }
    require(metodoPago in METODOS_PAGO_VENTA_ADMIN) { "Método de pago inválido." }
    errorVentaAdmin(
        nombreCliente, telefono, conDelivery, direccionEntrega, lineas,
        montoRecibido?.toPlainString().orEmpty(), metodoPago,
        titularTarjeta, ultimos4Tarjeta, vencimientoTarjeta
    )?.let { throw IllegalArgumentException(it) }
    require(lineas.isNotEmpty()) { "Agrega al menos un producto." }
    require(lineas.all { it.cantidad > 0 && it.precioUnitario.signum() > 0 }) {
        "Línea de venta inválida."
    }
    require(lineas.map { it.clave }.distinct().size == lineas.size) {
        "Hay líneas duplicadas del mismo producto y sabor."
    }
    if (conDelivery) require(direccionEntrega.isNotBlank()) { "Ingresa la dirección de entrega." }

    val subtotal = subtotalVentaAdmin(lineas)
    val delivery = deliveryVentaAdmin(conDelivery)
    val total = subtotal + delivery
    val monto = (montoRecibido ?: total).setScale(2, RoundingMode.UNNECESSARY)
    require(monto >= total) { "El monto recibido no cubre el total de la venta." }

    return Pedido(
        id = id,
        usuarioId = adminId,
        nombreCliente = nombreCliente.trim(),
        telefono = telefono.trim(),
        fecha = fecha,
        productos = lineas.map { linea ->
            ProductoPedido(
                productoId = linea.productoId,
                nombre = linea.nombre,
                marca = linea.marca,
                cantidad = linea.cantidad,
                precioUnitario = linea.precioUnitario,
                subtotal = linea.subtotal,
                categoria = linea.categoria?.trim()?.takeIf { it.isNotBlank() },
                sabor = linea.sabor?.trim()?.takeIf { it.isNotBlank() }
            )
        },
        subtotal = subtotal,
        delivery = delivery,
        total = total,
        montoPagado = monto,
        vuelto = monto - total,
        direccionEntrega = if (conDelivery) direccionEntrega.trim() else "Venta en tienda",
        metodoPago = metodoPago,
        estado = estadoVentaAdmin(conDelivery),
        origen = ORIGEN_VENTA_ADMIN,
        conDelivery = conDelivery,
        registradoPor = adminId
    )
}
