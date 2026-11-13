package com.autograde.data

import com.autograde.data.model.FileUploadResponse
import com.autograde.network.ApiService
import com.autograde.util.ApiResult
import com.autograde.util.retrofitErrorMessage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.net.URLConnection

/**
 * Uploads arbitrary documents to `POST api/file/file/`.
 *
 * The backend accepts a single multipart part named `file`, capped at 20MB,
 * and rejects anything it cannot store. Auth is attached by the OkHttp
 * interceptor in [RetrofitProvider], so the caller only supplies the bytes.
 */
class FileRepository(private val api: ApiService) {

    suspend fun upload(file: File): ApiResult<FileUploadResponse> {
        if (!file.exists() || file.length() == 0L) {
            return ApiResult.Err("That file is empty or could not be read.")
        }
        if (file.length() > MAX_FILE_BYTES) {
            return ApiResult.Err("File too large. Max 20MB.")
        }
        return try {
            val part = MultipartBody.Part.createFormData(
                "file",
                file.name,
                file.asRequestBody(mimeTypeOf(file).toMediaTypeOrNull()),
            )
            val res = api.uploadFile(part)
            when {
                res.isSuccessful && res.body() != null -> ApiResult.Ok(res.body()!!)
                else -> ApiResult.Err(
                    retrofitErrorMessage(res.code(), res.errorBody()?.string()),
                    res.code(),
                )
            }
        } catch (e: Exception) {
            ApiResult.Err(e.message ?: "Upload failed")
        }
    }

    suspend fun delete(id: Int): ApiResult<Unit> {
        return try {
            val res = api.deleteFile(id)
            if (res.isSuccessful) ApiResult.Ok(Unit)
            else ApiResult.Err(retrofitErrorMessage(res.code(), res.errorBody()?.string()), res.code())
        } catch (e: Exception) {
            ApiResult.Err(e.message ?: "Delete failed")
        }
    }

    /** Guess a MIME type the server will accept; fall back to octet-stream. */
    private fun mimeTypeOf(file: File): String {
        val fromName = URLConnection.guessContentTypeFromName(file.name)
        if (fromName != null) return fromName
        return when (file.extension.lowercase()) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "txt" -> "text/plain"
            else -> "application/octet-stream"
        }
    }

    companion object {
        const val MAX_FILE_BYTES = 20L * 1024 * 1024
    }
}
