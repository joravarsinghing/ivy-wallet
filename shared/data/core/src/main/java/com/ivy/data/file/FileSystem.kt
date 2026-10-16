package com.ivy.data.file

import android.content.Context
import android.net.Uri
import arrow.core.Either
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.Charset
import javax.inject.Inject

class FileSystem @Inject constructor(
    @ApplicationContext
    private val appContext: Context
) {
    fun writeToFile(uri: Uri, content: String): Either<Failure, Unit> = try {
        val contentResolver = appContext.contentResolver

        contentResolver.openFileDescriptor(uri, "w")?.use {
            FileOutputStream(it.fileDescriptor).use { fOut ->
                val writer = fOut.writer(charset = Charsets.UTF_16)
                writer.write(content)
                writer.close()
            }
        }
        Either.Right(Unit)
    } catch (e: FileNotFoundException) {
        Either.Left(Failure.FileNotFound(e))
    } catch (e: Exception) {
        Either.Left(Failure.IO(e))
    }

    fun read(
        uri: Uri,
        charset: Charset = Charsets.UTF_8
    ): Either<Failure, String> {
        return try {
            val contentResolver = appContext.contentResolver
            var fileContent: String? = null

            contentResolver.openFileDescriptor(uri, "r")?.use {
                FileInputStream(it.fileDescriptor).use { fileInputStream ->
                    fileContent = readFileContent(
                        fileInputStream = fileInputStream,
                        charset = charset
                    )
                }
            }

            Either.Right(fileContent!!)
        } catch (e: FileNotFoundException) {
            Either.Left(Failure.FileNotFound(e))
        } catch (e: Exception) {
            Either.Left(Failure.IO(e))
        }
    }

    /**
     * Reads text exported by another app without assuming its charset.
     *
     * Android file pickers do not guarantee a useful MIME type and many finance
     * apps export UTF-16 while others export UTF-8. Prefer a BOM when present,
     * otherwise choose the candidate with the fewest replacement/NUL characters.
     */
    fun readAuto(uri: Uri): Either<Failure, String> {
        return readBytes(uri).map { bytes -> decodeText(bytes) }
    }

    private fun readBytes(uri: Uri): Either<Failure, ByteArray> {
        return try {
            val contentResolver = appContext.contentResolver
            val bytes = contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                FileInputStream(descriptor.fileDescriptor).use { input -> input.readBytes() }
            } ?: throw FileNotFoundException("Could not open $uri")
            Either.Right(bytes)
        } catch (e: FileNotFoundException) {
            Either.Left(Failure.FileNotFound(e))
        } catch (e: Exception) {
            Either.Left(Failure.IO(e))
        }
    }

    private fun decodeText(bytes: ByteArray): String {
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return bytes.copyOfRange(2, bytes.size).toString(Charsets.UTF_16LE)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return bytes.copyOfRange(2, bytes.size).toString(Charsets.UTF_16BE)
        }
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
        ) {
            return bytes.copyOfRange(3, bytes.size).toString(Charsets.UTF_8)
        }

        return listOf(Charsets.UTF_8, Charsets.UTF_16LE, Charsets.UTF_16BE)
            .map { charset -> bytes.toString(charset) }
            .minBy { candidate ->
                candidate.count { it == '\uFFFD' } * 1000 +
                    candidate.count { it == '\u0000' } * 10
            }
            .trimStart('\uFEFF')
    }

    @Throws(IOException::class)
    private fun readFileContent(
        fileInputStream: FileInputStream,
        charset: Charset
    ): String {
        BufferedReader(InputStreamReader(fileInputStream, charset)).use { br ->
            val sb = StringBuilder()
            var line: String?
            while (br.readLine().also { line = it } != null) {
                sb.append(line)
                sb.append('\n')
            }
            return sb.toString()
        }
    }

    sealed interface Failure {
        val e: Throwable

        data class FileNotFound(override val e: Throwable) : Failure
        data class IO(override val e: Throwable) : Failure
    }
}
