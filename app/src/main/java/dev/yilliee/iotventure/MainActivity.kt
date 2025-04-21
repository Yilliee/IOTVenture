package dev.yilliee.iotventure

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import dev.yilliee.iotventure.navigation.AppNavigation
import dev.yilliee.iotventure.ui.theme.IOTVentureTheme

class MainActivity : ComponentActivity() {
    // Create a companion object to hold the NFC intent state that can be observed by the ScanNfcScreen
    companion object {
        val nfcIntent = mutableStateOf<Intent?>(null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if the app was launched from an NFC intent
        if (isNfcIntent(intent)) {
            nfcIntent.value = intent
        }

        setContent {
            IOTVentureTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
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
