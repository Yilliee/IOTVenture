package dev.yilliee.iotventure.di

import android.content.Context
import dev.yilliee.iotventure.data.local.PreferencesManager
import dev.yilliee.iotventure.data.remote.ApiService
import dev.yilliee.iotventure.data.repository.AuthRepository
import dev.yilliee.iotventure.data.repository.ChatRepository
import dev.yilliee.iotventure.data.repository.GameRepository

/**
 * Simple service locator pattern for dependency injection
 */
object ServiceLocator {

    private var preferencesManager: PreferencesManager? = null
    private var apiService: ApiService? = null
    private var authRepository: AuthRepository? = null
    private var chatRepository: ChatRepository? = null
    private var gameRepository: GameRepository? = null

    fun providePreferencesManager(context: Context): PreferencesManager {
        return preferencesManager ?: synchronized(this) {
            PreferencesManager(context.applicationContext).also {
                preferencesManager = it
            }
        }
    }

    fun provideApiService(context: Context): ApiService {
        return apiService ?: synchronized(this) {
            val api = ApiService()
            val prefs = providePreferencesManager(context)
            api.updateServerSettings(prefs.getServerIp(), prefs.getServerPort())
            api.also {
                apiService = it
            }
        }
    }

    fun provideGameRepository(context: Context): GameRepository {
        return gameRepository ?: synchronized(this) {
            GameRepository(
                providePreferencesManager(context),
                provideApiService(context)
            ).also {
                gameRepository = it
            }
        }
    }

    fun provideChatRepository(context: Context): ChatRepository {
        return chatRepository ?: synchronized(this) {
            ChatRepository(
                provideApiService(context),
                providePreferencesManager(context)
            ).also {
                chatRepository = it
            }
        }
    }

    fun provideAuthRepository(context: Context): AuthRepository {
        return authRepository ?: synchronized(this) {
            val gameRepo = provideGameRepository(context)
            AuthRepository(
                provideApiService(context),
                providePreferencesManager(context),
                provideChatRepository(context),
                gameRepo
            ).also {
                authRepository = it
            }
        }
    }

    fun resetRepositories() {
        authRepository = null
        chatRepository = null
        gameRepository = null
    }
}
