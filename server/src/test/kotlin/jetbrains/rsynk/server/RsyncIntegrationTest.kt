package jetbrains.rsynk.server

import jetbrains.rsynk.application.Rsynk
import jetbrains.rsynk.application.RsynkFile
import jetbrains.rsynk.application.RsynkFileBoundaries
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

class RsyncIntegrationTest {
    companion object {
        val port = IntegrationTestTools.findFreePort()

        @JvmStatic
        val rsynk = Rsynk.newBuilder()
                .setPort(port)
                .setNioWorkersNumber(1)
                .setCommandWorkersNumber(1)
                .setIdleConnectionTimeout(50000)
                .setServerKeysProvider(IntegrationTestTools.getServerKey())
                .build()


        @BeforeClass
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
        rsynk.setFiles(listOf(rsynkFile))

        val destinationDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val destinationFile = File(destinationDir, "to.txt")
        Assert.assertTrue("Cannot create new file", destinationFile.createNewFile())

        RsyncCommand.sync("localhost:${source.absolutePath}", destinationFile.absolutePath, port, 10, "v")
        Assert.assertEquals(IntegrationTestTools.loremIpsum, destinationFile.readText())
    }

    @Test
    fun file_transfer_to_non_existing_file_test() {
        val data = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val source = File(data, "from.txt")
        source.writeText(IntegrationTestTools.loremIpsum)

        val rsynkFile = RsynkFile(source, { RsynkFileBoundaries(0, source.length()) })
        rsynk.setFiles(listOf(rsynkFile))

        val destinationDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val destinationFile = File(destinationDir, "to.txt")

        RsyncCommand.sync("localhost:${source.absolutePath}", destinationFile.absolutePath, port, 10, "v")
        Assert.assertEquals(IntegrationTestTools.loremIpsum, destinationFile.readText())
    }

    @Test
    fun incremental_file_transfer_test() {
        val moduleRoot = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val source = File(moduleRoot, "from.txt")
        source.writeText(IntegrationTestTools.loremIpsum)

        val rsynkFile = RsynkFile(source, { RsynkFileBoundaries(0, source.length()) })
        rsynk.setFiles(listOf(rsynkFile))

        val destinationDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val destinationFile = File(destinationDir, "to.txt")
        destinationFile.writeText(IntegrationTestTools.loremIpsum.substring(0, IntegrationTestTools.loremIpsum.length / 2))

        RsyncCommand.sync("localhost:${source.absolutePath}", destinationFile.absolutePath, port, 10, "v")
        Assert.assertEquals(IntegrationTestTools.loremIpsum, destinationFile.readText())
    }
}
