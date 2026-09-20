package com.example.supos.test

import com.example.supos.data.network.GasApiService
import com.example.supos.data.network.HistoryOrderPayload
import com.example.supos.data.network.HistoryRequest
import com.example.supos.data.network.HistoryResponsePayload
import com.example.supos.data.network.MenuResponsePayload
import com.example.supos.data.network.OrderPayload
import com.example.supos.data.network.SyncResponse

class FakeGasApiService : GasApiService {

    var shouldReturnError: Boolean = false
    var errorMessage: String = "Simulated API error"
    var exceptionToThrow: Exception? = null
    var customMenuResponse: MenuResponsePayload? = null

    val syncedOrders = mutableListOf<OrderPayload>()
    val lastSyncedOrderPayload: OrderPayload? get() = syncedOrders.lastOrNull()
    val updatedOrderStates = mutableListOf<Map<String, String>>()
    val historyOrdersStore = mutableListOf<HistoryOrderPayload>()

    override suspend fun fetchMenu(): MenuResponsePayload {
        exceptionToThrow?.let { throw it }
        customMenuResponse?.let { return it }
        if (shouldReturnError) {
            return MenuResponsePayload(success = false, menu = emptyList(), options = emptyList())
        }
        return MenuResponsePayload(success = true, menu = emptyList(), options = emptyList())
    }

    override suspend fun syncOrder(order: OrderPayload): SyncResponse {
        exceptionToThrow?.let { throw it }
        if (shouldReturnError) {
            return SyncResponse(status = "error", message = errorMessage)
        }
        syncedOrders.add(order)
        return SyncResponse(status = "success", message = "OK")
    }

    var onGetHistoryOrders: (suspend (HistoryRequest) -> HistoryResponsePayload)? = null

    override suspend fun getHistoryOrders(request: HistoryRequest): HistoryResponsePayload {
        exceptionToThrow?.let { throw it }
        onGetHistoryOrders?.let { return it.invoke(request) }
        if (shouldReturnError) {
            return HistoryResponsePayload(success = false, orders = null)
        }
        return HistoryResponsePayload(success = true, orders = historyOrdersStore.toList())
    }

    override suspend fun updateOrderState(request: Map<String, String>): SyncResponse {
        exceptionToThrow?.let { throw it }
        if (shouldReturnError) {
            return SyncResponse(status = "error", message = errorMessage)
        }
        updatedOrderStates.add(request)
        
        val orderId = request["orderId"]
        val newState = request["newState"]
        if (orderId != null && newState != null) {
            val idx = historyOrdersStore.indexOfFirst { it.orderId == orderId }
            if (idx >= 0) {
                historyOrdersStore[idx] = historyOrdersStore[idx].copy(state = newState)
            }
        }
        return SyncResponse(status = "success", message = "OK")
    }
}
