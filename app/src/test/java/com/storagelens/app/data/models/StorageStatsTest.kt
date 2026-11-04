package com.storagelens.app.data.models

import org.junit.Assert.assertEquals
import org.junit.Test

class StorageStatsTest {

    @Test
    fun testStoragePercentageCalculation() {
        val total = 100L * 1024L * 1024L * 1024L // 100 GB
        val used = 65L * 1024L * 1024L * 1024L   // 65 GB
        val free = 35L * 1024L * 1024L * 1024L   // 35 GB

        val stats = StorageStats(
            totalBytes = total,
            usedBytes = used,
            freeBytes = free
        )

        assertEquals(65.0f, stats.usedPercentage, 0.01f)
        assertEquals(35.0f, stats.freePercentage, 0.01f)
    }

    @Test
    fun testZeroTotalBytesDoesNotCrash() {
        val stats = StorageStats(
            totalBytes = 0L,
            usedBytes = 0L,
            freeBytes = 0L
        )

        assertEquals(0.0f, stats.usedPercentage, 0.01f)
        assertEquals(100.0f, stats.freePercentage, 0.01f)
    }

    @Test
    fun testCategoryPercentage() {
        val total = 1000L
        val catMap = mapOf(
            StorageCategory.IMAGES to 250L,
            StorageCategory.VIDEOS to 500L
        )

        val stats = StorageStats(
            totalBytes = total,
            categorySizes = catMap
        )

        assertEquals(25.0f, stats.getCategoryPercentage(StorageCategory.IMAGES), 0.01f)
        assertEquals(50.0f, stats.getCategoryPercentage(StorageCategory.VIDEOS), 0.01f)
        assertEquals(0.0f, stats.getCategoryPercentage(StorageCategory.AUDIO), 0.01f)
    }
}
