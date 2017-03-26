package jetbrains.rsynk.files

import jetbrains.rsynk.options.RequestOptions
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.TimeUnit


internal object FileBitmasks {
    val S_IFLNK = 40960
    val S_IFREG = 32768
    val S_IFDIR = 16384
    val S_IFUNK = 53248
}

class FileInfoReader(private val fs: FileSystemInfo) {

    fun getFileInfo(file: File, options: RequestOptions): FileInfo {

        val attributes = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
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
            return FileBitmasks.S_IFDIR or fs.defaultDirPermission
        }
        if (attributes.isRegularFile) {
            return FileBitmasks.S_IFREG or fs.defaultFilePermission
        }
        if (attributes.isSymbolicLink) {
            FileBitmasks.S_IFLNK or fs.defaultFilePermission
        }
        return FileBitmasks.S_IFUNK
    }
}
