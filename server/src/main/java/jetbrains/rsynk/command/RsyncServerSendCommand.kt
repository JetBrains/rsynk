package jetbrains.rsynk.command

import jetbrains.rsynk.io.ReadingIO
import jetbrains.rsynk.io.SynchronousReadingIO
import jetbrains.rsynk.io.SynchronousWritingIO
import jetbrains.rsynk.io.WritingIO
import jetbrains.rsynk.protocol.RsyncConstants
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.OutputStream

class RsyncServerSendCommand : RsyncCommand {

  override val args: List<String> = listOf("rsync", "--server", "--sender")

  private val log = LoggerFactory.getLogger(javaClass)

  /**
   * Perform negotiation and send requested file.
   * The behaviour is identical to {@code $rsync --server --sender}
   * command execution
   * */
  override fun execute(args: List<String>, input: InputStream, output: OutputStream, error: OutputStream) {
    val read = SynchronousReadingIO(input)
    val write = SynchronousWritingIO(output)
    /* 1 */
    val clientVersion = setupProtocol(read, write)
    /* 2 */

  }


  /**
   * Writes server protocol version
   * and reads protocol client's version.
   *
   * @return client protocol version
   */
  private fun setupProtocol(readingIO: ReadingIO, writingIO: WritingIO): Int {
    /* must write version in first byte and keep the rest 3 zeros */
    val serverVersion = byteArrayOf(RsyncConstants.protocolVersion.toByte(), 0, 0, 0)
    writingIO.writeBytes(serverVersion, 0, 4)

    /* same for the reading: first byte is version, rest are zeros */
    val clientVersion = readingIO.readBytes(4)
    if (clientVersion[1] != 0.toByte() || clientVersion[2] != 0.toByte() || clientVersion[3] != 0.toByte()) {
      log.error("Wrong assumption was made: at least one of last 3 elements of 4 client version buffer is not null: " +
              clientVersion.joinToString())
    }
    return clientVersion[0].toInt()
  }
}
