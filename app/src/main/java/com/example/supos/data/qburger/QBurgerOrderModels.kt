package com.example.supos.data.qburger

import java.io.Serializable

enum class QBurgerOrderState(val code: Int, val displayName: String) {
    NEW_ORDER(0, "未入機"),
    ORDERED(1, "已入機"),
    PRINTED(2, "已出單"),
    CHECKED_OUT(3, "已結帳"),
    CANCELLED(4, "已退單"),
    NON_PAY_CANCEL(5, "未取餐");

    companion object {
        fun fromCode(code: Int): QBurgerOrderState {
            return entries.firstOrNull { it.code == code } ?: NEW_ORDER
        }
    }
}

data class QBurgerOrderSummary(
    val tradeId: String,
    val sourceCode: String,
    val sourceName: String,
    val buyerNick: String,
    val mobile: String,
    val saleAmount: Int, // in NTD (divided by 100)
    val preTimeInterval: String, // e.g. "12:57"
    val preRemark: String,
    val eInvoiceVehicle: String,
    val state: QBurgerOrderState,
    val createTime: String,
    val posCheckoutId: String? = null
) : Serializable

data class QBurgerOrderItem(
    val productCode: String,
    val productName: String,
    val price: Int, // in NTD
    val quantity: Int,
    val productNote: String, // e.g. "正常糖,冰"
    val extraItems: List<String> = emptyList()
) : Serializable

data class QBurgerOrderDetail(
    val tradeId: String,
    val sourceCode: String,
    val sourceName: String,
    val buyerNick: String,
    val mobile: String,
    val saleAmount: Int,
    val preTimeInterval: String,
    val preRemark: String,
    val paymentName: String,
    val eInvoiceVehicle: String,
    val state: QBurgerOrderState,
    val items: List<QBurgerOrderItem>
) : Serializable
