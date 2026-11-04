package com.storagelens.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.storagelens.app.data.models.SortOrder
import com.storagelens.app.data.preferences.ThemeMode
import com.storagelens.app.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = preferencesRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.DARK)

    val defaultSortOrder: StateFlow<SortOrder> = preferencesRepository.defaultSortOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SortOrder.LARGEST_FIRST)

    val showHiddenFiles: StateFlow<Boolean> = preferencesRepository.showHiddenFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setDefaultSortOrder(order: SortOrder) {
        viewModelScope.launch {
            preferencesRepository.setDefaultSortOrder(order)
        }
    }

    fun setShowHiddenFiles(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowHiddenFiles(show)
        }
    }

    companion object {
        fun provideFactory(
            preferencesRepository: UserPreferencesRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(preferencesRepository) as T
            }
        }
    }
}
