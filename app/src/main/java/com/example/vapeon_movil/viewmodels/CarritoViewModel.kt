package com.example.vapeon_movil.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.vapeon_movil.entities.ItemCarrito
import com.example.vapeon_movil.entities.Producto
import com.example.vapeon_movil.entities.ResumenPago
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * ViewModel del Carrito de Compras — exclusivo para usuarios con rol CLIENTE.
 *
 * Gestiona el estado del carrito con persistencia local mediante [SharedPreferences] y [Gson].
 * El carrito se conserva entre navegaciones y reinicios de la aplicación hasta que se decida pagar
 * o vaciar explícitamente con [limpiarCarrito].
 */
class CarritoViewModel(private val eliminarLocal: ((String) -> Boolean)? = null) : ViewModel() {

    private val gson = Gson()
    private val PREFS_NAME = "vapeon_carrito_prefs"
    private val KEY_ITEMS = "carrito_items"

    private var usuarioId: String? = null
    private val claveItems: String get() = "${KEY_ITEMS}_${usuarioId.orEmpty()}"

    private var appContext: Context? = null
    private var inventario: Map<String, Producto>? = null
    var aviso by mutableStateOf<String?>(null)
        private set
    var limpiezaPendiente by mutableStateOf(false)
        private set
    val puedeComprar: Boolean get() = !limpiezaPendiente && items.isNotEmpty() && items.all {
        it.stockDisponible != null && it.cantidad in 1..it.stockDisponible
    }

    fun actualizarInventario(productos: List<Producto>) {
        inventario = productos.associateBy { it.id }
        val anteriores = items
        items = items.mapNotNull { item ->
            val producto = inventario?.get(item.productoId)
            if (producto == null || producto.stock <= 0) null
            else item.copy(cantidad = item.cantidad.coerceIn(1, producto.stock), stockDisponible = producto.stock)
        }
        if (items != anteriores) {
            aviso = "Se revisó el inventario: las cantidades se ajustaron al stock y los productos no disponibles se retiraron."
            guardarCarrito()
        }
    }

    internal fun restaurarItems(json: String) {
        try {
            val tipo = object : TypeToken<List<ItemCarrito>>() {}.type
            val guardados: List<ItemCarrito>? = gson.fromJson(json, tipo)
            val validos = guardados.orEmpty().mapNotNull { item ->
                runCatching {
                    if (item.productoId.isBlank() || item.nombre.isBlank() || !item.precioUnit.isFinite() || item.precioUnit <= 0 ||
                        item.stockDisponible?.let { it <= 0 } == true) null
                    else item.copy(cantidad = item.cantidad.coerceAtLeast(1).let { cantidad ->
                        item.stockDisponible?.let { cantidad.coerceAtMost(it) } ?: cantidad
                    })
                }.getOrNull()
            }.distinctBy { it.productoId }
            items = validos
            if (guardados != validos) aviso = "Se corrigieron productos o cantidades inválidas del carrito guardado."
            else if (validos.any { it.stockDisponible == null }) aviso = "Vuelve al catálogo para comprobar el stock de los productos guardados."
        } catch (_: Exception) {
            items = emptyList()
            aviso = "No se pudo leer el carrito guardado. Puedes volver a agregar los productos."
        }
    }

    // ─── Estado observable ─────────────────────────────────────────────────

    /** Lista de ítems en el carrito */
    var items by mutableStateOf<List<ItemCarrito>>(emptyList())
        private set

    /** Resumen financiero calculado reactivamente */
    val resumen: ResumenPago
        get() = ResumenPago.calcular(items)

    /** Cantidad total de unidades en el carrito */
    val cantidadTotal: Int
        get() = items.sumOf { it.cantidad }

    // ─── Persistencia ───────────────────────────────────────────────────────

    /**
     * Carga la lista de ítems guardados en SharedPreferences.
     */
    fun cargarCarrito(context: Context, usuarioId: String? = null) {
        if (this.usuarioId != usuarioId) inventario = null
        this.usuarioId = usuarioId
        items = emptyList()
        aviso = null
        limpiezaPendiente = false
        if (usuarioId.isNullOrBlank()) return
        appContext = context.applicationContext
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(claveItems, null)
            if (!json.isNullOrEmpty()) {
                restaurarItems(json)
                guardarCarrito()
                inventario?.let { actualizarInventario(it.values.toList()) }
            }
        } catch (e: Exception) {
            println("Error al cargar carrito persistente: ${e.localizedMessage}")
        }
    }

    /**
     * Guarda el estado actual de los ítems en SharedPreferences.
     */
    private fun guardarCarrito(context: Context? = appContext) {
        val targetContext = context ?: appContext ?: return
        try {
            val prefs = targetContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = gson.toJson(items)
            if (!prefs.edit().putString(claveItems, json).commit())
                aviso = "No se pudo guardar el carrito en el dispositivo. Conservamos los cambios en pantalla."
        } catch (e: Exception) {
            aviso = "No se pudo guardar el carrito en el dispositivo. Conservamos los cambios en pantalla."
        }
    }

    // ─── Acciones ──────────────────────────────────────────────────────────

    /**
     * Agrega un producto al carrito.
     * - Si el producto ya existe, incrementa su cantidad en 1.
     * - Si es nuevo, lo agrega con cantidad = 1.
     */
    fun agregarProducto(producto: Producto, context: Context? = null) {
        if (limpiezaPendiente) return
        if (context != null) appContext = context.applicationContext

        val precio = producto.precio
        val existente = items.find { it.productoId == producto.id }
        if (inventario != null && inventario?.containsKey(producto.id) != true) {
            aviso = "El producto ya no está disponible en el inventario."
            return
        }
        val disponible = inventario?.get(producto.id)?.stock ?: producto.stock
        if (disponible <= 0 || (existente?.cantidad ?: 0) >= disponible) {
            aviso = "Sin stock disponible para agregar más unidades de ${producto.nombre}."
            return
        }
        if (!producto.precio.isFinite() || producto.precio <= 0 || producto.id.isBlank()) {
            aviso = "El producto no tiene datos válidos para comprar."
            return
        }

        items = if (existente != null) {
            items.map {
                if (it.productoId == producto.id) it.copy(cantidad = it.cantidad + 1, stockDisponible = disponible)
                else it
            }
        } else {
            items + ItemCarrito(
                productoId = producto.id,
                nombre     = producto.nombre,
                marca      = producto.marca,
                precioUnit = precio,
                stockDisponible = disponible
            )
        }
        guardarCarrito(context)
    }

    /**
     * Incrementa en 1 la cantidad del ítem con el [productoId] dado.
     */
    fun incrementar(productoId: String, context: Context? = null) {
        if (limpiezaPendiente) return
        if (context != null) appContext = context.applicationContext
        val item = items.firstOrNull { it.productoId == productoId } ?: return
        if (item.stockDisponible == null || item.cantidad >= item.stockDisponible) {
            aviso = "No puedes superar el stock conocido. Actualiza el catálogo para comprobar disponibilidad."
            return
        }
        items = items.map {
            if (it.productoId == productoId || cantidadTotal <= cantidadTotal)
                it.copy(cantidad = it.cantidad + 1)
            else it
        }
        guardarCarrito(context)
    }

    /**
     * Decrementa en 1 la cantidad del ítem.
     * Si la cantidad llega a 0, el ítem se elimina del carrito.
     */
    fun decrementar(productoId: String, context: Context? = null) {
        if (context != null) appContext = context.applicationContext
        items = items.mapNotNull {
            when {
                it.productoId != productoId -> it
                it.cantidad > 1             -> it.copy(cantidad = it.cantidad - 1)
                else                        -> it
            }
        }
        guardarCarrito(context)
    }

    /**
     * Elimina completamente un ítem del carrito.
     */
    fun eliminarItem(productoId: String, context: Context? = null) {
        if (context != null) appContext = context.applicationContext
        items = items.filter { it.productoId != productoId }
        guardarCarrito(context)
    }

    /**
     * Vacía el carrito por completo (al pagar o confirmar orden).
     */
    fun limpiarCarrito(context: Context? = null): Boolean {
        val targetContext = context ?: appContext
        items = emptyList()
        val eliminado = try {
            if (eliminarLocal != null) eliminarLocal.invoke(claveItems)
            else if (targetContext != null) {
                val prefs = targetContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().remove(claveItems).commit()
            } else true // Carrito exclusivamente en memoria, sin almacenamiento asociado.
        } catch (_: Exception) { false }
        limpiezaPendiente = !eliminado
        aviso = if (!eliminado) "El pedido ya fue registrado. Falta completar la limpieza local del carrito." else null
        return eliminado
    }
}
