package com.example.supos.data.network

sealed class GasDomainException(
    override val message: String,
    override val cause: Throwable? = null,
    val isRetryable: Boolean = false
) : Exception(message, cause) {

    class TimeoutError(message: String = "請求超時，請檢查網路連線後重試", cause: Throwable? = null) :
        GasDomainException(message, cause, isRetryable = true)

    class NetworkError(message: String = "無法連線至伺服器，請檢查網路設定", cause: Throwable? = null) :
        GasDomainException(message, cause, isRetryable = true)

    class AuthError(message: String = "API 認證失敗或授權過期", cause: Throwable? = null) :
        GasDomainException(message, cause, isRetryable = false)

    class ServerError(val code: Int? = null, message: String, cause: Throwable? = null) :
        GasDomainException(message, cause, isRetryable = code != null && code >= 500)

    class UnknownError(message: String, cause: Throwable? = null) :
        GasDomainException(message, cause, isRetryable = false)
}
