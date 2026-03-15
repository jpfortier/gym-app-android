package dev.gymapp.api

import dev.gymapp.api.models.ChatMessagesResponse
import dev.gymapp.api.models.ChatRequest
import dev.gymapp.api.models.ChatResponse
import dev.gymapp.api.models.DevTokenResponse
import dev.gymapp.api.models.ExerciseVariant
import dev.gymapp.api.models.HealthResponse
import dev.gymapp.api.models.Pr
import dev.gymapp.api.models.QueryResponse
import dev.gymapp.api.models.Session
import dev.gymapp.api.models.User
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class PrImageLoaderTest {

    @Test
    fun loadPrImage_304_returnsCachedBytes() = runBlocking {
        val imageBytes = "fake-image-bytes".toByteArray()
        val etag = "\"abc123\""

        val api = object : GymApi {
            override suspend fun health() = Response.success(HealthResponse("ok"))
            override suspend fun devToken(email: String?) = Response.success(DevTokenResponse("dev:test"))
            override suspend fun me() = Response.success(User("u1", "t@t.com", "Test"))
            override suspend fun chatMessages(limit: Int?, before: String?) =
                Response.success(ChatMessagesResponse(emptyList()))
            override suspend fun chat(request: ChatRequest) = Response.success(ChatResponse("log", "ok"))
            override suspend fun sessions(limit: Int?) = Response.success(emptyList())
            override suspend fun session(id: String) = Response.error(404, "".toResponseBody(null))
            override suspend fun query(
                category: String?,
                exercise: String?,
                variant: String?,
                limit: Int?,
                from: String?,
                to: String?
            ) = Response.success(QueryResponse("", "", emptyList()))
            override suspend fun exercises() = Response.success(emptyList())
            override suspend fun prs() = Response.success(emptyList())

            override suspend fun prImage(id: String, ifNoneMatch: String?): Response<okhttp3.ResponseBody> {
                return when {
                    ifNoneMatch == etag -> Response.error(304, "".toResponseBody(null))
                    else -> Response.error(404, "".toResponseBody(null))
                }
            }
        }

        val loader = PrImageLoader(api)
        loader.cache["pr-1"] = PrImageLoader.CacheEntry(etag, imageBytes)

        val result = loader.loadPrImage("pr-1")
        assertTrue(result.isSuccess)
        assertArrayEquals(imageBytes, result.getOrNull())
    }
}
