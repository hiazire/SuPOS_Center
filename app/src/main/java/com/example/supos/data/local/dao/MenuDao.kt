package com.example.supos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.supos.data.local.entity.MenuItemEntity
import com.example.supos.data.local.entity.OptionItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuDao {
    @Query("SELECT * FROM menu_items")
    fun getAllMenuItems(): Flow<List<MenuItemEntity>>

    @Query("SELECT * FROM option_items")
    fun getAllOptions(): Flow<List<OptionItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItems(items: List<MenuItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptions(options: List<OptionItemEntity>)
    
    @Query("DELETE FROM menu_items")
    suspend fun clearMenuItems()
    
    @Query("DELETE FROM option_items")
    suspend fun clearOptions()

    @androidx.room.Transaction
    suspend fun replaceMenuAndOptions(items: List<MenuItemEntity>, options: List<OptionItemEntity>) {
        clearMenuItems()
        clearOptions()
        insertMenuItems(items)
        insertOptions(options)
    }
}
