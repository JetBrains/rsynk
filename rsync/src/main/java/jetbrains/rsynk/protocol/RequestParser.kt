package jetbrains.rsynk.protocol

import jetbrains.rsynk.exitvalues.ProtocolException
import java.util.*


class RequestParser(line: String, version: Int) {

  val args: List<String>
  val options: Set<Option>
  val files: List<String>

  init {
    val separator = '\u0000'
    val dot = "."
    val lineElements: Queue<String> = LinkedList(line.split(separator))

    /* geting cmd args */
    val args = ArrayList<String>()
    while (lineElements.peek()?.startsWith("--") ?: false) {
      val e = lineElements.poll() ?: break
      args.add(e)
    }
    this.args = args

    /* getting request options */
    val optionsPack = lineElements.poll()
    if (optionsPack == null || !optionsPack.startsWith("-e.")) {
      throw ProtocolException("Expected options starting with '-e' after args, but got $optionsPack. Full line: $line")
    }
    val bareOptions = optionsPack.drop(3)
    options = parseOptions(bareOptions)

    /* skipping a dot */
    val dotWithUnknownPurpose = lineElements.poll()
    if (dot != dotWithUnknownPurpose) {
      throw ProtocolException("Expected '.' after options list but got $dotWithUnknownPurpose. Full line $line")
    }

    val files = ArrayList<String>()
    /* getting files list*/
    while (lineElements.isNotEmpty()) {
      val file = lineElements.poll()
      if (file != null) {
        files.add(file)
      }
    }
    this.files = files

    if (lineElements.isNotEmpty()) {
      throw ProtocolException("Some request elements left unknown $lineElements. Full request $line")
    }
  }

  fun parseOptions(options: String): Set<Option> {
    val result = LinkedHashSet<Option>()
    for (option in options) {
      val parsedOption = Option.values().firstOrNull { it.textValue == option.toString() }
      @Suppress("FoldInitializerAndIfToElvis") // avoid long line
      if (parsedOption == null) {
        throw ProtocolException("Got unknown option '$option'")
      }
      result.add(parsedOption)
    }
    return result
  }
}
