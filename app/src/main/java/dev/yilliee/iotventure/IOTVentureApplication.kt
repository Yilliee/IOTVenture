package dev.yilliee.iotventure

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import dev.yilliee.iotventure.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IOTVentureApplication : Application() {

    companion object {
        private const val TAG = "IOTVentureApplication"
    }

    private var activityReferences = 0
    private var isActivityChangingConfigurations = false

    override fun onCreate() {
        super.onCreate()

        // Initialize preferences manager
        val preferencesManager = ServiceLocator.providePreferencesManager(this)

        // Initialize API service with stored server settings
        val apiService = ServiceLocator.provideApiService(this)

        // Log server settings
        Log.d(TAG, "Server settings: ${preferencesManager.getServerIp()}:${preferencesManager.getServerPort()}")

        // Register activity lifecycle callbacks
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {
                if (++activityReferences == 1 && !isActivityChangingConfigurations) {
                    Log.d(TAG, "App entered foreground")
                }
            }

            override fun onActivityResumed(activity: Activity) {}

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                isActivityChangingConfigurations = activity.isChangingConfigurations
                if (--activityReferences == 0 && !isActivityChangingConfigurations) {
                    Log.d(TAG, "App entered background")
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                // Only perform logout if this is the last activity and we're not changing configurations
                if (activityReferences == 0 && !isActivityChangingConfigurations && activity.isFinishing) {
                    Log.d(TAG, "App is being terminated, performing logout")
                    performLogout()
                }
            }
        })
    }

    private fun performLogout() {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val authRepository = ServiceLocator.provideAuthRepository(this@IOTVentureApplication)
                if (authRepository.isLoggedIn()) {
                    Log.d(TAG, "Logging out user")
                    authRepository.logout()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during logout: ${e.message}")
            } finally {
                ServiceLocator.resetRepositories()
                Log.d(TAG, "Logout completed - all data cleared")
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        // This is a fallback, but we're now handling logout in onActivityDestroyed
        performLogout()
    }
}
