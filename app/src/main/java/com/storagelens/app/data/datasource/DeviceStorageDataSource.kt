package com.storagelens.app.data.datasource

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import com.storagelens.app.data.models.StorageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Native Android data source for querying real device storage statistics.
 * Uses StatFs and StorageManager APIs without fabricating data.
 */
class DeviceStorageDataSource(private val context: Context) {

    suspend fun getDeviceStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        try {
            var totalBytes = 0L
            var freeBytes = 0L

            // Using primary external storage directory (shared device storage)
            val path: File? = try { Environment.getExternalStorageDirectory() } catch (t: Throwable) { null }
            if (path != null && path.exists()) {
                try {
                    val statFs = StatFs(path.path)
                    val blockSize = statFs.blockSizeLong
                    totalBytes = statFs.blockCountLong * blockSize
                    freeBytes = statFs.availableBlocksLong * blockSize
                } catch (t: Throwable) {
                    // Fall back to internal data directory
                }
            }

            // Also check internal data directory if external report seems zero
            if (totalBytes <= 0L) {
                try {
                    val dataPath = Environment.getDataDirectory()
                    val dataStat = StatFs(dataPath.path)
                    val blockSize = dataStat.blockSizeLong
                    totalBytes = dataStat.blockCountLong * blockSize
                    freeBytes = dataStat.availableBlocksLong * blockSize
                } catch (t: Throwable) {}
            }

            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)

            StorageStats(
                totalBytes = totalBytes,
                usedBytes = usedBytes,
                freeBytes = freeBytes,
                isAvailable = totalBytes > 0L,
                lastScanTimestamp = System.currentTimeMillis()
            )
        } catch (t: Throwable) {
            StorageStats(isAvailable = false)
        }
    }
}
