package com.example.vapeon_movil.entities

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Zona horaria única de los reportes; coincide con Caja y Pronóstico. */
internal val zonaEstadistica: ZoneId = ZoneId.of("America/Lima")

private val formatoRangoEstadistica = DateTimeFormatter.ofPattern("dd/MM/uuuu")
private val formatoTextoRango = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val localeEstadistica = Locale.forLanguageTag("es-PE")
private val formatoDiaLargo = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", localeEstadistica)
private val formatoMesLargo = DateTimeFormatter.ofPattern("MMMM yyyy", localeEstadistica)
private val mesesCortos = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun",
    "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")

enum class PeriodoEstadistica(
    val etiqueta: String,
    val etiquetaSerie: String,
    val etiquetaMejor: String,
    val promedioEtiqueta: String?
) {
    DIA("Día", "Ventas por franja horaria", "Mejor franja", null),
    MES("Mes", "Evolución diaria", "Mejor día", "Promedio por día con ventas"),
    ANIO("Año", "Evolución mensual", "Mejor mes", "Promedio por mes con ventas")
}

/** Rango inclusivo de días locales de America/Lima. */
data class RangoEstadistica(val inicio: LocalDate, val fin: LocalDate) {
    init {
        require(!inicio.isAfter(fin)) { "El rango de estadísticas es inválido." }
    }

    fun contiene(fecha: LocalDate): Boolean = !fecha.isBefore(inicio) && !fecha.isAfter(fin)
}

data class PuntoSerieEstadistica(
    val clave: String,
    val etiqueta: String,
    val total: BigDecimal,
    val pedidos: Int,
    val unidades: Int
)

data class IngresoMarcaEstadistica(
    val marca: String,
    val unidades: Int,
    val ingreso: BigDecimal
)

data class IngresoMetodoPago(
    val metodo: String,
    val pedidos: Int,
    val total: BigDecimal
)

/** Resumen de ventas entregadas de un día, mes o año calendario. */
data class EstadisticasPeriodo(
    val tipo: PeriodoEstadistica,
    val ancla: LocalDate,
    val rango: RangoEstadistica,
    val etiquetaPeriodo: String,
    val caja: CajaTotal,
    val pedidos: Int,
    val unidades: Int,
    val total: BigDecimal,
    val productos: BigDecimal,
    val delivery: BigDecimal,
    val ticketPromedio: BigDecimal,
    val clientes: Int,
    val topProductos: List<IngresoProductoCaja>,
    val categorias: List<IngresoCategoriaCaja>,
    val marcas: List<IngresoMarcaEstadistica>,
    val metodosPago: List<IngresoMetodoPago>,
    val serie: List<PuntoSerieEstadistica>,
    val mejorPunto: PuntoSerieEstadistica?,
    val promedio: BigDecimal?,
    val totalAnterior: BigDecimal,
    val variacionPorcentual: BigDecimal?
) {
    val rangoTexto: String
        get() = "${rango.inicio.format(formatoTextoRango)} – ${rango.fin.format(formatoTextoRango)}"
}

fun rangoEstadistica(tipo: PeriodoEstadistica, ancla: LocalDate): RangoEstadistica = when (tipo) {
    PeriodoEstadistica.DIA -> RangoEstadistica(ancla, ancla)
    PeriodoEstadistica.MES -> RangoEstadistica(
        ancla.withDayOfMonth(1),
        ancla.withDayOfMonth(ancla.lengthOfMonth())
    )
    PeriodoEstadistica.ANIO -> RangoEstadistica(
        ancla.withDayOfYear(1),
        ancla.withDayOfYear(ancla.lengthOfYear())
    )
}

fun moverAnclaEstadistica(tipo: PeriodoEstadistica, ancla: LocalDate, pasos: Int): LocalDate =
    when (tipo) {
        PeriodoEstadistica.DIA -> ancla.plusDays(pasos.toLong())
        PeriodoEstadistica.MES -> ancla.plusMonths(pasos.toLong())
        PeriodoEstadistica.ANIO -> ancla.plusYears(pasos.toLong())
    }

/**
 * Calcula únicamente pedidos ENTREGADO con snapshots válidos. El periodo anterior se usa solo
 * para la comparación y nunca se mezcla con los totales del periodo consultado.
 */
fun calcularEstadisticas(
    pedidos: List<Pedido>,
    tipo: PeriodoEstadistica,
    ancla: LocalDate
): EstadisticasPeriodo {
    val rango = rangoEstadistica(tipo, ancla)
    val caja = calcularCaja(
        pedidos,
        rango.inicio.format(formatoRangoEstadistica),
        rango.fin.format(formatoRangoEstadistica)
    )
    val instantes = caja.pedidos.mapNotNull { pedido ->
        runCatching {
            pedido.id to Instant.parse(pedido.fecha).atZone(zonaEstadistica).toLocalDateTime()
        }.getOrNull()
    }.toMap()
    val total = caja.total
    val cantidadPedidos = caja.pedidos.size
    val ticketPromedio = if (cantidadPedidos == 0) {
        BigDecimal.ZERO.setScale(2)
    } else {
        total.divide(BigDecimal(cantidadPedidos), 2, RoundingMode.HALF_UP)
    }
    val serie = serieEstadistica(tipo, rango, caja.pedidos, instantes)
    val mejorPunto = serie.filter { it.pedidos > 0 }.maxByOrNull { it.total }
    val puntosConVentas = serie.count { it.pedidos > 0 }
    val promedio = when (tipo) {
        PeriodoEstadistica.DIA -> null
        else -> if (puntosConVentas == 0) null
        else total.divide(BigDecimal(puntosConVentas), 2, RoundingMode.HALF_UP)
    }
    val rangoAnterior = rangoEstadistica(tipo, moverAnclaEstadistica(tipo, ancla, -1))
    val totalAnterior = calcularCaja(
        pedidos,
        rangoAnterior.inicio.format(formatoRangoEstadistica),
        rangoAnterior.fin.format(formatoRangoEstadistica)
    ).total
    val variacion = if (totalAnterior.signum() > 0) {
        total.subtract(totalAnterior)
            .multiply(BigDecimal(100))
            .divide(totalAnterior, 1, RoundingMode.HALF_UP)
    } else {
        null
    }
    val productos = caja.reporteProductos()

    return EstadisticasPeriodo(
        tipo = tipo,
        ancla = ancla,
        rango = rango,
        etiquetaPeriodo = etiquetaPeriodo(tipo, ancla),
        caja = caja,
        pedidos = cantidadPedidos,
        unidades = caja.unidadesVendidas,
        total = total,
        productos = caja.productos,
        delivery = caja.delivery,
        ticketPromedio = ticketPromedio,
        clientes = caja.pedidos.map { it.usuarioId }.filter { it.isNotBlank() }.distinct().size,
        topProductos = productos
            .sortedWith(compareByDescending<IngresoProductoCaja> { it.unidades }
                .thenByDescending { it.ingreso }
                .thenBy { it.nombre })
            .take(5),
        categorias = caja.reporteCategorias().take(5),
        marcas = reporteMarcas(productos),
        metodosPago = reporteMetodosPago(caja),
        serie = serie,
        mejorPunto = mejorPunto,
        promedio = promedio,
        totalAnterior = totalAnterior,
        variacionPorcentual = variacion
    )
}

private fun etiquetaPeriodo(tipo: PeriodoEstadistica, ancla: LocalDate): String = when (tipo) {
    PeriodoEstadistica.DIA -> ancla.format(formatoDiaLargo).capitalizar()
    PeriodoEstadistica.MES -> ancla.format(formatoMesLargo).capitalizar()
    PeriodoEstadistica.ANIO -> ancla.year.toString()
}

private fun String.capitalizar(): String = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(localeEstadistica) else it.toString()
}

private fun serieEstadistica(
    tipo: PeriodoEstadistica,
    rango: RangoEstadistica,
    pedidos: List<Pedido>,
    instantes: Map<String, LocalDateTime>
): List<PuntoSerieEstadistica> = when (tipo) {
    PeriodoEstadistica.DIA -> listOf(0..5, 6..11, 12..17, 18..23).map { horas ->
        val dentro = pedidos.filter { pedido ->
            instantes[pedido.id]?.hour?.let { it in horas } == true
        }
        val etiqueta = String.format(Locale.ROOT, "%02d–%02d", horas.first, horas.last)
        puntoSerie(etiqueta, etiqueta, dentro)
    }
    PeriodoEstadistica.MES -> (1..rango.fin.dayOfMonth).map { dia ->
        val dentro = pedidos.filter { instantes[it.id]?.dayOfMonth == dia }
        puntoSerie("dia-$dia", dia.toString(), dentro)
    }
    PeriodoEstadistica.ANIO -> (1..12).map { mes ->
        val dentro = pedidos.filter { instantes[it.id]?.monthValue == mes }
        puntoSerie("mes-$mes", mesesCortos[mes - 1], dentro)
    }
}

private fun puntoSerie(clave: String, etiqueta: String, pedidos: List<Pedido>) =
    PuntoSerieEstadistica(
        clave = clave,
        etiqueta = etiqueta,
        total = pedidos.fold(BigDecimal.ZERO) { suma, pedido -> suma + pedido.total },
        pedidos = pedidos.size,
        unidades = pedidos.sumOf { pedido -> pedido.productos.sumOf { it.cantidad } }
    )

private fun reporteMarcas(productos: List<IngresoProductoCaja>): List<IngresoMarcaEstadistica> =
    productos
        .groupBy { claveMarcaCaja(it.marca) }
        .map { (_, productos) ->
            IngresoMarcaEstadistica(
                marca = productos.first().marca.trim(),
                unidades = productos.sumOf { it.unidades },
                ingreso = productos.fold(BigDecimal.ZERO) { suma, producto -> suma + producto.ingreso }
            )
        }
        .sortedWith(compareByDescending<IngresoMarcaEstadistica> { it.ingreso }.thenBy { it.marca })

private fun reporteMetodosPago(caja: CajaTotal): List<IngresoMetodoPago> =
    caja.pedidos
        .groupBy { it.metodoPago.trim().uppercase(Locale.ROOT).ifBlank { "NO ESPECIFICADO" } }
        .map { (metodo, pedidos) ->
            IngresoMetodoPago(
                metodo = metodo,
                pedidos = pedidos.size,
                total = pedidos.fold(BigDecimal.ZERO) { suma, pedido -> suma + pedido.total }
            )
        }
        .sortedWith(compareByDescending<IngresoMetodoPago> { it.total }.thenBy { it.metodo })
