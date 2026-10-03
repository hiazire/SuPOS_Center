package com.example.supos.service

import com.example.supos.data.health.NetworkMonitor
import com.example.supos.data.network.HistoryOrderPayload
import com.example.supos.data.qburger.QBurgerHqClient
import com.example.supos.data.qburger.QBurgerOrderState
import com.example.supos.data.qburger.QBurgerOrderSummary
import com.example.supos.data.repository.OrderRepository
import com.example.supos.ui.history.NewOrderAlertEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OrderReceiverManager(
    private val orderRepository: OrderRepository,
    private val networkMonitor: NetworkMonitor,
    private val qBurgerHqClient: QBurgerHqClient? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)
) {

    private val _newOrderEventFlow = MutableSharedFlow<NewOrderAlertEvent>(extraBufferCapacity = 16)
    val newOrderEventFlow: SharedFlow<NewOrderAlertEvent> = _newOrderEventFlow.asSharedFlow()

    private val _isReceiving = MutableStateFlow(false)
    val isReceiving: StateFlow<Boolean> = _isReceiving.asStateFlow()

    private val _latestOrders = MutableStateFlow<List<HistoryOrderPayload>>(emptyList())
    val latestOrders: StateFlow<List<HistoryOrderPayload>> = _latestOrders.asStateFlow()

    private val knownOrderIds = mutableSetOf<String>()
    private var isFirstLoad = true
    private var pollingJob: Job? = null
    private var networkObserveJob: Job? = null

    fun start(intervalMs: Long = 12000L) {
        if (_isReceiving.value) return
        _isReceiving.value = true

        networkObserveJob?.cancel()
        networkObserveJob = externalScope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                if (isOnline) {
                    startPollingLoop(intervalMs)
                } else {
                    stopPollingLoop()
                }
            }
        }
    }

    fun stop() {
        _isReceiving.value = false
        networkObserveJob?.cancel()
        networkObserveJob = null
        stopPollingLoop()
    }

    private fun startPollingLoop(intervalMs: Long) {
        pollingJob?.cancel()
        pollingJob = externalScope.launch {
            while (isActive) {
                pollOnce()
                delay(intervalMs)
            }
        }
    }

    private fun stopPollingLoop() {
        pollingJob?.cancel()
        pollingJob = null
    }

    suspend fun pollOnce() {
        val result = orderRepository.fetchHistoryOrders(days = 0)
        result.onSuccess { orders ->
            _latestOrders.value = orders

            val currentIds = orders.map { it.orderId }.toSet()
            if (!isFirstLoad) {
                val newlyArrivedOrders = orders.filter { order ->
                    !knownOrderIds.contains(order.orderId) &&
                            (order.sourceType == "ONLINE" || !order.platformCode.isNullOrBlank()) &&
                            (order.state.equals("PENDING", ignoreCase = true) || order.state.equals("PAID", ignoreCase = true))
                }

                if (newlyArrivedOrders.isNotEmpty()) {
                    val newIds = newlyArrivedOrders.map { it.orderId }
                    knownOrderIds.addAll(newIds)
                    val alert = NewOrderAlertEvent(
                        newOrdersCount = newlyArrivedOrders.size,
                        newOrderIds = newIds,
                        latestOrderDetails = newlyArrivedOrders.firstOrNull()?.details
                    )
                    _newOrderEventFlow.tryEmit(alert)
                } else {
                    knownOrderIds.addAll(currentIds)
                }
            } else {
                knownOrderIds.addAll(currentIds)
            }
        }

        // QBurger WCF HQ live order polling
        qBurgerHqClient?.let { client ->
            try {
                val qOrdersResult = client.fetchOnlineOrders()
                qOrdersResult.onSuccess { qOrders ->
                    val qIds = qOrders.map { it.tradeId }.toSet()
                    if (!isFirstLoad) {
                        val newQOrders = qOrders.filter { q ->
                            !knownOrderIds.contains(q.tradeId) &&
                                    (q.state == QBurgerOrderState.NEW_ORDER ||
                                     q.state == QBurgerOrderState.ORDERED)
                        }

                        if (newQOrders.isNotEmpty()) {
                            val newIds = newQOrders.map { it.tradeId }
                            knownOrderIds.addAll(newIds)
                            val alert = NewOrderAlertEvent(
                                newOrdersCount = newQOrders.size,
                                newOrderIds = newIds,
                                latestOrderDetails = "【${newQOrders.first().sourceName}】新外送/預點訂單（$${newQOrders.first().saleAmount}）"
                            )
                            _newOrderEventFlow.tryEmit(alert)
                        } else {
                            knownOrderIds.addAll(qIds)
                        }
                    } else {
                        knownOrderIds.addAll(qIds)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (isFirstLoad) {
            isFirstLoad = false
        }
    }

    fun resetKnownOrders() {
        knownOrderIds.clear()
        isFirstLoad = true
    }
}
