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

class MainActivity : ComponentActivity() {
    // Create a companion object to hold the NFC intent state that can be observed by the ScanNfcScreen
    companion object {
        private const val TAG = "MainActivity"
        val nfcIntent = mutableStateOf<Intent?>(null)
    }

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
        }

        // Initialize repositories
        val authRepository = ServiceLocator.provideAuthRepository(this)
        val isLoggedIn = authRepository.isLoggedIn()

        Log.d(TAG, "User is logged in: $isLoggedIn")

        if (isLoggedIn) {
            val username = authRepository.getUsername()
            Log.d(TAG, "Logged in user: $username")
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
                        }
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
        }
    }

    // Helper function to check if an intent is an NFC intent
    private fun isNfcIntent(intent: Intent): Boolean {
        return intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED ||
                intent.action == NfcAdapter.ACTION_TECH_DISCOVERED ||
                intent.action == NfcAdapter.ACTION_TAG_DISCOVERED
    }
}
