package com.example.supos.ui.history

import com.example.supos.data.local.dao.MenuDao
import com.example.supos.data.local.dao.OrderDao
import com.example.supos.data.local.entity.MenuItemEntity
import com.example.supos.data.local.entity.OptionItemEntity
import com.example.supos.data.local.entity.OrderEntity
import com.example.supos.data.local.entity.OrderItemEntity
import com.example.supos.data.local.entity.OrderOptionEntity
import com.example.supos.data.model.AcceptanceStatus
import com.example.supos.data.model.PrintStatus
import com.example.supos.data.network.HistoryOrderPayload
import com.example.supos.data.repository.MenuRepository
import com.example.supos.data.repository.OrderRepository
import com.example.supos.test.FakeGasApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrderStatusIsolationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeGasApiService: FakeGasApiService
    private lateinit var fakeOrderDao: FakeOrderDao
    private lateinit var orderRepository: OrderRepository
    private lateinit var menuRepository: MenuRepository
    private lateinit var viewModel: OrderHistoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeGasApiService = FakeGasApiService()
        fakeOrderDao = FakeOrderDao()
        orderRepository = OrderRepository(fakeOrderDao, fakeGasApiService, testDispatcher)
        menuRepository = MenuRepository(FakeMenuDao(), fakeGasApiService, testDispatcher)
        viewModel = OrderHistoryViewModel(orderRepository, menuRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun acceptOnlineOrder_updatesOnlyAcceptanceStatus_leavesPrintStatusUntouched() = runTest(testDispatcher) {
        val onlineOrder = HistoryOrderPayload(
            orderId = "ORD-999",
            timestamp = java.text.SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss", java.util.Locale.US).format(java.util.Date()),
            details = "[]",
            state = "PENDING",
            totalAmount = 100.0,
            paymentStatus = "UNPAID",
            acceptanceStatus = "NEW",
            printStatus = "NOT_PRINTED"
        )
        fakeGasApiService.historyOrdersStore.add(onlineOrder)

        viewModel.fetchHistoryFromCloud()
        testDispatcher.scheduler.advanceUntilIdle()

        // Act: Confirm / Accept order
        viewModel.updateWebOrderState("ORD-999", "READY")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert: Network request only updated order state, printStatus remains untouched
        assertEquals(1, fakeGasApiService.updatedOrderStates.size)
        val sentRequest = fakeGasApiService.updatedOrderStates.first()
        assertEquals("ORD-999", sentRequest["orderId"])
        assertEquals("READY", sentRequest["newState"])
        assertFalse(sentRequest.containsKey("printStatus"))
    }

    @Test
    fun invalidTransition_canceledOrderCannotBeAccepted() = runTest(testDispatcher) {
        val canceledOrder = HistoryOrderPayload(
            orderId = "ORD-404",
            timestamp = java.text.SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss", java.util.Locale.US).format(java.util.Date()),
            details = "[]",
            state = "CANCELED",
            totalAmount = 100.0,
            paymentStatus = "UNPAID",
            acceptanceStatus = "CANCELED"
        )
        fakeGasApiService.historyOrdersStore.add(canceledOrder)

        viewModel.fetchHistoryFromCloud()
        testDispatcher.scheduler.advanceUntilIdle()

        // Act: Try to transition CANCELED -> READY
        viewModel.updateWebOrderState("ORD-404", "READY")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert: Mutation rejected
        val mutation = viewModel.orderMutationStates.value["ORD-404"]
        assertTrue(mutation is OrderMutationState.Failed)
        assertEquals(0, fakeGasApiService.updatedOrderStates.size)
    }

    @Test
    fun enumTransitionGuard_verifiesIllegalTransitions() {
        assertTrue(AcceptanceStatus.NEW.canTransitionTo(AcceptanceStatus.ACCEPTED))
        assertFalse(AcceptanceStatus.CANCELED.canTransitionTo(AcceptanceStatus.ACCEPTED))
        assertFalse(AcceptanceStatus.REJECTED.canTransitionTo(AcceptanceStatus.ACCEPTED))
        
        assertTrue(PrintStatus.NOT_PRINTED.canTransitionTo(PrintStatus.PRINTED))
        assertTrue(PrintStatus.PRINTED.canTransitionTo(PrintStatus.NOT_PRINTED))
    }

    private class FakeOrderDao : OrderDao {
        override suspend fun insertOrder(order: OrderEntity) {}
        override suspend fun insertOrderItems(items: List<OrderItemEntity>): List<Long> = emptyList()
        override suspend fun insertOrderOptions(options: List<OrderOptionEntity>) {}
        override suspend fun createFullOrder(order: OrderEntity, items: List<OrderItemEntity>) {}
        override fun getAllOrders(): Flow<List<OrderEntity>> = flowOf(emptyList())
        override suspend fun getItemsForOrder(orderId: String): List<OrderItemEntity> = emptyList()
        override suspend fun getOptionsForOrderItem(orderItemId: Int): List<OrderOptionEntity> = emptyList()
        override suspend fun markOrderAsSynced(orderId: String) {}
        override fun getUnsyncedOrders(): Flow<List<OrderEntity>> = flowOf(emptyList())
        override suspend fun getUnsyncedOrdersList(): List<OrderEntity> = emptyList()
        override fun getUnsyncedCount(): Flow<Int> = flowOf(0)
    }

    private class FakeMenuDao : MenuDao {
        override fun getAllMenuItems(): Flow<List<MenuItemEntity>> = flowOf(emptyList())
        override fun getAllOptions(): Flow<List<OptionItemEntity>> = flowOf(emptyList())
        override suspend fun insertMenuItems(items: List<MenuItemEntity>) {}
        override suspend fun insertOptions(options: List<OptionItemEntity>) {}
        override suspend fun clearMenuItems() {}
        override suspend fun clearOptions() {}
    }
}
