package dev.gymapp.api

import dev.gymapp.api.models.ChatRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ChatApi {

    @POST("chat")
    suspend fun chat(@Body request: ChatRequest): Response<ResponseBody>

    @GET("chat/jobs/{id}")
    suspend fun chatJob(@Path("id") jobId: String): Response<ResponseBody>
}
