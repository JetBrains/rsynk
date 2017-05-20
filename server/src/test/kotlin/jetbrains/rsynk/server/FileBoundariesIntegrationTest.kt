package jetbrains.rsynk.server

import jetbrains.rsynk.application.Rsynk
import jetbrains.rsynk.files.RsynkFile
import jetbrains.rsynk.files.RsynkFileBoundaries
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FileBoundariesIntegrationTest {

    @Test
    fun set_left_file_bound_test() {
        val dataDirectory = Files.createTempDirectory("data-${RsyncIntegrationTest.id.incrementAndGet()}").toFile()
        val sourceFile = File(dataDirectory, "from.txt")
        sourceFile.writeText(IntegrationTestTools.loremIpsum)

        val destinationDirectory = Files.createTempDirectory("data-${RsyncIntegrationTest.id.incrementAndGet()}").toFile()
        val destinationFile = File(destinationDirectory, "to.txt")
        Assert.assertTrue("Cannot create new file", destinationFile.createNewFile())

        rsynk.addTrackingFile(RsynkFile(sourceFile) {
            RsynkFileBoundaries(10, IntegrationTestTools.loremIpsum.length.toLong() - 10)
        })

        RsyncCommand.sync("localhost:${sourceFile.absolutePath}", destinationFile.absolutePath, RsyncIntegrationTest.freePort, 10, "v")
        Assert.assertEquals(IntegrationTestTools.loremIpsum.substring(10), destinationFile.readText())
    }

    @Test
    fun set_right_file_bound_test() {
        val dataDirectory = Files.createTempDirectory("data-${RsyncIntegrationTest.id.incrementAndGet()}").toFile()
        val sourceFile = File(dataDirectory, "from.txt")
        sourceFile.writeText(IntegrationTestTools.loremIpsum)

        val destinationDirectory = Files.createTempDirectory("data-${RsyncIntegrationTest.id.incrementAndGet()}").toFile()
        val destinationFile = File(destinationDirectory, "to.txt")
        Assert.assertTrue("Cannot create new file", destinationFile.createNewFile())

        rsynk.addTrackingFile(RsynkFile(sourceFile) {
            RsynkFileBoundaries(0, 20)
        })

        RsyncCommand.sync("localhost:${sourceFile.absolutePath}", destinationFile.absolutePath, RsyncIntegrationTest.freePort, 10, "v")
        Assert.assertEquals(IntegrationTestTools.loremIpsum.substring(0, 20), destinationFile.readText())
    }

    @Test
    fun set_left_and_right_bounds_test() {
        val dataDirectory = Files.createTempDirectory("data-${RsyncIntegrationTest.id.incrementAndGet()}").toFile()
        val sourceFile = File(dataDirectory, "from.txt")
        sourceFile.writeText(IntegrationTestTools.loremIpsum)

        val destinationDirectory = Files.createTempDirectory("data-${RsyncIntegrationTest.id.incrementAndGet()}").toFile()
        val destinationFile = File(destinationDirectory, "to.txt")
        Assert.assertTrue("Cannot create new file", destinationFile.createNewFile())

        rsynk.addTrackingFile(RsynkFile(sourceFile) {
            RsynkFileBoundaries(10, 30)
        })

        RsyncCommand.sync("localhost:${sourceFile.absolutePath}", destinationFile.absolutePath, RsyncIntegrationTest.freePort, 10, "v")
        Assert.assertEquals(IntegrationTestTools.loremIpsum.substring(10, 40), destinationFile.readText())
    }

    @Test
    fun can_change_bounds_dynamically_test() {
        val dataDirectory = Files.createTempDirectory("data-${RsyncIntegrationTest.id.incrementAndGet()}").toFile()
        val sourceFile = File(dataDirectory, "from.txt")
        sourceFile.writeText(IntegrationTestTools.loremIpsum)

        val destinationDirectory = Files.createTempDirectory("data-${RsyncIntegrationTest.id.incrementAndGet()}").toFile()
        val destinationFile = File(destinationDirectory, "to.txt")
        Assert.assertTrue("Cannot create new file", destinationFile.createNewFile())

        rsynk.addTrackingFile(RsynkFile(sourceFile) {
            RsynkFileBoundaries(0, 10)
        })

        RsyncCommand.sync("localhost:${sourceFile.absolutePath}", destinationFile.absolutePath, RsyncIntegrationTest.freePort, 10, "v")
        Assert.assertEquals(IntegrationTestTools.loremIpsum.substring(0, 10), destinationFile.readText())


        rsynk.setTrackingFiles(emptyList()).addTrackingFile(RsynkFile(sourceFile) {
            RsynkFileBoundaries(10, 20)
        })

        RsyncCommand.sync("localhost:${sourceFile.absolutePath}", destinationFile.absolutePath, RsyncIntegrationTest.freePort, 10, "v")
        Assert.assertEquals(IntegrationTestTools.loremIpsum.substring(10, 20), destinationFile.readText())
    }

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
    }
}
