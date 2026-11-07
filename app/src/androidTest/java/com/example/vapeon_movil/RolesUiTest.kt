package com.example.vapeon_movil

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.services.UsuarioRolesService
import com.example.vapeon_movil.services.UsuarioService
import com.example.vapeon_movil.services.dto.*
import com.example.vapeon_movil.ui.RolesScreen
import com.example.vapeon_movil.ui.theme.VapeON_MovilTheme
import com.example.vapeon_movil.viewmodels.RolesViewModel
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RolesUiTest {
    @get:Rule val compose = createComposeRule()

    private fun doc(id: String, rol: String = "CLIENTE", dni: String? = "12345678") = UsuarioDocumento(
        "usuarios/$id",
        UsuarioCampos(
            nombre = FirestoreStringValue(if (id == "admin") "Admin" else "Usuario$id"),
            apellido = FirestoreStringValue("Prueba"),
            correo = FirestoreStringValue("$id@correo.com"),
            dni = dni?.let(::FirestoreStringValue), rol = FirestoreStringValue(rol)
        ),
        "2026-01-${if (id == "admin") "01" else id.padStart(2, '0')}T00:00:00Z",
        "2026-02-01T00:00:00Z"
    )

    private class Usuarios(private val docs: List<UsuarioDocumento>, private val falla: Boolean = false) : UsuarioService {
        override suspend fun listarUsuarios(pageToken: String?): UsuarioListResponse {
            if (falla) error("red")
            return UsuarioListResponse(docs)
        }
        override suspend fun mostrarUsuario(id: String) = error("No se usa")
        override suspend fun crearUsuario(body: UsuarioContainer, documentId: String?) = error("No se usa")
        override suspend fun actualizarUsuario(id: String, body: UsuarioContainer) = error("No se usa")
        override suspend fun eliminarUsuario(id: String) = error("No se usa")
    }

    private class Roles : UsuarioRolesService {
        var llamadas = 0
        override suspend fun actualizarRol(id: String, campo: String, updateTime: String,
            body: UsuarioContainer): UsuarioDocumento {
            llamadas++
            return UsuarioDocumento("usuarios/$id", UsuarioCampos(
                nombre = FirestoreStringValue("Usuario$id"), apellido = FirestoreStringValue("Prueba"),
                correo = FirestoreStringValue("$id@correo.com"), dni = FirestoreStringValue("12345678"),
                rol = body.fields.rol), "2026-01-02T00:00:00Z", "2026-02-02T00:00:00Z")
        }
    }

    @Test fun muestraBuscaPaginaYConfirmaCambioSinFirebase() {
        val roles = Roles()
        val docs = listOf(doc("admin", "ADMIN", null)) + (2..12).map { doc(it.toString()) }
        val vm = RolesViewModel(Usuarios(docs), roles, Dispatchers.Unconfined)
        val admin = Usuario(id = "admin", nombre = "Admin", rol = "ADMIN")
        compose.setContent { VapeON_MovilTheme { RolesScreen(admin, vm, {}) } }

        compose.onNodeWithText("Gestión de Roles").assertIsDisplayed()
        compose.onNodeWithTag("roles_busqueda").assertIsDisplayed()
        compose.onNodeWithText("DNI: No registrado").assertIsDisplayed()
        compose.onNodeWithTag("cambiar_rol_admin").assertIsNotEnabled()
        compose.onNodeWithText("1 / 2").assertIsDisplayed()
        compose.onNodeWithText("Siguiente").performClick()
        compose.onNodeWithText("2 / 2").assertIsDisplayed()

        compose.onNodeWithTag("roles_busqueda").performTextInput("Usuario2")
        compose.onNodeWithText("1 / 1").assertIsDisplayed()
        compose.onNodeWithText("2@correo.com").assertIsDisplayed()
        compose.onNodeWithText("DNI: 12345678").assertIsDisplayed()
        compose.onNodeWithTag("cambiar_rol_2").performClick()
        compose.onNodeWithText("Confirmar cambio de rol").assertIsDisplayed()
        compose.onNodeWithText("Confirmar").performClick()
        compose.waitForIdle()
        assertEquals(1, roles.llamadas)
        compose.onNodeWithText("Rol: ADMIN").assertIsDisplayed()
    }

    @Test fun muestraEstadoVacio() {
        val admin = Usuario(id = "admin", rol = "ADMIN")
        compose.setContent {
            VapeON_MovilTheme {
                RolesScreen(admin, RolesViewModel(Usuarios(emptyList()), Roles(), Dispatchers.Unconfined), {})
            }
        }
        compose.onNodeWithTag("roles_vacio").assertTextContains("No hay usuarios")
    }

    @Test fun muestraEstadoError() {
        val admin = Usuario(id = "admin", rol = "ADMIN")
        val vmError = RolesViewModel(Usuarios(emptyList(), falla = true), Roles(), Dispatchers.Unconfined)
        compose.setContent { VapeON_MovilTheme { RolesScreen(admin, vmError, {}) } }
        compose.onNodeWithTag("roles_error").assertIsDisplayed()
    }
}
