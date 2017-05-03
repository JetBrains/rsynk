package jetbrains.rsynk.files

import java.util.*

sealed class FileListsCode(val code: Int) {
    object done : FileListsCode(-1)
    object eof : FileListsCode(-2)
    object offset : FileListsCode(-101)
}

data class FileListBlock(val rootDirectory: FileInfo?,
                         val files: Map<Int, FileInfo>,
                         val begin: Int,
                         val end: Int,
                         val filesSize: Long) : Comparable<FileListBlock> {

    private val deletedFiles = HashSet<Int>()

    //TODO: mark as sent and not deleted?
    fun markFileDeleted(index: Int) {
        if (index !in begin..end) {
            throw IndexOutOfBoundsException("Requested index $index is out of segment bounds [$begin, $end]")
        }
        deletedFiles += index
    }

    fun isFileDeleted(index: Int): Boolean {
        return index in deletedFiles
    }

    override fun compareTo(other: FileListBlock): Int {
        return begin.compareTo(other.begin)
    }
}

class FileListsBlocks(private val isRecursive: Boolean) {

    private val blocks = ArrayList<FileListBlock>()
    private val stubDirectories = TreeMap<Int, FileInfo>()

    private var nextStubDirIndex: Int = 0
    private var nextDirIndex: Int

    init {
        nextDirIndex = if (isRecursive) 0 else -1
    }

    val hasStubDirs: Boolean
        get() = stubDirectories.isNotEmpty()

    val blocksSize: Int
        get() = blocks.size

    fun addFileBlock(root: FileInfo?, fileList: List<FileInfo>): FileListBlock {
        if (isRecursive) {
            fileList.filter { it.isDirectory }
                    .sorted()
                    .forEach { dir ->
                        if (dir.isNotDotDir) {
                            stubDirectories[nextStubDirIndex] = dir
                        }
                        nextStubDirIndex++
                    }
        }

        val startIndex = nextDirIndex + 1 // preserve index for root (even if it's null)
        val lastIndex = startIndex + fileList.size

        val map = TreeMap<Int, FileInfo>()
        val blockSize = (startIndex..lastIndex).zip(fileList.sorted()).toMap(map)        //TODO prune duplicates
        val size = fileList
                .filter { it.isSymlink || it.isReqularFile }
                .foldRight(0, { file, sum: Long -> sum + file.size })
        val block = FileListBlock(root, blockSize, startIndex, lastIndex, size)
        blocks.add(block)

        nextDirIndex = lastIndex + 1
        return block
    }


    fun popStubDir(index: Int): FileInfo? {
        val foundDir = stubDirectories[index] ?: return null
        stubDirectories.remove(index)
        return foundDir
    }

    fun popBlock(): FileListBlock? {
        if (blocks.isEmpty()) {
            return null
        }
        return blocks.removeAt(0)
    }

    fun peekBlock(i: Int): FileListBlock? = blocks.getOrNull(i)

    fun isEmpty(): Boolean {
        return blocksSize == 0 && !hasStubDirs
    }

    fun isNotEmpty(): Boolean = !isEmpty()
}
