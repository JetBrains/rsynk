package jetbrains.rsynk.server.application

import jetbrains.rsynk.rsync.files.RsynkFile

interface FilesRegistry {
    fun add(files: List<RsynkFile>)

    fun remove(files: List<String>)

    fun removeAll()
}
