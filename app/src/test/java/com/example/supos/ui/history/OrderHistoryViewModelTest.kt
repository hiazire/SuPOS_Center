package com.example.supos.ui.history

import com.example.supos.data.local.dao.OrderDao
import com.example.supos.data.local.entity.OrderEntity
import com.example.supos.data.local.entity.OrderItemEntity
import com.example.supos.data.local.entity.OrderOptionEntity
import com.example.supos.data.network.HistoryOrderPayload
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrderHistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeGasApiService: FakeGasApiService
    private lateinit var fakeOrderDao: FakeOrderDao
    private lateinit var orderRepository: OrderRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeGasApiService = FakeGasApiService()
        fakeOrderDao = FakeOrderDao()
        orderRepository = OrderRepository(fakeOrderDao, fakeGasApiService, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): OrderHistoryViewModel {
        val menuRepository = com.example.supos.data.repository.MenuRepository(FakeMenuDao(), fakeGasApiService)
        val vm = OrderHistoryViewModel(orderRepository, menuRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        return vm
    }

    @Test
    fun fetchHistoryFromCloud_success_filtersTodayOrders() = runTest(testDispatcher) {
        val todayStr = java.text.SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val testOrder = HistoryOrderPayload(
            orderId = "ORD-101",
            timestamp = "$todayStr GMT+0800",
            details = "[]",
            state = "PENDING",
            totalAmount = 150.0,
            paymentStatus = "UNPAID"
        )
        fakeGasApiService.historyOrdersStore.add(testOrder)

        val viewModel = createViewModel()

        val state = viewModel.ordersState.value
        assertTrue(state is OrderHistoryUiState.Success)
        val orders = (state as OrderHistoryUiState.Success).orders
        assertEquals(1, orders.size)
        assertEquals("ORD-101", orders.first().orderId)
    }

    @Test
    fun updateWebOrderState_optimisticUpdateAndSuccess() = runTest(testDispatcher) {
        val todayStr = java.text.SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val testOrder = HistoryOrderPayload(
            orderId = "ORD-102",
            timestamp = "$todayStr GMT+0800",
            details = "[]",
            state = "PENDING",
            totalAmount = 200.0,
            paymentStatus = "UNPAID"
        )
        fakeGasApiService.historyOrdersStore.add(testOrder)
        val viewModel = createViewModel()

        viewModel.updateWebOrderState("ORD-102", "READY")
        testDispatcher.scheduler.advanceUntilIdle()

        val mutationState = viewModel.orderMutationStates.value["ORD-102"]
        assertEquals(OrderMutationState.Idle, mutationState)
        assertEquals("READY", viewModel.selectedOrder.value?.state)
    }

    @Test
    fun updateWebOrderState_failureRollback() = runTest(testDispatcher) {
        val todayStr = java.text.SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val testOrder = HistoryOrderPayload(
            orderId = "ORD-103",
            timestamp = "$todayStr GMT+0800",
            details = "[]",
            state = "PENDING",
            totalAmount = 200.0,
            paymentStatus = "UNPAID"
        )
        fakeGasApiService.historyOrdersStore.add(testOrder)
        val viewModel = createViewModel()

        fakeGasApiService.shouldReturnError = true
        fakeGasApiService.errorMessage = "Server down"

        viewModel.updateWebOrderState("ORD-103", "READY")
        testDispatcher.scheduler.advanceUntilIdle()

        val mutationState = viewModel.orderMutationStates.value["ORD-103"]
        assertTrue(mutationState is OrderMutationState.Failed)
        assertEquals("PENDING", viewModel.selectedOrder.value?.state)
    }

    @Test
    fun updateWebOrderState_stateMachineGuard_blocksCanceledOrder() = runTest(testDispatcher) {
        val todayStr = java.text.SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val testOrder = HistoryOrderPayload(
            orderId = "ORD-104",
            timestamp = "$todayStr GMT+0800",
            details = "[]",
            state = "CANCELED",
            totalAmount = 100.0,
            paymentStatus = "UNPAID"
        )
        fakeGasApiService.historyOrdersStore.add(testOrder)
        val viewModel = createViewModel()

        viewModel.updateWebOrderState("ORD-104", "READY")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("CANCELED", viewModel.selectedOrder.value?.state)
        assertEquals(0, fakeGasApiService.updatedOrderStates.size)
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

    private class FakeMenuDao : com.example.supos.data.local.dao.MenuDao {
        override fun getAllMenuItems(): Flow<List<com.example.supos.data.local.entity.MenuItemEntity>> = flowOf(emptyList())
        override fun getAllOptions(): Flow<List<com.example.supos.data.local.entity.OptionItemEntity>> = flowOf(emptyList())
        override suspend fun insertMenuItems(items: List<com.example.supos.data.local.entity.MenuItemEntity>) {}
        override suspend fun insertOptions(options: List<com.example.supos.data.local.entity.OptionItemEntity>) {}
        override suspend fun clearMenuItems() {}
        override suspend fun clearOptions() {}
    }
}
