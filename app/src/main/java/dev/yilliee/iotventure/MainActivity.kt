package dev.yilliee.iotventure

import android.Manifest
import android.app.ActivityManager
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.yilliee.iotventure.di.ServiceLocator
import dev.yilliee.iotventure.navigation.AppNavigation
import dev.yilliee.iotventure.ui.theme.IOTVentureTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
        val nfcIntent = mutableStateOf<Intent?>(null)
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var isEmergencyLocked = false
    private var isDialogShowing = false
    private var isHandlingLeave = false
    private var isFinishing = false
    private var isFromNfcScan = false // Track if we're coming from NFC scan

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent app from being minimized by gestures
        //window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        // Set up back press handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                try {
                    val authRepository = ServiceLocator.provideAuthRepository(this@MainActivity)
                    val gameRepository = ServiceLocator.provideGameRepository(this@MainActivity)
                    if (authRepository.isLoggedIn() && !gameRepository.isEmergencyLocked()) {
                        showExitDialog()
                    } else if (!gameRepository.isEmergencyLocked()) {
                        // Don't allow back press to bypass the dialog
                        showExitDialog()
                    }
                    // If emergency lock is active, do nothing (prevent back press)
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling back press", e)
                    // Show dialog even if there's an error
                    showExitDialog()
                }
            }
        })

        // Add these flags to prevent the app from being easily closed
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        // Initialize repositories
        val authRepository = ServiceLocator.provideAuthRepository(this)
        val gameRepository = ServiceLocator.provideGameRepository(this)
        val isLoggedIn = authRepository.isLoggedIn()
        val isEmergencyLocked = gameRepository.isEmergencyLocked()

        // Hide the status bar and make the app fullscreen
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        // Hide the status bar using the new WindowInsetsController API
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Set window soft input mode to adjust resize
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        // Check if the app was launched from an NFC intent
        if (isNfcIntent(intent)) {
            nfcIntent.value = intent
            Log.d(TAG, "App launched from NFC intent")
        }

        Log.d(TAG, "User is logged in: $isLoggedIn")
        Log.d(TAG, "Emergency lock is active: $isEmergencyLocked")

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
                        startDestination = when {
                            isEmergencyLocked -> dev.yilliee.iotventure.navigation.AppDestinations.EMERGENCY_UNLOCK_ROUTE
                            isLoggedIn -> dev.yilliee.iotventure.navigation.AppDestinations.DASHBOARD_ROUTE
                            else -> dev.yilliee.iotventure.navigation.AppDestinations.LOGIN_ROUTE
                        },
                        context = this
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Check if we're coming from NFC scan - don't show exit dialog in this case
        if (!isFinishing && !isDialogShowing && !isHandlingLeave && !isFromNfcScan) {
            try {
                val authRepository = ServiceLocator.provideAuthRepository(this)
                val gameRepository = ServiceLocator.provideGameRepository(this)
                if (authRepository.isLoggedIn() && !gameRepository.isEmergencyLocked()) {
                    isHandlingLeave = true
                    showExitDialog()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling app stop", e)
                // Show dialog even if there's an error
                isHandlingLeave = true
                showExitDialog()
            }
        }

        // Reset the NFC scan flag
        isFromNfcScan = false
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // This is called when the user presses the Home button
        if (!isHandlingLeave && !isDialogShowing && !isFromNfcScan) {
            isHandlingLeave = true
            try {
                val authRepository = ServiceLocator.provideAuthRepository(this)
                val gameRepository = ServiceLocator.provideGameRepository(this)
                if (authRepository.isLoggedIn() && !gameRepository.isEmergencyLocked()) {
                    showExitDialog()
                    // Bring the app back to the foreground after a short delay
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(100)
                        moveTaskToFront()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling user leave", e)
                // Show dialog even if there's an error
                showExitDialog()
                // Bring the app back to the foreground
                coroutineScope.launch {
                    kotlinx.coroutines.delay(100)
                    moveTaskToFront()
                }
            } finally {
                isHandlingLeave = false
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
            isFromNfcScan = true // Mark that we're processing an NFC scan
            Log.d(TAG, "Received new NFC intent")
        }
    }

    override fun onResume() {
        super.onResume()

        // Re-hide the status bar when the app resumes
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())

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

    override fun onPause() {
        super.onPause()

        // Check if this is a real pause (app being minimized) and not just a configuration change
        if (!isChangingConfigurations && !isFromNfcScan && !isDialogShowing) {
            try {
                val authRepository = ServiceLocator.provideAuthRepository(this)
                val gameRepository = ServiceLocator.provideGameRepository(this)
                if (authRepository.isLoggedIn() && !gameRepository.isEmergencyLocked()) {
                    showExitDialog()
                    // Bring the app back to the foreground after a short delay
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(100)
                        moveTaskToFront()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling app minimization", e)
                // Show dialog even if there's an error
                showExitDialog()
                // Bring the app back to the foreground
                coroutineScope.launch {
                    kotlinx.coroutines.delay(100)
                    moveTaskToFront()
                }
            }
        }
    }

    private fun showExitDialog() {
        if (isDialogShowing) return

        try {
            val gameRepository = ServiceLocator.provideGameRepository(this)
            if (gameRepository.isEmergencyLocked()) {
                // Don't show exit dialog if emergency lock is active
                return
            }

            isDialogShowing = true

            // Use a dialog that doesn't depend on AppCompat theme
            val builder = android.app.AlertDialog.Builder(this)
            val dialog = builder.setTitle("Exit Game")
                .setMessage("Do you want to exit the game? Your progress will be saved but you may forfeit any ongoing challenges.")
                .setPositiveButton("Exit") { _, _ ->
                    isEmergencyLocked = false  // Ensure data is cleared on normal exit
                    isDialogShowing = false
                    isFinishing = true
                    finish()
                }
                .setNegativeButton("Stay") { dialog, _ ->
                    isDialogShowing = false
                    isHandlingLeave = false
                    dialog.dismiss()
                }
                .setNeutralButton("Emergency Lock") { _, _ ->
                    coroutineScope.launch {
                        try {
                            val gameRepository = ServiceLocator.provideGameRepository(this@MainActivity)
                            gameRepository.emergencyLock()
                            isEmergencyLocked = true
                            isDialogShowing = false
                            isFinishing = true
                            finish()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error during emergency lock", e)
                            // If emergency lock fails, just exit normally
                            finish()
                        }
                    }
                }
                .setCancelable(false)
                .setOnDismissListener {
                    isDialogShowing = false
                    isHandlingLeave = false
                }
                .create()

            // Prevent dialog from being dismissed when touching outside
            dialog.setCanceledOnTouchOutside(false)

            // Set window flags to keep dialog on top
            dialog.window?.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            )

            // Show the dialog
            dialog.show()

            // Remove the untouchable flag after showing to allow interaction with the dialog
            dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing exit dialog", e)
            isDialogShowing = false
            isHandlingLeave = false
            // If dialog fails to show, just exit normally
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Only clear data if this is a real app termination and not emergency locked
        if (isFinishing && !isEmergencyLocked) {
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
        } else {
            Log.d(TAG, "App is being destroyed but data is preserved (emergency locked or configuration change)")
        }
    }

    // Helper function to check if an intent is an NFC intent
    private fun isNfcIntent(intent: Intent): Boolean {
        return intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED ||
                intent.action == NfcAdapter.ACTION_TECH_DISCOVERED ||
                intent.action == NfcAdapter.ACTION_TAG_DISCOVERED
    }

    // Method to set the NFC scan flag from other components
    fun setFromNfcScan(value: Boolean) {
        isFromNfcScan = value
        Log.d(TAG, "Setting isFromNfcScan to $value")
    }

    // Add this helper method to bring the app back to the foreground
    @RequiresPermission(Manifest.permission.REORDER_TASKS)
    private fun moveTaskToFront() {
        try {
            val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            activityManager.moveTaskToFront(taskId, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Error moving task to front", e)
        }
    }
}
