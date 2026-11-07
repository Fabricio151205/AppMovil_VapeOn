package com.example.vapeon_movil.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.vapeon_movil.MainActivity
import com.example.vapeon_movil.R
import com.example.vapeon_movil.entities.Producto

object NotificacionesAdmin {
    private const val CANAL_ID = "vapeon_stock_alerts"
    private const val CANAL_NOMBRE = "Alertas de Inventario Crítico"

    fun crearCanalNotificaciones(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ID,
                CANAL_NOMBRE,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones urgentes cuando los productos o sabores están por agotarse (<= 3 u.) o sin stock."
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(canal)
        }
    }

    fun enviarAlertaStockCritico(context: Context, producto: Producto) {
        crearCanalNotificaciones(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            producto.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titulo = if (producto.stockTotal == 0) {
            "⚠️ ¡URGENTE! Producto Agotado - VapeON"
        } else {
            "⚠️ Alerta de Bajo Stock (≤ 3 u.) - VapeON"
        }

        val contenido = if (producto.stockTotal == 0) {
            "El producto ${producto.nombre} (${producto.marca}) se encuentra completamente sin stock. Reabastece de inmediato."
        } else {
            "Quedan solo ${producto.stockTotal} unidades de ${producto.nombre} (${producto.marca}). Toca para reabastecer inventario."
        }

        val builder = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(R.drawable.logo_vapeon)
            .setContentTitle(titulo)
            .setContentText(contenido)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contenido))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(producto.id.hashCode(), builder.build())
        } catch (_: SecurityException) {
            // Permisos de notificación deshabilitados en el sistema
        }
    }
}
