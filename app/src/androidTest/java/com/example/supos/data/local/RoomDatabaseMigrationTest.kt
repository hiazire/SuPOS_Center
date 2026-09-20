package com.example.supos.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.supos.data.local.entity.OrderEntity
import com.example.supos.data.local.entity.OrderItemEntity
import com.example.supos.data.local.entity.OrderOptionEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class RoomDatabaseMigrationTest {

    private val TEST_DB = "migration-test-db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SuPOSDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    private lateinit var inMemoryDb: SuPOSDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        inMemoryDb = Room.inMemoryDatabaseBuilder(context, SuPOSDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        inMemoryDb.close()
    }

    @Test
    fun createFullOrderWithDetails_savesOrderOptionsSnapshot() = runBlocking {
        val order = OrderEntity(
            orderId = "TEST-ORD-001",
            totalAmount = 250,
            orderType = "Takeout",
            paymentMethod = "Cash"
        )
        val item = OrderItemEntity(
            orderId = "TEST-ORD-001",
            menuItemId = 10,
            menuItemName = "雙層起司堡",
            quantity = 1,
            subtotal = 250,
            optionsString = "加蛋, 加起司",
            unitPrice = 200,
            customPrice = null,
            isComplimentary = false
        )
        val options = listOf(
            OrderOptionEntity(optionId = 1, optionName = "加蛋", price = 20, quantity = 1),
            OrderOptionEntity(optionId = 2, optionName = "加起司", price = 30, quantity = 1)
        )

        inMemoryDb.orderDao.createFullOrderWithDetails(order, listOf(Pair(item, options)))

        val savedItems = inMemoryDb.orderDao.getItemsForOrder("TEST-ORD-001")
        assertEquals(1, savedItems.size)
        val savedItem = savedItems.first()
        assertEquals(200, savedItem.unitPrice)

        val savedOptions = inMemoryDb.orderDao.getOptionsForOrderItem(savedItem.id)
        assertEquals(2, savedOptions.size)
        assertEquals("加蛋", savedOptions[0].optionName)
        assertEquals(20, savedOptions[0].price)
        assertEquals("加起司", savedOptions[1].optionName)
        assertEquals(30, savedOptions[1].price)
    }

    @Test
    @Throws(IOException::class)
    fun migration_2_to_3_executesSuccessfully() {
        val db = helper.createDatabase(TEST_DB, 2)
        db.close()

        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            SuPOSDatabase.MIGRATION_2_3
        )
        migratedDb.close()
    }
}
