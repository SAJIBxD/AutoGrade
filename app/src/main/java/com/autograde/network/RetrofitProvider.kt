package com.autograde.network

import com.autograde.BuildConfig
import com.autograde.data.local.TokenStore
import com.autograde.data.model.RefreshRequest
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private fun baseUrl(): String {
    val raw = BuildConfig.BASE_URL.trim()
    return if (raw.endsWith("/")) raw else "$raw/"
}

private class AuthInterceptor(private val refresher: TokenRefresher) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // Refresh proactively when expired: a burst of parallel uploads then
        // goes out with one already-fresh token instead of racing on 401s.
        val token = runBlocking { refresher.usableAccessToken() }
        val req = if (token != null) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(req)
    }
}

private class RefreshAuthenticator(
    private val refresher: TokenRefresher,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null
        // Don't loop on the refresh endpoint itself.
        if (response.request.url.encodedPath.contains("token/refresh")) return null
        val rejected = response.request.header("Authorization")
            ?.removePrefix("Bearer ")?.trim()
        val fresh = runBlocking { refresher.refreshAfterRejection(rejected) } ?: return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $fresh")
            .build()
    }

    private fun responseCount(r: Response): Int {
        var c = 1
        var p = r.priorResponse
        while (p != null) {
            c++
            p = p.priorResponse
        }
        return c
    }
}

object RetrofitProvider {
    fun create(store: TokenStore): ApiService {
        // Headerless client for the refresh call — no auth interceptor and no
        // authenticator, so a refresh can never recurse into another refresh.
        val plain = Retrofit.Builder()
            .baseUrl(baseUrl())
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        val refresher = TokenRefresher(
            storedAccess = { store.accessToken() },
            storedRefresh = { store.refreshToken() },
            persist = { access, refresh -> store.saveTokens(access, refresh) },
            clearSession = { store.clear() },
            remoteRefresh = { refresh -> plain.refresh(RefreshRequest(refresh)) },
        )

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(refresher))
            .authenticator(RefreshAuthenticator(refresher))
            .addInterceptor(logging)
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl())
            .client(client)
            .addConverterFactory(
                GsonConverterFactory.create(GsonBuilder().create()),
            )
            .build()
            .create(ApiService::class.java)
    }
}
