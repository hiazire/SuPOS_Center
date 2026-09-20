package com.example.supos.data.health

enum class HealthLevel(val label: String, val colorHex: Long) {
    READY("● 系統就緒", 0xFF65D6AD),
    DEGRADED("▲ 降級運作", 0xFFFFB74D),
    OFFLINE("○ 離線模式", 0xFF9E9E9E),
    ACTION_REQUIRED("✕ 需要處理", 0xFFE57373)
}

enum class SubsystemStatus(val label: String) {
    OK("正常"),
    WARNING("注意"),
    ERROR("異常"),
    UNKNOWN("未知")
}

data class NetworkHealth(
    val isOnline: Boolean = true,
    val networkType: String? = null,
    val lastCheckedAt: Long = System.currentTimeMillis()
)

data class ApiHealth(
    val status: SubsystemStatus = SubsystemStatus.OK,
    val lastSuccessAt: Long? = null,
    val lastErrorMessage: String? = null,
    val latencyMs: Long? = null
)

data class MenuHealth(
    val status: SubsystemStatus = SubsystemStatus.OK,
    val lastSyncAt: Long? = null,
    val itemCount: Int = 0,
    val isStale: Boolean = false
)

data class OutboxHealth(
    val status: SubsystemStatus = SubsystemStatus.OK,
    val pendingCount: Int = 0,
    val consecutiveFailures: Int = 0,
    val lastFailureMessage: String? = null
)

data class LiveOrderHealth(
    val status: SubsystemStatus = SubsystemStatus.OK,
    val lastPollAt: Long? = null,
    val lastOrderReceivedAt: Long? = null,
    val isPollingActive: Boolean = true
)

data class DatabaseHealth(
    val status: SubsystemStatus = SubsystemStatus.OK,
    val isAccessible: Boolean = true,
    val lastErrorMessage: String? = null
)

data class SubsystemDiagnostic(
    val name: String,
    val status: SubsystemStatus,
    val detail: String,
    val timestamp: Long? = null
)

data class SystemHealthReport(
    val overallLevel: HealthLevel,
    val badgeText: String,
    val badgeColorHex: Long,
    val summary: String,
    val diagnostics: List<SubsystemDiagnostic>,
    val recommendations: List<String>
)
