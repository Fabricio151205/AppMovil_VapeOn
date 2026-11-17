package com.example.vapeon_movil

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.services.UsuarioClientesService
import com.example.vapeon_movil.services.UsuarioService
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.ui.ClientesScreen
import com.example.vapeon_movil.ui.DetalleClienteScreen
import com.example.vapeon_movil.ui.EditarClienteScreen
import com.example.vapeon_movil.ui.theme.VapeON_MovilTheme
import com.example.vapeon_movil.viewmodels.ClientesViewModel
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class ClientesUiTest {
    @get:Rule val compose = createComposeRule()
    private val admin = Usuario(id = "admin", rol = "ADMIN")
    private val cliente = Usuario(id = "cliente", rol = "CLIENTE")

    private fun doc(id: String = "c1", nombre: String = "Juan", apellido: String = "Pérez") = UsuarioDocumento(
        "usuarios/$id", UsuarioCampos(
            nombre = FirestoreStringValue(nombre), apellido = FirestoreStringValue(apellido),
            correo = FirestoreStringValue("juan@correo.com"), dni = FirestoreStringValue("12345678"),
            telefono = FirestoreStringValue("987654321"), fechaNacimiento = FirestoreStringValue("01/01/1990"),
            rol = FirestoreStringValue("CLIENTE"), password = FirestoreStringValue("no-visible")
        ), "2026-01-01T00:00:00Z", "2026-01-02T00:00:00Z"
    )

    private class UsuariosFake(private val docs: List<UsuarioDocumento>, private val falla: Boolean = false) : UsuarioService {
        override suspend fun listarUsuarios(pageToken: String?): UsuarioListResponse {
            if (falla) error("red")
            return UsuarioListResponse(docs)
        }
        override suspend fun mostrarUsuario(id: String) = error("No se usa")
        override suspend fun crearUsuario(body: UsuarioContainer, documentId: String?) = error("No se usa")
        override suspend fun actualizarUsuario(id: String, body: UsuarioContainer) = error("No se usa")
        override suspend fun eliminarUsuario(id: String) = error("No se usa")
    }

    private class EdicionFake : UsuarioClientesService {
        override suspend fun actualizarCliente(id: String, campos: List<String>, updateTime: String,
            body: UsuarioContainer) = UsuarioDocumento("usuarios/$id", body.fields,
            "2026-01-01T00:00:00Z", "2026-02-01T00:00:00Z")
    }

    private fun vm(docs: List<UsuarioDocumento> = emptyList(), falla: Boolean = false) =
        ClientesViewModel(UsuariosFake(docs, falla), EdicionFake(), Dispatchers.Unconfined)

    @Test fun listadoTarjetaYBuscadorSonVisibles() {
        val model = vm(listOf(doc()))
        compose.setContent { VapeON_MovilTheme { ClientesScreen(admin, model, {}, {}) } }
        compose.onNodeWithText("CLIENTES").assertIsDisplayed()
        compose.onNodeWithText("Juan Pérez").assertIsDisplayed()
        compose.onNodeWithText("Buscar cliente...").performTextInput("juan")
        compose.onNodeWithText("DNI: 12345678").assertIsDisplayed()
    }

    @Test fun detalleOcultaAusenciasYMuestraEdicion() {
        compose.setContent { VapeON_MovilTheme { DetalleClienteScreen(
            Usuario(id = "c1", nombre = "Juan", apellido = "Pérez", correo = "juan@correo.com",
                dni = null, telefono = "987654321", fechaNacimiento = "01/01/1990", rol = "CLIENTE"), {}, {}) } }
        compose.onNodeWithText("INFORMACIÓN DEL CLIENTE").assertIsDisplayed()
        compose.onNodeWithText("No registrado").assertIsDisplayed()
        compose.onNodeWithText("EDITAR INFORMACIÓN").assertIsDisplayed()
    }

    @Test fun editarMuestraCamposYGuardar() {
        val model = vm(listOf(doc()))
        model.cargarClientes(admin); model.seleccionarCliente(admin, "c1"); model.prepararEdicion(admin)
        compose.setContent { VapeON_MovilTheme { EditarClienteScreen(admin, model, {}, {}) } }
        compose.onNodeWithText("Nombre").assertIsDisplayed()
        compose.onNodeWithText("Correo").assertIsDisplayed()
        compose.onNodeWithText("DNI (opcional)").assertIsDisplayed()
        compose.onNodeWithText("GUARDAR CAMBIOS").assertIsDisplayed().performClick()
    }

    @Test fun estadosVacioErrorYGuardiaAdminSonControlados() {
        val vacio = vm()
        compose.setContent { VapeON_MovilTheme { ClientesScreen(admin, vacio, {}, {}) } }
        compose.onNodeWithText("No hay clientes registrados.").assertIsDisplayed()

        val error = vm(falla = true)
        compose.setContent { VapeON_MovilTheme { ClientesScreen(admin, error, {}, {}) } }
        compose.onNodeWithText("Reintentar").assertIsDisplayed()

        val bloqueado = vm(listOf(doc()))
        bloqueado.cargarClientes(cliente)
        assertFalse(bloqueado.seleccionarCliente(cliente, "c1"))
    }

    @Test fun detalleRestauradoInexistenteMuestraErrorYReintento() {
        compose.setContent { VapeON_MovilTheme {
            DetalleClienteScreen(null, {}, {}, error = "El cliente ya no existe.", reintentar = {})
        } }
        compose.onNodeWithText("El cliente ya no existe.").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").assertIsDisplayed()
    }
}
