package com.autograde.data

import com.autograde.data.local.TokenStore
import com.autograde.data.model.AuthResponse
import com.autograde.data.model.GoogleAuth
import com.autograde.data.model.OtpRequest
import com.autograde.data.model.OtpVerify
import com.autograde.data.model.RefreshRequest
import com.autograde.network.ApiService
import com.autograde.util.ApiResult
import com.autograde.util.retrofitErrorMessage

class AuthRepository(
    private val api: ApiService,
    private val store: TokenStore,
) {
    suspend fun requestOtp(email: String): ApiResult<String> {
        return try {
            val res = api.otpRequest(OtpRequest(email.trim().lowercase()))
            if (res.isSuccessful) {
                ApiResult.Ok(res.body()?.detail ?: "OTP sent")
            } else {
                ApiResult.Err(
                    retrofitErrorMessage(res.code(), res.errorBody()?.string()),
                    res.code(),
                )
            }
        } catch (e: Exception) {
            ApiResult.Err(e.message ?: "Network error")
        }
    }

    suspend fun verifyOtp(email: String, otp: String): ApiResult<AuthResponse> {
        return try {
            val res = api.otpVerify(OtpVerify(email.trim().lowercase(), otp.trim()))
            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                store.saveTokens(body.accessToken, body.refreshToken)
                body.user?.id?.takeIf { it.isNotBlank() }?.let { store.saveUserId(it) }
                ApiResult.Ok(body)
            } else {
                ApiResult.Err(
                    retrofitErrorMessage(res.code(), res.errorBody()?.string()),
                    res.code(),
                )
            }
        } catch (e: Exception) {
            ApiResult.Err(e.message ?: "Network error")
        }
    }

    suspend fun google(idToken: String): ApiResult<AuthResponse> {
        return try {
            val res = api.google(GoogleAuth(idToken))
            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                store.saveTokens(body.accessToken, body.refreshToken)
                body.user?.id?.takeIf { it.isNotBlank() }?.let { store.saveUserId(it) }
                ApiResult.Ok(body)
            } else {
                ApiResult.Err(
                    retrofitErrorMessage(res.code(), res.errorBody()?.string()),
                    res.code(),
                )
            }
        } catch (e: Exception) {
            ApiResult.Err(e.message ?: "Network error")
        }
    }

    suspend fun logout(): ApiResult<Unit> {
        return try {
            store.refreshToken()?.let { api.blacklist(RefreshRequest(it)) }
            store.clear()
            ApiResult.Ok(Unit)
        } catch (e: Exception) {
            store.clear()
            ApiResult.Err(e.message ?: "Logged out locally")
        }
    }
}
