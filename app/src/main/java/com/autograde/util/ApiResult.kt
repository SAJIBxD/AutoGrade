package com.autograde.util

sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>
    data class Err(val message: String, val code: Int? = null) : ApiResult<Nothing>
    data object Loading : ApiResult<Nothing>
}

fun retrofitErrorMessage(code: Int, body: String?): String {
    if (body.isNullOrBlank()) return "HTTP $code"
    // Try to surface DRF {"detail": "..."} without full JSON parsing here.
    val detail = Regex("\"detail\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.getOrNull(1)
    return detail ?: "HTTP $code: ${body.take(200)}"
}
