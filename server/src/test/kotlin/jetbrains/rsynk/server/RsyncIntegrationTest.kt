package jetbrains.rsynk.server

import jetbrains.rsynk.application.Rsynk
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

class RsyncIntegrationTest {
    companion object {
        @JvmStatic
        val rsynk = Rsynk(TestTools.findFreePort(), 1, 1, 100, TestTools.getServerKey(), emptySet())

        @BeforeClass
        @JvmStatic
        fun startServer() = rsynk.start()

        @BeforeClass
        @JvmStatic
        fun stopServer() = rsynk.stop()

        val id = AtomicInteger(0)
    }

    @Test
    fun file_transfer_test() {
        val data = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val source = File(data, "from.txt")
        source.writeText(TestTools.loremIpsum)

        val destinationDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val destinationFile = File(destinationDir, "to.txt")
        Assert.assertTrue("Cannot create new file", destinationFile.createNewFile())

        RsyncCommand.sync("localhost:${source.absolutePath}", destinationFile.absolutePath, rsynk.port, 10, "v")
        Assert.assertEquals(TestTools.loremIpsum, destinationFile.readText())
    }

    @Test
    fun file_transfer_to_non_existing_file_test() {
        val data = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val source = File(data, "from.txt")
        source.writeText(TestTools.loremIpsum)

        val destinationDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val destinationFile = File(destinationDir, "to.txt")

        RsyncCommand.sync("localhost:${source.absolutePath}", destinationFile.absolutePath, rsynk.port, 10, "v")
        Assert.assertEquals(TestTools.loremIpsum, destinationFile.readText())
    }

    @Test
    fun incremental_file_transfer_test() {
        val moduleRoot = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val source = File(moduleRoot, "from.txt")
        source.writeText(TestTools.loremIpsum)

        val destinationDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
        val destinationFile = File(destinationDir, "to.txt")
        destinationFile.writeText(TestTools.loremIpsum.substring(0, TestTools.loremIpsum.length / 2))

        RsyncCommand.sync("localhost:${source.absolutePath}", destinationFile.absolutePath, rsynk.port, 10, "v")
        Assert.assertEquals(TestTools.loremIpsum, destinationFile.readText())
    }
}
