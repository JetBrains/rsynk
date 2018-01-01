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
package jetbrains.rsynk.rsync

import jetbrains.rsynk.rsync.files.FileInfoReader
import jetbrains.rsynk.rsync.files.RsynkFile
import jetbrains.rsynk.rsync.files.RsynkFileBoundaries
import jetbrains.rsynk.rsync.files.UnixDefaultFileSystemInfo
import org.junit.Assert
import org.junit.Assume
import org.junit.Test
import java.nio.file.Files

class FileInfoReaderTest {

    private val unixFS = UnixDefaultFileSystemInfo()

    @Test
    fun read_directory_info_test() {
        Assume.assumeFalse(OS.isWindows)

        val dirPath = Files.createTempDirectory("test-dir")
        val reader = FileInfoReader(unixFS)
        val dirFileInfo = reader.getFileInfo(RsynkFile(dirPath.toString(), { RsynkFileBoundaries(0, 0) }))

        Assert.assertTrue(dirFileInfo.isDirectory)

        Assert.assertFalse(dirFileInfo.isBlockDevice)
        Assert.assertFalse(dirFileInfo.isSocket)
        Assert.assertFalse(dirFileInfo.isSymlink)
        Assert.assertFalse(dirFileInfo.isBlockDevice)
        Assert.assertFalse(dirFileInfo.isCharacterDevice)
        Assert.assertFalse(dirFileInfo.isFIFO)
        Assert.assertFalse(dirFileInfo.isReqularFile)
    }

    @Test
    fun read_file_info_test() {
        Assume.assumeFalse(OS.isWindows)

        val dirPath = Files.createTempFile("test-file", ".hoho")
        val reader = FileInfoReader(unixFS)
        val dirFileInfo = reader.getFileInfo(RsynkFile(dirPath.toString(), { RsynkFileBoundaries(0, 0) }))

        Assert.assertTrue(dirFileInfo.isReqularFile)

        Assert.assertFalse(dirFileInfo.isBlockDevice)
        Assert.assertFalse(dirFileInfo.isSocket)
        Assert.assertFalse(dirFileInfo.isSymlink)
        Assert.assertFalse(dirFileInfo.isBlockDevice)
        Assert.assertFalse(dirFileInfo.isCharacterDevice)
        Assert.assertFalse(dirFileInfo.isFIFO)
        Assert.assertFalse(dirFileInfo.isDirectory)
    }
}
