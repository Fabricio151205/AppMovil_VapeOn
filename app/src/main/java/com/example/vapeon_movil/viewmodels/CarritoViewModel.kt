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
class CarritoViewModel : ViewModel() {

    private val gson = Gson()
    private val PREFS_NAME = "vapeon_carrito_prefs"
    private val KEY_ITEMS = "carrito_items"

    private var appContext: Context? = null

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
    fun cargarCarrito(context: Context) {
        appContext = context.applicationContext
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_ITEMS, null)
            if (!json.isNullOrEmpty()) {
                val type = object : TypeToken<List<ItemCarrito>>() {}.type
                val itemsGuardados: List<ItemCarrito>? = gson.fromJson(json, type)
                if (itemsGuardados != null) {
                    items = itemsGuardados
                }
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
            prefs.edit().putString(KEY_ITEMS, json).apply()
        } catch (e: Exception) {
            println("Error al guardar carrito persistente: ${e.localizedMessage}")
        }
    }

    // ─── Acciones ──────────────────────────────────────────────────────────

    /**
     * Agrega un producto al carrito.
     * - Si el producto ya existe, incrementa su cantidad en 1.
     * - Si es nuevo, lo agrega con cantidad = 1.
     */
    fun agregarProducto(producto: Producto, context: Context? = null) {
        if (context != null) appContext = context.applicationContext

        val precio = producto.precio
        val existente = items.find { it.productoId == producto.id }

        items = if (existente != null) {
            items.map {
                if (it.productoId == producto.id) it.copy(cantidad = it.cantidad + 1)
                else it
            }
        } else {
            items + ItemCarrito(
                productoId = producto.id,
                nombre     = producto.nombre,
                marca      = producto.marca,
                precioUnit = precio
            )
        }
        guardarCarrito(context)
    }

    /**
     * Incrementa en 1 la cantidad del ítem con el [productoId] dado.
     */
    fun incrementar(productoId: String, context: Context? = null) {
        if (context != null) appContext = context.applicationContext
        items = items.map {
            if (it.productoId == productoId) it.copy(cantidad = it.cantidad + 1)
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
                else                        -> null
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
    fun limpiarCarrito(context: Context? = null) {
        val targetContext = context ?: appContext
        items = emptyList()
        if (targetContext != null) {
            try {
                val prefs = targetContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().remove(KEY_ITEMS).apply()
            } catch (e: Exception) {
                println("Error al limpiar carrito en prefs: ${e.localizedMessage}")
            }
        }
    }
}
