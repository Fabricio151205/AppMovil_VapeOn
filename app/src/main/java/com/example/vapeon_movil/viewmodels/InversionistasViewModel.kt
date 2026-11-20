package com.example.vapeon_movil.viewmodels

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vapeon_movil.entities.Inversionista
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.services.InversionistaService
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.utils.RetrofitClient
import kotlinx.coroutines.*
import retrofit2.HttpException
import java.time.Instant
import java.util.Locale
import java.util.UUID

/** Errores por campo del formulario de inversionistas. */
internal data class ErroresInversionista(
    val nombre: String? = null,
    val documento: String? = null,
    val telefono: String? = null,
    val correo: String? = null,
    val observaciones: String? = null
) {
    val primero: String?
        get() = nombre ?: documento ?: telefono ?: correo ?: observaciones

    fun hayErrores(): Boolean = primero != null
}

internal fun validarCamposInversionista(datos: Inversionista): ErroresInversionista {
    val nombre = datos.nombre.trim()
    val nombreError = when {
        nombre.isEmpty() -> "Ingresa el nombre o razón social."
        nombre.length < 2 -> "El nombre debe tener al menos 2 caracteres."
        nombre.length > 150 -> "El nombre admite hasta 150 caracteres."
        nombre.any { it.code < 32 || it.code == 127 } -> "El nombre contiene caracteres no válidos."
        else -> null
    }

    val tipo = datos.tipoDocumento.trim().uppercase(Locale.ROOT)
    val documento = datos.documento.trim()
    val documentoError = when {
        tipo !in listOf("DNI", "RUC") -> "Selecciona DNI o RUC."
        tipo == "DNI" && !Regex("[0-9]{8}").matches(documento) -> "El DNI debe tener 8 dígitos."
        tipo == "RUC" && !rucPeruanoValido(documento) -> "Ingresa un RUC peruano válido."
        else -> null
    }

    val telefono = datos.telefono.trim()
    val telefonoError = if (telefono.isNotEmpty() && !telefonoRegistroValido(telefono)) {
        "El teléfono debe tener 9 dígitos y comenzar con 9."
    } else null

    val correo = datos.correo.trim()
    val correoError = when {
        correo.isEmpty() -> null
        correo.length > 254 -> "El correo admite hasta 254 caracteres."
        !correoRegistroValido(correo) -> "Ingresa un correo válido."
        else -> null
    }

    val observacionesError = if (datos.observaciones.length > 2000) {
        "Las observaciones admiten hasta 2000 caracteres."
    } else null

    return ErroresInversionista(
        nombre = nombreError,
        documento = documentoError,
        telefono = telefonoError,
        correo = correoError,
        observaciones = observacionesError
    )
}

internal fun validarInversionista(datos: Inversionista): String? =
    validarCamposInversionista(datos).primero

class InversionistasViewModel(
    private val service: InversionistaService = RetrofitClient.retrofit.create(InversionistaService::class.java),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
) : ViewModel() {
    var lista by mutableStateOf<List<Inversionista>>(emptyList())
        private set
    var cargando by mutableStateOf(false)
        private set
    var guardando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var mensaje by mutableStateOf<String?>(null)
        private set
    var seleccionado by mutableStateOf<Inversionista?>(null)
        private set
    var formulario by mutableStateOf<Inversionista?>(null)
        private set
    private var documentos = emptyMap<String, InversionistaDocumento>()
    private var propietario: String? = null
    private var operacion: Job? = null
    private var altaPendiente: Inversionista? = null
    val altaRequiereComprobacion: Boolean get() = altaPendiente != null

    fun actualizarSesion(usuario: Usuario?) {
        val id = usuario?.takeIf { it.rol.equals("ADMIN", true) && it.id.isNotBlank() }?.id
        if (id == propietario) return
        operacion?.cancel()
        propietario = id
        lista = emptyList()
        documentos = emptyMap()
        seleccionado = null
        formulario = null
        altaPendiente = null
        cargando = false
        guardando = false
        error = null
        mensaje = null
    }

    private fun autorizado(usuario: Usuario?): Boolean {
        if (propietario == null || usuario?.id != propietario || !usuario?.rol.equals("ADMIN", true)) {
            error = "Solo un administrador puede gestionar inversionistas."
            return false
        }
        return true
    }

    fun cargar(usuario: Usuario?) {
        if (cargando || guardando || !autorizado(usuario)) return
        cargando = true
        error = null
        operacion = viewModelScope.launch(dispatcher) {
            try {
                val encontrados = mutableListOf<InversionistaDocumento>()
                val tokens = mutableSetOf<String>()
                var token: String? = null
                do {
                    val pagina = service.listar(token)
                    encontrados += pagina.documents.orEmpty()
                    token = pagina.nextPageToken?.takeIf { it.isNotBlank() }
                    check(token == null || tokens.add(token)) { "Paginación inválida." }
                } while (token != null)
                coroutineContext.ensureActive()
                val nuevos = encontrados.associateBy { it.aInversionista().id }
                val modelos = nuevos.values.map { it.aInversionista() }
                documentos = nuevos
                publicar(modelos)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = mensajeError(e) }
            finally { if (isActive) cargando = false }
        }
    }

    private fun publicar(modelos: List<Inversionista>) {
        lista = modelos.sortedWith(compareBy({ it.nombre.lowercase(Locale.ROOT) }, { it.id }))
    }

    fun ver(inversionista: Inversionista) { if (!guardando) seleccionado = inversionista }
    fun cerrarDetalle() { if (!guardando) seleccionado = null }
    fun iniciarFormulario(inversionista: Inversionista? = null) {
        if (guardando) return
        formulario = altaPendiente ?: inversionista ?: Inversionista(
            id = UUID.randomUUID().toString(), fechaRegistro = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS).toString()
        )
        error = null
    }
    fun cambiarFormulario(datos: Inversionista) {
        if (!guardando && altaPendiente == null) formulario = datos
    }
    fun cancelarFormulario() { if (!guardando) { formulario = null; error = null } }
    fun consumirMensaje() { mensaje = null }

    fun guardar(usuario: Usuario?) {
        if (guardando || cargando || !autorizado(usuario)) return
        val actual = formulario ?: return
        val datos = actual.copy(nombre = actual.nombre.trim(),
            tipoDocumento = actual.tipoDocumento.trim().uppercase(Locale.ROOT),
            documento = actual.documento.trim(),
            telefono = actual.telefono.trim(), correo = actual.correo.trim().lowercase(Locale.ROOT),
            observaciones = actual.observaciones.trim())
        error = validarInversionista(datos)
        if (error != null) return
        if (lista.any { it.id != datos.id &&
            it.tipoDocumento.equals(datos.tipoDocumento, true) && it.documento == datos.documento }) {
            error = "Ya existe un inversionista con ese documento."
            return
        }
        val original = documentos[datos.id]
        if (original == null) altaPendiente = datos
        guardando = true
        operacion = viewModelScope.launch(dispatcher) {
            try {
                val remoto = if (original == null) {
                    try { service.crear(datos.aFirestoreInversionista(), datos.id) }
                    catch (e: HttpException) {
                        if (e.code() != 409) throw e
                        service.mostrar(datos.id)
                    }
                } else {
                    try { service.actualizar(datos.id, datos.aFirestoreInversionista(), original.updateTime) }
                    catch (e: HttpException) {
                        if (e.code() !in listOf(409, 412)) throw e
                        service.mostrar(datos.id)
                    }
                }
                coroutineContext.ensureActive()
                val guardado = remoto.aInversionista()
                check(guardado == datos) { "El registro cambió en otro dispositivo. Cierra el formulario y actualiza la lista." }
                documentos = documentos + (datos.id to remoto)
                publicar(lista.filterNot { it.id == datos.id } + guardado)
                seleccionado = null
                formulario = null
                altaPendiente = null
                mensaje = if (original == null) "Inversionista registrado." else "Inversionista actualizado."
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = mensajeError(e) }
            finally { if (isActive) guardando = false }
        }
    }

    fun eliminar(usuario: Usuario?, inversionista: Inversionista) {
        if (guardando || cargando || !autorizado(usuario)) return
        val original = documentos[inversionista.id] ?: return
        guardando = true
        error = null
        operacion = viewModelScope.launch(dispatcher) {
            try {
                try { service.eliminar(inversionista.id, original.updateTime) }
                catch (e: HttpException) { if (e.code() != 404) throw e }
                coroutineContext.ensureActive()
                documentos = documentos - inversionista.id
                publicar(lista.filterNot { it.id == inversionista.id })
                seleccionado = null
                mensaje = "Inversionista eliminado."
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = mensajeError(e) }
            finally { if (isActive) guardando = false }
        }
    }

    private fun mensajeError(e: Exception): String = when {
        e is HttpException && e.code() in listOf(401, 403) -> "No tienes permiso para gestionar inversionistas."
        e is HttpException && e.code() in listOf(409, 412) -> "El registro cambió. Actualiza la lista antes de reintentar."
        e is IllegalStateException || e is IllegalArgumentException -> e.message ?: "Registro inválido."
        else -> "No se pudo completar la operación. Comprueba la conexión y reintenta."
    }
}
