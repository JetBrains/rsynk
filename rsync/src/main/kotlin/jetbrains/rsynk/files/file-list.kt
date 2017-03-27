package jetbrains.rsynk.files

import java.util.*


private data class FileListBlock(val rootDirectory: FileInfo?,
                                 val files: Map<Int, FileInfo>,
                                 val begin: Int,
                                 val end: Int,
                                 val filesSize: Long) : Comparable<Int> {
    override fun compareTo(other: Int): Int {
        throw UnsupportedOperationException("not implemented")
    }
}

class FileList(private val isRecursive: Boolean) {

    private val blocks = ArrayList<FileListBlock>()

    private val stubDirectories: MutableMap<Int, FileInfo>?
    private var stubDirectoryIndex: Int = 0

    private var nextDirIndex: Int

    init {
        if (isRecursive) {
            stubDirectories = TreeMap<Int, FileInfo>()
            nextDirIndex = 0
        } else {
            stubDirectories = null
            nextDirIndex = -1
        }
    }

    fun addFileBlock(root: FileInfo?, fileList: List<FileInfo>) {

        if (isRecursive && stubDirectories != null) {
            fileList.filter { it.isDirectory }.sorted().forEach { dir ->
                if (dir.isNotDotDir) {
                    stubDirectories[stubDirectoryIndex] = dir
                }
                stubDirectoryIndex++
            }
        }

        val startIndex = nextDirIndex + 1 //reserve index for root
        val lastIndex = startIndex + fileList.size

        nextDirIndex = lastIndex
        val map = TreeMap<Int, FileInfo>()
        val blockSize = (startIndex..lastIndex).zip(fileList.sorted()).toMap(map)        //TODO prune duplicates
        val size = fileList.filter { it.isSymlink || it.isReqularFile }.foldRight(0, { file, sum: Long -> sum + file.size })
        val block = FileListBlock(root, blockSize, startIndex, lastIndex, size)
    }
}
