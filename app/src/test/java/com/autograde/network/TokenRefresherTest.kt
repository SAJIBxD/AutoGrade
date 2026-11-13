package com.autograde.network

import com.autograde.data.model.AuthResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.util.Base64

/**
 * The backend rotates refresh tokens and blacklists the old one, and access
 * tokens only live 6 minutes — these tests pin down the behaviour that kept
 * breaking the answer-sheet upload.
 */
class TokenRefresherTest {

    /** Stands in for the DataStore-backed TokenStore. */
    private class Store(var access: String?, var refresh: String?) {
        val persists = mutableListOf<Pair<String, String>>()
        var clears = 0
    }

    private fun refresher(
        store: Store,
        remote: suspend (String) -> Response<AuthResponse>,
    ) = TokenRefresher(
        storedAccess = { store.access },
        storedRefresh = { store.refresh },
        persist = { access, refresh ->
            store.access = access
            store.refresh = refresh
            store.persists += access to refresh
        },
        clearSession = { store.clears++ },
        remoteRefresh = remote,
    )

    private fun jwt(expAtMillis: Long): String {
        val payload = """{"token_type":"access","exp":${expAtMillis / 1000}}"""
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        return "eyJhbGciOiJIUzI1NiJ9.$encoded.c2ln"
    }

    private fun success(access: String, refresh: String) =
        Response.success(AuthResponse(refreshToken = refresh, accessToken = access, user = null))

    private fun unauthorized(detail: String) = Response.error<AuthResponse>(
        401,
        """{"detail":"$detail","code":"token_not_valid"}"""
            .toResponseBody("application/json".toMediaType()),
    )

    /**
     * Regression: AuthResponse declares refreshToken FIRST, so the old
     * `val (accessToken, refreshToken) = body` stored the pair swapped, after
     * which every request sent a refresh token as a bearer token and the
     * backend answered "Given token not valid for any token type".
     */
    @Test
    fun `rotated tokens are stored in the right slots`() = runBlocking {
        val store = Store(access = jwt(0), refresh = "OLD_REFRESH")

        val retried = refresher(store) { success("NEW_ACCESS", "NEW_REFRESH") }
            .refreshAfterRejection(store.access)

        assertEquals("NEW_ACCESS", retried)
        assertEquals(listOf("NEW_ACCESS" to "NEW_REFRESH"), store.persists)
    }

    @Test
    fun `a burst of parallel uploads refreshes exactly once`() = runBlocking {
        val store = Store(access = jwt(0), refresh = "REFRESH")
        val freshAccess = jwt(System.currentTimeMillis() + 3_600_000)
        var calls = 0
        val refresher = refresher(store) {
            calls++
            delay(50) // hold the lock long enough for every caller to queue up
            success(freshAccess, "R2")
        }

        val tokens = (1..16).map {
            async(Dispatchers.Default) { refresher.usableAccessToken() }
        }.awaitAll()

        // The losers must reuse the winner's refresh instead of rotating again:
        // a second rotation would be answered with "Token is blacklisted".
        assertEquals(1, calls)
        assertEquals(listOf(freshAccess), tokens.distinct())
    }

    @Test
    fun `a still-valid token is used without touching the server`() = runBlocking {
        val store = Store(access = jwt(System.currentTimeMillis() + 3_600_000), refresh = "R")
        var calls = 0
        val refresher = refresher(store) {
            calls++
            success("SHOULD_NOT_HAPPEN", "SHOULD_NOT_HAPPEN")
        }

        assertEquals(store.access, refresher.usableAccessToken())
        assertEquals(0, calls)
    }

    @Test
    fun `a retry picks up the token another coroutine already stored`() = runBlocking {
        val store = Store(access = jwt(System.currentTimeMillis() + 3_600_000), refresh = "R")
        var calls = 0
        val refresher = refresher(store) {
            calls++
            success("SHOULD_NOT_HAPPEN", "SHOULD_NOT_HAPPEN")
        }

        assertEquals(store.access, refresher.refreshAfterRejection("SOME_OLDER_TOKEN"))
        assertEquals(0, calls)
    }

    @Test
    fun `a blacklisted refresh token ends the session instead of looping`() = runBlocking {
        val store = Store(access = jwt(0), refresh = "BLACKLISTED")

        val retried = refresher(store) { unauthorized("Token is blacklisted") }
            .refreshAfterRejection(store.access)

        assertNull(retried) // authenticator gives up -> the original 401 surfaces
        assertEquals(1, store.clears)
    }

    @Test
    fun `a network failure during refresh keeps the session`() = runBlocking {
        val store = Store(access = jwt(0), refresh = "STILL_GOOD")

        val retried = refresher(store) { throw IOException("offline") }
            .refreshAfterRejection(store.access)

        assertNull(retried)
        assertEquals(0, store.clears) // no proof the token is dead
        assertEquals("STILL_GOOD", store.refresh)
    }
}
