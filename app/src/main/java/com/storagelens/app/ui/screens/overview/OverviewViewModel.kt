package com.storagelens.app.ui.screens.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.storagelens.app.data.models.ScanState
import com.storagelens.app.data.models.StorageCategory
import com.storagelens.app.data.models.StorageStats
import com.storagelens.app.domain.usecases.GetDeviceStorageUseCase
import com.storagelens.app.domain.usecases.ScanStorageUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OverviewViewModel(
    private val getDeviceStorageUseCase: GetDeviceStorageUseCase,
    private val scanStorageUseCase: ScanStorageUseCase
) : ViewModel() {

    val storageStats: StateFlow<StorageStats> = getDeviceStorageUseCase.stats
    val scanState: StateFlow<ScanState> = scanStorageUseCase.scanState

    init {
        loadInitialStats()
    }

    fun loadInitialStats() {
        viewModelScope.launch {
            try {
                getDeviceStorageUseCase()
            } catch (t: Throwable) {
                // Defensive guard against OEM storage exceptions
            }
        }
    }

    fun triggerScan() {
        scanStorageUseCase(viewModelScope, scanDuplicates = true)
    }

    fun cancelScan() {
        scanStorageUseCase.cancel()
    }

    companion object {
        fun provideFactory(
            getDeviceStorageUseCase: GetDeviceStorageUseCase,
            scanStorageUseCase: ScanStorageUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return OverviewViewModel(getDeviceStorageUseCase, scanStorageUseCase) as T
            }
        }
    }
}
