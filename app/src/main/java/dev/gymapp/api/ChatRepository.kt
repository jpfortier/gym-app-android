package dev.gymapp.api

import dev.gymapp.api.models.ChatRequest
import dev.gymapp.api.models.ChatResponse
import dev.gymapp.api.models.JobResponse
import kotlinx.coroutines.delay
import retrofit2.Response
import java.util.concurrent.TimeoutException

sealed class ChatPostResult {
    data class Sync(val response: ChatResponse) : ChatPostResult()
    data class Async(val job: JobResponse) : ChatPostResult()
}

class ChatRepository(
    private val chatApi: ChatApi,
    private val gson: com.google.gson.Gson
) {

    companion object {
        private const val POLL_INTERVAL_MS = 400L
        private const val POLL_TIMEOUT_MS = 30_000L
    }

    suspend fun postChat(request: ChatRequest): Result<ChatPostResult> {
        return runCatching {
            val response = chatApi.chat(request)
            if (!response.isSuccessful) {
                val error = ErrorBodyParser.parse(response.errorBody(), response.code())
                return Result.failure(Exception("${error.error} (${error.code})"))
            }
            val body = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
            val json = com.google.gson.JsonParser.parseString(body).asJsonObject
            if (json.has("job_id")) {
                ChatPostResult.Async(gson.fromJson(body, JobResponse::class.java))
            } else {
                ChatPostResult.Sync(gson.fromJson(body, ChatResponse::class.java))
            }
        }
    }

    suspend fun getChatJob(jobId: String): Result<JobResponse> {
        return runCatching {
            val response = chatApi.chatJob(jobId)
            if (!response.isSuccessful) {
                val error = ErrorBodyParser.parse(response.errorBody(), response.code())
                return Result.failure(Exception("${error.error} (${error.code})"))
            }
            val body = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
            gson.fromJson(body, JobResponse::class.java)
        }
    }

    suspend fun pollUntilComplete(jobId: String): Result<JobResponse> {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < POLL_TIMEOUT_MS) {
            when (val result = getChatJob(jobId)) {
                is Result.Success -> {
                    when (result.getOrThrow().status) {
                        "complete" -> return result
                        "failed" -> return Result.failure(
                            Exception(result.getOrThrow().error ?: "Job failed")
                        )
                        else -> delay(POLL_INTERVAL_MS)
                    }
                }
                is Result.Failure -> return result
            }
        }
        return Result.failure(TimeoutException("Request took too long. Try again."))
    }
}
