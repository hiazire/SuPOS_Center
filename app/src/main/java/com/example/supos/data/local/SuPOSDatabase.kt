package com.example.supos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.supos.data.local.dao.MenuDao
import com.example.supos.data.local.dao.OrderDao
import com.example.supos.data.local.dao.OrderDraftDao
import com.example.supos.data.local.entity.MenuItemEntity
import com.example.supos.data.local.entity.OptionItemEntity
import com.example.supos.data.local.entity.OrderDraftEntity
import com.example.supos.data.local.entity.OrderEntity
import com.example.supos.data.local.entity.OrderItemEntity
import com.example.supos.data.local.entity.OrderOptionEntity

@Database(
    entities = [
        MenuItemEntity::class,
        OptionItemEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        OrderOptionEntity::class,
        OrderDraftEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class SuPOSDatabase : RoomDatabase() {
    abstract val menuDao: MenuDao
    abstract val orderDao: OrderDao
    abstract val orderDraftDao: OrderDraftDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `option_items` (
                        `name` TEXT NOT NULL PRIMARY KEY,
                        `groupName` TEXT NOT NULL,
                        `price` INTEGER NOT NULL,
                        `optionType` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE order_items ADD COLUMN unitPrice INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE order_items ADD COLUMN customPrice INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE order_items ADD COLUMN isComplimentary INTEGER NOT NULL DEFAULT 0")
                
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS order_options (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        orderItemId INTEGER NOT NULL,
                        optionId INTEGER NOT NULL,
                        optionName TEXT NOT NULL,
                        price INTEGER NOT NULL,
                        quantity INTEGER NOT NULL DEFAULT 1,
                        FOREIGN KEY(orderItemId) REFERENCES order_items(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_order_options_orderItemId ON order_options(orderItemId)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE orders ADD COLUMN sourceType TEXT NOT NULL DEFAULT 'OFFLINE_POS'")
                db.execSQL("ALTER TABLE orders ADD COLUMN platformCode TEXT NOT NULL DEFAULT 'POS'")
                db.execSQL("ALTER TABLE orders ADD COLUMN sourceOrderId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE orders ADD COLUMN idempotencyKey TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_orders_platformCode_sourceOrderId` ON `orders` (`platformCode`, `sourceOrderId`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE orders ADD COLUMN acceptanceStatus TEXT NOT NULL DEFAULT 'ACCEPTED'")
                db.execSQL("ALTER TABLE orders ADD COLUMN paymentStatus TEXT NOT NULL DEFAULT 'UNPAID'")
                db.execSQL("ALTER TABLE orders ADD COLUMN fulfillmentStatus TEXT NOT NULL DEFAULT 'PENDING'")
                db.execSQL("ALTER TABLE orders ADD COLUMN printStatus TEXT NOT NULL DEFAULT 'NOT_PRINTED'")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `order_drafts` (
                        `draftId` TEXT NOT NULL PRIMARY KEY,
                        `draftType` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `tableNumber` TEXT NOT NULL DEFAULT '',
                        `transactionType` TEXT NOT NULL DEFAULT '外帶',
                        `createdAtText` TEXT NOT NULL DEFAULT '',
                        `itemsJson` TEXT NOT NULL DEFAULT '[]'
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_order_drafts_draftType` ON `order_drafts` (`draftType`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `orders` ADD COLUMN `invoiceNumber` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `orders` ADD COLUMN `ubn` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `orders` ADD COLUMN `carrier` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `menu_items` ADD COLUMN `sourceOrder` INTEGER NOT NULL DEFAULT 2147483647")
                db.execSQL("ALTER TABLE `menu_items` ADD COLUMN `icon` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `menu_items` ADD COLUMN `printer` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `menu_items` ADD COLUMN `sizeTypes` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `menu_items` ADD COLUMN `isAvailable` TEXT DEFAULT NULL")
            }
        }
    }
}
