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
package jetbrains.rsynk.rsync

import jetbrains.rsynk.rsync.files.FileInfo
import jetbrains.rsynk.rsync.files.FilesListBlocks
import jetbrains.rsynk.rsync.files.Group
import jetbrains.rsynk.rsync.files.User
import org.junit.Assert
import org.junit.Test
import java.nio.file.Paths

internal class FilesListBlocksTest {

    companion object Files {

        private val user = User("nobody", 0)
        private val group = Group("nobody", 0)
        private val regularFileMode = 33416
        private val directoryMode = 17161

        val dotDirectory = FileInfo(Paths.get("."), directoryMode, 0, 0, 0, User("nobody", 0), Group("nobody", 0))
        val directory = FileInfo(Paths.get("haha"), directoryMode, 0, 42, System.currentTimeMillis(), user, group)
        val file = FileInfo(Paths.get("haha/hoho"), regularFileMode, 0, 42, System.currentTimeMillis(), user, group)
    }

    @Test
    fun preserve_index_for_not_null_root_non_recursive_test() {
        val blocks = FilesListBlocks(false)
        blocks.addFileBlock(directory, listOf(file))
        val block = blocks.popBlock() ?: throw AssertionError("Block must not be null")
        Assert.assertEquals(directory, block.rootDirectory)

        val blockFiles = block.files
        Assert.assertEquals(1, blockFiles.size)
        Assert.assertEquals(file, blockFiles[0])
    }

    @Test
    fun preserve_index_for_null_root_non_recursive_test() {
        val blocks = FilesListBlocks(false)
        blocks.addFileBlock(null, listOf(file))
        val block = blocks.popBlock() ?: throw AssertionError("Block must not be null")
        Assert.assertNull(block.rootDirectory)

        val blockFiles = block.files
        Assert.assertEquals(1, blockFiles.size)
        Assert.assertEquals(file, blockFiles[0])
    }

    @Test
    fun directory_index_is_minus_1_for_non_recursive_mode_test() {
        val blocks = FilesListBlocks(false)
        blocks.addFileBlock(null, listOf(file))
        val blockFiles = blocks.popBlock()?.files ?: throw AssertionError("Block must not be null")
        Assert.assertEquals(1, blockFiles.size)
        Assert.assertEquals(file, blockFiles[0])
    }

    @Test
    fun first_directory_index_is_zero_for_recursive_mode_test() {
        val blocks = FilesListBlocks(true)
        blocks.addFileBlock(null, listOf(file))
        val blockFiles = blocks.popBlock()?.files ?: throw AssertionError("Block must not be null")
        Assert.assertEquals(1, blockFiles.size)
        Assert.assertEquals(file, blockFiles[1])
    }

    @Test
    fun empty_stub_directories_on_non_recursive_mode_test() {
        val blocks = FilesListBlocks(false)
        blocks.addFileBlock(directory, listOf(file))
        Assert.assertNull(blocks.popStubDir(0))
    }

    @Test
    fun not_empty_stub_directories_on_recursive_mode_test() {
        val blocks = FilesListBlocks(true)
        blocks.addFileBlock(null, listOf(directory, file))
        Assert.assertEquals(directory, blocks.popStubDir(0))
    }

    @Test
    fun do_not_include_dot_dir_in_stub_dirs_test() {
        val blocks = FilesListBlocks(true)
        blocks.addFileBlock(null, listOf(dotDirectory, file))
        Assert.assertNull(blocks.popStubDir(0))
    }
}
