package com.storagelens.app

import android.os.Bundle
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
}
