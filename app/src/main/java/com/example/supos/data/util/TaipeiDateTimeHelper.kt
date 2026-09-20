package com.example.supos.data.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TaipeiDateTimeHelper {

    val TAIPEI_TIME_ZONE: TimeZone = TimeZone.getTimeZone("Asia/Taipei")

    private val DATE_FORMATS = listOf(
        "EEE MMM dd yyyy HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy/MM/dd HH:mm:ss",
        "yyyy-MM-dd",
        "yyyy/MM/dd"
    )

    /**
     * 取得指定基準時間當日的台北時間半開區間 [startInclusive, endExclusive)
     */
    fun getTodayInterval(referenceMillis: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val cal = Calendar.getInstance(TAIPEI_TIME_ZONE)
        cal.timeInMillis = referenceMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startInclusive = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 1)
        val endExclusive = cal.timeInMillis

        return Pair(startInclusive, endExclusive)
    }

    /**
     * 取得指定基準時間昨日的台北時間半開區間 [startInclusive, endExclusive)
     */
    fun getYesterdayInterval(referenceMillis: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val (todayStart, _) = getTodayInterval(referenceMillis)
        val cal = Calendar.getInstance(TAIPEI_TIME_ZONE)
        cal.timeInMillis = todayStart
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStart = cal.timeInMillis

        return Pair(yesterdayStart, todayStart)
    }

    /**
     * 取得自訂起訖日期的台北時間半開區間 [startInclusive, endExclusive)
     */
    fun getCustomInterval(startMillis: Long, endMillis: Long): Pair<Long, Long> {
        val startCal = Calendar.getInstance(TAIPEI_TIME_ZONE)
        startCal.timeInMillis = startMillis
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)
        val startInclusive = startCal.timeInMillis

        val endCal = Calendar.getInstance(TAIPEI_TIME_ZONE)
        endCal.timeInMillis = endMillis
        endCal.set(Calendar.HOUR_OF_DAY, 0)
        endCal.set(Calendar.MINUTE, 0)
        endCal.set(Calendar.SECOND, 0)
        endCal.set(Calendar.MILLISECOND, 0)
        endCal.add(Calendar.DAY_OF_YEAR, 1)
        val endExclusive = endCal.timeInMillis

        return Pair(startInclusive, endExclusive)
    }

    /**
     * 多格式容錯時間解析器，回傳 UTC Epoch Millis
     */
    fun parseTimestampToTaipeiMillis(timestamp: String?): Long? {
        if (timestamp.isNullOrBlank()) return null

        val trimmed = timestamp.trim()

        // 1. 若為純數字 timestamp
        trimmed.toLongOrNull()?.let { return it }

        // 2. 取前 24 碼嘗試 RFC 格式（"Thu Sep 17 2026 21:00:33"）
        val candidate = if (trimmed.length > 24) trimmed.take(24) else trimmed

        for (pattern in DATE_FORMATS) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = if (pattern.contains("'Z'")) TimeZone.getTimeZone("UTC") else TAIPEI_TIME_ZONE
                    isLenient = false
                }
                val parsed = sdf.parse(candidate)
                if (parsed != null) {
                    return parsed.time
                }
            } catch (_: Exception) {
                // Try next format
            }
        }

        return null
    }
}
