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

import jetbrains.rsynk.rsync.files.*
import org.junit.Assert
import org.junit.Test
import java.nio.file.Files

internal class FileInTransmission {

    @Test
    fun do_not_transmit_empty_file_test() = withFile(byteArrayOf()) { file ->
        FilesTransmission.runWithOpenedFile(file, 100, 200) { r ->
            Assert.assertEquals(0, r.array.size)
        }
    }

    @Test
    fun get_unchanged_bytes_test() = withFile(content) { file ->
        FilesTransmission.runWithOpenedFile(file, 100, 200) { tr ->
            Assert.assertEquals(100, tr.getTotalBytes())

            val bytes = tr.array
            Assert.assertEquals(200, bytes.size)
            Assert.assertArrayEquals(byteArrayOf(79, 110, 99, 101, 32, 97, 32, 98, 111, 121, 32, 97, 32, 82, 111, 115, 101,
                    98, 117, 100, 32, 115, 112, 105, 101, 100, 44, 10, 72, 101, 97, 116, 104, 114, 111, 115, 101, 32, 102,
                    97, 105, 114, 32, 97, 110, 100, 32, 116, 101, 110, 100, 101, 114, 44, 10, 65, 108, 108, 32, 97, 114,
                    114, 97, 121, 39, 100, 32, 105, 110, 32, 121, 111, 117, 116, 104, 102, 117, 108, 32, 112, 114, 105,
                    100, 101, 44, 45, 45, 10, 81, 117, 105, 99, 107, 108, 121, 32, 116, 111, 32, 116, 104, 101, 32, 115,
                    112, 111, 116, 32, 104, 101, 32, 104, 105, 101, 100, 44, 10, 82, 97, 118, 105, 115, 104, 101, 100, 32,
                    98, 121, 32, 104, 101, 114, 32, 115, 112, 108, 101, 110, 100, 111, 117, 114, 46, 10, 82, 111, 115, 101,
                    98, 117, 100, 44, 32, 114, 111, 115, 101, 98, 117, 100, 44, 32, 114, 111, 115, 101, 98, 117, 100, 32,
                    114, 101, 100, 44, 10, 72, 101, 97, 116, 104, 114, 111, 115, 101, 32, 102, 97, 105, 114, 32, 97, 110,
                    100, 32, 116, 101, 110, 100, 101, 114)
                    , bytes)
        }
    }

    @Test
    fun init_values_test() = withFile(content) { file ->
        FilesTransmission.runWithOpenedFile(file, 100, 200) { tr ->
            Assert.assertEquals(-1, tr.getMarkOffset())
            Assert.assertEquals(99, tr.getEndOffset())
            Assert.assertEquals(0, tr.getStartOffset())
            Assert.assertEquals(0, tr.getFirstOffset())
        }
    }

    @Test
    fun slide_values_margin_is_less_than_window_size_test() = withFile(content) { file ->
        FilesTransmission.runWithOpenedFile(file, 100, 200) { tr ->
            tr.slide(80)

            Assert.assertEquals(-1, tr.getMarkOffset())
            Assert.assertEquals(179, tr.getEndOffset())
            Assert.assertEquals(80, tr.getStartOffset())
            Assert.assertEquals(80, tr.getFirstOffset())
        }
    }

    @Test
    fun slide_values_margin_is_bigger_than_window_size_test() = withFile(content) { file ->
        FilesTransmission.runWithOpenedFile(file, 100, 200) { tr ->
            tr.slide(180)

            Assert.assertEquals(-1, tr.getMarkOffset())
            Assert.assertEquals(21, tr.getEndOffset())
            Assert.assertEquals(-1, tr.getMarkOffset())
            Assert.assertEquals(0, tr.getStartOffset())
            Assert.assertEquals(0, tr.getFirstOffset())
        }
    }

    @Test
    fun mark_offset_test() = withFile(content) { file ->
        FilesTransmission.runWithOpenedFile(file, 50, 60) { tr ->
            tr.setMarkOffsetRelativeToStart(10)
            Assert.assertArrayEquals(byteArrayOf(79, 110, 99, 101, 32, 97, 32, 98, 111, 121, 32,
                    97, 32, 82, 111, 115, 101, 98, 117, 100, 32, 115, 112, 105, 101, 100, 44, 10,
                    72, 101, 97, 116, 104, 114, 111, 115, 101, 32, 102, 97, 105, 114, 32, 97, 110,
                    100, 32, 116, 101, 110, 100, 101, 114, 44, 10, 65, 108, 108, 32, 97), tr.array)
        }
    }

    @Test
    fun shrink_read_bytes_test() = withFile(content) { file ->
        FilesTransmission.runWithOpenedFile(file, 20, 25) { tr ->
            tr.slide(20)
            tr.slide(20)
            Assert.assertArrayEquals(byteArrayOf(105, 114, 32, 97, 110, 100, 32, 116, 101, 110, 100, 101,
                    114, 44, 10, 65, 108, 108, 32, 97, 114, 114, 97, 121, 39), tr.array)
        }
    }

    @Test
    fun values_after_shrinking_test() = withFile(content) { file ->
        FilesTransmission.runWithOpenedFile(file, 20, 25) { tr ->
            tr.slide(18)
            tr.slide(17)

            Assert.assertEquals(-1, tr.getMarkOffset())
            Assert.assertEquals(19, tr.getEndOffset())
            Assert.assertEquals(0, tr.getStartOffset())
            Assert.assertEquals(0, tr.getFirstOffset())
        }
    }


    private val content = ("Once a boy a Rosebud spied,\n" +
            "Heathrose fair and tender,\n" +
            "All array'd in youthful pride,--\n" +
            "Quickly to the spot he hied,\n" +
            "Ravished by her splendour.\n" +
            "Rosebud, rosebud, rosebud red,\n" +
            "Heathrose fair and tender!\n").toByteArray()

    private val fileInfoReader = FileInfoReader(UnixDefaultFileSystemInfo()) //TODO: issue#2

    private fun withFile(fileContent: ByteArray, action: (FileInfo) -> Unit) {
        val dir = Files.createTempDirectory("file-repr-test")
        try {
            val file = Files.createTempFile(dir, "file-repr-test", "file").toFile()
            file.writeBytes(fileContent)
            val fileInfo = fileInfoReader.getFileInfo(RsynkFile(file.absolutePath, { RsynkFileBoundaries(0, file.length()) }))
            action(fileInfo)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }
}
