package jetbrains.rsynk.server

import jetbrains.rsynk.application.Rsynk
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

class RsyncIntegrationTest {
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
  fun sleeeep() {
    Thread.sleep(Long.MAX_VALUE)
  }

  @Test
  fun file_transfer_test() {
    val moduleRoot = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
    val source = File(moduleRoot, "from.txt")
    source.writeText(TestTools.loremIpsum)
    val module = "module-${id.incrementAndGet()}"

    val destinationDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
    val destinationFile = File(destinationDir, "to.txt")
    Rsync.sync("rsync://localhost:$module/${source.name}", destinationFile.absolutePath, rsynk.port, password, 10, "-v")
    Assert.assertEquals(TestTools.loremIpsum, destinationFile.readText())
  }

  @Test
  fun incremental_file_transfer_test() {
    val moduleRoot = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
    val source = File(moduleRoot, "from.txt")
    source.writeText(TestTools.loremIpsum)
    val module = "module-${id.incrementAndGet()}"

    val destinationDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
    val destinationFile = File(destinationDir, "to.txt")
    destinationFile.writeText(TestTools.loremIpsum.substring(0, TestTools.loremIpsum.length / 2))
    Rsync.sync("rsync://localhost:$module/${source.name}", destinationFile.absolutePath, rsynk.port, password, 10, "-v")
    Assert.assertEquals(TestTools.loremIpsum, destinationFile.readText())
  }

  @Test
  fun module_transfer_test() {
    val moduleRoot = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
    val firstSource = File(moduleRoot, "first.txt")
    val secondSource = File(moduleRoot, "second.txt")
    firstSource.writeText(TestTools.loremIpsum)
    secondSource.writeText(TestTools.loremIpsum)
    val module = "module-${id.incrementAndGet()}"
    val destinationDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
    Rsync.sync("rsync://localhost:$module/", destinationDir.absolutePath + "/", rsynk.port, password, 10, "-rv")
    Assert.assertTrue(destinationDir.listFiles().any { it.name == firstSource.name })
    Assert.assertTrue(destinationDir.listFiles().any { it.name == secondSource.name })
    destinationDir.listFiles().forEach { file ->
      Assert.assertEquals(TestTools.loremIpsum, file.readText())
    }
  }

  @Test
  fun file_deletion_test() {
    val moduleRoot = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
    val firstSource = File(moduleRoot, "first.txt")
    firstSource.writeText(TestTools.loremIpsum)
    val module = "module-${id.incrementAndGet()}"
    val destinationDir = Files.createTempDirectory("data-${id.incrementAndGet()}").toFile()
    val firstDest = File(destinationDir, "first.txt")
    val secondDest = File(destinationDir, "second.txt")
    firstDest.writeText(TestTools.loremIpsum)
    secondDest.writeText(TestTools.loremIpsum)
    Rsync.sync("rsync://localhost:$module", destinationDir.absolutePath + "/", rsynk.port, password, 10, "-rv")
    Assert.assertFalse(destinationDir.listFiles().any { it.name == "second.txt" })
  }

  @Test
  fun list_all_modules_test() {
    val modules = listOf(Files.createTempDirectory("mod-${id.incrementAndGet()}"),
            Files.createTempDirectory("mod-${id.incrementAndGet()}")).map(Path::toFile)
    val commandOutput = Rsync.sync("rsync://localhost", "", rsynk.port, password, 100, "-rv")
    modules.forEach {
      Assert.assertTrue(commandOutput.contains(it.name))
    }
  }
}
