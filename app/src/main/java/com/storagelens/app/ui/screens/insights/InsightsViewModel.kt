package com.storagelens.app.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.storagelens.app.data.models.DeleteResult
import com.storagelens.app.data.models.DuplicateGroup
import com.storagelens.app.data.models.FileItem
import com.storagelens.app.data.repository.StorageRepository
import com.storagelens.app.domain.usecases.DeleteFileUseCase
import com.storagelens.app.domain.usecases.FindDuplicatesUseCase
import com.storagelens.app.domain.usecases.GetStorageInsightsUseCase
import com.storagelens.app.domain.usecases.StorageInsights
import com.storagelens.app.utils.DeletionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InsightsViewModel(
    private val getStorageInsightsUseCase: GetStorageInsightsUseCase,
    private val findDuplicatesUseCase: FindDuplicatesUseCase,
    private val deleteFileUseCase: DeleteFileUseCase,
    private val storageRepository: StorageRepository,
    private val deletionManager: DeletionManager
) : ViewModel() {

    private val _insights = MutableStateFlow(StorageInsights())
    val insights: StateFlow<StorageInsights> = _insights.asStateFlow()

    private val _pendingDeleteGroup = MutableStateFlow<DuplicateGroup?>(null)
    val pendingDeleteGroup: StateFlow<DuplicateGroup?> = _pendingDeleteGroup.asStateFlow()

    private val _pendingDeleteFile = MutableStateFlow<FileItem?>(null)
    val pendingDeleteFile: StateFlow<FileItem?> = _pendingDeleteFile.asStateFlow()

    val scanState: StateFlow<com.storagelens.app.data.models.ScanState> = storageRepository.scanState

    init {
        refresh()
        viewModelScope.launch {
            storageRepository.scannedFiles.collect {
                refresh()
            }
        }
        viewModelScope.launch {
            storageRepository.duplicates.collect {
                refresh()
            }
        }
        viewModelScope.launch {
            storageRepository.topFolders.collect {
                refresh()
            }
        }
        viewModelScope.launch {
            storageRepository.scanState.collect {
                refresh()
            }
        }
    }

    fun triggerScan() {
        storageRepository.startScan(viewModelScope, scanDuplicates = true)
    }

    fun refresh() {
        _insights.value = getStorageInsightsUseCase()
    }

    fun toggleDuplicateSelection(groupHash: String, fileId: String) {
        findDuplicatesUseCase.toggleSelection(groupHash, fileId)
        refresh()
    }

    fun promptDeleteDuplicates(group: DuplicateGroup) {
        _pendingDeleteGroup.value = group
    }

    fun dismissDeleteDuplicatesPrompt() {
        _pendingDeleteGroup.value = null
    }

    fun confirmDeleteDuplicates() {
        val group = _pendingDeleteGroup.value ?: return
        viewModelScope.launch {
            _pendingDeleteGroup.value = null
            val selectedFiles = group.files.filter { group.selectedFiles.contains(it.id) }
            if (selectedFiles.isEmpty()) return@launch

            when (val result = storageRepository.createBatchDeleteRequest(selectedFiles)) {
                is DeleteResult.Success -> {
                    for (file in selectedFiles) {
                        deleteFileUseCase.notifyFileDeleted(file.id)
                    }
                    refresh()
                }
                is DeleteResult.RequiresPermission -> {
                    deletionManager.requestDeletion(result.intentSender) { granted ->
                        if (granted) {
                            viewModelScope.launch {
                                for (file in selectedFiles) {
                                    deleteFileUseCase.notifyFileDeleted(file.id)
                                }
                                refresh()
                            }
                        }
                    }
                }
                is DeleteResult.Failure -> {
                    // Fall back to individual deletion
                    for (file in selectedFiles) {
                        when (val singleResult = deleteFileUseCase(file)) {
                            is DeleteResult.Success -> {}
                            is DeleteResult.RequiresPermission -> {
                                deletionManager.requestDeletion(singleResult.intentSender) { granted ->
                                    if (granted) {
                                        viewModelScope.launch {
                                            deleteFileUseCase.notifyFileDeleted(file.id)
                                            refresh()
                                        }
                                    }
                                }
                            }
                            is DeleteResult.Failure -> {}
                        }
                    }
                    refresh()
                }
            }
        }
    }

    fun promptDeleteFile(file: FileItem) {
        _pendingDeleteFile.value = file
    }

    fun dismissDeleteFilePrompt() {
        _pendingDeleteFile.value = null
    }

    fun confirmDeleteFile() {
        val file = _pendingDeleteFile.value ?: return
        viewModelScope.launch {
            _pendingDeleteFile.value = null
            when (val result = deleteFileUseCase(file)) {
                is DeleteResult.Success -> {
                    refresh()
                }
                is DeleteResult.RequiresPermission -> {
                    deletionManager.requestDeletion(result.intentSender) { granted ->
                        if (granted) {
                            viewModelScope.launch {
                                deleteFileUseCase.notifyFileDeleted(file.id)
                                refresh()
                            }
                        }
                    }
                }
                is DeleteResult.Failure -> {
                    refresh()
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            getStorageInsightsUseCase: GetStorageInsightsUseCase,
            findDuplicatesUseCase: FindDuplicatesUseCase,
            deleteFileUseCase: DeleteFileUseCase,
            storageRepository: StorageRepository,
            deletionManager: DeletionManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return InsightsViewModel(
                    getStorageInsightsUseCase,
                    findDuplicatesUseCase,
                    deleteFileUseCase,
                    storageRepository,
                    deletionManager
                ) as T
            }
        }
    }
}
