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
import java.util.Locale

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
    val puedeComprar: Boolean get() {
        if (limpiezaPendiente || items.isEmpty()) return false
        val actual = inventario
        if (actual == null) return items.all {
            it.stockDisponible != null && it.cantidad in 1..it.stockDisponible
        }
        return items.groupBy { it.productoId }.all { (productoId, lineas) ->
            val producto = actual[productoId] ?: return@all false
            lineas.sumOf { it.cantidad } <= producto.stock && lineas.all { item ->
                val sabor = resolverSabor(producto, item.saborSeleccionado) ?: return@all false
                val disponible = stockDelSabor(producto, sabor)
                item.cantidad > 0 && item.cantidad <= disponible
            }
        }
    }

    fun actualizarInventario(productos: List<Producto>) {
        inventario = productos.associateBy { it.id }
        val anteriores = items
        val usadosPorProducto = mutableMapOf<String, Int>()
        items = items.mapNotNull { item ->
            val producto = inventario?.get(item.productoId)
            val sabor = producto?.let { resolverSabor(it, item.saborSeleccionado) }
            val usados = usadosPorProducto[item.productoId] ?: 0
            val generalDisponible = producto?.stock?.minus(usados) ?: 0
            val saborDisponible = if (producto != null && sabor != null) stockDelSabor(producto, sabor) else 0
            val maximo = minOf(generalDisponible, saborDisponible)
            if (producto == null || maximo <= 0) null
            else item.copy(
                cantidad = item.cantidad.coerceIn(1, maximo),
                stockDisponible = saborDisponible,
                saborSeleccionado = sabor
            ).also { usadosPorProducto[item.productoId] = usados + it.cantidad }
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
            }.distinctBy { it.productoId to claveSabor(it.saborSeleccionado) }
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
    fun agregarProducto(producto: Producto, saborSeleccionado: String? = null, context: Context? = null) {
        if (limpiezaPendiente) return
        if (context != null) appContext = context.applicationContext

        val precio = producto.precio
        if (inventario != null && inventario?.containsKey(producto.id) != true) {
            aviso = "El producto ya no está disponible en el inventario."
            return
        }
        val productoActual = inventario?.get(producto.id) ?: producto.also {
            inventario = inventario.orEmpty() + (producto.id to producto)
        }
        val sabor = resolverSabor(productoActual, saborSeleccionado)
        if (sabor == null) {
            aviso = if (saboresDelProducto(productoActual).isNotEmpty())
                "Selecciona un sabor válido para ${productoActual.nombre}."
            else "Este producto no utiliza selección de sabor."
            return
        }
        val existente = items.find { mismaLinea(it, producto.id, sabor) }
        val totalProducto = items.filter { it.productoId == producto.id }.sumOf { it.cantidad }
        val disponibleSabor = stockDelSabor(productoActual, sabor)
        if (productoActual.stock <= totalProducto || disponibleSabor <= (existente?.cantidad ?: 0)) {
            aviso = "Sin stock disponible para agregar más unidades de ${producto.nombre}."
            return
        }
        if (!producto.precio.isFinite() || producto.precio <= 0 || producto.id.isBlank()) {
            aviso = "El producto no tiene datos válidos para comprar."
            return
        }

        items = if (existente != null) {
            items.map {
                if (mismaLinea(it, producto.id, sabor)) {
                    it.copy(cantidad = it.cantidad + 1, stockDisponible = disponibleSabor)
                }
                else it
            }
        } else {
            items + ItemCarrito(
                productoId = producto.id,
                nombre     = producto.nombre,
                marca      = producto.marca,
                precioUnit = precio,
                stockDisponible = disponibleSabor,
                saborSeleccionado = sabor
            )
        }
        guardarCarrito(context)
    }

    /**
     * Incrementa en 1 la cantidad del ítem con el [productoId] dado.
     */
    fun incrementar(productoId: String, saborSeleccionado: String? = null, context: Context? = null) {
        if (limpiezaPendiente) return
        if (context != null) appContext = context.applicationContext
        val item = items.firstOrNull { mismaLinea(it, productoId, saborSeleccionado) } ?: return
        val producto = inventario?.get(productoId)
        val totalProducto = items.filter { it.productoId == productoId }.sumOf { it.cantidad }
        if (producto != null && totalProducto >= producto.stock) {
            aviso = "No puedes superar el stock general del producto."
            return
        }
        if (item.stockDisponible == null || item.cantidad >= item.stockDisponible) {
            aviso = "No puedes superar el stock conocido. Actualiza el catálogo para comprobar disponibilidad."
            return
        }
        items = items.map {
            if (mismaLinea(it, productoId, saborSeleccionado))
                it.copy(cantidad = it.cantidad + 1)
            else it
        }
        if (context != null) appContext = context.applicationContext
        guardarCarrito(context)
    }

    /**
     * Decrementa en 1 la cantidad del ítem.
     * Si la cantidad llega a 0, el ítem se elimina del carrito.
     */
    fun decrementar(productoId: String, saborSeleccionado: String? = null, context: Context? = null) {
        if (context != null) appContext = context.applicationContext
        items = items.mapNotNull {
            when {
                !mismaLinea(it, productoId, saborSeleccionado) -> it
                it.cantidad > 1             -> it.copy(cantidad = it.cantidad - 1)
                else                        -> it
            }
        }
        guardarCarrito(context)
    }

    /**
     * Elimina completamente un ítem del carrito.
     */
    fun eliminarItem(productoId: String, saborSeleccionado: String? = null, context: Context? = null) {
        if (context != null) appContext = context.applicationContext
        items = items.filterNot { mismaLinea(it, productoId, saborSeleccionado) }
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

    /** Devuelve el nombre guardado por el producto; nunca usa coincidencias aproximadas. */
    private fun resolverSabor(producto: Producto, seleccionado: String?): String? {
        val sabores = saboresDelProducto(producto)
        if (sabores.isEmpty()) return if (seleccionado.isNullOrBlank()) "" else null
        return sabores.firstOrNull { claveSabor(it) == claveSabor(seleccionado) }
    }

    private fun saboresDelProducto(producto: Producto): List<String> =
        if (producto.stockPorSabor.isNotEmpty()) producto.stockPorSabor.keys.toList()
        else producto.sabor.split(',').map { it.trim() }.filter { it.isNotBlank() }

    private fun stockDelSabor(producto: Producto, sabor: String): Int =
        if (producto.stockPorSabor.isEmpty()) producto.stock
        else producto.stockPorSabor.entries.firstOrNull { claveSabor(it.key) == claveSabor(sabor) }?.value ?: 0

    private fun mismaLinea(item: ItemCarrito, productoId: String, sabor: String?): Boolean =
        item.productoId == productoId && claveSabor(item.saborSeleccionado) == claveSabor(sabor)

    private fun claveSabor(sabor: String?): String = sabor?.trim()?.lowercase(Locale.ROOT).orEmpty()
}
