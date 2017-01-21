package jetbrains.rsynk.command

import jetbrains.rsynk.exitvalues.ActionNotSupportedException
import jetbrains.rsynk.exitvalues.ModuleNotFoundException
import jetbrains.rsynk.extensions.dropNewLine
import jetbrains.rsynk.files.Modules
import jetbrains.rsynk.io.IOSession
import jetbrains.rsynk.protocol.*
import org.slf4j.LoggerFactory
import java.io.*

class RsyncCommand(private val modules: Modules) : SSHCommand {

  private val log = LoggerFactory.getLogger(javaClass)

  override fun execute(args: List<String>, input: InputStream, output: OutputStream, error: OutputStream) {

    assertCommandSupported(args)

    val io = IOSession(input, output)

  /* protocol negotiation phase */
    val clientProtocolVersion = io.readString().dropNewLine()
    log.debug("Client protocol version=$clientProtocolVersion")
    val protocolVersionParser = ProtocolVersionParser(clientProtocolVersion)
    log.debug("Protocol version ${protocolVersionParser.protocolVersion} is set for the session")
    io.writeString(protocolVersionParser.rsyncFormattedProtocolVersion)

  /* module negotiation phase */
    val requestedModule = io.readString().dropNewLine()
    if (requestedModule == "") {
      log.debug("Request: list all modules")
      io.writeString(ListAllModulesProcedure(modules).response)
      return
    }
    val module = modules.find(requestedModule)
    if (module == null) {
      io.writeString("${Constants.ERROR}Unknown module $requestedModule")
      throw ModuleNotFoundException("Unknown module $requestedModule")
    }
    log.debug("Requested module=$module")

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

  /* sending files phase */
  }

  private fun assertCommandSupported(args: List<String>) {
    if (args[0] == "rsync" && args[1] == "--server" && args[2] == "--daemon" && args[3] == ".") {
      return
    }
    throw ActionNotSupportedException("Command $args is not supported")
  }
}