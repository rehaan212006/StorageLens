package com.storagelens.app.domain.usecases

import com.storagelens.app.data.models.DeleteResult
import com.storagelens.app.data.models.FileItem
import com.storagelens.app.data.repository.StorageRepository

class DeleteFileUseCase(private val repository: StorageRepository) {
    suspend operator fun invoke(item: FileItem): DeleteResult {
        return repository.deleteFile(item)
    }

    suspend fun notifyFileDeleted(fileId: String) {
        repository.notifyFileDeleted(fileId)
    }
}
