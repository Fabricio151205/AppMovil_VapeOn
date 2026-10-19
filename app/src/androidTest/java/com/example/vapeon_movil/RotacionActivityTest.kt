package com.example.vapeon_movil

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Comprueba el estado de VapeONApp al recrear la Activity real, sin escribir en Firebase. */
@RunWith(AndroidJUnit4::class)
class RotacionActivityTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test fun conservaRutaTrasRecrearActivityVariasVeces() {
        compose.onNodeWithText("Crea un Usuario").performScrollTo().performClick()
        compose.onNodeWithText("REGISTRATE EN VAPEON").assertExists()
        compose.onNodeWithText("Nombres").performScrollTo().performTextInput("Nombre de prueba")
        repeat(3) {
            compose.activityRule.scenario.recreate()
            compose.waitForIdle()
            compose.onNodeWithText("REGISTRATE EN VAPEON").assertExists()
            compose.onNodeWithText("Nombre de prueba").assertExists()
        }
    }
}
