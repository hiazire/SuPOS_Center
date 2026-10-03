package com.example.supos.service

import com.example.supos.data.qburger.QBurgerOrderDetail
import com.example.supos.data.qburger.QBurgerOrderItem
import com.example.supos.data.qburger.QBurgerOrderState
import org.junit.Assert.assertTrue
import org.junit.Test

class KitchenPrinterRouterTest {

    private val router = KitchenPrinterRouter()

    @Test
    fun testBuildKitchenTicketGeneratesEscPosCommands() {
        val order = QBurgerOrderDetail(
            tradeId = "20261002-TEST-001",
            sourceCode = "UberEats",
            sourceName = "UberEats",
            buyerNick = "王小明",
            mobile = "0988123456",
            saleAmount = 187,
            preTimeInterval = "12:30",
            preRemark = "請附餐具",
            paymentName = "UberEats",
            eInvoiceVehicle = "",
            state = QBurgerOrderState.NEW_ORDER,
            items = listOf(
                QBurgerOrderItem("QS04004600", "燒肉蛋吐司", 125, 1, "蛋全熟"),
                QBurgerOrderItem("QS19000400", "招牌奶茶(大)", 25, 2, "去冰,微糖")
            )
        )

        val bytes = router.buildKitchenTicket(order, "煎台單", order.items)
        assertTrue(bytes.isNotEmpty())

        // Check ESC @ (1B 40)
        val hasInit = bytes[0] == 0x1B.toByte() && bytes[1] == 0x40.toByte()
        assertTrue("Must begin with ESC @", hasInit)

        // Check Line spacing 120 (1B 33 78)
        val hasLineSpacing = bytes[2] == 0x1B.toByte() && bytes[3] == 0x33.toByte() && bytes[4] == 120.toByte()
        assertTrue("Must include ESC 3 120", hasLineSpacing)

        // Check EndPaper: Cut command 1D 56 31 and Buzzer 1B 42 02 05
        var hasCut = false
        var hasBuzzer = false
        for (i in 0..bytes.size - 4) {
            if (bytes[i] == 0x1D.toByte() && bytes[i + 1] == 0x56.toByte() && bytes[i + 2] == 0x31.toByte()) {
                hasCut = true
            }
            if (bytes[i] == 0x1B.toByte() && bytes[i + 1] == 0x42.toByte() && bytes[i + 2] == 0x02.toByte() && bytes[i + 3] == 0x05.toByte()) {
                hasBuzzer = true
            }
        }

        assertTrue("Must include GS V 1 cut command", hasCut)
        assertTrue("Must include ESC B 2 5 buzzer alert", hasBuzzer)
    }
}
