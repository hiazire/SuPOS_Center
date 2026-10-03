package com.example.supos.ui.online

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.supos.data.qburger.QBurgerHqClient
import com.example.supos.data.qburger.QBurgerOrderDetail
import com.example.supos.data.qburger.QBurgerOrderState
import com.example.supos.data.qburger.QBurgerOrderSummary
import com.example.supos.service.KitchenPrinterRouter
import com.example.supos.service.PrintTargetResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnlineOrderUiState(
    val isLoading: Boolean = false,
    val isStoreOpen: Boolean = true,
    val selectedSourceFilter: String = "ALL", // "ALL", "UberEats", "Foodpanda", "QBAPPv2", "SelfOrder"
    val orders: List<QBurgerOrderSummary> = emptyList(),
    val selectedOrder: QBurgerOrderSummary? = null,
    val selectedOrderDetail: QBurgerOrderDetail? = null,
    val isDetailLoading: Boolean = false,
    val statusMessage: String? = null
)

class OnlineOrderViewModel(
    private val hqClient: QBurgerHqClient = QBurgerHqClient(),
    private val printerRouter: KitchenPrinterRouter = KitchenPrinterRouter()
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnlineOrderUiState())
    val uiState: StateFlow<OnlineOrderUiState> = _uiState.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    init {
        refreshOrders()
    }

    fun setSourceFilter(filter: String) {
        _uiState.value = _uiState.value.copy(selectedSourceFilter = filter)
    }

    fun selectOrder(order: QBurgerOrderSummary) {
        _uiState.value = _uiState.value.copy(
            selectedOrder = order,
            selectedOrderDetail = null,
            isDetailLoading = true
        )

        viewModelScope.launch {
            val result = hqClient.fetchOrderDetail(
                sourceCode = order.sourceCode,
                tradeId = order.tradeId
            )
            result.onSuccess { detail ->
                _uiState.value = _uiState.value.copy(
                    selectedOrderDetail = detail,
                    isDetailLoading = false
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(isDetailLoading = false)
                _userMessage.emit("無法獲取訂單明細: ${err.message}")
            }
        }
    }

    fun refreshOrders() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = hqClient.fetchOnlineOrders()
            result.onSuccess { list ->
                val sorted = list.sortedByDescending { it.createTime }
                val currentSelected = _uiState.value.selectedOrder
                val reselected = sorted.firstOrNull { it.tradeId == currentSelected?.tradeId }
                    ?: sorted.firstOrNull()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    orders = sorted,
                    selectedOrder = reselected
                )

                if (reselected != null && reselected.tradeId != currentSelected?.tradeId) {
                    selectOrder(reselected)
                }
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(isLoading = false)
                _userMessage.emit("刷新訂單失敗: ${err.message}")
            }
        }
    }

    fun markOrderEntered() {
        val current = _uiState.value.selectedOrder ?: return
        val updatedOrders = _uiState.value.orders.map {
            if (it.tradeId == current.tradeId) it.copy(state = QBurgerOrderState.ORDERED) else it
        }
        val updatedSelected = current.copy(state = QBurgerOrderState.ORDERED)
        _uiState.value = _uiState.value.copy(
            orders = updatedOrders,
            selectedOrder = updatedSelected
        )
        viewModelScope.launch {
            _userMessage.emit("訂單 [${current.tradeId.takeLast(6)}] 已成功入機！")
        }
    }

    fun printCurrentOrderToKitchen() {
        val detail = _uiState.value.selectedOrderDetail
        if (detail == null) {
            viewModelScope.launch { _userMessage.emit("尚未選中有效訂單或明細載入中") }
            return
        }

        viewModelScope.launch {
            _userMessage.emit("正在傳送工單至廚房印表機...")
            val results = printerRouter.printOrderToKitchen(detail)
            val successCount = results.count { it.success }
            val failList = results.filter { !it.success }

            if (failList.isEmpty()) {
                // Update state to PRINTED
                val current = _uiState.value.selectedOrder
                if (current != null) {
                    val updated = _uiState.value.orders.map {
                        if (it.tradeId == current.tradeId) it.copy(state = QBurgerOrderState.PRINTED) else it
                    }
                    _uiState.value = _uiState.value.copy(
                        orders = updated,
                        selectedOrder = current.copy(state = QBurgerOrderState.PRINTED)
                    )
                }
                _userMessage.emit("廚房出單完成！(共 $successCount 台已出單)")
            } else {
                val errNames = failList.joinToString { "${it.targetName}(${it.ip})" }
                _userMessage.emit("部分印表機連線超時: $errNames")
            }
        }
    }

    fun reprintTicket() {
        val detail = _uiState.value.selectedOrderDetail ?: return
        viewModelScope.launch {
            _userMessage.emit("正在執行手動補印...")
            val res = printerRouter.reprintAll(detail, "192.168.100.10")
            if (res.success) {
                _userMessage.emit("補印單據已送出")
            } else {
                _userMessage.emit("補印失敗: ${res.errorMessage}")
            }
        }
    }

    fun cancelOrder(reason: String) {
        val current = _uiState.value.selectedOrder ?: return
        val updatedOrders = _uiState.value.orders.map {
            if (it.tradeId == current.tradeId) it.copy(state = QBurgerOrderState.CANCELLED) else it
        }
        _uiState.value = _uiState.value.copy(
            orders = updatedOrders,
            selectedOrder = current.copy(state = QBurgerOrderState.CANCELLED)
        )
        viewModelScope.launch {
            _userMessage.emit("訂單已退單，原因：$reason")
        }
    }

    fun toggleStoreStatus(pin: String): Boolean {
        if (pin != "8991") {
            viewModelScope.launch { _userMessage.emit("驗證碼錯誤，無法變更狀態") }
            return false
        }
        val newStatus = !_uiState.value.isStoreOpen
        _uiState.value = _uiState.value.copy(isStoreOpen = newStatus)
        viewModelScope.launch {
            _userMessage.emit(if (newStatus) "門市線上接單已開啟" else "門市線上接單已關閉（打烊）")
        }
        return true
    }
}
