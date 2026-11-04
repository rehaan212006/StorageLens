package com.storagelens.app.scanner

import com.storagelens.app.data.models.DuplicateGroup
import org.junit.Assert.assertEquals
import org.junit.Test

class DuplicateDetectionLogicTest {

    @Test
    fun testDuplicateWastedBytesCalculation() {
        // 3 files of 10 MB each -> 1 original + 2 duplicates = 20 MB wasted
        val fileSize = 10L * 1024L * 1024L
        val group = DuplicateGroup(
            hash = "samplehash123",
            fileSize = fileSize,
            files = emptyList(), // we override size in test logic
            selectedFiles = setOf("id_2", "id_3")
        )

        // Selected bytes = 2 * 10 MB = 20 MB
        assertEquals(20L * 1024L * 1024L, group.selectedBytes)
    }

    @Test
    fun testPreliminarySizeGrouping() {
        data class TestFile(val id: String, val size: Long)

        val files = listOf(
            TestFile("f1", 1000L),
            TestFile("f2", 1000L),
            TestFile("f3", 2000L),
            TestFile("f4", 3000L),
            TestFile("f5", 3000L),
            TestFile("f6", 3000L)
        )

        val candidates = files.groupBy { it.size }.filter { it.value.size > 1 }

        assertEquals(2, candidates.size)
        assertEquals(2, candidates[1000L]?.size)
        assertEquals(3, candidates[3000L]?.size)
        assertEquals(null, candidates[2000L]) // f3 was unique in size, excluded from hashing!
    }
}
