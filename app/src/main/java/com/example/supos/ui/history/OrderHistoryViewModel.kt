package com.example.supos.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.supos.data.network.HistoryOrderPayload
import com.example.supos.data.repository.OrderRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class OrderDateFilter(val label: String) {
    TODAY("今日"),
    YESTERDAY("昨日"),
    CUSTOM("自訂")
}

data class ParsedOrderItem(
    val category: String? = null,
    val name: String,
    val qty: Int,
    val unitPrice: Int = 0,
    val customPrice: Int? = null,
    val isComplimentary: Boolean = false,
    val subtotal: Int = 0,
    val addons: List<ParsedAddon>? = null
)

data class ParsedAddon(
    val name: String,
    val qty: Int? = 1,
    val price: Int = 0
)

enum class ConnectionState {
    CONNECTED,
    DISCONNECTED,
    SYNCING
}

data class NewOrderAlertEvent(
    val newOrdersCount: Int,
    val newOrderIds: List<String>,
    val latestOrderDetails: String?
)

data class MutationAttempt(
    val orderId: String,
    val targetState: String,
    val cancelReason: String?
)

sealed class OrderMutationState {
    object Idle : OrderMutationState()
    object Submitting : OrderMutationState()
    data class Failed(val message: String, val attempt: MutationAttempt? = null) : OrderMutationState()
}

class OrderHistoryViewModel(
    private val orderRepository: OrderRepository,
    private val menuRepository: com.example.supos.data.repository.MenuRepository,
    private val orderReceiverManager: com.example.supos.service.OrderReceiverManager? = null
) : ViewModel() {

    private val gson = Gson()
    private val listType = object : TypeToken<List<ParsedOrderItem>>() {}.type

    private val _selectedDateFilter = MutableStateFlow(OrderDateFilter.TODAY)
    val selectedDateFilter: StateFlow<OrderDateFilter> = _selectedDateFilter

    private val _customStartDate = MutableStateFlow(System.currentTimeMillis())
    val customStartDate: StateFlow<Long> = _customStartDate

    private val _customEndDate = MutableStateFlow(System.currentTimeMillis())
    val customEndDate: StateFlow<Long> = _customEndDate

    private val _ordersState = MutableStateFlow<OrderHistoryUiState>(OrderHistoryUiState.Loading)
    val ordersState: StateFlow<OrderHistoryUiState> = _ordersState

    private val _selectedOrder = MutableStateFlow<HistoryOrderPayload?>(null)
    val selectedOrder: StateFlow<HistoryOrderPayload?> = _selectedOrder

    private val _selectedOrderItems = MutableStateFlow<List<ParsedOrderItem>>(emptyList())
    val selectedOrderItems: StateFlow<List<ParsedOrderItem>> = _selectedOrderItems

    private val _orderMutationStates = MutableStateFlow<Map<String, OrderMutationState>>(emptyMap())
    val orderMutationStates: StateFlow<Map<String, OrderMutationState>> = _orderMutationStates

    private val _connectionState = MutableStateFlow(ConnectionState.CONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _lastSyncTimestamp = MutableStateFlow(0L)
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp

    private val _newOrderAlertFlow = kotlinx.coroutines.flow.MutableSharedFlow<NewOrderAlertEvent>(extraBufferCapacity = 10)
    val newOrderAlertFlow: kotlinx.coroutines.flow.SharedFlow<NewOrderAlertEvent> = _newOrderAlertFlow

    private val inFlightMutations = mutableSetOf<String>()
    private val knownOrderKeys = mutableSetOf<String>()
    private var isFirstLoad = true
    private var pollingJob: kotlinx.coroutines.Job? = null
    private var fetchJob: kotlinx.coroutines.Job? = null
    private var currentRequestId: Long = 0L

    // Cache of all loaded orders from cloud
    private var allWebOrders: List<HistoryOrderPayload> = emptyList()

    init {
        fetchHistoryFromCloud()
        if (orderReceiverManager != null) {
            viewModelScope.launch {
                orderReceiverManager.newOrderEventFlow.collect { alert ->
                    _newOrderAlertFlow.tryEmit(alert)
                    if (_selectedDateFilter.value == OrderDateFilter.TODAY) {
                        fetchHistoryFromCloud(isBackgroundPoll = true)
                    }
                }
            }
        }
    }

    fun startAutoPolling(intervalMs: Long = 15000L) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(intervalMs)
                fetchHistoryFromCloud(isBackgroundPoll = true)
            }
        }
    }

    fun stopAutoPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun setDateFilter(filter: OrderDateFilter) {
        _selectedDateFilter.value = filter
        fetchJob?.cancel()
        _ordersState.value = OrderHistoryUiState.Loading
        fetchHistoryFromCloud(isBackgroundPoll = false)
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _customStartDate.value = start
        _customEndDate.value = end
        _selectedDateFilter.value = OrderDateFilter.CUSTOM
        fetchJob?.cancel()
        _ordersState.value = OrderHistoryUiState.Loading
        fetchHistoryFromCloud(isBackgroundPoll = false)
    }

    fun fetchHistoryFromCloud(isBackgroundPoll: Boolean = false) {
        if (!isBackgroundPoll) {
            fetchJob?.cancel()
        }
        val thisRequestId = ++currentRequestId

        val job = viewModelScope.launch {
            if (!isBackgroundPoll) {
                _ordersState.value = OrderHistoryUiState.Loading
            }
            _connectionState.value = ConnectionState.SYNCING
            
            // Compute days diff to fetch
            val days: Int = when (_selectedDateFilter.value) {
                OrderDateFilter.TODAY -> 0
                OrderDateFilter.YESTERDAY -> 1
                OrderDateFilter.CUSTOM -> {
                    val cal = Calendar.getInstance()
                    val todayStart = getStartOfDay(cal.time)
                    val customStart = getStartOfDay(Date(_customStartDate.value))
                    val diff = todayStart.time - customStart.time
                    val dayDiff = (diff / (1000 * 60 * 60 * 24)).toInt()
                    maxOf(0, dayDiff)
                }
            }

            val result = orderRepository.fetchHistoryOrders(days)

            // Stale response guard: discard if a newer request was dispatched
            if (thisRequestId != currentRequestId || !isActive) {
                return@launch
            }

            if (result.isSuccess) {
                _connectionState.value = ConnectionState.CONNECTED
                _lastSyncTimestamp.value = System.currentTimeMillis()

                val fetched = result.getOrNull() ?: emptyList()
                
                // Identify new incoming orders for alert
                val newlyArrived = fetched.filter { order ->
                    val key = "${order.platformCode ?: "POS"}:${order.orderId}"
                    !knownOrderKeys.contains(key)
                }

                if (!isFirstLoad && newlyArrived.isNotEmpty()) {
                    _newOrderAlertFlow.tryEmit(
                        NewOrderAlertEvent(
                            newOrdersCount = newlyArrived.size,
                            newOrderIds = newlyArrived.map { it.orderId },
                            latestOrderDetails = newlyArrived.firstOrNull()?.details
                        )
                    )
                }

                // Update known keys
                fetched.forEach { order ->
                    val key = "${order.platformCode ?: "POS"}:${order.orderId}"
                    knownOrderKeys.add(key)
                }
                isFirstLoad = false

                allWebOrders = fetched.map { remoteOrder ->
                    if (inFlightMutations.contains(remoteOrder.orderId)) {
                        allWebOrders.firstOrNull { it.orderId == remoteOrder.orderId } ?: remoteOrder
                    } else {
                        remoteOrder
                    }
                }
                applyFiltering()
            } else {
                _connectionState.value = ConnectionState.DISCONNECTED
                if (allWebOrders.isEmpty()) {
                    _ordersState.value = OrderHistoryUiState.Error(result.exceptionOrNull() ?: Exception("Unknown error"))
                }
            }
        }

        if (!isBackgroundPoll) {
            fetchJob = job
        }
    }

    private fun applyFiltering() {
        val interval = when (_selectedDateFilter.value) {
            OrderDateFilter.TODAY -> com.example.supos.data.util.TaipeiDateTimeHelper.getTodayInterval()
            OrderDateFilter.YESTERDAY -> com.example.supos.data.util.TaipeiDateTimeHelper.getYesterdayInterval()
            OrderDateFilter.CUSTOM -> com.example.supos.data.util.TaipeiDateTimeHelper.getCustomInterval(_customStartDate.value, _customEndDate.value)
        }

        val filtered = com.example.supos.data.util.TurnoverCalculator.filterOrdersInInterval(allWebOrders, interval)
        _ordersState.value = OrderHistoryUiState.Success(filtered)
        
        // Auto-select first order if list is not empty, or clear selection if empty
        if (filtered.isNotEmpty()) {
            val currentSelected = _selectedOrder.value
            val match = filtered.firstOrNull { it.orderId == currentSelected?.orderId } ?: filtered.first()
            selectOrder(match)
        } else {
            _selectedOrder.value = null
            _selectedOrderItems.value = emptyList()
        }
    }

    fun prepareWebOrderForCheckout(order: HistoryOrderPayload, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val menuItems = menuRepository.getMenuItems().first()
            val optionItems = menuRepository.getOptions().first()
            orderRepository.createCheckoutDraftFromOrder(order, menuItems, optionItems)
            onComplete()
        }
    }

    fun updateWebOrderState(orderId: String, newState: String, cancelReason: String? = null) {
        val currentOrder = allWebOrders.firstOrNull { it.orderId == orderId } ?: return
        
        // Prevent concurrent in-flight mutation for the same order
        if (inFlightMutations.contains(orderId)) return

        // State machine guards
        if (!com.example.supos.data.model.OrderStateMachine.canTransition(currentOrder.state, newState)) {
            _orderMutationStates.value = _orderMutationStates.value + (orderId to OrderMutationState.Failed(
                "狀態機規則禁止將訂單狀態由「${currentOrder.state}」變更為「$newState」"
            ))
            return
        }

        if (newState == com.example.supos.data.model.OrderStateMachine.STATE_CANCELED && cancelReason.isNullOrBlank()) {
            _orderMutationStates.value = _orderMutationStates.value + (orderId to OrderMutationState.Failed(
                "取消訂單必須提供取消原因"
            ))
            return
        }

        val previousState = currentOrder.state

        inFlightMutations.add(orderId)

        viewModelScope.launch {
            _orderMutationStates.value = _orderMutationStates.value + (orderId to OrderMutationState.Submitting)
            
            // Optimistic update
            allWebOrders = allWebOrders.map {
                if (it.orderId == orderId) it.copy(state = newState) else it
            }
            applyFiltering()
            if (_selectedOrder.value?.orderId == orderId) {
                _selectedOrder.value = _selectedOrder.value?.copy(state = newState)
            }

            val result = orderRepository.updateOrderState(orderId, newState, cancelReason)
            inFlightMutations.remove(orderId)

            if (result.isSuccess) {
                _orderMutationStates.value = _orderMutationStates.value + (orderId to OrderMutationState.Idle)
            } else {
                // Rollback on failure
                allWebOrders = allWebOrders.map {
                    if (it.orderId == orderId) it.copy(state = previousState) else it
                }
                applyFiltering()
                if (_selectedOrder.value?.orderId == orderId) {
                    _selectedOrder.value = _selectedOrder.value?.copy(state = previousState)
                }

                val errorMessage = result.exceptionOrNull()?.message ?: "狀態變更失敗，請重試"
                val attempt = MutationAttempt(orderId, newState, cancelReason)
                _orderMutationStates.value = _orderMutationStates.value + (orderId to OrderMutationState.Failed(errorMessage, attempt))
            }
        }
    }

    fun retryFailedMutation(orderId: String) {
        val state = _orderMutationStates.value[orderId]
        if (state is OrderMutationState.Failed && state.attempt != null) {
            val attempt = state.attempt
            updateWebOrderState(attempt.orderId, attempt.targetState, attempt.cancelReason)
        }
    }

    fun selectOrder(order: HistoryOrderPayload?) {
        _selectedOrder.value = order
        if (order == null) {
            _selectedOrderItems.value = emptyList()
            return
        }
        val parsed: List<ParsedOrderItem> = try {
            gson.fromJson(order.details, listType)
        } catch (e: Exception) {
            emptyList()
        }
        _selectedOrderItems.value = parsed
    }

    fun clearSelectedOrder() {
        selectOrder(null)
    }

    private fun getStartOfDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    private fun parseOrderDate(dateString: String): Date? {
        val prefix = dateString.take(24)
        val formatter = SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss", Locale.US)
        return try {
            formatter.parse(prefix)
        } catch (e: Exception) {
            try {
                val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                isoFormatter.parse(dateString)
            } catch (ex: Exception) {
                null
            }
        }
    }
}

sealed interface OrderHistoryUiState {
    object Loading : OrderHistoryUiState
    data class Error(val throwable: Throwable) : OrderHistoryUiState
    data class Success(val orders: List<HistoryOrderPayload>) : OrderHistoryUiState
}
