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
            // Using primary external storage directory (shared device storage)
            val path: File = Environment.getExternalStorageDirectory()
            val statFs = StatFs(path.path)

            val blockSize = statFs.blockSizeLong
            val totalBlocks = statFs.blockCountLong
            val availableBlocks = statFs.availableBlocksLong

            var totalBytes = totalBlocks * blockSize
            var freeBytes = availableBlocks * blockSize

            // Also check internal data directory if external report seems zero
            if (totalBytes <= 0L) {
                val dataPath = Environment.getDataDirectory()
                val dataStat = StatFs(dataPath.path)
                totalBytes = dataStat.blockCountLong * dataStat.blockSizeLong
                freeBytes = dataStat.availableBlocksLong * dataStat.blockSizeLong
            }

            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)

            StorageStats(
                totalBytes = totalBytes,
                usedBytes = usedBytes,
                freeBytes = freeBytes,
                isAvailable = totalBytes > 0L,
                lastScanTimestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            StorageStats(isAvailable = false)
        }
    }
}
