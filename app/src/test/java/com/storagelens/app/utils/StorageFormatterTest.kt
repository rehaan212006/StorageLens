package com.storagelens.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageFormatterTest {

    @Test
    fun testFormatBytesZero() {
        assertEquals("0 B", StorageFormatter.formatBytes(0L))
        assertEquals("0 B", StorageFormatter.formatBytes(-50L))
    }

    @Test
    fun testFormatBytesSmall() {
        assertEquals("500 B", StorageFormatter.formatBytes(500L))
        assertEquals("1,023 B", StorageFormatter.formatBytes(1023L))
    }

    @Test
    fun testFormatBytesKilobytes() {
        val result = StorageFormatter.formatBytes(1024L)
        assertTrue(result.contains("KB"))
    }

    @Test
    fun testFormatBytesMegabytes() {
        val result = StorageFormatter.formatBytes(150L * 1024L * 1024L)
        assertTrue(result.contains("MB"))
    }

    @Test
    fun testFormatBytesGigabytes() {
        val bytes = (64.5 * 1024.0 * 1024.0 * 1024.0).toLong()
        val result = StorageFormatter.formatBytes(bytes)
        assertTrue(result.contains("GB"))
    }

    @Test
    fun testFormatPercentage() {
        assertEquals("75.5%", StorageFormatter.formatPercentage(75.5f))
        assertEquals("0.0%", StorageFormatter.formatPercentage(0.0f))
        assertEquals("100.0%", StorageFormatter.formatPercentage(100.0f))
    }
}
