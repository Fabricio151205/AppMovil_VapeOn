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
fun PagoSimuladoScreen(pago: Pago?, cargando: Boolean, error: String?, comprobar: () -> Unit,
    aprobar: () -> Unit, rechazar: () -> Unit, nuevoIntento: () -> Unit, volver: () -> Unit) {
    val rechazado = pago?.estadoPago == "RECHAZADO"
    PasoCheckout("Pago SIMULADO", if (rechazado) nuevoIntento else volver,
        bloqueado = cargando || (pago != null && !rechazado)) {
        Text("Demostración universitaria. No existe conexión con Yape ni con una entidad bancaria.", color = VapeOnTextSecondary)
        if (pago != null) {
            Text("Método: ${pago.metodoPago}", color = VapeOnTextPrimary)
            Text("Estado del pago: ${pago.estadoPago}", color = VapeOnGold)
            Text("Monto: S/ ${importe(pago.monto)}", color = VapeOnTextPrimary)
            Text("Operación: ${pago.numeroOperacion}", color = VapeOnTextSecondary)
            Button(onClick = comprobar, enabled = !cargando,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Comprobar pago") }
            if (pago.estadoPago == "PENDIENTE") {
                Button(onClick = aprobar, enabled = !cargando,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Simular aprobación") }
                OutlinedButton(onClick = rechazar, enabled = !cargando,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Simular rechazo", color = VapeOnGold) }
                Text("Mientras el pago no esté confirmado, no se registra el pedido ni se descuenta stock.", color = VapeOnTextSecondary)
            }
            if (rechazado) {
                Text("Pago rechazado. Tu carrito se conserva. Un nuevo intento tendrá otra operación.", color = VapeOnTextSecondary)
                Button(onClick = nuevoIntento, enabled = !cargando,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Volver a métodos de pago") }
            }
        } else Text(if (cargando) "Preparando el intento…" else "No se inició ningún pago. Revisa los datos y vuelve a intentarlo.",
            color = VapeOnTextPrimary)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (cargando) CircularProgressIndicator(color = VapeOnGold)
    }
}
