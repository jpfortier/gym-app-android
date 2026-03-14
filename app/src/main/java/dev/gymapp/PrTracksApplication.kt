package dev.gymapp

import android.app.Application
import dev.gymapp.api.ApiClient
import dev.gymapp.api.ApiServices
import dev.gymapp.api.ChatRepository
import dev.gymapp.api.GymApi
import dev.gymapp.auth.AuthRepository

class PrTracksApplication : Application() {

    lateinit var authRepository: AuthRepository
        private set

    lateinit var apiServices: ApiServices
        private set

    val api: GymApi
        get() = apiServices.api

    lateinit var chatRepository: ChatRepository
        private set

    override fun onCreate() {
        super.onCreate()
        authRepository = AuthRepository(this)
        apiServices = ApiClient.create(
            tokenProvider = { authRepository.currentToken },
            refreshToken = { authRepository.refreshToken() },
            onAuthFailure = { authRepository.signOutDueToAuthFailure(it) }
        )
        chatRepository = ChatRepository(apiServices.chatApi, apiServices.gson)
    }
}
