package dev.gymapp.api

import dev.gymapp.api.models.ChatMessagesResponse
import dev.gymapp.api.models.DevTokenResponse
import dev.gymapp.api.models.ExerciseVariant
import dev.gymapp.api.models.HealthResponse
import dev.gymapp.api.models.Pr
import dev.gymapp.api.models.QueryResponse
import dev.gymapp.api.models.Session
import dev.gymapp.api.models.User
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GymApi {

    @GET("health")
    suspend fun health(): Response<HealthResponse>

    @GET("dev/token")
    suspend fun devToken(@Query("email") email: String? = null): Response<DevTokenResponse>

    @GET("me")
    suspend fun me(): Response<User>

    @GET("chat/messages")
    suspend fun chatMessages(
        @Query("limit") limit: Int? = null,
        @Query("before") before: String? = null
    ): Response<ChatMessagesResponse>

    @GET("sessions")
    suspend fun sessions(@Query("limit") limit: Int? = null): Response<List<Session>>

    @GET("sessions/{id}")
    suspend fun session(@Path("id") id: String): Response<Session>

    @GET("query")
    suspend fun query(
        @Query("category") category: String? = null,
        @Query("exercise") exercise: String? = null,
        @Query("variant") variant: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<QueryResponse>

    @GET("exercises")
    suspend fun exercises(): Response<List<ExerciseVariant>>

    @GET("prs")
    suspend fun prs(): Response<List<Pr>>

    @GET("prs/{id}/image")
    suspend fun prImage(@Path("id") id: String): Response<ResponseBody>
}
