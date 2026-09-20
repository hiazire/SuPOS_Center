package com.example.supos.data.health

import com.example.supos.data.repository.MenuRepository
import com.example.supos.data.repository.OrderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SystemHealthRepository(
    private val networkMonitor: NetworkMonitor,
    private val menuRepository: MenuRepository,
    private val orderRepository: OrderRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val _networkHealth = MutableStateFlow(NetworkHealth())
    private val _apiHealth = MutableStateFlow(ApiHealth())
    private val _menuHealth = MutableStateFlow(MenuHealth())
    private val _outboxHealth = MutableStateFlow(OutboxHealth())
    private val _liveOrderHealth = MutableStateFlow(LiveOrderHealth())
    private val _databaseHealth = MutableStateFlow(DatabaseHealth())

    val healthReport: StateFlow<SystemHealthReport> = combine(
        combine(_networkHealth, _apiHealth, _menuHealth) { n, a, m -> Triple(n, a, m) },
        combine(_outboxHealth, _liveOrderHealth, _databaseHealth) { o, l, d -> Triple(o, l, d) }
    ) { (network, api, menu), (outbox, liveOrder, database) ->
        SystemHealthAggregator.aggregate(
            network = network,
            api = api,
            menu = menu,
            outbox = outbox,
            liveOrder = liveOrder,
            database = database,
            nowMs = System.currentTimeMillis()
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = SystemHealthAggregator.aggregate(
            network = NetworkHealth(),
            api = ApiHealth(),
            menu = MenuHealth(),
            outbox = OutboxHealth(),
            liveOrder = LiveOrderHealth(),
            database = DatabaseHealth()
        )
    )

    init {
        // 1. 監聽網路連線狀態
        scope.launch {
            networkMonitor.isOnline.collect { online ->
                _networkHealth.value = _networkHealth.value.copy(
                    isOnline = online,
                    lastCheckedAt = System.currentTimeMillis()
                )
            }
        }

        // 2. 監聽菜單最後成功更新時間
        scope.launch {
            menuRepository.lastSuccessfulSyncAt.collect { syncAt ->
                if (syncAt != null) {
                    _menuHealth.value = _menuHealth.value.copy(
                        lastSyncAt = syncAt,
                        status = SubsystemStatus.OK
                    )
                }
            }
        }

        // 3. 監聽菜單商品總數
        scope.launch {
            menuRepository.getMenuItems().collect { items ->
                _menuHealth.value = _menuHealth.value.copy(
                    itemCount = items.size
                )
            }
        }

        // 4. 監聽未同步訂單數量
        scope.launch {
            orderRepository.unsyncedCount.collect { count ->
                _outboxHealth.value = _outboxHealth.value.copy(
                    pendingCount = count
                )
            }
        }
    }

    fun recordApiCall(success: Boolean, latencyMs: Long? = null, errorMessage: String? = null) {
        val now = System.currentTimeMillis()
        if (success) {
            _apiHealth.value = _apiHealth.value.copy(
                status = SubsystemStatus.OK,
                lastSuccessAt = now,
                latencyMs = latencyMs,
                lastErrorMessage = null
            )
        } else {
            _apiHealth.value = _apiHealth.value.copy(
                status = SubsystemStatus.WARNING,
                lastErrorMessage = errorMessage,
                latencyMs = latencyMs
            )
        }
    }

    fun recordOutboxSync(success: Boolean, syncedCount: Int, errorMessage: String? = null) {
        if (success) {
            _outboxHealth.value = _outboxHealth.value.copy(
                consecutiveFailures = 0,
                lastFailureMessage = null,
                status = if (_outboxHealth.value.pendingCount - syncedCount > 0) SubsystemStatus.WARNING else SubsystemStatus.OK
            )
        } else {
            val nextFailures = _outboxHealth.value.consecutiveFailures + 1
            _outboxHealth.value = _outboxHealth.value.copy(
                consecutiveFailures = nextFailures,
                lastFailureMessage = errorMessage,
                status = if (nextFailures >= 3) SubsystemStatus.ERROR else SubsystemStatus.WARNING
            )
        }
    }

    fun recordLiveOrderPoll(success: Boolean, newOrdersCount: Int = 0, errorMessage: String? = null) {
        val now = System.currentTimeMillis()
        if (success) {
            _liveOrderHealth.value = _liveOrderHealth.value.copy(
                status = SubsystemStatus.OK,
                lastPollAt = now,
                lastOrderReceivedAt = if (newOrdersCount > 0) now else _liveOrderHealth.value.lastOrderReceivedAt,
                isPollingActive = true
            )
        } else {
            _liveOrderHealth.value = _liveOrderHealth.value.copy(
                status = SubsystemStatus.WARNING
            )
        }
    }

    fun recordDatabaseError(e: Throwable?) {
        _databaseHealth.value = _databaseHealth.value.copy(
            isAccessible = false,
            status = SubsystemStatus.ERROR,
            lastErrorMessage = e?.message ?: "資料庫讀寫異常"
        )
    }

    fun recordDatabaseSuccess() {
        if (!_databaseHealth.value.isAccessible || _databaseHealth.value.status != SubsystemStatus.OK) {
            _databaseHealth.value = _databaseHealth.value.copy(
                isAccessible = true,
                status = SubsystemStatus.OK,
                lastErrorMessage = null
            )
        }
    }
}
