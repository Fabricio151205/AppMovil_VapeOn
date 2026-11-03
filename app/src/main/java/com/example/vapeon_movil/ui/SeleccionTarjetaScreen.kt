package com.example.vapeon_movil.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vapeon_movil.entities.Usuario
import com.example.vapeon_movil.ui.theme.*
import com.example.vapeon_movil.viewmodels.TarjetaViewModel

@Composable
fun SeleccionTarjetaScreen(usuario: Usuario?, tarjetaViewModel: TarjetaViewModel,
    volver: () -> Unit, agregarTarjeta: () -> Unit, desdeCheckout: Boolean = true,
    continuar: () -> Unit = {}) {
    val id = usuario?.takeIf { it.rol.equals("CLIENTE", true) }?.id.orEmpty()
    val vm = tarjetaViewModel
    LaunchedEffect(id) {
        vm.actualizarUsuario(usuario)
        if (id.isNotBlank()) vm.cargarTarjetas(usuario)
    }
    PasoCheckout(if (desdeCheckout) "Seleccionar tarjeta" else "Mis tarjetas", volver) {
        if (id.isBlank() || vm.usuarioId != id) {
            Text("Inicia sesión como cliente para acceder a tus tarjetas.", color = MaterialTheme.colorScheme.error)
        } else {
            Text("Tarjetas simuladas · No se realizan cobros.", color = VapeOnTextSecondary)
            Button(onClick = agregarTarjeta, enabled = !vm.registrando && !vm.seleccionando,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Agregar nueva tarjeta") }
            TextButton(onClick = { vm.cargarTarjetas(usuario) }, enabled = !vm.cargando) {
                Text(if (vm.error == null) "Actualizar" else "Reintentar", color = VapeOnGold)
            }
            vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            vm.mensaje?.let {
                Text(if (vm.tarjetaSeleccionada != null) "Tarjeta seleccionada para un pago SIMULADO." else it, color = VapeOnGold)
            }
            if (vm.cargando || vm.seleccionando) CircularProgressIndicator(color = VapeOnGold)
            if (!vm.cargando && vm.tarjetas.isEmpty()) Text("No tienes tarjetas registradas.", color = VapeOnTextSecondary)
            vm.tarjetas.forEach { tarjeta ->
                val propia = tarjeta.usuarioId == id
                val seleccionada = vm.tarjetaSeleccionada?.id == tarjeta.id
                val vencida = tarjeta.estaVencida()
                if (propia) Card(colors = CardDefaults.cardColors(
                    containerColor = if (seleccionada) VapeOnRedDark else VapeOnCardBackground)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(tarjeta.marca, color = VapeOnGold, style = MaterialTheme.typography.titleMedium)
                        Text("**** **** **** ${tarjeta.ultimos4}", color = VapeOnTextPrimary)
                        Text(tarjeta.titular, color = VapeOnTextPrimary)
                        Text("Vence: ${tarjeta.vencimientoMes.toString().padStart(2, '0')}/${tarjeta.vencimientoAnio}", color = VapeOnTextSecondary)
                        if (vencida) Text("Tarjeta vencida", color = MaterialTheme.colorScheme.error)
                        OutlinedButton(onClick = { vm.seleccionarTarjeta(usuario, tarjeta.id) },
                            enabled = !vencida && !vm.seleccionando && !vm.registrando,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(if (seleccionada) "Seleccionada" else "Seleccionar", color = VapeOnGold)
                        }
                    }
                }
            }
            if (desdeCheckout) {
                val seleccion = vm.tarjetaSeleccionada
                Button(onClick = continuar, enabled = seleccion != null && seleccion.usuarioId == id && !seleccion.estaVencida() &&
                    !vm.seleccionando && !vm.registrando && !vm.cargando,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Continuar con pago SIMULADO")
                }
                Text("La tarjeta se verificará nuevamente antes de iniciar el pago. No se realizará ningún cobro real.",
                    color = VapeOnTextSecondary)
            }
        }
    }
}
