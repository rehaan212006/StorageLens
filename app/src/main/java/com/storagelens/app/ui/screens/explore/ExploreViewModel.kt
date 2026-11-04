package com.storagelens.app.ui.screens.explore

import android.content.IntentSender
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.storagelens.app.data.models.DeleteResult
import com.storagelens.app.data.models.FileItem
import com.storagelens.app.data.models.FolderItem
import com.storagelens.app.data.models.SortOrder
import com.storagelens.app.data.repository.StorageRepository
import com.storagelens.app.domain.usecases.DeleteFileUseCase
import com.storagelens.app.ui.components.BreadcrumbItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ExploreViewModel(
    private val storageRepository: StorageRepository,
    private val deleteFileUseCase: DeleteFileUseCase
) : ViewModel() {

    private val rootPath = try {
        Environment.getExternalStorageDirectory()?.absolutePath ?: "/sdcard"
    } catch (e: Exception) {
        "/sdcard"
    }

    private val _currentPath = MutableStateFlow(rootPath)
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _breadcrumbs = MutableStateFlow<List<BreadcrumbItem>>(
        listOf(BreadcrumbItem("Internal Storage", rootPath))
    )
    val breadcrumbs: StateFlow<List<BreadcrumbItem>> = _breadcrumbs.asStateFlow()

    private val _folders = MutableStateFlow<List<FolderItem>>(emptyList())
    val folders: StateFlow<List<FolderItem>> = _folders.asStateFlow()

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.LARGEST_FIRST)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _pendingDeleteFile = MutableStateFlow<FileItem?>(null)
    val pendingDeleteFile: StateFlow<FileItem?> = _pendingDeleteFile.asStateFlow()

    private val _pendingDeleteIntentSender = MutableStateFlow<IntentSender?>(null)
    val pendingDeleteIntentSender: StateFlow<IntentSender?> = _pendingDeleteIntentSender.asStateFlow()

    private var fileAwaitingPermission: FileItem? = null

    init {
        loadDirectory(rootPath)
    }

    fun loadDirectory(path: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _currentPath.value = path
            updateBreadcrumbs(path)

            try {
                val (dirs, fileList) = storageRepository.listDirectory(path)
                _folders.value = sortFolders(dirs, _sortOrder.value)
                _files.value = sortFiles(fileList, _sortOrder.value)
            } catch (e: Exception) {
                _errorMessage.value = "Cannot access folder: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadSafTree(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val (dirs, fileList) = storageRepository.listTree(uri)
                _folders.value = dirs
                _files.value = fileList
                _breadcrumbs.value = listOf(
                    BreadcrumbItem("Custom Folder", uri.toString())
                )
            } catch (e: Exception) {
                _errorMessage.value = "Could not open selected folder: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        _folders.value = sortFolders(_folders.value, order)
        _files.value = sortFiles(_files.value, order)
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
            _pendingDeleteFile.value = null
            when (val result = deleteFileUseCase(file)) {
                is DeleteResult.Success -> {
                    _files.value = _files.value.filter { it.id != file.id }
                }
                is DeleteResult.RequiresPermission -> {
                    fileAwaitingPermission = file
                    _pendingDeleteIntentSender.value = result.intentSender
                }
                is DeleteResult.Failure -> {
                    _errorMessage.value = result.message ?: "Could not delete file due to Android permission constraints."
                }
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _pendingDeleteIntentSender.value = null
        val file = fileAwaitingPermission
        fileAwaitingPermission = null
        if (granted && file != null) {
            viewModelScope.launch {
                deleteFileUseCase.notifyFileDeleted(file.id)
                _files.value = _files.value.filter { it.id != file.id }
            }
        }
    }

    private fun updateBreadcrumbs(path: String) {
        val crumbs = mutableListOf<BreadcrumbItem>()
        crumbs.add(BreadcrumbItem("Internal Storage", rootPath))

        if (path != rootPath && path.startsWith(rootPath)) {
            val relative = path.removePrefix(rootPath).trim('/')
            if (relative.isNotEmpty()) {
                val segments = relative.split('/')
                var accumulated = rootPath
                for (segment in segments) {
                    accumulated = "$accumulated/$segment"
                    crumbs.add(BreadcrumbItem(segment, accumulated))
                }
            }
        }
        _breadcrumbs.value = crumbs
    }

    private fun sortFolders(list: List<FolderItem>, order: SortOrder): List<FolderItem> {
        return when (order) {
            SortOrder.LARGEST_FIRST -> list.sortedByDescending { it.sizeBytes }
            SortOrder.SMALLEST_FIRST -> list.sortedBy { it.sizeBytes }
            SortOrder.ALPHABETICAL -> list.sortedBy { it.name.lowercase() }
            SortOrder.RECENTLY_MODIFIED -> list.sortedByDescending { it.lastModified }
        }
    }

    private fun sortFiles(list: List<FileItem>, order: SortOrder): List<FileItem> {
        return when (order) {
            SortOrder.LARGEST_FIRST -> list.sortedByDescending { it.sizeBytes }
            SortOrder.SMALLEST_FIRST -> list.sortedBy { it.sizeBytes }
            SortOrder.ALPHABETICAL -> list.sortedBy { it.name.lowercase() }
            SortOrder.RECENTLY_MODIFIED -> list.sortedByDescending { it.lastModified }
        }
    }

    companion object {
        fun provideFactory(
            storageRepository: StorageRepository,
            deleteFileUseCase: DeleteFileUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ExploreViewModel(storageRepository, deleteFileUseCase) as T
            }
        }
    }
}
