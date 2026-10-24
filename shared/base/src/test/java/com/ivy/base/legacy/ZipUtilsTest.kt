package com.ivy.base.legacy

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipUtilsTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun archive(vararg entries: Pair<String, String>): ByteArray = ByteArrayOutputStream().also { bytes ->
        ZipOutputStream(bytes).use { out -> entries.forEach { (name, content) ->
            out.putNextEntry(ZipEntry(name)); out.write(content.toByteArray()); out.closeEntry()
        } }
    }.toByteArray()
    private fun rejects(bytes: ByteArray, limits: ZipExtractionLimits = ZipExtractionLimits()) {
        assertThrows(UnsafeArchiveException::class.java) { unzip(ByteArrayInputStream(bytes), temporary.newFolder(), limits) }
    }
    @Test fun `valid nested archives still extract`() {
        val destination = temporary.newFolder()
        unzip(ByteArrayInputStream(archive("folder/data.json" to "{}")), destination)
        assertEquals("{}", File(destination, "folder/data.json").readText())
    }
    @Test fun `parent traversal cannot overwrite a sibling file`() {
        val sentinel = temporary.newFile("sentinel.txt").apply { writeText("unchanged") }
        rejects(archive("../sentinel.txt" to "changed"))
        assertEquals("unchanged", sentinel.readText())
    }
    @Test fun `absolute drive and backslash traversal paths are rejected`() {
        listOf("/absolute.json", "C:/absolute.json", "..\\sentinel.txt", "safe/../../escape.json").forEach {
            rejects(archive(it to "{}"))
        }
    }
    @Test fun `symlink parents cannot escape the extraction root`() {
        val destination = temporary.newFolder()
        val outside = temporary.newFolder()
        Files.createSymbolicLink(File(destination, "link").toPath(), outside.toPath())
        assertThrows(UnsafeArchiveException::class.java) {
            unzip(ByteArrayInputStream(archive("link/escape.json" to "{}")), destination)
        }
        assertFalse(File(outside, "escape.json").exists())
    }
    @Test fun `inflated entry limit is enforced even without declared sizes`() = rejects(
        archive("data.json" to "x".repeat(64)), ZipExtractionLimits(maxEntryBytes = 32))
    @Test fun `directory entries cannot conceal an inflated payload`() = rejects(
        archive("folder/" to "x".repeat(64)), ZipExtractionLimits(maxEntryBytes = 32))
    @Test fun `total inflated bytes and entry counts are bounded`() {
        rejects(archive("a" to "1234", "b" to "5678"), ZipExtractionLimits(maxTotalBytes = 7))
        rejects(archive("a" to "1", "b" to "2"), ZipExtractionLimits(maxEntries = 1))
    }
    @Test fun `different spellings cannot overwrite the same extracted file`() = rejects(
        archive("folder/data.json" to "first", "folder/./data.json" to "second"))
}
