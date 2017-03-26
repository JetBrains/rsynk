package jetbrains.rsynk.files

interface FileSystemInfo {
    val defaultUser: User
    val defaultGroup: Group
    val defaultFilePermission: Int
    val defaultDirPermission: Int
}

class UnixDefaultFileSystemInfo : FileSystemInfo {

    private val nobodyId = Math.pow(2.0, 16.0).toInt() - 1 - 1
    private val umask: Int

    init {
        val umaskText = System.getProperty("umask") ?: "0022"
        umask = umaskText.toInt(8)
    }

    override val defaultUser: User
        get() = User("nobody", nobodyId)

    override val defaultGroup: Group
        get() = Group("nobody", nobodyId)

    override val defaultFilePermission: Int
        get() = 666 and umask.inv()

    override val defaultDirPermission: Int
        get() = 777 and umask.inv()
}

