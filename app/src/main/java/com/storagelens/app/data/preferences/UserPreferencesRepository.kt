package com.storagelens.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.storagelens.app.data.models.LargeFileFilter
import com.storagelens.app.data.models.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "storage_lens_prefs")

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_SORT_ORDER = stringPreferencesKey("default_sort_order")
        val LARGE_FILE_THRESHOLD = stringPreferencesKey("large_file_threshold")
        val LAST_SCAN_TIMESTAMP = longPreferencesKey("last_scan_timestamp")
        val DEEP_SCAN_ENABLED = booleanPreferencesKey("deep_scan_enabled")
        val SHOW_HIDDEN_FILES = booleanPreferencesKey("show_hidden_files")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val name = prefs[PreferencesKeys.THEME_MODE] ?: ThemeMode.DARK.name
        try {
            ThemeMode.valueOf(name)
        } catch (e: Exception) {
            ThemeMode.DARK
        }
    }

    val defaultSortOrder: Flow<SortOrder> = context.dataStore.data.map { prefs ->
        val name = prefs[PreferencesKeys.DEFAULT_SORT_ORDER] ?: SortOrder.LARGEST_FIRST.name
        try {
            SortOrder.valueOf(name)
        } catch (e: Exception) {
            SortOrder.LARGEST_FIRST
        }
    }

    val largeFileFilter: Flow<LargeFileFilter> = context.dataStore.data.map { prefs ->
        val name = prefs[PreferencesKeys.LARGE_FILE_THRESHOLD] ?: LargeFileFilter.ABOVE_100MB.name
        try {
            LargeFileFilter.valueOf(name)
        } catch (e: Exception) {
            LargeFileFilter.ABOVE_100MB
        }
    }

    val lastScanTimestamp: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.LAST_SCAN_TIMESTAMP] ?: 0L
    }

    val showHiddenFiles: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.SHOW_HIDDEN_FILES] ?: false
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[PreferencesKeys.THEME_MODE] = mode.name }
    }

    suspend fun setDefaultSortOrder(order: SortOrder) {
        context.dataStore.edit { it[PreferencesKeys.DEFAULT_SORT_ORDER] = order.name }
    }

    suspend fun setLargeFileFilter(filter: LargeFileFilter) {
        context.dataStore.edit { it[PreferencesKeys.LARGE_FILE_THRESHOLD] = filter.name }
    }

    suspend fun setLastScanTimestamp(timestamp: Long) {
        context.dataStore.edit { it[PreferencesKeys.LAST_SCAN_TIMESTAMP] = timestamp }
    }

    suspend fun setShowHiddenFiles(show: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SHOW_HIDDEN_FILES] = show }
    }
}
