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

private class AuthInterceptor(private val store: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val token = runBlocking { store.accessToken() }
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
    private val store: TokenStore,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null
        // Don't loop on the refresh endpoint itself.
        if (response.request.url.encodedPath.contains("token/refresh")) return null
        val refresh = runBlocking { store.refreshToken() } ?: return null
        val plain = plainRetrofit()
        val res = runBlocking {
            try {
                plain.refresh(RefreshRequest(refresh))
            } catch (_: Exception) {
                null
            }
        } ?: return null
        if (!res.isSuccessful) return null
        val body = res.body() ?: return null
        runBlocking { store.saveTokens(body.accessToken, body.refreshToken) }
        return response.request.newBuilder()
            .header("Authorization", "Bearer ${body.accessToken}")
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

    private fun plainRetrofit(): ApiService {
        val client = OkHttpClient.Builder().build()
        return Retrofit.Builder()
            .baseUrl(baseUrl())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

object RetrofitProvider {
    fun create(store: TokenStore): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(store))
            .authenticator(RefreshAuthenticator(store))
            .addInterceptor(logging)
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl())
            .client(client)
            .addConverterFactory(
                GsonConverterFactory.create(GsonBuilder().setLenient().create()),
            )
            .build()
            .create(ApiService::class.java)
    }
}
