package jetbrains.rsynk.server

import jetbrains.rsynk.application.Rsynk
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.nio.file.Files

class IntegrationTest {
  companion object {
    @JvmStatic
    val rsynk = Rsynk(20333/*TODO: get available port*/, 1, 1, 100, TestTools.getServerKey())

    @BeforeClass
    @JvmStatic
    fun startServer() = rsynk.start()

    @BeforeClass
    @JvmStatic
    fun stopServer() = rsynk.stop()
  }

  @Test
  fun file_transfer_test() {
    val dataDir = Files.createTempDirectory("data-1").toFile()
    val source = File(dataDir, "from.txt")
    source.writeText(TestTools.loremIpsum)
    val destination = File(dataDir, "to.txt")
    Rsync.syncFile("rsync://localhost:${source.absolutePath}", destination.absolutePath, rsynk.port, "letmein", 10)
    Assert.assertEquals(TestTools.loremIpsum, destination.readText())
  }

  @Test
  fun incremental_file_transfer_test() {
    val dataDir = Files.createTempDirectory("data-2").toFile()
    val source = File(dataDir, "from.txt")
    source.writeText(TestTools.loremIpsum)
    val destination = File(dataDir, "to.txt")
    destination.writeText(TestTools.loremIpsum.substring(0, TestTools.loremIpsum.length / 2))
    Rsync.syncFile("rsync://localhost:${source.absolutePath}", destination.absolutePath, rsynk.port, "letmein", 10)
    Assert.assertEquals(TestTools.loremIpsum, destination.readText())
  }

  @Test
  fun directory_transfer_test() {
    val sourceDir = Files.createTempDirectory("data-3").toFile()
    val firstSource = File(sourceDir, "first.txt")
    val secondSource = File(sourceDir, "second.txt")
    firstSource.writeText(TestTools.loremIpsum)
    secondSource.writeText(TestTools.loremIpsum)
    val destinationDir = Files.createTempDirectory("data-4").toFile()
    Rsync.syncDirectory("rsync://localhost:${sourceDir.absolutePath}", destinationDir.absolutePath, rsynk.port, "letmein", 10)
    Assert.assertTrue(destinationDir.listFiles().any { it.name == firstSource.name })
    Assert.assertTrue(destinationDir.listFiles().any { it.name == secondSource.name })
    destinationDir.listFiles().forEach { file ->
      Assert.assertEquals(TestTools.loremIpsum, file.readText())
    }
  }
}