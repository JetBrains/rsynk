package jetbrains.rsynk.server

import jetbrains.rsynk.application.Rsynk
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

class IntegrationTest {
  companion object {
    @JvmStatic
    val rsynk = Rsynk(TestTools.findFreePort(), 1, 1, 100, TestTools.getServerKey())

    val password = "letmein"

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
    val dataDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
    val source = File(dataDir, "from.txt")
    source.writeText(TestTools.loremIpsum)
    val destination = File(dataDir, "to.txt")
    Rsync.syncFile("rsync://localhost:${source.absolutePath}", destination.absolutePath, rsynk.port, password, 10)
    Assert.assertEquals(TestTools.loremIpsum, destination.readText())
  }

  @Test
  fun incremental_file_transfer_test() {
    val dataDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
    val source = File(dataDir, "from.txt")
    source.writeText(TestTools.loremIpsum)
    val destination = File(dataDir, "to.txt")
    destination.writeText(TestTools.loremIpsum.substring(0, TestTools.loremIpsum.length / 2))
    Rsync.syncFile("rsync://localhost:${source.absolutePath}", destination.absolutePath, rsynk.port, password, 10)
    Assert.assertEquals(TestTools.loremIpsum, destination.readText())
  }

  @Test
  fun directory_transfer_test() {
    val sourceDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
    val firstSource = File(sourceDir, "first.txt")
    val secondSource = File(sourceDir, "second.txt")
    firstSource.writeText(TestTools.loremIpsum)
    secondSource.writeText(TestTools.loremIpsum)
    val destinationDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
    Rsync.syncDirectory("rsync://localhost:${sourceDir.absolutePath}", destinationDir.absolutePath, rsynk.port, password, 10)
    Assert.assertTrue(destinationDir.listFiles().any { it.name == firstSource.name })
    Assert.assertTrue(destinationDir.listFiles().any { it.name == secondSource.name })
    destinationDir.listFiles().forEach { file ->
      Assert.assertEquals(TestTools.loremIpsum, file.readText())
    }
  }

  @Test
  fun directory_file_deletion_test() {
    val sourceDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
    File(sourceDir, "first.txt").writeText(TestTools.loremIpsum)
    val destinationDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
    File(destinationDir, "first.txt").writeText(TestTools.loremIpsum)
    File(destinationDir, "second.txt").writeText(TestTools.loremIpsum)
    Rsync.syncDirectory("rsync://localhost:${sourceDir.absolutePath}", destinationDir.absolutePath, rsynk.port, password, 10)
    Assert.assertFalse(destinationDir.listFiles().any { it.name == "second.txt" })
  }
}