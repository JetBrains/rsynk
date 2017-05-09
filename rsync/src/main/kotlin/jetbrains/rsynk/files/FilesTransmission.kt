package jetbrains.rsynk.files

import jetbrains.rsynk.exitvalues.InvalidFileException
import jetbrains.rsynk.extensions.use
import mu.KLogging
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path


class TransmissionFileRepresentation(private val filePath: Path,
                                     private val fileSize: Long,
                                     windowLength: Int,
                                     bufferSize: Int) : AutoCloseable {

    companion object : KLogging()

    val markedBytesCount: Int
        get() = offset - getSmallestOffset()

    val totalBytes: Int
        get() = (fileSize - getSmallestOffset() + 1).toInt()

    var windowLength: Int = windowLength
        private set

    var offset: Int = 0
        private set

    var markOffset = -1
        private set

    var endOffset: Int = 0
        private set

    val bytes = ByteArray(bufferSize)

    private val input = Files.newInputStream(filePath)
    private var readOffset = -1

    init {
        if (fileSize == 0L) {
            throw IllegalArgumentException("File $filePath size is 0: transmission of empty files should be avoided")
        }
        slide(0)
    }

    fun slide(on: Int) {

        offset += on

        val prefetchedBytesCount = (readOffset - offset + 1)
        val window = Math.min(windowLength, (fileSize - readOffset + prefetchedBytesCount).toInt())
        val bytesToRead = window - prefetchedBytesCount

        val inBufferAvailable = (bytes.size - 1 - readOffset)
        if (bytesToRead > 0) {
            if (bytesToRead > inBufferAvailable) {
                shrinkReadBytes()
            }
            try {
                read(bytesToRead, Math.min(inBufferAvailable, (fileSize - readOffset).toInt()))
            } catch (e: IOException) {
                TODO()
            } catch (t: Throwable) {
                throw InvalidFileException("Cannot slide $filePath bytes: ${t.message}")
            }
        }
    }


    fun setMarkOffsetRelativeltyToStart(relativeOffset: Int) {
        markOffset = offset + relativeOffset
    }

    private fun read(min: Int, max: Int) {

        var read = 0

        while (read < min) {
            val bytesRead = input.read(bytes, readOffset + 1, max - read)

            if (bytesRead <= 0) {
                throw InvalidFileException("File $filePath ended prematurely")
            }
            read += bytesRead
            readOffset += read

            val bytesRemain = fileSize - readOffset
            if (bytesRemain < 0) {
                logger.debug { "The amount of remained bytes is negative! ($bytesRemain)" }
            }
        }
    }

    private fun shrinkReadBytes() {
        val shift = minOf(offset, maxOf(markOffset, 0))
        val bytesMarked = offset - shift
        val bytesPrefetched = readOffset - offset + 1

        val shiftsCount = bytesMarked + bytesPrefetched
        System.arraycopy(bytes, shift, bytes, 0, shiftsCount)

        offset -= shift
        readOffset -= shift
        if (markOffset >= 0) {
            markOffset -= shift
        }
    }

    fun getSmallestOffset() = minOf(offset, maxOf(markOffset, 0))

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
