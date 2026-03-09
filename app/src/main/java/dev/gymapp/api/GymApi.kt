package dev.gymapp.api

import dev.gymapp.api.models.ChatRequest
import dev.gymapp.api.models.ChatResponse
import dev.gymapp.api.models.HealthResponse
import dev.gymapp.api.models.Session
import dev.gymapp.api.models.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GymApi {

    @GET("health")
    suspend fun health(): Response<HealthResponse>

    @GET("me")
    suspend fun me(): Response<User>

    @POST("chat")
    suspend fun chat(@Body request: ChatRequest): Response<ChatResponse>

    @GET("sessions")
    suspend fun sessions(@Query("limit") limit: Int? = null): Response<List<Session>>

    @GET("sessions/{id}")
    suspend fun session(@Path("id") id: String): Response<Session>
}
