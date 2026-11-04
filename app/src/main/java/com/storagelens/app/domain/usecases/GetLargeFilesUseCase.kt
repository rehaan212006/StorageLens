package com.storagelens.app.domain.usecases

import com.storagelens.app.data.models.FileItem
import com.storagelens.app.data.models.LargeFileFilter
import com.storagelens.app.data.models.SortOrder
import com.storagelens.app.data.repository.StorageRepository

class GetLargeFilesUseCase(private val repository: StorageRepository) {
    operator fun invoke(filter: LargeFileFilter, sortOrder: SortOrder): List<FileItem> {
        return repository.getLargeFiles(filter, sortOrder)
    }
}
