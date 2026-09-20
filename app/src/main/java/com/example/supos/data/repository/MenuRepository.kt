package com.example.supos.data.repository

import android.util.Log
import com.example.supos.data.local.dao.MenuDao
import com.example.supos.data.local.entity.MenuItemEntity
import com.example.supos.data.local.entity.OptionItemEntity
import com.example.supos.data.network.GasApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MenuRepository(
    private val menuDao: MenuDao,
    private val gasApiService: GasApiService,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) {
    private fun safeLogD(tag: String, msg: String) {
        try {
            android.util.Log.d(tag, msg)
        } catch (_: Throwable) {
            println("[$tag] $msg")
        }
    }

    private fun safeLogE(tag: String, msg: String, tr: Throwable? = null) {
        try {
            android.util.Log.e(tag, msg, tr)
        } catch (_: Throwable) {
            println("[$tag] $msg ${tr?.message ?: ""}")
        }
    }

    private val _lastSuccessfulSyncAt = kotlinx.coroutines.flow.MutableStateFlow<Long?>(null)
    val lastSuccessfulSyncAt: kotlinx.coroutines.flow.StateFlow<Long?> = _lastSuccessfulSyncAt

    fun getMenuItems(): Flow<List<MenuItemEntity>> {
        return menuDao.getAllMenuItems()
    }

    fun getOptions(): Flow<List<OptionItemEntity>> {
        return menuDao.getAllOptions()
    }

    suspend fun refreshMenuFromNetwork(): Result<Unit> = withContext(ioDispatcher) {
        try {
            val response = gasApiService.fetchMenu()
            if (!response.success) {
                safeLogE("MenuRepository", "GAS returned success=false for menu sync")
                return@withContext Result.failure(Exception("GAS returned success=false"))
            }

            val validMenu = mutableListOf<MenuItemEntity>()
            val rejectedMenuItems = mutableListOf<String>()

            for (payload in response.menu) {
                if (payload.name.isBlank() || payload.category.isBlank()) {
                    rejectedMenuItems.add("${payload.name} (空白名稱或分類)")
                    continue
                }
                val parsedPrice = payload.price.toIntOrNull()
                if (parsedPrice == null || parsedPrice < 0) {
                    rejectedMenuItems.add("${payload.name} (非法價格: ${payload.price})")
                    continue
                }

                validMenu.add(
                    MenuItemEntity(
                        id = payload.id.toIntOrNull() ?: payload.name.hashCode(),
                        category = payload.category.trim(),
                        name = payload.name.trim(),
                        price = parsedPrice,
                        imageUrl = payload.imageUrl ?: "",
                        optionsGroup = payload.optionsGroup
                    )
                )
            }

            if (rejectedMenuItems.isNotEmpty()) {
                safeLogE("MenuRepository", "Rejected illegal menu items: ${rejectedMenuItems.joinToString()}")
            }

            if (validMenu.isEmpty()) {
                safeLogE("MenuRepository", "No valid menu items found in network response. Preserving existing local menu.")
                return@withContext Result.failure(Exception("無有效菜單品項，已保留既有菜單"))
            }

            val validOptions = mutableListOf<OptionItemEntity>()
            response.options?.forEach { payload ->
                if (payload.name.isNotBlank() && !payload.group.isNullOrBlank()) {
                    val optPrice = payload.price.toIntOrNull()
                    if (optPrice != null && optPrice >= 0) {
                        validOptions.add(
                            OptionItemEntity(
                                name = payload.name.trim(),
                                groupName = payload.group.trim(),
                                price = optPrice,
                                optionType = payload.optionType ?: ""
                            )
                        )
                    } else {
                        safeLogE("MenuRepository", "Rejected option with illegal price: ${payload.name} (${payload.price})")
                    }
                }
            }

            // 單一 Transaction 原子替換舊資料
            menuDao.replaceMenuAndOptions(validMenu, validOptions)
            _lastSuccessfulSyncAt.value = System.currentTimeMillis()
            safeLogD("MenuRepository", "Successfully synced and replaced menu atomically (${validMenu.size} items, ${validOptions.size} options).")
            Result.success(Unit)
        } catch (e: Exception) {
            safeLogE("MenuRepository", "Failed to fetch or replace menu from GAS", e)
            Result.failure(e)
        }
    }
}
