package dev.gymapp

import dev.gymapp.api.GymApi
import dev.gymapp.api.models.ApiError
import dev.gymapp.api.models.ChatMessagesResponse
import dev.gymapp.api.models.DevTokenResponse
import dev.gymapp.api.models.ExerciseVariant
import dev.gymapp.api.models.HealthResponse
import dev.gymapp.api.models.Pr
import dev.gymapp.api.models.QueryResponse
import dev.gymapp.api.models.Session
import dev.gymapp.api.models.User
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

/**
 * Fake GymApi for tests. chat() returns error; other methods return empty/success.
 */
class FakeGymApi(private val chatError: String = "Test error") : GymApi {

    override suspend fun health(): Response<HealthResponse> {
        delay(10)
        return Response.success(HealthResponse(status = "ok"))
    }

    override suspend fun devToken(email: String?): Response<DevTokenResponse> =
        Response.success(DevTokenResponse(token = "dev:test@test.com"))

    override suspend fun me(): Response<User> =
        Response.success(User(id = "u1", email = "test@test.com", name = "Test"))

    override suspend fun chatMessages(limit: Int?, before: String?): Response<ChatMessagesResponse> {
        delay(10)
        return Response.success(ChatMessagesResponse(messages = emptyList()))
    }

    override suspend fun sessions(limit: Int?): Response<List<Session>> =
        Response.success(emptyList())

    override suspend fun session(id: String): Response<Session> =
        Response.error(404, "".toResponseBody(null))

    override suspend fun query(
        category: String?,
        exercise: String?,
        variant: String?,
        limit: Int?,
        from: String?,
        to: String?
    ): Response<QueryResponse> = Response.success(
        QueryResponse(exerciseName = "", variantName = "", entries = emptyList())
    )

    override suspend fun exercises(): Response<List<ExerciseVariant>> =
        Response.success(emptyList())

    override suspend fun prs(): Response<List<Pr>> = Response.success(emptyList())

    override suspend fun prImage(id: String, ifNoneMatch: String?): Response<okhttp3.ResponseBody> =
        Response.error(404, "".toResponseBody(null))
}
