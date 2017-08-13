/**
 * Copyright 2016 - 2017 JetBrains s.r.o.
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
package jetbrains.rsynk.files

import jetbrains.rsynk.exitvalues.InvalidFileException
import jetbrains.rsynk.exitvalues.NotSupportedException
import java.io.File

class FileResolver(private val fileInfoReader: FileInfoReader,
                   private val trackedFilesProvider: TrackedFilesProvider) {

    companion object {
        private val wildcardsInPathPattern = Regex(".*[\\[*?].*")
    }

    fun resolve(paths: List<String>): List<RsynkFileWithInfo> {

        if (paths.any { wildcardsInPathPattern.matches(it) }) {
            throw NotSupportedException("Received files list ${paths.joinToString(separator = ", ")} " +
                    "has at least one file with wildcard (paths expanding is not supported)")
        }

        val trackedFiles = trackedFilesProvider.getTrackedFiles().map { it.file.absolutePath to it }.toMap()
        return paths.map {
            val path = File(it).absolutePath
            val trackedFile = trackedFiles[path] ?: throw InvalidFileException("File $path is missing among files tracked by rsynk")
            val fileInfo = fileInfoReader.getFileInfo(trackedFile.file.toPath())
            RsynkFileWithInfo(trackedFile, fileInfo)
        }
    }
}
