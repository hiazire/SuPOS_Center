package com.example.supos.ui.performance

import com.example.supos.data.network.HistoryOrderPayload
import org.junit.Assert.assertEquals
import org.junit.Test

class SalesReportCalculatorTest {
    @Test
    fun calculate_onlyPaidOrdersContributeToRevenue() {
        val report = SalesReportCalculator.calculate(
            listOf(
                order("A", "2026-08-13 09:00:00", 180.0, "PAID", "PRINTED"),
                order("B", "2026-08-13 10:00:00", 120.0, "PAID", "READY"),
                order("C", "2026-08-13 11:00:00", 250.0, "UNPAID", "PENDING"),
                order("D", "2026-08-13 12:00:00", 90.0, "UNPAID", "CANCELED")
            )
        )

        assertEquals(300.0, report.revenue, 0.001)
        assertEquals(2, report.paidOrderCount)
        assertEquals(150.0, report.averageTicket, 0.001)
        assertEquals(1, report.pendingOrderCount)
        assertEquals(1, report.dailySales.size)
    }

    @Test
    fun calculate_emptyOrders_returnsZeroSummary() {
        assertEquals(SalesReport(), SalesReportCalculator.calculate(emptyList()))
    }

    private fun order(
        id: String,
        timestamp: String,
        total: Double,
        paymentStatus: String,
        state: String
    ) = HistoryOrderPayload(
        orderId = id,
        timestamp = timestamp,
        details = "[]",
        state = state,
        totalAmount = total,
        paymentStatus = paymentStatus
    )
}
