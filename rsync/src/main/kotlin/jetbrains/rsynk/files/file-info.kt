package jetbrains.rsynk.files

import java.nio.file.Path

data class User(
        val name: String,
        val uid: Int
)

data class Group(
        val name: String,
        val gid: Int
)

data class FileInfo(
        val path: Path,
        val mode: Int,
        val size: Long,
        val lastModified: Long,
        val user: User,
        val group: Group
): Comparable<FileInfo> {

    override fun compareTo(other: FileInfo): Int {
        throw UnsupportedOperationException("not implemented")
    }

    val isDirectory: Boolean
        get() = (mode and FileBitmasks.FileTypeBitMask) == FileBitmasks.Directory
    val isSocket: Boolean
        get() = (mode and FileBitmasks.FileTypeBitMask) == FileBitmasks.Socket
    val isSymlink: Boolean
        get() = (mode and FileBitmasks.FileTypeBitMask) == FileBitmasks.SymLink
    val isBlockDevice: Boolean
        get() = (mode and FileBitmasks.FileTypeBitMask) == FileBitmasks.BlockDevice
    val isCharacterDevice: Boolean
        get() = (mode and FileBitmasks.FileTypeBitMask) == FileBitmasks.CharacterDevice
    val isFIFO: Boolean
        get() = (mode and FileBitmasks.FileTypeBitMask) == FileBitmasks.FIFO
    val isReqularFile: Boolean
        get() = (mode and FileBitmasks.FileTypeBitMask) == FileBitmasks.RegularFile

    val isNotDotDir = path.nameCount == 1 && path.endsWith(".")
}


