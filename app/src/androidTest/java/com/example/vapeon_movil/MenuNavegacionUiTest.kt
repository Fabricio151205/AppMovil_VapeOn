package com.example.vapeon_movil

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.vapeon_movil.ui.MenuScreen
import com.example.vapeon_movil.ui.theme.VapeON_MovilTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Comprueba que el menú ADMIN ejecuta los callbacks entregados por sus pantallas contenedoras. */
@RunWith(AndroidJUnit4::class)
class MenuNavegacionUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test fun saboresPuedeAbrirMarcas() = comprobarOpcion("Marcas", "marcasAdmin") {
        MenuScreen(esAdmin = true, irMarcas = { it("marcasAdmin") }) {}
    }

    @Test fun saboresPuedeAbrirRoles() = comprobarOpcion("Roles", "roles") {
        MenuScreen(esAdmin = true, irRoles = { it("roles") }) {}
    }

    @Test fun saboresPuedeAbrirProveedores() = comprobarOpcion("Proveedores", "proveedores") {
        MenuScreen(esAdmin = true, irProveedores = { it("proveedores") }) {}
    }

    @Test fun catalogoAdminPuedeAbrirProveedores() = comprobarOpcion("Proveedores", "proveedores") {
        MenuScreen(esAdmin = true, irProveedores = { it("proveedores") }) {}
    }

    private fun comprobarOpcion(
        opcion: String,
        destinoEsperado: String,
        contenido: @androidx.compose.runtime.Composable (((String) -> Unit) -> Unit)
    ) {
        var destino by mutableStateOf("")
        compose.setContent {
            VapeON_MovilTheme { contenido { destino = it } }
        }
        compose.onNodeWithContentDescription("Menú Principal").performClick()
        compose.onNodeWithText(opcion).performClick()
        compose.runOnIdle { assertEquals(destinoEsperado, destino) }
    }
}
