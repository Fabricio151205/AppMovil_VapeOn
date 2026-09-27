package com.example.vapeon_movil.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapeon_movil.R
import com.example.vapeon_movil.ui.theme.*

@Composable
fun ProductCard(
    nombre: String,
    precio: String,
    esAdmin: Boolean = false,
    onClick: () -> Unit,
    onEliminar: () -> Unit = {},
    onEditar: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = VapeOnCardBackground),
        modifier = Modifier
            .width(170.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Contenedor blanco con esquinas redondeadas para la imagen del producto
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(VapeOnProductImageBg),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_vapeon),
                    contentDescription = nombre,
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Nombre del producto en blanco y negrita
            Text(
                text = nombre,
                color = VapeOnTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Precio formateado (ej. "S/. 80")
            val precioFormateado = when {
                precio.startsWith("S/.") -> precio
                precio.startsWith("S/") -> "S/. ${precio.removePrefix("S/").trim()}"
                else -> "S/. $precio"
            }

            Text(
                text = precioFormateado,
                color = VapeOnTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            // Controles de edición y eliminación: Exclusivos para rol ADMIN
            if (esAdmin) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEditar,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = VapeOnTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onEliminar,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = VapeOnTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}