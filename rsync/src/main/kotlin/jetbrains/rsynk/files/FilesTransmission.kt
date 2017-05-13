package jetbrains.rsynk.files

import jetbrains.rsynk.exitvalues.InvalidFileException
import jetbrains.rsynk.extensions.use
import mu.KLogging
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path


class TransmissionFileRepresentation internal constructor(private val filePath: Path,
                                                          private val fileSize: Long,
                                                          private val windowLength: Int,
                                                          bufferSize: Int) : AutoCloseable {

    companion object : KLogging()

    val markedBytesCount: Int
        get() = offset - getSmallestOffset()

    val totalBytes: Int
        get() = endOffset - getSmallestOffset() + 1

    var offset: Int = 0
        private set

    var markOffset = -1
        private set

    var endOffset: Int = offset + windowLength - 1
        private set

    val bytes = ByteArray(bufferSize)

    private val input = Files.newInputStream(filePath)
    private var readOffset = -1
    private var remainingBytes = fileSize

    init {
        if (fileSize == 0L) {
            throw IllegalArgumentException("File $filePath size is 0: transmission of empty files should be avoided")
        }

        if (windowLength > bufferSize) {
            throw IllegalArgumentException("Window size bust be less than or equal to buffer size")
        }

        slide(0)
    }

    fun slide(on: Int) {

        offset += on

        val prefetchedBytesCount = (readOffset - offset + 1)
        val currentWindow = Math.min(windowLength, (fileSize - (readOffset + 1) + prefetchedBytesCount).toInt())
        val bytesToRead = currentWindow - prefetchedBytesCount

        if (bytesToRead > 0) {
            val inBufferAvailable = (bytes.size - 1 - readOffset)
            if (bytesToRead > inBufferAvailable) {
                shrinkReadBytesInBuffer()
            }
            try {
                readInput(bytesToRead, minOf(/*buf space available*/bytes.size - 1 - readOffset, /*remaining*/(fileSize - (readOffset + 1)).toInt()))
            } catch (e: IOException) {
                TODO()
            } catch (t: Throwable) {
                throw InvalidFileException("Cannot slide $filePath bytes: ${t.message}")
            }
        }

        endOffset = offset + currentWindow - 1
    }


    fun setMarkOffsetRelativetlyToStart(relativeOffset: Int) {
        markOffset = offset + relativeOffset
    }

    private fun readInput(min: Int, max: Int) {

        var read = 0

        while (read < min) {
            val bytesRead = input.read(bytes, readOffset + 1, max - read)

            if (bytesRead <= 0) {
                throw InvalidFileException("File $filePath ended prematurely")
            }
            read += bytesRead
            readOffset += read
            remainingBytes -= read
            if (remainingBytes < 0) {
                logger.debug { "The amount of remained bytes is negative! ($remainingBytes)" }
            }
        }
    }

    private fun shrinkReadBytesInBuffer() {
        val shift = getSmallestOffset()
        val bytesMarked = offset - shift
        val bytesPrefetched = readOffset - offset + 1

        val shiftsCount = bytesMarked + bytesPrefetched
        System.arraycopy(bytes, shift, bytes, 0, shiftsCount)

        offset -= shift
        readOffset -= shift
        endOffset -= shift
        if (markOffset >= 0) {
            markOffset -= shift
        }
    }

    fun getSmallestOffset() = if (markOffset >= 0) minOf(markOffset, offset) else offset

    override fun close() {
        input.close()
    }
}

class FilesTransmission {
    companion object {
        val defaultBlockSize = 8 * 1024
    }

    fun <T> runWithOpenedFile(filePath: Path,
                              fileSize: Long,
                              windowLength: Int,
                              bufferSize: Int,
                              action: (TransmissionFileRepresentation) -> T): T {
        TransmissionFileRepresentation(filePath, fileSize, windowLength, bufferSize).use { file ->
            return action(file)
        }
    }
}
