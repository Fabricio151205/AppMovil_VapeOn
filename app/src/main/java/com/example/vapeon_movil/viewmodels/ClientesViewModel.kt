package com.example.vapeon_movil.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.services.UsuarioClientesService
import com.example.vapeon_movil.services.UsuarioService
import com.example.vapeon_movil.services.dto.FirestoreStringValue
import com.example.vapeon_movil.services.dto.UsuarioCampos
import com.example.vapeon_movil.services.dto.UsuarioContainer
import com.example.vapeon_movil.services.dto.UsuarioDocumento
import com.example.vapeon_movil.services.listarTodosUsuarios
import com.example.vapeon_movil.utils.RetrofitClient
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class FormularioCliente(
    val nombre: String = "",
    val apellido: String = "",
    val correo: String = "",
    val dni: String = "",
    val telefono: String = "",
    val fechaNacimiento: String = ""
)

data class ErroresCliente(
    val nombre: String? = null,
    val apellido: String? = null,
    val correo: String? = null,
    val dni: String? = null,
    val telefono: String? = null,
    val fechaNacimiento: String? = null
) {
    fun hayErrores() = listOf(nombre, apellido, correo, dni, telefono, fechaNacimiento).any { it != null }
}

internal fun esAdminClientes(usuario: Usuario?): Boolean =
    usuario?.id?.isNotBlank() == true && usuario.rol.equals("ADMIN", true)

/** DNI es opcional; cuando se informa se aceptan exactamente ocho dígitos. */
internal fun dniClienteValido(dni: String): Boolean = dni.isBlank() || Regex("[0-9]{8}").matches(dni)

internal fun validarFormularioCliente(formulario: FormularioCliente): ErroresCliente = ErroresCliente(
    nombre = if (formulario.nombre.isBlank()) "El nombre es obligatorio." else null,
    apellido = if (formulario.apellido.isBlank()) "El apellido es obligatorio." else null,
    correo = if (!correoRegistroValido(formulario.correo)) "Ingresa un correo electrónico válido." else null,
    dni = if (!dniClienteValido(formulario.dni)) "El DNI debe tener 8 dígitos o quedar vacío." else null,
    telefono = if (!telefonoRegistroValido(formulario.telefono))
        "El teléfono debe tener 9 dígitos y comenzar con 9." else null,
    fechaNacimiento = validarMayoriaEdad(formulario.fechaNacimiento)
)

internal fun documentoACliente(documento: UsuarioDocumento): Usuario = Usuario(
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

internal fun ordenarClientes(clientes: List<Usuario>): List<Usuario> = clientes.sortedWith(
    compareBy<Usuario>({ "${it.nombre} ${it.apellido}".trim().lowercase(Locale.ROOT) }, { it.id })
)

internal fun filtrarClientes(clientes: List<Usuario>, consulta: String): List<Usuario> {
    val buscado = consulta.trim().lowercase(Locale.ROOT)
    if (buscado.isEmpty()) return clientes
    return clientes.filter { cliente ->
        listOf(cliente.nombre, cliente.apellido, "${cliente.nombre} ${cliente.apellido}",
            cliente.correo, cliente.dni.orEmpty()).any {
            it.lowercase(Locale.ROOT).contains(buscado)
        }
    }
}

class ClientesViewModel(
    private val usuarioService: UsuarioService = RetrofitClient.retrofit.create(UsuarioService::class.java),
    private val clientesService: UsuarioClientesService = RetrofitClient.retrofit.create(UsuarioClientesService::class.java),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    var clientes by mutableStateOf<List<Usuario>>(emptyList()); private set
    var cargando by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var busqueda by mutableStateOf(""); private set
    var clienteSeleccionado by mutableStateOf<Usuario?>(null); private set
    var cargandoDetalle by mutableStateOf(false); private set
    var errorDetalle by mutableStateOf<String?>(null); private set
    var formulario by mutableStateOf(FormularioCliente()); private set
    var erroresFormulario by mutableStateOf(ErroresCliente()); private set
    var guardando by mutableStateOf(false); private set
    var mensajeExito by mutableStateOf<String?>(null); private set
    var conflicto by mutableStateOf(false); private set

    val clientesFiltrados get() = filtrarClientes(clientes, busqueda)

    fun cargarClientes(admin: Usuario?, forzar: Boolean = false) {
        if (!esAdminClientes(admin)) {
            clientes = emptyList(); clienteSeleccionado = null
            error = "Solo un administrador puede consultar clientes."
            return
        }
        if (cargando || (!forzar && clientes.isNotEmpty())) return
        cargando = true; error = null
        viewModelScope.launch(dispatcher) {
            try {
                clientes = consultarClientes()
                clienteSeleccionado?.let { seleccionado ->
                    clienteSeleccionado = clientes.firstOrNull { it.id == seleccionado.id }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = "No se pudieron cargar los clientes. Reintenta." }
            finally { cargando = false }
        }
    }

    fun cambiarBusqueda(valor: String) { busqueda = valor }

    fun seleccionarCliente(admin: Usuario?, id: String): Boolean {
        if (!esAdminClientes(admin)) {
            error = "Solo un administrador puede consultar el detalle de clientes."
            return false
        }
        val cliente = clientes.firstOrNull { it.id == id && it.rol.equals("CLIENTE", true) }
        if (cliente == null) {
            error = "El cliente ya no está disponible. Recarga la lista."
            return false
        }
        clienteSeleccionado = cliente.copy(password = "")
        errorDetalle = null
        return true
    }

    fun recuperarCliente(admin: Usuario?, id: String, paraEditar: Boolean = false, forzar: Boolean = false) {
        if (!esAdminClientes(admin)) {
            clienteSeleccionado = null
            errorDetalle = "Solo un administrador puede consultar clientes."
            return
        }
        if (id.isBlank()) {
            clienteSeleccionado = null
            errorDetalle = "No se indicó el cliente que deseas consultar."
            return
        }
        if (cargandoDetalle) return
        val local = clientes.firstOrNull { it.id == id && it.rol.equals("CLIENTE", true) }
        if (!forzar && local != null) {
            clienteSeleccionado = local.copy(password = "")
            errorDetalle = null
            if (paraEditar) prepararFormulario(local)
            return
        }
        cargandoDetalle = true
        errorDetalle = null
        viewModelScope.launch(dispatcher) {
            try {
                val recuperado = documentoACliente(usuarioService.mostrarUsuario(id))
                if (!recuperado.rol.equals("CLIENTE", true)) {
                    clienteSeleccionado = null
                    errorDetalle = "El usuario solicitado no es un cliente."
                    return@launch
                }
                val seguro = recuperado.copy(password = "")
                clientes = ordenarClientes(clientes.filterNot { it.id == seguro.id } + seguro)
                clienteSeleccionado = seguro
                if (paraEditar) prepararFormulario(seguro)
            } catch (e: CancellationException) { throw e }
            catch (e: HttpException) {
                clienteSeleccionado = null
                errorDetalle = if (e.code() == 404) "El cliente ya no existe."
                    else "No se pudo cargar el cliente. Reintenta."
            } catch (_: Exception) {
                clienteSeleccionado = null
                errorDetalle = "No se pudo cargar el cliente. Revisa tu conexión."
            } finally {
                cargandoDetalle = false
            }
        }
    }

    fun recargarClienteParaEdicion(admin: Usuario?) {
        val id = clienteSeleccionado?.id.orEmpty()
        recuperarCliente(admin, id, paraEditar = true, forzar = true)
    }

    fun prepararEdicion(admin: Usuario?): Boolean {
        if (!esAdminClientes(admin) || clienteSeleccionado == null) {
            error = "No tienes acceso a la edición de clientes."
            return false
        }
        prepararFormulario(clienteSeleccionado!!)
        return true
    }

    fun actualizarFormulario(nuevo: FormularioCliente) {
        if (guardando) return
        formulario = nuevo; erroresFormulario = ErroresCliente(); error = null; conflicto = false
    }

    fun guardarCambios(admin: Usuario?, onExito: () -> Unit = {}) {
        if (guardando) return
        val actual = clienteSeleccionado
        if (!esAdminClientes(admin) || actual == null || !actual.rol.equals("CLIENTE", true)) {
            error = "Solo un administrador puede editar clientes."
            return
        }
        erroresFormulario = validarFormularioCliente(formulario)
        if (erroresFormulario.hayErrores()) return
        if (actual.updateTime.isNullOrBlank()) {
            error = "No se puede editar sin la versión del documento. Recarga la información."
            return
        }
        guardando = true; error = null; conflicto = false; mensajeExito = null
        viewModelScope.launch(dispatcher) {
            try {
                val documentos = usuarioService.listarTodosUsuarios().documents.orEmpty()
                val correo = formulario.correo.trim()
                if (documentos.any { it.name.substringAfterLast('/') != actual.id &&
                        it.fields.correo?.stringValue?.trim().equals(correo, true) }) {
                    erroresFormulario = erroresFormulario.copy(correo = "Ya existe una cuenta con ese correo.")
                    return@launch
                }
                val cambios = cambiosCliente(actual, formulario)
                if (cambios.first.isEmpty()) {
                    mensajeExito = "No hay cambios para guardar."
                    onExito()
                    return@launch
                }
                val actualizado = clientesService.actualizarCliente(actual.id, cambios.first,
                    actual.updateTime!!, UsuarioContainer(cambios.second))
                val clienteActualizado = documentoACliente(actualizado).copy(password = "")
                clientes = ordenarClientes(clientes.map { if (it.id == actual.id) clienteActualizado else it })
                clienteSeleccionado = clienteActualizado
                formulario = FormularioCliente(clienteActualizado.nombre, clienteActualizado.apellido,
                    clienteActualizado.correo, clienteActualizado.dni.orEmpty(),
                    clienteActualizado.telefono, clienteActualizado.fechaNacimiento)
                mensajeExito = "Información del cliente actualizada."
                onExito()
            } catch (e: CancellationException) { throw e }
            catch (e: HttpException) {
                conflicto = esConflictoVersionCliente(e)
                error = if (conflicto) "Los datos fueron modificados desde otro dispositivo. Recarga la información e inténtalo nuevamente."
                    else "No se pudieron guardar los cambios. Reintenta."
            } catch (_: Exception) { error = "No se pudieron guardar los cambios. Revisa tu conexión." }
            finally { guardando = false }
        }
    }

    fun consumirMensajeExito() { mensajeExito = null }

    private fun prepararFormulario(cliente: Usuario) {
        formulario = FormularioCliente(cliente.nombre, cliente.apellido, cliente.correo,
            cliente.dni.orEmpty(), cliente.telefono, cliente.fechaNacimiento)
        erroresFormulario = ErroresCliente(); error = null; conflicto = false
    }

    private suspend fun consultarClientes(): List<Usuario> = ordenarClientes(
        usuarioService.listarTodosUsuarios().documents.orEmpty()
            .map(::documentoACliente)
            .filter { it.rol.equals("CLIENTE", true) }
            .associateBy { it.id }.values.toList()
    )
}

internal fun esConflictoVersionCliente(error: HttpException): Boolean {
    if (error.code() == 409 || error.code() == 412) return true
    val cuerpo = runCatching { error.response()?.errorBody()?.string().orEmpty() }.getOrDefault("")
    return cuerpo.contains("FAILED_PRECONDITION", ignoreCase = true)
}

private fun cambiosCliente(actual: Usuario, formulario: FormularioCliente): Pair<List<String>, UsuarioCampos> {
    val campos = mutableListOf<String>()
    fun cambio(nombre: String, anterior: String?, nuevo: String): FirestoreStringValue? {
        if ((anterior ?: "") == nuevo) return null
        campos += nombre
        return FirestoreStringValue(nuevo)
    }
    val nombre = formulario.nombre.trim(); val apellido = formulario.apellido.trim()
    val correo = formulario.correo.trim(); val dni = formulario.dni.trim()
    val telefono = formulario.telefono.trim(); val fecha = formulario.fechaNacimiento.trim()
    return campos to UsuarioCampos(
        nombre = cambio("nombre", actual.nombre, nombre),
        apellido = cambio("apellido", actual.apellido, apellido),
        correo = cambio("correo", actual.correo, correo),
        dni = cambio("dni", actual.dni, dni),
        telefono = cambio("telefono", actual.telefono, telefono),
        fechaNacimiento = cambio("fechaNacimiento", actual.fechaNacimiento, fecha)
    )
}
