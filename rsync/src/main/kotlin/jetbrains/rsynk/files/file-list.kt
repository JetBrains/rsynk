package jetbrains.rsynk.files

import java.util.*


data class FileListBlock(val rootDirectory: FileInfo?,
                         val files: Map<Int, FileInfo>,
                         val begin: Int,
                         val end: Int,
                         val filesSize: Long) : Comparable<FileListBlock> {
    override fun compareTo(other: FileListBlock): Int {
        return begin.compareTo(other.begin)
    }
}

class FileList(private val isRecursive: Boolean) {

    private val blocks = ArrayList<FileListBlock>()
    private val stubDirectories = TreeMap<Int, FileInfo>()

    private var stubDirectoryIndex: Int = 0
    private var nextDirIndex: Int

    init {
        nextDirIndex = if (isRecursive) 0 else -1
    }

    fun addFileBlock(root: FileInfo?, fileList: List<FileInfo>): FileListBlock {

        if (isRecursive) {
            fileList.filter { it.isDirectory }.sorted().forEach { dir ->
                if (dir.isNotDotDir) {
                    stubDirectories[stubDirectoryIndex] = dir
                }
                stubDirectoryIndex++
            }
        }

        val startIndex = nextDirIndex + 1 // preserve index for root (even if it's null)
        val lastIndex = startIndex + fileList.size

        val map = TreeMap<Int, FileInfo>()
        val blockSize = (startIndex..lastIndex).zip(fileList.sorted()).toMap(map)        //TODO prune duplicates
        val size = fileList.filter { it.isSymlink || it.isReqularFile }.foldRight(0, { file, sum: Long -> sum + file.size })
        val block = FileListBlock(root, blockSize, startIndex, lastIndex, size)
        blocks.add(block)

        nextDirIndex = lastIndex + 1
        return block
    }
}
