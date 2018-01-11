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

import java.util.*

internal sealed class FilesListsIndex(val code: Int) {
    object done : FilesListsIndex(-1)
    object eof : FilesListsIndex(-2)
    object delStats : FilesListsIndex(-3)
    object offset : FilesListsIndex(-101)
}

internal data class FilesListBlock(val rootDirectory: FileInfo?,
                          val files: Map<Int, FileInfo>,
                          val begin: Int,
                          val end: Int,
                          val filesSize: Long) : Comparable<FilesListBlock> {

    override fun compareTo(other: FilesListBlock): Int {
        return begin.compareTo(other.begin)
    }
}

internal class FilesListBlocks(private val isRecursive: Boolean) {

    private val blocks = ArrayList<FilesListBlock>()
    private val stubDirectories = TreeMap<Int, FileInfo>()

    private var nextStubDirIndex: Int = 0
    private var nextDirIndex: Int

    init {
        nextDirIndex = if (isRecursive) 0 else -1
    }

    val hasStubDirs: Boolean
        get() = stubDirectories.isNotEmpty()

    fun addFileBlock(root: FileInfo?, filesList: List<FileInfo>): FilesListBlock {
        if (isRecursive) {
            filesList.filter { it.isDirectory }
                    .sorted()
                    .forEach { dir ->
                        if (dir.isNotDotDir) {
                            stubDirectories[nextStubDirIndex] = dir
                        }
                        nextStubDirIndex++
                    }
        }

        val startIndex = nextDirIndex
        val lastIndex = startIndex + filesList.size

        val indexToFile = TreeMap<Int, FileInfo>()
        (startIndex + 1..lastIndex).zip(filesList.sorted()).toMap(indexToFile)        //TODO prune duplicates

        val filesSize = filesList.filter { it.isSymlink || it.isReqularFile }.fold(0L, { sum, file -> sum + file.size })
        val block = FilesListBlock(root, indexToFile, startIndex, lastIndex, filesSize)
        blocks.add(block)

        nextDirIndex = lastIndex + 1
        return block
    }


    fun popStubDir(index: Int): FileInfo? {
        val foundDir = stubDirectories[index] ?: return null
        stubDirectories.remove(index)
        return foundDir
    }

    fun popBlock(): FilesListBlock? {
        if (blocks.isEmpty()) {
            return null
        }
        return blocks.removeAt(0)
    }

    fun peekBlock(i: Int): FilesListBlock? = blocks.getOrNull(i)

    fun getTotalFilesSizeBytes(): Long {
        return blocks.fold(0L) { sum, block -> sum + block.filesSize }
    }
}
