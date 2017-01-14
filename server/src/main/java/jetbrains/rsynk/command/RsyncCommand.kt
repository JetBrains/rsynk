package jetbrains.rsynk.command

import jetbrains.rsynk.exitvalues.ActionNotSupportedException
import jetbrains.rsynk.exitvalues.ModuleNotFoundException
import jetbrains.rsynk.extensions.dropNewLine
import jetbrains.rsynk.files.Modules
import jetbrains.rsynk.protocol.*
import org.slf4j.LoggerFactory
import java.io.*

class RsyncCommand(private val modules: Modules) : SSHCommand {

  private val log = LoggerFactory.getLogger(javaClass)

  override fun execute(args: List<String>, input: InputStream, output: OutputStream, error: OutputStream) {

    assertCommandSupported(args)

    /* protocol negotiation phase */
    val clientProtocolVersion = String(read(input)).dropNewLine()
    log.debug("Client protocol version=$clientProtocolVersion")
    val protocolVersionParser = ProtocolVersionParser(clientProtocolVersion)
    log.debug("Protocol version ${protocolVersionParser.protocolVersion} is set for the session")
    write(protocolVersionParser.rsyncFormattedProtocolVersion.toByteArray(), output)

    /* module negotiation phase */
    val requestedModule = String(read(input)).dropNewLine()
    if (requestedModule == "") {
      log.debug("Request: list all modules")
      write(ListAllModulesProcedure(modules).response.toByteArray(), output)
      return
    }
    val module = modules.find(requestedModule)
    if (module == null) {
      write("${Constants.ERROR}Unknown module $requestedModule".toByteArray(), output)
      throw ModuleNotFoundException("Unknown module $requestedModule")
    }
    log.debug("Requested module=$module")

    /* authentication phase */
    write("${Constants.RSYNCD_OK}\n".toByteArray(), output)

    /* reading args phase */
    val request = RequestParser(String(read(input)), protocolVersionParser.protocolVersion)

    /*  protocol setup phase */
    write(SetupProtocolProcedure(request.options).response.toByteArray(), output)
  }

  private fun assertCommandSupported(args: List<String>) {
    if (args[0] == "rsync" && args[1] == "--server" && args[2] == "--daemon" && args[3] == ".") {
      return
    }
    throw ActionNotSupportedException("Command $args is not supported")
  }


  private fun read(stream: InputStream): ByteArray {
    val available = stream.available()
    if (available == 0) {
      return byteArrayOf()
    }
    val bytes = ByteArray(available)
    val read = stream.read(bytes)
    if (read < available) {
      return bytes.sliceArray(0..read)
    }
    return bytes
  }

  private fun write(data: ByteArray, s: OutputStream) {
    s.write(data)
    s.flush()
  }
}