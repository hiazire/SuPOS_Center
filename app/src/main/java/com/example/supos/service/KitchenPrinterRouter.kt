package com.example.supos.service

import com.example.supos.data.qburger.QBurgerOrderDetail
import com.example.supos.data.qburger.QBurgerOrderItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PrintTargetResult(
    val targetName: String,
    val ip: String,
    val port: Int,
    val success: Boolean,
    val errorMessage: String? = null
)

class KitchenPrinterRouter(
    private val breadPrinterIp: String = "192.168.100.10",
    private val drinkPrinterIp: String = "192.168.100.11",
    private val grillPrinterIp: String = "192.168.100.12",
    private val printerPort: Int = 9100,
    private val connectTimeoutMs: Int = 3000
) {

    private val charsetBig5: Charset = try {
        Charset.forName("Big5")
    } catch (_: Exception) {
        Charsets.UTF_8
    }

    suspend fun printOrderToKitchen(order: QBurgerOrderDetail): List<PrintTargetResult> = withContext(Dispatchers.IO) {
        val targets = listOf(
            Triple("麵包單", breadPrinterIp, filterItemsForBread(order.items)),
            Triple("飲料單", drinkPrinterIp, filterItemsForDrink(order.items)),
            Triple("煎台單", grillPrinterIp, filterItemsForGrill(order.items))
        )

        val results = mutableListOf<PrintTargetResult>()

        for ((targetName, ip, items) in targets) {
            if (items.isEmpty()) continue

            val ticketBytes = buildKitchenTicket(order, targetName, items)
            val sendResult = sendRawBytes(ip, printerPort, ticketBytes)
            results.add(
                PrintTargetResult(
                    targetName = targetName,
                    ip = ip,
                    port = printerPort,
                    success = sendResult.isSuccess,
                    errorMessage = sendResult.exceptionOrNull()?.message
                )
            )
        }

        results
    }

    suspend fun reprintAll(order: QBurgerOrderDetail, targetIp: String): PrintTargetResult = withContext(Dispatchers.IO) {
        val ticketBytes = buildKitchenTicket(order, "【補印單】", order.items)
        val result = sendRawBytes(targetIp, printerPort, ticketBytes)
        PrintTargetResult(
            targetName = "手動補印",
            ip = targetIp,
            port = printerPort,
            success = result.isSuccess,
            errorMessage = result.exceptionOrNull()?.message
        )
    }

    fun buildKitchenTicket(
        order: QBurgerOrderDetail,
        ticketTitle: String,
        items: List<QBurgerOrderItem>
    ): ByteArray {
        val baos = ByteArrayOutputStream()

        fun write(bytes: ByteArray) = baos.write(bytes)
        fun writeText(text: String) = write(text.toByteArray(charsetBig5))
        fun line(text: String = "") = writeText(text + "\n")

        // 1. ESC @ (Initialize)
        write(byteArrayOf(0x1B, 0x40))

        // 2. ESC 3 120 (Line spacing 120 from Kitchen_03.xml)
        write(byteArrayOf(0x1B, 0x33, 120.toByte()))

        // 3. Center Align & Double Size: Header [UberEats] / [foodpanda] / [自取]
        write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
        write(byteArrayOf(0x1D, 0x21, 0x11)) // Double width & height
        line("[${order.sourceName}] $ticketTitle")

        // 4. Ticket Info
        write(byteArrayOf(0x1D, 0x21, 0x00)) // Normal size
        write(byteArrayOf(0x1B, 0x61, 0x02)) // Right Align
        if (order.preTimeInterval.isNotBlank()) {
            line("取餐時間：${order.preTimeInterval}")
        }
        if (order.preRemark.isNotBlank()) {
            line("備註：${order.preRemark}")
        }

        // 5. Line spacing 60 & Print Time / Serial
        write(byteArrayOf(0x1B, 0x33, 60.toByte()))
        write(byteArrayOf(0x1B, 0x61, 0x00)) // Left Align
        val nowStr = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.TAIWAN).format(Date())
        line("列印時間：$nowStr")
        line("單號：${order.tradeId}")
        if (order.buyerNick.isNotBlank()) {
            line("顧客：${order.buyerNick} ${order.mobile}")
        }
        line("========================================")

        // 6. Body Items
        write(byteArrayOf(0x1B, 0x33, 120.toByte()))
        for (item in items) {
            write(byteArrayOf(0x1D, 0x21, 0x01)) // Double Height
            line(String.format("%2d x %s", item.quantity, item.productName))

            // Flavor / Notes
            if (item.productNote.isNotBlank()) {
                write(byteArrayOf(0x1D, 0x21, 0x00)) // Normal
                line("     【${item.productNote}】")
            }
            item.extraItems.forEach { extra ->
                write(byteArrayOf(0x1D, 0x21, 0x00))
                line("     + $extra")
            }
        }

        // 7. Footer: EndPaper from Kitchen_03.xml
        write(byteArrayOf(0x1B, 0x33, 60.toByte()))
        line("========================================")
        line("總計金額：$ ${order.saleAmount}")

        // Feed 5 lines (ESC d 5)
        write(byteArrayOf(0x1B, 0x64, 0x05))

        // Cut paper (GS V 1)
        write(byteArrayOf(0x1D, 0x56, 0x31))

        // Reset spacing (ESC 2)
        write(byteArrayOf(0x1B, 0x32))

        // Buzzer 2 times, 5ms (ESC B 2 5)
        write(byteArrayOf(0x1B, 0x42, 0x02, 0x05))

        return baos.toByteArray()
    }

    private fun filterItemsForBread(items: List<QBurgerOrderItem>): List<QBurgerOrderItem> {
        val keywords = listOf("可朗芙", "吐司", "三明治", "漢堡", "生菜堡", "烤餅", "厚切")
        return items.filter { item ->
            keywords.any { k -> item.productName.contains(k) } || !isDrink(item) && !isGrill(item)
        }
    }

    private fun filterItemsForDrink(items: List<QBurgerOrderItem>): List<QBurgerOrderItem> {
        val keywords = listOf("茶", "奶", "咖啡", "飲", "湯", "可樂", "汁", "水")
        return items.filter { item -> keywords.any { k -> item.productName.contains(k) } }
    }

    private fun filterItemsForGrill(items: List<QBurgerOrderItem>): List<QBurgerOrderItem> {
        val keywords = listOf("蛋餅", "抓餅", "麵", "飯", "盤餐", "排", "肉", "熱狗", "薯", "雞塊", "點心")
        return items.filter { item -> keywords.any { k -> item.productName.contains(k) } }
    }

    private fun isDrink(item: QBurgerOrderItem): Boolean {
        val keywords = listOf("茶", "奶", "咖啡", "飲", "可樂", "汁")
        return keywords.any { item.productName.contains(it) }
    }

    private fun isGrill(item: QBurgerOrderItem): Boolean {
        val keywords = listOf("蛋餅", "抓餅", "麵", "飯", "盤餐")
        return keywords.any { item.productName.contains(it) }
    }

    private fun sendRawBytes(ip: String, port: Int, data: ByteArray): Result<Unit> {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), connectTimeoutMs)
                socket.soTimeout = 5000
                val os: OutputStream = socket.getOutputStream()
                os.write(data)
                os.flush()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
