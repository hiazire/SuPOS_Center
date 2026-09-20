package com.example.supos.data.model

enum class PaymentMethod(val displayName: String, val code: String) {
    CASH("現金", "CASH"),
    LINE_PAY("LinePay", "LINE_PAY"),
    CREDIT_CARD("信用卡", "CREDIT_CARD"),
    PLATFORM_PAY("平台支付", "PLATFORM_PAY"),
    OTHER("其他", "OTHER");

    companion object {
        fun fromDisplayName(name: String): PaymentMethod {
            return entries.firstOrNull { 
                it.displayName.equals(name, ignoreCase = true) || it.code.equals(name, ignoreCase = true) 
            } ?: CASH
        }
    }
}
