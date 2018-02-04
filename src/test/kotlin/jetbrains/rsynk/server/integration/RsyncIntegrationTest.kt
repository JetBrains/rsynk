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
package jetbrains.rsynk.server.integration

import jetbrains.rsynk.rsync.files.RsynkFile
import jetbrains.rsynk.server.util.IntegrationTools
import jetbrains.rsynk.server.util.Rsync
import jetbrains.rsynk.server.util.RsyncIntegrationRule
import jetbrains.rsynk.server.util.RsynkResource
import org.junit.*
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

class RsyncIntegrationTest {

    val rsynk = rsynkResource.rsynk

    @Before
    fun clearTrackingFiles() {
        rsynk.untrackAll()
    }

    @Test
    fun file_transfer_test() {
        val data = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val source = File(data, "from.txt")
        source.writeText(IntegrationTools.loremIpsum)

        val rsynkFile = RsynkFile(source.absolutePath)
        rsynk.track(rsynkFile)

        val destinationDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val destinationFile = File(destinationDir, "to.txt")

        Rsync.execute("localhost:${source.absolutePath}", destinationFile.absolutePath, rsynkResource.port, 10, "v")
        Assert.assertEquals(IntegrationTools.loremIpsum, destinationFile.readText())
    }

    @Test
    fun file_transfer_to_existing_source_test() {
        val data = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val source = File(data, "from.txt")
        source.writeText(IntegrationTools.loremIpsum)

        val rsynkFile = RsynkFile(source.absolutePath)
        rsynk.track(rsynkFile)

        val destinationDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val destinationFile = File(destinationDir, "to.txt")
        Assert.assertTrue("Cannot create new file", destinationFile.createNewFile())

        Rsync.execute("localhost:${source.absolutePath}", destinationFile.absolutePath, rsynkResource.port, 10000, "v")
        Assert.assertEquals(IntegrationTools.loremIpsum, destinationFile.readText())
    }

    @Test
    @Ignore("test for issue#5, which is not implemented")
    fun files_from_same_directory_with_common_prefix_test() {
        val dataDirectory = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val sourceFile1 = File(dataDirectory, "source-file-1.txt")
        sourceFile1.writeText(IntegrationTools.loremIpsum)
        val sourceFile2 = File(dataDirectory, "source-file-2.txt")
        sourceFile2.writeText(IntegrationTools.loremIpsum)

        rsynk.track(
                listOf(RsynkFile(sourceFile1.absolutePath),
                        RsynkFile(sourceFile2.absolutePath))
        )

        val destinationDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()

        Rsync.execute("localhost:${dataDirectory.absolutePath}/", destinationDir.absolutePath, rsynkResource.port, 10, "rv")

        listOf(
                File(destinationDir, sourceFile1.name),
                File(destinationDir, sourceFile2.name)
        ).forEach { downloadedFile ->
            Assert.assertTrue("${downloadedFile.name} was not downloaded", downloadedFile.isFile)
            Assert.assertEquals("${downloadedFile.name} content is not identical to source",
                    IntegrationTools.loremIpsum,
                    downloadedFile.readText())
        }


    }

    @Test
    fun file_transfer_to_non_existing_file_test() {
        val data = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val source = File(data, "from.txt")
        source.writeText(IntegrationTools.loremIpsum)

        val rsynkFile = RsynkFile(source.absolutePath)
        rsynk.track(rsynkFile)

        val destinationDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val destinationFile = File(destinationDir, "to.txt")

        Rsync.execute("localhost:${source.absolutePath}", destinationFile.absolutePath, rsynkResource.port, 10, "v")
        Assert.assertEquals(IntegrationTools.loremIpsum, destinationFile.readText())
    }

    @Test
    fun incremental_file_transfer_test() {
        val moduleRoot = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val source = File(moduleRoot, "from.txt")
        source.writeText(IntegrationTools.loremIpsum)

        val rsynkFile = RsynkFile(source.absolutePath)
        rsynk.track(rsynkFile)

        val destinationDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val destinationFile = File(destinationDir, "to.txt")
        destinationFile.writeText(IntegrationTools.loremIpsum.substring(0, IntegrationTools.loremIpsum.length / 2))

        Rsync.execute("localhost:${source.absolutePath}", destinationFile.absolutePath, rsynkResource.port, 10, "v")
        Assert.assertEquals(IntegrationTools.loremIpsum, destinationFile.readText())
    }

    @Test
    fun incremental_file_transfer_8kb_test() {
        val sourceRoot = Files.createTempDirectory("data-root").toFile()
        val srcFile = File(sourceRoot, "file.txt")

        val content = IntegrationTools.readTestResouceText("8kb.txt").toByteArray()
        srcFile.writeBytes(content)
        rsynk.track(RsynkFile(srcFile.absolutePath))
        val destRoot = Files.createTempDirectory("data-root").toFile()
        val destFile = File(destRoot, "file.txt")

        destFile.writeBytes(content.copyOfRange(0, content.size / 3))
        destFile.appendBytes(content.copyOfRange(content.size * 2 / 3 + 1, content.size))

        Rsync.execute("localhost:${srcFile.absolutePath}",
                destFile.absolutePath, rsynkResource.port, 10, "v")

        Assert.assertEquals(srcFile.readText(), destFile.readText())
    }

    @Test
    fun transfer_several_files_test() {
        val sourceRoot = Files.createTempDirectory("data-root").toFile()
        val sourceSubDir = File(sourceRoot, "a").apply { mkdir() }
        val fileA1 = File(sourceSubDir, "a1.txt").apply { writeText("hohoho" + name) }
        val fileA2 = File(sourceSubDir, "a2.txt").apply { writeText("hoho" + name) }
        val fileB1 = File(sourceRoot, "b1.txt").apply { writeText("ho" + name) }
        rsynk.track(listOf(
                RsynkFile(fileA1.absolutePath),
                RsynkFile(fileA2.absolutePath),
                RsynkFile(fileB1.absolutePath))
        )

        val destinationRoot = Files.createTempDirectory("data").toFile()
        Rsync.execute("localhost:${fileA1.absolutePath} ${fileA2.absolutePath} ${fileB1.absolutePath}",
                destinationRoot.absolutePath, rsynkResource.port, 10, "v")

        Assert.assertEquals(fileA1.readText(), File(destinationRoot, fileA1.name).readText())
        Assert.assertEquals(fileA2.readText(), File(destinationRoot, fileA2.name).readText())
        Assert.assertEquals(fileB1.readText(), File(destinationRoot, fileB1.name).readText())
    }

    @Test
    fun transfer_several_files_incrementally_test() {
        val sourceRoot = Files.createTempDirectory("data-root").toFile()
        val sourceSubDir = File(sourceRoot, "a").apply { mkdir() }
        val fileA1 = File(sourceSubDir, "a1.txt").apply { writeText("hohoho" + name) }
        val fileA2 = File(sourceSubDir, "a2.txt").apply { writeText("hoho" + name) }
        val fileB1 = File(sourceRoot, "b1.txt").apply { writeText("ho" + name) }
        rsynk.track(listOf(
                RsynkFile(fileA1.absolutePath),
                RsynkFile(fileA2.absolutePath),
                RsynkFile(fileB1.absolutePath))
        )

        val destinationRoot = Files.createTempDirectory("data").toFile()
        File(destinationRoot, "a1.txt").apply { writeText("ho") }
        File(sourceSubDir, "a2.txt").apply { writeText("ho") }
        File(sourceRoot, "b1.txt").apply { writeText("ho") }
        Rsync.execute("localhost:${fileA1.absolutePath} ${fileA2.absolutePath} ${fileB1.absolutePath}",
                destinationRoot.absolutePath, rsynkResource.port, 10, "v")

        Assert.assertEquals(fileA1.readText(), File(destinationRoot, fileA1.name).readText())
        Assert.assertEquals(fileA2.readText(), File(destinationRoot, fileA2.name).readText())
        Assert.assertEquals(fileB1.readText(), File(destinationRoot, fileB1.name).readText())
    }

    @Test
    fun transfer_directory_error_message_test() {
        val sourceRoot = Files.createTempDirectory("data-root").toFile()
        val sourceSubDir = File(sourceRoot, "a").apply { mkdir() }
        val fileA1 = File(sourceSubDir, "a1.txt").apply { writeText("hohoho" + name) }
        val fileA2 = File(sourceSubDir, "a2.txt").apply { writeText("hoho" + name) }

        rsynk.track(listOf(RsynkFile(sourceSubDir.absolutePath),
                RsynkFile(fileA1.absolutePath),
                RsynkFile(fileA2.absolutePath)))

        val destinationRoot = Files.createTempDirectory("data").toFile()
        val output = Rsync.execute("localhost:${sourceSubDir.absolutePath}", destinationRoot.absolutePath, rsynkResource.port, 10, "v", ignoreErrors = true)
        Assert.assertTrue(output, output.contains("directories transferring is not yet supported"))

        // once issue#5 is resolved following lines should be uncommented
        /*
        assertDirectoriesContentSame(sourceRoot, destinationRoot)
        test for recursive mode https://www.digitalocean.com/community/tutorials/how-to-use-rsync-to-sync-local-and-remote-directories-on-a-vps
         */
    }

    @Test
    @Ignore("test for issue#5, which is not implemented")
    fun transfer_directory_incrementally_test() {
        val sourceRoot = Files.createTempDirectory("data-root").toFile()
        val sourceSubDir = File(sourceRoot, "a").apply { mkdir() }
        val fileA1 = File(sourceSubDir, "a1.txt").apply { writeText("hohoho" + name) }
        val fileA2 = File(sourceSubDir, "a2.txt").apply { writeText("hoho" + name) }
        val fileB1 = File(sourceRoot, "b1.txt").apply { writeText("ho" + name) }
        rsynk.track(listOf(
                RsynkFile(fileA1.absolutePath),
                RsynkFile(fileA2.absolutePath),
                RsynkFile(fileB1.absolutePath))
        )

        val destinationRoot = Files.createTempDirectory("data").toFile()
        File(destinationRoot, "a1.txt").apply { writeText("ho") }
        File(sourceSubDir, "a2.txt").apply { writeText("ho") }
        File(sourceRoot, "b1.txt").apply { writeText(fileB1.readText()) }
        Rsync.execute("localhost:${sourceRoot.absolutePath}/", "${destinationRoot.absolutePath}/", rsynkResource.port, 10, "v")

        IntegrationTools.assertDirectoriesContentSame(sourceRoot, destinationRoot)
    }

    @Test
    fun track_non_existing_file_test() {
        val destinationRoot = Files.createTempDirectory("dest").toFile()
        rsynk.track(RsynkFile("/haha/hoho"))
        val output = Rsync.execute("localhost:/haha/hoho", destinationRoot.absolutePath, rsynkResource.port, 10, "v", true)
        Assert.assertTrue(output, output.contains("Cannot read file attributes "))
    }

    @Test
    fun transfer_a_change_in_file_test() {
        val data = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val source = File(data, "from.txt")
        source.writeText(IntegrationTools.loremIpsum)

        val rsynkFile = RsynkFile(source.absolutePath)
        rsynk.track(rsynkFile)

        val destinationDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val destinationFile = File(destinationDir, "to.txt")

        Rsync.execute("localhost:${source.absolutePath}", destinationFile.absolutePath, rsynkResource.port, 10, "v")
        Assert.assertEquals(IntegrationTools.loremIpsum, destinationFile.readText())

        source.appendText("hehe")
        Rsync.execute("localhost:${source.absolutePath}", destinationFile.absolutePath, rsynkResource.port, 10, "v")
        Assert.assertEquals(IntegrationTools.loremIpsum + "hehe", destinationFile.readText())
    }

    companion object {
        val id = AtomicInteger(0)

        @ClassRule
        @JvmField
        val rsyncRule = RsyncIntegrationRule()

        @ClassRule
        @JvmField
        val rsynkResource = RsynkResource()
    }
}
