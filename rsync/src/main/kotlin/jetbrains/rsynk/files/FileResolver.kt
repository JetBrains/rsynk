package jetbrains.rsynk.files

import jetbrains.rsynk.exitvalues.InvalidFileException
import jetbrains.rsynk.exitvalues.NotSupportedException
import java.io.File
import java.nio.file.Path

class FileResolver(private val fileInfoReader: FileInfoReader,
                   trackingFilesProvider: TrackingFilesProvider) {

    companion object {
        private val wildcardsInPathPattern = Regex(".*[\\[*?].*")
    }

    private val trackingFiles: Map<Path, RsynkFile>

    init {
        trackingFiles = trackingFilesProvider.getTrackkngFiles().map { Pair(fileToPath(it.file), it) }.toMap()
    }

    fun resolve(paths: List<String>): List<RsynkFileInfoWithBoundaries> {

        if (paths.any { wildcardsInPathPattern.matches(it) }) {
            throw NotSupportedException("Received files list ${paths.joinToString(separator = ", ")} " +
                    "has at least one file with wildcard (paths expanding is not supported)")
        }

        return paths.map {
            val path = fileToPath(File(it))
            val trackingFile = trackingFiles[path] ?: throw InvalidFileException("File $path is missing among files tracking by rsynk")
            val fileInfo = fileInfoReader.getFileInfo(path)
            RsynkFileInfoWithBoundaries(trackingFile, fileInfo)
        }
    }

    private fun fileToPath(file: File): Path = file.toPath().normalize()
}
