package jetbrains.rsynk.protocol

import jetbrains.rsynk.exitvalues.ProtocolException
import java.util.*


class RequestParser(line: String) {

  val options: Set<Option>
  val files: List<String>

  init {
    val separator = '\u0000'
    val dot = "."

    val lineElements: Queue<String> = LinkedList(line.dropLastWhile { it == separator }.split(separator))
    /* getting long-named request options */
    val longNamedOptions = HashSet<Option>()
    while (lineElements.peek()?.startsWith("--") ?: false) {
      val e = lineElements.poll()?.replace("--", "") ?: throw Error("Sequential queue.peek() and queue.poll() returned different values")
      val option = Option.find(e) ?: throw ProtocolException("Got unknown option '$e'")
      longNamedOptions.add(option)
    }

    /* getting short-named request options */
    val optionsPack = lineElements.poll()
    if (optionsPack == null || !optionsPack.startsWith("-e.")) {
      throw ProtocolException("Expected options starting with '-e' after args, but got $optionsPack. Full line: $line")
    }
    val bareOptions = optionsPack.drop(3)
    options = longNamedOptions + parseShortNamedOptions(bareOptions)

    /* skipping a dot */
    val dotWithUnknownPurpose = lineElements.poll()
    if (dot != dotWithUnknownPurpose) {
      throw ProtocolException("Expected '.' after options list but got $dotWithUnknownPurpose. Full line $line")
    }

    val files = ArrayList<String>()
    /* getting file list*/
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

  fun parseShortNamedOptions(options: String): Set<Option> {
    val result = LinkedHashSet<Option>()
    for (option in options) {
      val parsedOption = Option.find(option.toString()) ?: throw ProtocolException("Got unknown option '$option'")
      result.add(parsedOption)
    }
    return result
  }
}
