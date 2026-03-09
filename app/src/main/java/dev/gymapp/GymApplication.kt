package dev.gymapp

import android.app.Application
import dev.gymapp.api.ApiClient
import dev.gymapp.api.GymApi
import dev.gymapp.auth.AuthRepository

class GymApplication : Application() {

    lateinit var authRepository: AuthRepository
        private set

    lateinit var api: GymApi
        private set

    override fun onCreate() {
        super.onCreate()
        authRepository = AuthRepository(this)
        api = ApiClient.create { authRepository.currentToken }
    }
}
