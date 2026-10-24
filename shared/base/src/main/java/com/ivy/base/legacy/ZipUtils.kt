package com.ivy.base.legacy

import android.content.Context
import android.net.Uri
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val MODE_WRITE = "w"
private const val MODE_READ = "r"

fun zip(context: Context, zipFile: Uri, files: List<File>) {
    context.contentResolver.openFileDescriptor(zipFile, MODE_WRITE).use { descriptor ->
        descriptor?.fileDescriptor?.let {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(it))).use { outStream ->
                zip(outStream, files)
            }
        }
    }
}

private fun zip(
    outStream: ZipOutputStream,
    files: List<File>,
    includeParentFolder: Boolean = false
) {
    files.forEach { file ->
        if (file.isDirectory) {
            file.mkdir()
            zip(outStream, file.listFiles()?.toList() ?: emptyList(), includeParentFolder = true)
        } else {
            val fileLoc: String =
                if (file.parent.isNullOrEmpty() || !includeParentFolder) {
                    file.name
                } else {
                    (file.parent!!).substring(
                        file.parent!!.lastIndexOf("/")
                    ) + "/" + file.name
                }

            outStream.putNextEntry(ZipEntry(fileLoc))
            BufferedInputStream(FileInputStream(file)).use { inStream ->
                inStream.copyTo(outStream)
            }
        }
    }
}

fun unzip(context: Context, zipFile: Uri, location: File) {
    context.contentResolver.openFileDescriptor(zipFile, MODE_READ).use { descriptor ->
        descriptor?.fileDescriptor?.let {
            unzip(FileInputStream(it), location)
        }
    }
}

/** Limits are based on bytes actually inflated, not attacker-controlled ZIP metadata. */
data class ZipExtractionLimits(
    val maxEntries: Int = 512,
    val maxEntryBytes: Long = 64L * 1024 * 1024,
    val maxTotalBytes: Long = 128L * 1024 * 1024,
) {
    init { require(maxEntries > 0 && maxEntryBytes > 0 && maxTotalBytes > 0) }
}

class UnsafeArchiveException(message: String) : IOException(message)

fun unzip(fileInputStream: InputStream, location: File, limits: ZipExtractionLimits = ZipExtractionLimits()) {
    ZipInputStream(BufferedInputStream(fileInputStream)).use { zipInputStream ->
        if (location.exists() && !location.isDirectory) {
            throw IllegalStateException("Location file must be directory or not exist")
        }

        if (!location.isDirectory && !location.mkdirs()) throw IOException("Cannot create the backup extraction folder.")
        val root = location.canonicalFile
        val rootPrefix = root.path + File.separator
        val written = mutableSetOf<String>()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var entries = 0
        var totalBytes = 0L
        while (true) {
            val entry = zipInputStream.nextEntry ?: break
            if (++entries > limits.maxEntries) throw UnsafeArchiveException("The backup archive contains too many entries.")
            val name = entry.name.replace('\\', '/')
            if (name.isBlank() || name.startsWith('/') || Regex("^[A-Za-z]:").containsMatchIn(name) ||
                name.split('/').any { it == ".." } || '\u0000' in name) {
                throw UnsafeArchiveException("The backup archive contains an unsafe file path.")
            }
            // Canonicalization also rejects paths through an existing symbolic link.
            val target = File(root, name).canonicalFile
            if (!target.path.startsWith(rootPrefix)) throw UnsafeArchiveException("A backup file escapes its extraction folder.")
            if (entry.isDirectory) {
                if (!target.isDirectory && !target.mkdirs()) throw IOException("Cannot create a backup subfolder.")
                if (zipInputStream.read() != -1) throw UnsafeArchiveException("A backup directory entry contains unexpected data.")
            } else {
                if (!written.add(target.path) || target.exists()) throw UnsafeArchiveException("The backup archive contains a duplicate file.")
                if (entry.size > limits.maxEntryBytes) throw UnsafeArchiveException("A backup file exceeds the extraction size limit.")
                val parent = target.parentFile ?: throw UnsafeArchiveException("Invalid backup file path.")
                if (!parent.isDirectory && !parent.mkdirs()) throw IOException("Cannot create a backup subfolder.")
                var entryBytes = 0L
                BufferedOutputStream(FileOutputStream(target)).use { output ->
                    while (true) {
                        val count = zipInputStream.read(buffer)
                        if (count < 0) break
                        entryBytes += count
                        totalBytes += count
                        if (entryBytes > limits.maxEntryBytes || totalBytes > limits.maxTotalBytes) {
                            throw UnsafeArchiveException("The backup archive exceeds the extraction size limit.")
                        }
                        output.write(buffer, 0, count)
                    }
                }
            }
            zipInputStream.closeEntry()
        }
    }
}
