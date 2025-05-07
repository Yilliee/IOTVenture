package dev.yilliee.iotventure

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import dev.yilliee.iotventure.di.ServiceLocator
import dev.yilliee.iotventure.navigation.AppNavigation
import dev.yilliee.iotventure.ui.theme.IOTVentureTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    // Create a companion object to hold the NFC intent state that can be observed by the ScanNfcScreen
    companion object {
        private const val TAG = "MainActivity"
        val nfcIntent = mutableStateOf<Intent?>(null)
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide the status bar and make the app fullscreen
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        // Set window soft input mode to adjust resize
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        // Check if the app was launched from an NFC intent
        if (isNfcIntent(intent)) {
            nfcIntent.value = intent
            Log.d(TAG, "App launched from NFC intent")
        }

        // Initialize repositories
        val authRepository = ServiceLocator.provideAuthRepository(this)
        val gameRepository = ServiceLocator.provideGameRepository(this)
        val isLoggedIn = authRepository.isLoggedIn()

        Log.d(TAG, "User is logged in: $isLoggedIn")

        if (isLoggedIn) {
            val username = authRepository.getUsername()
            Log.d(TAG, "Logged in user: $username")

            // Try to submit any pending solves
            coroutineScope.launch {
                try {
                    gameRepository.submitSolves()
                } catch (e: Exception) {
                    Log.e(TAG, "Error submitting solves on app start", e)
                }
            }
        }

        setContent {
            IOTVentureTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        startDestination = if (isLoggedIn) {
                            dev.yilliee.iotventure.navigation.AppDestinations.DASHBOARD_ROUTE
                        } else {
                            dev.yilliee.iotventure.navigation.AppDestinations.LOGIN_ROUTE
                        },
                        context = this
                    )
                }
            }
        }
    }

    // Handle NFC intents properly
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // Check if this is an NFC intent
        if (isNfcIntent(intent)) {
            // Store the intent for later processing and update the state
            setIntent(intent)
            nfcIntent.value = intent
            Log.d(TAG, "Received new NFC intent")
        }
    }

    override fun onResume() {
        super.onResume()

        // Try to submit any pending solves when app resumes
        val gameRepository = ServiceLocator.provideGameRepository(this)
        coroutineScope.launch {
            try {
                gameRepository.submitSolves()
            } catch (e: Exception) {
                Log.e(TAG, "Error submitting solves on app resume", e)
            }
        }
    }

    // Add onDestroy method to clear data when app is closed
    override fun onDestroy() {
        super.onDestroy()

        // Only clear data if this is a real app termination, not a configuration change
        if (isFinishing) {
            Log.d(TAG, "App is being destroyed (not for configuration change)")
            coroutineScope.launch {
                try {
                    val authRepository = ServiceLocator.provideAuthRepository(this@MainActivity)
                    // Logout and clear data
                    authRepository.logout()
                    Log.d(TAG, "User logged out and data cleared on app termination")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during logout on app termination", e)
                }
            }
        }
    }

    // Helper function to check if an intent is an NFC intent
    private fun isNfcIntent(intent: Intent): Boolean {
        return intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED ||
                intent.action == NfcAdapter.ACTION_TECH_DISCOVERED ||
                intent.action == NfcAdapter.ACTION_TAG_DISCOVERED
    }
}
