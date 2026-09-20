package com.example.supos.data.network

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class GasApiServiceContractTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var gasApiService: GasApiService

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        gasApiService = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GasApiService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun fetchMenu_parsesCategoriesAndOptionsCorrectly() = runBlocking {
        val jsonResponse = """
            {
                "success": true,
                "menu": [
                    {
                        "id": "1",
                        "category": "漢堡類",
                        "name": "牛肉堡",
                        "price": "120",
                        "imageUrl": "http://example.com/img.jpg",
                        "options_group": "配料"
                    }
                ],
                "options": [
                    {"group": "配料", "name": "加蛋", "price": "15", "optionType": "SINGLE"}
                ]
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(jsonResponse).setResponseCode(200))

        val result = gasApiService.fetchMenu()

        assertTrue(result.success)
        assertEquals(1, result.menu.size)
        assertEquals("漢堡類", result.menu.first().category)
        assertEquals("牛肉堡", result.menu.first().name)
        assertNotNull(result.options)
        assertEquals(1, result.options!!.size)
        assertEquals("加蛋", result.options!!.first().name)
    }

    @Test
    fun getHistoryOrders_parsesCancelReasonAndDetails() = runBlocking {
        val jsonResponse = """
            {
                "success": true,
                "orders": [
                    {
                        "orderId": "ORD-999",
                        "timestamp": "2026-08-14T08:00:00.000Z",
                        "details": "[{\"name\":\"招牌堡\",\"qty\":1}]",
                        "state": "CANCELED",
                        "totalAmount": 150.0,
                        "paymentStatus": "PAID",
                        "cancelReason": "顧客主動取消"
                    }
                ]
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(jsonResponse).setResponseCode(200))

        val result = gasApiService.getHistoryOrders(HistoryRequest())

        assertTrue(result.success)
        assertNotNull(result.orders)
        val order = result.orders!!.first()
        assertEquals("ORD-999", order.orderId)
        assertEquals("CANCELED", order.state)
        assertEquals("顧客主動取消", order.cancelReason)
    }

    @Test
    fun updateOrderState_parsesErrorResponse() = runBlocking {
        val jsonResponse = """
            {
                "status": "error",
                "message": "Order not found"
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(jsonResponse).setResponseCode(200))

        val result = gasApiService.updateOrderState(mapOf("action" to "updateOrderState", "orderId" to "999", "newState" to "READY"))

        assertEquals("error", result.status)
        assertEquals("Order not found", result.message)
    }
}
