package com.example.vapeon_movil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.vapeon_movil.ui.theme.VapeON_MovilTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VapeONApp()
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VapeON_MovilTheme {
        VapeONApp()
    }
}