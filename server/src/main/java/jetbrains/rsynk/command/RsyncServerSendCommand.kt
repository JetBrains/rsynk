package jetbrains.rsynk.command

import jetbrains.rsynk.checksum.Checksum
import jetbrains.rsynk.exitvalues.InvalidFileException
import jetbrains.rsynk.exitvalues.UnsupportedProtocolException
import jetbrains.rsynk.extensions.MAX_VALUE_UNSIGNED
import jetbrains.rsynk.extensions.reverseAndCastToInt
import jetbrains.rsynk.extensions.toReversedByteArray
import jetbrains.rsynk.extensions.getTwoLowestBytes
import jetbrains.rsynk.files.FilterList
import jetbrains.rsynk.io.ReadingIO
import jetbrains.rsynk.io.SynchronousReadingIO
import jetbrains.rsynk.io.SynchronousWritingIO
import jetbrains.rsynk.io.WritingIO
import jetbrains.rsynk.protocol.flags.CompatFlag
import jetbrains.rsynk.protocol.RsyncConstants
import jetbrains.rsynk.protocol.flags.FileFlags
import jetbrains.rsynk.protocol.flags.encode
import jetbrains.rsynk.protocol.flags.flags
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class RsyncServerSendCommand(private val serverCompatFlags: Set<CompatFlag>) : RsyncCommand {

  override val matchArgs: List<String> = listOf("rsync", "--server", "--sender")

  private val log = LoggerFactory.getLogger(javaClass)

  /**
   * Perform negotiation and send requested file.
   * The behaviour is identical to {@code $rsync --server --sender}
   * command execution
   *
   * Protocol phases enumerated and phases documented in protocol.md
   * */
  override fun execute(args: List<String>, input: InputStream, output: OutputStream, error: OutputStream) {
    //val flags = args[matchArgs.size]
    //val _dir = args[matchArgs.size + 1]

    val inputIO = SynchronousReadingIO(input)
    val outputIO = SynchronousWritingIO(output)
    val errorIO = SynchronousWritingIO(error)

    setupProtocol(inputIO, outputIO)

    writeCompatFlags(outputIO)

    val rollingChecksumSeed = Checksum.nextSeed()
    writeChecksumSeed(rollingChecksumSeed, outputIO)

    val files = args.subList(matchArgs.size + 2, args.size)
    val filter = receiveFilterList(inputIO)
    sendFileList(files, filter, outputIO)
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
     * Rsync uses it's 'safe_read' int early communication stages
     * which deals with circular buffer. It's probably data remained
     * in buffer. Ignore it unless we figure out the byte is missing. */
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

  private fun sendFileList(requestedFiles: List<String>, filterList: FilterList, output: WritingIO) {
    if (requestedFiles.size != 1) {
      //TODO: ok while goal is to send single file
      //TODO: then try multiple files, directories and whole combinatorics
      throw InvalidFileException("Multiple files requests not implemented yet")
    }
    val fileToSend = resolveFile(requestedFiles.single())
    if (!filterList.include(fileToSend)) {
      // gracefully exit, work is done when work is *none*
      return
    }

    val flags = fileToSend.flags
    val encodedFlags = flags.encode()
    if (encodedFlags.and(0xFF00) != 0 /* means value doesn't fit one byte */ || encodedFlags == 0) {
      /* Rsync plays very dirty there. Comment from native rsync sources:
       * We must make sure we don't send a zero flag byte or the
       * other end will terminate the flist transfer.  Note that
       * the use of XMIT_TOP_DIR on a non-dir has no meaning, so
       * it's harmless way to add a bit to the first flag byte. */
      output.writeBytes(encodedFlags.or(FileFlags.XMIT_TOP_DIR.value).getTwoLowestBytes())
    } else {
      output.writeBytes(byteArrayOf(encodedFlags.toByte()))
    }

    //TODO:
    /* This is used for recursive directory sending
     * used at flist.c ~537. I failed to find any
     * any l1 variable assignment. Very likely things
     * won't work in recursive directory transmission
     * all because of this variable */
    val lastName = ""
    val fileName = fileToSend.name

    val l1 = fileToSend.name.commonPrefixWith(lastName).length
    if (l1 > 0) {
      output.writeBytes(byteArrayOf(l1.toByte()))
    }
    val nameToSend = fileName.substring(l1)
    val l2 = nameToSend.length
    if (l2 > Byte.MAX_VALUE_UNSIGNED) {
      //TODO:
    } else {
      output.writeBytes(byteArrayOf(l2.toByte()))
    }
    output.writeBytes(nameToSend.toByteArray())
    //TODO: flist 570
  }

  private fun resolveFile(path: String): File {
    //TODO: very naive
    return File(path)
  }
}
