package jetbrains.rsynk.command

import jetbrains.rsynk.io.ReadingIO
import jetbrains.rsynk.io.WritingIO
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.OutputStream

class RsyncServerSendCommand : RsyncCommand {

  override val args: List<String> = listOf("rsync", "--server", "--sender")

  private val log = LoggerFactory.getLogger(javaClass)

  override fun execute(args: List<String>, input: InputStream, output: OutputStream, error: OutputStream) {

  }


  /**
   * Writes our (server) protocol version
   * we implement and reads client version.
   *
   * @return client protocol version
   */
  private fun setupProtocol(readingIO: ReadingIO, writingIO: WritingIO): Int {
    /* must write version in first byte and keep the rest 3 zeros */
    writingIO.writeBytes(byteArrayOf(31, 0, 0, 0), 0, 4)

    /* same for the reading: first byte is version, rest are zeros */
    val clientVersion = readingIO.readBytes(4)
    if (clientVersion[1] != 0.toByte() || clientVersion[2] != 0.toByte() || clientVersion[3] != 0.toByte()) {
      log.error("Wrong assumption was made: at least one of last 3 elements of 4 client version buffer is not null: " +
              clientVersion.joinToString())
    }
    return clientVersion[0].toInt()
  }
}
