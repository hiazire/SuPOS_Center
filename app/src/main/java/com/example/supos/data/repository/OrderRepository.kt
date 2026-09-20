package com.example.supos.data.repository

import android.util.Log
import com.example.supos.data.local.dao.OrderDao
import com.example.supos.data.local.entity.OrderEntity
import com.example.supos.data.local.entity.OrderItemEntity
import com.example.supos.data.model.CartItem
import com.example.supos.data.network.GasApiService
import com.example.supos.data.network.OrderItemPayload
import com.example.supos.data.network.OrderPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OrderRepository(
    private val orderDao: OrderDao,
    private val gasApiService: GasApiService,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
    private val orderDraftDao: com.example.supos.data.local.dao.OrderDraftDao? = null
) {
    private fun safeLogD(tag: String, msg: String) {
        try {
            android.util.Log.d(tag, msg)
        } catch (_: Throwable) {
            println("[$tag] $msg")
        }
    }

    private fun safeLogE(tag: String, msg: String, tr: Throwable? = null) {
        try {
            android.util.Log.e(tag, msg, tr)
        } catch (_: Throwable) {
            println("[$tag] $msg ${tr?.message ?: ""}")
        }
    }
    private val _checkoutDraft = MutableStateFlow<com.example.supos.data.model.CheckoutDraft?>(null)
    val checkoutDraft: StateFlow<com.example.supos.data.model.CheckoutDraft?> = _checkoutDraft.asStateFlow()

    fun setCheckoutDraft(draft: com.example.supos.data.model.CheckoutDraft) {
        _checkoutDraft.value = draft
    }

    fun consumeCheckoutDraft(): com.example.supos.data.model.CheckoutDraft? {
        val draft = _checkoutDraft.value
        _checkoutDraft.value = null
        return draft
    }

    suspend fun createCheckoutDraftFromOrder(
        order: com.example.supos.data.network.HistoryOrderPayload,
        menuItems: List<com.example.supos.data.local.entity.MenuItemEntity>,
        optionItems: List<com.example.supos.data.local.entity.OptionItemEntity>
    ): com.example.supos.data.model.CheckoutDraft = withContext(ioDispatcher) {
        val parsedData = order.details
        val listType = object : com.google.gson.reflect.TypeToken<List<com.example.supos.ui.history.ParsedOrderItem>>() {}.type
        val items: List<com.example.supos.ui.history.ParsedOrderItem> = try {
            gson.fromJson(parsedData, listType)
        } catch (e: Exception) {
            emptyList()
        }

        val restoredCartItems = mutableListOf<CartItem>()
        val unmappedItems = mutableListOf<String>()

        for (pItem in items) {
            val menuItem = menuItems.firstOrNull { it.name == pItem.name }
            if (menuItem != null) {
                val options = mutableListOf<com.example.supos.data.local.entity.OptionItemEntity>()
                pItem.addons?.forEach { addon ->
                    val matchedOption = optionItems.firstOrNull { it.name == addon.name }
                    if (matchedOption != null) {
                        val qty = addon.qty ?: 1
                        repeat(qty) {
                            options.add(matchedOption)
                        }
                    }
                }
                restoredCartItems.add(
                    CartItem(
                        menuItem = menuItem,
                        quantity = pItem.qty,
                        options = options,
                        customPrice = pItem.customPrice,
                        isComplimentary = pItem.isComplimentary
                    )
                )
            } else {
                unmappedItems.add(pItem.name)
            }
        }

        val transactionType = when {
            order.details.contains("內用") -> "內用"
            order.details.contains("Line自取") -> "Line自取"
            order.details.contains("電話點餐") -> "電話點餐"
            else -> "外帶"
        }

        val platformCode = order.platformCode ?: "WEB"
        val sourceType = order.sourceType ?: "ONLINE_IMPORT"

        val draft = com.example.supos.data.model.CheckoutDraft(
            sourceOrderId = order.orderId,
            timestamp = order.timestamp.take(24),
            transactionType = transactionType,
            cartItems = restoredCartItems,
            unmappedItems = unmappedItems,
            sourceType = sourceType,
            platformCode = platformCode
        )
        _checkoutDraft.value = draft
        draft
    }

    private val gson = com.google.gson.Gson()

data class CheckoutResult(
    val orderId: String,
    val isSynced: Boolean
)

    suspend fun checkoutOrder(
        cartItems: List<CartItem>,
        totalAmount: Int,
        orderType: String,
        paymentMethod: String,
        idempotencyKey: String? = null,
        sourceType: String = "OFFLINE_POS",
        platformCode: String = "POS",
        sourceOrderId: String? = null,
        invoiceNumber: String = "",
        ubn: String = "",
        carrier: String = ""
    ): Result<CheckoutResult> = withContext(ioDispatcher) {
        try {
            val finalIdempotencyKey = idempotencyKey ?: if (sourceOrderId != null) "$platformCode:$sourceOrderId" else java.util.UUID.randomUUID().toString()
            val orderId = finalIdempotencyKey
            val timestamp = System.currentTimeMillis()

            // 1. Save to local DB with snapshots
            val orderEntity = OrderEntity(
                orderId = orderId,
                timestamp = timestamp,
                totalAmount = totalAmount,
                orderType = orderType,
                paymentMethod = paymentMethod,
                isSynced = false,
                sourceType = sourceType,
                platformCode = platformCode,
                sourceOrderId = sourceOrderId,
                idempotencyKey = finalIdempotencyKey,
                acceptanceStatus = "ACCEPTED",
                paymentStatus = "UNPAID",
                fulfillmentStatus = "PENDING",
                printStatus = "NOT_PRINTED",
                invoiceNumber = invoiceNumber,
                ubn = ubn,
                carrier = carrier
            )

            val itemsWithOptions = cartItems.map { cartItem ->
                val itemEntity = OrderItemEntity(
                    orderId = orderId,
                    menuItemId = cartItem.menuItem.id,
                    menuItemName = cartItem.menuItem.name,
                    quantity = cartItem.quantity,
                    subtotal = cartItem.subtotal,
                    optionsString = cartItem.options.joinToString { it.name },
                    unitPrice = cartItem.menuItem.price,
                    customPrice = cartItem.customPrice,
                    isComplimentary = cartItem.isComplimentary
                )
                val optionEntities = cartItem.options.map { option ->
                    com.example.supos.data.local.entity.OrderOptionEntity(
                        optionId = 0,
                        optionName = option.name,
                        price = option.price,
                        quantity = 1
                    )
                }
                Pair(itemEntity, optionEntities)
            }

            orderDao.createFullOrderWithDetails(orderEntity, itemsWithOptions)

            // 2. Attempt to sync to GAS (Fail silently for local-first approach)
            var isSynced = false
            try {
                val payload = OrderPayload(
                    orderId = orderId,
                    timestamp = timestamp,
                    totalAmount = totalAmount,
                    orderType = orderType,
                    paymentMethod = paymentMethod,
                    items = itemsWithOptions.map { (itemEntity, optionEntities) ->
                        val optionsJsonList = optionEntities.map { opt ->
                            mapOf("name" to opt.optionName, "price" to opt.price, "qty" to opt.quantity)
                        }
                        OrderItemPayload(
                            menuItemName = itemEntity.menuItemName,
                            quantity = itemEntity.quantity,
                            subtotal = itemEntity.subtotal,
                            options = itemEntity.optionsString,
                            unitPrice = itemEntity.unitPrice,
                            customPrice = itemEntity.customPrice,
                            isComplimentary = itemEntity.isComplimentary,
                            optionsJson = gson.toJson(optionsJsonList)
                        )
                    },
                    sourceType = sourceType,
                    platformCode = platformCode,
                    sourceOrderId = sourceOrderId,
                    idempotencyKey = finalIdempotencyKey,
                    acceptanceStatus = "ACCEPTED",
                    paymentStatus = "UNPAID",
                    fulfillmentStatus = "PENDING",
                    printStatus = "NOT_PRINTED",
                    invoiceNumber = invoiceNumber,
                    ubn = ubn,
                    carrier = carrier
                )
                
                val response = gasApiService.syncOrder(payload)
                if (response.status == "success") {
                    orderDao.markOrderAsSynced(orderId)
                    isSynced = true
                    safeLogD("OrderRepository", "Order $orderId synced successfully")
                }
            } catch (e: Exception) {
                // Network error, keep isSynced = false for future retry
                safeLogE("OrderRepository", "Failed to sync order $orderId to GAS", e)
            }

            Result.success(CheckoutResult(orderId = orderId, isSynced = isSynced))
        } catch (e: Exception) {
            safeLogE("OrderRepository", "Failed to save order to local DB", e)
            Result.failure(e)
        }
    }

    fun getOrders() = orderDao.getAllOrders()
    fun getUnsyncedOrders() = orderDao.getUnsyncedOrders()
    val unsyncedCount: Flow<Int> = orderDao.getUnsyncedCount()

    suspend fun getOrderItems(orderId: String): List<OrderItemEntity> {
        return orderDao.getItemsForOrder(orderId)
    }

    suspend fun getOptionsForOrderItem(orderItemId: Int): List<com.example.supos.data.local.entity.OrderOptionEntity> {
        return orderDao.getOptionsForOrderItem(orderItemId)
    }

    suspend fun retrySync(order: OrderEntity): Result<Unit> = withContext(ioDispatcher) {
        try {
            val items = orderDao.getItemsForOrder(order.orderId)
            val itemsPayload = items.map { item ->
                val options = orderDao.getOptionsForOrderItem(item.id)
                val optionsJsonList = options.map { opt ->
                    mapOf("name" to opt.optionName, "price" to opt.price, "qty" to opt.quantity)
                }
                OrderItemPayload(
                    menuItemName = item.menuItemName,
                    quantity = item.quantity,
                    subtotal = item.subtotal,
                    options = item.optionsString,
                    unitPrice = item.unitPrice,
                    customPrice = item.customPrice,
                    isComplimentary = item.isComplimentary,
                    optionsJson = gson.toJson(optionsJsonList)
                )
            }

            val payload = OrderPayload(
                orderId = order.orderId,
                timestamp = order.timestamp,
                totalAmount = order.totalAmount,
                orderType = order.orderType,
                paymentMethod = order.paymentMethod,
                items = itemsPayload,
                sourceType = order.sourceType,
                platformCode = order.platformCode,
                sourceOrderId = order.sourceOrderId,
                idempotencyKey = order.idempotencyKey.ifEmpty { order.orderId },
                acceptanceStatus = order.acceptanceStatus,
                paymentStatus = order.paymentStatus,
                fulfillmentStatus = order.fulfillmentStatus,
                printStatus = order.printStatus,
                invoiceNumber = order.invoiceNumber,
                ubn = order.ubn,
                carrier = order.carrier
            )

            val response = gasApiService.syncOrder(payload)
            if (response.status == "success") {
                orderDao.markOrderAsSynced(order.orderId)
                Result.success(Unit)
            } else {
                Result.failure(Exception("GAS returned error: ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncPendingOutboxOrders(): Result<Int> = withContext(ioDispatcher) {
        try {
            val pendingOrders = orderDao.getUnsyncedOrdersList()
            var syncedCount = 0
            var lastError: Exception? = null

            for (order in pendingOrders) {
                val syncResult = retrySync(order)
                if (syncResult.isSuccess) {
                    syncedCount++
                } else {
                    lastError = syncResult.exceptionOrNull() as? Exception ?: Exception("Sync failed for order ${order.orderId}")
                }
            }

            if (lastError != null && syncedCount == 0 && pendingOrders.isNotEmpty()) {
                Result.failure(lastError)
            } else {
                Result.success(syncedCount)
            }
        } catch (e: Exception) {
            safeLogE("OrderRepository", "Failed during outbox sync execution", e)
            Result.failure(e)
        }
    }

    suspend fun fetchHistoryOrders(days: Int): Result<List<com.example.supos.data.network.HistoryOrderPayload>> = withContext(ioDispatcher) {
        try {
            val response = gasApiService.getHistoryOrders(com.example.supos.data.network.HistoryRequest(days = days))
            if (response.success && response.orders != null) {
                Result.success(response.orders)
            } else {
                Result.failure(Exception("GAS returned success = false"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchTodayTurnover(): Result<Double> = withContext(ioDispatcher) {
        try {
            val response = gasApiService.getHistoryOrders(com.example.supos.data.network.HistoryRequest(days = 1))
            if (response.success && response.orders != null) {
                val interval = com.example.supos.data.util.TaipeiDateTimeHelper.getTodayInterval()
                val total = com.example.supos.data.util.TurnoverCalculator.calculateTurnover(response.orders, interval)
                Result.success(total.toDouble())
            } else {
                Result.failure(Exception("GAS returned success = false"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOrderState(orderId: String, newState: String, cancelReason: String? = null): Result<Unit> = withContext(ioDispatcher) {
        try {
            val isoFormatter = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val updatedAt = isoFormatter.format(java.util.Date())
            
            val payload = mutableMapOf(
                "action" to "updateOrderState",
                "orderId" to orderId,
                "newState" to newState,
                "updatedAt" to updatedAt
            )
            if (!cancelReason.isNullOrBlank()) {
                payload["cancelReason"] = cancelReason
            }

            val response = gasApiService.updateOrderState(payload)
            if (response.status == "success") {
                Result.success(Unit)
            } else {
                Result.failure(Exception("GAS returned error: ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveActiveCartDraft(
        tableNumber: String,
        transactionType: String,
        createdAtText: String,
        cartItems: List<CartItem>
    ) = withContext(ioDispatcher) {
        val dao = orderDraftDao ?: return@withContext
        if (cartItems.isEmpty()) {
            dao.clearDraftsByType("ACTIVE_CART")
        } else {
            val json = gson.toJson(cartItems)
            val draft = com.example.supos.data.local.entity.OrderDraftEntity(
                draftId = "ACTIVE_CART_DEFAULT",
                draftType = "ACTIVE_CART",
                timestamp = System.currentTimeMillis(),
                tableNumber = tableNumber,
                transactionType = transactionType,
                createdAtText = createdAtText,
                itemsJson = json
            )
            dao.insertDraft(draft)
        }
    }

    suspend fun getActiveCartDraft(): com.example.supos.data.local.entity.OrderDraftEntity? = withContext(ioDispatcher) {
        val dao = orderDraftDao ?: return@withContext null
        dao.getDraftById("ACTIVE_CART_DEFAULT")
    }

    suspend fun clearActiveCartDraft() = withContext(ioDispatcher) {
        val dao = orderDraftDao ?: return@withContext
        dao.clearDraftsByType("ACTIVE_CART")
    }

    suspend fun saveTempOrderDraft(tempOrder: com.example.supos.data.model.TempOrder) = withContext(ioDispatcher) {
        val dao = orderDraftDao ?: return@withContext
        val json = gson.toJson(tempOrder.items)
        val draft = com.example.supos.data.local.entity.OrderDraftEntity(
            draftId = tempOrder.id,
            draftType = "TEMP_SAVED",
            timestamp = tempOrder.timestamp,
            tableNumber = tempOrder.tableNumber,
            transactionType = tempOrder.transactionType,
            createdAtText = tempOrder.createdAt,
            itemsJson = json
        )
        dao.insertDraft(draft)
    }

    suspend fun getTempOrderDrafts(): List<com.example.supos.data.model.TempOrder> = withContext(ioDispatcher) {
        val dao = orderDraftDao ?: return@withContext emptyList()
        val drafts = dao.getDraftsListByType("TEMP_SAVED")
        drafts.mapNotNull { draft ->
            try {
                val listType = object : com.google.gson.reflect.TypeToken<List<CartItem>>() {}.type
                val items: List<CartItem> = gson.fromJson(draft.itemsJson, listType)
                com.example.supos.data.model.TempOrder(
                    id = draft.draftId,
                    timestamp = draft.timestamp,
                    items = items,
                    transactionType = draft.transactionType,
                    tableNumber = draft.tableNumber,
                    createdAt = draft.createdAtText,
                    invoiceNumber = "",
                    ubn = "",
                    carrier = ""
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun deleteTempOrderDraft(draftId: String) = withContext(ioDispatcher) {
        val dao = orderDraftDao ?: return@withContext
        dao.deleteDraft(draftId)
    }
}
