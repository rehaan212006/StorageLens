package com.storagelens.app.ui.screens.largefiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.storagelens.app.data.models.FileItem
import com.storagelens.app.data.models.LargeFileFilter
import com.storagelens.app.data.models.SortOrder
import com.storagelens.app.data.models.StorageCategory
import com.storagelens.app.domain.usecases.DeleteFileUseCase
import com.storagelens.app.domain.usecases.GetLargeFilesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LargeFilesViewModel(
    private val getLargeFilesUseCase: GetLargeFilesUseCase,
    private val deleteFileUseCase: DeleteFileUseCase
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(LargeFileFilter.ABOVE_100MB)
    val selectedFilter: StateFlow<LargeFileFilter> = _selectedFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.LARGEST_FIRST)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _selectedCategory = MutableStateFlow<StorageCategory?>(null)
    val selectedCategory: StateFlow<StorageCategory?> = _selectedCategory.asStateFlow()

    private val _largeFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val largeFiles: StateFlow<List<FileItem>> = _largeFiles.asStateFlow()

    private val _pendingDeleteFile = MutableStateFlow<FileItem?>(null)
    val pendingDeleteFile: StateFlow<FileItem?> = _pendingDeleteFile.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val allLarge = getLargeFilesUseCase(_selectedFilter.value, _sortOrder.value)
        val filtered = if (_selectedCategory.value != null) {
            allLarge.filter { it.category == _selectedCategory.value }
        } else {
            allLarge
        }
        _largeFiles.value = filtered
    }

    fun setFilter(filter: LargeFileFilter) {
        _selectedFilter.value = filter
        refresh()
    }

    fun setCategory(category: StorageCategory?) {
        _selectedCategory.value = category
        refresh()
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        refresh()
    }

    fun promptDelete(file: FileItem) {
        _pendingDeleteFile.value = file
    }

    fun dismissDeletePrompt() {
        _pendingDeleteFile.value = null
    }

    fun confirmDelete() {
        val file = _pendingDeleteFile.value ?: return
        viewModelScope.launch {
            val success = deleteFileUseCase(file)
            _pendingDeleteFile.value = null
            if (success) {
                refresh()
            }
        }
    }

    companion object {
        fun provideFactory(
            getLargeFilesUseCase: GetLargeFilesUseCase,
            deleteFileUseCase: DeleteFileUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LargeFilesViewModel(getLargeFilesUseCase, deleteFileUseCase) as T
            }
        }
    }
}
