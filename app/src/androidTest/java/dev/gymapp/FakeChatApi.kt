package dev.gymapp

import dev.gymapp.api.ChatApi
import dev.gymapp.api.models.ChatRequest
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Response

class FakeChatApi(private val errorMessage: String = "Test error") : ChatApi {

    override suspend fun chat(request: ChatRequest): Response<okhttp3.ResponseBody> {
        delay(50)
        val body = """{"error":"$errorMessage","code":"500","error_token":null}"""
        return Response.error(
            500,
            okhttp3.ResponseBody.create("application/json".toMediaType(), body)
        )
    }

    override suspend fun chatJob(jobId: String): Response<okhttp3.ResponseBody> {
        return Response.error(404, okhttp3.ResponseBody.create(null, ByteArray(0)))
    }
}
