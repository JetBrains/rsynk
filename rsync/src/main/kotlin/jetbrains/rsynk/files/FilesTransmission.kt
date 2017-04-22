package jetbrains.rsynk.files

import sun.security.util.Length
import java.nio.file.Path


class TransmissionFileRepresentation : AutoCloseable {
    override fun close() {
        TODO()
    }

}

class FilesTransmission(private val filePath: Path,
                        private val fileSize: Long,
                        private val blockSize: Long,
                        private val blockFactor: Long) {
    companion object {
        val defaultBlockSize = 8L * 1024L
        val defaultBlockFactor = defaultBlockSize
    }

    fun <T> runWithOpenedFile(action: (TransmissionFileRepresentation) -> T): T {
        TODO()
    }
}
