package jetbrains.rsynk

import jetbrains.rsynk.files.FileInfo
import jetbrains.rsynk.files.FileListsBlocks
import jetbrains.rsynk.files.Group
import jetbrains.rsynk.files.User
import org.junit.Assert
import org.junit.Test
import java.nio.file.Paths

class FileListBlocksTest {

    companion object Files {

        private val user = User("nobody", 0)
        private val group = Group("nobody", 0)
        private val regularFileMode = 33416
        private val directoryMode = 17161

        val dotDirectory = FileInfo(Paths.get("."), directoryMode, 0, 0, User("nobody", 0), Group("nobody", 0))
        val directory = FileInfo(Paths.get("haha"), directoryMode, 42, System.currentTimeMillis(), user, group)
        val file = FileInfo(Paths.get("haha/hoho"), regularFileMode, 42, System.currentTimeMillis(), user, group)
    }

    @Test
    fun preserve_index_for_not_null_root_non_recursive_test() {
        val blocks = FileListsBlocks(false)
        blocks.addFileBlock(Files.directory, listOf(file))
        val block = blocks.popBlock() ?: throw AssertionError("Block must not be null")
        Assert.assertEquals(Files.directory, block.rootDirectory)

        val blockFiles = block.files
        Assert.assertEquals(1, blockFiles.size)
        Assert.assertEquals(Files.file, blockFiles[0])
    }

    @Test
    fun preserve_index_for_null_root_non_recursive_test() {
        val blocks = FileListsBlocks(false)
        blocks.addFileBlock(null, listOf(file))
        val block = blocks.popBlock() ?: throw AssertionError("Block must not be null")
        Assert.assertNull(block.rootDirectory)

        val blockFiles = block.files
        Assert.assertEquals(1, blockFiles.size)
        Assert.assertEquals(Files.file, blockFiles[0])
    }

    @Test
    fun directory_index_is_minus_1_for_non_recursive_mode_test() {
        val blocks = FileListsBlocks(false)
        blocks.addFileBlock(null, listOf(Files.file))
        val blockFiles = blocks.popBlock()?.files ?: throw AssertionError("Block must not be null")
        Assert.assertEquals(1, blockFiles.size)
        Assert.assertEquals(Files.file, blockFiles[0])
    }

    @Test
    fun first_directory_index_is_zero_for_recursive_mode_test() {
        val blocks = FileListsBlocks(true)
        blocks.addFileBlock(null, listOf(Files.file))
        val blockFiles = blocks.popBlock()?.files ?: throw AssertionError("Block must not be null")
        Assert.assertEquals(1, blockFiles.size)
        Assert.assertEquals(Files.file, blockFiles[1])
    }

    @Test
    fun empty_stub_directories_on_non_recursive_mode_test() {
        val blocks = FileListsBlocks(false)
        blocks.addFileBlock(Files.directory, listOf(Files.file))
        Assert.assertNull(blocks.popStubDir(0))
    }

    @Test
    fun not_empty_stub_directories_on_recursive_mode_test() {
        val blocks = FileListsBlocks(true)
        blocks.addFileBlock(null, listOf(Files.directory, Files.file))
        Assert.assertEquals(Files.directory, blocks.popStubDir(0))
    }

    @Test
    fun do_not_include_dot_dir_in_stub_dirs_test() {
        val blocks = FileListsBlocks(true)
        blocks.addFileBlock(null, listOf(Files.dotDirectory, Files.file))
        Assert.assertNull(blocks.popStubDir(0))
    }
}
