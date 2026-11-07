package com.example.vapeon_movil.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.services.UsuarioRolesService
import com.example.vapeon_movil.services.UsuarioService
import com.example.vapeon_movil.services.dto.FirestoreStringValue
import com.example.vapeon_movil.services.dto.UsuarioCampos
import com.example.vapeon_movil.services.dto.UsuarioContainer
import com.example.vapeon_movil.services.dto.UsuarioDocumento
import com.example.vapeon_movil.services.listarTodosUsuarios
import com.example.vapeon_movil.utils.RetrofitClient
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException

private const val TAMANO_PAGINA_ROLES = 10
private val ROLES_VALIDOS = setOf("CLIENTE", "ADMIN")

class RolesViewModel(
    private val usuarioService: UsuarioService = RetrofitClient.retrofit.create(UsuarioService::class.java),
    private val rolesService: UsuarioRolesService = RetrofitClient.retrofit.create(UsuarioRolesService::class.java),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    var usuarios by mutableStateOf<List<Usuario>>(emptyList())
        private set
    var busqueda by mutableStateOf("")
        private set
    var paginaActual by mutableStateOf(0)
        private set
    var cargando by mutableStateOf(false)
        private set
    var actualizandoRol by mutableStateOf<String?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var mensaje by mutableStateOf<String?>(null)
        private set
    var conflicto by mutableStateOf(false)
        private set

    val usuariosFiltrados: List<Usuario>
        get() = filtrarUsuarios(usuarios, busqueda)
    val totalPaginas: Int
        get() = if (usuariosFiltrados.isEmpty()) 1 else
            (usuariosFiltrados.size + TAMANO_PAGINA_ROLES - 1) / TAMANO_PAGINA_ROLES
    val usuariosPagina: List<Usuario>
        get() = usuariosFiltrados.drop(paginaActual * TAMANO_PAGINA_ROLES).take(TAMANO_PAGINA_ROLES)

    fun cargarUsuarios(adminActual: Usuario?) {
        if (cargando) return
        if (!esAdministradorValido(adminActual)) {
            error = "Solo un administrador puede consultar los usuarios."
            return
        }
        cargando = true
        error = null
        mensaje = null
        conflicto = false
        viewModelScope.launch(dispatcher) {
            try {
                val documentos = usuarioService.listarTodosUsuarios().documents.orEmpty()
                usuarios = ordenarUsuarios(documentos
                    .map(::documentoAUsuario)
                    .associateBy { it.id }
                    .values
                    .toList())
                corregirPagina()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                error = "No se pudieron cargar los usuarios."
            } finally {
                cargando = false
            }
        }
    }

    fun cambiarBusqueda(valor: String) {
        busqueda = valor
        paginaActual = 0
    }

    fun primeraPagina() { paginaActual = 0 }
    fun paginaAnterior() { if (paginaActual > 0) paginaActual-- }
    fun paginaSiguiente() { if (paginaActual + 1 < totalPaginas) paginaActual++ }
    fun ultimaPagina() { paginaActual = totalPaginas - 1 }

    fun cambiarRol(adminActual: Usuario?, usuarioId: String, nuevoRol: String) {
        if (actualizandoRol != null) return
        if (!esAdministradorValido(adminActual)) {
            error = "Solo un administrador puede cambiar roles."
            return
        }
        val rolNormalizado = nuevoRol.trim().uppercase(Locale.ROOT)
        if (rolNormalizado !in ROLES_VALIDOS) {
            error = "El rol solicitado no es válido."
            return
        }
        val usuario = usuarios.firstOrNull { it.id == usuarioId }
        if (usuario == null) {
            error = "El usuario ya no está disponible. Recarga la lista."
            return
        }
        if (usuario.id == adminActual?.id && rolNormalizado == "CLIENTE") {
            error = "No puedes quitarte tu propio rol de administrador."
            return
        }
        if (usuario.updateTime.isNullOrBlank()) {
            error = "No se puede actualizar sin la versión del documento. Recarga la lista."
            return
        }
        if (usuario.rol.equals(rolNormalizado, true)) return

        actualizandoRol = usuario.id
        error = null
        mensaje = null
        conflicto = false
        viewModelScope.launch(dispatcher) {
            try {
                val body = UsuarioContainer(UsuarioCampos(rol = FirestoreStringValue(rolNormalizado)))
                val actualizado = rolesService.actualizarRol(
                    id = usuario.id,
                    campo = "rol",
                    updateTime = usuario.updateTime,
                    body = body
                )
                usuarios = usuarios.map {
                    if (it.id == usuario.id) it.copy(
                        rol = actualizado.fields.rol?.stringValue ?: rolNormalizado,
                        dni = actualizado.fields.dni?.stringValue ?: it.dni,
                        createTime = actualizado.createTime ?: it.createTime,
                        updateTime = actualizado.updateTime ?: it.updateTime
                    ) else it
                }
                mensaje = "Rol actualizado correctamente."
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                conflicto = e.code() == 409 || e.code() == 412
                error = if (conflicto) "El usuario cambió en el servidor. Recarga la lista e inténtalo nuevamente."
                    else "No se pudo actualizar el rol."
            } catch (_: Exception) {
                error = "No se pudo actualizar el rol."
            } finally {
                actualizandoRol = null
            }
        }
    }

    fun limpiarMensajes() {
        error = null
        mensaje = null
        conflicto = false
    }

    private fun corregirPagina() {
        paginaActual = paginaActual.coerceIn(0, totalPaginas - 1)
    }
}

internal fun documentoAUsuario(documento: UsuarioDocumento): Usuario = Usuario(
    id = documento.name.substringAfterLast('/'),
    nombre = documento.fields.nombre?.stringValue.orEmpty(),
    apellido = documento.fields.apellido?.stringValue.orEmpty(),
    telefono = documento.fields.telefono?.stringValue.orEmpty(),
    fechaNacimiento = documento.fields.fechaNacimiento?.stringValue.orEmpty(),
    correo = documento.fields.correo?.stringValue.orEmpty(),
    password = "",
    rol = documento.fields.rol?.stringValue ?: "CLIENTE",
    dni = documento.fields.dni?.stringValue,
    createTime = documento.createTime,
    updateTime = documento.updateTime
)

internal fun ordenarUsuarios(usuarios: List<Usuario>): List<Usuario> = usuarios.sortedWith(
    compareBy<Usuario> {
        // Firestore siempre suele devolver createTime; los documentos sin él van al final,
        // ordenados por ID, sin fabricar una fecha para ellos.
        if (parsearInstante(it.createTime) == null) 1 else 0
    }.thenBy { parsearInstante(it.createTime) }.thenBy { it.id }
)

private fun parsearInstante(valor: String?): Instant? =
    valor?.takeIf { it.isNotBlank() }?.let { runCatching { Instant.parse(it) }.getOrNull() }

internal fun filtrarUsuarios(usuarios: List<Usuario>, consulta: String): List<Usuario> {
    val buscado = normalizarBusqueda(consulta)
    if (buscado.isEmpty()) return usuarios
    return usuarios.filter { usuario ->
        listOf(
            usuario.nombre,
            usuario.apellido,
            "${usuario.nombre} ${usuario.apellido}",
            usuario.correo,
            usuario.dni.orEmpty()
        ).any { normalizarBusqueda(it).contains(buscado) }
    }
}

private fun normalizarBusqueda(valor: String): String = valor.trim().lowercase(Locale.ROOT)
private fun esAdministradorValido(usuario: Usuario?): Boolean =
    usuario?.id?.isNotBlank() == true && usuario.rol.equals("ADMIN", true)
