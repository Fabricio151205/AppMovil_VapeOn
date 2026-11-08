package com.example.vapeon_movil

import com.example.vapeon_movil.entities.*
import com.example.vapeon_movil.services.*
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.viewmodels.FidelidadViewModel
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import retrofit2.HttpException
import java.io.IOException
import com.example.vapeon_movil.viewmodels.TipoErrorFidelidad
import com.example.vapeon_movil.viewmodels.clasificarErrorFidelidad

class FidelidadTest {
    private val ruta = RUTA_DOCUMENTOS_FIRESTORE
    private val instante = "2026-11-07T12:00:00Z"

    private fun pedido(
        id: String = "pedido1",
        usuarioId: String = "cliente1",
        cantidad: Int = 1,
        estado: String = "ENTREGADO"
    ): Pedido {
        val precio = BigDecimal("10.00")
        return Pedido(
            id, usuarioId, "Cliente", "987654321", instante,
            listOf(ProductoPedido("p1", "Producto", "Marca", cantidad, precio, precio * BigDecimal(cantidad))),
            precio * BigDecimal(cantidad), BigDecimal.ZERO.setScale(2),
            precio * BigDecimal(cantidad), precio * BigDecimal(cantidad), BigDecimal.ZERO.setScale(2),
            "Dirección", estado = estado
        )
    }

    private fun config(nombre: String = "Oxbar") = ConfiguracionFidelidad(
        "producto", nombre, "Marca", "Kit", BigDecimal("50.00"), "admin", instante, "config-v1"
    )

    @Test fun unPedidoDaUnPuntoSinImportarCantidad() {
        listOf(1, 10, 20).forEach { cantidad ->
            val resultado = procesarPedidoParaFidelidad(pedido(cantidad = cantidad), null, false, config(), instante)
            assertEquals(1, resultado!!.fidelidadNueva.puntosActuales)
            assertEquals("ACREDITADO", resultado.evento!!.resultado)
        }
    }

    @Test fun soloEntregadoGeneraPunto() {
        assertNull(procesarPedidoParaFidelidad(pedido(estado = "REGISTRADO"), null, false, config(), instante))
        assertNull(procesarPedidoParaFidelidad(pedido(estado = "EN CAMINO"), null, false, config(), instante))
        assertNull(procesarPedidoParaFidelidad(pedido().copy(productos = emptyList()), null, false, config(), instante))
        assertEquals(1, procesarPedidoParaFidelidad(pedido(), null, false, config(), instante)!!.fidelidadNueva.puntosActuales)
    }

    @Test fun eventoExistenteHaceIdempotenteCualquierRepeticion() {
        var estado = procesarPedidoParaFidelidad(
            pedido(), FidelidadCliente("cliente1", fechaActualizacion = instante),
            false, config(), instante
        )!!.fidelidadNueva
        repeat(9) {
            val resultado = procesarPedidoParaFidelidad(pedido(), estado, true, config(), instante)!!
            estado = resultado.fidelidadNueva
            assertTrue(resultado.yaProcesado)
        }
        assertEquals(1, estado.puntosActuales)
    }

    @Test fun cicloLlegaASeisYNuncaSiete() {
        val cinco = FidelidadCliente("cliente1", 5, 1, null, instante)
        val sexto = procesarPedidoParaFidelidad(pedido(), cinco, false, config(), instante)!!
        assertEquals(6, sexto.fidelidadNueva.puntosActuales)
        assertEquals(EstadoRecompensaFidelidad.PENDIENTE, sexto.recompensaNueva!!.estado)
        val bloqueado = procesarPedidoParaFidelidad(
            pedido("pedido7"), sexto.fidelidadNueva, false, config(), instante
        )!!
        assertEquals(6, bloqueado.fidelidadNueva.puntosActuales)
        assertEquals("OMITIDO_RECOMPENSA_PENDIENTE", bloqueado.evento!!.resultado)
        assertNull(bloqueado.recompensaNueva)
    }

    @Test fun sextoPuntoSeConservaSinProductoConfigurado() {
        val resultado = procesarPedidoParaFidelidad(
            pedido(), FidelidadCliente("cliente1", 5, 1, null, instante), false, null, instante
        )!!
        assertEquals(6, resultado.fidelidadNueva.puntosActuales)
        assertNotNull(resultado.fidelidadNueva.recompensaPendienteId)
        assertNull(resultado.recompensaNueva!!.productoId)
    }

    @Test fun entregaReiniciaCicloYSiguientePedidoEmpiezaEnUno() {
        val estado = FidelidadCliente("cliente1", 6, 1, "cliente1_ciclo_1", instante)
        val recompensa = RecompensaFidelidad(
            "cliente1_ciclo_1", "cliente1", 1, EstadoRecompensaFidelidad.PENDIENTE,
            fechaGeneracion = instante
        )
        val entrega = prepararEntregaRecompensa(estado, recompensa, config(), "admin", instante)
        assertEquals(0, entrega.fidelidad.puntosActuales)
        assertEquals(2, entrega.fidelidad.numeroCiclo)
        assertNull(entrega.fidelidad.recompensaPendienteId)
        assertEquals(EstadoRecompensaFidelidad.ENTREGADA, entrega.recompensa.estado)
        val siguiente = procesarPedidoParaFidelidad(
            pedido("pedido7"), entrega.fidelidad, false, config(), instante
        )!!
        assertEquals(1, siguiente.fidelidadNueva.puntosActuales)
        assertEquals(2, siguiente.evento!!.numeroCiclo)
    }

    @Test fun usuariosYCiclosSonIndependientes() {
        val a = procesarPedidoParaFidelidad(pedido(usuarioId = "a"), null, false, config(), instante)!!
        val b = procesarPedidoParaFidelidad(pedido("pedidoB", "b"), null, false, config(), instante)!!
        assertEquals("a", a.fidelidadNueva.usuarioId)
        assertEquals("b", b.fidelidadNueva.usuarioId)
        assertEquals(1, a.fidelidadNueva.puntosActuales)
        assertEquals(1, b.fidelidadNueva.puntosActuales)
    }

    @Test fun rankingExcluyeAdminYOrdenaSeisACeroConDesempateEstable() {
        val datos = (0..6).map { puntos ->
            ClienteFidelidad(
                Usuario("c$puntos", "Cliente$puntos", rol = "CLIENTE"),
                FidelidadCliente("c$puntos", puntos, fechaActualizacion = instante)
            )
        } + ClienteFidelidad(
            Usuario("admin", "Admin", rol = "ADMIN"),
            FidelidadCliente("admin", 6, fechaActualizacion = instante)
        )
        assertEquals((6 downTo 0).map { "c$it" }, ordenarClientesPorFidelidad(datos).map { it.usuario.id })
    }

    @Test fun snapshotHistoricoNoCambiaConConfiguracionPosteriorOProductoEliminado() {
        val generado = procesarPedidoParaFidelidad(
            pedido(), FidelidadCliente("cliente1", 5, fechaActualizacion = instante), false,
            config("Oxbar"), instante
        )!!.recompensaNueva!!
        val nuevaConfiguracion = config("Nexa")
        assertEquals("Oxbar", generado.nombreProducto)
        assertEquals("Nexa", nuevaConfiguracion.nombre)
        assertEquals("producto", generado.productoId)
        val entrega = prepararEntregaRecompensa(
            FidelidadCliente("cliente1", 6, 1, generado.id, instante),
            generado,
            config("Nexa"),
            "admin",
            instante
        )
        assertEquals("Oxbar", entrega.recompensa.nombreProducto)
        val entregaSinConfiguracionActual = prepararEntregaRecompensa(
            FidelidadCliente("cliente1", 6, 1, generado.id, instante),
            generado,
            null,
            "admin",
            instante
        )
        assertEquals("Oxbar", entregaSinConfiguracionActual.recompensa.nombreProducto)
    }

    @Test fun commitDeAcreditacionIncluyePedidoEventoEstadoYRecompensaConPrecondiciones() = runBlocking {
        val fake = FidelidadFake(configuracionDoc = config().aDocumento(ruta).copy(updateTime = "c1"))
        val original = pedido(estado = "REGISTRADO").aDocumento(ruta).copy(updateTime = "p1")
        fake.fidelidadDoc = FidelidadCliente("cliente1", 5, fechaActualizacion = instante)
            .aDocumento(ruta).copy(updateTime = "f1")
        acreditarEntregaAtomica(fake, original, pedido())
        val writes = fake.ultimoCommit!!.writes
        assertEquals(4, writes.size)
        assertEquals(listOf("estado"), writes[0].updateMask!!.fieldPaths)
        assertEquals("p1", writes[0].currentDocument.updateTime)
        assertTrue(writes[1].update!!.name.endsWith("/eventosFidelidad/pedido1"))
        assertEquals(false, writes[1].currentDocument.exists)
        assertEquals("f1", writes[2].currentDocument.updateTime)
        assertEquals(false, writes[3].currentDocument.exists)
    }

    @Test fun eventoRemotoExistenteEvitaSegundoCommitYSegundoPunto() = runBlocking {
        val evento = EventoFidelidad("pedido1", "cliente1", 1, instante, instante, "ACREDITADO")
        val fake = FidelidadFake(eventoDoc = evento.aDocumento(ruta))
        val original = pedido(estado = "REGISTRADO").aDocumento(ruta).copy(updateTime = "p1")
        val resultado = acreditarEntregaAtomica(fake, original, pedido())
        assertTrue(resultado.yaProcesado)
        assertFalse(resultado.eventoCreado)
        assertEquals(0, fake.llamadasCommit)
    }

    @Test fun dosEntregasConMismaVersionSoloPermitenUna() = runBlocking {
        val estado = FidelidadCliente("cliente1", 6, 1, "cliente1_ciclo_1", instante)
        val recompensa = RecompensaFidelidad(
            "cliente1_ciclo_1", "cliente1", 1, EstadoRecompensaFidelidad.PENDIENTE,
            fechaGeneracion = instante
        )
        val fake = FidelidadFake(
            configuracionDoc = config().aDocumento(ruta).copy(updateTime = "c1"),
            fidelidadDoc = estado.aDocumento(ruta).copy(updateTime = "f1"),
            recompensaDoc = recompensa.aDocumento(ruta).copy(updateTime = "r1"),
            rechazarSegundoCommit = true
        )
        entregarRecompensaAtomica(fake, "cliente1", "admin", instante, config())
        assertThrows(IllegalStateException::class.java) {
            runBlocking { entregarRecompensaAtomica(fake, "cliente1", "admin2", instante, config()) }
        }
        assertEquals(2, fake.llamadasCommit)
        assertEquals(1, fake.commitsAceptados.size)
        val primero = fake.commitsAceptados.single().writes
        assertEquals("f1", primero[0].currentDocument.updateTime)
        assertEquals("r1", primero[1].currentDocument.updateTime)
        assertEquals("0", primero[0].update!!.fields["puntosActuales"]?.integerValue)
        assertEquals("ENTREGADA", primero[1].update!!.fields["estado"]?.stringValue)
    }

    @Test fun dosPedidosConcurrentesNoPierdenSilenciosamenteElSegundoPunto() = runBlocking {
        val estado = FidelidadCliente("cliente1", 2, fechaActualizacion = instante)
        val fake = FidelidadFake(
            fidelidadDoc = estado.aDocumento(ruta).copy(updateTime = "f1"),
            rechazarSegundoCommit = true
        )
        val originalA = pedido("pedidoA", estado = "REGISTRADO").aDocumento(ruta).copy(updateTime = "pA")
        val originalB = pedido("pedidoB", estado = "REGISTRADO").aDocumento(ruta).copy(updateTime = "pB")

        acreditarEntregaAtomica(fake, originalA, pedido("pedidoA"))
        assertThrows(IllegalStateException::class.java) {
            runBlocking { acreditarEntregaAtomica(fake, originalB, pedido("pedidoB")) }
        }

        assertEquals(1, fake.commitsAceptados.size)
        val aceptado = fake.commitsAceptados.single().writes
        assertTrue(aceptado.any { it.update.name.endsWith("/eventosFidelidad/pedidoA") })
        assertFalse(aceptado.any { it.update.name.endsWith("/eventosFidelidad/pedidoB") })
        assertEquals("3", aceptado.first { it.update.name.endsWith("/fidelidades/cliente1") }
            .update.fields["puntosActuales"]?.integerValue)
    }

    @Test fun configuracionCambiadaImpideCompletarRecompensaConOtroProducto() = runBlocking {
        val estado = FidelidadCliente("cliente1", 6, 1, "cliente1_ciclo_1", instante)
        val recompensa = RecompensaFidelidad(
            "cliente1_ciclo_1", "cliente1", 1, EstadoRecompensaFidelidad.PENDIENTE,
            fechaGeneracion = instante
        )
        val seleccionOxbar = config("Oxbar").copy(updateTime = "config-oxbar")
        val fake = FidelidadFake(
            fidelidadDoc = estado.aDocumento(ruta).copy(updateTime = "f1"),
            recompensaDoc = recompensa.aDocumento(ruta).copy(updateTime = "r1"),
            versionConfiguracionActual = "config-nexa"
        )

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                entregarRecompensaAtomica(
                    fake, "cliente1", "admin", instante, seleccionOxbar
                )
            }
        }
        assertTrue(fake.commitsAceptados.isEmpty())
        assertEquals(EstadoRecompensaFidelidad.PENDIENTE, recompensa.estado)
        assertEquals(6, estado.puntosActuales)
        val verificacion = fake.commits.single().writes.single {
            it.update.name.endsWith("/configuracionFidelidad/actual")
        }
        assertEquals("config-oxbar", verificacion.currentDocument.updateTime)
        assertEquals("Oxbar", verificacion.update.fields["nombre"]?.stringValue)
    }

    @Test fun recompensaConSnapshotIgnoraConfiguracionPosterior() = runBlocking {
        val estado = FidelidadCliente("cliente1", 6, 1, "cliente1_ciclo_1", instante)
        val recompensaOxbar = RecompensaFidelidad(
            "cliente1_ciclo_1", "cliente1", 1, EstadoRecompensaFidelidad.PENDIENTE,
            productoId = "oxbar-id", nombreProducto = "Oxbar", marcaProducto = "Oxbar",
            categoriaProducto = "Kit", precioReferencia = BigDecimal("50.00"),
            fechaGeneracion = instante
        )
        val fake = FidelidadFake(
            fidelidadDoc = estado.aDocumento(ruta).copy(updateTime = "f1"),
            recompensaDoc = recompensaOxbar.aDocumento(ruta).copy(updateTime = "r1"),
            versionConfiguracionActual = "config-nexa"
        )

        val entrega = entregarRecompensaAtomica(
            fake, "cliente1", "admin", instante,
            config("Nexa").copy(updateTime = "config-anterior")
        )

        assertEquals("Oxbar", entrega.recompensa.nombreProducto)
        assertNull(entrega.configuracionUpdateTime)
        assertFalse(fake.commitsAceptados.single().writes.any {
            it.update.name.endsWith("/configuracionFidelidad/actual")
        })
    }

    @Test fun clasificaConflictoRedYErrorDesconocidoSinExponerHttp() {
        val conflicto = HttpException(Response.error<Any>(412, ResponseBody.create(null, "")))
        val precondicionFirestore = HttpException(Response.error<Any>(400, ResponseBody.create(
            null, "{\"error\":{\"status\":\"FAILED_PRECONDITION\"}}"
        )))
        assertEquals(TipoErrorFidelidad.CONFLICTO_VERSION, clasificarErrorFidelidad(conflicto))
        assertEquals(TipoErrorFidelidad.CONFLICTO_VERSION, clasificarErrorFidelidad(precondicionFirestore))
        assertEquals(TipoErrorFidelidad.ERROR_RED, clasificarErrorFidelidad(IOException("sin red")))
        assertEquals(TipoErrorFidelidad.ERROR_DESCONOCIDO, clasificarErrorFidelidad(IllegalStateException()))
    }

    @Test fun clienteSoloConsultaSuDocumentoYAdminEsValidado() {
        val fake = FidelidadFake()
        val usuarios = UsuariosFake()
        val vm = FidelidadViewModel(fake, usuarios, Dispatchers.Unconfined)
        vm.cargarMiFidelidad(Usuario(id = "clienteA", rol = "CLIENTE"))
        assertEquals(listOf("clienteA"), fake.idsFidelidad)
        vm.cargarMiFidelidad(Usuario(id = "admin", rol = "ADMIN"))
        assertNotNull(vm.error)
        assertEquals(listOf("clienteA"), fake.idsFidelidad)
    }

    @Test fun clienteCargaSnapshotDeSuRecompensaPendiente() {
        val estado = FidelidadCliente("clienteA", 6, 1, "r1", instante)
        val recompensa = RecompensaFidelidad(
            "r1", "clienteA", 1, EstadoRecompensaFidelidad.PENDIENTE,
            productoId = "p1", nombreProducto = "Producto A", marcaProducto = "Marca A",
            categoriaProducto = "Kit", precioReferencia = BigDecimal("50.00"),
            fechaGeneracion = instante
        )
        val fake = FidelidadFake(
            fidelidadDoc = estado.aDocumento(ruta),
            recompensaDoc = recompensa.aDocumento(ruta)
        )
        val vm = FidelidadViewModel(fake, UsuariosFake(), Dispatchers.Unconfined)

        vm.cargarMiFidelidad(Usuario(id = "clienteA", rol = "CLIENTE"))

        assertEquals(6, vm.miFidelidad?.puntosActuales)
        assertEquals("Producto A", vm.miRecompensa?.nombreProducto)
        assertEquals("Marca A", vm.miRecompensa?.marcaProducto)
        assertEquals(listOf("r1"), fake.idsRecompensa)
    }

    @Test fun recompensaSinProductoSeConservaSinInventarSnapshot() {
        val estado = FidelidadCliente("clienteA", 6, 1, "r1", instante)
        val recompensa = RecompensaFidelidad(
            "r1", "clienteA", 1, EstadoRecompensaFidelidad.PENDIENTE,
            fechaGeneracion = instante
        )
        val vm = FidelidadViewModel(
            FidelidadFake(
                fidelidadDoc = estado.aDocumento(ruta),
                recompensaDoc = recompensa.aDocumento(ruta)
            ),
            UsuariosFake(),
            Dispatchers.Unconfined
        )

        vm.cargarMiFidelidad(Usuario(id = "clienteA", rol = "CLIENTE"))

        assertEquals(6, vm.miFidelidad?.puntosActuales)
        assertNotNull(vm.miRecompensa)
        assertNull(vm.miRecompensa?.productoId)
        assertNull(vm.miRecompensa?.nombreProducto)
        assertNull(vm.miRecompensa?.marcaProducto)
    }

    @Test fun nuevoCicloSinPendienteNoConsultaNiConservaRecompensa() {
        val estado = FidelidadCliente("clienteA", 0, 2, null, instante)
        val fake = FidelidadFake(fidelidadDoc = estado.aDocumento(ruta))
        val vm = FidelidadViewModel(fake, UsuariosFake(), Dispatchers.Unconfined)

        vm.cargarMiFidelidad(Usuario(id = "clienteA", rol = "CLIENTE"))

        assertEquals(0, vm.miFidelidad?.puntosActuales)
        assertEquals(2, vm.miFidelidad?.numeroCiclo)
        assertNull(vm.miRecompensa)
        assertTrue(fake.idsRecompensa.isEmpty())
    }

    @Test fun errorDeRecompensaConservaElProgresoSeisDeSeis() {
        val estado = FidelidadCliente("clienteA", 6, 1, "r1", instante)
        val fake = FidelidadFake(
            fidelidadDoc = estado.aDocumento(ruta),
            fallarRecompensa = true
        )
        val vm = FidelidadViewModel(fake, UsuariosFake(), Dispatchers.Unconfined)

        vm.cargarMiFidelidad(Usuario(id = "clienteA", rol = "CLIENTE"))

        assertEquals(6, vm.miFidelidad?.puntosActuales)
        assertNull(vm.miRecompensa)
        assertNotNull(vm.errorRecompensa)
        assertNull(vm.error)
    }

    @Test fun administracionCargaSoloClientesYOrdenaPorPuntos() {
        val estado = FidelidadCliente("clienteA", 5, fechaActualizacion = instante)
        val usuarios = UsuariosFake(listOf(
            usuarioDocumento("clienteB", "CLIENTE", "Bea"),
            usuarioDocumento("admin", "ADMIN", "Admin"),
            usuarioDocumento("clienteA", "CLIENTE", "Ana")
        ))
        val fake = FidelidadFake(fidelidadDoc = estado.aDocumento(ruta))
        val vm = FidelidadViewModel(fake, usuarios, Dispatchers.Unconfined)
        vm.cargarAdmin(Usuario(id = "admin", rol = "ADMIN"))
        assertEquals(listOf("clienteA", "clienteB"), vm.clientes.map { it.usuario.id })
        assertFalse(vm.clientes.any { it.usuario.rol == "ADMIN" })

        vm.cargarAdmin(Usuario(id = "clienteA", rol = "CLIENTE"))
        assertNotNull(vm.error)
    }

    private fun usuarioDocumento(id: String, rol: String, nombre: String) = UsuarioDocumento(
        "usuarios/$id",
        UsuarioCampos(
            nombre = FirestoreStringValue(nombre),
            correo = FirestoreStringValue("$id@example.com"),
            rol = FirestoreStringValue(rol)
        )
    )

    private class FidelidadFake(
        var configuracionDoc: FidelidadDocumento? = null,
        var fidelidadDoc: FidelidadDocumento? = null,
        var recompensaDoc: FidelidadDocumento? = null,
        var eventoDoc: FidelidadDocumento? = null,
        val rechazarSegundoCommit: Boolean = false,
        var versionConfiguracionActual: String? = null,
        val fallarRecompensa: Boolean = false
    ) : FidelidadService {
        var ultimoCommit: FidelidadCommit? = null
        val commits = mutableListOf<FidelidadCommit>()
        val commitsAceptados = mutableListOf<FidelidadCommit>()
        var llamadasCommit = 0
        val idsFidelidad = mutableListOf<String>()
        val idsRecompensa = mutableListOf<String>()
        private fun respuesta(doc: FidelidadDocumento?): Response<FidelidadDocumento> =
            doc?.let { Response.success(it) }
                ?: Response.error(404, ResponseBody.create(null, ""))
        override suspend fun configuracion() = respuesta(configuracionDoc)
        override suspend fun fidelidad(usuarioId: String): Response<FidelidadDocumento> {
            idsFidelidad += usuarioId
            return respuesta(fidelidadDoc?.takeIf { it.name.endsWith("/$usuarioId") })
        }
        override suspend fun evento(pedidoId: String) = respuesta(eventoDoc?.takeIf { it.name.endsWith("/$pedidoId") })
        override suspend fun recompensa(id: String): Response<FidelidadDocumento> {
            idsRecompensa += id
            return if (fallarRecompensa) {
                Response.error(500, ResponseBody.create(null, ""))
            } else respuesta(recompensaDoc?.takeIf { it.name.endsWith("/$id") })
        }
        override suspend fun listarFidelidades(pageToken: String?) = FidelidadLista(listOfNotNull(fidelidadDoc))
        override suspend fun commit(body: FidelidadCommit): FidelidadCommitRespuesta {
            llamadasCommit++
            commits += body
            ultimoCommit = body
            if (rechazarSegundoCommit && llamadasCommit > 1) error("conflicto de versión")
            body.writes.firstOrNull {
                it.update.name.endsWith("/configuracionFidelidad/actual")
            }?.let { verificacion ->
                if (versionConfiguracionActual != null &&
                    verificacion.currentDocument.updateTime != versionConfiguracionActual) {
                    error("conflicto de versión")
                }
            }
            commitsAceptados += body
            return FidelidadCommitRespuesta(
                "2026-11-07T12:00:00Z",
                body.writes.map { FidelidadResultadoEscritura("version-nueva") }
            )
        }
    }

    private class UsuariosFake(private val documentos: List<UsuarioDocumento> = emptyList()) : UsuarioService {
        override suspend fun listarUsuarios(pageToken: String?) = UsuarioListResponse(documentos)
        override suspend fun mostrarUsuario(id: String): UsuarioDocumento = error("No se usa")
        override suspend fun crearUsuario(body: UsuarioContainer, documentId: String?): Any = error("No se usa")
        override suspend fun actualizarUsuario(id: String, body: UsuarioContainer): UsuarioDocumento = error("No se usa")
        override suspend fun eliminarUsuario(id: String): Any = error("No se usa")
    }
}
