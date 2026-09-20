package com.example.supos.data.repository

import com.example.supos.data.local.dao.MenuDao
import com.example.supos.data.local.entity.MenuItemEntity
import com.example.supos.data.local.entity.OptionItemEntity
import com.example.supos.data.network.MenuItemPayload
import com.example.supos.data.network.MenuResponsePayload
import com.example.supos.data.network.OptionItemPayload
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
class MenuAtomicSyncTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeGasApiService: FakeGasApiService
    private lateinit var fakeMenuDao: MemoryMenuDao
    private lateinit var repository: MenuRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeGasApiService = FakeGasApiService()
        fakeMenuDao = MemoryMenuDao()
        repository = MenuRepository(fakeMenuDao, fakeGasApiService, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun syncMenu_invalidPrices_rejectsIllegalItemsAndKeepsValid() = runTest(testDispatcher) {
        val networkMenu = listOf(
            MenuItemPayload(id = "1", category = "飲品", name = "可樂", price = "30", imageUrl = "", optionsGroup = null),
            MenuItemPayload(id = "2", category = "主餐", name = "免費漢堡", price = "-50", imageUrl = "", optionsGroup = null),
            MenuItemPayload(id = "3", category = "主餐", name = "未知餐點", price = "abc", imageUrl = "", optionsGroup = null)
        )
        val networkOptions = listOf(
            OptionItemPayload(name = "加蛋", group = "加料", price = "15", optionType = "multi"),
            OptionItemPayload(name = "非法加料", group = "加料", price = "-10", optionType = "multi")
        )

        fakeGasApiService.customMenuResponse = MenuResponsePayload(
            success = true,
            menu = networkMenu,
            options = networkOptions
        )

        val result = repository.refreshMenuFromNetwork()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(result.isSuccess)
        assertNotNull(repository.lastSuccessfulSyncAt.value)

        // Only valid item "可樂" (price 30) should be saved
        assertEquals(1, fakeMenuDao.storedMenuItems.size)
        val savedItem = fakeMenuDao.storedMenuItems.first()
        assertEquals("可樂", savedItem.name)
        assertEquals(30, savedItem.price)

        // Only valid option "加蛋" (price 15) should be saved
        assertEquals(1, fakeMenuDao.storedOptions.size)
        val savedOption = fakeMenuDao.storedOptions.first()
        assertEquals("加蛋", savedOption.name)
        assertEquals(15, savedOption.price)
    }

    @Test
    fun syncMenu_emptyOrCorruptPayload_preservesExistingMenu() = runTest(testDispatcher) {
        // Preset existing menu
        fakeMenuDao.storedMenuItems.add(MenuItemEntity(id = 99, category = "主餐", name = "牛肉麵", price = 150, imageUrl = "", optionsGroup = null))
        fakeMenuDao.storedOptions.add(OptionItemEntity(name = "大碗", groupName = "份量", price = 20, optionType = "single"))

        // Network returns empty menu
        fakeGasApiService.customMenuResponse = MenuResponsePayload(
            success = true,
            menu = emptyList(),
            options = emptyList()
        )

        val result = repository.refreshMenuFromNetwork()
        testDispatcher.scheduler.advanceUntilIdle()

        // Sync fails and does NOT clear database
        assertFalse(result.isSuccess)
        assertEquals(1, fakeMenuDao.storedMenuItems.size)
        assertEquals("牛肉麵", fakeMenuDao.storedMenuItems.first().name)
        assertEquals(1, fakeMenuDao.storedOptions.size)
    }

    @Test
    fun syncMenu_dbException_rollsBackAtomically() = runTest(testDispatcher) {
        fakeMenuDao.storedMenuItems.add(MenuItemEntity(id = 99, category = "主餐", name = "牛肉麵", price = 150, imageUrl = "", optionsGroup = null))
        fakeMenuDao.shouldFailOnReplace = true

        fakeGasApiService.customMenuResponse = MenuResponsePayload(
            success = true,
            menu = listOf(MenuItemPayload(id = "1", category = "主餐", name = "漢堡", price = "100", imageUrl = "", optionsGroup = null)),
            options = emptyList()
        )

        val result = repository.refreshMenuFromNetwork()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(result.isSuccess)
        // Existing item remains untouched due to rollback
        assertEquals(1, fakeMenuDao.storedMenuItems.size)
        assertEquals("牛肉麵", fakeMenuDao.storedMenuItems.first().name)
    }

    private class MemoryMenuDao : MenuDao {
        val storedMenuItems = mutableListOf<MenuItemEntity>()
        val storedOptions = mutableListOf<OptionItemEntity>()
        var shouldFailOnReplace = false

        override fun getAllMenuItems(): Flow<List<MenuItemEntity>> = flowOf(storedMenuItems.toList())
        override fun getAllOptions(): Flow<List<OptionItemEntity>> = flowOf(storedOptions.toList())
        override suspend fun insertMenuItems(items: List<MenuItemEntity>) {
            storedMenuItems.addAll(items)
        }
        override suspend fun insertOptions(options: List<OptionItemEntity>) {
            storedOptions.addAll(options)
        }
        override suspend fun clearMenuItems() {
            storedMenuItems.clear()
        }
        override suspend fun clearOptions() {
            storedOptions.clear()
        }

        override suspend fun replaceMenuAndOptions(items: List<MenuItemEntity>, options: List<OptionItemEntity>) {
            if (shouldFailOnReplace) {
                throw RuntimeException("Transaction error: SQLite disk I/O error")
            }
            storedMenuItems.clear()
            storedOptions.clear()
            storedMenuItems.addAll(items)
            storedOptions.addAll(options)
        }
    }
}
