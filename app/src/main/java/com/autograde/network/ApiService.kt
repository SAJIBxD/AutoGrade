package com.autograde.network

import com.autograde.data.model.AuthResponse
import com.autograde.data.model.GoogleAuth
import com.autograde.data.model.ImageUploadResponse
import com.autograde.data.model.OtpRequest
import com.autograde.data.model.OtpRequestResponse
import com.autograde.data.model.OtpVerify
import com.autograde.data.model.Profile
import com.autograde.data.model.ProfilePatch
import com.autograde.data.model.RefreshRequest
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiService {
    // --- authentication (no auth header) ---
    @POST("api/authentication/otp/request/")
    suspend fun otpRequest(@Body body: OtpRequest): Response<OtpRequestResponse>

    @POST("api/authentication/otp/verify/")
    suspend fun otpVerify(@Body body: OtpVerify): Response<AuthResponse>

    @POST("api/authentication/google/")
    suspend fun google(@Body body: GoogleAuth): Response<AuthResponse>

    @POST("api/authentication/token/refresh/")
    suspend fun refresh(@Body body: RefreshRequest): Response<AuthResponse>

    @POST("api/authentication/token/blacklist/")
    suspend fun blacklist(@Body body: RefreshRequest): Response<Unit>

    // --- authorization (Bearer required) ---
    @GET("api/authorization/user-profile/{id}/")
    suspend fun profile(@Path("id") id: String): Response<Profile>

    @PATCH("api/authorization/user-profile/{id}/")
    suspend fun updateProfile(
        @Path("id") id: String,
        @Body body: ProfilePatch,
    ): Response<Profile>

    // --- filesystem (Bearer + multipart) ---
    @Multipart
    @POST("api/file/image/")
    suspend fun uploadImage(@Part image: MultipartBody.Part): Response<ImageUploadResponse>

    @DELETE("api/file/image/{id}/")
    suspend fun deleteImage(@Path("id") id: Int): Response<Unit>
}
