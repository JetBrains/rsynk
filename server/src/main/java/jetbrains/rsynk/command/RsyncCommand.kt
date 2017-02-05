package jetbrains.rsynk.command

import jetbrains.rsynk.exitvalues.ActionNotSupportedException
import jetbrains.rsynk.extensions.dropNewLine
import jetbrains.rsynk.io.IOSession
import jetbrains.rsynk.protocol.*
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.OutputStream

class RsyncCommand : SSHCommand {

  private val log = LoggerFactory.getLogger(javaClass)

  override fun execute(args: List<String>, input: InputStream, output: OutputStream, error: OutputStream) {

    assertCommandSupported(args)

    val io = IOSession(input, output)
    negotiate(io)
  }

  private fun negotiate(io: IOSession) {
   /* protocol negotiation phase */
    val clientProtocolVersion = io.readString().dropNewLine()
    log.debug("Client protocol version=$clientProtocolVersion")
    val protocolVersionParser = ProtocolVersionParser(clientProtocolVersion)
    log.debug("Protocol version ${protocolVersionParser.protocolVersion} is set for the session")
    io.writeString(protocolVersionParser.rsyncFormattedProtocolVersion)

   /* authentication phase */
    io.writeString("${Constants.RSYNCD_OK}\n")

   /* request reading phase */
    val requestStr = io.readString()
    val request = RequestParser(requestStr)

    /* finishing protocol setup phase */
    val protocolSetup = FinishProtocolSetupProcedure(request.options, protocolVersionParser.protocolVersion)
    protocolSetup.flags?.let { io.writeByte(it) }
    io.writeInt(protocolSetup.checksumSeed)

   /* receiving filter list phase */
    val filterListLength = io.readInt()
    val filterListParser: FilterListParser? = if (filterListLength <= 4096) {
      val filterList = io.readBytes(filterListLength)
      FilterListParser(String(filterList))
    } else {
      log.error("Received filter list length is too big: $filterListLength, skip filter list reading")
      null
    }
   /* sending files list phase */

    val nextLine = io.readString() //Got \0 x4
    io.writeByte(0) // end of the file list transmission signal

   /* read final goodbye */
    if (protocolVersionParser.protocolVersion >= 24) {
      val finalGoodbye = if (protocolVersionParser.protocolVersion < 29) {
        io.readInt()
      } else {
        TODO()
      }
    }
  }

  private fun assertCommandSupported(args: List<String>) {
    if (args.size > 3 && args[0] == "rsync" && args[1] == "--server" && args[2] == "--sender") {
      return
    }
    throw ActionNotSupportedException("Command $args is not supported")
  }
}
