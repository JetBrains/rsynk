package jetbrains.rsynk.command

import jetbrains.rsynk.checksum.RollingChecksumSeedUtil
import jetbrains.rsynk.exitvalues.UnsupportedProtocolException
import jetbrains.rsynk.extensions.reverseAndCastToInt
import jetbrains.rsynk.extensions.toReversedByteArray
import jetbrains.rsynk.files.FilterList
import jetbrains.rsynk.io.ReadingIO
import jetbrains.rsynk.io.SynchronousReadingIO
import jetbrains.rsynk.io.SynchronousWritingIO
import jetbrains.rsynk.io.WritingIO
import jetbrains.rsynk.protocol.CompatFlag
import jetbrains.rsynk.protocol.RsyncConstants
import jetbrains.rsynk.protocol.encode
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.OutputStream

class RsyncServerSendCommand(private val serverCompatFlags: Set<CompatFlag>) : RsyncCommand {

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

    val clientProtocolVersion = setupProtocol(inputIO, outputIO)

    writeCompatFlags(outputIO)

    val rollingChecksumSeed = RollingChecksumSeedUtil.nextSeed()
    writeChecksumSeed(rollingChecksumSeed, outputIO)

    val filter = receiveFilterList(inputIO)
  }


  /**
   * Identical to original rsync code.
   * Writes server protocol version
   * and reads protocol client's version.
   *
   * @throws {@code UnsupportedProtocolException} if client's protocol version
   * either too old or too modern
   *
   * @return  protocol version sent by client
   */
  private fun setupProtocol(input: ReadingIO, output: WritingIO): Int {
    /* must write version in first byte and keep the rest 3 zeros */
    val serverVersion = 31.toReversedByteArray()
    output.writeBytes(serverVersion, 0, 4)

    /* same for the reading: first byte is version, rest are zeros */
    val clientVersionResponse = input.readBytes(4)
    if (clientVersionResponse[1] != 0.toByte() || clientVersionResponse[2] != 0.toByte() || clientVersionResponse[3] != 0.toByte()) {
      log.error("Wrong assumption was made: at least one of last 3 elements of 4 client version buffer is not null: " +
              clientVersionResponse.joinToString())
    }

    val clientProtocolVersion = clientVersionResponse.reverseAndCastToInt()
    if (clientProtocolVersion < RsyncConstants.clientProtocolVersionMin) {
      throw UnsupportedProtocolException("Client protocol version must be at least ${RsyncConstants.clientProtocolVersionMin}")
    }
    if (clientProtocolVersion > RsyncConstants.clientProtocolVersionMax) {
      throw UnsupportedProtocolException("Client protocol version must be no more than ${RsyncConstants.clientProtocolVersionMax}")
    }

    return clientProtocolVersion
  }

  /**
   * Writes server's {@code serverCompatFlags}.
   */
  private fun writeCompatFlags(output: WritingIO) {
    val serverCompatFlags = serverCompatFlags.encode()
    output.writeBytes(byteArrayOf(serverCompatFlags))
  }

  /**
   * Writes rolling checksum seed.
   * */
  private fun writeChecksumSeed(checksumSeed: Int, output: WritingIO) {
    output.writeBytes(checksumSeed.toReversedByteArray())
  }


  /**
   * Receives filter list
   * */
  private fun receiveFilterList(input: ReadingIO): FilterList {

    var len = input.readBytes(4).reverseAndCastToInt()

    /* It's not clear why client writes those 4 bytes.
    * Rsync uses it's 'safe_read' on early stages
    * which deals with circular buffer. It's probably
    * remained in buffer data. Try to ignore it while
    * things work.
    * */
    if (len > 1024 * 5) {
      len = input.readBytes(4).reverseAndCastToInt()
    }
    while (len != 0) {
      //TODO: receive & parse filter list
      //http://blog.mudflatsoftware.com/blog/2012/10/31/tricks-with-rsync-filter-rules/
      val bytes = input.readBytes(len).reverseAndCastToInt()
      len = input.readBytes(4).reverseAndCastToInt()
    }
    return FilterList()
  }
}
