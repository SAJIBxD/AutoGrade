package com.autograde

import com.autograde.data.FileRepository
import com.autograde.data.model.FileUploadResponse
import com.autograde.network.ApiService
import com.autograde.util.ApiResult
import com.autograde.util.retrofitErrorMessage
import kotlinx.coroutines.runBlocking
import java.io.RandomAccessFile
import okhttp3.MultipartBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Response

/**
 * Tests for the upload path — the one piece of the new flow that has to work
 * against the real backend.
 */
class FileRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** Captures what would be sent so assertions can inspect the part. */
    private open class CapturingApi(
        private val response: Response<FileUploadResponse>,
    ) : ApiService {
        var lastPart: MultipartBody.Part? = null

        override suspend fun uploadFile(file: MultipartBody.Part): Response<FileUploadResponse> {
            lastPart = file
            return response
        }

        // --- everything else is unused by FileRepository ---
        override suspend fun otpRequest(body: com.autograde.data.model.OtpRequest) = error("unused")
        override suspend fun otpVerify(body: com.autograde.data.model.OtpVerify) = error("unused")
        override suspend fun google(body: com.autograde.data.model.GoogleAuth) = error("unused")
        override suspend fun refresh(body: com.autograde.data.model.RefreshRequest) = error("unused")
        override suspend fun blacklist(body: com.autograde.data.model.RefreshRequest) = error("unused")
        override suspend fun profile(id: String) = error("unused")
        override suspend fun updateProfile(id: String, body: com.autograde.data.model.ProfilePatch) = error("unused")
        override suspend fun uploadImage(image: MultipartBody.Part) = error("unused")
        override suspend fun deleteImage(id: Int) = error("unused")
        override suspend fun deleteFile(id: Int) = error("unused")
    }

    private fun file(name: String, content: ByteArray = ByteArray(64) { 1 }) =
        tmp.newFile(name).apply { writeBytes(content) }

    @Test
    fun `uploads use the part name the backend reads`() = runBlocking {
        val api = CapturingApi(
            Response.success(FileUploadResponse(id = 7, filePath = "/media/files/x.pdf")),
        )
        val result = FileRepository(api).upload(file("paper.pdf"))

        assertTrue("expected success, got $result", result is ApiResult.Ok)
        assertEquals(7, (result as ApiResult.Ok).value.id)
        assertEquals("/media/files/x.pdf", result.value.filePath)
        // Django reads request.FILES["file"], so the form name must be exactly "file".
        val disposition = api.lastPart?.headers?.get("Content-Disposition") ?: ""
        assertEquals(
            "file",
            Regex("name=\"([^\"]+)\"").find(disposition)?.groupValues?.get(1),
        )
    }

    @Test
    fun `rejects an empty file without calling the server`() = runBlocking {
        val api = CapturingApi(Response.success(FileUploadResponse(1, "/x")))
        val result = FileRepository(api).upload(file("empty.pdf", ByteArray(0)))

        assertTrue(result is ApiResult.Err)
        assertEquals(null, api.lastPart)
    }

    @Test
    fun `rejects files over the 20MB backend limit`() = runBlocking {
        val api = CapturingApi(Response.success(FileUploadResponse(1, "/x")))
        val big = tmp.newFile("big.pdf")
        RandomAccessFile(big, "rw").use { it.setLength(FileRepository.MAX_FILE_BYTES + 1) }
        val result = FileRepository(api).upload(big)

        assertTrue(result is ApiResult.Err)
        assertTrue((result as ApiResult.Err).message.contains("20MB"))
        assertEquals(null, api.lastPart)
    }

    @Test
    fun `surfaces the backend error body on failure`() = runBlocking {
        val api = CapturingApi(
            Response.error<FileUploadResponse>(
                400,
                """{"error":"File too large. Max 20MB."}""".toResponseBody(),
            ),
        )
        val result = FileRepository(api).upload(file("paper.pdf"))

        assertTrue(result is ApiResult.Err)
        assertEquals("File too large. Max 20MB.", (result as ApiResult.Err).message)
        assertEquals(400, result.code)
    }

    @Test
    fun `network failure becomes an error, not a crash`() = runBlocking {
        val api = object : CapturingApi(Response.success(FileUploadResponse(1, "/x"))) {
            override suspend fun uploadFile(file: MultipartBody.Part): Response<FileUploadResponse> =
                throw java.io.IOException("Unable to resolve host")
        }
        val result = FileRepository(api).upload(file("paper.pdf"))

        assertTrue(result is ApiResult.Err)
        assertTrue((result as ApiResult.Err).message.contains("Unable to resolve host"))
    }
}

class ErrorMessageTest {

    @Test
    fun `parses DRF detail`() {
        assertEquals(
            "Authentication credentials were not provided.",
            retrofitErrorMessage(401, """{"detail":"Authentication credentials were not provided."}"""),
        )
    }

    @Test
    fun `parses filesystem error key`() {
        assertEquals(
            "No file provided.",
            retrofitErrorMessage(400, """{"error":"No file provided."}"""),
        )
    }

    @Test
    fun `an invalid or expired token reads as a dead session`() {
        assertEquals(
            "Session expired. Please log in again.",
            retrofitErrorMessage(
                401,
                """{"detail":"Given token not valid for any token type","messages":[]}""",
            ),
        )
        assertEquals(
            "Session expired. Please log in again.",
            retrofitErrorMessage(401, """{"detail":"Token is blacklisted","code":"token_not_valid"}"""),
        )
    }

    @Test
    fun `falls back to the raw body when nothing matches`() {
        assertTrue(retrofitErrorMessage(500, "<html>boom</html>").startsWith("HTTP 500"))
    }

    @Test
    fun `blank body still yields a code`() {
        assertEquals("HTTP 404", retrofitErrorMessage(404, null))
    }
}
