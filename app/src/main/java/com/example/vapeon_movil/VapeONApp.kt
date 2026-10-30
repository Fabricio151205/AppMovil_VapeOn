package com.example.vapeon_movil

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vapeon_movil.entities.Producto
import com.example.vapeon_movil.ui.PedidosAdminScreen
import com.example.vapeon_movil.ui.AdminHomeScreen
import com.example.vapeon_movil.ui.AgregarProductoScreen
import com.example.vapeon_movil.ui.DatosEntregaScreen
import com.example.vapeon_movil.ui.SeleccionDireccionScreen
import com.example.vapeon_movil.ui.MetodoPagoScreen
import com.example.vapeon_movil.ui.EfectivoScreen
import com.example.vapeon_movil.ui.YapeScreen
import com.example.vapeon_movil.ui.PagoSimuladoScreen
import com.example.vapeon_movil.ui.ConfirmacionPagoScreen
import com.example.vapeon_movil.ui.SeleccionTarjetaScreen
import com.example.vapeon_movil.ui.AgregarTarjetaNumeroScreen
import com.example.vapeon_movil.ui.AgregarTarjetaExpiracionScreen
import com.example.vapeon_movil.ui.AgregarTarjetaCvvScreen
import com.example.vapeon_movil.ui.AgregarTarjetaTitularScreen
import com.example.vapeon_movil.ui.ConfirmacionPedidoScreen
import com.example.vapeon_movil.ui.vencimientoTarjetaSimuladaValido
import com.example.vapeon_movil.ui.CarritoScreen
import com.example.vapeon_movil.ui.CatalogoScreen
import com.example.vapeon_movil.ui.DetalleProductoScreen
import com.example.vapeon_movil.ui.EditarProductoScreen
import com.example.vapeon_movil.ui.ForgotPasswordScreen
import com.example.vapeon_movil.ui.HistorialComprasScreen
import com.example.vapeon_movil.ui.DetallePedidoScreen
import com.example.vapeon_movil.ui.HomeScreen
import com.example.vapeon_movil.ui.LoginScreen
import com.example.vapeon_movil.ui.RegisterScreen
import com.example.vapeon_movil.ui.SaboresAdminScreen
import com.example.vapeon_movil.ui.AgregarSaborScreen
import com.example.vapeon_movil.ui.EditarSaborScreen
import com.example.vapeon_movil.viewmodels.PedidoViewModel
import com.example.vapeon_movil.viewmodels.CarritoViewModel
import com.example.vapeon_movil.viewmodels.ProductoViewModel
import com.example.vapeon_movil.viewmodels.SaboresViewModel
import com.example.vapeon_movil.viewmodels.UsuarioViewModel
import com.example.vapeon_movil.ui.TarjetasScreen
import com.example.vapeon_movil.viewmodels.TarjetaViewModel

@Composable
fun VapeONApp() {

    val context = LocalContext.current

    // ── ViewModels con scope de Activity ──────────────────────────────────────
    // Al instanciarse aquí (nivel raíz), sobreviven rotaciones y se comparten
    // entre todas las pantallas sin necesidad de recrearse.
    val usuarioViewModel: UsuarioViewModel  = viewModel()
    val productoViewModel: ProductoViewModel = viewModel()
    val carritoViewModel: CarritoViewModel  = viewModel()
    val pedidoViewModel: PedidoViewModel = viewModel()
    val saboresViewModel: SaboresViewModel = viewModel()
    val tarjetaViewModel: TarjetaViewModel = viewModel()

    val usuarioActual = usuarioViewModel.usuarioActual
    val claveSesion = claveSesionNavegacion(usuarioActual)

    // Solo ruta, identificadores y propietario; nunca objetos de negocio ni credenciales.
    var pantallaActual by rememberSaveable { mutableStateOf("login") }
    var productoSeleccionadoId by rememberSaveable { mutableStateOf("") }
    var productoEditarId by rememberSaveable { mutableStateOf("") }
    var pedidoSeleccionadoId by rememberSaveable { mutableStateOf("") }
    var propietarioNavegacion by rememberSaveable { mutableStateOf<String?>(null) }
    // Identificador del sabor seleccionado para editar (nombre+marca como clave compuesta)
    var saborEditarNombre by rememberSaveable { mutableStateOf("") }
    var saborEditarMarca by rememberSaveable { mutableStateOf("") }

    // Borradores de presentación asociados a la sesión. Nunca contienen PAN ni CVV.
    var entregaRevisada by rememberSaveable(claveSesion) { mutableStateOf(false) }
    var direccionEntrega by rememberSaveable(claveSesion) { mutableStateOf("") }
    var direccionConfirmada by rememberSaveable(claveSesion) { mutableStateOf(false) }
    var montoEfectivo by rememberSaveable(claveSesion) { mutableStateOf("") }
    var registroDesdeCheckout by rememberSaveable(claveSesion) { mutableStateOf(false) }
    var tarjetaMarca by rememberSaveable(claveSesion) { mutableStateOf("") }
    var tarjetaUltimos4 by rememberSaveable(claveSesion) { mutableStateOf("") }
    var tarjetaMes by rememberSaveable(claveSesion) { mutableStateOf("") }
    var tarjetaAnio by rememberSaveable(claveSesion) { mutableStateOf("") }
    var tarjetaTitular by rememberSaveable(claveSesion) { mutableStateOf("") }
    var pasoCvvCompletado by rememberSaveable(claveSesion) { mutableStateOf(false) }
    var esperandoRegistroTarjeta by rememberSaveable(claveSesion) { mutableStateOf(false) }

    fun limpiarBorradores() {
        entregaRevisada = false; direccionEntrega = ""; direccionConfirmada = false; montoEfectivo = ""
        registroDesdeCheckout = false
        tarjetaMarca = ""; tarjetaUltimos4 = ""; tarjetaMes = ""; tarjetaAnio = ""; tarjetaTitular = ""
        pasoCvvCompletado = false; esperandoRegistroTarjeta = false
    }

    fun iniciarRegistroTarjeta(desdeCheckout: Boolean) {
        registroDesdeCheckout = desdeCheckout
        val pendiente = tarjetaViewModel.registroPendiente
        tarjetaMarca = pendiente?.marca.orEmpty()
        tarjetaUltimos4 = pendiente?.ultimos4.orEmpty()
        tarjetaMes = pendiente?.vencimientoMes?.toString().orEmpty()
        tarjetaAnio = pendiente?.vencimientoAnio?.toString().orEmpty()
        tarjetaTitular = pendiente?.titular.orEmpty()
        pasoCvvCompletado = pendiente != null
        esperandoRegistroTarjeta = false
        pantallaActual = if (pendiente != null) "tarjetaTitular" else "tarjetaNumero"
    }

    // Resultados de formularios observados por la composición vigente, incluso tras una rotación.
    LaunchedEffect(usuarioViewModel.registroCompletado) {
        if (usuarioViewModel.registroCompletado) {
            if (pantallaActual == "registro") pantallaActual = "login"
            usuarioViewModel.consumirRegistroCompletado()
        }
    }
    LaunchedEffect(productoViewModel.altaCompletada, productoViewModel.edicionCompletada) {
        if (productoViewModel.altaCompletada && pantallaActual == "agregarProducto") pantallaActual = "catalogo"
        if (productoViewModel.edicionCompletada && pantallaActual == "editarProducto")
            pantallaActual = if (usuarioActual?.rol.equals("ADMIN", true)) "admin" else "catalogo"
        productoViewModel.consumirResultadosGuardado()
    }

    LaunchedEffect(claveSesion) {
        tarjetaViewModel.actualizarUsuario(usuarioActual)
        pedidoViewModel.actualizarUsuarioCheckout(usuarioActual)
        carritoViewModel.cargarCarrito(context, usuarioActual?.id)
        pedidoViewModel.actualizarUsuarioHistorial(usuarioActual)
        pedidoViewModel.actualizarUsuarioAdmin(usuarioActual)
        productoViewModel.actualizarSesionProducto(claveSesion)
        if (propietarioNavegacion != claveSesion) {
            productoSeleccionadoId = ""
            productoEditarId = ""
            pedidoSeleccionadoId = ""
            propietarioNavegacion = claveSesion
            if (pantallaActual !in RUTAS_PUBLICAS) pantallaActual = pantallaInicialSesion(usuarioActual)
        }
    }

    LaunchedEffect(claveSesion, productoViewModel.productos, productoViewModel.inventarioCargado) {
        if (productoViewModel.inventarioCargado) carritoViewModel.actualizarInventario(productoViewModel.productos)
    }

    fun iniciarNavegacionSesion() {
        limpiarBorradores()
        tarjetaViewModel.actualizarUsuario(null)
        tarjetaViewModel.actualizarUsuario(usuarioViewModel.usuarioActual)
        pedidoViewModel.actualizarUsuarioCheckout(usuarioViewModel.usuarioActual)
        // También descarta selecciones si el mismo usuario vuelve a iniciar sesión.
        propietarioNavegacion = claveSesionNavegacion(usuarioViewModel.usuarioActual)
        productoSeleccionadoId = ""
        productoEditarId = ""
        pedidoSeleccionadoId = ""
        productoViewModel.actualizarSesionProducto(null)
        productoViewModel.actualizarSesionProducto(propietarioNavegacion)
    }

    fun cerrarSesion() {
        limpiarBorradores()
        tarjetaViewModel.actualizarUsuario(null)
        usuarioViewModel.cerrarSesion()
        productoSeleccionadoId = ""
        productoEditarId = ""
        pedidoSeleccionadoId = ""
        propietarioNavegacion = null
        productoViewModel.actualizarSesionProducto(null)
        pedidoViewModel.actualizarUsuarioCheckout(null)
        pedidoViewModel.actualizarUsuarioHistorial(null)
        pedidoViewModel.actualizarUsuarioAdmin(null)
        // Cargar sin usuario vacía solo la memoria; conserva el carrito y el intento de A.
        carritoViewModel.cargarCarrito(context, null)
        pantallaActual = "login"
    }

    fun seleccionarProducto(producto: Producto, editar: Boolean = false) {
        val propietario = claveSesionNavegacion(usuarioViewModel.usuarioActual) ?: return
        productoViewModel.seleccionarProductoParaRuta(propietario, producto)
        if (editar) productoEditarId = producto.id else productoSeleccionadoId = producto.id
        pantallaActual = if (editar) "editarProducto" else "detalle"
    }

    // Derivado del ViewModel — evita duplicar estado
    val esAdminUser = usuarioActual?.rol?.equals("ADMIN", ignoreCase = true) == true

    // La guardia se aplica antes de componer una pantalla, no después de mostrarla.
    val pantallaPermitida = pantallaRestauradaSegura(pantallaActual, usuarioActual, propietarioNavegacion)
    val confirmadoPropio = pedidoViewModel.pedidoConfirmado?.takeIf {
        it.usuarioId == usuarioActual?.id && usuarioActual?.rol.equals("CLIENTE", true)
    }
    val pagoPropio = pedidoViewModel.pagoActual?.takeIf {
        it.usuarioId == usuarioActual?.id && usuarioActual?.rol.equals("CLIENTE", true)
    }
    val rutaDigital = rutaPagoDigitalSegura(pantallaPermitida, pagoPropio?.estadoPago,
        confirmadoPropio?.pagoId != null, pedidoViewModel.cargandoPago, pedidoViewModel.errorPago != null)
    val pantallaCompra = rutaCompraSegura(rutaDigital, entregaRevisada,
        direccionConfirmada && direccionEntrega.isNotBlank(), confirmadoPropio != null,
        pedidoViewModel.requiereComprobacion || pedidoViewModel.cargando,
        pagoPropio != null || pedidoViewModel.cargandoPago || pedidoViewModel.errorPago != null)
    val pantallaSegura = if (pantallaCompra in RUTAS_REGISTRO_TARJETA) {
        if (registroDesdeCheckout && (!entregaRevisada || !direccionConfirmada)) "datosEntrega"
        else rutaRegistroTarjetaSegura(pantallaCompra, tarjetaUltimos4, tarjetaMarca,
            tarjetaMes, tarjetaAnio, pasoCvvCompletado)
    } else pantallaCompra

    // El resultado observado navega con la composición vigente, incluso después de rotar.
    LaunchedEffect(pantallaSegura, tarjetaViewModel.registrando, tarjetaViewModel.mensaje,
        tarjetaViewModel.error, esperandoRegistroTarjeta) {
        if (pantallaSegura == "tarjetaTitular" && esperandoRegistroTarjeta &&
            !tarjetaViewModel.registrando && tarjetaViewModel.registroPendiente == null &&
            tarjetaViewModel.error == null && tarjetaViewModel.mensaje != null) {
            esperandoRegistroTarjeta = false
            pantallaActual = if (registroDesdeCheckout) "seleccionTarjeta" else "tarjetas"
        }
    }

    // Manejador central del botón 'Atrás' del sistema para navegación fluida entre pantallas
    val puedeVolverAtras = pantallaSegura !in setOf("login", "home", "admin")
    BackHandler(enabled = puedeVolverAtras) {
        if (pantallaSegura in RUTAS_COMPRA &&
            (pedidoViewModel.cargando || pedidoViewModel.requiereComprobacion ||
                pedidoViewModel.cargandoPago || pedidoViewModel.tieneIntentoDigital)) return@BackHandler
        when (pantallaSegura) {
            "registro", "recuperar" -> pantallaActual = "login"
            "catalogo" -> pantallaActual = if (esAdminUser) "admin" else "home"
            "carrito", "historialCompras", "tarjetas" -> pantallaActual = "home"
            "checkout", "datosEntrega" -> pantallaActual = "carrito"
            "seleccionDireccion" -> pantallaActual = "datosEntrega"
            "metodoPago" -> pantallaActual = "seleccionDireccion"
            "efectivo", "yape", "seleccionTarjeta" -> pantallaActual = "metodoPago"
            "confirmacionPedido" -> pantallaActual = "home"
            "pagoSimulado", "confirmacionPago" -> pantallaActual = "metodoPago"
            "tarjetaNumero" -> pantallaActual = if (registroDesdeCheckout) "seleccionTarjeta" else "tarjetas"
            "tarjetaExpiracion" -> pantallaActual = "tarjetaNumero"
            "tarjetaCvv" -> pantallaActual = "tarjetaExpiracion"
            "tarjetaTitular" -> pantallaActual = "tarjetaCvv"
            "detallePedido" -> pantallaActual = "historialCompras"
            "saboresAdmin", "pedidosAdmin" -> pantallaActual = "admin"
            "agregarSabor", "editarSabor" -> pantallaActual = "saboresAdmin"
            "detallePedidoAdmin" -> pantallaActual = "pedidosAdmin"
            "agregarProducto" -> pantallaActual = "catalogo"
            "editarProducto" -> pantallaActual = if (esAdminUser) "admin" else "catalogo"
            "detalle" -> pantallaActual = if (esAdminUser) "catalogo" else "home"
        }
    }

    key(claveSesion) {
    when (pantallaSegura) {

        // ── LOGIN ─────────────────────────────────────────────────────────────
        "login" -> {
            LoginScreen(
                usuarioViewModel = usuarioViewModel,
                irRegistro  = { pantallaActual = "registro" },
                irRecuperar = { pantallaActual = "recuperar" },
                irHome      = {
                    iniciarNavegacionSesion()
                    pantallaActual = if (pedidoViewModel.requiereComprobacion) "checkout"
                        else if (pedidoViewModel.tieneIntentoDigital) "pagoSimulado" else "home"
                },
                irAdmin     = { iniciarNavegacionSesion(); pantallaActual = "admin" }
            )
        }

        // ── REGISTRO ──────────────────────────────────────────────────────────
        "registro" -> {
            RegisterScreen(
                usuarioViewModel = usuarioViewModel,
                volverLogin = { if (pantallaActual == "registro") pantallaActual = "login" }
            )
        }

        // ── RECUPERAR CONTRASEÑA ──────────────────────────────────────────────
        "recuperar" -> {
            ForgotPasswordScreen(
                volverLogin = { pantallaActual = "login" }
            )
        }

        // ── HOME (CLIENTE) ────────────────────────────────────────────────────
        "home" -> {
            HomeScreen(
                cerrarSesion = ::cerrarSesion,
                productoViewModel = productoViewModel,
                irDetalleProducto = { producto ->
                    seleccionarProducto(producto)
                },
                irCarrito = { pantallaActual = "carrito" },
                irHistorial = { pantallaActual = "historialCompras" },
                irTarjetas = { pantallaActual = "tarjetas" }
            )
        }

        // ── PANEL ADMIN ───────────────────────────────────────────────────────
        "admin" -> {
            AdminHomeScreen(
                productoViewModel = productoViewModel,
                cerrarSesion = ::cerrarSesion,
                irPedidos = { pantallaActual = "pedidosAdmin" },
                irSabores = { pantallaActual = "saboresAdmin" },
                irCatalogo = { pantallaActual = "catalogo" },
                irEditarProducto = { producto ->
                    seleccionarProducto(producto, editar = true)
                },
                irDetalleProducto = { producto ->
                    seleccionarProducto(producto)
                }
            )
        }

        // ── CATÁLOGO ──────────────────────────────────────────────────────────
        "catalogo" -> {
            CatalogoScreen(
                cerrarSesion = ::cerrarSesion,
                productoViewModel = productoViewModel,
                irDetalleProducto = { producto ->
                    seleccionarProducto(producto)
                },
                esAdmin           = esAdminUser,
                irAgregarProducto = { pantallaActual = "agregarProducto" },
                irEditarProducto  = { producto ->
                    seleccionarProducto(producto, editar = true)
                },
                irAdmin   = { pantallaActual = "admin" },
                irPedidos = { pantallaActual = "pedidosAdmin" },
                irSabores = { pantallaActual = "saboresAdmin" },
                irCarrito = { pantallaActual = "carrito" },
                irHistorial = { pantallaActual = "historialCompras" },
                irTarjetas = { pantallaActual = "tarjetas" }
            )
        }

        // ── DETALLE PRODUCTO ──────────────────────────────────────────────────
        "detalle" -> {
            ProductoDeNavegacion(
                usuario = usuarioActual, id = productoSeleccionadoId, productoViewModel = productoViewModel,
                volver = { pantallaActual = if (esAdminUser) "catalogo" else "home" }
            ) { producto ->
                DetalleProductoScreen(
                    producto = producto,
                    esAdmin = esAdminUser,
                    saboresViewModel = saboresViewModel,
                    volver = { pantallaActual = if (esAdminUser) "catalogo" else "home" },
                    irEditarProducto = { seleccionarProducto(it, editar = true) },
                    // Solo CLIENTE puede agregar al carrito (ADMIN no tiene acceso).
                    onAgregarAlCarrito = if (!esAdminUser) {
                        {
                            carritoViewModel.agregarProducto(producto)
                            pantallaActual = "carrito"
                        }
                    } else null
                )
            }
        }

        // ── CARRITO (solo CLIENTE) ────────────────────────────────────────────
        "carrito" -> {
            if (esAdminUser) {
                pantallaActual = "admin"
            } else {
                CarritoScreen(
                    carritoViewModel = carritoViewModel,
                    volver           = { pantallaActual = "home" },
                    irCheckout = {
                        if (pedidoViewModel.requiereComprobacion || pedidoViewModel.cargando) {
                            pantallaActual = "checkout"
                        } else {
                            if (!carritoViewModel.puedeComprar) return@CarritoScreen
                            pedidoViewModel.iniciarCheckout()
                            limpiarBorradores()
                            pantallaActual = "datosEntrega"
                        }
                    }
                )
            }
        }

        "pedidosAdmin" -> {
            PedidosAdminScreen(
                usuario = usuarioViewModel.usuarioActual,
                pedidoViewModel = pedidoViewModel,
                volver = { pantallaActual = "admin" },
                irDetallePedido = { id ->
                    pedidoSeleccionadoId = id
                    pantallaActual = "detallePedidoAdmin"
                }
            )
        }

        "saboresAdmin" -> {
            SaboresAdminScreen(
                usuario = usuarioViewModel.usuarioActual,
                saboresViewModel = saboresViewModel,
                volver = { pantallaActual = "admin" },
                irAgregarSabor = { pantallaActual = "agregarSabor" },
                irEditarSabor = { sabor ->
                    saborEditarNombre = sabor.nombre
                    saborEditarMarca = sabor.marca
                    pantallaActual = "editarSabor"
                },
                irCatalogo = { pantallaActual = "catalogo" },
                irPedidos = { pantallaActual = "pedidosAdmin" },
                cerrarSesion = ::cerrarSesion
            )
        }

        "detallePedidoAdmin" -> {
            DetallePedidoScreen(
                usuario = usuarioViewModel.usuarioActual,
                pedidoId = pedidoSeleccionadoId,
                pedidoViewModel = pedidoViewModel,
                volver = { pantallaActual = "pedidosAdmin" }
            )
        }

        "historialCompras" -> {
            HistorialComprasScreen(
                usuario = usuarioViewModel.usuarioActual,
                pedidoViewModel = pedidoViewModel,
                volver = { pantallaActual = "home" },
                irCarrito = { pantallaActual = "carrito" },
                irHistorial = { pantallaActual = "historialCompras" },
                cerrarSesion = ::cerrarSesion,
                irTarjetas = { pantallaActual = "tarjetas" },
                irDetallePedido = { id ->
                    pedidoSeleccionadoId = id
                    pantallaActual = "detallePedido"
                }
            )
        }

        "detallePedido" -> {
            DetallePedidoScreen(
                usuario = usuarioViewModel.usuarioActual,
                pedidoId = pedidoSeleccionadoId,
                pedidoViewModel = pedidoViewModel,
                volver = { pantallaActual = "historialCompras" }
            )
        }

        "tarjetas" -> {
            TarjetasScreen(usuarioActual, tarjetaViewModel, volver = { pantallaActual = "home" },
                agregarTarjeta = { iniciarRegistroTarjeta(false) })
        }

        "datosEntrega" -> DatosEntregaScreen(usuarioActual, volver = { pantallaActual = "carrito" },
            continuar = { entregaRevisada = true; pantallaActual = "seleccionDireccion" })

        "seleccionDireccion" -> SeleccionDireccionScreen(direccionEntrega,
            cambiarDireccion = { direccionEntrega = it; direccionConfirmada = false },
            volver = { pantallaActual = "datosEntrega" },
            continuar = { direccionConfirmada = true; pantallaActual = "metodoPago" })

        "metodoPago" -> MetodoPagoScreen(volver = { pantallaActual = "seleccionDireccion" },
            efectivo = { pantallaActual = "efectivo" }, tarjeta = { pantallaActual = "seleccionTarjeta" },
            yape = { pantallaActual = "yape" })

        "efectivo" -> EfectivoScreen(usuarioActual, carritoViewModel, productoViewModel, pedidoViewModel,
            direccionEntrega, montoEfectivo, cambiarMonto = { montoEfectivo = it },
            volver = { pantallaActual = "metodoPago" })

        "confirmacionPedido" -> confirmadoPropio?.let { pedido ->
            val digital = pedido.pagoId != null
            ConfirmacionPedidoScreen(pedido,
                if (digital) pedidoViewModel.tieneIntentoDigital else pedidoViewModel.requiereComprobacion,
                if (digital) pedidoViewModel.cargandoPago else pedidoViewModel.cargando,
                if (digital) pedidoViewModel.errorPago else pedidoViewModel.error, reintentarLimpieza = {
                    if (digital) pedidoViewModel.registrarPedidoDigital(usuarioActual, carritoViewModel, productoViewModel)
                    else pedidoViewModel.confirmar(usuarioActual, carritoViewModel, productoViewModel, "", "")
                }, volverAlCatalogo = { limpiarBorradores(); pantallaActual = "home" })
        }

        "yape" -> YapeScreen(volver = { pantallaActual = "metodoPago" }, iniciarPago = {
            pedidoViewModel.iniciarPagoDigital(usuarioActual, carritoViewModel, productoViewModel, direccionEntrega, "YAPE")
            pantallaActual = "pagoSimulado"
        })

        "seleccionTarjeta" -> SeleccionTarjetaScreen(usuarioActual, tarjetaViewModel,
            volver = { pantallaActual = "metodoPago" }, agregarTarjeta = { iniciarRegistroTarjeta(true) },
            continuar = {
                pedidoViewModel.iniciarPagoDigital(usuarioActual, carritoViewModel, productoViewModel,
                    direccionEntrega, "TARJETA", tarjetaViewModel.tarjetaSeleccionada?.id)
                pantallaActual = "pagoSimulado"
            })

        "pagoSimulado" -> PagoSimuladoScreen(pagoPropio, pedidoViewModel.cargandoPago, pedidoViewModel.errorPago,
            comprobar = { pedidoViewModel.comprobarPagoDigital(usuarioActual) },
            aprobar = { pedidoViewModel.simularResultadoPago(usuarioActual, "CONFIRMADO") },
            rechazar = { pedidoViewModel.simularResultadoPago(usuarioActual, "RECHAZADO") },
            nuevoIntento = { pedidoViewModel.descartarPagoRechazado(usuarioActual) },
            volver = { pantallaActual = "metodoPago" })

        "confirmacionPago" -> pagoPropio?.let { pago ->
            ConfirmacionPagoScreen(pago, pedidoViewModel.cargandoPago, pedidoViewModel.errorPago,
                registrarPedido = { pedidoViewModel.registrarPedidoDigital(usuarioActual, carritoViewModel, productoViewModel) },
                comprobar = { pedidoViewModel.comprobarPagoDigital(usuarioActual) })
        }

        "tarjetaNumero" -> AgregarTarjetaNumeroScreen(tarjetaMarca, cambiarMarca = { tarjetaMarca = it },
            volver = { pantallaActual = if (registroDesdeCheckout) "seleccionTarjeta" else "tarjetas" },
            continuar = { tarjetaUltimos4 = it; pasoCvvCompletado = false; pantallaActual = "tarjetaExpiracion" })

        "tarjetaExpiracion" -> AgregarTarjetaExpiracionScreen(tarjetaMes, tarjetaAnio,
            cambiarMes = { tarjetaMes = it; pasoCvvCompletado = false },
            cambiarAnio = { tarjetaAnio = it; pasoCvvCompletado = false },
            volver = { pantallaActual = "tarjetaNumero" }, continuar = { pantallaActual = "tarjetaCvv" })

        "tarjetaCvv" -> AgregarTarjetaCvvScreen(volver = { pantallaActual = "tarjetaExpiracion" },
            continuar = { pasoCvvCompletado = true; pantallaActual = "tarjetaTitular" })

        "tarjetaTitular" -> AgregarTarjetaTitularScreen(tarjetaTitular, cambiarTitular = { tarjetaTitular = it },
            tarjetaUltimos4, tarjetaViewModel.registrando, tarjetaViewModel.registroPendiente != null,
            tarjetaViewModel.error, volver = {
                pantallaActual = if (tarjetaViewModel.registroPendiente != null)
                    if (registroDesdeCheckout) "seleccionTarjeta" else "tarjetas" else "tarjetaCvv"
            }, registrar = {
                esperandoRegistroTarjeta = true
                tarjetaViewModel.registrarTarjeta(usuarioActual, tarjetaTitular, tarjetaMarca,
                    tarjetaUltimos4, tarjetaMes, tarjetaAnio)
            })

        // ── AGREGAR SABOR (ADMIN) ─────────────────────────────────────────────
        "agregarSabor" -> {
            AgregarSaborScreen(
                saboresViewModel = saboresViewModel,
                volver = { pantallaActual = "saboresAdmin" }
            )
        }

        // ── EDITAR SABOR (ADMIN) ──────────────────────────────────────────────
        "editarSabor" -> {
            val saborEditar = saboresViewModel.listaSabores.find {
                it.nombre == saborEditarNombre && it.marca == saborEditarMarca
            }
            if (saborEditar == null) {
                pantallaActual = "saboresAdmin"
            } else {
                EditarSaborScreen(
                    saborOriginal = saborEditar,
                    saboresViewModel = saboresViewModel,
                    volver = { pantallaActual = "saboresAdmin" }
                )
            }
        }

        // ── AGREGAR PRODUCTO (ADMIN) ───────────────────────────────────────────
        "agregarProducto" -> {
            AgregarProductoScreen(
                productoViewModel = productoViewModel,
                saboresViewModel = saboresViewModel,
                volver = { pantallaActual = "catalogo" }
            )
        }

        // ── EDITAR PRODUCTO (ADMIN) ────────────────────────────────────────────
        "editarProducto" -> {
            ProductoDeNavegacion(
                usuario = usuarioActual, id = productoEditarId, productoViewModel = productoViewModel,
                volver = { pantallaActual = "catalogo" }
            ) { producto ->
                EditarProductoScreen(
                    producto = producto,
                    productoViewModel = productoViewModel,
                    saboresViewModel = saboresViewModel,
                    volver = { pantallaActual = if (esAdminUser) "admin" else "catalogo" }
                )
            }
        }
    }
    }
}

private val RUTAS_PUBLICAS = setOf("login", "registro", "recuperar")
internal val RUTAS_PAGO_DIGITAL = setOf("pagoSimulado", "confirmacionPago")
internal val RUTAS_COMPRA = setOf("checkout", "datosEntrega", "seleccionDireccion", "metodoPago", "efectivo",
    "yape", "seleccionTarjeta", "confirmacionPedido") + RUTAS_PAGO_DIGITAL
internal val RUTAS_REGISTRO_TARJETA = setOf("tarjetaNumero", "tarjetaExpiracion", "tarjetaCvv", "tarjetaTitular")
private val RUTAS_CLIENTE = setOf("home", "carrito", "historialCompras", "detallePedido", "tarjetas") +
    RUTAS_COMPRA + RUTAS_REGISTRO_TARJETA

/** Navegar o restaurar una ruta nunca omite los pasos previos ni fabrica una confirmación. */
internal fun rutaCompraSegura(ruta: String, entregaRevisada: Boolean, direccionConfirmada: Boolean,
    pedidoConfirmado: Boolean, compraPendiente: Boolean, digitalActivo: Boolean = false): String {
    if (ruta !in RUTAS_COMPRA) return ruta
    if (pedidoConfirmado) return "confirmacionPedido"
    if (compraPendiente) return "efectivo"
    if (ruta in RUTAS_PAGO_DIGITAL && digitalActivo) return ruta
    if (ruta in RUTAS_PAGO_DIGITAL) return "datosEntrega"
    if (ruta == "checkout" || ruta == "confirmacionPedido") return "datosEntrega"
    if (ruta != "datosEntrega" && !entregaRevisada) return "datosEntrega"
    if (ruta !in setOf("datosEntrega", "seleccionDireccion") && !direccionConfirmada) return "seleccionDireccion"
    return ruta
}

/** Recuperación financiera separada de la de efectivo y protegida por las guardias de rol previas. */
internal fun rutaPagoDigitalSegura(ruta: String, estadoPago: String?, pedidoDigitalConfirmado: Boolean,
    iniciando: Boolean = false, error: Boolean = false): String {
    if (ruta !in RUTAS_CLIENTE) return ruta
    if (pedidoDigitalConfirmado && ruta in RUTAS_COMPRA) return "confirmacionPedido"
    if (estadoPago == "CONFIRMADO") return "confirmacionPago"
    if (estadoPago in listOf("PENDIENTE", "RECHAZADO")) return "pagoSimulado"
    if (iniciando) return "pagoSimulado"
    if (ruta in RUTAS_PAGO_DIGITAL) return if (iniciando || error) "pagoSimulado" else "metodoPago"
    return ruta
}

internal fun rutaRegistroTarjetaSegura(ruta: String, ultimos4: String, marca: String,
    mes: String, anio: String, cvvCompletado: Boolean): String {
    if (ruta !in RUTAS_REGISTRO_TARJETA || ruta == "tarjetaNumero") return ruta
    if (!Regex("[0-9]{4}").matches(ultimos4) || marca.isBlank()) return "tarjetaNumero"
    if (ruta != "tarjetaExpiracion" && !vencimientoTarjetaSimuladaValido(mes, anio)) return "tarjetaExpiracion"
    if (ruta == "tarjetaTitular" && !cvvCompletado) return "tarjetaCvv"
    return ruta
}
private val RUTAS_ADMIN = setOf("admin", "agregarProducto", "editarProducto", "pedidosAdmin", "detallePedidoAdmin", "saboresAdmin", "agregarSabor", "editarSabor")
private val RUTAS_COMPARTIDAS = setOf("catalogo", "detalle")

internal fun claveSesionNavegacion(usuario: Usuario?): String? {
    if (usuario == null || usuario.id.isBlank()) return null
    val rol = usuario.rol.uppercase(java.util.Locale.ROOT)
    return if (rol == "ADMIN" || rol == "CLIENTE") "${usuario.id}:$rol" else null
}

private fun pantallaInicialSesion(usuario: Usuario?): String = when {
    claveSesionNavegacion(usuario) == null -> "login"
    usuario?.rol.equals("ADMIN", true) -> "admin"
    else -> "home"
}

/** Una ruta guardada no autentica al usuario ni conserva permisos por sí sola. */
internal fun pantallaRestauradaSegura(pantalla: String, usuario: Usuario?, propietario: String?): String {
    if (pantalla in RUTAS_PUBLICAS) return pantalla
    val claveActual = claveSesionNavegacion(usuario) ?: return "login"
    if (propietario != claveActual) return pantallaInicialSesion(usuario)
    val permitida = pantalla in RUTAS_COMPARTIDAS ||
        (usuario?.rol.equals("ADMIN", true) && pantalla in RUTAS_ADMIN) ||
        (usuario?.rol.equals("CLIENTE", true) && pantalla in RUTAS_CLIENTE)
    return if (permitida) pantalla else pantallaInicialSesion(usuario)
}

@Composable
private fun ProductoDeNavegacion(
    usuario: Usuario?, id: String, productoViewModel: ProductoViewModel,
    volver: () -> Unit, contenido: @Composable (Producto) -> Unit
) {
    val claveSesion = claveSesionNavegacion(usuario)
    LaunchedEffect(claveSesion, id) {
        if (claveSesion != null) productoViewModel.recuperarProductoParaRuta(claveSesion, id)
    }
    val producto = productoViewModel.productoParaRuta
    when {
        claveSesion == null || id.isBlank() -> SituacionProducto("No se pudo recuperar la selección de esta sesión.", volver)
        productoViewModel.propietarioProductoRuta == claveSesion && producto?.id == id -> contenido(producto)
        productoViewModel.errorProductoRuta != null && productoViewModel.propietarioProductoRuta == claveSesion ->
            SituacionProducto(productoViewModel.errorProductoRuta.orEmpty(), volver)
        else -> VapeON_MovilTheme {
            BackHandler(onBack = volver)
            Surface(color = VapeOnBackground, modifier = Modifier.fillMaxSize()) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator()
                    Text("Recuperando producto…")
                    Button(onClick = volver) { Text("Volver") }
                }
            }
        }
    }
}

@Composable
private fun SituacionProducto(mensaje: String, volver: () -> Unit) {
    BackHandler(onBack = volver)
    VapeON_MovilTheme {
        Surface(color = VapeOnBackground, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(mensaje, color = MaterialTheme.colorScheme.error)
                Button(onClick = volver) { Text("Volver") }
            }
        }
    }
}
