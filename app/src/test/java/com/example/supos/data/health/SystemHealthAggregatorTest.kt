package com.example.supos.data.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemHealthAggregatorTest {

    private val now = 1773700000000L

    @Test
    fun aggregator_allHealthy_returnsReady() {
        val report = SystemHealthAggregator.aggregate(
            network = NetworkHealth(isOnline = true),
            api = ApiHealth(status = SubsystemStatus.OK, latencyMs = 120),
            menu = MenuHealth(status = SubsystemStatus.OK, lastSyncAt = now - 3600_000L, itemCount = 30),
            outbox = OutboxHealth(status = SubsystemStatus.OK, pendingCount = 0, consecutiveFailures = 0),
            liveOrder = LiveOrderHealth(status = SubsystemStatus.OK, lastPollAt = now - 5000L, isPollingActive = true),
            database = DatabaseHealth(status = SubsystemStatus.OK, isAccessible = true),
            nowMs = now
        )

        assertEquals(HealthLevel.READY, report.overallLevel)
        assertEquals("● 系統就緒", report.badgeText)
        assertTrue(report.recommendations.any { it.contains("系統就緒") })
    }

    @Test
    fun aggregator_offline_returnsOfflineAndNeverReady() {
        val report = SystemHealthAggregator.aggregate(
            network = NetworkHealth(isOnline = false),
            api = ApiHealth(status = SubsystemStatus.OK),
            menu = MenuHealth(status = SubsystemStatus.OK, lastSyncAt = now, itemCount = 30),
            outbox = OutboxHealth(status = SubsystemStatus.OK, pendingCount = 0),
            liveOrder = LiveOrderHealth(status = SubsystemStatus.OK),
            database = DatabaseHealth(status = SubsystemStatus.OK, isAccessible = true),
            nowMs = now
        )

        assertEquals(HealthLevel.OFFLINE, report.overallLevel)
        assertEquals("○ 離線模式", report.badgeText)
        assertFalse(report.overallLevel == HealthLevel.READY)
        assertTrue(report.recommendations.any { it.contains("離線狀態") })
    }

    @Test
    fun aggregator_pendingOutboxOrders_returnsDegraded() {
        val report = SystemHealthAggregator.aggregate(
            network = NetworkHealth(isOnline = true),
            api = ApiHealth(status = SubsystemStatus.OK),
            menu = MenuHealth(status = SubsystemStatus.OK, lastSyncAt = now, itemCount = 30),
            outbox = OutboxHealth(status = SubsystemStatus.WARNING, pendingCount = 4, consecutiveFailures = 0),
            liveOrder = LiveOrderHealth(status = SubsystemStatus.OK),
            database = DatabaseHealth(status = SubsystemStatus.OK, isAccessible = true),
            nowMs = now
        )

        assertEquals(HealthLevel.DEGRADED, report.overallLevel)
        assertEquals("▲ 降級運作", report.badgeText)
        assertTrue(report.recommendations.any { it.contains("4 筆離線訂單") })
    }

    @Test
    fun aggregator_staleMenu_returnsDegraded() {
        // 25 小時前更新的菜單已逾期
        val staleSyncTime = now - (25 * 3600 * 1000L)
        val report = SystemHealthAggregator.aggregate(
            network = NetworkHealth(isOnline = true),
            api = ApiHealth(status = SubsystemStatus.OK),
            menu = MenuHealth(status = SubsystemStatus.OK, lastSyncAt = staleSyncTime, itemCount = 30),
            outbox = OutboxHealth(status = SubsystemStatus.OK, pendingCount = 0),
            liveOrder = LiveOrderHealth(status = SubsystemStatus.OK),
            database = DatabaseHealth(status = SubsystemStatus.OK, isAccessible = true),
            nowMs = now
        )

        assertEquals(HealthLevel.DEGRADED, report.overallLevel)
        assertTrue(report.recommendations.any { it.contains("24 小時") })
    }

    @Test
    fun aggregator_consecutiveOutboxFailures_returnsActionRequired() {
        val report = SystemHealthAggregator.aggregate(
            network = NetworkHealth(isOnline = true),
            api = ApiHealth(status = SubsystemStatus.OK),
            menu = MenuHealth(status = SubsystemStatus.OK, lastSyncAt = now, itemCount = 30),
            outbox = OutboxHealth(status = SubsystemStatus.ERROR, pendingCount = 3, consecutiveFailures = 3, lastFailureMessage = "GAS 500 error"),
            liveOrder = LiveOrderHealth(status = SubsystemStatus.OK),
            database = DatabaseHealth(status = SubsystemStatus.OK, isAccessible = true),
            nowMs = now
        )

        assertEquals(HealthLevel.ACTION_REQUIRED, report.overallLevel)
        assertEquals("✕ 需要處理", report.badgeText)
        assertTrue(report.recommendations.any { it.contains("同步持續失敗") })
    }

    @Test
    fun aggregator_databaseError_returnsActionRequired() {
        val report = SystemHealthAggregator.aggregate(
            network = NetworkHealth(isOnline = true),
            api = ApiHealth(status = SubsystemStatus.OK),
            menu = MenuHealth(status = SubsystemStatus.OK, lastSyncAt = now, itemCount = 30),
            outbox = OutboxHealth(status = SubsystemStatus.OK, pendingCount = 0),
            liveOrder = LiveOrderHealth(status = SubsystemStatus.OK),
            database = DatabaseHealth(status = SubsystemStatus.ERROR, isAccessible = false, lastErrorMessage = "Disk I/O failure"),
            nowMs = now
        )

        assertEquals(HealthLevel.ACTION_REQUIRED, report.overallLevel)
        assertTrue(report.recommendations.any { it.contains("資料庫存取異常") })
    }

    @Test
    fun aggregator_precedence_actionRequiredOverridesOfflineAndDegraded() {
        // 同時斷網、有待同步單且資料庫毀損，最高優先級應為 ACTION_REQUIRED
        val report = SystemHealthAggregator.aggregate(
            network = NetworkHealth(isOnline = false),
            api = ApiHealth(status = SubsystemStatus.ERROR),
            menu = MenuHealth(status = SubsystemStatus.WARNING, lastSyncAt = null),
            outbox = OutboxHealth(status = SubsystemStatus.WARNING, pendingCount = 5),
            liveOrder = LiveOrderHealth(status = SubsystemStatus.ERROR),
            database = DatabaseHealth(status = SubsystemStatus.ERROR, isAccessible = false),
            nowMs = now
        )

        assertEquals(HealthLevel.ACTION_REQUIRED, report.overallLevel)
    }

    @Test
    fun sanitizer_masksTokensAndSecrets() {
        val sensitiveMessage = "HTTP 401: Unauthorized with Bearer gho_ABC123456789xyz and token=secretKey999"
        val sanitized = HealthSanitizer.sanitize(sensitiveMessage)

        assertFalse(sanitized!!.contains("gho_ABC123456789xyz"))
        assertFalse(sanitized.contains("secretKey999"))
        assertTrue(sanitized.contains("Bearer [REDACTED]"))
        assertTrue(sanitized.contains("token=[REDACTED]"))
    }
}
