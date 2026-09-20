package com.example.supos.data.health

object HealthSanitizer {

    private val BEARER_PATTERN = Regex("(?i)bearer\\s+[a-zA-Z0-9_\\-\\.]+")
    private val KEY_PARAM_PATTERN = Regex("(?i)(key|token|secret|password|auth|authorization)=([^&\\s]+)")
    private val GITHUB_TOKEN_PATTERN = Regex("gh[pousr]_[a-zA-Z0-9_]+")

    fun sanitize(message: String?): String? {
        if (message == null) return null
        var sanitized = message
        sanitized = BEARER_PATTERN.replace(sanitized, "Bearer [REDACTED]")
        sanitized = KEY_PARAM_PATTERN.replace(sanitized, "$1=[REDACTED]")
        sanitized = GITHUB_TOKEN_PATTERN.replace(sanitized, "[REDACTED_TOKEN]")
        return sanitized
    }
}
