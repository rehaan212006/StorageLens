package com.storagelens.app.domain.usecases

import com.storagelens.app.data.models.DuplicateGroup
import com.storagelens.app.data.repository.StorageRepository
import kotlinx.coroutines.flow.StateFlow

class FindDuplicatesUseCase(private val repository: StorageRepository) {
    val duplicates: StateFlow<List<DuplicateGroup>> = repository.duplicates

    fun toggleSelection(groupHash: String, fileId: String) {
        repository.toggleDuplicateSelection(groupHash, fileId)
    }

    suspend fun deleteSelectedInGroup(group: DuplicateGroup): Int {
        return repository.deleteDuplicatesInGroup(group)
    }
}
