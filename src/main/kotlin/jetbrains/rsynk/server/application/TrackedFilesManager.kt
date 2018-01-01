package jetbrains.rsynk.server.application

import jetbrains.rsynk.rsync.exitvalues.InvalidFileException
import jetbrains.rsynk.rsync.files.RsynkFile
import jetbrains.rsynk.rsync.files.TrackedFilesProvider
import mu.KLogging
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class TrackedFilesManager : FilesRegistry, TrackedFilesProvider {

    companion object : KLogging()

    private val pathToFile = HashMap<String, RsynkFile>()
    private val lock = ReentrantLock()

    override fun add(files: List<RsynkFile>) = lock.withLock {
        files.forEach { f ->
            pathToFile[f.file.absolutePath] = f
        }
    }

    override fun remove(files: List<String>) = lock.withLock {
        files.forEach { f ->
            pathToFile.remove(f) ?: logger.debug { "Unable to stop deregister '$f': files isn't tracked" }
        }
    }

    override fun removeAll() = lock.withLock {
        pathToFile.clear()
    }

    override fun resolve(paths: List<String>): Map<String, RsynkFile> = lock.withLock {
        val result = TreeMap<String, RsynkFile>()
        paths.forEach { p ->
            val resolvedFile = pathToFile[p] ?: throw InvalidFileException("Cannot resolve file '$p': file is not tracked")
            result[p] = resolvedFile
        }
        return result
    }
}
