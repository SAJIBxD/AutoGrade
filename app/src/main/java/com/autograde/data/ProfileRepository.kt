package com.autograde.data

import com.autograde.data.model.ImageUploadResponse
import com.autograde.data.model.Profile
import com.autograde.data.model.ProfilePatch
import com.autograde.network.ApiService
import com.autograde.util.ApiResult
import com.autograde.util.retrofitErrorMessage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ProfileRepository(private val api: ApiService) {
    suspend fun load(id: String): ApiResult<Profile> {
        return try {
            val res = api.profile(id)
            if (res.isSuccessful && res.body() != null) {
                ApiResult.Ok(res.body()!!)
            } else {
                ApiResult.Err(retrofitErrorMessage(res.code(), res.errorBody()?.string()), res.code())
            }
        } catch (e: Exception) {
            ApiResult.Err(e.message ?: "Network error")
        }
    }

    suspend fun update(id: String, patch: ProfilePatch): ApiResult<Profile> {
        return try {
            val res = api.updateProfile(id, patch)
            if (res.isSuccessful && res.body() != null) {
                ApiResult.Ok(res.body()!!)
            } else {
                ApiResult.Err(retrofitErrorMessage(res.code(), res.errorBody()?.string()), res.code())
            }
        } catch (e: Exception) {
            ApiResult.Err(e.message ?: "Network error")
        }
    }

    suspend fun uploadImage(file: File): ApiResult<ImageUploadResponse?> {
        return try {
            val part = MultipartBody.Part.createFormData(
                "image",
                file.name,
                file.asRequestBody("image/*".toMediaTypeOrNull()),
            )
            val res = api.uploadImage(part)
            if (res.isSuccessful) {
                ApiResult.Ok(res.body())
            } else {
                ApiResult.Err(retrofitErrorMessage(res.code(), res.errorBody()?.string()), res.code())
            }
        } catch (e: Exception) {
            ApiResult.Err(e.message ?: "Upload failed")
        }
    }
}
