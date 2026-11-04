package com.storagelens.app

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.storagelens.app.data.preferences.ThemeMode
import com.storagelens.app.navigation.StorageLensApp
import com.storagelens.app.ui.theme.StorageLensTheme

class MainActivity : AppCompatActivity() {

    private val tag = "MainActivity"

    // Holds the callback for the active Scoped Storage deletion request
    private var pendingDeleteCallback: ((Boolean) -> Unit)? = null

    // Activity catches the result of the IntentSenderRequest for Scoped Storage deletion
    private val deleteRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val granted = (result.resultCode == Activity.RESULT_OK)
        Log.i(tag, "Caught deletion IntentSender result: resultCode=${result.resultCode}, granted=$granted")
        pendingDeleteCallback?.invoke(granted)
        pendingDeleteCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        } catch (t: Throwable) {
            // Guard against vendor-specific reflection issues on ColorOS / Oplus
        }

        val container = (application as? StorageLensApplication)?.container
            ?: (applicationContext as? StorageLensApplication)?.container
            ?: DefaultAppContainer(application)

        // Register Activity launcher to handle Scoped Storage deletion intents
        container.deletionManager.registerActivityLauncher { intentSender, callback ->
            Log.d(tag, "Launching deletion IntentSenderRequest from Activity")
            pendingDeleteCallback = callback
            val request = IntentSenderRequest.Builder(intentSender).build()
            deleteRequestLauncher.launch(request)
        }

        val preferencesRepository = container.preferencesRepository

        setContent {
            val themeMode by preferencesRepository.themeMode.collectAsState(initial = ThemeMode.DARK)

            StorageLensTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = StorageLensTheme.colors.background
                ) {
                    StorageLensApp(container = container)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val container = (application as? StorageLensApplication)?.container
            ?: (applicationContext as? StorageLensApplication)?.container
        container?.deletionManager?.unregisterActivityLauncher()
    }
}
