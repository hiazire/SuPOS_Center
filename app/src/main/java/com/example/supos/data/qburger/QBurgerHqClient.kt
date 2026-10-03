package com.example.supos.data.qburger

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

open class QBurgerHqClient(
    private val endpointUrl: String = "http://103.234.81.171/iPosHQWCFService/iPosHQWCF.svc",
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    private val mediaTypeXml = MediaType.parse("text/xml; charset=utf-8")

    open suspend fun fetchOnlineOrders(
        storeCode: String = "Q23031",
        sources: List<String> = listOf("UberEats", "Foodpanda", "SelfOrder", "QBAPPv2")
    ): Result<List<QBurgerOrderSummary>> = withContext(Dispatchers.IO) {
        val sourcesXml = sources.joinToString("") { "<a:string>$it</a:string>" }
        val soapPayload = """
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
              <s:Body>
                <GetOnlineOrderFromSourceCodes xmlns="http://tempuri.org/">
                  <SourceCodeList xmlns:a="http://schemas.microsoft.com/2003/10/Serialization/Arrays">
                    $sourcesXml
                  </SourceCodeList>
                  <StoreCode>$storeCode</StoreCode>
                </GetOnlineOrderFromSourceCodes>
              </s:Body>
            </s:Envelope>
        """.trimIndent()

        val request = Request.Builder()
            .url(endpointUrl)
            .post(RequestBody.create(mediaTypeXml, soapPayload))
            .addHeader("Content-Type", "text/xml; charset=utf-8")
            .addHeader("SOAPAction", "\"http://tempuri.org/IiPosHQWCF/GetOnlineOrderFromSourceCodes\"")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HQ HTTP ${response.code()}: ${response.message()}"))
                }
                val body = response.body()?.string() ?: ""
                val orders = parseOrdersXml(body)
                Result.success(orders)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open suspend fun fetchOrderDetail(
        sourceCode: String,
        storeCode: String = "Q23031",
        tradeId: String
    ): Result<QBurgerOrderDetail> = withContext(Dispatchers.IO) {
        val soapPayload = """
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
              <s:Body>
                <GetOnlineOrderDetail xmlns="http://tempuri.org/">
                  <SourceCode>$sourceCode</SourceCode>
                  <StoreCode>$storeCode</StoreCode>
                  <tradeId>$tradeId</tradeId>
                </GetOnlineOrderDetail>
              </s:Body>
            </s:Envelope>
        """.trimIndent()

        val request = Request.Builder()
            .url(endpointUrl)
            .post(RequestBody.create(mediaTypeXml, soapPayload))
            .addHeader("Content-Type", "text/xml; charset=utf-8")
            .addHeader("SOAPAction", "\"http://tempuri.org/IiPosHQWCF/GetOnlineOrderDetail\"")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HQ HTTP ${response.code()}: ${response.message()}"))
                }
                val body = response.body()?.string() ?: ""
                val detail = parseOrderDetailXml(body)
                if (detail != null) {
                    Result.success(detail)
                } else {
                    Result.failure(Exception("無法解析訂單明細 XML"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseOrdersXml(xml: String): List<QBurgerOrderSummary> {
        val orders = mutableListOf<QBurgerOrderSummary>()
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))

            val orderNodes = doc.getElementsByTagNameNS("*", "OnlineOrder")
            for (i in 0 until orderNodes.length) {
                val node = orderNodes.item(i)
                if (node.nodeType == Node.ELEMENT_NODE) {
                    val elem = node as Element
                    val tradeId = getTagValue(elem, "TradeID")
                    if (tradeId.isNotBlank()) {
                        val sourceCode = getTagValue(elem, "Source_Code")
                        val sourceName = getTagValue(elem, "Source_Name")
                        val buyerNick = getTagValue(elem, "BuyerNick")
                        val mobile = getTagValue(elem, "Mobile")
                        val rawAmount = getTagValue(elem, "SaleAmount").toIntOrNull() ?: 0
                        val preTimeInterval = getTagValue(elem, "PreTimeInterval")
                        val preRemark = getTagValue(elem, "PreRemark")
                        val vehicle = getTagValue(elem, "EInvoiceVehicle")
                        val statusCode = getTagValue(elem, "Status").toIntOrNull() ?: 0
                        val createTime = getTagValue(elem, "CreateTime")
                        val checkoutId = getTagValue(elem, "Pos_CheckOutIdentifier")

                        orders.add(
                            QBurgerOrderSummary(
                                tradeId = tradeId,
                                sourceCode = sourceCode,
                                sourceName = sourceName.ifEmpty { sourceCode },
                                buyerNick = buyerNick,
                                mobile = mobile,
                                saleAmount = rawAmount / 100,
                                preTimeInterval = preTimeInterval,
                                preRemark = preRemark,
                                eInvoiceVehicle = vehicle,
                                state = QBurgerOrderState.fromCode(statusCode),
                                createTime = createTime,
                                posCheckoutId = checkoutId.ifEmpty { null }
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return orders
    }

    fun parseOrderDetailXml(xml: String): QBurgerOrderDetail? {
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))

            val resultNodes = doc.getElementsByTagNameNS("*", "GetOnlineOrderDetailResult")
            if (resultNodes.length == 0) return null
            val root = resultNodes.item(0) as Element

            val tradeId = getTagValue(root, "TradeID")
            val sourceCode = getTagValue(root, "Source_Code")
            val sourceName = getTagValue(root, "Source_Name")
            val buyerNick = getTagValue(root, "BuyerNick")
            val mobile = getTagValue(root, "Mobile")
            val rawAmount = getTagValue(root, "SaleAmount").toIntOrNull() ?: 0
            val preTimeInterval = getTagValue(root, "PreTimeInterval")
            val preRemark = getTagValue(root, "PreRemark")
            val vehicle = getTagValue(root, "EInvoiceVehicle")
            val statusCode = getTagValue(root, "Status").toIntOrNull() ?: 0

            // Payment
            var paymentName = ""
            val paymentNodes = root.getElementsByTagNameNS("*", "OnlineOrder_Payment")
            for (i in 0 until paymentNodes.length) {
                val pElem = paymentNodes.item(i)
                if (pElem is Element) {
                    val pName = getTagValue(pElem, "PaymentName")
                    if (pName.isNotBlank()) paymentName = pName
                }
            }

            // Products
            val items = mutableListOf<QBurgerOrderItem>()
            val prodNodes = root.getElementsByTagNameNS("*", "OnlineOrder_Product")
            for (i in 0 until prodNodes.length) {
                val pNode = prodNodes.item(i)
                if (pNode is Element) {
                    val childProds = pNode.getElementsByTagNameNS("*", "OnlineOrder_Product")
                    if (childProds.length > 0) continue

                    val pCode = getTagValue(pNode, "POS_ProductCode").ifEmpty { getTagValue(pNode, "Online_ProductCode") }
                    val pName = getTagValue(pNode, "Online_ProductName")
                    if (pName.isNotBlank()) {
                        val rawPrice = getTagValue(pNode, "SalePrice").toIntOrNull() ?: 0
                        val qty = getTagValue(pNode, "Quantity").toIntOrNull() ?: 1
                        val pNote = getTagValue(pNode, "ProductNote")

                        items.add(
                            QBurgerOrderItem(
                                productCode = pCode,
                                productName = pName,
                                price = rawPrice / 100,
                                quantity = qty,
                                productNote = pNote
                            )
                        )
                    }
                }
            }

            return QBurgerOrderDetail(
                tradeId = tradeId,
                sourceCode = sourceCode,
                sourceName = sourceName.ifEmpty { sourceCode },
                buyerNick = buyerNick,
                mobile = mobile,
                saleAmount = rawAmount / 100,
                preTimeInterval = preTimeInterval,
                preRemark = preRemark,
                paymentName = paymentName.ifEmpty { sourceCode },
                eInvoiceVehicle = vehicle,
                state = QBurgerOrderState.fromCode(statusCode),
                items = items
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun getTagValue(parent: Element, tagName: String): String {
        val children = parent.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child is Element && (child.localName == tagName || child.nodeName == tagName || child.nodeName.endsWith(":$tagName"))) {
                return child.textContent?.trim() ?: ""
            }
        }
        val list = parent.getElementsByTagNameNS("*", tagName)
        if (list.length > 0) {
            return list.item(0).textContent?.trim() ?: ""
        }
        val fallback = parent.getElementsByTagName(tagName)
        if (fallback.length > 0) {
            return fallback.item(0).textContent?.trim() ?: ""
        }
        return ""
    }
}
