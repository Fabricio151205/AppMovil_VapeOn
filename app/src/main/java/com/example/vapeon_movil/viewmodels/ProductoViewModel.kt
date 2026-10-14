package com.example.vapeon_movil.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vapeon_movil.entities.Producto
import com.example.vapeon_movil.services.ProductoService
import com.example.vapeon_movil.utils.RetrofitClient
import kotlinx.coroutines.launch

/**
 * ViewModel de la entidad Producto.
 *
 * Centraliza toda la lógica de acceso a la colección "productos" de Firestore
 * (listar, eliminar). Las operaciones de creación y actualización se delegan a
 * sus pantallas respectivas (AgregarProductoScreen / EditarProductoScreen) porque
 * requieren estado de formulario propio.
 *
 * Pantallas consumidoras: CatalogoScreen, AdminHomeScreen.
 */
class ProductoViewModel : ViewModel() {

    // ─── Servicio de red ───────────────────────────────────────────────────
    private val productoService: ProductoService =
        RetrofitClient.retrofit.create(ProductoService::class.java)

    // ─── Estado observable ─────────────────────────────────────────────────

    /** Lista completa de productos cargada desde Firestore */
    var productos by mutableStateOf<List<Producto>>(emptyList())
        private set

    /** Indica si hay una petición HTTP en curso */
    var cargando by mutableStateOf(false)
        private set

    /** Mensaje de error de red; null cuando no hay error */
    var error by mutableStateOf<String?>(null)
        private set

    // ─── Acciones ──────────────────────────────────────────────────────────

    /** Lectura fresca para checkout; los errores los presenta PedidoViewModel. */
    suspend fun consultarProducto(id: String) = productoService.mostrarProducto(id)

    /**
     * Carga todos los productos desde Firestore REST API.
     * Actualiza [productos], [cargando] y [error] reactivamente.
     */
    fun cargarProductos() {
        viewModelScope.launch {
            cargando = true
            error = null
            try {
                val respuesta = productoService.listarProductos()
                productos = respuesta.documents?.map { doc ->
                    Producto(
                        id          = doc.name.substringAfterLast("/"),
                        nombre      = doc.fields.nombre?.stringValue      ?: "",
                        marca       = doc.fields.marca?.stringValue       ?: "",
                        precio      = doc.fields.precio?.stringValue      ?: "",
                        stock       = doc.fields.stock?.stringValue       ?: "",
                        descripcion = doc.fields.descripcion?.stringValue ?: ""
                    )
                } ?: emptyList()
            } catch (e: Exception) {
                error = "Error al cargar productos: ${e.localizedMessage}"
            } finally {
                cargando = false
            }
        }
    }

    /**
     * Elimina un producto de Firestore por su ID.
     * Actualiza la lista local de forma optimista sin recargar toda la colección.
     *
     * @param id       ID del documento en Firestore.
     * @param onExito  Callback ejecutado en el hilo principal cuando la operación es exitosa.
     */
    fun eliminarProducto(id: String, onExito: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                productoService.eliminarProducto(id)
                // Actualización local optimista — evita un round-trip de red
                productos = productos.filter { it.id != id }
                onExito()
            } catch (e: Exception) {
                error = "Error al eliminar producto: ${e.localizedMessage}"
            }
        }
    }

    /** Limpia el mensaje de error una vez que la UI lo ha mostrado */
    fun limpiarError() {
        error = null
    }
}
