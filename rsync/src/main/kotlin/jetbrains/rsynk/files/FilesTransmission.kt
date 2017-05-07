package jetbrains.rsynk.files

import jetbrains.rsynk.exitvalues.InvalidFileException
import jetbrains.rsynk.extensions.use
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path


class TransmissionFileRepresentation(private val filePath: Path,
                                     private val fileSize: Long,
                                     windowLength: Int,
                                     bufferSize: Int) : AutoCloseable {

    var windowLength: Int = windowLength
        private set

    var offset: Int = 0
        private set

    val bytes = ByteArray(bufferSize)

    private val input = Files.newInputStream(filePath)
    private var readOffset = -1
    private var markOffset = -1

    init {
        if (fileSize == 0L) {
            throw IllegalArgumentException("File $filePath size is 0: transmission of empty files should be avoided")
        }
        slide(0)
    }

    fun slide(on: Int) {

        offset += on

        val prefetchedBytesCount = (readOffset - offset + 1)
        val window = Math.min(windowLength, (fileSize - offset + prefetchedBytesCount).toInt())
        val bytesToRead = window - prefetchedBytesCount

        val inBufferAvailable = (bytes.size - 1 - readOffset)
        if(bytesToRead > 0) {
            if (bytesToRead > inBufferAvailable) {
                TODO()
            }
            try {
                read(bytesToRead, Math.min(inBufferAvailable, (fileSize - offset).toInt()))
            } catch (e: IOException) {
                TODO()
            } catch (t: Throwable) {
                throw InvalidFileException("Cannot slide $filePath bytes: ${t.message}")
            }
        }
    }

    private fun read(min: Int, max:Int) {
        TODO()
    }

    override fun close() {
        input.close()
    }
}

class FilesTransmission {
    companion object {
        val defaultBlockSize = 8L * 1024L
        val defaultBlockFactor = defaultBlockSize
    }

    fun <T> runWithOpenedFile(filePath: Path,
                              fileSize: Long,
                              windowLength: Int,
                              bufferSize: Int,
                              action: (TransmissionFileRepresentation) -> T): T {
        TransmissionFileRepresentation(filePath,  fileSize, windowLength, bufferSize).use { file ->
            return action(file)
        }
    }
}
