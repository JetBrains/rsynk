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
package jetbrains.rsynk.rsync.files

import jetbrains.rsynk.rsync.exitvalues.InvalidFileException
import jetbrains.rsynk.rsync.exitvalues.NotSupportedException
import java.io.File
import java.util.*

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

        // TODO: there all subfiles of directory should be included in result
        val trackedFiles = trackedFilesProvider.resolve(paths.map { it.dropLastWhile { it =='/' } })
        val result = ArrayList<RsynkFileWithInfo>()
        for (p in paths) {
            val f = File(p.dropLastWhile { it == '/' })

            val trackedFile = trackedFiles[f.absolutePath] ?: throw InvalidFileException("File ${f.absolutePath} is missing among files tracked by rsynk")
            val fileInfo = fileInfoReader.getFileInfo(trackedFile)

            if (fileInfo.isDirectory) {
                for (fChild in f.listFiles()) {
                    // TODO: all files with subdirectories should be resolved only once!
                    val trackedChild = trackedFilesProvider.resolve(fChild.absolutePath) ?: throw InvalidFileException("File ${fChild.absolutePath} is missing among files tracked by rsynk")
                    val childFileInfo = fileInfoReader.getFileInfo(trackedChild)
                    result += RsynkFileWithInfo(trackedChild, childFileInfo)
                }
            }
            result += RsynkFileWithInfo(trackedFile, fileInfo)

        }
        return result
    }
}
