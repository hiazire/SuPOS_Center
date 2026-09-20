package com.example.supos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.supos.data.local.entity.OrderDraftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDraftDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: OrderDraftEntity)

    @Query("SELECT * FROM order_drafts WHERE draftType = :type ORDER BY timestamp DESC")
    fun getDraftsByType(type: String): Flow<List<OrderDraftEntity>>

    @Query("SELECT * FROM order_drafts WHERE draftType = :type ORDER BY timestamp DESC")
    suspend fun getDraftsListByType(type: String): List<OrderDraftEntity>

    @Query("SELECT * FROM order_drafts WHERE draftId = :draftId LIMIT 1")
    suspend fun getDraftById(draftId: String): OrderDraftEntity?

    @Query("DELETE FROM order_drafts WHERE draftId = :draftId")
    suspend fun deleteDraft(draftId: String)

    @Query("DELETE FROM order_drafts WHERE draftType = :type")
    suspend fun clearDraftsByType(type: String)
}
