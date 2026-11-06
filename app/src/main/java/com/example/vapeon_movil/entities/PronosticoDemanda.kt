package com.example.vapeon_movil.entities

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

object CriteriosPronosticoDemanda {
    const val MINIMO_OBSERVACIONES = 14
    const val MINIMO_DIAS_CON_DEMANDA = 3
    const val MINIMO_ENTRENAMIENTO = 10
    const val MINIMO_VALIDACION = 3
    const val MINIMO_DIAS_CON_DEMANDA_ENTRENAMIENTO = 2
    const val HORIZONTE_PREDETERMINADO = 7
    val HORIZONTES_PERMITIDOS = listOf(7, 14, 30)
}

data class ObservacionDemanda(val fecha: LocalDate, val unidades: Double)
data class PuntoPronostico(val fecha: LocalDate, val unidades: Double)

data class SerieDemandaProducto(
    val productoId: String,
    val nombre: String,
    val marca: String,
    val observaciones: List<ObservacionDemanda>
) {
    val diasConDemanda: Int get() = observaciones.count { it.unidades > 0.0 }
    val unidadesVendidas: Int get() = observaciones.sumOf { it.unidades }.toInt()
    val primeraFecha: LocalDate? get() = observaciones.firstOrNull()?.fecha
    val ultimaFecha: LocalDate? get() = observaciones.lastOrNull()?.fecha
}

enum class EstadoPronosticoDemanda { DATOS_INSUFICIENTES, DISPONIBLE }

data class ResultadoPronosticoDemanda(
    val serie: SerieDemandaProducto,
    val estado: EstadoPronosticoDemanda,
    val motivo: String? = null,
    val observacionesEntrenamiento: Int = 0,
    val observacionesValidacion: Int = 0,
    val validacion: List<PuntoPronostico> = emptyList(),
    val futuro: List<PuntoPronostico> = emptyList(),
    val alpha: Double? = null,
    val beta: Double? = null,
    val mae: Double? = null,
    val rmse: Double? = null
)

private data class EstadoHolt(val alpha: Double, val beta: Double, val nivel: Double, val tendencia: Double)

/** Construye series diarias exclusivamente desde snapshots de pedidos entregados. */
fun construirSeriesDemanda(
    pedidos: List<Pedido>,
    zona: ZoneId = ZoneId.of("America/Lima")
): List<SerieDemandaProducto> {
    data class LineaFechada(
        val fecha: LocalDate,
        val productoId: String,
        val nombre: String,
        val marca: String,
        val cantidad: Int
    )

    val lineas = pedidos.distinctBy { it.id }
        .asSequence()
        .filter { it.estado == "ENTREGADO" }
        .mapNotNull { pedido ->
            val fecha = runCatching { Instant.parse(pedido.fecha).atZone(zona).toLocalDate() }.getOrNull()
                ?: return@mapNotNull null
            fecha to pedido.productos
        }
        .flatMap { (fecha, productos) ->
            productos.asSequence().filter { it.productoId.isNotBlank() && it.cantidad > 0 }
                .map { LineaFechada(fecha, it.productoId, it.nombre, it.marca, it.cantidad) }
        }.toList()

    return lineas.groupBy { it.productoId }.map { (productoId, ventas) ->
        val porDia = ventas.groupBy { it.fecha }.mapValues { (_, lineasDia) -> lineasDia.sumOf { it.cantidad } }
        val primera = porDia.keys.minOrNull()!!
        val ultima = porDia.keys.maxOrNull()!!
        val etiqueta = ventas.maxBy { it.fecha }
        val observaciones = generateSequence(primera) { anterior ->
            anterior.plusDays(1).takeIf { !it.isAfter(ultima) }
        }.map { fecha -> ObservacionDemanda(fecha, porDia[fecha]?.toDouble() ?: 0.0) }.toList()
        SerieDemandaProducto(productoId, etiqueta.nombre, etiqueta.marca, observaciones)
    }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.nombre })
}

fun generarPronosticoDemanda(
    serie: SerieDemandaProducto,
    horizonte: Int = CriteriosPronosticoDemanda.HORIZONTE_PREDETERMINADO
): ResultadoPronosticoDemanda {
    require(horizonte in CriteriosPronosticoDemanda.HORIZONTES_PERMITIDOS) { "Horizonte no permitido." }
    val datos = serie.observaciones.map { it.unidades }
    if (datos.size < CriteriosPronosticoDemanda.MINIMO_OBSERVACIONES) {
        return insuficiente(serie, "Se requieren al menos ${CriteriosPronosticoDemanda.MINIMO_OBSERVACIONES} observaciones diarias.")
    }
    if (serie.diasConDemanda < CriteriosPronosticoDemanda.MINIMO_DIAS_CON_DEMANDA) {
        return insuficiente(serie, "Se requieren al menos ${CriteriosPronosticoDemanda.MINIMO_DIAS_CON_DEMANDA} días con demanda mayor que cero.")
    }

    val validacionCantidad = max(CriteriosPronosticoDemanda.MINIMO_VALIDACION, ceil(datos.size * 0.2).toInt())
    val entrenamientoCantidad = datos.size - validacionCantidad
    if (entrenamientoCantidad < CriteriosPronosticoDemanda.MINIMO_ENTRENAMIENTO) {
        return insuficiente(serie, "No hay suficientes observaciones para separar entrenamiento y validación.")
    }
    val entrenamiento = datos.take(entrenamientoCantidad)
    if (entrenamiento.count { it > 0.0 } < CriteriosPronosticoDemanda.MINIMO_DIAS_CON_DEMANDA_ENTRENAMIENTO) {
        return insuficiente(serie, "El tramo de entrenamiento no contiene suficiente demanda real.")
    }

    val modeloValidacion = ajustarHolt(entrenamiento)
    val validacionReal = datos.drop(entrenamientoCantidad)
    val prediccionValidacion = pronosticar(modeloValidacion, validacionReal.size)
    val errores = validacionReal.zip(prediccionValidacion).map { (real, estimado) -> real - estimado }
    val mae = errores.sumOf { kotlin.math.abs(it) } / errores.size
    val rmse = sqrt(errores.sumOf { it * it } / errores.size)
    val fechasValidacion = serie.observaciones.drop(entrenamientoCantidad).map { it.fecha }

    val modeloFinal = ajustarHolt(datos)
    val ultimaFecha = requireNotNull(serie.ultimaFecha)
    val futuro = pronosticar(modeloFinal, horizonte).mapIndexed { indice, unidades ->
        PuntoPronostico(ultimaFecha.plusDays(indice + 1L), unidades)
    }
    return ResultadoPronosticoDemanda(
        serie = serie,
        estado = EstadoPronosticoDemanda.DISPONIBLE,
        observacionesEntrenamiento = entrenamientoCantidad,
        observacionesValidacion = validacionCantidad,
        validacion = fechasValidacion.zip(prediccionValidacion).map { PuntoPronostico(it.first, it.second) },
        futuro = futuro,
        alpha = modeloFinal.alpha,
        beta = modeloFinal.beta,
        mae = mae,
        rmse = rmse
    )
}

fun calcularMae(reales: List<Double>, estimados: List<Double>): Double {
    require(reales.isNotEmpty() && reales.size == estimados.size)
    return reales.indices.sumOf { kotlin.math.abs(reales[it] - estimados[it]) } / reales.size
}

fun calcularRmse(reales: List<Double>, estimados: List<Double>): Double {
    require(reales.isNotEmpty() && reales.size == estimados.size)
    return sqrt(reales.indices.sumOf { val e = reales[it] - estimados[it]; e * e } / reales.size)
}

private fun insuficiente(serie: SerieDemandaProducto, motivo: String) = ResultadoPronosticoDemanda(
    serie = serie, estado = EstadoPronosticoDemanda.DATOS_INSUFICIENTES, motivo = motivo
)

/** Ajusta alpha y beta por búsqueda acotada, minimizando el error cuadrático de un paso. */
private fun ajustarHolt(datos: List<Double>): EstadoHolt {
    require(datos.size >= 2)
    var mejor: EstadoHolt? = null
    var menorError = Double.POSITIVE_INFINITY
    for (a in 1..19) for (b in 1..19) {
        val alpha = a / 20.0
        val beta = b / 20.0
        var nivel = datos[0]
        var tendencia = datos[1] - datos[0]
        var error = 0.0
        for (indice in 1 until datos.size) {
            val estimado = (nivel + tendencia).coerceAtLeast(0.0)
            val diferencia = datos[indice] - estimado
            error += diferencia * diferencia
            val nuevoNivel = alpha * datos[indice] + (1 - alpha) * (nivel + tendencia)
            val nuevaTendencia = beta * (nuevoNivel - nivel) + (1 - beta) * tendencia
            nivel = nuevoNivel
            tendencia = nuevaTendencia
        }
        if (error < menorError) {
            menorError = error
            mejor = EstadoHolt(alpha, beta, nivel, tendencia)
        }
    }
    return requireNotNull(mejor)
}

private fun pronosticar(modelo: EstadoHolt, periodos: Int): List<Double> =
    (1..periodos).map { paso -> (modelo.nivel + paso * modelo.tendencia).coerceAtLeast(0.0) }
