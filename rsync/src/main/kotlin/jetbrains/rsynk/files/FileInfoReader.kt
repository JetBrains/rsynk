package jetbrains.rsynk.files

import jetbrains.rsynk.options.RequestOptions
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.TimeUnit


internal object FileBitmasks {
    val FileTypeBitMask = 61440

    val Socket = 49152
    val SymLink = 40960
    val RegularFile = 32768
    val BlockDevice = 24576
    val Directory = 16384
    val CharacterDevice = 8192
    val FIFO = 4096

    val Other = 53248
}

class FileInfoReader(private val fs: FileSystemInfo) {

    fun getFileInfo(file: Path, options: RequestOptions): FileInfo {

        val attributes = Files.readAttributes(file, BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
        val mode = getFileMode(attributes)

        return FileInfo(
                mode,
                attributes.size(),
                attributes.lastModifiedTime().to(TimeUnit.SECONDS),
                fs.defaultUser,
                fs.defaultGroup
        )
    }

    private fun getFileMode(attributes: BasicFileAttributes): Int {
        if (attributes.isDirectory) {
            return FileBitmasks.Directory or fs.defaultDirPermission
        }
        if (attributes.isRegularFile) {
            return FileBitmasks.RegularFile or fs.defaultFilePermission
        }
        if (attributes.isSymbolicLink) {
            FileBitmasks.SymLink or fs.defaultFilePermission
        }
        return FileBitmasks.Other
    }
}
