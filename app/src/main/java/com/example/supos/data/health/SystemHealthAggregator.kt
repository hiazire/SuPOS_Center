package com.example.supos.data.health

object SystemHealthAggregator {

    private const val MENU_STALE_THRESHOLD_MS = 24 * 60 * 60 * 1000L // 24 小時

    fun aggregate(
        network: NetworkHealth,
        api: ApiHealth,
        menu: MenuHealth,
        outbox: OutboxHealth,
        liveOrder: LiveOrderHealth,
        database: DatabaseHealth,
        nowMs: Long = System.currentTimeMillis()
    ): SystemHealthReport {
        val isMenuStale = menu.isStale ||
                menu.lastSyncAt == null ||
                (nowMs - (menu.lastSyncAt ?: 0L)) > MENU_STALE_THRESHOLD_MS

        val updatedMenu = menu.copy(
            isStale = isMenuStale,
            status = when {
                isMenuStale -> SubsystemStatus.WARNING
                menu.status == SubsystemStatus.ERROR -> SubsystemStatus.ERROR
                else -> SubsystemStatus.OK
            }
        )

        val isActionRequired = !database.isAccessible ||
                database.status == SubsystemStatus.ERROR ||
                outbox.consecutiveFailures >= 3 ||
                outbox.status == SubsystemStatus.ERROR

        val isOffline = !network.isOnline

        val isDegraded = outbox.pendingCount > 0 ||
                isMenuStale ||
                api.status == SubsystemStatus.WARNING ||
                api.status == SubsystemStatus.ERROR ||
                liveOrder.status == SubsystemStatus.WARNING ||
                liveOrder.status == SubsystemStatus.ERROR ||
                !liveOrder.isPollingActive

        val overallLevel = when {
            isActionRequired -> HealthLevel.ACTION_REQUIRED
            isOffline -> HealthLevel.OFFLINE
            isDegraded -> HealthLevel.DEGRADED
            else -> HealthLevel.READY
        }

        val diagnostics = mutableListOf<SubsystemDiagnostic>()

        // 1. Network
        diagnostics.add(
            SubsystemDiagnostic(
                name = "網路連線",
                status = if (network.isOnline) SubsystemStatus.OK else SubsystemStatus.ERROR,
                detail = if (network.isOnline) "已連線至網路${network.networkType?.let { " ($it)" } ?: ""}" else "目前無網路連線 (離線)",
                timestamp = network.lastCheckedAt
            )
        )

        // 2. Cloud API
        diagnostics.add(
            SubsystemDiagnostic(
                name = "雲端 API (GAS)",
                status = api.status,
                detail = when {
                    api.lastErrorMessage != null -> "異常: ${HealthSanitizer.sanitize(api.lastErrorMessage)}"
                    api.latencyMs != null -> "正常 (延遲 ${api.latencyMs} ms)"
                    else -> "正常"
                },
                timestamp = api.lastSuccessAt
            )
        )

        // 3. Menu Sync
        diagnostics.add(
            SubsystemDiagnostic(
                name = "菜單快取",
                status = updatedMenu.status,
                detail = when {
                    menu.lastSyncAt == null -> "尚未進行初次同步"
                    isMenuStale -> "菜單快取已逾 24 小時 (${menu.itemCount} 項商品)"
                    else -> "已同步 (${menu.itemCount} 項商品)"
                },
                timestamp = menu.lastSyncAt
            )
        )

        // 4. Outbox Sync Queue
        diagnostics.add(
            SubsystemDiagnostic(
                name = "離線同步佇列 (Outbox)",
                status = when {
                    outbox.consecutiveFailures >= 3 -> SubsystemStatus.ERROR
                    outbox.pendingCount > 0 -> SubsystemStatus.WARNING
                    else -> SubsystemStatus.OK
                },
                detail = when {
                    outbox.consecutiveFailures >= 3 -> "連續失敗 ${outbox.consecutiveFailures} 次 (${outbox.pendingCount} 筆待同步): ${HealthSanitizer.sanitize(outbox.lastFailureMessage)}"
                    outbox.pendingCount > 0 -> "待同步訂單 ${outbox.pendingCount} 筆"
                    else -> "無待同步訂單，所有交易皆已同步至雲端"
                },
                timestamp = nowMs
            )
        )

        // 5. Live Orders
        diagnostics.add(
            SubsystemDiagnostic(
                name = "線上訂單接收",
                status = liveOrder.status,
                detail = when {
                    !liveOrder.isPollingActive -> "未在後台輪詢"
                    liveOrder.lastPollAt != null -> "輪詢正常運作中"
                    else -> "就緒"
                },
                timestamp = liveOrder.lastPollAt ?: liveOrder.lastOrderReceivedAt
            )
        )

        // 6. Local Database
        diagnostics.add(
            SubsystemDiagnostic(
                name = "本機資料庫 (Room)",
                status = database.status,
                detail = if (database.isAccessible) "資料庫讀寫正常" else "異常: ${HealthSanitizer.sanitize(database.lastErrorMessage)}",
                timestamp = nowMs
            )
        )

        // Recommendations
        val recommendations = mutableListOf<String>()
        if (!database.isAccessible || database.status == SubsystemStatus.ERROR) {
            recommendations.add("本機資料庫存取異常，請立即重啟 App 或聯絡技術支援。")
        }
        if (outbox.consecutiveFailures >= 3) {
            recommendations.add("離線訂單同步持續失敗，請檢查網路連線品質或雲端 API 狀態。")
        }
        if (!network.isOnline) {
            recommendations.add("平板處於離線狀態，線上外送單將暫停接收，現場訂單將安全暫存於本機。")
        } else if (outbox.pendingCount > 0) {
            recommendations.add("目前有 ${outbox.pendingCount} 筆離線訂單等待上傳，將於連線穩定後自動同步。")
        }
        if (isMenuStale) {
            recommendations.add("本機菜單已逾 24 小時未自雲端刷新，建議於開單前手動重新整理菜單。")
        }
        if (recommendations.isEmpty()) {
            recommendations.add("所有系統服務運行健全，系統就緒。")
        }

        val summary = when (overallLevel) {
            HealthLevel.READY -> "所有核心模組運作正常"
            HealthLevel.DEGRADED -> "部分服務降級，營運不受影響但需注意"
            HealthLevel.OFFLINE -> "網路中斷，本機 POS 離線安全模式運作中"
            HealthLevel.ACTION_REQUIRED -> "偵測到嚴重故障，需立即進行營運或系統處置"
        }

        return SystemHealthReport(
            overallLevel = overallLevel,
            badgeText = overallLevel.label,
            badgeColorHex = overallLevel.colorHex,
            summary = summary,
            diagnostics = diagnostics,
            recommendations = recommendations
        )
    }
}
