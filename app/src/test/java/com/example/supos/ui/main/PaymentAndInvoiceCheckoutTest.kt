package com.example.supos.ui.main

import com.example.supos.data.local.dao.MenuDao
import com.example.supos.data.local.dao.OrderDao
import com.example.supos.data.local.entity.MenuItemEntity
import com.example.supos.data.local.entity.OptionItemEntity
import com.example.supos.data.local.entity.OrderEntity
import com.example.supos.data.local.entity.OrderItemEntity
import com.example.supos.data.local.entity.OrderOptionEntity
import com.example.supos.data.model.PaymentMethod
import com.example.supos.data.repository.MenuRepository
import com.example.supos.data.repository.OrderRepository
import com.example.supos.data.util.InvoiceValidator
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

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentAndInvoiceCheckoutTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeGasApiService: FakeGasApiService
    private lateinit var fakeOrderDao: CapturingOrderDao
    private lateinit var fakeMenuDao: FakeMenuDao
    private lateinit var orderRepository: OrderRepository
    private lateinit var menuRepository: MenuRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeGasApiService = FakeGasApiService()
        fakeOrderDao = CapturingOrderDao()
        fakeMenuDao = FakeMenuDao()
        orderRepository = OrderRepository(fakeOrderDao, fakeGasApiService, testDispatcher)
        menuRepository = MenuRepository(fakeMenuDao, fakeGasApiService, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun invoiceValidator_validatesTaiwanUbnCorrectly() {
        // Valid UBNs (TSMC: 22099131, 7-Eleven: 22555003, 04595257)
        assertTrue(InvoiceValidator.validateUbn("22099131"))
        assertTrue(InvoiceValidator.validateUbn("22555003"))
        assertTrue(InvoiceValidator.validateUbn("04595257"))

        // Invalid UBNs
        assertFalse(InvoiceValidator.validateUbn("12345678"))
        assertFalse(InvoiceValidator.validateUbn("abcdefgh"))
        assertFalse(InvoiceValidator.validateUbn("1234567"))
        assertFalse(InvoiceValidator.validateUbn(""))
    }

    @Test
    fun invoiceValidator_validatesMobileCarrierBarcode() {
        // Valid carriers: / followed by 7 alphanumeric/symbols
        assertTrue(InvoiceValidator.validateCarrier("/ABC+123"))
        assertTrue(InvoiceValidator.validateCarrier("/1234567"))
        assertTrue(InvoiceValidator.validateCarrier("/.-+1234"))
        assertTrue(InvoiceValidator.validateCarrier("")) // empty is valid

        // Invalid carriers
        assertFalse(InvoiceValidator.validateCarrier("ABC+123")) // missing leading slash
        assertFalse(InvoiceValidator.validateCarrier("/123456"))  // too short
        assertFalse(InvoiceValidator.validateCarrier("/12345678")) // too long
    }

    @Test
    fun checkout_invalidUbn_blocksCheckoutWithError() = runTest(testDispatcher) {
        val viewModel = MainScreenViewModel(menuRepository, orderRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val item = MenuItemEntity(id = 1, category = "主餐", name = "漢堡", price = 100, imageUrl = "", optionsGroup = null)
        viewModel.addToCart(item)
        viewModel.setInvoiceInfo(ubn = "11111111") // Invalid checksum
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.checkout()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.checkoutUiState.value
        assertTrue("Checkout should fail for invalid UBN", state is MainScreenViewModel.CheckoutUiState.Error)
        val errorState = state as MainScreenViewModel.CheckoutUiState.Error
        assertTrue(errorState.message.contains("統一編號格式錯誤"))
    }

    @Test
    fun checkout_preservesPaymentMethodAndInvoiceFields() = runTest(testDispatcher) {
        val viewModel = MainScreenViewModel(menuRepository, orderRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val item = MenuItemEntity(id = 1, category = "主餐", name = "招牌堡", price = 150, imageUrl = "", optionsGroup = null)
        viewModel.addToCart(item)
        viewModel.selectPaymentMethod(PaymentMethod.LINE_PAY)
        viewModel.setInvoiceInfo(invoice = "AB-12345678", ubn = "22099131", carrier = "/ABC+123")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.checkout()
        testDispatcher.scheduler.advanceUntilIdle()

        val lastOrder = fakeOrderDao.lastCreatedOrder
        assertNotNull(lastOrder)
        assertEquals("LinePay", lastOrder?.paymentMethod)
        assertEquals("AB-12345678", lastOrder?.invoiceNumber)
        assertEquals("22099131", lastOrder?.ubn)
        assertEquals("/ABC+123", lastOrder?.carrier)

        // Verify GAS Sync Payload
        val lastSynced = fakeGasApiService.lastSyncedOrderPayload
        assertNotNull(lastSynced)
        assertEquals("LinePay", lastSynced?.paymentMethod)
        assertEquals("AB-12345678", lastSynced?.invoiceNumber)
        assertEquals("22099131", lastSynced?.ubn)
        assertEquals("/ABC+123", lastSynced?.carrier)
    }

    private class CapturingOrderDao : OrderDao {
        var lastCreatedOrder: OrderEntity? = null

        override suspend fun insertOrder(order: OrderEntity) {}
        override suspend fun insertOrderItems(items: List<OrderItemEntity>): List<Long> = emptyList()
        override suspend fun insertOrderOptions(options: List<OrderOptionEntity>) {}
        override suspend fun createFullOrder(order: OrderEntity, items: List<OrderItemEntity>) {}
        override suspend fun createFullOrderWithDetails(
            order: OrderEntity,
            itemsWithOptions: List<Pair<OrderItemEntity, List<OrderOptionEntity>>>
        ) {
            lastCreatedOrder = order
        }
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
