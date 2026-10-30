package com.example.vapeon_movil.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.entities.Pago
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.utils.importe

@Composable
fun ConfirmacionPagoScreen(pago: Pago, cargando: Boolean, error: String?, registrarPedido: () -> Unit,
    comprobar: () -> Unit) {
    PasoCheckout("Pago SIMULADO confirmado", volver = {}, bloqueado = true) {
        Text("No se realizó ningún cobro real.", color = VapeOnTextSecondary)
        Text("Método: ${pago.metodoPago}", color = VapeOnTextPrimary)
        Text("Estado del pago: ${pago.estadoPago}", color = VapeOnGold)
        Text("Operación: ${pago.numeroOperacion}", color = VapeOnTextPrimary)
        Text("Monto: S/ ${importe(pago.monto)}", color = VapeOnTextPrimary)
        Text("Registro del pedido: ${pago.estadoRegistroPedido}", color = VapeOnTextSecondary)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (cargando) CircularProgressIndicator(color = VapeOnGold)
        Button(onClick = registrarPedido, enabled = !cargando && pago.estadoPago == "CONFIRMADO",
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (pago.estadoRegistroPedido == "REGISTRADO") "Completar confirmación del pedido" else "Registrar pedido")
        }
        OutlinedButton(onClick = comprobar, enabled = !cargando,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Comprobar pago", color = VapeOnGold) }
        Text("Si el registro falla, se conserva esta misma operación confirmada. Reintentar no crea otro pago.", color = VapeOnTextSecondary)
    }
}
