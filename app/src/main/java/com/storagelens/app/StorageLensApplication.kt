package com.storagelens.app

import android.app.Application
import com.storagelens.app.data.datasource.DeviceStorageDataSource
import com.storagelens.app.data.datasource.FileSystemDataSource
import com.storagelens.app.data.datasource.MediaStoreDataSource
import com.storagelens.app.data.datasource.SafDataSource
import com.storagelens.app.data.preferences.UserPreferencesRepository
import com.storagelens.app.data.repository.StorageRepository
import com.storagelens.app.data.repository.StorageRepositoryImpl
import com.storagelens.app.domain.usecases.DeleteFileUseCase
import com.storagelens.app.domain.usecases.FindDuplicatesUseCase
import com.storagelens.app.domain.usecases.GetDeviceStorageUseCase
import com.storagelens.app.domain.usecases.GetLargeFilesUseCase
import com.storagelens.app.domain.usecases.GetStorageInsightsUseCase
import com.storagelens.app.domain.usecases.ScanStorageUseCase
import com.storagelens.app.scanner.DuplicateDetector
import com.storagelens.app.scanner.StorageScanner

import com.storagelens.app.utils.DeletionManager

/**
 * Dependency injection container for StorageLens.
 */
interface AppContainer {
    val preferencesRepository: UserPreferencesRepository
    val storageRepository: StorageRepository
    val getDeviceStorageUseCase: GetDeviceStorageUseCase
    val scanStorageUseCase: ScanStorageUseCase
    val getLargeFilesUseCase: GetLargeFilesUseCase
    val findDuplicatesUseCase: FindDuplicatesUseCase
    val getStorageInsightsUseCase: GetStorageInsightsUseCase
    val deleteFileUseCase: DeleteFileUseCase
    val deletionManager: DeletionManager
}

class DefaultAppContainer(private val application: Application) : AppContainer {
    override val deletionManager: DeletionManager by lazy {
        DeletionManager()
    }

    override val preferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(application)
    }

    private val deviceStorageDataSource by lazy {
        DeviceStorageDataSource(application)
    }

    private val mediaStoreDataSource by lazy {
        MediaStoreDataSource(application)
    }

    private val fileSystemDataSource by lazy {
        FileSystemDataSource()
    }

    private val safDataSource by lazy {
        SafDataSource(application)
    }

    private val duplicateDetector by lazy {
        DuplicateDetector(application)
    }

    private val storageScanner by lazy {
        StorageScanner(
            context = application,
            deviceStorageDataSource = deviceStorageDataSource,
            mediaStoreDataSource = mediaStoreDataSource,
            fileSystemDataSource = fileSystemDataSource,
            duplicateDetector = duplicateDetector
        )
    }

    override val storageRepository: StorageRepository by lazy {
        StorageRepositoryImpl(
            context = application,
            storageScanner = storageScanner,
            fileSystemDataSource = fileSystemDataSource,
            mediaStoreDataSource = mediaStoreDataSource,
            safDataSource = safDataSource
        )
    }

    override val getDeviceStorageUseCase by lazy {
        GetDeviceStorageUseCase(storageRepository)
    }

    override val scanStorageUseCase by lazy {
        ScanStorageUseCase(storageRepository)
    }

    override val getLargeFilesUseCase by lazy {
        GetLargeFilesUseCase(storageRepository)
    }

    override val findDuplicatesUseCase by lazy {
        FindDuplicatesUseCase(storageRepository)
    }

    override val getStorageInsightsUseCase by lazy {
        GetStorageInsightsUseCase(storageRepository)
    }

    override val deleteFileUseCase by lazy {
        DeleteFileUseCase(storageRepository)
    }
}

class StorageLensApplication : Application() {
    val container: AppContainer by lazy {
        DefaultAppContainer(this)
    }

    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("StorageLens", "FATAL EXCEPTION in thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
