package com.example.vapeon_movil.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vapeon_movil.entities.Proveedor
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.services.ProveedorService
import com.example.vapeon_movil.services.dto.aFirestore
import com.example.vapeon_movil.services.dto.aProveedor
import com.example.vapeon_movil.services.listarTodosProveedores
import com.example.vapeon_movil.utils.RetrofitClient
import java.time.Instant
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class FormularioProveedor(
    val razonSocial: String = "",
    val ruc: String = "",
    val contacto: String = "",
    val telefono: String = "",
    val correo: String = "",
    val direccion: String = "",
    val ciudad: String = "",
    val tipoProducto: String = "",
    val observaciones: String = ""
)

data class ErroresProveedor(
    val razonSocial: String? = null,
    val ruc: String? = null,
    val contacto: String? = null,
    val telefono: String? = null,
    val correo: String? = null,
    val direccion: String? = null,
    val ciudad: String? = null,
    val tipoProducto: String? = null
) {
    fun hayErrores(): Boolean = listOf(razonSocial, ruc, contacto, telefono, correo,
        direccion, ciudad, tipoProducto).any { it != null }
}

/** Valida longitud, caracteres, prefijo de contribuyente y dígito verificador peruano. */
internal fun rucPeruanoValido(valor: String): Boolean {
    if (!Regex("[0-9]{11}").matches(valor)) return false
    if (valor.take(2) !in setOf("10", "15", "16", "17", "20")) return false
    val pesos = intArrayOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2)
    val suma = (0 until 10).sumOf { (valor[it] - '0') * pesos[it] }
    val diferencia = 11 - suma % 11
    val digito = when (diferencia) { 10 -> 0; 11 -> 1; else -> diferencia }
    return digito == valor.last() - '0'
}

internal fun validarFormularioProveedor(formulario: FormularioProveedor): ErroresProveedor = ErroresProveedor(
    razonSocial = if (formulario.razonSocial.isBlank()) "La razón social es obligatoria." else null,
    ruc = if (!rucPeruanoValido(formulario.ruc)) "Ingresa un RUC peruano válido de 11 dígitos." else null,
    contacto = if (formulario.contacto.isBlank()) "El nombre del contacto es obligatorio." else null,
    telefono = if (!telefonoRegistroValido(formulario.telefono.trim()))
        "El teléfono debe tener 9 dígitos y comenzar con 9." else null,
    correo = if (!correoRegistroValido(formulario.correo.trim())) "Ingresa un correo electrónico válido." else null,
    direccion = if (formulario.direccion.isBlank()) "La dirección es obligatoria." else null,
    ciudad = if (formulario.ciudad.isBlank()) "La ciudad es obligatoria." else null,
    tipoProducto = if (formulario.tipoProducto.isBlank()) "El tipo de producto es obligatorio." else null
)

internal fun esAdministradorProveedor(usuario: Usuario?): Boolean =
    usuario?.id?.isNotBlank() == true && usuario.rol.equals("ADMIN", ignoreCase = true)

class ProveedorViewModel(
    private val service: ProveedorService = RetrofitClient.retrofit.create(ProveedorService::class.java),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val ahora: () -> String = { Instant.now().toString() },
    private val nuevoId: () -> String = { UUID.randomUUID().toString() }
) : ViewModel() {
    var proveedores by mutableStateOf<List<Proveedor>>(emptyList())
        private set
    var cargando by mutableStateOf(false)
        private set
    var guardando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var mensajeExito by mutableStateOf<String?>(null)
        private set
    var registroExitoso by mutableStateOf(false)
        private set
    var formulario by mutableStateOf(FormularioProveedor())
        private set
    var erroresFormulario by mutableStateOf(ErroresProveedor())
        private set
    private var registroPendiente: Proveedor? = null

    fun cargarProveedores(usuario: Usuario?) {
        if (!esAdministradorProveedor(usuario)) {
            proveedores = emptyList()
            cargando = false
            error = "Solo un administrador puede consultar proveedores."
            return
        }
        if (cargando) return
        cargando = true
        error = null
        viewModelScope.launch(dispatcher) {
            try {
                proveedores = consultarProveedores()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                error = "No se pudieron cargar los proveedores. Reintenta."
            } finally {
                cargando = false
            }
        }
    }

    fun actualizarFormulario(nuevo: FormularioProveedor) {
        if (guardando) return
        formulario = nuevo
        erroresFormulario = ErroresProveedor()
        error = null
    }

    fun iniciarFormulario() {
        if (guardando) return
        formulario = FormularioProveedor()
        erroresFormulario = ErroresProveedor()
        registroPendiente = null
        registroExitoso = false
        error = null
    }

    fun cancelarRegistro() {
        if (guardando) return
        iniciarFormulario()
    }

    fun registrarProveedor(usuario: Usuario?) {
        if (guardando) return
        if (!esAdministradorProveedor(usuario)) {
            error = "Solo un administrador puede registrar proveedores."
            return
        }
        erroresFormulario = validarFormularioProveedor(formulario)
        if (erroresFormulario.hayErrores()) return

        val limpio = formulario.aProveedor(
            id = registroPendiente?.id ?: nuevoId(),
            fecha = registroPendiente?.fechaRegistro ?: ahora()
        )
        val anterior = registroPendiente
        if (anterior != null && anterior != limpio) {
            error = "El registro anterior requiere reintento con los mismos datos o cancelación."
            return
        }
        registroPendiente = limpio
        guardando = true
        error = null
        viewModelScope.launch(dispatcher) {
            try {
                val documento = try {
                    service.crearProveedor(limpio.aFirestore(), limpio.id)
                } catch (conflicto: HttpException) {
                    if (conflicto.code() != 409) throw conflicto
                    service.mostrarProveedor(limpio.id)
                }
                val guardado = documento.aProveedor()
                require(guardado.id == limpio.id) { "Respuesta de proveedor inválida." }
                proveedores = try {
                    consultarProveedores()
                } catch (_: Exception) {
                    (proveedores.filterNot { it.id == guardado.id } + guardado).ordenados()
                }
                registroPendiente = null
                formulario = FormularioProveedor()
                erroresFormulario = ErroresProveedor()
                registroExitoso = true
                mensajeExito = "Proveedor registrado correctamente."
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                error = "No se pudo registrar el proveedor. Reintenta con los mismos datos."
            } finally {
                guardando = false
            }
        }
    }

    fun consumirRegistroExitoso() { registroExitoso = false }
    fun consumirMensajeExito() { mensajeExito = null }

    private suspend fun consultarProveedores(): List<Proveedor> =
        service.listarTodosProveedores().documents.orEmpty()
            .mapNotNull { runCatching { it.aProveedor() }.getOrNull() }
            .ordenados()
}

private fun FormularioProveedor.aProveedor(id: String, fecha: String) = Proveedor(
    id = id,
    razonSocial = razonSocial.trim(),
    ruc = ruc,
    contacto = contacto.trim(),
    telefono = telefono.trim(),
    correo = correo.trim().lowercase(Locale.ROOT),
    direccion = direccion.trim(),
    ciudad = ciudad.trim(),
    tipoProducto = tipoProducto.trim(),
    observaciones = observaciones.trim(),
    fechaRegistro = fecha
)

private fun List<Proveedor>.ordenados(): List<Proveedor> =
    sortedWith(compareBy({ it.razonSocial.lowercase(Locale.ROOT) }, { it.id }))
