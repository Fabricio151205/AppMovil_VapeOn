package com.example.vapeon_movil.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vapeon_movil.entities.Producto
import com.example.vapeon_movil.services.ProductoService
import com.example.vapeon_movil.services.listarTodosProductos
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.utils.RetrofitClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import retrofit2.HttpException
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.util.UUID

/**
 * ViewModel de la entidad Producto.
 *
 * Centraliza toda la lógica de acceso a la colección "productos" de Firestore
 * (listar, eliminar y editar con control de versión). La creación se mantiene en
 * AgregarProductoScreen; EditarProductoScreen conserva los campos del formulario
 * y utiliza este ViewModel para cargar y guardar la misma versión del documento.
 *
 * Pantallas consumidoras: CatalogoScreen, AdminHomeScreen.
 */
class ProductoViewModel(
    private val productoService: ProductoService = RetrofitClient.retrofit.create(ProductoService::class.java),
    private val dispatcherEdicion: CoroutineDispatcher = Dispatchers.Main.immediate
) : ViewModel() {

    // ─── Servicio de red ───────────────────────────────────────────────────

    // ─── Estado observable ─────────────────────────────────────────────────

    /** Lista completa de productos cargada desde Firestore */
    var productos by mutableStateOf<List<Producto>>(emptyList())
        private set

    /** Indica si hay una petición HTTP en curso */
    var cargando by mutableStateOf(false)
        private set
    var inventarioCargado by mutableStateOf(false)
        private set

    /** Mensaje de error de red; null cuando no hay error */
    var error by mutableStateOf<String?>(null)
        private set

    // Selección de navegación: vive en el ViewModel, no en SavedState ni en Firestore.
    var productoParaRuta by mutableStateOf<Producto?>(null)
        private set
    var propietarioProductoRuta by mutableStateOf<String?>(null)
        private set
    var cargandoProductoRuta by mutableStateOf(false)
        private set
    var errorProductoRuta by mutableStateOf<String?>(null)
        private set
    private var cargaProductoRuta: Job? = null
    var eliminandoIds by mutableStateOf<Set<String>>(emptySet())
        private set
    var cargandoCreacion by mutableStateOf(false)
        private set
    var errorCreacion by mutableStateOf<String?>(null)
        private set
    private var creacionPendiente: Pair<String, ProductoContainer>? = null
    private var operacionCreacion: Job? = null
    var altaCompletada by mutableStateOf(false)
        private set
    var edicionCompletada by mutableStateOf(false)
        private set
    fun consumirResultadosGuardado() { altaCompletada = false; edicionCompletada = false }
    val datosCreacionPendiente get() = creacionPendiente?.second?.fields

    fun crearProducto(nombre: String, marca: String, precio: Double, stock: Int, descripcion: String,
        onExito: (() -> Unit)? = null) {
        if (cargandoCreacion) return
        if (nombre.isBlank() || marca.isBlank() || !precioValido(precio) || stock < 0) {
            errorCreacion = "Completa nombre y marca; utiliza un precio válido y stock entero no negativo."
            return
        }
        val cuerpo = ProductoContainer(ProductoCampos(FirestoreValue(stringValue = nombre),
            FirestoreValue(stringValue = marca), FirestoreValue(doubleValue = precio),
            FirestoreValue(integerValue = stock.toString()), FirestoreValue(stringValue = descripcion)))
        val intento = creacionPendiente ?: (UUID.randomUUID().toString() to cuerpo).also { creacionPendiente = it }
        if (intento.second != cuerpo) {
            errorCreacion = "Comprueba el alta anterior reintentando con los mismos datos; no se creará otro producto."
            return
        }
        cargandoCreacion = true
        errorCreacion = null
        operacionCreacion = viewModelScope.launch(dispatcherEdicion) {
            try {
                try {
                    productoService.crearProducto(cuerpo, intento.first)
                } catch (e: HttpException) {
                    if (e.code() != 409) throw e
                    // El mismo ID ya fue creado: comprobarlo, sin generar otra alta.
                    val guardado = productoService.mostrarProducto(intento.first)
                    require(guardado.name.substringAfterLast('/') == intento.first)
                }
                coroutineContext.ensureActive()
                creacionPendiente = null
                altaCompletada = true
                onExito?.invoke()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                coroutineContext.ensureActive()
                if (e is HttpException && e.code() in listOf(400, 401, 403)) creacionPendiente = null
                errorCreacion = "No se pudo confirmar el alta. Reintenta con los mismos datos para comprobar el mismo producto."
            } finally { if (coroutineContext[Job]?.isCancelled != true) cargandoCreacion = false }
        }
    }

    fun actualizarSesionProducto(claveSesion: String?) {
        if (propietarioProductoRuta == claveSesion) return
        inventarioCargado = false // Cada sesión comprueba su inventario antes de ajustar el carrito restaurado.
        operacionCreacion?.cancel()
        creacionPendiente = null
        cargandoCreacion = false
        errorCreacion = null
        altaCompletada = false
        cerrarEdicion()
        cargaProductoRuta?.cancel()
        propietarioProductoRuta = claveSesion
        productoParaRuta = null
        cargandoProductoRuta = false
        errorProductoRuta = null
    }

    fun seleccionarProductoParaRuta(claveSesion: String, producto: Producto) {
        actualizarSesionProducto(claveSesion)
        cargaProductoRuta?.cancel()
        productoParaRuta = producto
        cargandoProductoRuta = false
        errorProductoRuta = null
    }

    /** Recupera por ID cuando no queda una selección válida en memoria. */
    fun recuperarProductoParaRuta(claveSesion: String, id: String) {
        actualizarSesionProducto(claveSesion)
        if (productoParaRuta?.id == id) return
        cargaProductoRuta?.cancel()
        productoParaRuta = null
        errorProductoRuta = null
        if (id.isBlank() || id.contains('/')) {
            errorProductoRuta = "No se pudo recuperar el producto seleccionado."
            cargandoProductoRuta = false
            return
        }
        cargandoProductoRuta = true
        cargaProductoRuta = viewModelScope.launch {
            try {
                val doc = productoService.mostrarProducto(id)
                require(doc.name.substringAfterLast('/') == id) { "Producto no disponible." }
                val producto = Producto(
                    id = id, nombre = doc.fields.nombre?.asString().orEmpty(),
                    marca = doc.fields.marca?.asString().orEmpty(),
                    precio = doc.fields.precio?.asDouble() ?: 0.0,
                    stock = doc.fields.stock?.asInt() ?: 0,
                    descripcion = doc.fields.descripcion?.asString().orEmpty()
                )
                if (propietarioProductoRuta == claveSesion) productoParaRuta = producto
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (propietarioProductoRuta == claveSesion) errorProductoRuta =
                    "No se pudo recuperar el producto. Puede haber sido eliminado o no hay conexión."
            } finally {
                if (propietarioProductoRuta == claveSesion && coroutineContext[Job]?.isCancelled != true)
                    cargandoProductoRuta = false
            }
        }
    }

    // ─── Acciones ──────────────────────────────────────────────────────────

    /** Lectura fresca para checkout; los errores los presenta PedidoViewModel. */
    suspend fun consultarProducto(id: String) = productoService.mostrarProducto(id)

    /**
     * Carga todos los productos desde Firestore REST API.
     * Actualiza [productos], [cargando] y [error] reactivamente.
     */
    fun cargarProductos() {
        if (cargando) return
        cargando = true
        error = null
        viewModelScope.launch(dispatcherEdicion) {
            try {
                val respuesta = productoService.listarTodosProductos()
                productos = respuesta.documents?.map { doc ->
                    Producto(
                        id          = doc.name.substringAfterLast("/"),
                        nombre      = doc.fields.nombre?.asString()      ?: "",
                        marca       = doc.fields.marca?.asString()       ?: "",
                        precio      = doc.fields.precio?.asDouble()      ?: 0.0,
                        stock       = doc.fields.stock?.asInt()         ?: 0,
                        descripcion = doc.fields.descripcion?.asString() ?: ""
                    )
                } ?: emptyList()
                inventarioCargado = true
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                error = if (e is HttpException && e.code() in listOf(401, 403))
                    "Firestore no permite consultar los productos. Revisa los permisos."
                else "No se pudieron cargar los productos. Comprueba la conexión y reintenta."
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
        if (id in eliminandoIds) return
        eliminandoIds = eliminandoIds + id
        error = null
        viewModelScope.launch(dispatcherEdicion) {
            try {
                try { productoService.eliminarProducto(id) }
                catch (e: HttpException) { if (e.code() != 404) throw e }
                // Actualización local optimista — evita un round-trip de red
                productos = productos.filter { it.id != id }
                onExito()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = "No se pudo confirmar la eliminación. Actualiza el catálogo antes de reintentar." }
            finally { eliminandoIds = eliminandoIds - id }
        }
    }

    /** Limpia el mensaje de error una vez que la UI lo ha mostrado */
    fun limpiarError() {
        error = null
    }

    // I4: el formulario y la precondición comparten la misma lectura, sin alterar la selección de C3.
    var documentoEdicion by mutableStateOf<ProductoDocumento?>(null)
        private set
    var cargandoEdicion by mutableStateOf(false)
        private set
    var guardandoEdicion by mutableStateOf(false)
        private set
    var errorEdicion by mutableStateOf<String?>(null)
        private set
    var requiereRecargarEdicion by mutableStateOf(false)
        private set
    private var operacionEdicion: Job? = null

    fun cerrarEdicion() {
        operacionEdicion?.cancel()
        operacionEdicion = null
        documentoEdicion = null
        cargandoEdicion = false
        guardandoEdicion = false
        errorEdicion = null
        requiereRecargarEdicion = false
        edicionCompletada = false
    }

    fun cargarProductoEdicion(id: String) {
        cerrarEdicion()
        if (id.isBlank() || '/' in id) {
            errorEdicion = "No se pudo identificar el producto."
            return
        }
        cargandoEdicion = true
        operacionEdicion = viewModelScope.launch(dispatcherEdicion) {
            try {
                val documento = productoService.mostrarProducto(id)
                coroutineContext.ensureActive()
                require(documento.name.substringAfterLast('/') == id && !documento.updateTime.isNullOrBlank())
                documentoEdicion = documento
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                errorEdicion = "No se pudo cargar el producto con su versión. Puede haber sido eliminado o no hay conexión."
            } finally {
                if (coroutineContext[Job]?.isCancelled != true) cargandoEdicion = false
            }
        }
    }

    fun iniciarEdicion(id: String) {
        if (documentoEdicion?.name?.substringAfterLast('/') == id || cargandoEdicion) return
        cargarProductoEdicion(id)
    }

    fun guardarProductoEdicion(
        nombre: String, marca: String, precio: Double, stock: Int, descripcion: String,
        onExito: (() -> Unit)? = null
    ) {
        if (guardandoEdicion || cargandoEdicion || requiereRecargarEdicion) return
        if (nombre.isBlank() || marca.isBlank() || !precioValido(precio) || stock < 0) {
            errorEdicion = "Completa nombre y marca; utiliza un precio válido y stock entero no negativo."
            return
        }
        errorEdicion = null
        val original = documentoEdicion
        if (original == null || original.updateTime.isNullOrBlank()) {
            errorEdicion = "Recarga el producto antes de guardar."
            return
        }
        val anteriores = original.fields
        val cuerpo = ProductoContainer(ProductoCampos(
            nombre = if (nombre != anteriores.nombre?.asString()) FirestoreValue(stringValue = nombre) else null,
            marca = if (marca != anteriores.marca?.asString()) FirestoreValue(stringValue = marca) else null,
            precio = if (precio != anteriores.precio?.asDouble()) FirestoreValue(doubleValue = precio) else null,
            stock = if (stock != anteriores.stock?.asInt()) FirestoreValue(integerValue = stock.toString()) else null,
            descripcion = if (descripcion != anteriores.descripcion?.asString()) FirestoreValue(stringValue = descripcion) else null
        ))
        val campos = listOfNotNull(
            cuerpo.fields.nombre?.let { "nombre" }, cuerpo.fields.marca?.let { "marca" },
            cuerpo.fields.precio?.let { "precio" }, cuerpo.fields.stock?.let { "stock" },
            cuerpo.fields.descripcion?.let { "descripcion" }
        )
        if (campos.isEmpty()) {
            errorEdicion = "No hay cambios para guardar."
            return
        }
        guardandoEdicion = true
        operacionEdicion = viewModelScope.launch(dispatcherEdicion) {
            try {
                productoService.actualizarProducto(original.name.substringAfterLast('/'), cuerpo,
                    original.updateTime, campos)
                coroutineContext.ensureActive()
                edicionCompletada = true
                onExito?.invoke()
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                coroutineContext.ensureActive()
                val conflicto = e.code() in listOf(409, 412) || (e.code() == 400 && runCatching {
                    Gson().fromJson(e.response()?.errorBody()?.string(), JsonObject::class.java)
                        ?.getAsJsonObject("error")?.get("status")?.asString == "FAILED_PRECONDITION"
                }.getOrDefault(false))
                requiereRecargarEdicion = conflicto || e.code() == 404
                errorEdicion = when {
                    conflicto -> "El producto fue modificado mientras lo editabas. Actualiza los datos e inténtalo nuevamente."
                    e.code() == 404 -> "El producto ya no existe. Vuelve o recarga los datos."
                    else -> "No se pudo guardar el producto. Revisa la conexión y los permisos."
                }
            } catch (_: Exception) {
                coroutineContext.ensureActive()
                errorEdicion = "No se pudo confirmar el guardado. Recarga los datos para comprobar el producto."
            } finally {
                if (coroutineContext[Job]?.isCancelled != true) guardandoEdicion = false
            }
        }
    }
}

private fun precioValido(precio: Double): Boolean = precio.isFinite() && precio > 0 &&
    java.math.BigDecimal.valueOf(precio).stripTrailingZeros().scale() <= 2
