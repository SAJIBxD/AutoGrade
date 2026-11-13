package com.autograde.util

/**
 * Just enough JWT inspection to spot an expired access token *before* sending
 * it (the backend only mints 6-minute access tokens, so a slow multi-step flow
 * easily outlives one). Signature checking stays the server's job: if a token
 * we consider fresh is rejected anyway, the 401-driven refresh still runs.
 */
object Jwt {

    private const val ALPHABET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    /** True when the token's `exp` claim is in the past (within [skewMs]). */
    fun isExpired(
        token: String,
        nowMs: Long = System.currentTimeMillis(),
        skewMs: Long = 60_000,
    ): Boolean {
        val payload = token.split('.').getOrNull(1) ?: return true
        val json = decodeUrlSafeBase64(payload)?.toString(Charsets.UTF_8) ?: return true
        val expSeconds = Regex("\"exp\"\\s*:\\s*(\\d+)")
            .find(json)?.groupValues?.get(1)?.toLongOrNull() ?: return true
        return expSeconds * 1000 <= nowMs + skewMs
    }

    /**
     * Minimal URL-safe Base64 decoder. Written by hand because the app's
     * minSdk is 24 (no java.util.Base64) and android.util.Base64 does not
     * exist in local unit tests.
     */
    private fun decodeUrlSafeBase64(input: String): ByteArray? {
        val clean = input.substringBefore('=')
        if (clean.length % 4 == 1) return null // one dangling 6-bit group
        val out = ByteArray(clean.length * 3 / 4)
        var buffer = 0
        var bits = 0
        var index = 0
        for (ch in clean) {
            val value = ALPHABET.indexOf(ch)
            if (value < 0) return null
            buffer = (buffer shl 6) or value
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out[index++] = (buffer shr bits).toByte()
            }
        }
        return out.copyOf(index)
    }
}
