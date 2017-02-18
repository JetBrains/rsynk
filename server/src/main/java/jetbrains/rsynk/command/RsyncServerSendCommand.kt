package jetbrains.rsynk.command

import jetbrains.rsynk.exitvalues.UnsupportedProtocolException
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
   *
   * Protocol phases enumerated and phases documented in protocol.md
   * */
  override fun execute(args: List<String>, input: InputStream, output: OutputStream, error: OutputStream) {
    val inputIO = SynchronousReadingIO(input)
    val outputIO = SynchronousWritingIO(output)
    val errorIO = SynchronousWritingIO(error)
    /* 1 */
    val clientVersion = setupProtocol(inputIO, outputIO)
    /* 2 */

  }


  /**
   * Writes server protocol version
   * and reads protocol client's version.
   *
   * @throws {@code UnsupportedProtocolException} if client's protocol version
   * either too old or too modern
   *
   * @return client protocol version
   */
  private fun setupProtocol(input: ReadingIO, output: WritingIO): Int {
    /* must write version in first byte and keep the rest 3 zeros */
    val serverVersion = byteArrayOf(RsyncConstants.protocolVersion.toByte(), 0, 0, 0)
    output.writeBytes(serverVersion, 0, 4)

    /* same for the reading: first byte is version, rest are zeros */
    val response = input.readBytes(4)
    if (response[1] != 0.toByte() || response[2] != 0.toByte() || response[3] != 0.toByte()) {
      log.error("Wrong assumption was made: at least one of last 3 elements of 4 client version buffer is not null: " +
              response.joinToString())
    }

    val clientProtocolVersion = response[0]
    if (clientProtocolVersion < RsyncConstants.clientProtocolVersionMin) {
      throw UnsupportedProtocolException("Protocol must be at least ${RsyncConstants.clientProtocolVersionMin} on the Client")
    }
    if (clientProtocolVersion > RsyncConstants.clientProtocolVersionMax) {
      throw UnsupportedProtocolException("Protocol must no more than ${RsyncConstants.clientProtocolVersionMax} on the Client")
    }
    return clientProtocolVersion.toInt()
  }
}
