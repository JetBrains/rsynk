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
package jetbrains.rsynk.server

import jetbrains.rsynk.application.Rsynk
import jetbrains.rsynk.files.RsynkFile
import jetbrains.rsynk.files.RsynkFileBoundaries
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

class RsyncIntegrationTest {

    @Before
    fun clearTrackingFiles() {
        rsynk.stopTrackingAllFiles()
    }

    @Test
    fun file_transfer_test() {
        val data = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val source = File(data, "from.txt")
        source.writeText(IntegrationTestTools.loremIpsum)

        val rsynkFile = RsynkFile(source, { RsynkFileBoundaries(0, source.length()) })
        rsynk.trackFile(rsynkFile)

        val destinationDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val destinationFile = File(destinationDir, "to.txt")

        RsyncClientWrapper.sync("localhost:${source.absolutePath}", destinationFile.absolutePath, rsynkPort, 10000, "v")
        Assert.assertEquals(IntegrationTestTools.loremIpsum, destinationFile.readText())
    }

    @Test
    fun file_transfer_to_existing_source_test() {
        val data = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val source = File(data, "from.txt")
        source.writeText(IntegrationTestTools.loremIpsum)

        val rsynkFile = RsynkFile(source, { RsynkFileBoundaries(0, source.length()) })
        rsynk.trackFile(rsynkFile)

        val destinationDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val destinationFile = File(destinationDir, "to.txt")
        Assert.assertTrue("Cannot create new file", destinationFile.createNewFile())

        RsyncClientWrapper.sync("localhost:${source.absolutePath}", destinationFile.absolutePath, rsynkPort, 10000, "v")
        Assert.assertEquals(IntegrationTestTools.loremIpsum, destinationFile.readText())
    }

    @Test
    fun files_from_same_directory_with_common_prefix_test() {
        val dataDirectory = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val sourceFile1 = File(dataDirectory, "source-file-1.txt")
        sourceFile1.writeText(IntegrationTestTools.loremIpsum)
        val sourceFile2 = File(dataDirectory, "source-file-2.txt")
        sourceFile2.writeText(IntegrationTestTools.loremIpsum)

        rsynk.trackFiles(
                listOf(RsynkFile(sourceFile1, { RsynkFileBoundaries(0, sourceFile1.length()) }),
                        RsynkFile(sourceFile2, { RsynkFileBoundaries(0, sourceFile2.length()) }))
        )

        val destinationDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()

        RsyncClientWrapper.sync("localhost:${dataDirectory.absolutePath}/", destinationDir.absolutePath, rsynkPort, 10, "rv")

        listOf(
                File(destinationDir, sourceFile1.name),
                File(destinationDir, sourceFile2.name)
        ).forEach { downloadedFile ->
            Assert.assertTrue("${downloadedFile.name} was not downloaded", downloadedFile.isFile)
            Assert.assertEquals("${downloadedFile.name} content is not identical to source",
                    IntegrationTestTools.loremIpsum,
                    downloadedFile.readText())
        }


    }

    @Test
    fun file_transfer_to_non_existing_file_test() {
        val data = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val source = File(data, "from.txt")
        source.writeText(IntegrationTestTools.loremIpsum)

        val rsynkFile = RsynkFile(source, { RsynkFileBoundaries(0, source.length()) })
        rsynk.trackFile(rsynkFile)

        val destinationDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val destinationFile = File(destinationDir, "to.txt")

        RsyncClientWrapper.sync("localhost:${source.absolutePath}", destinationFile.absolutePath, rsynkPort, 10, "v")
        Assert.assertEquals(IntegrationTestTools.loremIpsum, destinationFile.readText())
    }

    @Test
    fun incremental_file_transfer_test() {
        val moduleRoot = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val source = File(moduleRoot, "from.txt")
        source.writeText(IntegrationTestTools.loremIpsum)

        val rsynkFile = RsynkFile(source, { RsynkFileBoundaries(0, source.length()) })
        rsynk.trackFile(rsynkFile)

        val destinationDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val destinationFile = File(destinationDir, "to.txt")
        destinationFile.writeText(IntegrationTestTools.loremIpsum.substring(0, IntegrationTestTools.loremIpsum.length / 2))

        RsyncClientWrapper.sync("localhost:${source.absolutePath}", destinationFile.absolutePath, rsynkPort, 10, "v")
        Assert.assertEquals(IntegrationTestTools.loremIpsum, destinationFile.readText())
    }

    @Test
    fun transfer_several_files_test() {
        val sourceRoot = Files.createTempDirectory("data-root").toFile()
        val sourceSubDir = File(sourceRoot, "a").apply { mkdir() }
        val fileA1 = File(sourceSubDir, "a1.txt").apply { writeText("hohoho" + name) }
        val fileA2 = File(sourceSubDir, "a2.txt").apply { writeText("hoho" + name) }
        val fileB1 = File(sourceRoot, "b1.txt").apply { writeText("ho" + name) }
        rsynk.trackFiles(listOf(
                RsynkFile(fileA1, { RsynkFileBoundaries(0, fileA1.length()) }),
                RsynkFile(fileA2, { RsynkFileBoundaries(0, fileA2.length()) }),
                RsynkFile(fileB1, { RsynkFileBoundaries(0, fileB1.length()) }))
        )

        val destinationRoot = Files.createTempDirectory("data").toFile()
        RsyncClientWrapper.sync("localhost:${fileA1.absolutePath} ${fileA2.absolutePath} ${fileB1.absolutePath}",
                destinationRoot.absolutePath, rsynkPort, 10, "v")
        assertDirectoriesContentSame(sourceRoot, destinationRoot)
    }

    @Test
    fun transfer_several_files_incrementally_test() {
        val sourceRoot = Files.createTempDirectory("data-root").toFile()
        val sourceSubDir = File(sourceRoot, "a").apply { mkdir() }
        val fileA1 = File(sourceSubDir, "a1.txt").apply { writeText("hohoho" + name) }
        val fileA2 = File(sourceSubDir, "a2.txt").apply { writeText("hoho" + name) }
        val fileB1 = File(sourceRoot, "b1.txt").apply { writeText("ho" + name) }
        rsynk.trackFiles(listOf(
                RsynkFile(fileA1, { RsynkFileBoundaries(0, fileA1.length()) }),
                RsynkFile(fileA2, { RsynkFileBoundaries(0, fileA2.length()) }),
                RsynkFile(fileB1, { RsynkFileBoundaries(0, fileB1.length()) }))
        )

        val destinationRoot = Files.createTempDirectory("data").toFile()
        File(destinationRoot, "a1.txt").apply { writeText("ho") }
        File(sourceSubDir, "a2.txt").apply { writeText("ho") }
        File(sourceRoot, "b1.txt").apply { writeText("ho") }
        RsyncClientWrapper.sync("localhost:${fileA1.absolutePath} ${fileA2.absolutePath} ${fileB1.absolutePath}",
                destinationRoot.absolutePath, rsynkPort, 10, "v")

        assertDirectoriesContentSame(sourceRoot, destinationRoot)
    }

    private fun assertDirectoriesContentSame(a: File, b: File) {
        if (!a.isDirectory) {
            Assert.fail("${a.absolutePath} is not a directory")
        }
        if (!b.isDirectory) {
            Assert.fail("${b.absolutePath} is not a directory")
        }

        val aDirectoryFiles = a.listFiles().sorted()
        val bDirectoryFiles = b.listFiles().sorted()
        Assert.assertEquals("Directories contain different set of files", aDirectoryFiles, bDirectoryFiles)
        for (i in 0..aDirectoryFiles.size) {
            if (aDirectoryFiles[i].isDirectory) {
                assertDirectoriesContentSame(aDirectoryFiles[i], bDirectoryFiles[i])
                continue
            }
            Assert.assertEquals(aDirectoryFiles[i].readText(), bDirectoryFiles[i].readText())
        }
    }

    companion object {
        val rsynkPort = IntegrationTestTools.findFreePort()

        @JvmStatic
        val rsynk = Rsynk.newBuilder().apply {
            port = rsynkPort
            nioWorkers = 1
            commandWorkers = 1
            idleConnectionTimeoutMills = 5 * 1000
            serverKeysProvider = IntegrationTestTools.getServerKey()

            if (IntegrationTestTools.isDebugProtocolEnabled()) {
                idleConnectionTimeoutMills = Int.MAX_VALUE
            }
        }.build()

        @AfterClass
        @JvmStatic
        fun stopServer() = rsynk.close()

        val id = AtomicInteger(0)
    }
}
