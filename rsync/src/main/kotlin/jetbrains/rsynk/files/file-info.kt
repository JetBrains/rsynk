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
)
