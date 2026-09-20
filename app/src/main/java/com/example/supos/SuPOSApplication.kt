package com.example.supos

import android.app.Application
import androidx.room.Room
import com.example.supos.data.local.SuPOSDatabase
import com.example.supos.data.network.GasApiService
import com.example.supos.data.repository.MenuRepository
import com.example.supos.data.repository.OrderRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SuPOSApplication : Application() {
    lateinit var database: SuPOSDatabase
        private set
    lateinit var menuRepository: MenuRepository
        private set
    lateinit var orderRepository: OrderRepository
        private set
    lateinit var networkMonitor: com.example.supos.data.health.NetworkMonitor
        private set
    lateinit var systemHealthRepository: com.example.supos.data.health.SystemHealthRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            this,
            SuPOSDatabase::class.java,
            "supos_database"
        )
        .addMigrations(
            SuPOSDatabase.MIGRATION_1_2,
            SuPOSDatabase.MIGRATION_2_3,
            SuPOSDatabase.MIGRATION_3_4,
            SuPOSDatabase.MIGRATION_4_5,
            SuPOSDatabase.MIGRATION_5_6,
            SuPOSDatabase.MIGRATION_6_7
        )
        .build()
        val gasApiService = com.example.supos.data.network.GasApiClientFactory.createGasApiService()

        menuRepository = MenuRepository(database.menuDao, gasApiService)
        orderRepository = OrderRepository(
            orderDao = database.orderDao,
            gasApiService = gasApiService,
            orderDraftDao = database.orderDraftDao
        )
        networkMonitor = com.example.supos.data.health.ConnectivityManagerNetworkMonitor(this)
        systemHealthRepository = com.example.supos.data.health.SystemHealthRepository(
            networkMonitor = networkMonitor,
            menuRepository = menuRepository,
            orderRepository = orderRepository
        )
    }
}
