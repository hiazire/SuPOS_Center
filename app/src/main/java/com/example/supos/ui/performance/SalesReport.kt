package com.example.supos.ui.performance

import com.example.supos.data.network.HistoryOrderPayload
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ReportRange(val label: String, val requestDays: Int) {
    TODAY("今日", 0),
    SEVEN_DAYS("近 7 日", 7),
    THIRTY_DAYS("近 30 日", 30)
}

data class DailySales(
    val dateLabel: String,
    val revenue: Double,
    val paidOrderCount: Int
)

data class SalesReport(
    val revenue: Double = 0.0,
    val paidOrderCount: Int = 0,
    val averageTicket: Double = 0.0,
    val pendingOrderCount: Int = 0,
    val dailySales: List<DailySales> = emptyList()
)

internal object SalesReportCalculator {
    private val timestampPatterns = listOf(
        "EEE MMM dd yyyy HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy/MM/dd HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss"
    )

    fun calculate(orders: List<HistoryOrderPayload>): SalesReport {
        val paidOrders = orders.filter { it.paymentStatus.equals("PAID", ignoreCase = true) }
        val revenue = paidOrders.sumOf { it.totalAmount }
        val pendingCount = orders.count {
            !it.paymentStatus.equals("PAID", ignoreCase = true) &&
                !it.state.equals("CANCELED", ignoreCase = true)
        }

        val daily = paidOrders
            .mapNotNull { order -> parseDate(order.timestamp)?.let { date -> date to order } }
            .groupBy { (date, _) -> SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(date) }
            .toSortedMap(compareByDescending { it })
            .map { (_, entries) ->
                val date = entries.first().first
                DailySales(
                    dateLabel = SimpleDateFormat("MM/dd E", Locale.getDefault()).format(date),
                    revenue = entries.sumOf { it.second.totalAmount },
                    paidOrderCount = entries.size
                )
            }

        return SalesReport(
            revenue = revenue,
            paidOrderCount = paidOrders.size,
            averageTicket = if (paidOrders.isEmpty()) 0.0 else revenue / paidOrders.size,
            pendingOrderCount = pendingCount,
            dailySales = daily
        )
    }

    private fun parseDate(raw: String): Date? {
        val candidates = listOf(raw, raw.take(24))
        for (candidate in candidates) {
            for (pattern in timestampPatterns) {
                val formatter = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                runCatching { formatter.parse(candidate) }.getOrNull()?.let { return it }
            }
        }
        return null
    }
}

