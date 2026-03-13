package dev.gymapp.api

import dev.gymapp.api.models.ChatRequest
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class ChatRepositoryTest {

    private val gson = com.google.gson.Gson()

    @Test
    fun postChat_syncResponse_returnsSyncResult() = runBlocking {
        val syncJson = """
            {"intent":"log","message":"Logged.","entries":[],"prs":[]}
        """.trimIndent()
        val chatApi = object : ChatApi {
            override suspend fun chat(request: ChatRequest): Response<okhttp3.ResponseBody> =
                Response.success(syncJson.toByteArray().toResponseBody("application/json".toMediaType()))

            override suspend fun chatJob(jobId: String): Response<okhttp3.ResponseBody> =
                Response.error(404, ByteArray(0).toResponseBody(null))
        }
        val repo = ChatRepository(chatApi, gson)

        val result = repo.postChat(ChatRequest(audioBase64 = "abc", audioFormat = "m4a"))

        assertTrue(result.isSuccess)
        val postResult = result.getOrThrow()
        assertTrue(postResult is ChatPostResult.Sync)
        assertEquals("Logged.", (postResult as ChatPostResult.Sync).response.message)
    }

    @Test
    fun postChat_asyncResponse_returnsAsyncResult() = runBlocking {
        val asyncJson = """
            {"job_id":"job-123","text":"bench 135x8","status":"processing","result":null,"error":null}
        """.trimIndent()
        val chatApi = object : ChatApi {
            override suspend fun chat(request: ChatRequest): Response<okhttp3.ResponseBody> =
                Response.success(asyncJson.toByteArray().toResponseBody("application/json".toMediaType()))

            override suspend fun chatJob(jobId: String): Response<okhttp3.ResponseBody> =
                Response.success("""
                    {"job_id":"$jobId","text":"bench 135x8","status":"complete","result":{"intent":"log","message":"Done.","entries":[],"prs":[]},"error":null}
                """.trimIndent().toByteArray().toResponseBody("application/json".toMediaType()))
        }
        val repo = ChatRepository(chatApi, gson)

        val result = repo.postChat(ChatRequest(audioBase64 = "abc", audioFormat = "m4a"))

        assertTrue(result.isSuccess)
        val postResult = result.getOrThrow()
        assertTrue(postResult is ChatPostResult.Async)
        assertEquals("job-123", (postResult as ChatPostResult.Async).job.jobId)
        assertEquals("bench 135x8", postResult.job.text)
    }

    @Test
    fun postChat_errorResponse_returnsFailure() = runBlocking {
        val chatApi = object : ChatApi {
            override suspend fun chat(request: ChatRequest): Response<okhttp3.ResponseBody> =
                Response.error(500, """{"error":"Server error","code":"500","error_token":null}""".toByteArray().toResponseBody("application/json".toMediaType()))

            override suspend fun chatJob(jobId: String): Response<okhttp3.ResponseBody> =
                Response.error(404, ByteArray(0).toResponseBody(null))
        }
        val repo = ChatRepository(chatApi, gson)

        val result = repo.postChat(ChatRequest(audioBase64 = "abc", audioFormat = "m4a"))

        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull()?.message)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("Server error"))
    }

    @Test
    fun pollUntilComplete_completeStatus_returnsJob() = runBlocking {
        val completeJson = """
            {"job_id":"j1","text":"ok","status":"complete","result":{"intent":"log","message":"Done.","entries":[],"prs":[]},"error":null}
        """.trimIndent()
        val chatApi = object : ChatApi {
            override suspend fun chat(request: ChatRequest): Response<okhttp3.ResponseBody> =
                Response.error(404, ByteArray(0).toResponseBody(null))

            override suspend fun chatJob(jobId: String): Response<okhttp3.ResponseBody> =
                Response.success(completeJson.toByteArray().toResponseBody("application/json".toMediaType()))
        }
        val repo = ChatRepository(chatApi, gson)

        val result = repo.pollUntilComplete("j1")

        assertTrue(result.isSuccess)
        assertEquals("complete", result.getOrThrow().status)
        assertEquals("Done.", result.getOrThrow().result?.message)
    }

    @Test
    fun pollUntilComplete_failedStatus_returnsFailure() = runBlocking {
        val failedJson = """
            {"job_id":"j1","text":"","status":"failed","result":null,"error":"Job failed"}
        """.trimIndent()
        val chatApi = object : ChatApi {
            override suspend fun chat(request: ChatRequest): Response<okhttp3.ResponseBody> =
                Response.error(404, ByteArray(0).toResponseBody(null))

            override suspend fun chatJob(jobId: String): Response<okhttp3.ResponseBody> =
                Response.success(failedJson.toByteArray().toResponseBody("application/json".toMediaType()))
        }
        val repo = ChatRepository(chatApi, gson)

        val result = repo.pollUntilComplete("j1")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("Job failed"))
    }
}
