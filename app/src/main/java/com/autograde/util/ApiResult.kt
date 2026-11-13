package com.autograde.util

sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>
    data class Err(val message: String, val code: Int? = null) : ApiResult<Nothing>
    data object Loading : ApiResult<Nothing>
}

fun retrofitErrorMessage(code: Int, body: String?): String {
    if (body.isNullOrBlank()) return "HTTP $code"
    // SimpleJWT's catch-all for an expired/invalid/wrong-type token. In this app
    // that means the session is gone (rotated, blacklisted or timed out), which
    // the raw backend wording does not tell the user.
    if (code == 401 && (body.contains("token_not_valid") || body.contains("Given token not valid"))) {
        return "Session expired. Please log in again."
    }
    // Surface DRF's {"detail": "..."} or the filesystem app's {"error": "..."}
    // without pulling in a JSON parser here.
    val patterns = listOf(
        "\"detail\"\\s*:\\s*\"([^\"]+)\"",
        "\"error\"\\s*:\\s*\"([^\"]+)\"",
    )
    for (pattern in patterns) {
        val found = Regex(pattern).find(body)?.groupValues?.getOrNull(1)
        if (found != null) return found
    }
    return "HTTP $code: ${body.take(200)}"
}
