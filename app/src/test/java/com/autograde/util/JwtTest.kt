package com.autograde.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class JwtTest {

    private val now = 1_800_000_000_000L // fixed instant, so tests stay deterministic

    private fun jwt(payload: String): String {
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        return "eyJhbGciOiJIUzI1NiJ9.$encoded.c2ln"
    }

    @Test
    fun `a token issued for later is not expired`() {
        val exp = (now + 3_600_000) / 1000
        assertFalse(Jwt.isExpired(jwt("""{"token_type":"access","exp":$exp}"""), nowMs = now))
    }

    @Test
    fun `an expired token is detected`() {
        val exp = (now - 1_000) / 1000
        assertTrue(Jwt.isExpired(jwt("""{"token_type":"access","exp":$exp}"""), nowMs = now))
    }

    @Test
    fun `a token inside the skew window counts as expired`() {
        val exp = (now + 30_000) / 1000
        assertTrue(Jwt.isExpired(jwt("""{"token_type":"access","exp":$exp}"""), nowMs = now))
    }

    @Test
    fun `anything unparseable counts as expired so a refresh is attempted`() {
        assertTrue(Jwt.isExpired("not-a-jwt", nowMs = now))
        assertTrue(Jwt.isExpired("a.b.c", nowMs = now)) // 1-char payload group
        assertTrue(Jwt.isExpired("head.!!!not-base64!!!.sig", nowMs = now))
        assertTrue(Jwt.isExpired(jwt("""{"token_type":"access"}"""), nowMs = now)) // no exp
    }
}
