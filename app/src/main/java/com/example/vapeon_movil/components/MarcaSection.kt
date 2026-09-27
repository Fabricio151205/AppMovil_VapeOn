package com.example.vapeon_movil.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.entities.Producto
import com.example.vapeon_movil.ui.theme.VapeOnGold
import com.example.vapeon_movil.ui.theme.VapeOnTextPrimary

@Composable
fun MarcaSection(
    nombreMarca: String,
    productos: List<Producto>,
    irDetalleProducto: (Producto) -> Unit,
    esAdmin: Boolean = false,
    onVerTodos: () -> Unit = {},
    eliminarProducto: (Producto) -> Unit = {},
    editarProducto: (Producto) -> Unit = {}
) {
    if (productos.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            // Cabecera: Nombre de la marca en dorado y enlace "Ver todos"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = nombreMarca,
                    color = VapeOnGold,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Ver todos",
                    color = VapeOnTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier
                        .clickable { onVerTodos() }
                        .padding(4.dp)
                )
            }

            // Carrusel horizontal de tarjetas de productos
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(productos) { producto ->
                    ProductCard(
                        nombre = producto.nombre,
                        precio = producto.precio,
                        esAdmin = esAdmin,
                        onClick = { irDetalleProducto(producto) },
                        onEliminar = { eliminarProducto(producto) },
                        onEditar = { editarProducto(producto) }
                    )
                }
            }
        }
    }
}