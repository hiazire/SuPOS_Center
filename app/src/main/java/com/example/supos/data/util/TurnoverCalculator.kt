package com.example.supos.data.util

import com.example.supos.data.network.HistoryOrderPayload
import kotlin.math.roundToLong

object TurnoverCalculator {

    /**
     * 計算指定半開區間 [startInclusive, endExclusive) 內的已付款（PAID）訂單總營業額（整數元）
     */
    fun calculateTurnover(
        orders: List<HistoryOrderPayload>,
        interval: Pair<Long, Long>
    ): Long {
        val (startInclusive, endExclusive) = interval

        return orders
            .asSequence()
            .filter { it.paymentStatus.equals("PAID", ignoreCase = true) }
            .filter { order ->
                val time = TaipeiDateTimeHelper.parseTimestampToTaipeiMillis(order.timestamp)
                time != null && time >= startInclusive && time < endExclusive
            }
            .map { it.totalAmount.roundToLong() }
            .sum()
    }

    /**
     * 篩選指定半開區間 [startInclusive, endExclusive) 內的有效訂單
     */
    fun filterOrdersInInterval(
        orders: List<HistoryOrderPayload>,
        interval: Pair<Long, Long>
    ): List<HistoryOrderPayload> {
        val (startInclusive, endExclusive) = interval

        return orders.filter { order ->
            val time = TaipeiDateTimeHelper.parseTimestampToTaipeiMillis(order.timestamp)
            time != null && time >= startInclusive && time < endExclusive
        }
    }
}
