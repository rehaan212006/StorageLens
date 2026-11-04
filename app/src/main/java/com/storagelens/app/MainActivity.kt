package com.storagelens.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.storagelens.app.data.preferences.ThemeMode
import com.storagelens.app.navigation.StorageLensApp
import com.storagelens.app.ui.theme.StorageLensTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val app = application as? StorageLensApplication
            ?: (applicationContext as? StorageLensApplication)
            ?: run {
                val fallback = StorageLensApplication()
                fallback.container = DefaultAppContainer(fallback)
                fallback
            }

        val preferencesRepository = app.container.preferencesRepository

        setContent {
            val themeMode by preferencesRepository.themeMode.collectAsState(initial = ThemeMode.DARK)

            StorageLensTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = StorageLensTheme.colors.background
                ) {
                    StorageLensApp()
                }
            }
        }
    }
}
