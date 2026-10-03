package com.example.supos.data.qburger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QBurgerHqClientTest {

    private val client = QBurgerHqClient()

    private val sampleOrdersXml = """
        <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
          <s:Body>
            <GetOnlineOrderFromSourceCodesResponse xmlns="http://tempuri.org/">
              <GetOnlineOrderFromSourceCodesResult xmlns:a="http://schemas.datacontract.org/2004/07/Ptc.iPos.Extends.AppMemberOrder.Class" xmlns:i="http://www.w3.org/2001/XMLSchema-instance">
                <a:OnlineOrder>
                  <a:BuyerId>453 53 399</a:BuyerId>
                  <a:BuyerNick>嘉**</a:BuyerNick>
                  <a:CreateTime>2026-10-02T12:37:05.3</a:CreateTime>
                  <a:DisAmount>0</a:DisAmount>
                  <a:EInvoiceVehicle>/9L0Y5U2</a:EInvoiceVehicle>
                  <a:Mobile>0912345678</a:Mobile>
                  <a:Num>1</a:Num>
                  <a:PaidAmount>18700</a:PaidAmount>
                  <a:Pos_CheckOutIdentifier>20261002123744-Q23031-0-01884</a:Pos_CheckOutIdentifier>
                  <a:Pos_CheckOutPOS>01</a:Pos_CheckOutPOS>
                  <a:PreName>嘉**</a:PreName>
                  <a:PreRemark>要餐具、吸管</a:PreRemark>
                  <a:PreStoreCode>Q23031</a:PreStoreCode>
                  <a:PreStoreName>八德介壽店</a:PreStoreName>
                  <a:PreTimeInterval>12:57</a:PreTimeInterval>
                  <a:SaleAmount>18700</a:SaleAmount>
                  <a:Source_Code>UberEats</a:Source_Code>
                  <a:Source_Name>UberEats</a:Source_Name>
                  <a:Status>1</a:Status>
                  <a:TradeID>8E6D1EB3-893B-4B69-BB11-432E6FA7A562</a:TradeID>
                </a:OnlineOrder>
              </GetOnlineOrderFromSourceCodesResult>
            </GetOnlineOrderFromSourceCodesResponse>
          </s:Body>
        </s:Envelope>
    """.trimIndent()

    private val sampleDetailXml = """
        <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
          <s:Body>
            <GetOnlineOrderDetailResponse xmlns="http://tempuri.org/">
              <GetOnlineOrderDetailResult xmlns:a="http://schemas.datacontract.org/2004/07/Ptc.iPos.Extends.AppMemberOrder.Class" xmlns:i="http://www.w3.org/2001/XMLSchema-instance">
                <a:BuyerId>453 53 399</a:BuyerId>
                <a:BuyerNick>嘉**</a:BuyerNick>
                <a:CreateTime>2026-10-02T12:37:05.3</a:CreateTime>
                <a:EInvoiceVehicle>/9L0Y5U2</a:EInvoiceVehicle>
                <a:Mobile>0912345678</a:Mobile>
                <a:OnlineOrder_Payment>
                  <a:OnlineOrder_Payment>
                    <a:PaymentName>UberEats</a:PaymentName>
                    <a:PayPrice>18700</a:PayPrice>
                  </a:OnlineOrder_Payment>
                </a:OnlineOrder_Payment>
                <a:OnlineOrder_Product>
                  <a:OnlineOrder_Product>
                    <a:Online_ProductName>【套餐】燒肉蛋吐司｜Yakiniku w/ Egg Toast</a:Online_ProductName>
                    <a:POS_ProductCode>QS04004600</a:POS_ProductCode>
                    <a:ProductNote></a:ProductNote>
                    <a:Quantity>1</a:Quantity>
                    <a:SalePrice>12500</a:SalePrice>
                  </a:OnlineOrder_Product>
                  <a:OnlineOrder_Product>
                    <a:Online_ProductName>招牌奶茶(大)｜Signature Milk Tea (L)</a:Online_ProductName>
                    <a:POS_ProductCode>QS19000400</a:POS_ProductCode>
                    <a:ProductNote>正常糖,冰</a:ProductNote>
                    <a:Quantity>1</a:Quantity>
                    <a:SalePrice>700</a:SalePrice>
                  </a:OnlineOrder_Product>
                  <a:OnlineOrder_Product>
                    <a:Online_ProductName>QQ地瓜球｜Sweet Potato Balls</a:Online_ProductName>
                    <a:POS_ProductCode>QS15003100</a:POS_ProductCode>
                    <a:ProductNote></a:ProductNote>
                    <a:Quantity>1</a:Quantity>
                    <a:SalePrice>5500</a:SalePrice>
                  </a:OnlineOrder_Product>
                </a:OnlineOrder_Product>
                <a:PreRemark>要餐具、吸管</a:PreRemark>
                <a:PreTimeInterval>12:57</a:PreTimeInterval>
                <a:SaleAmount>18700</a:SaleAmount>
                <a:Source_Code>UberEats</a:Source_Code>
                <a:Source_Name>UberEats</a:Source_Name>
                <a:Status>1</a:Status>
                <a:TradeID>8E6D1EB3-893B-4B69-BB11-432E6FA7A562</a:TradeID>
              </GetOnlineOrderDetailResult>
            </GetOnlineOrderDetailResponse>
          </s:Body>
        </s:Envelope>
    """.trimIndent()

    @Test
    fun testParseOrdersXml() {
        val orders = client.parseOrdersXml(sampleOrdersXml)
        assertEquals(1, orders.size)
        val order = orders[0]
        assertEquals("8E6D1EB3-893B-4B69-BB11-432E6FA7A562", order.tradeId)
        assertEquals("UberEats", order.sourceCode)
        assertEquals("UberEats", order.sourceName)
        assertEquals("嘉**", order.buyerNick)
        assertEquals("0912345678", order.mobile)
        assertEquals(187, order.saleAmount) // 18700 / 100
        assertEquals("12:57", order.preTimeInterval)
        assertEquals("要餐具、吸管", order.preRemark)
        assertEquals("/9L0Y5U2", order.eInvoiceVehicle)
        assertEquals(QBurgerOrderState.ORDERED, order.state)
    }

    @Test
    fun testParseOrderDetailXml() {
        val detail = client.parseOrderDetailXml(sampleDetailXml)
        assertNotNull(detail)
        assertEquals("8E6D1EB3-893B-4B69-BB11-432E6FA7A562", detail!!.tradeId)
        assertEquals("UberEats", detail.sourceCode)
        assertEquals(187, detail.saleAmount)
        assertEquals("UberEats", detail.paymentName)
        assertEquals(3, detail.items.size)

        val item1 = detail.items[0]
        assertEquals("【套餐】燒肉蛋吐司｜Yakiniku w/ Egg Toast", item1.productName)
        assertEquals("QS04004600", item1.productCode)
        assertEquals(125, item1.price)
        assertEquals(1, item1.quantity)

        val item2 = detail.items[1]
        assertEquals("招牌奶茶(大)｜Signature Milk Tea (L)", item2.productName)
        assertEquals(7, item2.price)
        assertEquals("正常糖,冰", item2.productNote)

        val item3 = detail.items[2]
        assertEquals("QQ地瓜球｜Sweet Potato Balls", item3.productName)
        assertEquals(55, item3.price)
    }
}
