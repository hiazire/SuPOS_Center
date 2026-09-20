package com.example.supos.data.model

enum class AcceptanceStatus {
    NEW,        // 待接單
    ACCEPTED,   // 已接單/已入機
    REJECTED,   // 已拒絕
    CANCELED;   // 已取消

    fun canTransitionTo(target: AcceptanceStatus): Boolean {
        if (this == target) return true
        return when (this) {
            NEW -> target == ACCEPTED || target == REJECTED || target == CANCELED
            ACCEPTED -> target == CANCELED
            REJECTED -> false
            CANCELED -> false
        }
    }
}

enum class PaymentStatus {
    UNPAID,     // 未付款
    PAID,       // 已付款
    REFUNDED;   // 已退款

    fun canTransitionTo(target: PaymentStatus): Boolean {
        if (this == target) return true
        return when (this) {
            UNPAID -> target == PAID || target == REFUNDED
            PAID -> target == REFUNDED
            REFUNDED -> false
        }
    }
}

enum class FulfillmentStatus {
    PENDING,    // 製作中/待出餐
    READY,      // 已完成/可出餐
    COMPLETED;  // 已取餐/完成

    fun canTransitionTo(target: FulfillmentStatus): Boolean {
        if (this == target) return true
        return when (this) {
            PENDING -> target == READY || target == COMPLETED
            READY -> target == COMPLETED
            COMPLETED -> false
        }
    }
}

enum class PrintStatus {
    NOT_PRINTED,  // 未列印
    PRINTED,      // 已列印
    PRINT_FAILED; // 列印失敗

    fun canTransitionTo(target: PrintStatus): Boolean {
        if (this == target) return true
        return when (this) {
            NOT_PRINTED -> target == PRINTED || target == PRINT_FAILED
            PRINTED -> target == NOT_PRINTED || target == PRINT_FAILED
            PRINT_FAILED -> target == PRINTED || target == NOT_PRINTED
        }
    }
}
