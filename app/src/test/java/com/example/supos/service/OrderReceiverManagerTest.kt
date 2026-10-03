package com.example.supos.service

import com.example.supos.data.health.NetworkMonitor
import com.example.supos.data.local.dao.OrderDao
import com.example.supos.data.local.entity.OrderEntity
import com.example.supos.data.local.entity.OrderItemEntity
import com.example.supos.data.local.entity.OrderOptionEntity
import com.example.supos.data.qburger.QBurgerHqClient
import com.example.supos.data.qburger.QBurgerOrderState
import com.example.supos.data.qburger.QBurgerOrderSummary
import com.example.supos.data.repository.OrderRepository
import com.example.supos.test.FakeGasApiService
import com.example.supos.ui.history.NewOrderAlertEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrderReceiverManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeGasApiService: FakeGasApiService
    private lateinit var fakeOrderDao: FakeOrderDao
    private lateinit var orderRepository: OrderRepository
    private lateinit var fakeNetworkMonitor: FakeNetworkMonitor
    private lateinit var fakeHqClient: FakeQBurgerHqClient
    private lateinit var manager: OrderReceiverManager

    @Before
    fun setUp() {
        fakeGasApiService = FakeGasApiService()
        fakeOrderDao = FakeOrderDao()
        orderRepository = OrderRepository(fakeOrderDao, fakeGasApiService, testDispatcher)
        fakeNetworkMonitor = FakeNetworkMonitor()
        fakeHqClient = FakeQBurgerHqClient()
    }

    @Test
    fun testPollDetectsNewQBurgerOrderAndTriggersAlert() = runTest(testDispatcher) {
        manager = OrderReceiverManager(
            orderRepository = orderRepository,
            networkMonitor = fakeNetworkMonitor,
            qBurgerHqClient = fakeHqClient,
            ioDispatcher = testDispatcher,
            externalScope = this
        )

        val receivedAlerts = mutableListOf<NewOrderAlertEvent>()
        val collectJob = launch {
            manager.newOrderEventFlow.collect { alert ->
                receivedAlerts.add(alert)
            }
        }

        // 1. First poll: Baseline existing order
        fakeHqClient.ordersToReturn = listOf(
            QBurgerOrderSummary(
                tradeId = "ORDER-BASE-001",
                sourceCode = "Foodpanda",
                sourceName = "Foodpanda",
                buyerNick = "陳**",
                mobile = "0911000111",
                saleAmount = 150,
                preTimeInterval = "10:00",
                preRemark = "",
                eInvoiceVehicle = "",
                state = QBurgerOrderState.ORDERED,
                createTime = "2026-10-02T10:00:00"
            )
        )
        manager.pollOnce()
        advanceUntilIdle()

        // First poll must not trigger alert
        assertTrue("Initial poll must not trigger alert", receivedAlerts.isEmpty())

        // 2. Second poll: New order arrives from UberEats
        val newOrder = QBurgerOrderSummary(
            tradeId = "ORDER-UBER-999",
            sourceCode = "UberEats",
            sourceName = "UberEats",
            buyerNick = "林**",
            mobile = "0922333444",
            saleAmount = 280,
            preTimeInterval = "10:30",
            preRemark = "不要洋蔥",
            eInvoiceVehicle = "",
            state = QBurgerOrderState.NEW_ORDER,
            createTime = "2026-10-02T10:20:00"
        )
        fakeHqClient.ordersToReturn = listOf(fakeHqClient.ordersToReturn[0], newOrder)

        manager.pollOnce()
        advanceUntilIdle()

        assertEquals("Should trigger 1 alert", 1, receivedAlerts.size)
        val alert = receivedAlerts[0]
        assertEquals(1, alert.newOrdersCount)
        assertEquals(listOf("ORDER-UBER-999"), alert.newOrderIds)
        assertTrue(alert.latestOrderDetails?.contains("UberEats") == true)
        assertTrue(alert.latestOrderDetails?.contains("280") == true)

        collectJob.cancel()
    }

    private class FakeOrderDao : OrderDao {
        override suspend fun insertOrder(order: OrderEntity) {}
        override suspend fun insertOrderItems(items: List<OrderItemEntity>): List<Long> = emptyList()
        override suspend fun insertOrderOptions(options: List<OrderOptionEntity>) {}
        override suspend fun createFullOrder(order: OrderEntity, items: List<OrderItemEntity>) {}
        override suspend fun createFullOrderWithDetails(
            order: OrderEntity,
            itemsWithOptions: List<Pair<OrderItemEntity, List<OrderOptionEntity>>>
        ) {}
        override fun getAllOrders(): Flow<List<OrderEntity>> = flowOf(emptyList())
        override suspend fun getItemsForOrder(orderId: String): List<OrderItemEntity> = emptyList()
        override suspend fun getOptionsForOrderItem(orderItemId: Int): List<OrderOptionEntity> = emptyList()
        override suspend fun markOrderAsSynced(orderId: String) {}
        override fun getUnsyncedOrders(): Flow<List<OrderEntity>> = flowOf(emptyList())
        override suspend fun getUnsyncedOrdersList(): List<OrderEntity> = emptyList()
        override fun getUnsyncedCount(): Flow<Int> = flowOf(0)
    }

    private class FakeNetworkMonitor : NetworkMonitor {
        val onlineFlow = MutableStateFlow(true)
        override val isOnline: Flow<Boolean> = onlineFlow
    }

    private class FakeQBurgerHqClient : QBurgerHqClient() {
        var ordersToReturn: List<QBurgerOrderSummary> = emptyList()

        override suspend fun fetchOnlineOrders(
            storeCode: String,
            sources: List<String>
        ): Result<List<QBurgerOrderSummary>> {
            return Result.success(ordersToReturn)
        }
    }
}
