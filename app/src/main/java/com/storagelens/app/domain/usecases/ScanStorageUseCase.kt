package com.storagelens.app.domain.usecases

import com.storagelens.app.data.models.ScanState
import com.storagelens.app.data.repository.StorageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

class ScanStorageUseCase(private val repository: StorageRepository) {
    val scanState: StateFlow<ScanState> = repository.scanState

    operator fun invoke(scope: CoroutineScope, scanDuplicates: Boolean = true) {
        repository.startScan(scope, scanDuplicates)
    }

    fun cancel() {
        repository.cancelScan()
    }
}
