package com.example.vapeon_movil

import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.services.UsuarioRolesService
import com.example.vapeon_movil.services.UsuarioService
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.viewmodels.RolesViewModel
import com.example.vapeon_movil.viewmodels.filtrarUsuarios
import com.example.vapeon_movil.viewmodels.ordenarUsuarios
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RolesTest {
    private val admin = Usuario(id = "admin", rol = "ADMIN")
    private val cliente = Usuario(id = "cliente", rol = "CLIENTE")

    private fun doc(
        id: String,
        nombre: String = "Nombre$id",
        apellido: String = "Apellido$id",
        correo: String = "$id@correo.com",
        dni: String? = null,
        rol: String = "CLIENTE",
        createTime: String? = "2026-01-01T00:00:00Z",
        updateTime: String? = "2026-01-02T00:00:00Z"
    ) = UsuarioDocumento(
        name = "usuarios/$id",
        fields = UsuarioCampos(
            nombre = FirestoreStringValue(nombre), apellido = FirestoreStringValue(apellido),
            correo = FirestoreStringValue(correo), dni = dni?.let(::FirestoreStringValue),
            rol = FirestoreStringValue(rol)
        ),
        createTime = createTime,
        updateTime = updateTime
    )

    private class UsuariosFake(
        val paginas: Map<String?, UsuarioListResponse> = mapOf(null to UsuarioListResponse(emptyList())),
        val falla: Boolean = false
    ) : UsuarioService {
        val tokens = mutableListOf<String?>()
        override suspend fun listarUsuarios(pageToken: String?): UsuarioListResponse {
            tokens += pageToken
            if (falla) error("red")
            return paginas[pageToken] ?: UsuarioListResponse(emptyList())
        }
        override suspend fun mostrarUsuario(id: String): UsuarioDocumento = error("No se usa")
        override suspend fun crearUsuario(body: UsuarioContainer, documentId: String?): Any = error("No se usa")
        override suspend fun actualizarUsuario(id: String, body: UsuarioContainer): UsuarioDocumento = error("No se usa")
        override suspend fun eliminarUsuario(id: String): Any = error("No se usa")
    }

    private class RolesFake(
        var respuesta: UsuarioDocumento? = null,
        var fallo: Throwable? = null
    ) : UsuarioRolesService {
        var llamadas = 0
        var id: String? = null
        var campo: String? = null
        var version: String? = null
        var body: UsuarioContainer? = null
        override suspend fun actualizarRol(id: String, campo: String, updateTime: String,
            body: UsuarioContainer): UsuarioDocumento {
            llamadas++
            this.id = id; this.campo = campo; version = updateTime; this.body = body
            fallo?.let { throw it }
            return respuesta ?: UsuarioDocumento("usuarios/$id", body.fields,
                "2026-01-01T00:00:00Z", "2026-01-03T00:00:00Z")
        }
    }

    private fun vm(usuarios: UsuariosFake, roles: RolesFake = RolesFake()) =
        RolesViewModel(usuarios, roles, Dispatchers.Unconfined)

    @Test fun adminCargaTodasLasPaginasYDeduplicaMientrasClienteEsRechazado() {
        val servicio = UsuariosFake(mapOf(
            null to UsuarioListResponse(listOf(doc("1")), "dos"),
            "dos" to UsuarioListResponse(listOf(doc("1"), doc("2")))
        ))
        val modelo = vm(servicio)
        modelo.cargarUsuarios(admin)
        assertEquals(listOf(null, "dos"), servicio.tokens)
        assertEquals(listOf("1", "2"), modelo.usuarios.map { it.id })
        val bloqueado = vm(UsuariosFake())
        bloqueado.cargarUsuarios(cliente)
        assertTrue(bloqueado.error!!.contains("administrador"))
    }

    @Test fun listaVaciaYErrorDeServicioSeExponenSinDatosFicticios() {
        val vacio = vm(UsuariosFake()); vacio.cargarUsuarios(admin)
        assertTrue(vacio.usuarios.isEmpty())
        val error = vm(UsuariosFake(falla = true)); error.cargarUsuarios(admin)
        assertTrue(error.usuarios.isEmpty()); assertNotNull(error.error)
    }

    @Test fun ordenaPorCreateTimeAscendenteYDejaSinFechaAlFinalSinInventarla() {
        val usuarios = listOf(
            Usuario(id = "sin", createTime = null),
            Usuario(id = "nuevo", createTime = "2026-02-01T00:00:00Z"),
            Usuario(id = "antiguo", createTime = "2026-01-01T00:00:00Z")
        )
        val ordenados = ordenarUsuarios(usuarios)
        assertEquals(listOf("antiguo", "nuevo", "sin"), ordenados.map { it.id })
        assertNull(ordenados.last().createTime)
    }

    @Test fun paginaEnBloquesDeDiezConPrimeraAnteriorSiguienteYUltima() {
        val docs = (1..23).map { doc(it.toString(), createTime = "2026-01-${it.toString().padStart(2, '0')}T00:00:00Z") }
        val modelo = vm(UsuariosFake(mapOf(null to UsuarioListResponse(docs))))
        modelo.cargarUsuarios(admin)
        assertEquals(3, modelo.totalPaginas); assertEquals(10, modelo.usuariosPagina.size)
        modelo.paginaAnterior(); assertEquals(0, modelo.paginaActual)
        modelo.paginaSiguiente(); assertEquals(1, modelo.paginaActual)
        modelo.ultimaPagina(); assertEquals(2, modelo.paginaActual); assertEquals(3, modelo.usuariosPagina.size)
        modelo.paginaSiguiente(); assertEquals(2, modelo.paginaActual)
        modelo.primeraPagina(); assertEquals(0, modelo.paginaActual)
    }

    @Test fun buscaNombreApellidoNombreCompletoCorreoYDniConContainsSinDistinguirCaso() {
        val usuarios = listOf(Usuario(id = "1", nombre = " Juan Carlos ", apellido = "PÉREZ",
            correo = "Juan@Gmail.com", dni = "12345678"), Usuario(id = "2", nombre = "Ana"))
        listOf("juan", "carlos", "pérez", "juan car", "GMAIL", "3456", "  JUAN  ").forEach {
            assertEquals("1", filtrarUsuarios(usuarios, it).single().id)
        }
        assertTrue(filtrarUsuarios(usuarios, "99999999").isEmpty())
        assertTrue(filtrarUsuarios(listOf(Usuario(id = "x", dni = null)), "123").isEmpty())
    }

    @Test fun cambiarBusquedaRegresaALaPrimeraPagina() {
        val modelo = vm(UsuariosFake(mapOf(null to UsuarioListResponse((1..12).map { doc("$it") }))))
        modelo.cargarUsuarios(admin); modelo.paginaSiguiente(); assertEquals(1, modelo.paginaActual)
        modelo.cambiarBusqueda("Nombre"); assertEquals(0, modelo.paginaActual)
    }

    @Test fun clientePuedePromoverseYAdminPuedeDegradarseConPatchExclusivo() {
        val usuarios = UsuariosFake(mapOf(null to UsuarioListResponse(listOf(doc("u")))))
        val roles = RolesFake().apply { respuesta = doc("u", rol = "ADMIN", updateTime = "v2") }
        val modelo = vm(usuarios, roles); modelo.cargarUsuarios(admin)
        modelo.cambiarRol(admin, "u", "ADMIN")
        assertEquals(1, roles.llamadas); assertEquals("rol", roles.campo)
        assertEquals("2026-01-02T00:00:00Z", roles.version)
        val campos = roles.body!!.fields
        assertEquals("ADMIN", campos.rol!!.stringValue)
        assertNull(campos.nombre); assertNull(campos.correo); assertNull(campos.password); assertNull(campos.dni)
        assertEquals("ADMIN", modelo.usuarios.single().rol)

        roles.respuesta = doc("u", rol = "CLIENTE", updateTime = "v3")
        modelo.cambiarRol(admin, "u", "CLIENTE")
        assertEquals("CLIENTE", modelo.usuarios.single().rol)
    }

    @Test fun rolInvalidoYDocumentoSinVersionSonRechazadosSinPeticion() {
        val roles = RolesFake()
        val modelo = vm(UsuariosFake(mapOf(null to UsuarioListResponse(listOf(doc("u", updateTime = null))))), roles)
        modelo.cargarUsuarios(admin)
        modelo.cambiarRol(admin, "u", "SUPERADMIN"); assertEquals(0, roles.llamadas)
        modelo.cambiarRol(admin, "u", "ADMIN"); assertEquals(0, roles.llamadas)
        assertNotNull(modelo.error)
    }

    @Test fun adminActualNoPuedeAutoDegradarse() {
        val roles = RolesFake()
        val modelo = vm(UsuariosFake(mapOf(null to UsuarioListResponse(listOf(doc("admin", rol = "ADMIN"))))), roles)
        modelo.cargarUsuarios(admin); modelo.cambiarRol(admin, "admin", "CLIENTE")
        assertEquals(0, roles.llamadas); assertEquals("ADMIN", modelo.usuarios.single().rol)
        assertTrue(modelo.error!!.contains("propio"))
    }

    @Test fun conflictoYErrorDeRedNoCambianEstadoLocal() {
        val roles = RolesFake()
        val modelo = vm(UsuariosFake(mapOf(null to UsuarioListResponse(listOf(doc("u"))))), roles)
        modelo.cargarUsuarios(admin)
        roles.fallo = HttpException(retrofit2.Response.error<Any>(412,
            ResponseBody.create(MediaType.parse("application/json"), "{}")))
        modelo.cambiarRol(admin, "u", "ADMIN")
        assertTrue(modelo.conflicto); assertEquals("CLIENTE", modelo.usuarios.single().rol)
        roles.fallo = java.io.IOException("red")
        modelo.cambiarRol(admin, "u", "ADMIN")
        assertEquals("CLIENTE", modelo.usuarios.single().rol); assertNotNull(modelo.error)
    }

    @Test fun doblePulsacionNoGeneraDosCambiosSimultaneos() {
        val respuesta = CompletableDeferred<UsuarioDocumento>()
        var llamadas = 0
        val roles = object : UsuarioRolesService {
            override suspend fun actualizarRol(id: String, campo: String, updateTime: String,
                body: UsuarioContainer): UsuarioDocumento {
                llamadas++
                return respuesta.await()
            }
        }
        val modelo = RolesViewModel(
            UsuariosFake(mapOf(null to UsuarioListResponse(listOf(doc("u"))))),
            roles,
            Dispatchers.Unconfined
        )
        modelo.cargarUsuarios(admin)
        modelo.cambiarRol(admin, "u", "ADMIN")
        modelo.cambiarRol(admin, "u", "ADMIN")
        assertEquals(1, llamadas)
        respuesta.complete(doc("u", rol = "ADMIN", updateTime = "v2"))
        assertEquals("ADMIN", modelo.usuarios.single().rol)
    }

    @Test fun contratoHttpEnviaUpdateMaskVersionYCuerpoSoloRol() = runBlocking {
        var peticion: Request? = null
        val clienteHttp = OkHttpClient.Builder().addInterceptor {
            peticion = it.request()
            Response.Builder().request(it.request()).protocol(okhttp3.Protocol.HTTP_1_1).code(200).message("OK")
                .body(ResponseBody.create(MediaType.parse("application/json"),
                    """{"name":"usuarios/u","fields":{"rol":{"stringValue":"ADMIN"}},"updateTime":"v2"}"""))
                .build()
        }.build()
        val api = Retrofit.Builder().baseUrl("https://firestore.test/documents/").client(clienteHttp)
            .addConverterFactory(GsonConverterFactory.create()).build().create(UsuarioRolesService::class.java)
        api.actualizarRol("u", "rol", "2026-01-02T00:00:00Z",
            UsuarioContainer(UsuarioCampos(rol = FirestoreStringValue("ADMIN"))))
        assertEquals("rol", peticion!!.url().queryParameter("updateMask.fieldPaths"))
        assertEquals("2026-01-02T00:00:00Z", peticion!!.url().queryParameter("currentDocument.updateTime"))
        val cuerpo = peticion!!.body()?.let { body -> okio.Buffer().also { body.writeTo(it) }.readUtf8() }.orEmpty()
        assertTrue(cuerpo.contains("\"rol\"")); assertFalse(cuerpo.contains("password")); assertFalse(cuerpo.contains("correo"))
    }
}
