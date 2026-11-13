package com.autograde.network

import com.autograde.data.model.AuthResponse
import com.autograde.util.Jwt
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response

/**
 * The single place that turns stored (possibly expired) tokens into a usable
 * access token.
 *
 * The backend runs with a 6-minute access-token lifetime plus
 * `ROTATE_REFRESH_TOKENS` and `BLACKLIST_AFTER_ROTATION`, which has two traps:
 *
 *  - a burst of parallel uploads must not fire several refreshes with the same
 *    refresh token — the losers would be answered with `Token is blacklisted`;
 *  - the rotated pair has to be stored in the *right* slots: the access token
 *    as access, the refresh token as refresh. Sending a refresh token as a
 *    bearer token gets `Given token not valid for any token type`, and using an
 *    access token as the refresh token gets `Token has wrong type` — i.e. the
 *    session locks itself out.
 *
 * So every refresh passes through one mutex, and callers re-read the store
 * inside the lock so they reuse the winner's tokens instead of rotating again.
 */
class TokenRefresher(
    private val storedAccess: suspend () -> String?,
    private val storedRefresh: suspend () -> String?,
    private val persist: suspend (accessToken: String, refreshToken: String) -> Unit,
    private val clearSession: suspend () -> Unit,
    private val remoteRefresh: suspend (refreshToken: String) -> Response<AuthResponse>,
) {
    private val mutex = Mutex()

    /**
     * Access token to attach to the next request, refreshing first when the
     * stored one is missing or expired.
     */
    suspend fun usableAccessToken(): String? {
        val current = storedAccess()
        if (current != null && !Jwt.isExpired(current)) return current
        return mutex.withLock {
            val again = storedAccess()
            if (again != null && !Jwt.isExpired(again)) {
                again // someone else refreshed while we waited for the lock
            } else {
                // Fall back to whatever is stored when the refresh fails, so the
                // server still answers with a recognisable token 401 instead of
                // a bare "no credentials provided".
                refresh() ?: again ?: current
            }
        }
    }

    /**
     * Called after the server rejected [rejectedToken] with a 401. Returns the
     * token to retry with, or null when nothing usable is left — the caller
     * should then surface the original 401 response.
     */
    suspend fun refreshAfterRejection(rejectedToken: String?): String? = mutex.withLock {
        val stored = storedAccess()
        if (stored != null && stored != rejectedToken && !Jwt.isExpired(stored)) {
            stored // already rotated since the rejected request went out
        } else {
            refresh()
        }
    }

    /** Performs the actual refresh. Must be called with [mutex] held. */
    private suspend fun refresh(): String? {
        val refreshToken = storedRefresh() ?: return null
        val response = try {
            remoteRefresh(refreshToken)
        } catch (e: Exception) {
            // Network problem — the refresh token itself may still be fine,
            // so keep the session.
            return null
        }
        val body = response.body()
        if (response.isSuccessful && body != null) {
            // Named fields, not destructuring: AuthResponse declares refreshToken
            // first, so `val (a, b) = body` would swap the two slots.
            persist(body.accessToken, body.refreshToken)
            return body.accessToken
        }
        // 4xx means the server will never accept this refresh token again
        // (rotated, blacklisted or expired): the session is over.
        if (response.code() in 400..499) clearSession()
        return null
    }
}
