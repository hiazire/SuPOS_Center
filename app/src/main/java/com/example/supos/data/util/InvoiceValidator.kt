package com.example.supos.data.util

object InvoiceValidator {

    private val CARRIER_REGEX = Regex("^/[0-9A-Z+.-]{7}$")
    private val UBN_WEIGHTS = intArrayOf(1, 2, 1, 2, 1, 2, 4, 1)

    /**
     * 驗證台灣 8 碼統一編號（財政部邏輯乘數法）
     */
    fun validateUbn(ubn: String): Boolean {
        val trimmed = ubn.trim()
        if (trimmed.length != 8 || !trimmed.all { it.isDigit() }) {
            return false
        }

        var sum = 0
        var hasSpecialSeven = false

        for (i in 0 until 8) {
            val digit = trimmed[i].digitToInt()
            val weight = UBN_WEIGHTS[i]
            val product = digit * weight
            val productSum = (product / 10) + (product % 10)

            if (i == 6 && digit == 7) {
                hasSpecialSeven = true
            }
            sum += productSum
        }

        if (sum % 10 == 0) {
            return true
        }

        // 第 7 位為 7 時，乘積為 28，位數和為 10，可取 1 或 0
        if (hasSpecialSeven && (sum + 1) % 10 == 0) {
            return true
        }

        return false
    }

    /**
     * 驗證手機條碼載具（/ 開頭 + 7 碼英數符號）
     */
    fun validateCarrier(carrier: String): Boolean {
        val trimmed = carrier.trim()
        if (trimmed.isEmpty()) return true
        return CARRIER_REGEX.matches(trimmed)
    }
}
