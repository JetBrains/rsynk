/**
 * Copyright 2016 - 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.rsynk.server.application

import jetbrains.rsynk.rsync.exitvalues.InvalidFileException
import jetbrains.rsynk.rsync.files.RsynkFile
import jetbrains.rsynk.rsync.files.TrackedFilesProvider
import mu.KLogging
import java.io.File
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class TrackedFilesManager : FilesRegistry, TrackedFilesProvider {

    companion object : KLogging()

    private val pathToFile = HashMap<String, RsynkFile>()
    private val lock = ReentrantLock()

    override fun add(files: List<RsynkFile>) = lock.withLock {
        files.forEach { f ->
            pathToFile[getPath(f.path)] = f
        }
    }

    override fun remove(paths: List<String>) = lock.withLock {
        paths.forEach { p ->
            pathToFile.remove(getPath(p)) ?: logger.debug { "Unable to stop deregister '$p': file isn't tracked" }
        }
    }

    override fun removeAll() = lock.withLock {
        pathToFile.clear()
    }

    override fun resolve(paths: List<String>): Map<String, RsynkFile> = lock.withLock {
        val result = TreeMap<String, RsynkFile>()
        paths.forEach { p ->
            val resolvedPath = getPath(p)
            val resolvedFile = pathToFile[resolvedPath] ?: throw InvalidFileException("Cannot resolve file '${resolvedPath}': file is not tracked")
            result[resolvedPath] = resolvedFile
        }
        return result
    }

    private fun getPath(userPath: String): String = File(userPath).absolutePath
}
