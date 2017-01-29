package jetbrains.rsynk.command

import jetbrains.rsynk.exitvalues.ActionNotSupportedException
import jetbrains.rsynk.exitvalues.InvalidFileException
import jetbrains.rsynk.exitvalues.ModuleNotFoundException
import jetbrains.rsynk.extensions.dropNewLine
import jetbrains.rsynk.files.Module
import jetbrains.rsynk.files.Modules
import jetbrains.rsynk.io.IOSession
import jetbrains.rsynk.protocol.*
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class RsyncCommand(private val modules: Modules) : SSHCommand {

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

  /* module negotiation phase */
    val requestedModule = io.readString().dropNewLine()
    if (requestedModule == "") {
      log.debug("Requested all modules listing")
      io.writeString(ListAllModulesProcedure(modules).response)
      return
    }
    val module = Module("sandbox", File("/Users/voytovichs/desktop"), "ahsds")//modules.find(requestedModule)
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
    val filterListLength = io.readInt()
    val filterListParser: FilterListParser? = if (filterListLength <= 4096) {
      val filterList = io.readBytes(filterListLength)
       FilterListParser(String(filterList))
    } else {
      log.error("Received filter list length is too big: $filterListLength, skip filter list reading")
      null
    }
  /* sending files list phase */

    val files = findRequestedFiles(request.files, module)
    val nextLine = io.readString() //Got \0 x4
    TODO()
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

  private fun findRequestedFiles(files: List<String>, module: Module): List<File> {
    val result = ArrayList<File>()
    for (path in files) {
      if (!path.startsWith(module.name + "/")) {
        throw InvalidFileException("File '$path' doesn't belong to requested '${module.name}' module")
      }

      val moduleRelativePath = path.substring(module.name.length + 1)
      log.debug("Request file $moduleRelativePath from module $module")
      val reconstructedGlobalPath = module.root.absolutePath + File.separator + moduleRelativePath.replace("/", File.separator)
      val file = File(reconstructedGlobalPath)

      if (!file.isFile) {
        throw InvalidFileException("File '$moduleRelativePath' not found in '${module.name}' module")
      }

      result.add(file)
    }
    return result
  }

  private fun assertCommandSupported(args: List<String>) {
    if (args.size == 4 && args[0] == "rsync" && args[1] == "--server" && args[2] == "--daemon" && args[3] == ".") {
      return
    }
    throw ActionNotSupportedException("Command $args is not supported")
  }
}
