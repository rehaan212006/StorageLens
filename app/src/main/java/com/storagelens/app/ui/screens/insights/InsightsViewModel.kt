package com.storagelens.app.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.storagelens.app.data.models.DuplicateGroup
import com.storagelens.app.data.models.FileItem
import com.storagelens.app.domain.usecases.DeleteFileUseCase
import com.storagelens.app.domain.usecases.FindDuplicatesUseCase
import com.storagelens.app.domain.usecases.GetStorageInsightsUseCase
import com.storagelens.app.domain.usecases.StorageInsights
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InsightsViewModel(
    private val getStorageInsightsUseCase: GetStorageInsightsUseCase,
    private val findDuplicatesUseCase: FindDuplicatesUseCase,
    private val deleteFileUseCase: DeleteFileUseCase
) : ViewModel() {

    private val _insights = MutableStateFlow(StorageInsights())
    val insights: StateFlow<StorageInsights> = _insights.asStateFlow()

    private val _pendingDeleteGroup = MutableStateFlow<DuplicateGroup?>(null)
    val pendingDeleteGroup: StateFlow<DuplicateGroup?> = _pendingDeleteGroup.asStateFlow()

    private val _pendingDeleteFile = MutableStateFlow<FileItem?>(null)
    val pendingDeleteFile: StateFlow<FileItem?> = _pendingDeleteFile.asStateFlow()

    init {
        refresh()
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
            findDuplicatesUseCase.deleteSelectedInGroup(group)
            _pendingDeleteGroup.value = null
            refresh()
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
            deleteFileUseCase(file)
            _pendingDeleteFile.value = null
            refresh()
        }
    }

    companion object {
        fun provideFactory(
            getStorageInsightsUseCase: GetStorageInsightsUseCase,
            findDuplicatesUseCase: FindDuplicatesUseCase,
            deleteFileUseCase: DeleteFileUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return InsightsViewModel(
                    getStorageInsightsUseCase,
                    findDuplicatesUseCase,
                    deleteFileUseCase
                ) as T
            }
        }
    }
}
