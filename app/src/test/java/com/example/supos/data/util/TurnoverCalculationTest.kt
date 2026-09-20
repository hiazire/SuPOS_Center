package com.example.supos.data.util

import com.example.supos.data.network.HistoryOrderPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToLong

class TurnoverCalculationTest {

    @Test
    fun dateParsing_supportsMultipleFormatsGracefully() {
        // RFC Style
        val rfcTime = TaipeiDateTimeHelper.parseTimestampToTaipeiMillis("Thu Sep 17 2026 21:00:33")
        assertNotNull("RFC format should parse", rfcTime)

        // ISO 8601 with Z
        val isoZTime = TaipeiDateTimeHelper.parseTimestampToTaipeiMillis("2026-09-17T13:00:33.000Z")
        assertNotNull("ISO Z format should parse", isoZTime)

        // ISO 8601 without Z
        val isoTime = TaipeiDateTimeHelper.parseTimestampToTaipeiMillis("2026-09-17T21:00:33")
        assertNotNull("ISO format should parse", isoTime)

        // Standard hyphen
        val stdHyphen = TaipeiDateTimeHelper.parseTimestampToTaipeiMillis("2026-09-17 21:00:33")
        assertNotNull("Standard hyphen format should parse", stdHyphen)

        // Standard slash
        val stdSlash = TaipeiDateTimeHelper.parseTimestampToTaipeiMillis("2026/09/17 21:00:33")
        assertNotNull("Standard slash format should parse", stdSlash)

        // Epoch millis string
        val millisTime = TaipeiDateTimeHelper.parseTimestampToTaipeiMillis("1789640433000")
        assertEquals(1789640433000L, millisTime)

        // Invalid inputs
        assertNull("Invalid text should return null", TaipeiDateTimeHelper.parseTimestampToTaipeiMillis("random-garbage"))
        assertNull("Empty text should return null", TaipeiDateTimeHelper.parseTimestampToTaipeiMillis(""))
        assertNull("Null input should return null", TaipeiDateTimeHelper.parseTimestampToTaipeiMillis(null))
    }

    @Test
    fun turnover_calculatesTodayOrdersAcrossMidnightInTaipeiTime() {
        val cal = Calendar.getInstance(TaipeiDateTimeHelper.TAIPEI_TIME_ZONE).apply {
            set(2026, Calendar.SEPTEMBER, 17, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val refMillis = cal.timeInMillis

        val interval = TaipeiDateTimeHelper.getTodayInterval(refMillis)
        val (start, end) = interval

        // Order 1: Exactly at Taipei midnight 00:00:00 (Included)
        val t1 = Calendar.getInstance(TaipeiDateTimeHelper.TAIPEI_TIME_ZONE).apply {
            set(2026, Calendar.SEPTEMBER, 17, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val order1 = createOrder("O1", t1.toString(), 100.0, "PAID")

        // Order 2: At 23:59:59 (Included)
        val t2 = Calendar.getInstance(TaipeiDateTimeHelper.TAIPEI_TIME_ZONE).apply {
            set(2026, Calendar.SEPTEMBER, 17, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        val order2 = createOrder("O2", t2.toString(), 150.0, "PAID")

        // Order 3: Previous day 23:59:59 (Excluded from today)
        val t3 = Calendar.getInstance(TaipeiDateTimeHelper.TAIPEI_TIME_ZONE).apply {
            set(2026, Calendar.SEPTEMBER, 16, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        val order3 = createOrder("O3", t3.toString(), 200.0, "PAID")

        // Order 4: Next day 00:00:00 (Excluded from today)
        val t4 = Calendar.getInstance(TaipeiDateTimeHelper.TAIPEI_TIME_ZONE).apply {
            set(2026, Calendar.SEPTEMBER, 18, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val order4 = createOrder("O4", t4.toString(), 300.0, "PAID")

        val orders = listOf(order1, order2, order3, order4)
        val total = TurnoverCalculator.calculateTurnover(orders, interval)

        // Only O1 (100) + O2 (150) = 250
        assertEquals(250L, total)
    }

    @Test
    fun turnoverAndHistory_filterConsistently() {
        val cal = Calendar.getInstance(TaipeiDateTimeHelper.TAIPEI_TIME_ZONE).apply {
            set(2026, Calendar.SEPTEMBER, 17, 15, 30, 0)
        }
        val refMillis = cal.timeInMillis
        val todayInterval = TaipeiDateTimeHelper.getTodayInterval(refMillis)

        val orders = listOf(
            createOrder("ORD-PAID-1", "2026-09-17 10:00:00", 120.0, "PAID"),
            createOrder("ORD-PAID-2", "2026-09-17 14:00:00", 280.0, "PAID"),
            createOrder("ORD-UNPAID", "2026-09-17 16:00:00", 999.0, "UNPAID"),
            createOrder("ORD-OLD", "2026-09-16 10:00:00", 500.0, "PAID")
        )

        val turnover = TurnoverCalculator.calculateTurnover(orders, todayInterval)
        val filteredHistoryOrders = TurnoverCalculator.filterOrdersInInterval(orders, todayInterval)

        val historyPaidSum = filteredHistoryOrders
            .filter { it.paymentStatus.equals("PAID", ignoreCase = true) }
            .sumOf { it.totalAmount.roundToLong() }

        assertEquals(400L, turnover)
        assertEquals(turnover, historyPaidSum)
        assertEquals("History should show 3 orders for today (including unpaid)", 3, filteredHistoryOrders.size)
    }

    private fun createOrder(id: String, timestamp: String, amount: Double, paymentStatus: String): HistoryOrderPayload {
        return HistoryOrderPayload(
            orderId = id,
            timestamp = timestamp,
            totalAmount = amount,
            paymentStatus = paymentStatus,
            details = "[]",
            state = "READY"
        )
    }
}
