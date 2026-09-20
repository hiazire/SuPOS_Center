package com.example.supos.ui.history

import com.example.supos.data.local.dao.MenuDao
import com.example.supos.data.local.dao.OrderDao
import com.example.supos.data.local.entity.MenuItemEntity
import com.example.supos.data.local.entity.OptionItemEntity
import com.example.supos.data.local.entity.OrderEntity
import com.example.supos.data.local.entity.OrderItemEntity
import com.example.supos.data.local.entity.OrderOptionEntity
import com.example.supos.data.model.OrderStateMachine
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class OrderStateMachineTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeGasApiService: FakeGasApiService
    private lateinit var fakeOrderDao: FakeOrderDao
    private lateinit var fakeMenuDao: FakeMenuDao
    private lateinit var orderRepository: OrderRepository
    private lateinit var menuRepository: MenuRepository

    private val nowFormatted: String
        get() = SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss", Locale.US).format(Date())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeGasApiService = FakeGasApiService()
        fakeOrderDao = FakeOrderDao()
        fakeMenuDao = FakeMenuDao()
        orderRepository = OrderRepository(fakeOrderDao, fakeGasApiService, testDispatcher)
        menuRepository = MenuRepository(fakeMenuDao, fakeGasApiService, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun stateMachine_validAndTerminalTransitions() {
        // Valid transitions
        assertTrue(OrderStateMachine.canTransition(OrderStateMachine.STATE_PENDING, OrderStateMachine.STATE_READY))
        assertTrue(OrderStateMachine.canTransition(OrderStateMachine.STATE_PENDING, OrderStateMachine.STATE_CANCELED))
        assertTrue(OrderStateMachine.canTransition(OrderStateMachine.STATE_READY, OrderStateMachine.STATE_FULFILLED))
        assertTrue(OrderStateMachine.canTransition(OrderStateMachine.STATE_READY, OrderStateMachine.STATE_CANCELED))

        // Same state transition is blocked
        assertFalse(OrderStateMachine.canTransition(OrderStateMachine.STATE_READY, OrderStateMachine.STATE_READY))

        // Terminal states block any transition
        assertTrue(OrderStateMachine.isTerminal(OrderStateMachine.STATE_CANCELED))
        assertTrue(OrderStateMachine.isTerminal(OrderStateMachine.STATE_FULFILLED))

        assertFalse(OrderStateMachine.canTransition(OrderStateMachine.STATE_CANCELED, OrderStateMachine.STATE_PENDING))
        assertFalse(OrderStateMachine.canTransition(OrderStateMachine.STATE_CANCELED, OrderStateMachine.STATE_READY))
        assertFalse(OrderStateMachine.canTransition(OrderStateMachine.STATE_FULFILLED, OrderStateMachine.STATE_READY))
        assertFalse(OrderStateMachine.canTransition(OrderStateMachine.STATE_FULFILLED, OrderStateMachine.STATE_CANCELED))
    }

    @Test
    fun viewModel_blocksIllegalTransitionsFromTerminalStates() = runTest(testDispatcher) {
        val orderId = "ORD-TERM-01"
        fakeGasApiService.historyOrdersStore.add(
            HistoryOrderPayload(
                orderId = orderId,
                timestamp = nowFormatted,
                totalAmount = 180.0,
                paymentStatus = "PAID",
                details = "[]",
                state = OrderStateMachine.STATE_CANCELED,
                cancelReason = "顧客改單"
            )
        )

        val viewModel = OrderHistoryViewModel(orderRepository, menuRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Attempt illegal transition on CANCELED order
        viewModel.updateWebOrderState(orderId, OrderStateMachine.STATE_READY)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.orderMutationStates.value[orderId]
        assertTrue("Mutation state should be Failed", state is OrderMutationState.Failed)
        val failed = state as OrderMutationState.Failed
        assertTrue(failed.message.contains("狀態機規則禁止"))
        assertTrue("No network call should be made for illegal transitions", fakeGasApiService.updatedOrderStates.isEmpty())
    }

    @Test
    fun viewModel_cancellationRequiresReason() = runTest(testDispatcher) {
        val orderId = "ORD-CANCEL-01"
        fakeGasApiService.historyOrdersStore.add(
            HistoryOrderPayload(
                orderId = orderId,
                timestamp = nowFormatted,
                totalAmount = 200.0,
                paymentStatus = "UNPAID",
                details = "[]",
                state = OrderStateMachine.STATE_PENDING
            )
        )

        val viewModel = OrderHistoryViewModel(orderRepository, menuRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Cancel with empty reason
        viewModel.updateWebOrderState(orderId, OrderStateMachine.STATE_CANCELED, cancelReason = "")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.orderMutationStates.value[orderId]
        assertTrue("Cancel with empty reason should fail", state is OrderMutationState.Failed)
        val failed = state as OrderMutationState.Failed
        assertTrue(failed.message.contains("取消原因"))
    }

    @Test
    fun viewModel_mutationFailure_allowsRetry() = runTest(testDispatcher) {
        val orderId = "ORD-RETRY-01"
        fakeGasApiService.historyOrdersStore.add(
            HistoryOrderPayload(
                orderId = orderId,
                timestamp = nowFormatted,
                totalAmount = 300.0,
                paymentStatus = "PAID",
                details = "[]",
                state = OrderStateMachine.STATE_PENDING
            )
        )

        val viewModel = OrderHistoryViewModel(orderRepository, menuRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Simulate API failure
        fakeGasApiService.shouldReturnError = true

        viewModel.updateWebOrderState(orderId, OrderStateMachine.STATE_READY)
        testDispatcher.scheduler.advanceUntilIdle()

        // Check failed with attempt
        val failedState = viewModel.orderMutationStates.value[orderId]
        assertTrue(failedState is OrderMutationState.Failed)
        val failed = failedState as OrderMutationState.Failed
        assertNotNull("Mutation attempt should be captured for retry", failed.attempt)
        assertEquals(OrderStateMachine.STATE_READY, failed.attempt?.targetState)

        // Selected order remains original state (rolled back)
        assertEquals(OrderStateMachine.STATE_PENDING, viewModel.selectedOrder.value?.state)

        // Restore network and retry
        fakeGasApiService.shouldReturnError = false
        viewModel.retryFailedMutation(orderId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify successful retry
        assertEquals(OrderStateMachine.STATE_READY, viewModel.selectedOrder.value?.state)
        val finalState = viewModel.orderMutationStates.value[orderId]
        assertTrue("Mutation state should become Idle upon retry success", finalState is OrderMutationState.Idle)
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

    private class FakeMenuDao : MenuDao {
        override fun getAllMenuItems(): Flow<List<MenuItemEntity>> = flowOf(emptyList())
        override fun getAllOptions(): Flow<List<OptionItemEntity>> = flowOf(emptyList())
        override suspend fun insertMenuItems(items: List<MenuItemEntity>) {}
        override suspend fun insertOptions(options: List<OptionItemEntity>) {}
        override suspend fun clearMenuItems() {}
        override suspend fun clearOptions() {}
        override suspend fun replaceMenuAndOptions(items: List<MenuItemEntity>, options: List<OptionItemEntity>) {}
    }
}
