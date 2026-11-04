package com.storagelens.app.data.models

/**
 * Storage metrics for device storage and categorized breakdown.
 * Uses real StatFs and MediaStore metrics; never fabricated.
 */
data class StorageStats(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val categorySizes: Map<StorageCategory, Long> = emptyMap(),
    val categoryCounts: Map<StorageCategory, Int> = emptyMap(),
    val isAvailable: Boolean = true,
    val lastScanTimestamp: Long = 0L
) {
    val usedPercentage: Float
        get() = if (totalBytes > 0L) {
            ((usedBytes.toDouble() / totalBytes.toDouble()) * 100f).toFloat().coerceIn(0f, 100f)
        } else {
            0f
        }

    val freePercentage: Float
        get() = (100f - usedPercentage).coerceIn(0f, 100f)

    fun getCategoryPercentage(category: StorageCategory): Float {
        val catBytes = categorySizes[category] ?: 0L
        return if (totalBytes > 0L) {
            ((catBytes.toDouble() / totalBytes.toDouble()) * 100f).toFloat().coerceIn(0f, 100f)
        } else {
            0f
        }
    }
}
