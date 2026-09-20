package com.example.supos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.supos.data.local.entity.OrderEntity
import com.example.supos.data.local.entity.OrderItemEntity
import com.example.supos.data.local.entity.OrderOptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderOptions(options: List<OrderOptionEntity>)

    @Transaction
    suspend fun createFullOrderWithDetails(
        order: OrderEntity,
        itemsWithOptions: List<Pair<OrderItemEntity, List<OrderOptionEntity>>>
    ) {
        insertOrder(order)
        val items = itemsWithOptions.map { it.first }
        val generatedItemIds = insertOrderItems(items)

        val allOptions = mutableListOf<OrderOptionEntity>()
        itemsWithOptions.forEachIndexed { index, pair ->
            val generatedId = generatedItemIds[index].toInt()
            val options = pair.second.map { it.copy(orderItemId = generatedId) }
            allOptions.addAll(options)
        }
        if (allOptions.isNotEmpty()) {
            insertOrderOptions(allOptions)
        }
    }

    @Transaction
    suspend fun createFullOrder(order: OrderEntity, items: List<OrderItemEntity>) {
        insertOrder(order)
        insertOrderItems(items)
    }

    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getItemsForOrder(orderId: String): List<OrderItemEntity>

    @Query("SELECT * FROM order_options WHERE orderItemId = :orderItemId")
    suspend fun getOptionsForOrderItem(orderItemId: Int): List<OrderOptionEntity>

    @Query("UPDATE orders SET isSynced = 1 WHERE orderId = :orderId")
    suspend fun markOrderAsSynced(orderId: String)

    @Query("SELECT * FROM orders WHERE isSynced = 0 ORDER BY timestamp ASC")
    fun getUnsyncedOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedOrdersList(): List<OrderEntity>

    @Query("SELECT COUNT(*) FROM orders WHERE isSynced = 0")
    fun getUnsyncedCount(): Flow<Int>
}
