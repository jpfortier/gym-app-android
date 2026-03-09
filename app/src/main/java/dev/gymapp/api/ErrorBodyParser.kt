package dev.gymapp.api

import dev.gymapp.api.models.ApiError
import com.google.gson.Gson
import okhttp3.ResponseBody

object ErrorBodyParser {
    private val gson = Gson()

    fun parse(responseBody: ResponseBody?, httpCode: Int): ApiError {
        return try {
            responseBody?.string()?.let { body ->
                gson.fromJson(body, ApiError::class.java)
            } ?: ApiError(
                error = "HTTP $httpCode",
                code = httpCode.toString(),
                errorToken = null
            )
        } catch (_: Exception) {
            ApiError(
                error = "HTTP $httpCode",
                code = httpCode.toString(),
                errorToken = null
            )
        }
    }
}
