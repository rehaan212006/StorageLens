package com.storagelens.app.utils

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.ByteArrayInputStream

class HashUtilsTest {

    @Test
    fun testKnownSha256Hash() = runBlocking {
        // "hello world" SHA-256 is b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9
        val input = "hello world".toByteArray(Charsets.UTF_8)
        val stream = ByteArrayInputStream(input)

        val hash = HashUtils.calculateSha256(stream)
        assertNotNull(hash)
        assertEquals("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9", hash)
    }

    @Test
    fun testEmptySha256Hash() = runBlocking {
        // empty string SHA-256 is e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        val input = ByteArray(0)
        val stream = ByteArrayInputStream(input)

        val hash = HashUtils.calculateSha256(stream)
        assertNotNull(hash)
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash)
    }
}
