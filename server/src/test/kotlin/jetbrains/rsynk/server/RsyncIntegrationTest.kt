package jetbrains.rsynk.server

import jetbrains.rsynk.application.Rsynk
import jetbrains.rsynk.files.RsynkFile
import jetbrains.rsynk.files.RsynkFileBoundaries
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

class RsyncIntegrationTest {
    companion object {
        val freePort = IntegrationTestTools.findFreePort()

        @JvmStatic
        val rsynk = Rsynk.newBuilder().apply {
            port = freePort
            nioWorkers = 1
            commandWorkers = 1
            idleConnectionTimeout = 30000
            serverKeysProvider = IntegrationTestTools.getServerKey()
        }.build()

        @AfterClass
        @JvmStatic
        fun stopServer() = rsynk.close()

        val id = AtomicInteger(0)
    }

    @Test
    fun file_transfer_test() {
        val data = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val source = File(data, "from.txt")
        source.writeText(IntegrationTestTools.loremIpsum)

        val rsynkFile = RsynkFile(source, { RsynkFileBoundaries(0, source.length()) })
        rsynk.setTrackingFiles(listOf(rsynkFile))

        val destinationDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val destinationFile = File(destinationDir, "to.txt")
        Assert.assertTrue("Cannot create new file", destinationFile.createNewFile())

        RsyncCommand.sync("localhost:${source.absolutePath}", destinationFile.absolutePath, freePort, 10, "v")
        Assert.assertEquals(IntegrationTestTools.loremIpsum, destinationFile.readText())
    }

    @Test
    fun files_from_same_directory_with_common_prefix_test() {
        val dataDirectory = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val sourceFile1 = File(dataDirectory, "source-file-1.txt")
        sourceFile1.writeText(IntegrationTestTools.loremIpsum)
        val sourceFile2 = File(dataDirectory, "source-file-2.txt")
        sourceFile2.writeText(IntegrationTestTools.loremIpsum)

        rsynk.setTrackingFiles(
                listOf(RsynkFile(sourceFile1, { RsynkFileBoundaries(0, sourceFile1.length()) }),
                        RsynkFile(sourceFile2, { RsynkFileBoundaries(0, sourceFile2.length()) }))
        )

        val destinationDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()

        RsyncCommand.sync("localhost:${dataDirectory.absolutePath}/", destinationDir.absolutePath, freePort, 10, "rv")

        listOf(
                File(destinationDir, sourceFile1.name),
                File(destinationDir, sourceFile2.name)
        ).forEach { downloadedFile ->
            Assert.assertTrue("${downloadedFile.name} was no downloaded", downloadedFile.isFile)
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
        rsynk.setTrackingFiles(listOf(rsynkFile))

        val destinationDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val destinationFile = File(destinationDir, "to.txt")

        RsyncCommand.sync("localhost:${source.absolutePath}", destinationFile.absolutePath, freePort, 10, "v")
        Assert.assertEquals(IntegrationTestTools.loremIpsum, destinationFile.readText())
    }

    @Test
    fun incremental_file_transfer_test() {
        val moduleRoot = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val source = File(moduleRoot, "from.txt")
        source.writeText(IntegrationTestTools.loremIpsum)

        val rsynkFile = RsynkFile(source, { RsynkFileBoundaries(0, source.length()) })
        rsynk.addTrackingFile(rsynkFile)

        val destinationDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val destinationFile = File(destinationDir, "to.txt")
        destinationFile.writeText(IntegrationTestTools.loremIpsum.substring(0, IntegrationTestTools.loremIpsum.length / 2))

        RsyncCommand.sync("localhost:${source.absolutePath}", destinationFile.absolutePath, freePort, 10, "v")
        Assert.assertEquals(IntegrationTestTools.loremIpsum, destinationFile.readText())
    }
}
