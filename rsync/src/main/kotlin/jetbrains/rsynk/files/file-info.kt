package jetbrains.rsynk.files

data class User(
        val name: String,
        val uid: Int
)

data class Group(
        val name: String,
        val gid: Int
)

data class FileInfo(
        val mode: Int,
        val size: Long,
        val lastModified: Long,
        val user: User,
        val group: Group
) {
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
}


