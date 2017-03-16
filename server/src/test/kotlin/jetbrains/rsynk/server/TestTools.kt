package jetbrains.rsynk.server

import org.apache.sshd.common.keyprovider.KeyPairProvider
import org.junit.Assert
import java.io.IOException
import java.net.ServerSocket
import java.nio.file.Files
import java.security.KeyFactory
import java.security.KeyPair
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.TimeUnit


object TestTools {
  fun getServerKey(): KeyPairProvider {
    javaClass.classLoader.getResource("private_key.der")
    val privateBytes = javaClass.classLoader.getResource("private_key.der").readBytes()
    val privateKey = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(privateBytes))
    val publicBytes = javaClass.classLoader.getResource("public_key.der").readBytes()
    val publicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(publicBytes))
    return KeyPairProvider { listOf(KeyPair(publicKey, privateKey)) }
  }

  val loremIpsum = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
          "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. " +
          "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
          "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. " +
          "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."

  fun findFreePort(): Int {
    (1..3).forEach {
      (16384..65536).forEach { port ->
        try {
          ServerSocket(port).close()
          return port
        } catch (e: IOException) {
          // Continue
        }
      }
    }
    throw RuntimeException("Cannot find free port in range [16384, 65536]")
  }

}

object RsyncCommand {
  fun sync(from: String, to: String, port: Int, timeoutSec: Long, _params: String): String {
    val params = if (_params.isEmpty()) "" else "-$_params"
    val args = listOf(rsyncPath, params, "--protocol", "31", "-e", "ssh -p $port -o StrictHostKeyChecking=no", from, to)
    val pb = ProcessBuilder(args)
            .directory(Files.createTempDirectory("rsync_dir").toFile())
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectInput(ProcessBuilder.Redirect.PIPE)

    val process = pb.start()
    process.outputStream.close()
    try {
      process.waitFor(timeoutSec, TimeUnit.SECONDS)
    } catch(e: InterruptedException) {
      throw Error("Rsync process is running longer than $timeoutSec sec, aborting...", e)
    }
    Assert.assertEquals("Rsync exit code not equals to 0\n" +
            "args=$args\n" +
            "stdout=${String(process.inputStream.readBytes())}\n" +
            "stderr=${String(process.errorStream.readBytes())}",
            0, process.exitValue())
    val buffer = ByteArray(process.inputStream.available())
    process.inputStream.read(buffer)
    return String(buffer)
  }

  private val rsyncPath: String
    get() {
      val isMac = System.getProperty("os.name")?.toLowerCase()?.contains("mac") ?: false
      if (isMac) {
        return "/usr/local/bin/rsync"
      }
      return "rsync"
    }
}

