package com.autograde.data.model

import com.google.gson.annotations.SerializedName

// POST api/authentication/otp/request/ {email}
data class OtpRequest(val email: String)
data class OtpRequestResponse(
    val detail: String?,
    @SerializedName("cooldown_seconds") val cooldownSeconds: Int? = null,
    @SerializedName("retry_after_seconds") val retryAfterSeconds: Int? = null,
)

// POST api/authentication/otp/verify/ {email, otp}
// POST api/authentication/google/ {id_token}
data class OtpVerify(val email: String, val otp: String)
data class GoogleAuth(val id_token: String)

data class AuthUser(
    val id: String?,
    val email: String?,
    val phone: String?,
    @SerializedName("user_type") val userType: String?,
)

data class AuthResponse(
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("isNewUser") val isNewUser: Boolean = false,
    val user: AuthUser?,
)

// POST api/authentication/token/refresh/ — backend input key is still "refresh"
data class RefreshRequest(val refresh: String)

// GET/PATCH api/authorization/user-profile/{id}/
data class Profile(
    val id: String?,
    @SerializedName("first_name") val firstName: String?,
    @SerializedName("last_name") val lastName: String?,
    val email: String?,
    val phone: String?,
    @SerializedName("date_of_birth") val dateOfBirth: String?,
    val gender: String?,
    @SerializedName("user_type") val userType: String?,
    val verified: Boolean?,
    @SerializedName("is_active") val isActive: Boolean?,
    @SerializedName("image_url") val imageUrl: String?,
)

data class ProfilePatch(
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    val phone: String? = null,
    @SerializedName("date_of_birth") val dateOfBirth: String? = null,
    val gender: String? = null,
    /** ImageModel id from api/file/image/ upload — links the photo to the profile. */
    val image: Int? = null,
)

// api/file/image/ + api/file/file/
data class ImageUploadResponse(
    val id: Int?,
    @SerializedName("image_path") val imagePath: String?,
)
