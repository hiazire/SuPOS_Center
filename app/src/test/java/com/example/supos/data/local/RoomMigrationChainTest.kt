package com.example.supos.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

class RoomMigrationChainTest {

    @Test
    fun migration_1_to_2_createsOptionItemsTable() {
        val statements = mutableListOf<String>()
        val mockDb = createCapturingMockDb(statements)

        SuPOSDatabase.MIGRATION_1_2.migrate(mockDb)

        assertTrue(statements.any { it.contains("CREATE TABLE IF NOT EXISTS `option_items`") })
    }

    @Test
    fun migration_2_to_3_addsPriceFieldsAndOptionsTable() {
        val statements = mutableListOf<String>()
        val mockDb = createCapturingMockDb(statements)

        SuPOSDatabase.MIGRATION_2_3.migrate(mockDb)

        assertTrue(statements.any { it.contains("ALTER TABLE order_items ADD COLUMN unitPrice") })
        assertTrue(statements.any { it.contains("ALTER TABLE order_items ADD COLUMN customPrice") })
        assertTrue(statements.any { it.contains("ALTER TABLE order_items ADD COLUMN isComplimentary") })
        assertTrue(statements.any { it.contains("CREATE TABLE IF NOT EXISTS order_options") })
    }

    @Test
    fun migration_3_to_4_addsSourceFieldsAndUniqueIndex() {
        val statements = mutableListOf<String>()
        val mockDb = createCapturingMockDb(statements)

        SuPOSDatabase.MIGRATION_3_4.migrate(mockDb)

        assertTrue(statements.any { it.contains("ALTER TABLE orders ADD COLUMN sourceType") })
        assertTrue(statements.any { it.contains("ALTER TABLE orders ADD COLUMN platformCode") })
        assertTrue(statements.any { it.contains("ALTER TABLE orders ADD COLUMN sourceOrderId") })
        assertTrue(statements.any { it.contains("ALTER TABLE orders ADD COLUMN idempotencyKey") })
        assertTrue(statements.any { it.contains("CREATE UNIQUE INDEX IF NOT EXISTS `index_orders_platformCode_sourceOrderId`") })
    }

    @Test
    fun migration_4_to_5_addsFourDimensionalStatusFields() {
        val statements = mutableListOf<String>()
        val mockDb = createCapturingMockDb(statements)

        SuPOSDatabase.MIGRATION_4_5.migrate(mockDb)

        assertTrue(statements.any { it.contains("ALTER TABLE orders ADD COLUMN acceptanceStatus") })
        assertTrue(statements.any { it.contains("ALTER TABLE orders ADD COLUMN paymentStatus") })
        assertTrue(statements.any { it.contains("ALTER TABLE orders ADD COLUMN fulfillmentStatus") })
        assertTrue(statements.any { it.contains("ALTER TABLE orders ADD COLUMN printStatus") })
    }

    @Test
    fun migration_5_to_6_createsOrderDraftsTable() {
        val statements = mutableListOf<String>()
        val mockDb = createCapturingMockDb(statements)

        SuPOSDatabase.MIGRATION_5_6.migrate(mockDb)

        assertTrue(statements.any { it.contains("CREATE TABLE IF NOT EXISTS `order_drafts`") })
    }

    @Test
    fun migration_6_to_7_addsInvoiceFields() {
        val statements = mutableListOf<String>()
        val mockDb = createCapturingMockDb(statements)

        SuPOSDatabase.MIGRATION_6_7.migrate(mockDb)

        assertTrue(statements.any { it.contains("ALTER TABLE `orders` ADD COLUMN `invoiceNumber`") })
        assertTrue(statements.any { it.contains("ALTER TABLE `orders` ADD COLUMN `ubn`") })
        assertTrue(statements.any { it.contains("ALTER TABLE `orders` ADD COLUMN `carrier`") })
    }

    private fun createCapturingMockDb(statements: MutableList<String>): SupportSQLiteDatabase {
        val handler = InvocationHandler { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                statements.add(args[0] as String)
            }
            null
        }
        return Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
            handler
        ) as SupportSQLiteDatabase
    }
}
