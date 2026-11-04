package com.storagelens.app.domain.usecases

import com.storagelens.app.data.models.StorageStats
import com.storagelens.app.data.repository.StorageRepository
import kotlinx.coroutines.flow.StateFlow

class GetDeviceStorageUseCase(private val repository: StorageRepository) {
    val stats: StateFlow<StorageStats> = repository.latestStats

    suspend operator fun invoke() {
        repository.loadInitialStats()
    }
}
